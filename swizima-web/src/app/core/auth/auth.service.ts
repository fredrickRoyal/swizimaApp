import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import {
  AccountCategory,
  ClientUser,
  LoginRequest,
  LoginResponse,
  OtpChallengeResponse,
  OtpChannel,
} from '../models/api.models';

const SESSION_KEY = 'swz.session';
const CHALLENGE_KEY = 'swz.otpChallenge';

/** OTP challenge plus the client-side deadlines derived from the relative timers. */
export interface PendingChallenge extends OtpChallengeResponse {
  username: string;
  /** epoch ms – computed from expiresInSeconds so it is timezone independent */
  expiresAtMs: number;
  resendAtMs: number;
}

/**
 * Two-step login (password -> OTP) state, backed by signals and sessionStorage
 * (cleared when the browser tab closes).
 *
 * NOTE: usersapi does not issue tokens yet, so the session only records who signed in.
 * When JWTs are added, store the token here and attach it in the API interceptor.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly _session = signal<LoginResponse | null>(read<LoginResponse>(SESSION_KEY));
  private readonly _challenge = signal<PendingChallenge | null>(read<PendingChallenge>(CHALLENGE_KEY));

  readonly session = this._session.asReadonly();
  readonly challenge = this._challenge.asReadonly();
  readonly isAuthenticated = computed(() => this._session() !== null);
  readonly displayName = computed(() => this._session()?.username ?? '');

  /** Active client ID when the signed-in user belongs to a client (Client Portal user). */
  readonly clientId = computed(
    () => this._session()?.defaultClientId ?? this._session()?.clientIds?.[0] ?? null,
  );

  /** Account category of the signed-in user: SUPER_ADMIN, CLIENT_ADMIN, or USER. */
  readonly accountCategory = computed<AccountCategory>(() => {
    const s = this._session();
    if (!s) return 'USER';
    if (isDefaultSuperAdmin(s.username, s.email) || s.role === 'SUPER_ADMIN' || s.role === 'ADMIN' || s.accountCategory === 'SUPER_ADMIN') {
      return 'SUPER_ADMIN';
    }
    if (s.accountCategory === 'CLIENT_ADMIN' || s.role === 'CLIENT_ADMIN' || this.clientId()) {
      return 'CLIENT_ADMIN';
    }
    return 'USER';
  });

  /** True when the user is a platform administrator (SUPER_ADMIN / ADMIN, or no client link). */
  readonly isAdmin = computed(() => {
    const s = this._session();
    if (!s) return false;
    if (isDefaultSuperAdmin(s.username, s.email) || s.role === 'SUPER_ADMIN' || s.role === 'ADMIN' || s.accountCategory === 'SUPER_ADMIN') {
      return true;
    }
    if (s.accountCategory === 'CLIENT_ADMIN' || s.role === 'CLIENT_ADMIN') {
      return false;
    }
    return !this.clientId();
  });

  /** True when the signed-in user is a client admin/user accessing the Client Portal module. */
  readonly isClientPortalUser = computed(() => this.isAuthenticated() && !this.isAdmin() && !!this.clientId());

  /** Default landing path after sign-in. */
  defaultHomeUrl(): string {
    return this.isClientPortalUser() ? '/portal/dashboard' : '/dashboard';
  }

  /** Step 1: verify username/password; the API sends an OTP and returns a challenge. */
  login(request: LoginRequest): Observable<OtpChallengeResponse> {
    return this.http
      .post<OtpChallengeResponse>('/api/v1/auth/login', request)
      .pipe(tap((challenge) => this.storeChallenge(challenge, request.username)));
  }

  /** Passwordless alternative to step 1: just email/phone; the API sends an OTP. */
  loginWithOtp(request: {
    username: string;
    otpChannel?: OtpChannel;
    accountCategory?: AccountCategory;
  }): Observable<OtpChallengeResponse> {
    return this.http
      .post<OtpChallengeResponse>('/api/v1/auth/login/otp', request)
      .pipe(tap((challenge) => this.storeChallenge(challenge, request.username)));
  }

  /** Step 2: verify the OTP; on success the user is signed in (resolving client links if needed). */
  verifyOtp(code: string): Observable<LoginResponse> {
    const challenge = this.requireChallenge();
    return this.http
      .post<LoginResponse>('/api/v1/auth/verify-otp', { otpReference: challenge.otpReference, code })
      .pipe(
        switchMap((session) => this.enrichClientLinks(session)),
        tap((session) => {
          this._session.set(session);
          write(SESSION_KEY, session);
          this.clearChallenge();
        }),
      );
  }

  /**
   * If the backend response did not already include defaultClientId (e.g. older usersapi container),
   * look up the user's client links via /api/v1/users/client-users/by-identity.
   */
  private enrichClientLinks(session: LoginResponse): Observable<LoginResponse> {
    if (isDefaultSuperAdmin(session.username, session.email) || session.role === 'SUPER_ADMIN' || session.role === 'ADMIN') {
      return of({
        ...session,
        role: 'SUPER_ADMIN',
        accountCategory: 'SUPER_ADMIN',
      });
    }
    if (session.defaultClientId || (session.clientIds && session.clientIds.length > 0)) {
      return of({
        ...session,
        role: session.role === 'USER' || !session.role ? 'CLIENT_ADMIN' : session.role,
        accountCategory: session.accountCategory ?? 'CLIENT_ADMIN',
      });
    }
    const isEmail = session.usernameType === 'EMAIL' || session.username.includes('@');
    const email = session.email ?? (isEmail ? session.username : undefined);
    const phoneNumber = session.phoneNumber ?? (!isEmail ? session.username : undefined);
    if (!email && !phoneNumber) {
      return of(session);
    }
    const params: Record<string, string> = {};
    if (email) params['email'] = email;
    if (phoneNumber) params['phoneNumber'] = phoneNumber;

    return this.http.get<ClientUser[]>('/api/v1/users/client-users/by-identity', { params }).pipe(
      map((links) => {
        if (!links || links.length === 0) return session;
        const clientIds = [...new Set(links.map((l) => l.clientRef))];
        const defaultLink = links.find((l) => l.isDefaultClient) ?? links[0];
        const isStaff = links.every((l) => !!l.staffRef);
        const category: AccountCategory = isStaff ? 'USER' : 'CLIENT_ADMIN';
        return {
          ...session,
          accountCategory: session.accountCategory ?? category,
          role: session.role === 'USER' || !session.role ? category : session.role,
          defaultClientId: defaultLink?.clientRef ?? clientIds[0],
          clientIds,
        };
      }),
      catchError(() => of(session)),
    );
  }

  /** Request a fresh code, optionally on a different channel. */
  resendOtp(channel?: OtpChannel): Observable<OtpChallengeResponse> {
    const challenge = this.requireChallenge();
    return this.http
      .post<OtpChallengeResponse>('/api/v1/auth/resend-otp', { otpReference: challenge.otpReference, channel })
      .pipe(tap((next) => this.storeChallenge(next, challenge.username)));
  }

  clearChallenge(): void {
    this._challenge.set(null);
    sessionStorage.removeItem(CHALLENGE_KEY);
  }

  logout(): void {
    this._session.set(null);
    sessionStorage.removeItem(SESSION_KEY);
    this.clearChallenge();
  }

  private storeChallenge(challenge: OtpChallengeResponse, username: string): void {
    const now = Date.now();
    const pending: PendingChallenge = {
      ...challenge,
      username,
      expiresAtMs: deadline(now, challenge.expiresInSeconds, challenge.expiresAt, 300),
      resendAtMs: deadline(now, challenge.resendAvailableInSeconds, challenge.resendAvailableAt, 60),
    };
    this._challenge.set(pending);
    write(CHALLENGE_KEY, pending);
  }

  private requireChallenge(): PendingChallenge {
    const challenge = this._challenge();
    if (!challenge) {
      throw new Error('No pending OTP challenge');
    }
    return challenge;
  }
}

function isDefaultSuperAdmin(username?: string, email?: string): boolean {
  const u = (username ?? '').trim().toLowerCase();
  const e = (email ?? '').trim().toLowerCase();
  return u.startsWith('kfredrick') || e.startsWith('kfredrick');
}

function read<T>(key: string): T | null {
  try {
    const raw = sessionStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : null;
  } catch {
    return null;
  }
}

function write(key: string, value: unknown): void {
  sessionStorage.setItem(key, JSON.stringify(value));
}

/**
 * Epoch-ms deadline. Prefers the relative seconds from the API (clock/timezone safe),
 * falls back to the absolute local timestamp, then to a default.
 */
function deadline(now: number, seconds: number | undefined, absolute: string | undefined, fallbackSeconds: number): number {
  if (typeof seconds === 'number' && Number.isFinite(seconds)) {
    return now + seconds * 1000;
  }
  const parsed = absolute ? Date.parse(absolute) : NaN;
  return Number.isFinite(parsed) ? parsed : now + fallbackSeconds * 1000;
}
