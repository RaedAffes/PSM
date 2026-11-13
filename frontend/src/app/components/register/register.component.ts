import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-page auth-page--register">
      <a class="auth-page-brand" routerLink="/login" aria-label="Projects Manager">Projects Manager</a>
      <section class="auth-panel" aria-labelledby="register-title">
        <div class="auth-form-panel">
          <h1 id="register-title">Créer un compte</h1>
          <p class="auth-intro">Rejoignez votre espace de gestion de projets.</p>

          <form [formGroup]="form" (ngSubmit)="submit()">
            <div class="auth-field-row">
              <div class="auth-field">
                <label for="register-nom">Nom</label>
                <div class="auth-input-wrap">
                  <input id="register-nom" type="text" formControlName="nom" autocomplete="family-name" placeholder="Nom" />
                </div>
              </div>
              <div class="auth-field">
                <label for="register-prenom">Prénom</label>
                <div class="auth-input-wrap">
                  <input id="register-prenom" type="text" formControlName="prenom" autocomplete="given-name" placeholder="Prénom" />
                </div>
              </div>
            </div>

            <div class="auth-field-row">
              <div class="auth-field">
                <label for="register-email">Adresse e-mail</label>
                <div class="auth-input-wrap">
                  <input id="register-email" type="email" formControlName="email" autocomplete="email" placeholder="email@domaine.com" />
                  <i class="ni ni-email-83 auth-input-icon" aria-hidden="true"></i>
                </div>
              </div>
              <div class="auth-field">
                <label for="register-tel">Téléphone</label>
                <div class="auth-input-wrap">
                  <input id="register-tel" type="tel" formControlName="tel" autocomplete="tel" placeholder="Téléphone" />
                  <i class="ni ni-mobile-button auth-input-icon" aria-hidden="true"></i>
                </div>
              </div>
            </div>

            <div class="auth-field">
              <label for="register-password">Mot de passe</label>
              <div class="auth-input-wrap">
                <input id="register-password" type="password" formControlName="password" autocomplete="new-password" placeholder="6 caractères minimum" />
                <i class="ni ni-lock-circle-open auth-input-icon" aria-hidden="true"></i>
              </div>
            </div>

            <button class="auth-submit" type="submit" [disabled]="loading || form.invalid">
              {{ loading ? 'Inscription...' : 'Créer mon compte' }}
            </button>
          </form>

          <p class="auth-switch">
            Déjà inscrit ?
            <a routerLink="/login">Se connecter</a>
          </p>
        </div>

        <aside class="auth-welcome-panel">
          <div>
            <p class="auth-eyebrow">PROJECTS MANAGER</p>
            <h2>Vos projets commencent ici.</h2>
            <p>Créez votre compte et retrouvez votre espace de gestion au même endroit.</p>
          </div>
        </aside>
      </section>
    </main>
  `,
})
export class RegisterComponent {
  loading = false;
  form: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly router: Router,
  ) {
    this.form = this.fb.group({
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      tel: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(6)]],
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue() as {
      nom: string | null;
      prenom: string | null;
      email: string | null;
      tel: string | null;
      password: string | null;
    };

    const payload = {
      nom: values.nom ?? '',
      prenom: values.prenom ?? '',
      email: values.email ?? '',
      tel: values.tel ?? '',
      password: values.password ?? '',
    };

    this.loading = true;

    this.authService.register(payload).subscribe({
      next: () => {
        this.router.navigate(['/login']);
      },
      error: () => this.loading = false,
      complete: () => {
        this.loading = false;
      },
    });
  }
}
