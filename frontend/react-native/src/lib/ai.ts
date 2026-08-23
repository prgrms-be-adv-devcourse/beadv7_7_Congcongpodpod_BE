import { ImageManipulator, SaveFormat } from 'expo-image-manipulator';
import type { ImagePickerAsset } from 'expo-image-picker';
import { File } from 'expo-file-system';
import { Platform } from 'react-native';

import { api } from './api';

export type FoodClassification = {
  predictedCategory: string;
  confidence: number;
  executionTimeMs: number;
};

export type FoodAnalysisPhase = 'preparing' | 'compressing' | 'compressingAgain' | 'uploading' | 'analyzing';

// Ingress는 10MB까지 허용합니다. multipart 부가 데이터를 고려해 클라이언트는 9MB에서 차단합니다.
export const MAX_FOOD_IMAGE_BYTES = 9 * 1024 * 1024;
export const FOOD_CLASSIFICATION_TIMEOUT_MS = 5_000;

export class FoodImageTooLargeError extends Error {
  constructor() {
    super('이미지를 압축해도 용량이 너무 커요. 10MB 이하의 다른 사진을 선택해주세요.');
    this.name = 'FoodImageTooLargeError';
  }
}

export type PreparedImage = { uri: string; blob?: Blob; fileSize: number; contentType: string; fileName: string };
const uploadContentTypes = new Set(['image/jpeg', 'image/png', 'image/webp']);

function normalizeContentType(contentType?: string) {
  const normalized = contentType?.toLowerCase();
  if (normalized === 'image/jpg' || normalized === 'image/pjpeg') return 'image/jpeg';
  return normalized || 'image/jpeg';
}

function normalizeFileName(fileName: string | null | undefined, contentType: string) {
  const fallbackExtension = contentType === 'image/png' ? 'png' : contentType === 'image/webp' ? 'webp' : 'jpg';
  const candidate = fileName?.trim() || 'dish-image';
  if (/\.(jpe?g|png|webp)$/i.test(candidate)) return candidate.replace(/\.jpeg$/i, '.jpg');
  return `${candidate}.${fallbackExtension}`;
}

async function readOriginalImage(asset: ImagePickerAsset): Promise<PreparedImage> {
  if (Platform.OS === 'web') {
    const response = await fetch(asset.uri);
    if (!response.ok) throw new Error('선택한 이미지를 열지 못했어요.');
    const blob = await response.blob();
    const contentType = normalizeContentType(asset.mimeType || blob.type);
    return { uri: asset.uri, blob, fileSize: blob.size, contentType, fileName: normalizeFileName(asset.fileName, contentType) };
  }
  const contentType = normalizeContentType(asset.mimeType);
  return { uri: asset.uri, fileSize: asset.fileSize ?? new File(asset.uri).size, contentType, fileName: normalizeFileName(asset.fileName, contentType) };
}

export async function prepareFoodImage(asset: ImagePickerAsset, onPhase?: (phase: FoodAnalysisPhase) => void, _forUpload = false): Promise<PreparedImage> {
  onPhase?.('preparing');
  const original = await readOriginalImage(asset);
  if (original.fileSize > 0 && original.fileSize <= MAX_FOOD_IMAGE_BYTES && uploadContentTypes.has(original.contentType)) return original;

  const attempts = [
    { width: 1024, compress: 0.55 },
    { width: 768, compress: 0.4 },
  ];

  for (const [index, attempt] of attempts.entries()) {
    onPhase?.(index === 0
      ? (asset.fileSize ?? 0) > 1024 * 1024 ? 'compressing' : 'preparing'
      : 'compressingAgain');
    const context = ImageManipulator.manipulate(asset.uri);
    if (Number.isFinite(asset.width) && Number.isFinite(asset.height) && (asset.width ?? 0) > 0 && (asset.height ?? 0) > 0 && asset.width! > attempt.width) {
      // Web 구현은 height: null을 0으로 처리하므로, 속성을 생략해 원본 비율을 유지합니다.
      context.resize({ width: attempt.width });
    }
    const rendered = await context.renderAsync();
    const prepared = await rendered.saveAsync({ compress: attempt.compress, format: SaveFormat.JPEG });

    if (Platform.OS === 'web') {
      const blob = await fetch(prepared.uri).then((response) => response.blob());
      if (blob.size <= MAX_FOOD_IMAGE_BYTES) return { uri: prepared.uri, blob, fileSize: blob.size, contentType: 'image/jpeg', fileName: 'dish-image.jpg' };
    } else {
      const fileSize = new File(prepared.uri).size;
      if (fileSize <= MAX_FOOD_IMAGE_BYTES) return { uri: prepared.uri, fileSize, contentType: 'image/jpeg', fileName: 'dish-image.jpg' };
    }
  }

  throw new FoodImageTooLargeError();
}

export async function classifyFoodImage(asset: ImagePickerAsset, onPhase?: (phase: FoodAnalysisPhase) => void) {
  const prepared = await prepareFoodImage(asset, onPhase);
  const formData = new FormData();
  if (Platform.OS === 'web') {
    formData.append('image', prepared.blob!, prepared.fileName);
  } else {
    formData.append('image', {
      uri: prepared.uri,
      name: prepared.fileName,
      type: prepared.contentType,
    } as unknown as Blob);
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), FOOD_CLASSIFICATION_TIMEOUT_MS);
  const analyzingTimer = setTimeout(() => onPhase?.('analyzing'), 700);
  try {
    onPhase?.('uploading');
    return await api<FoodClassification>(
      '/ai/classify',
      { method: 'POST', body: formData, signal: controller.signal },
      { globalLoading: false, timeoutMs: FOOD_CLASSIFICATION_TIMEOUT_MS },
    );
  } finally {
    clearTimeout(timeout);
    clearTimeout(analyzingTimer);
  }
}
