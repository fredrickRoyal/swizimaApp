import { Injectable, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

/**
 * Sets the document title to "<Route title> · Swizima" (good for SEO and browser tabs)
 * and exposes the current route title so the top bar can show it.
 */
@Injectable({ providedIn: 'root' })
export class AppTitleStrategy extends TitleStrategy {
  private readonly title = inject(Title);
  readonly pageTitle = signal('');

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const routeTitle = this.buildTitle(snapshot) ?? '';
    this.pageTitle.set(routeTitle);
    this.title.setTitle(routeTitle ? `${routeTitle} · Swizima` : 'Swizima');
  }
}
