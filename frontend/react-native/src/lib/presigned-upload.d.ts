export type PresignedUploadRequest = {
  url: string;
  uri: string;
  contentType: string;
  requiredHeaders?: Record<string, string>;
  blob?: Blob;
};

export function uploadPresignedFile(
  request: PresignedUploadRequest,
): Promise<{ ok: boolean; status: number }>;
