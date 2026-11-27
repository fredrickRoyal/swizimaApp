import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { OutOfOfficeNotification, UserSystemRole } from '../models/stafftrack.models';
import {
  StaffTrackApiService,
  extractApiErrorMessage,
} from './stafftrack-api.service';
import { StaffTrackStore } from './stafftrack.store';

export type LoginMode = 'email' | 'phone';
export type OtpChannel = 'EMAIL' | 'SMS' | 'WHATSAPP';

export interface StaffSession {
  id: string;
  staffId?: string;
  fullName: string;
  initials: string;
  email?: string;
  phone?: string;
  role: string;
  systemRole?: string;
  designation?: string;
  department: string;
  dutyStation?: string;
  organisationId?: string;
  organisationName?: string;
  organisationCode?: string;
  identifier: string;
  loginMode: LoginMode;
  signedInAt: string;
}

export interface OtpChallenge {
  otpReference: string;
  identifier: string;
  maskedIdentifier: string;
  loginMode: LoginMode;
  channel: OtpChannel;
  expiresAt: number;
  fullName?: string;
  role?: string;
  department?: string;
  message?: string;
}

export interface CountryDialCode {
  code: string;
  dial: string;
  name: string;
  flag: string;
}

export const COUNTRY_DIAL_CODES: CountryDialCode[] = [
  { code: 'UG', dial: '+256', name: 'Uganda', flag: '🇺🇬' },
  { code: 'KE', dial: '+254', name: 'Kenya', flag: '🇰🇪' },
  { code: 'TZ', dial: '+255', name: 'Tanzania', flag: '🇹🇿' },
  { code: 'RW', dial: '+250', name: 'Rwanda', flag: '🇷🇼' },
  { code: 'SS', dial: '+211', name: 'South Sudan', flag: '🇸🇸' },
  { code: 'CD', dial: '+243', name: 'DR Congo', flag: '🇨🇩' },
  { code: 'ZA', dial: '+27', name: 'South Africa', flag: '🇿🇦' },
  { code: 'GB', dial: '+44', name: 'United Kingdom', flag: '🇬🇧' },
  { code: 'US', dial: '+1', name: 'United States', flag: '🇺🇸' },
];

