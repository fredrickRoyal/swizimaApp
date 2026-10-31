import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import {
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

  /** Step 1: verify username/password; the API sends an OTP and returns a challenge. */
  login(request: LoginRequest): Observable<OtpChallengeResponse> {
    return this.http
      .post<OtpChallengeResponse>('/api/v1/auth/login', request)
      .pipe(tap((challenge) => this.storeChallenge(challenge, request.username)));
  }

  /** Step 2: verify the OTP; on success the user is signed in. */
  verifyOtp(code: string): Observable<LoginResponse> {
    const challenge = this.requireChallenge();
    return this.http
      .post<LoginResponse>('/api/v1/auth/verify-otp', { otpReference: challenge.otpReference, code })
      .pipe(
        tap((session) => {
          this._session.set(session);
          write(SESSION_KEY, session);
          this.clearChallenge();
        }),
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
