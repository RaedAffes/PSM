import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  Client,
  InvoiceFileModel,
  InvoiceModel,
  InvoiceRequest,
  InvoiceStats,
  InvoiceStatus,
  ProjectModel,
} from '../../models';
import { AuthService } from '../../services/auth.service';
import { ClientService } from '../../services/client.service';
import { ErrorNotificationService } from '../../services/error-notification.service';
import { InvoiceService } from '../../services/invoice.service';
import { ProjectService } from '../../services/project.service';

type StatusFilter = 'ALL' | 'UNPAID' | 'PAID' | 'OVERDUE';

@Component({
  selector: 'app-invoices',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="container-fluid py-4">
      <div class="row">
        <div class="col-xl-3 col-md-6 mb-4">
          <div class="card card-stats mb-0">
            <div class="card-body">
              <div class="row">
                <div class="col">
                  <h5 class="card-title text-uppercase text-muted mb-0">Total facturé</h5>
                  <span class="h3 font-weight-bold mb-0">{{ stats.totalInvoiced | currency: 'EUR' }}</span>
                </div>
                <div class="col-auto">
                  <div class="icon icon-shape bg-primary text-white rounded-circle shadow">
                    <i class="ni ni-notes"></i>
                  </div>
                </div>
              </div>
              <p class="mt-3 mb-0 text-muted text-sm">{{ invoices.length }} facture(s) affichée(s)</p>
            </div>
          </div>
        </div>
        <div class="col-xl-3 col-md-6 mb-4">
          <div class="card card-stats mb-0">
            <div class="card-body">
              <div class="row">
                <div class="col">
                  <h5 class="card-title text-uppercase text-muted mb-0">Encaissé</h5>
                  <span class="h3 font-weight-bold mb-0 text-success">{{ stats.totalPaid | currency: 'EUR' }}</span>
                </div>
                <div class="col-auto">
                  <div class="icon icon-shape bg-success text-white rounded-circle shadow">
                    <i class="ni ni-check-bold"></i>
                  </div>
                </div>
              </div>
              <p class="mt-3 mb-0 text-muted text-sm">{{ stats.paidCount }} facture(s) payée(s)</p>
            </div>
          </div>
        </div>
        <div class="col-xl-3 col-md-6 mb-4">
          <div class="card card-stats mb-0">
            <div class="card-body">
              <div class="row">
                <div class="col">
                  <h5 class="card-title text-uppercase text-muted mb-0">Impayé</h5>
                  <span class="h3 font-weight-bold mb-0 text-warning">{{ stats.totalUnpaid | currency: 'EUR' }}</span>
                </div>
                <div class="col-auto">
                  <div class="icon icon-shape bg-warning text-white rounded-circle shadow">
                    <i class="ni ni-time-alarm"></i>
                  </div>
                </div>
              </div>
              <p class="mt-3 mb-0 text-muted text-sm">{{ stats.unpaidCount }} facture(s) impayée(s)</p>
            </div>
          </div>
        </div>
        <div class="col-xl-3 col-md-6 mb-4">
          <div class="card card-stats mb-0">
            <div class="card-body">
              <div class="row">
                <div class="col">
                  <h5 class="card-title text-uppercase text-muted mb-0">En retard</h5>
                  <span class="h3 font-weight-bold mb-0 text-danger">{{ stats.overdueCount }}</span>
                </div>
                <div class="col-auto">
                  <div class="icon icon-shape bg-danger text-white rounded-circle shadow">
                    <i class="ni ni-button-pause"></i>
                  </div>
                </div>
              </div>
              <p class="mt-3 mb-0 text-muted text-sm">Échéance dépassée et non payée</p>
            </div>
          </div>
        </div>
      </div>

      <div class="row">
        <div class="col-xl-4 col-lg-5 mb-4" *ngIf="isAdmin">
          <div class="card shadow">
            <div class="card-header border-0">
              <h2 class="h3 mb-0">{{ editingId ? 'Modifier la facture' : 'Nouvelle facture' }}</h2>
            </div>
            <div class="card-body">
              <form [formGroup]="form" (ngSubmit)="submit()">
                <div class="form-group">
                  <label class="form-control-label" for="invoice-numero">Numéro</label>
                  <input
                    id="invoice-numero"
                    class="form-control"
                    formControlName="numero"
                    placeholder="Auto (FAC-2026-0001)"
                  />
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="invoice-client">Client</label>
                  <select id="invoice-client" class="form-control" formControlName="clientId">
                    <option [ngValue]="null" disabled>Choisir un client</option>
                    <option *ngFor="let client of clients" [ngValue]="client.id">{{ client.nom }}</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="invoice-project">Projet (optionnel)</label>
                  <select id="invoice-project" class="form-control" formControlName="projectId">
                    <option [ngValue]="null">Aucun projet</option>
                    <option *ngFor="let project of filteredProjects" [ngValue]="project.id">
                      {{ project.libelle }}
                    </option>
                  </select>
                </div>
                <div class="row">
                  <div class="col-sm-6">
                    <div class="form-group">
                      <label class="form-control-label" for="invoice-issue">Date d'émission</label>
                      <input id="invoice-issue" class="form-control" type="date" formControlName="issueDate" />
                    </div>
                  </div>
                  <div class="col-sm-6">
                    <div class="form-group">
                      <label class="form-control-label" for="invoice-due">Échéance</label>
                      <input id="invoice-due" class="form-control" type="date" formControlName="dueDate" />
                    </div>
                  </div>
                </div>
                <div class="row">
                  <div class="col-sm-6">
                    <div class="form-group">
                      <label class="form-control-label" for="invoice-amount">Montant HT</label>
                      <input
                        id="invoice-amount"
                        class="form-control"
                        type="number"
                        min="0"
                        step="0.01"
                        formControlName="amount"
                      />
                    </div>
                  </div>
                  <div class="col-sm-6">
                    <div class="form-group">
                      <label class="form-control-label" for="invoice-tax">TVA (%)</label>
                      <input
                        id="invoice-tax"
                        class="form-control"
                        type="number"
                        min="0"
                        step="0.01"
                        formControlName="taxRate"
                      />
                    </div>
                  </div>
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="invoice-method">Mode de paiement</label>
                  <select id="invoice-method" class="form-control" formControlName="paymentMethod">
                    <option [ngValue]="null">Non défini</option>
                    <option value="Virement">Virement</option>
                    <option value="Chèque">Chèque</option>
                    <option value="Espèces">Espèces</option>
                    <option value="Carte bancaire">Carte bancaire</option>
                    <option value="Prélèvement">Prélèvement</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-control-label" for="invoice-notes">Notes</label>
                  <textarea id="invoice-notes" class="form-control" rows="2" formControlName="notes"></textarea>
                </div>

                <div class="alert alert-light border mb-3 py-2">
                  <div class="d-flex justify-content-between">
                    <span class="text-muted text-sm">Total TTC</span>
                    <strong>{{ previewTotal | currency: 'EUR' }}</strong>
                  </div>
                </div>

                <button type="submit" class="btn btn-primary" [disabled]="form.invalid">
                  {{ editingId ? 'Enregistrer' : 'Créer la facture' }}
                </button>
                <button type="button" class="btn btn-link text-muted" *ngIf="editingId" (click)="resetForm()">
                  Annuler
                </button>
              </form>
            </div>
          </div>
        </div>

        <div class="mb-4" [ngClass]="isAdmin ? 'col-xl-8 col-lg-7' : 'col-xl-12'">
          <div class="card shadow">
            <div class="card-header border-0 pb-0">
              <div class="row align-items-center">
                <div class="col">
                  <h2 class="h3 mb-0 d-flex align-items-center">
                    <img class="psm-facture-icon mr-2" src="/facture.png" alt="" aria-hidden="true" />
                    Factures
                  </h2>
                </div>
              </div>
            </div>
            <form class="invoice-filters" [formGroup]="filterForm">
              <div class="invoice-filter">
                <label class="invoice-filter-label" for="invoice-filter-status">
                  <i class="ni ni-bullet-list-67 mr-1"></i>Statut
                </label>
                <select
                  id="invoice-filter-status"
                  class="form-control form-control-sm"
                  formControlName="status"
                  aria-label="Filtrer par statut"
                >
                  <option value="ALL">Tous les statuts</option>
                  <option value="UNPAID">Impayées</option>
                  <option value="PAID">Payées</option>
                  <option value="OVERDUE">En retard</option>
                </select>
              </div>
              <div class="invoice-filter">
                <label class="invoice-filter-label" for="invoice-filter-client">
                  <i class="ni ni-single-02 mr-1"></i>Client
                </label>
                <select
                  id="invoice-filter-client"
                  class="form-control form-control-sm"
                  formControlName="clientId"
                  aria-label="Filtrer par client"
                >
                  <option [ngValue]="null">Tous les clients</option>
                  <option *ngFor="let client of clients" [ngValue]="client.id">{{ client.nom }}</option>
                </select>
              </div>
            </form>
            <div class="table-responsive">
              <table class="table align-items-center table-flush">
                <thead class="thead-light">
                  <tr>
                    <th scope="col">N°</th>
                    <th scope="col">Client</th>
                    <th scope="col">Projet</th>
                    <th scope="col">Émission</th>
                    <th scope="col">Échéance</th>
                    <th scope="col">Montant TTC</th>
                    <th scope="col">Statut</th>
                    <th scope="col" *ngIf="isAdmin">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <ng-container *ngFor="let invoice of filteredInvoices">
                    <tr>
                      <th scope="row">
                        <span class="d-block font-weight-bold">{{ invoice.numero }}</span>
                        <small class="text-muted" *ngIf="invoice.paidDate">
                          Payée le {{ invoice.paidDate | date: 'dd/MM/yyyy' }}
                        </small>
                        <button
                          type="button"
                          class="btn btn-sm btn-link p-0 invoice-files-toggle"
                          [attr.aria-expanded]="expandedInvoiceId === invoice.id"
                          [attr.aria-label]="'Afficher ou ajouter des pièces jointes à la facture ' + invoice.numero"
                          (click)="toggleFiles(invoice)"
                        >
                          <i class="ni ni-paperclip mr-1"></i>
                          Pièces jointes · voir / ajouter<span *ngIf="filesFor(invoice.id).length"> ({{ filesFor(invoice.id).length }})</span>
                        </button>
                      </th>
                      <td>{{ invoice.clientName ?? '—' }}</td>
                      <td>{{ invoice.projectLibelle ?? '—' }}</td>
                      <td>{{ invoice.issueDate | date: 'dd/MM/yyyy' }}</td>
                      <td>{{ invoice.dueDate ? (invoice.dueDate | date: 'dd/MM/yyyy') : '—' }}</td>
                      <td class="font-weight-bold">{{ invoice.totalAmount | currency: 'EUR' }}</td>
                      <td>
                        <span
                          class="badge badge-pill"
                          [ngClass]="invoice.status === 'PAID' ? 'badge-success' : invoice.overdue ? 'badge-danger' : 'badge-warning'"
                        >
                          {{ invoice.status === 'PAID' ? 'Payée' : invoice.overdue ? 'En retard' : 'Impayée' }}
                        </span>
                      </td>
                      <td *ngIf="isAdmin">
                        <div class="d-flex flex-wrap">
                          <button type="button" class="btn btn-sm btn-outline-primary mr-2 mb-1" (click)="edit(invoice)">
                            Modifier
                          </button>
                          <button
                            *ngIf="invoice.status === 'UNPAID'"
                            type="button"
                            class="btn btn-sm btn-outline-success mr-2 mb-1"
                            (click)="markPaid(invoice)"
                          >
                            Marquer payée
                          </button>
                          <button
                            *ngIf="invoice.status === 'PAID'"
                            type="button"
                            class="btn btn-sm btn-outline-warning mr-2 mb-1"
                            (click)="markUnpaid(invoice)"
                          >
                            Marquer impayée
                          </button>
                          <button type="button" class="btn btn-sm btn-outline-danger mb-1" (click)="remove(invoice)">
                            Supprimer
                          </button>
                        </div>
                      </td>
                    </tr>

                    <tr *ngIf="expandedInvoiceId === invoice.id" class="invoice-files-row">
                      <td [attr.colspan]="isAdmin ? 8 : 7">
                        <div class="d-flex flex-wrap justify-content-between align-items-center mb-2">
                          <h4 class="h6 mb-0 d-flex align-items-center">
                            <img class="psm-facture-icon mr-2" src="/facture.png" alt="" aria-hidden="true" />
                            Fichiers de la facture {{ invoice.numero }}
                          </h4>
                          <button type="button" class="btn btn-sm btn-link text-muted" (click)="collapseFiles()">
                            Fermer
                          </button>
                        </div>

                        <div *ngIf="canUploadFiles" class="d-flex flex-wrap align-items-center mb-3">
                          <label class="btn btn-sm btn-outline-primary mb-0 mr-2" [class.disabled]="uploading">
                            <i class="ni ni-cloud-upload-96 mr-1"></i>
                            {{ uploading ? 'Envoi…' : 'Ajouter un fichier' }}
                            <input type="file" class="d-none" [disabled]="uploading" (change)="uploadFile(invoice, $event)" />
                          </label>
                          <span class="text-muted text-sm">PDF, image, Word, Excel, ZIP… — 10 Mo max.</span>
                        </div>

                        <p *ngIf="loadingFiles" class="text-muted text-sm mb-2">Chargement des fichiers…</p>

                        <ul *ngIf="!loadingFiles" class="list-unstyled mb-0">
                          <li
                            *ngFor="let file of filesFor(invoice.id)"
                            class="d-flex flex-wrap justify-content-between align-items-center border-top py-2"
                          >
                            <span class="text-truncate mr-3 invoice-file-name">
                              <i class="ni ni-paperclip text-muted mr-1"></i>
                              <strong>{{ file.fileName }}</strong>
                              <small class="text-muted ml-2">{{ formatSize(file.size) }}</small>
                            </span>
                            <span class="d-flex">
                              <button
                                type="button"
                                class="btn btn-sm btn-outline-secondary mr-2"
                                (click)="downloadFile(invoice, file)"
                              >
                                Télécharger
                              </button>
                              <button
                                *ngIf="isAdmin"
                                type="button"
                                class="btn btn-sm btn-outline-danger"
                                (click)="deleteFile(invoice, file)"
                              >
                                Supprimer
                              </button>
                            </span>
                          </li>
                          <li *ngIf="filesFor(invoice.id).length === 0" class="text-muted text-sm border-top py-2">
                            Aucun fichier pour cette facture.
                          </li>
                        </ul>
                      </td>
                    </tr>
                  </ng-container>

                  <tr *ngIf="filteredInvoices.length === 0">
                    <td [attr.colspan]="isAdmin ? 8 : 7" class="text-center text-muted py-4">
                      Aucune facture trouvée.
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [
    '.psm-facture-icon { width: 1.9rem; height: 1.9rem; flex: 0 0 1.9rem; }',
    '.invoice-filters { display: flex; flex-wrap: wrap; gap: 1rem 1.5rem; margin: 1rem 0 1.25rem; padding: 0.9rem 1rem; border: 1px solid #e7e5e4; border-radius: 0.6rem; background: #f8f7f5; }',
    '.invoice-filter { display: flex; min-width: 14rem; flex: 1 1 14rem; flex-direction: column; gap: 0.35rem; }',
    '.invoice-filter-label { margin: 0; color: #6b7280; font-size: 0.68rem; font-weight: 700; letter-spacing: 0.08em; text-transform: uppercase; }',
    '.invoice-files-toggle { line-height: 1.2; }',
    '.invoice-files-row > td { background: #f8f7f5; }',
    '.invoice-file-name { max-width: 28rem; }',
  ],
})
export class InvoicesComponent implements OnInit {
  readonly isAdmin: boolean;
  readonly canUploadFiles: boolean;

