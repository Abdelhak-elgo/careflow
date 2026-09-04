import { Injectable, computed, inject, signal } from '@angular/core';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';

export const authConfig: AuthConfig = {
  issuer: 'http://localhost:8080/realms/careflow',
  clientId: 'careflow-frontend',
  redirectUri: window.location.origin + '/',
  postLogoutRedirectUri: window.location.origin + '/',
  responseType: 'code',
  scope: 'openid profile email',
  requireHttps: false,
  showDebugInformation: false,
};

interface JwtClaims {
  preferred_username?: string;
  email?: string;
  realm_access?: { roles?: string[] };
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oauth = inject(OAuthService);
  private readonly _tick = signal(0);

  readonly isLoggedIn = computed(() => {
    this._tick();
    return this.oauth.hasValidAccessToken();
  });

  readonly claims = computed<JwtClaims>(() => {
    this._tick();
    return (this.oauth.getIdentityClaims() ?? {}) as JwtClaims;
  });

  private readonly accessTokenClaims = computed<JwtClaims>(() => {
    this._tick();
    return decodeJwtPayload(this.oauth.getAccessToken());
  });

  readonly userName = computed(
    () =>
      this.claims().preferred_username ??
      this.accessTokenClaims().preferred_username ??
      'anonymous',
  );
  // Keycloak emits realm_access.roles on the access token by default, not the ID token.
  readonly roles = computed<readonly string[]>(
    () => this.accessTokenClaims().realm_access?.roles ?? [],
  );
  readonly isAdmin = computed(() => this.roles().includes('admin'));

  async bootstrap(): Promise<void> {
    this.oauth.configure(authConfig);
    this.oauth.setupAutomaticSilentRefresh();
    await this.oauth.loadDiscoveryDocumentAndTryLogin();
    this._tick.set(this._tick() + 1);

    this.oauth.events.subscribe(() => this._tick.set(this._tick() + 1));
  }

  login(): void {
    this.oauth.initCodeFlow();
  }

  logout(): void {
    this.oauth.logOut();
  }

  accessToken(): string | null {
    return this.oauth.getAccessToken() || null;
  }
}

function decodeJwtPayload(token: string | null | undefined): JwtClaims {
  if (!token) {
    return {};
  }
  const parts = token.split('.');
  if (parts.length < 2) {
    return {};
  }
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const json = atob(padded);
    return JSON.parse(json) as JwtClaims;
  } catch {
    return {};
  }
}
