export type LoginRequiredRequest = { redirect: string; onCancel?: () => void };

let listener: ((request: LoginRequiredRequest) => void) | undefined;

export function showLoginRequired(redirect: string, onCancel?: () => void) {
  listener?.({ redirect, onCancel });
}

export function subscribeLoginRequired(next: (request: LoginRequiredRequest) => void) {
  listener = next;
  return () => {
    if (listener === next) listener = undefined;
  };
}
