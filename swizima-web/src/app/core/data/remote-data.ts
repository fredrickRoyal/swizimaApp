import { DestroyRef, Signal, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Observable, catchError, debounceTime, map, of, startWith, switchMap } from 'rxjs';
import { toErrorMessage } from '../http/api-error';

export interface RemoteState<T> {
  loading: boolean;
  data: T | null;
  error: string | null;
}

/**
 * Re-fetches whenever `params` changes (latest request wins) and exposes
 * loading / data / error as a signal. Must be called in an injection context
 * (e.g. a component field initializer).
 */
export function remoteData<P, T>(params: Signal<P>, fetch: (params: P) => Observable<T>) {
  const destroyRef = inject(DestroyRef);
  const reloadTick = signal(0);
  const state = signal<RemoteState<T>>({ loading: true, data: null, error: null });

  toObservable(computed(() => ({ params: params(), tick: reloadTick() })))
    .pipe(
      switchMap(({ params: p }) =>
        fetch(p).pipe(
          map((data): RemoteState<T> => ({ loading: false, data, error: null })),
          catchError((err) => of<RemoteState<T>>({ loading: false, data: null, error: toErrorMessage(err) })),
          // keep the previous data visible while the next page loads
          startWith<RemoteState<T>>({ ...state(), loading: true, error: null }),
        ),
      ),
      takeUntilDestroyed(destroyRef),
    )
    .subscribe((next) => state.set(next));

  return {
    state: state.asReadonly(),
    reload: () => reloadTick.update((v) => v + 1),
  };
}

/** A read-only copy of `source` that only updates after `ms` of inactivity (for search boxes). */
export function debounced<T>(source: Signal<T>, ms = 300): Signal<T> {
  return toSignal(toObservable(source).pipe(debounceTime(ms)), { initialValue: source() });
}
