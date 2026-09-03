import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { ClaimService } from '../../core/api/claim.service';
import { AttachmentService } from '../../core/api/attachment.service';
import { CARE_TYPES, SubmitClaimRequest } from '../../core/models/claim.models';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-submit-claim',
  imports: [ReactiveFormsModule, DecimalPipe],
  templateUrl: './submit-claim.html',
  styleUrl: './submit-claim.scss',
})
export class SubmitClaim {
  private readonly fb = inject(FormBuilder);
  private readonly claims = inject(ClaimService);
  private readonly attachments = inject(AttachmentService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly careTypes = CARE_TYPES;
  protected readonly submitting = signal(false);
  protected readonly queuedFiles = signal<readonly File[]>([]);
  protected readonly dragOver = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    patientId: ['patient-42', [Validators.required, Validators.maxLength(64)]],
    careType: ['DENTAL' as SubmitClaimRequest['careType'], [Validators.required]],
    amount: [0.01, [Validators.required, Validators.min(0.01)]],
    currency: ['EUR', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    careDate: [new Date().toISOString().slice(0, 10), [Validators.required]],
  });

  onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.enqueue(Array.from(input.files));
      input.value = '';
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.enqueue(Array.from(files));
    }
  }

  removeFile(index: number): void {
    this.queuedFiles.update((files) => files.filter((_, i) => i !== index));
  }

  kilobytes(bytes: number): number {
    return bytes / 1024;
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const payload: SubmitClaimRequest = this.form.getRawValue();
    this.claims.submit(payload).subscribe({
      next: (claim) => {
        const files = this.queuedFiles();
        if (files.length === 0) {
          this.notifications.success(
            `Demande ${claim.status} (${claim.decisionReason ?? 'sans raison'})`,
          );
          this.submitting.set(false);
          this.router.navigate(['/claims', claim.id]);
          return;
        }
        this.uploadAll(claim.id, files, claim.status, claim.decisionReason);
      },
      error: () => this.submitting.set(false),
    });
  }

  private uploadAll(
    claimId: string,
    files: readonly File[],
    status: string,
    reason: string | null,
  ): void {
    const uploads = files.map((file) =>
      this.attachments.upload(claimId, file).pipe(
        map(() => ({ file, ok: true as const })),
        catchError(() => of({ file, ok: false as const })),
      ),
    );
    forkJoin(uploads).subscribe((results) => {
      const failed = results.filter((r) => !r.ok);
      if (failed.length === 0) {
        this.notifications.success(
          `Demande ${status} — ${files.length} pièce(s) jointe(s) uploadée(s).`,
        );
      } else {
        this.notifications.error(
          `Demande créée (${status}) mais ${failed.length}/${files.length} upload(s) ont échoué : ${failed
            .map((r) => r.file.name)
            .join(', ')}`,
        );
      }
      this.submitting.set(false);
      this.queuedFiles.set([]);
      this.router.navigate(['/claims', claimId]);
    });
  }

  private enqueue(files: File[]): void {
    this.queuedFiles.update((current) => [...current, ...files]);
  }
}
