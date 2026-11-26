import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ConfirmService } from '../../core/services/confirm.service';
import { Icon } from '../icon/icon';

@Component({
  selector: 'app-confirm-dialog',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    .confirm-icon-box {
      width: 42px;
      height: 42px;
      border-radius: 12px;
      display: grid;
      place-items: center;
      background: var(--danger-bg);
      color: var(--danger);
      flex: none;
    }
    .confirm-body-row {
      display: flex;
      align-items: flex-start;
      gap: 0.9rem;
    }
    .confirm-message {
      font-size: 0.92rem;
      line-height: 1.5;
      color: var(--text);
    }
    .confirm-hint {
      margin-top: 0.3rem;
      font-size: 0.8rem;
      color: var(--text-muted);
    }
    .btn-danger-solid {
      background: var(--danger);
      border-color: var(--danger);
      color: #ffffff;
    }
    .btn-danger-solid:hover {
      filter: brightness(0.93);
    }
  `,
  template: `
    @if (confirmService.activePrompt(); as prompt) {
      <div class="modal-backdrop" style="z-index: 95">
        <div
          class="modal-card"
          style="width: min(460px, 100%)"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="confirm-dialog-title"
          aria-describedby="confirm-dialog-desc"
        >
          <div class="modal-header">
            <h2 id="confirm-dialog-title" style="font-size: 1.02rem">{{ prompt.title }}</h2>
            <button
              type="button"
              class="btn btn-sm btn-ghost"
              aria-label="Cancel"
              (click)="confirmService.cancel()"
            >
              <app-icon name="close" />
            </button>
          </div>

          <div class="modal-body">
            <div class="confirm-body-row">
              <span class="confirm-icon-box" aria-hidden="true">
                <app-icon name="trash" />
              </span>
              <div>
                <p id="confirm-dialog-desc" class="confirm-message">{{ prompt.message }}</p>
                <p class="confirm-hint">This action cannot be undone.</p>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn" (click)="confirmService.cancel()">
              {{ prompt.cancelLabel }}
            </button>
            <button
              type="button"
              class="btn btn-danger-solid"
              (click)="confirmService.confirm()"
            >
              <app-icon name="trash" />
              <span>{{ prompt.confirmLabel }}</span>
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class ConfirmDialog {
  protected readonly confirmService = inject(ConfirmService);
}

