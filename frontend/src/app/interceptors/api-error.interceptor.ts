import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { ErrorNotificationService } from '../services/error-notification.service';

@Injectable()
export class ApiErrorInterceptor implements HttpInterceptor {
  constructor(private readonly notifications: ErrorNotificationService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError((error: unknown) => {
        this.notifications.show(this.getMessage(error));
        return throwError(() => error);
      }),
    );
  }

  private getMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'Une erreur inattendue est survenue.';
    }

    const body = error.error;
    if (typeof body === 'string' && body.trim()) {
      try {
        return this.messageFromBody(JSON.parse(body));
      } catch {
        return body;
      }
    }

    const message = this.messageFromBody(body);
    if (message) {
      return message;
    }

    if (error.status === 0) {
      return 'Aucune réponse du serveur. Vérifiez que le backend est démarré et que la connexion est autorisée.';
    }

    return `La requête a échoué (HTTP ${error.status}${error.statusText ? ` : ${error.statusText}` : ''}).`;
  }

  private messageFromBody(body: unknown): string {
    if (!body || typeof body !== 'object') {
      return '';
    }

    const payload = body as Record<string, unknown>;
    for (const key of ['message', 'detail', 'title']) {
      if (typeof payload[key] === 'string' && payload[key].trim()) {
        return payload[key] as string;
      }
    }

    if (Array.isArray(payload['errors'])) {
      return payload['errors']
        .map((item: unknown) => {
          if (typeof item === 'string') {
            return item;
          }
          if (item && typeof item === 'object') {
            const entry = item as Record<string, unknown>;
            return [entry['field'], entry['defaultMessage'] ?? entry['message']]
              .filter((value): value is string => typeof value === 'string')
              .join(' : ');
          }
          return '';
        })
        .filter(Boolean)
        .join('; ');
    }

    return '';
  }
}
