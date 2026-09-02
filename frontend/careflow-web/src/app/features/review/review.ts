import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClaimService } from '../../core/api/claim.service';
import { AdminDecision, ClaimResponse } from '../../core/models/claim.models';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-review',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe],
  templateUrl: './review.html',
  styleUrl: './review.scss',
})
export class Review {
  private readonly fb = inject(FormBuilder);
  private readonly claims = inject(ClaimService);
  private readonly notifications = inject(NotificationService);

  protected readonly loading = signal(false);
  protected readonly submitting = signal<string | null>(null);
  protected readonly rows = signal<ClaimResponse[]>([]);
  protected readonly isEmpty = computed(() => !this.loading() && this.rows().length === 0);

  private readonly forms = new Map<string, ReturnType<Review['buildForm']>>();

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.claims.list('PENDING').subscribe({
      next: (list) => {
        this.forms.clear();
        list.forEach((c) => this.forms.set(c.id, this.buildForm()));
        this.rows.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  formFor(id: string) {
    let form = this.forms.get(id);
    if (!form) {
      form = this.buildForm();
      this.forms.set(id, form);
    }
    return form;
  }

  decide(id: string): void {
    const form = this.formFor(id);
    if (form.invalid || this.submitting()) {
      form.markAllAsTouched();
      return;
    }
    this.submitting.set(id);
    const { decision, reason } = form.getRawValue();
    this.claims.decide(id, { decision: decision as AdminDecision, reason }).subscribe({
      next: (updated) => {
        this.notifications.success(`Demande ${updated.id.slice(0, 8)}… → ${updated.status}`);
        this.rows.update((list) => list.filter((c) => c.id !== id));
        this.forms.delete(id);
        this.submitting.set(null);
      },
      error: () => this.submitting.set(null),
    });
  }

  private buildForm() {
    return this.fb.nonNullable.group({
      decision: ['APPROVED' as AdminDecision, [Validators.required]],
      reason: ['', [Validators.required, Validators.maxLength(500)]],
    });
  }
}
