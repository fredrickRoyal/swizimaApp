import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark';

const STORAGE_KEY = 'swz.theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly doc = inject(DOCUMENT);

  private readonly mode = signal<ThemeMode>(this.readInitialTheme());

  readonly theme = this.mode.asReadonly();
  readonly isDark = computed(() => this.mode() === 'dark');

  constructor() {
    this.applyTheme(this.mode(), false);
  }

  toggle(): void {
    this.setTheme(this.mode() === 'dark' ? 'light' : 'dark');
  }

  setTheme(next: ThemeMode): void {
    this.mode.set(next);
    this.applyTheme(next, true);
  }

  private readInitialTheme(): ThemeMode {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved === 'light' || saved === 'dark') {
        return saved;
      }
    } catch {
      // ignore storage errors
    }
    const rootAttr = this.doc.documentElement.getAttribute('data-theme');
    if (rootAttr === 'light' || rootAttr === 'dark') {
      return rootAttr;
    }
    if (typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }
    return 'light';
  }

  private applyTheme(mode: ThemeMode, persist: boolean): void {
    const root = this.doc.documentElement;
    root.setAttribute('data-theme', mode);
    root.style.colorScheme = mode;

    const metaTheme = this.doc.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    if (metaTheme) {
      metaTheme.content = mode === 'dark' ? '#0d1326' : '#f8faff';
    }

    if (persist) {
      try {
        localStorage.setItem(STORAGE_KEY, mode);
      } catch {
        // ignore storage errors
      }
    }
  }
}

