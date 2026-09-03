import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AttachmentService } from '../../core/api/attachment.service';
import { Attachment } from '../../core/models/attachment.models';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-admin-files',
  imports: [DatePipe, DecimalPipe, RouterLink],
  templateUrl: './admin-files.html',
  styleUrl: './admin-files.scss',
})
export class AdminFiles {
  private readonly attachments = inject(AttachmentService);
  private readonly notifications = inject(NotificationService);

  protected readonly loading = signal(true);
  protected readonly items = signal<Attachment[]>([]);
  protected readonly renamingId = signal<string | null>(null);
  protected readonly renameValue = signal<string>('');
  protected readonly isEmpty = computed(() => !this.loading() && this.items().length === 0);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.attachments.adminListAll().subscribe({
      next: (list) => {
        this.items.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  startRename(a: Attachment): void {
    this.renamingId.set(a.id);
    this.renameValue.set(a.originalName);
  }

  cancelRename(): void {
    this.renamingId.set(null);
    this.renameValue.set('');
  }

  onRenameInput(event: Event): void {
    this.renameValue.set((event.target as HTMLInputElement).value);
  }

  submitRename(id: string): void {
    const name = this.renameValue().trim();
    if (!name) {
      this.notifications.error('Le nom ne peut pas être vide.');
      return;
    }
    this.attachments.rename(id, { originalName: name }).subscribe({
      next: (updated) => {
        this.items.update((list) => list.map((a) => (a.id === id ? updated : a)));
        this.notifications.success('Pièce jointe renommée.');
        this.cancelRename();
      },
    });
  }

  download(a: Attachment): void {
    this.attachments.download(a.id).subscribe({
      next: (blob) => triggerBlobDownload(blob, a.originalName),
    });
  }

  delete(a: Attachment): void {
    if (!confirm(`Supprimer "${a.originalName}" ?`)) return;
    this.attachments.delete(a.id).subscribe({
      next: () => {
        this.items.update((list) => list.filter((x) => x.id !== a.id));
        this.notifications.success('Pièce jointe supprimée.');
      },
    });
  }

  kilobytes(bytes: number): number {
    return bytes / 1024;
  }
}

function triggerBlobDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
