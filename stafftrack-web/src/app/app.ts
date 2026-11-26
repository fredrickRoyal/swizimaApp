import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ConfirmDialog } from './shared/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ConfirmDialog],
  template: `
    <router-outlet />
    <app-confirm-dialog />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {}
