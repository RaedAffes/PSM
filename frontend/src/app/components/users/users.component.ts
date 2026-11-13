import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { ErrorNotificationService } from '../../services/error-notification.service';
import { UserService, UserWriteRequest } from '../../services/user.service';
import { UserModel } from '../../models';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="container-fluid py-4">
      <div *ngIf="successMessage" class="alert alert-success" role="status">{{ successMessage }}</div>
      <div class="row">
        <div class="col-xl-4 col-lg-5 mb-4">
          <div class="card">
            <div class="card-header border-0">
              <h2 class="h3 mb-0">{{ editingUserId ? 'Modifier l’utilisateur' : 'Nouvel utilisateur' }}</h2>
            </div>
            <div class="card-body">
              <form [formGroup]="userForm" (ngSubmit)="submitUser()">
                <div class="form-group">
                  <label class="form-control-label" for="user-nom">Nom</label>
                  <input id="user-nom" class="form-control" formControlName="nom">
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="user-prenom">Prénom</label>
                  <input id="user-prenom" class="form-control" formControlName="prenom">
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="user-email">E-mail</label>
                  <input id="user-email" class="form-control" type="email" formControlName="email">
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="user-tel">Téléphone</label>
                  <input id="user-tel" class="form-control" type="tel" formControlName="tel">
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="user-password">
                    {{ editingUserId ? 'Nouveau mot de passe (facultatif)' : 'Mot de passe' }}
                  </label>
                  <input id="user-password" class="form-control" type="password" formControlName="password">
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="user-roles">Rôles</label>
                  <select id="user-roles" class="form-control" formControlName="roles" multiple [size]="roles.length">
                    <option *ngFor="let role of roles" [value]="role">{{ role }}</option>
                  </select>
                </div>
                <button class="btn btn-primary" type="submit" [disabled]="loading || userForm.invalid">
                  {{ editingUserId ? 'Enregistrer' : 'Créer l’utilisateur' }}
                </button>
                <button *ngIf="editingUserId" class="btn btn-link text-muted" type="button" (click)="resetUserForm()">
                  Annuler
                </button>
              </form>
            </div>
          </div>
        </div>

        <div class="col-xl-8 col-lg-7 mb-4">
          <div class="card">
            <div class="card-header border-0">
              <h2 class="h3 mb-0">Utilisateurs</h2>
            </div>
            <div class="table-responsive">
              <table class="table align-items-center table-flush">
                <thead class="thead-light">
                  <tr>
                    <th>Nom</th>
                    <th>E-mail</th>
                    <th>Téléphone</th>
                    <th>Rôles</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let user of users">
                    <th>{{ user.prenom }} {{ user.nom }}</th>
                    <td>{{ user.email }}</td>
                    <td>{{ user.tel }}</td>
                    <td>{{ user.roles.join(', ') }}</td>
                    <td>
                      <div class="d-flex">
                        <button class="btn btn-sm btn-outline-primary mr-2" type="button" (click)="editUser(user)">
                          Modifier
                        </button>
                        <button
                          class="btn btn-sm btn-outline-danger"
                          type="button"
                          [disabled]="isCurrentUser(user)"
                          [attr.aria-label]="'Supprimer ' + user.email"
                          (click)="deleteUser(user)">
                          Supprimer
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr *ngIf="users.length === 0">
                    <td colspan="5" class="text-center text-muted py-4">Aucun utilisateur trouvé.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

    </section>
  `,
})
export class UsersComponent implements OnInit {
  users: UserModel[] = [];
  readonly roles = ['ADMIN', 'COLLABORATEUR'];
  editingUserId: number | null = null;
  loading = false;
  successMessage = '';
  readonly currentEmail: string | null;
  userForm: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly userService: UserService,
    private readonly authService: AuthService,
    private readonly errors: ErrorNotificationService,
    private readonly changeDetector: ChangeDetectorRef,
  ) {
    this.currentEmail = authService.getEmail()?.toLowerCase() ?? null;
    this.userForm = this.fb.group({
      nom: ['', Validators.required],
      prenom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      tel: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(6)]],
      roles: [['COLLABORATEUR'], Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.userService.getAll().subscribe((users) => {
      this.users = users;
      this.changeDetector.markForCheck();
    });
  }

  submitUser(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    const raw = this.userForm.getRawValue() as {
      nom: string | null;
      prenom: string | null;
      email: string | null;
      tel: string | null;
      password: string | null;
      roles: string[] | null;
    };
    const payload: UserWriteRequest = {
      nom: raw.nom ?? '',
      prenom: raw.prenom ?? '',
      email: raw.email ?? '',
      tel: raw.tel ?? '',
      password: raw.password ?? '',
      roles: raw.roles ?? [],
    };
    this.loading = true;
    this.errors.dismiss();

    const request = this.editingUserId
      ? this.userService.update(this.editingUserId, payload)
      : this.userService.create(payload);
    request.subscribe({
      next: () => {
        this.successMessage = this.editingUserId ? 'Utilisateur modifié.' : 'Utilisateur créé.';
        this.resetUserForm();
        this.loadData();
        this.changeDetector.markForCheck();
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

  editUser(user: UserModel): void {
    this.editingUserId = user.id;
    this.userForm.controls['password'].setValidators([Validators.minLength(6)]);
    this.userForm.patchValue({
      nom: user.nom,
      prenom: user.prenom,
      email: user.email,
      tel: user.tel,
      password: '',
      roles: user.roles,
    });
    this.userForm.controls['password'].updateValueAndValidity();
    this.successMessage = '';
  }

  deleteUser(user: UserModel): void {
    if (this.isCurrentUser(user) || !window.confirm(`Supprimer le compte ${user.email} ?`)) {
      return;
    }
    this.errors.dismiss();
    this.userService.delete(user.id).subscribe(() => {
      this.successMessage = 'Utilisateur supprimé.';
      this.loadData();
      this.changeDetector.markForCheck();
    });
  }

  resetUserForm(): void {
    this.editingUserId = null;
    this.userForm.reset({ nom: '', prenom: '', email: '', tel: '', password: '', roles: ['COLLABORATEUR'] });
    this.userForm.controls['password'].setValidators([Validators.required, Validators.minLength(6)]);
    this.userForm.controls['password'].updateValueAndValidity();
  }

  isCurrentUser(user: UserModel): boolean {
    return user.email.toLowerCase() === this.currentEmail;
  }
}
