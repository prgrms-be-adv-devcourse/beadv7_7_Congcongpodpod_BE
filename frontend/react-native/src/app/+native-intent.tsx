const APP_SCHEME = 'lastdish:';

export function redirectSystemPath({ path, initial }: { path: string; initial: boolean }) {
  try {
    // Card and easy-payment apps return through the bare merchant scheme.
    // While a payment is active, routing that URL would unmount the Toss widget
    // and resolve it as the home route before the success callback arrives.
    if (!initial && isPaymentAppReturn(path)) return null;
    return path;
  } catch {
    return initial ? '/' : null;
  }
}

function isPaymentAppReturn(path: string) {
  const normalized = path.trim();
  if (!normalized.toLowerCase().startsWith(APP_SCHEME)) return false;

  const url = new URL(normalized);
  return !url.hostname && (!url.pathname || url.pathname === '/');
}
