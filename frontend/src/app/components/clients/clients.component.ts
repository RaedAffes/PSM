import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Client } from '../../models';
import { AuthService } from '../../services/auth.service';
import { ClientService } from '../../services/client.service';
import { ErrorNotificationService } from '../../services/error-notification.service';

@Component({
  selector: 'app-clients',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="container-fluid py-4">
      <div class="row">
        <div class="col-xl-4 col-lg-5 mb-4">
          <div class="card shadow">
            <div class="card-header border-0">
              <h2 class="h3 mb-0">{{ editingId ? 'Modifier le client' : 'Nouveau client' }}</h2>
            </div>
            <div class="card-body">
              <form [formGroup]="form" (ngSubmit)="submit()">
                <div class="form-group">
                  <label class="form-control-label" for="client-nom">Nom</label>
                  <input id="client-nom" class="form-control" formControlName="nom" />
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="client-email">Email</label>
                  <input id="client-email" class="form-control" formControlName="email" type="email" />
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="client-tel">Téléphone</label>
                  <input id="client-tel" class="form-control" formControlName="tel" type="tel" />
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="client-adresse">Adresse</label>
                  <input id="client-adresse" class="form-control" formControlName="adresse" />
                </div>
                <button type="submit" class="btn btn-primary" [disabled]="form.invalid">
                  {{ editingId ? 'Enregistrer' : 'Ajouter le client' }}
                </button>
                <button type="button" class="btn btn-link text-muted" *ngIf="editingId" (click)="resetForm()">Annuler</button>
              </form>
            </div>
          </div>
        </div>

        <div class="col-xl-8 col-lg-7 mb-4">
          <div class="card shadow">
            <div class="card-header border-0">
              <h2 class="h3 mb-0">Liste des clients</h2>
            </div>
            <div class="table-responsive">
              <table class="table align-items-center table-flush">
                <thead class="thead-light">
                  <tr>
                    <th scope="col">Nom</th>
                    <th scope="col">Email</th>
                    <th scope="col">Téléphone</th>
                    <th scope="col">Adresse</th>
                    <th scope="col" *ngIf="isAdmin">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let client of clients">
                    <th scope="row">{{ client.nom }}</th>
                    <td>{{ client.email }}</td>
                    <td>{{ client.tel }}</td>
                    <td>{{ client.adresse }}</td>
                    <td *ngIf="isAdmin">
                      <div class="d-flex">
                        <button type="button" class="btn btn-sm btn-outline-primary mr-2" (click)="edit(client)">Modifier</button>
                        <button type="button" class="btn btn-sm btn-outline-danger" (click)="delete(client.id!)">Supprimer</button>
                      </div>
                    </td>
                  </tr>
                  <tr *ngIf="clients.length === 0">
                    <td [attr.colspan]="isAdmin ? 5 : 4" class="text-center text-muted py-4">Aucun client trouvé.</td>
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
export class ClientsComponent implements OnInit {
  clients: Client[] = [];
  editingId: number | null = null;
  readonly isAdmin: boolean;
  form: ReturnType<FormBuilder['group']>;

  constructor(
    private readonly fb: FormBuilder,
    private readonly clientService: ClientService,
    authService: AuthService,
    private readonly errors: ErrorNotificationService,
    private readonly changeDetector: ChangeDetectorRef,
  ) {
    this.isAdmin = authService.isAdmin();
    this.form = this.fb.group({
      nom: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      tel: ['', Validators.required],
      adresse: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadClients();
  }

  loadClients(): void {
    this.clientService.getAll().subscribe((clients) => {
      this.clients = clients;
      this.errors.dismiss();
      this.changeDetector.markForCheck();
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const values = this.form.getRawValue() as {
      nom: string | null;
      email: string | null;
      tel: string | null;
      adresse: string | null;
    };

    const payload: Client = {
      nom: values.nom ?? '',
      email: values.email ?? '',
      tel: values.tel ?? '',
      adresse: values.adresse ?? '',
    };

    if (this.editingId) {
      this.clientService.update(this.editingId, payload).subscribe(() => {
        this.loadClients();
        this.resetForm();
        this.changeDetector.markForCheck();
      });
      return;
    }

    this.clientService.create(payload).subscribe(() => {
      this.loadClients();
      this.resetForm();
      this.changeDetector.markForCheck();
    });
  }

  edit(client: Client): void {
    this.editingId = client.id ?? null;
    this.form.patchValue({
      nom: client.nom,
      email: client.email,
      tel: client.tel,
      adresse: client.adresse,
    });
    this.changeDetector.markForCheck();
  }

  delete(id: number): void {
    this.clientService.delete(id).subscribe(() => {
      this.loadClients();
      this.changeDetector.markForCheck();
    });
  }

  resetForm(): void {
    this.editingId = null;
    this.form.reset();
    this.changeDetector.markForCheck();
  }
}
