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

export async function uploadPresignedFile({ url, uri, contentType, requiredHeaders }: PresignedUploadRequest) {
  const response = await uploadAsync(url, uri, {
    httpMethod: 'PUT',
    uploadType: FileSystemUploadType.BINARY_CONTENT,
    sessionType: FileSystemSessionType.FOREGROUND,
    headers: { 'Content-Type': contentType, ...requiredHeaders },
  });

  return { ok: response.status >= 200 && response.status < 300, status: response.status };
}
