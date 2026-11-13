import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { filter, take } from 'rxjs';
import { UserModel } from '../../models';
import { AuthService } from '../../services/auth.service';
import { ErrorNotificationService } from '../../services/error-notification.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="container-fluid py-4">
      <div *ngIf="successMessage" class="alert alert-success" role="status">{{ successMessage }}</div>
      <div class="row">
        <div class="col-xl-8 col-lg-7 mb-4">
          <div class="card shadow">
            <div class="card-header border-0">
              <h2 class="h3 mb-1">Informations personnelles</h2>
              <p class="mb-0 text-muted">Consultez et mettez à jour les informations de votre compte.</p>
            </div>
            <div class="card-body">
              <form [formGroup]="form" (ngSubmit)="saveProfile()">
                <div class="form-row">
                  <div class="form-group col-md-6">
                    <label class="form-control-label" for="profile-prenom">Prénom</label>
                    <input id="profile-prenom" class="form-control" formControlName="prenom" autocomplete="given-name">
                  </div>
                  <div class="form-group col-md-6">
                    <label class="form-control-label" for="profile-nom">Nom</label>
                    <input id="profile-nom" class="form-control" formControlName="nom" autocomplete="family-name">
                  </div>
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="profile-tel">Téléphone</label>
                  <input id="profile-tel" class="form-control" type="tel" formControlName="tel" autocomplete="tel">
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="profile-password">Nouveau mot de passe</label>
                  <input
                    id="profile-password"
                    class="form-control"
                    type="password"
                    formControlName="password"
                    autocomplete="new-password"
                    placeholder="Laissez vide pour conserver le mot de passe actuel">
                  <small class="form-text text-muted">Au moins 6 caractères. Laissez vide si vous ne souhaitez pas le modifier.</small>
                </div>
                <button class="btn btn-primary" type="submit" [disabled]="loading || form.invalid || !profile">
                  {{ loading ? 'Enregistrement...' : 'Enregistrer les modifications' }}
                </button>
              </form>
            </div>
          </div>
        </div>

        <div class="col-xl-4 col-lg-5 mb-4">
          <div class="card shadow account-card" *ngIf="profile as user">
            <div class="card-body text-center">
              <span class="profile-avatar" aria-hidden="true">{{ initials(user) }}</span>
              <h2 class="h3 mt-3 mb-1">{{ user.prenom }} {{ user.nom }}</h2>
              <p class="text-muted mb-4">{{ user.email }}</p>
              <dl class="text-left mb-0">
                <div class="account-detail">
                  <dt>Téléphone</dt>
                  <dd>{{ user.tel || 'Non renseigné' }}</dd>
                </div>
                <div class="account-detail">
                  <dt>Rôle{{ user.roles.length === 1 ? '' : 's' }}</dt>
                  <dd>
                    <span class="badge badge-role" *ngFor="let role of user.roles">{{ role }}</span>
                    <span *ngIf="user.roles.length === 0" class="text-muted">Utilisateur</span>
                  </dd>
                </div>
              </dl>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [
    '.account-card .profile-avatar { display: inline-flex; width: 4.5rem; height: 4.5rem; align-items: center; justify-content: center; border-radius: 50%; background: linear-gradient(135deg, #f0873f, #b84c13); color: #fff; font-size: 1.4rem; font-weight: 700; }',
    '.account-detail { padding: 0.85rem 0; border-top: 1px solid #49433f; }',
    '.account-detail dt { margin-bottom: 0.35rem; color: #c9c0bb; font-size: 0.75rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; }',
    '.account-detail dd { margin: 0; color: #fff; overflow-wrap: anywhere; }',
    '.badge-role { margin: 0 0.35rem 0.25rem 0; padding: 0.35rem 0.55rem; background: #6b351a; color: #ffe3c2; }',
  ],
})
export class ProfileComponent implements OnInit {
  profile: UserModel | null = null;
  loading = false;
  successMessage = '';
  readonly form: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly errors: ErrorNotificationService,
    private readonly changeDetector: ChangeDetectorRef,
  ) {
    this.form = this.fb.group({
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      tel: ['', Validators.required],
      password: ['', Validators.minLength(6)],
    });
  }

  ngOnInit(): void {
    this.authService.profile$
      .pipe(
        filter((profile): profile is UserModel => profile !== null),
        take(1),
      )
      .subscribe((profile) => {
        this.profile = profile;
        this.form.patchValue({
          nom: profile.nom,
          prenom: profile.prenom,
          tel: profile.tel,
        });
        this.changeDetector.markForCheck();
      });
  }

  saveProfile(): void {
    if (this.form.invalid || !this.profile) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue() as {
      nom: string | null;
      prenom: string | null;
      tel: string | null;
      password: string | null;
    };
    const payload: { nom: string; prenom: string; tel: string; password?: string } = {
      nom: values.nom ?? '',
      prenom: values.prenom ?? '',
      tel: values.tel ?? '',
    };
    const password = values.password?.trim();
    if (password) {
      payload.password = password;
    }

    this.loading = true;
    this.successMessage = '';
    this.errors.dismiss();
    this.authService.updateProfile(payload).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.form.patchValue({ nom: profile.nom, prenom: profile.prenom, tel: profile.tel, password: '' });
        this.successMessage = 'Votre profil a été mis à jour.';
      },
      error: () => {
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      complete: () => {
        this.loading = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  initials(user: UserModel): string {
    const value = `${user.prenom?.trim().charAt(0) ?? ''}${user.nom?.trim().charAt(0) ?? ''}`.toUpperCase();
    return value || user.email.trim().charAt(0).toUpperCase();
  }
}
