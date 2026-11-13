import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-page auth-page--login">
      <a class="auth-page-brand" routerLink="/login" aria-label="Projects Manager">Projects Manager</a>
      <section class="auth-panel" aria-labelledby="login-title">
        <div class="auth-form-panel">
          <h1 id="login-title">Connexion</h1>
          <p class="auth-intro">Connectez-vous à votre espace de gestion de projets.</p>

          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="auth-field">
              <label for="login-email">Adresse e-mail</label>
              <div class="auth-input-wrap">
                <input id="login-email" type="email" formControlName="email" autocomplete="email" placeholder="ex: admin@psm.com" />
                <i class="ni ni-email-83 auth-input-icon" aria-hidden="true"></i>
              </div>
            </div>

            <div class="auth-field">
              <label for="login-password">Mot de passe</label>
              <div class="auth-input-wrap">
                <input id="login-password" type="password" formControlName="password" autocomplete="current-password" placeholder="Votre mot de passe" />
                <i class="ni ni-lock-circle-open auth-input-icon" aria-hidden="true"></i>
              </div>
            </div>

            <button class="auth-submit" type="submit" [disabled]="loading || form.invalid">
              {{ loading ? 'Connexion...' : 'Se connecter' }}
            </button>
          </form>

          <p class="auth-switch">
            Pas encore inscrit ?
            <a routerLink="/register">Créer un compte</a>
          </p>
        </div>

        <aside class="auth-welcome-panel">
          <div>
            <p class="auth-eyebrow">PROJECTS MANAGER</p>
            <h2>Ravi de vous revoir !</h2>
            <p>Retrouvez vos clients et vos projets dans un espace simple et sécurisé.</p>
          </div>
        </aside>
      </section>
    </main>
  `,
})
export class LoginComponent {
  loading = false;
  form: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue() as { email: string | null; password: string | null };
    const payload = {
      email: values.email ?? '',
      password: values.password ?? '',
    };

    this.loading = true;

    this.authService.login(payload).subscribe({
      next: (response) => {
        this.authService.setToken(response.token);
        this.router.navigate(['/dashboard']);
      },
      error: () => this.loading = false,
      complete: () => {
        this.loading = false;
      },
    });
  }
}
