import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ClaimService } from '../../core/api/claim.service';
import { CARE_TYPES, SubmitClaimRequest } from '../../core/models/claim.models';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-submit-claim',
  imports: [ReactiveFormsModule],
  templateUrl: './submit-claim.html',
  styleUrl: './submit-claim.scss',
})
export class SubmitClaim {
  private readonly fb = inject(FormBuilder);
  private readonly claims = inject(ClaimService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly careTypes = CARE_TYPES;
  protected readonly submitting = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    patientId: ['patient-42', [Validators.required, Validators.maxLength(64)]],
    careType: ['DENTAL' as SubmitClaimRequest['careType'], [Validators.required]],
    amount: [0.01, [Validators.required, Validators.min(0.01)]],
    currency: ['EUR', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    careDate: [new Date().toISOString().slice(0, 10), [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const payload: SubmitClaimRequest = this.form.getRawValue();
    this.claims.submit(payload).subscribe({
      next: (claim) => {
        this.notifications.success(`Demande ${claim.status} (${claim.decisionReason ?? 'sans raison'})`);
        this.submitting.set(false);
        this.router.navigate(['/']);
      },
      error: () => this.submitting.set(false),
    });
  }
}
