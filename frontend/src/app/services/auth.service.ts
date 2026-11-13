import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { UserModel } from '../models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'psm-token';
  private readonly apiUrl = '/api/auth';
  private readonly profileSubject = new BehaviorSubject<UserModel | null>(null);

  readonly profile$ = this.profileSubject.asObservable();

  constructor(private http: HttpClient) {
    // Defer the initial fetch: the auth interceptor depends on this service, so
    // calling HTTP from the constructor creates a circular DI error (NG0200).
    if (this.isAuthenticated()) {
      setTimeout(() => this.refreshProfile());
    }
  }

  login(payload: { email: string; password: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, payload);
  }

  register(payload: {
    email: string;
    password: string;
    tel: string;
    nom: string;
    prenom: string;
  }): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, payload);
  }

  setToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
    this.refreshProfile();
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.profileSubject.next(null);
  }

  me(): Observable<UserModel> {
    return this.http.get<UserModel>(`${this.apiUrl}/me`);
  }

  updateProfile(payload: { nom: string; prenom: string; tel: string; password?: string }): Observable<UserModel> {
    return this.http.put<UserModel>(`${this.apiUrl}/me`, payload).pipe(
      tap((profile) => this.profileSubject.next(profile)),
    );
  }

  refreshProfile(): void {
    if (!this.isAuthenticated()) {
      this.profileSubject.next(null);
      return;
    }

    this.me().subscribe({
      next: (profile) => this.profileSubject.next(profile),
      error: () => this.profileSubject.next(null),
    });
  }

  getRoles(): string[] {
    const token = this.getToken();
    if (!token) {
      return [];
    }

    try {
      const encodedPayload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(encodedPayload.padEnd(Math.ceil(encodedPayload.length / 4) * 4, '=')));
      return Array.isArray(payload.roles) ? payload.roles.filter((role: unknown): role is string => typeof role === 'string') : [];
    } catch {
      return [];
    }
  }

  getEmail(): string | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    try {
      const encodedPayload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(encodedPayload.padEnd(Math.ceil(encodedPayload.length / 4) * 4, '=')));
      return typeof payload.sub === 'string' ? payload.sub : null;
    } catch {
      return null;
    }
  }

  isAdmin(): boolean {
    return this.getRoles().includes('ADMIN');
  }
}
