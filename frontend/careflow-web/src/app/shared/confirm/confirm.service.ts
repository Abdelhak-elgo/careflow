import { Injectable, signal } from '@angular/core';

export type ConfirmTone = 'neutral' | 'danger';

export interface ConfirmRequest {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: ConfirmTone;
}

interface OpenState extends Required<Omit<ConfirmRequest, 'tone'>> {
  tone: ConfirmTone;
  resolve: (accepted: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly _state = signal<OpenState | null>(null);
  readonly state = this._state.asReadonly();

  ask(request: ConfirmRequest): Promise<boolean> {
    return new Promise((resolve) => {
      this._state.set({
        title: request.title,
        message: request.message,
        confirmLabel: request.confirmLabel ?? 'Confirmer',
        cancelLabel: request.cancelLabel ?? 'Annuler',
        tone: request.tone ?? 'neutral',
        resolve,
      });
    });
  }

  accept(): void {
    const current = this._state();
    if (!current) return;
    this._state.set(null);
    current.resolve(true);
  }

  dismiss(): void {
    const current = this._state();
    if (!current) return;
    this._state.set(null);
    current.resolve(false);
  }
}
