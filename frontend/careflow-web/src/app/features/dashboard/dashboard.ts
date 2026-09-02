import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ClaimService } from '../../core/api/claim.service';
import { CLAIM_STATUSES, ClaimResponse, ClaimStatus } from '../../core/models/claim.models';

type StatusFilter = ClaimStatus | 'ALL';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DatePipe, DecimalPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly claims = inject(ClaimService);

  protected readonly statuses: readonly StatusFilter[] = ['ALL', ...CLAIM_STATUSES];
  protected readonly activeStatus = signal<StatusFilter>('ALL');
  protected readonly loading = signal(false);
  protected readonly rows = signal<ClaimResponse[]>([]);

  protected readonly isEmpty = computed(() => !this.loading() && this.rows().length === 0);

  constructor() {
    this.load('ALL');
  }

  selectStatus(status: StatusFilter): void {
    this.activeStatus.set(status);
    this.load(status);
  }

  private load(status: StatusFilter): void {
    this.loading.set(true);
    const filter = status === 'ALL' ? undefined : status;
    this.claims.list(filter).subscribe({
      next: (list) => {
        this.rows.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  statusClasses(status: ClaimStatus): string {
    switch (status) {
      case 'APPROVED':
        return 'bg-emerald-100 text-emerald-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      case 'PENDING':
        return 'bg-amber-100 text-amber-800';
    }
  }
}
