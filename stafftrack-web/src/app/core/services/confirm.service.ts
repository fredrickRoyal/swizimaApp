import { Injectable, signal } from '@angular/core';

export interface ConfirmPrompt {
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel: string;
  onConfirm: () => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly activePrompt = signal<ConfirmPrompt | null>(null);

  ask(options: {
    title?: string;
    message: string;
    confirmLabel?: string;
    cancelLabel?: string;
    onConfirm: () => void;
  }): void {
    this.activePrompt.set({
      title: options.title || 'Confirm Delete',
      message: options.message,
      confirmLabel: options.confirmLabel || 'Delete',
      cancelLabel: options.cancelLabel || 'Cancel',
      onConfirm: options.onConfirm,
    });
  }

  confirm(): void {
    const prompt = this.activePrompt();
    this.activePrompt.set(null);
    if (prompt) {
      prompt.onConfirm();
    }
  }

  cancel(): void {
    this.activePrompt.set(null);
  }
}

