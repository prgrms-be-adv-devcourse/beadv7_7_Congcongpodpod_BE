type Entry<T> = { value: T; expiresAt: number };

const values = new Map<string, Entry<unknown>>();
const inflight = new Map<string, Promise<unknown>>();

export async function cachedQuery<T>(key: string, loader: () => Promise<T>, ttlMs = 10_000, force = false): Promise<T> {
  const cached = values.get(key) as Entry<T> | undefined;
  if (!force && cached && cached.expiresAt > Date.now()) return cached.value;
  const pending = inflight.get(key) as Promise<T> | undefined;
  if (!force && pending) return pending;
  const promise = loader().then((value) => {
    values.set(key, { value, expiresAt: Date.now() + ttlMs });
    return value;
  }).finally(() => {
    if (inflight.get(key) === promise) inflight.delete(key);
  });
  inflight.set(key, promise);
  return promise;
}

export function invalidateQueries(prefix?: string) {
  if (!prefix) {
    values.clear();
    inflight.clear();
    return;
  }
  [...values.keys()].filter((key) => key.startsWith(prefix)).forEach((key) => values.delete(key));
}
