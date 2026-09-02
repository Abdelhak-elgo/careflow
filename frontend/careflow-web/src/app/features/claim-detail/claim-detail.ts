import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClaimService } from '../../core/api/claim.service';
import { ClaimResponse } from '../../core/models/claim.models';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-claim-detail',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe, RouterLink],
  templateUrl: './claim-detail.html',
  styleUrl: './claim-detail.scss',
})
export class ClaimDetail {
  private readonly claims = inject(ClaimService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly editing = signal(false);
  protected readonly claim = signal<ClaimResponse | null>(null);
  protected readonly editable = computed(() => this.claim()?.status === 'PENDING');
  protected readonly claimId = this.route.snapshot.paramMap.get('id') ?? '';

  protected readonly form = this.fb.nonNullable.group({
    patientId: ['', [Validators.required, Validators.maxLength(64)]],
    careDate: ['', [Validators.required]],
  });

  constructor() {
    if (this.claimId) {
      this.load();
    }
  }

  private load(): void {
    this.loading.set(true);
    this.claims.getById(this.claimId).subscribe({
      next: (c) => {
        this.claim.set(c);
        this.form.patchValue({ patientId: c.patientId, careDate: c.careDate });
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  startEdit(): void {
    if (!this.editable()) return;
    this.editing.set(true);
  }

  cancelEdit(): void {
    const c = this.claim();
    if (c) {
      this.form.patchValue({ patientId: c.patientId, careDate: c.careDate });
    }
    this.editing.set(false);
  }

  save(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.claims.update(this.claimId, this.form.getRawValue()).subscribe({
      next: (updated) => {
        this.claim.set(updated);
        this.notifications.success('Demande mise à jour.');
        this.saving.set(false);
        this.editing.set(false);
      },
      error: () => this.saving.set(false),
    });
  }

  statusClasses(status: string): string {
    switch (status) {
      case 'APPROVED': return 'bg-emerald-100 text-emerald-800';
      case 'REJECTED': return 'bg-red-100 text-red-800';
      case 'PENDING': return 'bg-amber-100 text-amber-800';
      default: return 'bg-slate-100 text-slate-800';
    }
  }
}
