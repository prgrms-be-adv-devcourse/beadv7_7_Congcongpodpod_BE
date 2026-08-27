import {
  FileSystemSessionType,
  FileSystemUploadType,
  uploadAsync,
} from 'expo-file-system/legacy';

export type PresignedUploadRequest = {
  url: string;
  uri: string;
  contentType: string;
  requiredHeaders?: Record<string, string>;
};

function buildUploadHeaders(contentType: string, requiredHeaders?: Record<string, string>) {
  const headers = Object.fromEntries(
    Object.entries(requiredHeaders ?? {}).map(([name, value]) => [name.toLowerCase(), value]),
  );
  headers['content-type'] ??= contentType;
  return headers;
}

export async function uploadPresignedFile({ url, uri, contentType, requiredHeaders }: PresignedUploadRequest) {
  const response = await uploadAsync(url, uri, {
    httpMethod: 'PUT',
    uploadType: FileSystemUploadType.BINARY_CONTENT,
    sessionType: FileSystemSessionType.FOREGROUND,
    // Header names are case-insensitive, but native uploaders may serialize duplicate
    // `Content-Type`/`content-type` entries differently and invalidate the S3 signature.
    headers: buildUploadHeaders(contentType, requiredHeaders),
  });

  return {
    ok: response.status >= 200 && response.status < 300,
    status: response.status,
    body: response.body,
  };
}
