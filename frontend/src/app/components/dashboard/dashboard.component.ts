import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { ProjectModel } from '../../models';
import { AuthService } from '../../services/auth.service';
import { ClientService } from '../../services/client.service';
import { InvoiceService } from '../../services/invoice.service';
import { ProjectService } from '../../services/project.service';
import { UserService } from '../../services/user.service';
import { ErrorNotificationService } from '../../services/error-notification.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="header bg-gradient-primary py-4">
      <div class="container-fluid">
        <div class="header-body">
          <div class="row">
            <div class="mb-4" [ngClass]="isAdmin ? 'col-xl-3 col-lg-6' : 'col-xl-4 col-lg-6'">
              <div class="card card-stats mb-0">
                <div class="card-body">
                  <div class="row">
                    <div class="col">
                      <h5 class="card-title text-uppercase text-muted mb-0">Clients</h5>
                      <span class="h2 font-weight-bold mb-0">{{ stats.clients }}</span>
                    </div>
                    <div class="col-auto">
                      <div class="icon icon-shape bg-danger text-white rounded-circle shadow">
                        <i class="ni ni-single-02"></i>
                      </div>
                    </div>
                  </div>
                  <p class="mt-3 mb-0 text-muted text-sm">Clients enregistrés</p>
                </div>
              </div>
            </div>
            <div class="mb-4" [ngClass]="isAdmin ? 'col-xl-3 col-lg-6' : 'col-xl-4 col-lg-6'">
              <div class="card card-stats mb-0">
                <div class="card-body">
                  <div class="row">
                    <div class="col">
                      <h5 class="card-title text-uppercase text-muted mb-0">Projets</h5>
                      <span class="h2 font-weight-bold mb-0">{{ stats.projects }}</span>
                    </div>
                    <div class="col-auto">
                      <div class="icon icon-shape bg-primary text-white rounded-circle shadow">
                        <i class="ni ni-collection"></i>
                      </div>
                    </div>
                  </div>
                  <p class="mt-3 mb-0 text-muted text-sm">Projets suivis</p>
                </div>
              </div>
            </div>
            <div class="mb-4" [ngClass]="isAdmin ? 'col-xl-3 col-lg-6' : 'col-xl-4 col-lg-6'">
              <div class="card card-stats mb-0">
                <div class="card-body">
                  <div class="row">
                    <div class="col">
                      <h5 class="card-title text-uppercase text-muted mb-0">Impayé</h5>
                      <span class="h2 font-weight-bold mb-0 text-warning">{{ stats.unpaidTotal | currency:'EUR' }}</span>
                    </div>
                    <div class="col-auto">
                      <div class="icon icon-shape bg-warning text-white rounded-circle shadow">
                        <i class="ni ni-notes"></i>
                      </div>
                    </div>
                  </div>
                  <p class="mt-3 mb-0 text-muted text-sm">
                    {{ stats.unpaidCount }} facture(s) · {{ stats.overdueCount }} en retard
                  </p>
                </div>
              </div>
            </div>
            <div class="col-xl-3 col-lg-6 mb-4" *ngIf="isAdmin">
              <div class="card card-stats mb-0">
                <div class="card-body">
                  <div class="row">
                    <div class="col">
                      <h5 class="card-title text-uppercase text-muted mb-0">Utilisateurs</h5>
                      <span class="h2 font-weight-bold mb-0">{{ stats.users }}</span>
                    </div>
                    <div class="col-auto">
                      <div class="icon icon-shape bg-primary text-white rounded-circle shadow">
                        <i class="ni ni-circle-08"></i>
                      </div>
                    </div>
                  </div>
                  <p class="mt-3 mb-0 text-muted text-sm">Comptes enregistrés</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="container-fluid pt-4">
      <div class="row">
        <div class="col-xl-12 mb-5">
          <div class="card shadow">
            <div class="card-header border-0">
              <div class="row align-items-center">
                <div class="col">
                  <h3 class="mb-0">Projets récents</h3>
                </div>
                <div class="col text-right">
                  <a routerLink="/projects" class="btn btn-sm btn-primary">Tous les projets</a>
                </div>
              </div>
            </div>
            <div class="table-responsive">
              <table class="table align-items-center table-flush">
                <thead class="thead-light">
                  <tr>
                    <th scope="col">Projet</th>
                    <th scope="col">Client</th>
                    <th scope="col">Budget</th>
                    <th scope="col">Durée</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let project of recentProjects">
                    <th scope="row">{{ project.libelle }}</th>
                    <td>{{ project.client?.nom ?? '—' }}</td>
                    <td>{{ project.budget | currency:'EUR' }}</td>
                    <td>{{ project.duree }} jours</td>
                  </tr>
                  <tr *ngIf="recentProjects.length === 0">
                    <td colspan="4" class="text-center text-muted py-4">Aucun projet trouvé.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class DashboardComponent implements OnInit {
  readonly isAdmin: boolean;
  recentProjects: ProjectModel[] = [];
  stats = {
    clients: 0,
    projects: 0,
    users: 0,
    unpaidTotal: 0,
    unpaidCount: 0,
    overdueCount: 0,
  };

  constructor(
    private readonly clientService: ClientService,
    private readonly projectService: ProjectService,
    private readonly invoiceService: InvoiceService,
    private readonly userService: UserService,
    authService: AuthService,
    private readonly errors: ErrorNotificationService,
    private readonly changeDetector: ChangeDetectorRef,
  ) {
    this.isAdmin = authService.isAdmin();
  }

  ngOnInit(): void {
    forkJoin({
      clients: this.clientService.getAll(),
      projects: this.projectService.getAll(),
      invoices: this.invoiceService.getStats(),
      users: this.isAdmin ? this.userService.getAll() : of([]),
    }).subscribe(({ clients, projects, invoices, users }) => {
      this.stats.clients = clients.length;
      this.stats.projects = projects.length;
      this.stats.users = users.length;
      this.stats.unpaidTotal = invoices.totalUnpaid;
      this.stats.unpaidCount = invoices.unpaidCount;
      this.stats.overdueCount = invoices.overdueCount;
      this.recentProjects = projects.slice(0, 5);
      this.errors.dismiss();
      this.changeDetector.markForCheck();
    });
  }
}
