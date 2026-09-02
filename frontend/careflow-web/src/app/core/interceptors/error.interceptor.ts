import { HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ProblemDetail } from '../models/claim.models';
import { NotificationService } from '../notifications/notification.service';

export const errorInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const notifications = inject(NotificationService);
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      notifications.error(formatMessage(error));
      return throwError(() => error);
    })
  );
};

function formatMessage(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'Impossible de joindre le serveur. Le backend est-il démarré ?';
  }
  const problem = error.error as ProblemDetail | undefined;
  if (problem?.violations?.length) {
    const details = problem.violations.map((v) => `${v.field}: ${v.message}`).join(', ');
    return `${problem.title ?? 'Erreur'} — ${details}`;
  }
  if (problem?.detail) {
    return `${problem.title ?? 'Erreur'} — ${problem.detail}`;
  }
  return `Erreur ${error.status}: ${error.statusText}`;
}
