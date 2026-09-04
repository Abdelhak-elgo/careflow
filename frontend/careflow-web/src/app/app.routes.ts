import { Routes } from '@angular/router';
import { adminGuard } from './core/auth/admin.guard';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
    title: 'CareFlow — Accueil',
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
    canActivate: [authGuard],
    title: 'CareFlow — Demandes',
  },
  {
    path: 'submit',
    loadComponent: () => import('./features/submit-claim/submit-claim').then((m) => m.SubmitClaim),
    canActivate: [authGuard],
    title: 'CareFlow — Nouvelle demande',
  },
  {
    path: 'review',
    loadComponent: () => import('./features/review/review').then((m) => m.Review),
    canActivate: [adminGuard],
    title: 'CareFlow — Revue admin',
  },
  {
    path: 'claims/:id',
    loadComponent: () => import('./features/claim-detail/claim-detail').then((m) => m.ClaimDetail),
    canActivate: [authGuard],
    title: 'CareFlow — Détail demande',
  },
  {
    path: 'admin/audit',
    loadComponent: () => import('./features/admin-audit/admin-audit').then((m) => m.AdminAudit),
    canActivate: [adminGuard],
    title: 'CareFlow — Audit',
  },
  {
    path: 'admin/files',
    loadComponent: () => import('./features/admin-files/admin-files').then((m) => m.AdminFiles),
    canActivate: [adminGuard],
    title: 'CareFlow — Fichiers',
  },
  { path: '**', redirectTo: '' },
];