  readonly form: FormGroup<{
    numero: FormControl<string | null>;
    clientId: FormControl<number | null>;
    projectId: FormControl<number | null>;
    issueDate: FormControl<string | null>;
    dueDate: FormControl<string | null>;
    amount: FormControl<number | null>;
    taxRate: FormControl<number | null>;
    paymentMethod: FormControl<string | null>;
    notes: FormControl<string | null>;
  }>;

  readonly filterForm: FormGroup<{
    status: FormControl<StatusFilter | null>;
    clientId: FormControl<number | null>;
  }>;

  clients: Client[] = [];
  projects: ProjectModel[] = [];
  invoices: InvoiceModel[] = [];
  stats: InvoiceStats = {
    totalInvoiced: 0,
    totalPaid: 0,
    totalUnpaid: 0,
    paidCount: 0,
    unpaidCount: 0,
    overdueCount: 0,
  };
  editingId: number | null = null;
  expandedInvoiceId: number | null = null;
  loadingFiles = false;
  uploading = false;

  private readonly filesCache = new Map<number, InvoiceFileModel[]>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly invoiceService: InvoiceService,
    private readonly clientService: ClientService,
    private readonly projectService: ProjectService,
    authService: AuthService,
    private readonly errors: ErrorNotificationService,
    private readonly changeDetector: ChangeDetectorRef,
  ) {
    this.isAdmin = authService.isAdmin();
    this.canUploadFiles = authService.getRoles().some((role) => role === 'ADMIN' || role === 'COLLABORATEUR');
    this.form = this.fb.group({
      numero: this.fb.control<string | null>(''),
      clientId: this.fb.control<number | null>(null, Validators.required),
      projectId: this.fb.control<number | null>(null),
      issueDate: this.fb.control<string | null>(this.today(), Validators.required),
      dueDate: this.fb.control<string | null>(null),
      amount: this.fb.control<number | null>(0, [Validators.required, Validators.min(0)]),
      taxRate: this.fb.control<number | null>(0, [Validators.required, Validators.min(0)]),
      paymentMethod: this.fb.control<string | null>(null),
      notes: this.fb.control<string | null>(''),
    });
    this.filterForm = this.fb.group({
      status: this.fb.control<StatusFilter | null>('ALL'),
      clientId: this.fb.control<number | null>(null),
    });

    this.filterForm.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.loadInvoices());
    this.form.controls.clientId.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.form.controls.projectId.setValue(null));
  }

  ngOnInit(): void {
    this.loadClients();
    this.loadProjects();
    this.loadInvoices();
    this.loadStats();
  }

  get filteredProjects(): ProjectModel[] {
    const clientId = this.form.controls.clientId.value;
    return clientId ? this.projects.filter((project) => project.client?.id === clientId) : this.projects;
  }

  get filteredInvoices(): InvoiceModel[] {
    return this.invoices;
  }

  get previewTotal(): number {
    const amount = Number(this.form.controls.amount.value ?? 0);
    const taxRate = Number(this.form.controls.taxRate.value ?? 0);
    return Math.round(amount * (1 + taxRate / 100) * 100) / 100;
  }

  loadClients(): void {
    this.clientService.getAll().subscribe((clients) => {
      this.clients = clients;
      this.changeDetector.markForCheck();
    });
  }

  loadProjects(): void {
    this.projectService.getAll().subscribe((projects) => {
      this.projects = projects;
      this.changeDetector.markForCheck();
    });
  }

  loadInvoices(): void {
    const status = this.filterForm.controls.status.value ?? 'ALL';
    const clientId = this.filterForm.controls.clientId.value ?? undefined;

    const request =
      status === 'OVERDUE'
        ? this.invoiceService.getAll({ status: 'UNPAID' as InvoiceStatus, clientId })
        : this.invoiceService.getAll({
            status: status === 'ALL' ? undefined : (status as InvoiceStatus),
            clientId,
          });

    request.subscribe((invoices) => {
      this.invoices = status === 'OVERDUE' ? invoices.filter((invoice) => invoice.overdue) : invoices;
      this.errors.dismiss();
      this.changeDetector.markForCheck();
    });
  }

  loadStats(): void {
    this.invoiceService.getStats().subscribe((stats) => {
      this.stats = stats;
      this.changeDetector.markForCheck();
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    if (!raw.clientId) {
      return;
    }

    const payload: InvoiceRequest = {
      numero: raw.numero?.trim() ? raw.numero.trim() : null,
      clientId: raw.clientId,
      projectId: raw.projectId,
      issueDate: raw.issueDate ?? this.today(),
      dueDate: raw.dueDate,
      amount: Number(raw.amount ?? 0),
      taxRate: Number(raw.taxRate ?? 0),
      paymentMethod: raw.paymentMethod,
      notes: raw.notes,
    };

    const request = this.editingId
      ? this.invoiceService.update(this.editingId, payload)
      : this.invoiceService.create(payload);

    request.subscribe(() => {
      this.loadInvoices();
      this.loadStats();
      this.resetForm();
      this.changeDetector.markForCheck();
    });
  }

  edit(invoice: InvoiceModel): void {
    this.editingId = invoice.id;
    this.form.patchValue({
      numero: invoice.numero,
      clientId: invoice.clientId,
      projectId: invoice.projectId,
      issueDate: invoice.issueDate,
      dueDate: invoice.dueDate,
      amount: invoice.amount,
      taxRate: invoice.taxRate,
      paymentMethod: invoice.paymentMethod,
      notes: invoice.notes,
    });
    this.changeDetector.markForCheck();
  }

  markPaid(invoice: InvoiceModel): void {
    if (!window.confirm(`Marquer la facture ${invoice.numero} comme payée ?`)) {
      return;
    }
    const method = window.prompt(
      'Mode de paiement (Virement, Chèque, Espèces, Carte bancaire, Prélèvement)',
      invoice.paymentMethod ?? 'Virement',
    );
    if (method === null) {
      return;
    }
    this.invoiceService.markPaid(invoice.id, method.trim() || undefined).subscribe(() => {
      this.loadInvoices();
      this.loadStats();
      this.changeDetector.markForCheck();
    });
  }

  markUnpaid(invoice: InvoiceModel): void {
    if (!window.confirm(`Repasser la facture ${invoice.numero} en impayée ?`)) {
      return;
    }
    this.invoiceService.markUnpaid(invoice.id).subscribe(() => {
      this.loadInvoices();
      this.loadStats();
      this.changeDetector.markForCheck();
    });
  }

  remove(invoice: InvoiceModel): void {
    if (!window.confirm(`Supprimer la facture ${invoice.numero} ?`)) {
      return;
    }
    this.invoiceService.delete(invoice.id).subscribe(() => {
      this.filesCache.delete(invoice.id);
      if (this.expandedInvoiceId === invoice.id) {
        this.expandedInvoiceId = null;
      }
      this.loadInvoices();
      this.loadStats();
      if (this.editingId === invoice.id) {
        this.resetForm();
      }
      this.changeDetector.markForCheck();
    });
  }

  toggleFiles(invoice: InvoiceModel): void {
    if (this.expandedInvoiceId === invoice.id) {
      this.expandedInvoiceId = null;
      this.changeDetector.markForCheck();
      return;
    }
    this.expandedInvoiceId = invoice.id;
    this.loadFiles(invoice.id);
    this.changeDetector.markForCheck();
  }

  collapseFiles(): void {
    this.expandedInvoiceId = null;
    this.changeDetector.markForCheck();
  }

  filesFor(invoiceId: number): InvoiceFileModel[] {
    return this.filesCache.get(invoiceId) ?? [];
  }

  loadFiles(invoiceId: number): void {
    this.loadingFiles = true;
    this.invoiceService.getFiles(invoiceId).subscribe({
      next: (files) => {
        this.filesCache.set(invoiceId, files);
        this.loadingFiles = false;
        this.changeDetector.markForCheck();
      },
      error: () => {
        this.loadingFiles = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  uploadFile(invoice: InvoiceModel, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0);
    if (!file) {
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.errors.show('Le fichier dépasse la taille maximale de 10 Mo.');
      input.value = '';
      return;
    }

    this.uploading = true;
    this.invoiceService.uploadFile(invoice.id, file).subscribe({
      next: () => {
        input.value = '';
        this.uploading = false;
        this.loadFiles(invoice.id);
      },
      error: () => {
        input.value = '';
        this.uploading = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  downloadFile(invoice: InvoiceModel, file: InvoiceFileModel): void {
    this.invoiceService.downloadFile(invoice.id, file.id).subscribe((blob) => {
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = file.fileName;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    });
  }

  deleteFile(invoice: InvoiceModel, file: InvoiceFileModel): void {
    if (!window.confirm(`Supprimer le fichier ${file.fileName} ?`)) {
      return;
    }
    this.invoiceService.deleteFile(invoice.id, file.id).subscribe(() => {
      this.loadFiles(invoice.id);
      this.changeDetector.markForCheck();
    });
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} o`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} Ko`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  resetForm(): void {
    this.editingId = null;
    this.form.reset({
      numero: '',
      clientId: null,
      projectId: null,
      issueDate: this.today(),
      dueDate: null,
      amount: 0,
      taxRate: 0,
      paymentMethod: null,
      notes: '',
    });
    this.changeDetector.markForCheck();
  }

  private today(): string {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    return `${now.getFullYear()}-${month}-${day}`;
  }
}
