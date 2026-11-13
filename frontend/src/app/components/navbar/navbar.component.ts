import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar navbar-vertical navbar-expand-md navbar-dark psm-sidebar fixed-left" aria-label="Navigation principale">
      <div class="container-fluid">
        <div class="sidebar-header">
          <a class="navbar-brand psm-brand" routerLink="/dashboard">
            <img class="psm-brand-icon" src="/icon.png" alt="" aria-hidden="true" loading="eager" fetchpriority="high">
            <span>Projects Manager</span>
          </a>
        </div>
        <div class="navbar-collapse" id="psm-navigation">
          <ul class="navbar-nav primary-nav">
            <li class="nav-item">
              <a class="nav-link" routerLink="/dashboard" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }" title="Tableau de bord">
                <i class="ni ni-tv-2 text-primary"></i>
                <span class="nav-link-text">Tableau de bord</span>
              </a>
            </li>
            <li class="nav-item">
              <a class="nav-link" routerLink="/clients" routerLinkActive="active" title="Clients">
                <i class="ni ni-single-02 text-orange"></i>
                <span class="nav-link-text">Clients</span>
              </a>
            </li>
            <li class="nav-item">
              <a class="nav-link" routerLink="/projects" routerLinkActive="active" title="Projets">
                <i class="ni ni-collection text-green"></i>
                <span class="nav-link-text">Projets</span>
              </a>
            </li>
            <li class="nav-item">
              <a class="nav-link" routerLink="/invoices" routerLinkActive="active" title="Factures">
                <img class="psm-nav-icon" src="/facture.png" alt="" aria-hidden="true">
                <span class="nav-link-text">Factures</span>
              </a>
            </li>
            <li class="nav-item" *ngIf="authService.isAdmin()">
              <a class="nav-link" routerLink="/users" routerLinkActive="active" title="Utilisateurs">
                <i class="ni ni-single-02 text-info"></i>
                <span class="nav-link-text">Utilisateurs</span>
              </a>
            </li>
          </ul>
          <section class="sidebar-account" aria-label="Compte">
            <h6 class="navbar-heading">Compte</h6>
            <ul class="navbar-nav">
              <li class="nav-item">
                <a class="nav-link" routerLink="/profile" routerLinkActive="active" title="Profil">
                  <i class="ni ni-single-02 text-info"></i>
                  <span class="nav-link-text">Profil</span>
                </a>
              </li>
              <li class="nav-item">
                <button type="button" class="nav-link nav-logout" title="Déconnexion" aria-label="Déconnexion" (click)="logout()">
                  <i class="ni ni-user-run text-danger"></i>
                  <span class="nav-link-text">Déconnexion</span>
                </button>
              </li>
            </ul>
          </section>
        </div>
      </div>
    </nav>
  `,
  styles: [
    ':host { display: block; position: fixed; z-index: 1050; inset: 0 auto 0 0; width: 250px; }',
    ':host > nav { display: flex; height: 100vh; align-items: stretch; background: #1c1b1a !important; border-right: 1px solid #49433f; }',
    ':host .container-fluid { display: flex; height: 100%; flex-direction: column; align-items: stretch; padding: 0 0.75rem; }',
    '.sidebar-header { display: flex; min-height: 4.25rem; align-items: center; gap: 0.5rem; padding: 0.5rem 0.35rem; }',
    '.psm-brand { display: flex; min-width: 0; flex: 1; align-items: center; gap: 0.6rem; margin: 0; color: #fff; font-family: Poppins, "Open Sans", sans-serif; font-size: 1.05rem; font-weight: 700; letter-spacing: -0.035em; white-space: nowrap; }',
    '.psm-brand-icon { width: 2.15rem; height: 2.15rem; flex: 0 0 2.15rem; border-radius: 0.55rem; }',
    '.psm-brand:hover { color: #ffe3c2; }',
    '.navbar-collapse { display: flex !important; min-height: 0; flex: 1; flex-direction: column; align-items: stretch; width: 100%; padding: 0.5rem 0 0.75rem; }',
    '.navbar-nav { flex-direction: column; }',
    '.primary-nav { min-height: 0; flex: 1; overflow-y: auto; scrollbar-width: none; }',
    '.primary-nav::-webkit-scrollbar { display: none; }',
    '.navbar-nav .nav-link { display: flex; width: 100%; align-items: center; gap: 0.75rem; margin: 0.15rem 0; padding: 0.7rem 1rem; border-radius: 0.5rem; }',
    '.navbar-nav .nav-link { color: #d3ccc7 !important; }',
    '.navbar-nav .nav-link.active { color: #fff !important; background: #b84c13; }',
    '.navbar-nav .nav-link.active i { color: #fff !important; }',
    '.navbar-nav .nav-link:hover:not(.active) { color: #fff !important; background-color: #292522; }',
    '.navbar-nav .nav-link i { display: inline-flex; width: 1.4rem; flex: 0 0 1.4rem; justify-content: center; margin: 0; font-size: 1.05rem; }',
    '.navbar-nav .nav-link .psm-nav-icon { display: inline-flex; width: 1.4rem; height: 1.4rem; flex: 0 0 1.4rem; }',
    '.sidebar-account { margin-top: auto; padding: 0.85rem 0.35rem 0; border-top: 1px solid #49433f; }',
    '.navbar-heading { margin: 0 0 0.5rem; padding: 0 0.65rem; color: #c9c0bb !important; font-size: 0.72rem; letter-spacing: 0.1em; text-transform: uppercase; }',
    '.nav-logout { width: 100%; border: 0; background: transparent; text-align: left; cursor: pointer; }',
    '.nav-logout:hover { background-color: #292522; }',
    '@media (max-width: 767.98px) { :host { position: relative; width: 100%; height: auto; inset: auto; } :host > nav { height: auto; min-height: 4rem; } :host .container-fluid { height: auto; padding: 0 0.75rem; } .sidebar-header { width: 100%; } .navbar-collapse { flex: 0 0 100%; padding-bottom: 0.75rem; } .sidebar-account { margin-top: 0.75rem; } }',
  ]
})
export class NavbarComponent {
  constructor(public readonly authService: AuthService, private readonly router: Router) {}

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
