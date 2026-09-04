import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { AuditService } from '../../core/api/audit.service';
import { AuditLogEntry } from '../../core/models/audit.models';

@Component({
  selector: 'app-admin-audit',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './admin-audit.html',
  styleUrl: './admin-audit.scss',
})
export class AdminAudit {
  private readonly audit = inject(AuditService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(false);
  protected readonly items = signal<AuditLogEntry[]>([]);
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly size = 50;
  protected readonly pageCount = computed(() => Math.max(1, Math.ceil(this.total() / this.size)));

  protected readonly filterForm = this.fb.nonNullable.group({
    entityType: [''],
    entityId: [''],
    actor: [''],
  });

  constructor() {
    this.reload();
  }

  applyFilters(): void {
    this.page.set(0);
    this.reload();
  }

  resetFilters(): void {
    this.filterForm.reset({ entityType: '', entityId: '', actor: '' });
    this.applyFilters();
  }

  next(): void {
    if (this.page() + 1 < this.pageCount()) {
      this.page.update((p) => p + 1);
      this.reload();
    }
  }

  prev(): void {
    if (this.page() > 0) {
      this.page.update((p) => p - 1);
      this.reload();
    }
  }

  actionBadge(action: string): string {
    switch (action) {
      case 'CLAIM_SUBMITTED': return 'bg-blue-100 text-blue-800';
      case 'CLAIM_DECIDED':   return 'bg-purple-100 text-purple-800';
      case 'CLAIM_UPDATED':   return 'bg-amber-100 text-amber-800';
      default: return 'bg-slate-100 text-slate-800';
    }
  }

  prettyDetails(json: string | null): string {
    if (!json) return '—';
    try {
      const obj = JSON.parse(json) as Record<string, unknown>;
      return Object.entries(obj).map(([k, v]) => `${k}: ${String(v)}`).join(' · ');
    } catch {
      return json;
    }
  }

  private reload(): void {
    this.loading.set(true);
    const raw = this.filterForm.getRawValue();
    this.audit
      .list({
        entityType: raw.entityType || undefined,
        entityId: raw.entityId || undefined,
        actor: raw.actor || undefined,
        page: this.page(),
        size: this.size,
      })
      .subscribe({
        next: (p) => {
          this.items.set(p.items);
          this.total.set(p.total);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
