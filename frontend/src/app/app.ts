import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { NavbarComponent } from './components/navbar/navbar.component';
import { AuthService } from './services/auth.service';
import { ErrorNotificationService } from './services/error-notification.service';

@Component({
  imports: [CommonModule, RouterOutlet, NavbarComponent],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  readonly currentYear = new Date().getFullYear();
  pageTitle = 'Tableau de bord';
  isAuthPage = false;
  private readonly pageTitles: Record<string, string> = {
    '/dashboard': 'Tableau de bord',
    '/clients': 'Clients',
    '/projects': 'Projets',
    '/invoices': 'Factures',
    '/profile': 'Mon profil',
    '/users': 'Utilisateurs',
  };

  constructor(
    public readonly authService: AuthService,
    public readonly errors: ErrorNotificationService,
    router: Router,
  ) {
    this.updatePageTitle(router.url);
    router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => this.updatePageTitle(event.urlAfterRedirects));
  }

  private updatePageTitle(url: string): void {
    const path = url.split(/[?#]/, 1)[0].replace(/\/$/, '') || '/';
    this.isAuthPage = path === '/login' || path === '/register';
    this.pageTitle = this.pageTitles[path] ?? 'Projects Manager';
  }
}
