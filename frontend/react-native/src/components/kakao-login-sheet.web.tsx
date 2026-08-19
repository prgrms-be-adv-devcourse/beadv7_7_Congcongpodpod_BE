import { useEffect } from 'react';

import type { KakaoLoginSheetProps } from './kakao-login-sheet.native';

export function KakaoLoginSheet({ url }: KakaoLoginSheetProps) {
  useEffect(() => { if (url) window.location.assign(url); }, [url]);
  return null;
}
