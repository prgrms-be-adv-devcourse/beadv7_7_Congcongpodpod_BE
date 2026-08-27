export type PresignedUploadRequest = {
  url: string;
  uri: string;
  contentType: string;
  requiredHeaders?: Record<string, string>;
  blob?: Blob;
};

export async function uploadPresignedFile({ url, uri, contentType, requiredHeaders, blob }: PresignedUploadRequest) {
  const body = blob ?? await fetch(uri).then((response) => {
    if (!response.ok) throw new Error('상품 이미지를 준비하지 못했어요.');
    return response.blob();
  });
  const response = await fetch(url, {
    method: 'PUT',
    headers: { 'Content-Type': contentType, ...requiredHeaders },
    body,
  });

  return { ok: response.ok, status: response.status };
}
