import { Injectable, signal } from '@angular/core';

export type NotificationSeverity = 'info' | 'success' | 'error';

export interface Notification {
  id: number;
  severity: NotificationSeverity;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly _notifications = signal<Notification[]>([]);
  readonly notifications = this._notifications.asReadonly();
  private nextId = 1;

  push(severity: NotificationSeverity, message: string, autoDismissMs = 5000): void {
    const notification: Notification = { id: this.nextId++, severity, message };
    this._notifications.update((list) => [...list, notification]);
    if (autoDismissMs > 0) {
      setTimeout(() => this.dismiss(notification.id), autoDismissMs);
    }
  }

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message, 8000);
  }

  dismiss(id: number): void {
    this._notifications.update((list) => list.filter((n) => n.id !== id));
  }
}
