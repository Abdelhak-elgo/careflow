import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
    title: 'CareFlow — Demandes',
  },
  {
    path: 'submit',
    loadComponent: () => import('./features/submit-claim/submit-claim').then((m) => m.SubmitClaim),
    title: 'CareFlow — Nouvelle demande',
  },
  {
    path: 'review',
    loadComponent: () => import('./features/review/review').then((m) => m.Review),
    title: 'CareFlow — Revue admin',
  },
  { path: '**', redirectTo: '' },
];
