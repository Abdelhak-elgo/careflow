import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { NotificationService } from '../notifications/notification.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const notifications = inject(NotificationService);

  if (!auth.isLoggedIn()) {
    notifications.error('Vous devez être connecté pour accéder à cette page.');
    auth.login();
    return false;
  }
  if (!auth.isAdmin()) {
    notifications.error('Accès refusé : rôle ADMIN requis.');
    return router.parseUrl('/');
  }
  return true;
};
