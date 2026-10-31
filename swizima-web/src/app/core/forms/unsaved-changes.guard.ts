import { CanDeactivateFn } from '@angular/router';

/** Implemented by pages with forms that can hold unsaved input. */
export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

/** Asks before leaving a page whose form has unsaved changes (in-app navigation). */
export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) =>
  !component.hasUnsavedChanges() || window.confirm('You have unsaved changes. Leave this page and discard them?');
