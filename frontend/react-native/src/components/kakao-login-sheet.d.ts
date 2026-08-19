export type KakaoLoginSheetProps = { url?: string; onClose: () => void; onNavigate: (url: string) => boolean; onHttpError: () => void };
export declare function KakaoLoginSheet(props: KakaoLoginSheetProps): null | React.JSX.Element;