const SESSION_KEY = 'stafftrack.session.v1';
const CHALLENGE_KEY = 'stafftrack.otp.challenge.v1';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly router = inject(Router);
  private readonly api = inject(StaffTrackApiService);
  private readonly store = inject(StaffTrackStore);

  readonly session = signal<StaffSession | null>(this.loadSession());
  readonly challenge = signal<OtpChallenge | null>(this.loadChallenge());
  readonly loading = signal(false);
  readonly isAuthenticated = computed(() => this.session() !== null);

  readonly resolvedSystemRole = computed<UserSystemRole>(() => {
    const sess = this.session();
    if (!sess) return 'STAFF_OFFICER';

    const emailLower = (sess.email || sess.identifier || '').trim().toLowerCase();
    const normPhone = this.normalizePhone(sess.phone || sess.identifier);

    const matchedUser = this.store.userAccounts().find((u) => {
      if (sess.id && u.id === sess.id) return true;
      if (
        emailLower &&
        (u.email?.toLowerCase() === emailLower || u.username?.toLowerCase() === emailLower)
      ) {
        return true;
      }
      if (normPhone.length > 6 && this.normalizePhone(u.phone) === normPhone) {
        return true;
      }
      return false;
    });

    if (matchedUser?.role) {
      return matchedUser.role;
    }

    const rawRole = (sess.systemRole || sess.role || '').trim().toUpperCase().replace(/\s+/g, '_');
    if (
      rawRole === 'SUPER_ADMIN' ||
      rawRole === 'DEPARTMENT_HEAD' ||
      rawRole === 'DESK_OFFICER' ||
      rawRole === 'STAFF_OFFICER'
    ) {
      return rawRole as UserSystemRole;
    }

    if (
      emailLower === 'kfredrick35@gmail.com' ||
      emailLower === 'fredrick.kasoma@opm.go.ug' ||
      normPhone === '256700408387' ||
      normPhone === '256772408387'
    ) {
      return 'SUPER_ADMIN';
    }

    return 'STAFF_OFFICER';
  });

  readonly isStaffOfficer = computed<boolean>(() => {
    if (!this.isAuthenticated()) return false;
    return this.resolvedSystemRole() === 'STAFF_OFFICER';
  });

  readonly isDepartmentOrUnitHead = computed<boolean>(() => {
    if (!this.isAuthenticated()) return false;
    if (this.resolvedSystemRole() === 'DEPARTMENT_HEAD') {
      return true;
    }
    const sess = this.session();
    const raw = (sess?.systemRole || sess?.role || '')
      .trim()
      .toUpperCase()
      .replace(/[\s/\-]+/g, '_');
    return (
      raw === 'DEPARTMENT_HEAD' ||
      raw === 'UNIT_HEAD' ||
      raw === 'DEPARTMENT_UNIT_HEAD'
    );
  });

  canViewOutOfOfficeRecord(item: OutOfOfficeNotification): boolean {
    if (!this.isStaffOfficer()) {
      return true;
    }
    const sess = this.session();
    if (!sess) return false;

    const emailLower = (sess.email || sess.identifier || '').trim().toLowerCase();
    const normPhone = this.normalizePhone(sess.phone || sess.identifier);

    const matchedStaff = this.store.staffMembers().find((s) => {
      if (sess.staffId && s.id === sess.staffId) return true;
      if (sess.id && s.id === sess.id) return true;
      if (
        emailLower &&
        (s.email?.toLowerCase() === emailLower ||
          s.alternativeEmail?.toLowerCase() === emailLower)
      ) {
        return true;
      }
      if (
        normPhone.length > 6 &&
        (this.normalizePhone(s.phone) === normPhone ||
          this.normalizePhone(s.alternativePhone) === normPhone)
      ) {
        return true;
      }
      return false;
    });

    const targetStaffId = (sess.staffId || matchedStaff?.id || '').trim();
    const targetName = (
      sess.fullName ||
      (matchedStaff ? `${matchedStaff.surname} ${matchedStaff.lastName}` : '')
    ).trim();

    if (targetStaffId && item.staffMemberId && item.staffMemberId === targetStaffId) {
      return true;
    }
    if (targetName && this.namesMatch(item.officerName, targetName)) {
      return true;
    }
    if (this.isAssignedUnitHeadForRecord(item)) {
      return true;
    }
    return false;
  }

  canReviewAsUnitHead(item: OutOfOfficeNotification): boolean {
    if (!this.isAuthenticated()) return false;
    if (!this.isStaffOfficer()) {
      return true;
    }
    return this.isAssignedUnitHeadForRecord(item);
  }

  private isAssignedUnitHeadForRecord(item: OutOfOfficeNotification): boolean {
    const sess = this.session();
    if (!sess) return false;
    const emailLower = (sess.email || sess.identifier || '').trim().toLowerCase();
    const normPhone = this.normalizePhone(sess.phone || sess.identifier);
    const fullName = (sess.fullName || '').trim();

    if (emailLower && item.unitHeadEmail?.trim().toLowerCase() === emailLower) {
      return true;
    }
    if (normPhone.length > 6 && this.normalizePhone(item.unitHeadPhone) === normPhone) {
      return true;
    }
    if (fullName && item.unitHeadName && this.namesMatch(item.unitHeadName, fullName)) {
      return true;
    }
    return false;
  }

  private namesMatch(nameA?: string, nameB?: string): boolean {
    if (!nameA || !nameB) return false;
    const normA = nameA.trim().toLowerCase().replace(/\s+/g, ' ');
    const normB = nameB.trim().toLowerCase().replace(/\s+/g, ' ');
    if (!normA || !normB) return false;
    if (normA === normB) return true;
    const tokensA = normA.split(' ').filter(Boolean).sort().join(' ');
    const tokensB = normB.split(' ').filter(Boolean).sort().join(' ');
    return tokensA.length > 0 && tokensA === tokensB;
  }

  requestOtp(
    identifier: string,
    loginMode: LoginMode,
    channel: OtpChannel,
    onResult?: (res: { ok: boolean; error?: string }) => void
  ): void {
    const clean = identifier.trim();
    this.loading.set(true);

    this.api.requestAuthOtp({ identifier: clean, loginMode, channel }).subscribe({
      next: (resp) => {
        this.loading.set(false);
        const challenge: OtpChallenge = {
          otpReference: resp.otpReference,
          identifier: resp.identifier,
          maskedIdentifier: resp.maskedIdentifier,
          loginMode: resp.loginMode,
          channel: resp.channel,
          expiresAt: resp.expiresAt,
          fullName: resp.fullName,
          role: resp.role,
          department: resp.department,
          message: resp.message,
        };
        this.saveChallenge(challenge);
        onResult?.({ ok: true });
      },
      error: (err) => {
        this.loading.set(false);
        onResult?.({ ok: false, error: extractApiErrorMessage(err) });
      },
    });
  }

  verifyOtp(
    code: string,
    onResult?: (res: { ok: boolean; error?: string }) => void
  ): void {
    const current = this.challenge();
    if (!current) {
      onResult?.({
        ok: false,
        error: 'Your verification session expired. Please request a new code.',
      });
      return;
    }
    const cleanCode = code.trim();
    if (!/^\d{6}$/.test(cleanCode)) {
      onResult?.({ ok: false, error: 'Please enter a valid 6-digit verification code.' });
      return;
    }

    this.loading.set(true);
    this.api
      .verifyAuthOtp({
        otpReference: current.otpReference,
        identifier: current.identifier,
        code: cleanCode,
      })
      .subscribe({
        next: (resp) => {
          this.loading.set(false);
          this.store.recordUserLogin(current.identifier);
          this.completeSignIn(resp);
          this.store.refreshFromApi();
          onResult?.({ ok: true });
        },
        error: (err) => {
          this.loading.set(false);
          onResult?.({ ok: false, error: extractApiErrorMessage(err) });
        },
      });
  }

  private completeLocalSignIn(current: OtpChallenge): void {
    const officer = this.resolveOfficerFromStore(current.identifier);
    const fullName = officer.fullName || current.fullName || 'Kasoma Fredrick';
    const initials =
      fullName
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0]?.toUpperCase() ?? '')
        .join('') || 'FK';

    this.store.recordUserLogin(current.identifier);

    const session: StaffSession = {
      id: officer.id || 'USR-FREDRICK-001',
      staffId: officer.staffId,
      fullName,
      initials,
      email: officer.email,
      phone: officer.phone,
      role: officer.role || 'Principal Officer',
      designation: officer.designation,
      department: officer.department || 'Department of Refugees',
      dutyStation: officer.dutyStation || 'DOR-Kampala',
      organisationId: officer.organisationId || 'ORG-OPM-001',
      organisationName: officer.organisationName || 'Office of the Prime Minister (OPM)',
      organisationCode: officer.organisationCode || 'OPM',
      identifier: current.identifier,
      loginMode: current.loginMode,
      signedInAt: new Date().toISOString(),
    };
    this.completeSignIn(session);
  }

  private completeSignIn(session: StaffSession): void {
    this.session.set(session);
    this.clearChallenge();
    try {
      localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    } catch {}
  }

  private saveChallenge(challenge: OtpChallenge): void {
    this.challenge.set(challenge);
    try {
      sessionStorage.setItem(CHALLENGE_KEY, JSON.stringify(challenge));
    } catch {}
  }

  clearChallenge(): void {
    this.challenge.set(null);
    try {
      sessionStorage.removeItem(CHALLENGE_KEY);
    } catch {}
  }

  logout(): void {
    this.session.set(null);
    this.clearChallenge();
    try {
      localStorage.removeItem(SESSION_KEY);
    } catch {}
    void this.router.navigateByUrl('/login');
  }

  private normalizePhone(phone?: string): string {
    if (!phone) return '';
    const digits = phone.replace(/[^0-9]/g, '');
    if (digits.startsWith('0') && digits.length === 10) {
      return '256' + digits.slice(1);
    }
    return digits;
  }

  private resolveOfficerFromStore(identifier: string): {
    ok: boolean;
    error?: string;
    id?: string;
    staffId?: string;
    fullName?: string;
    email?: string;
    phone?: string;
    role?: string;
    designation?: string;
    department?: string;
    dutyStation?: string;
    organisationId?: string;
    organisationName?: string;
    organisationCode?: string;
  } {
    const lower = identifier.trim().toLowerCase();
    const normPhone = this.normalizePhone(identifier);

    const matchedUser = this.store.userAccounts().find((u) => {
      const emailMatch = u.email.toLowerCase() === lower || u.username.toLowerCase() === lower;
      const phoneMatch = normPhone.length > 6 && this.normalizePhone(u.phone) === normPhone;
      return emailMatch || phoneMatch;
    });

    const defaultOrg = this.store.organisations()[0];

    if (matchedUser) {
      if (matchedUser.status === 'SUSPENDED') {
        return {
          ok: false,
          error: `Your user account (${matchedUser.fullName}) is currently suspended.`,
        };
      }
      const orgLink =
        this.store
          .clientUserLinks()
          .find(
            (cu) =>
              (cu.userEmail?.toLowerCase() === matchedUser.email.toLowerCase() ||
                this.normalizePhone(cu.userPhoneNumber) === this.normalizePhone(matchedUser.phone)) &&
              cu.isDefaultClient
          ) ||
        this.store
          .clientUserLinks()
          .find(
            (cu) =>
              cu.userEmail?.toLowerCase() === matchedUser.email.toLowerCase() ||
              this.normalizePhone(cu.userPhoneNumber) === this.normalizePhone(matchedUser.phone)
          );

      return {
        ok: true,
        id: matchedUser.id,
        staffId: matchedUser.staffId,
        fullName: matchedUser.fullName,
        email: matchedUser.email,
        phone: matchedUser.phone,
        role: matchedUser.designation || matchedUser.role,
        designation: matchedUser.designation,
        department: matchedUser.department,
        dutyStation: matchedUser.dutyStation,
        organisationId: orgLink?.clientRef || defaultOrg?.id || 'ORG-OPM-001',
        organisationName:
          orgLink?.clientName || defaultOrg?.name || 'Office of the Prime Minister (OPM)',
        organisationCode: orgLink?.clientCode || defaultOrg?.code || 'OPM',
      };
    }

    const matchedStaff = this.store.staffMembers().find((s) => {
      const emailMatch =
        s.email.toLowerCase() === lower ||
        s.alternativeEmail?.toLowerCase() === lower;
      const phoneMatch =
        normPhone.length > 6 &&
        (this.normalizePhone(s.phone) === normPhone ||
          this.normalizePhone(s.alternativePhone) === normPhone);
      return emailMatch || phoneMatch;
    });

    if (matchedStaff) {
      return {
        ok: true,
        id: matchedStaff.id,
        staffId: matchedStaff.id,
        fullName: `${matchedStaff.surname} ${matchedStaff.lastName}`,
        email: matchedStaff.email,
        phone: matchedStaff.phone,
        role: matchedStaff.designation,
        designation: matchedStaff.designation,
        department: matchedStaff.department,
        dutyStation: matchedStaff.dutyStation,
        organisationId: defaultOrg?.id || 'ORG-OPM-001',
        organisationName: defaultOrg?.name || 'Office of the Prime Minister (OPM)',
        organisationCode: defaultOrg?.code || 'OPM',
      };
    }

    // Default fallback for seeded Kasoma Fredrick even before store finishes initial HTTP fetch
    if (
      lower === 'kfredrick35@gmail.com' ||
      lower === 'fredrick.kasoma@opm.go.ug' ||
      normPhone === '256700408387' ||
      normPhone === '256772408387'
    ) {
      return {
        ok: true,
        id: 'USR-FREDRICK-001',
        fullName: 'Kasoma Fredrick',
        email: 'kfredrick35@gmail.com',
        phone: '+256 700 408 387',
        role: 'Principal Officer',
        designation: 'Principal Officer',
        department: 'Department of Refugees',
        dutyStation: 'DOR-Kampala',
        organisationId: 'ORG-OPM-001',
        organisationName: 'Office of the Prime Minister (OPM)',
        organisationCode: 'OPM',
      };
    }

    return {
      ok: false,
      error: `No registered user account found for "${identifier}". Please assign the user in User Management first.`,
    };
  }

  private maskIdentifier(value: string, mode: LoginMode): string {
    if (mode === 'email' && value.includes('@')) {
      const [user, domain] = value.split('@');
      if (user.length <= 2) return `${user[0] ?? '*'}***@${domain}`;
      return `${user.slice(0, 2)}***${user.slice(-1)}@${domain}`;
    }
    const digits = value.replace(/\s+/g, '');
    if (digits.length > 6) {
      return `${digits.slice(0, 4)} ••• ${digits.slice(-3)}`;
    }
    return value;
  }

  private loadSession(): StaffSession | null {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      if (raw) return JSON.parse(raw);
    } catch {}
    return null;
  }

  private loadChallenge(): OtpChallenge | null {
    try {
      const raw = sessionStorage.getItem(CHALLENGE_KEY);
      if (raw) return JSON.parse(raw);
    } catch {}
    return null;
  }
}
