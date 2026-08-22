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

type PreparedImage = { uri: string; blob?: Blob };

async function prepareFoodImage(asset: ImagePickerAsset, onPhase?: (phase: FoodAnalysisPhase) => void): Promise<PreparedImage> {
  const attempts = [
    { width: 1024, compress: 0.55 },
    { width: 768, compress: 0.4 },
  ];

  for (const [index, attempt] of attempts.entries()) {
    onPhase?.(index === 0
      ? (asset.fileSize ?? 0) > 1024 * 1024 ? 'compressing' : 'preparing'
      : 'compressingAgain');
    const context = ImageManipulator.manipulate(asset.uri);
    if ((asset.width ?? attempt.width + 1) > attempt.width) {
      // Web 구현은 height: null을 0으로 처리하므로, 속성을 생략해 원본 비율을 유지합니다.
      context.resize({ width: attempt.width });
    }
    const rendered = await context.renderAsync();
    const prepared = await rendered.saveAsync({ compress: attempt.compress, format: SaveFormat.JPEG });

    if (Platform.OS === 'web') {
      const blob = await fetch(prepared.uri).then((response) => response.blob());
      if (blob.size <= MAX_FOOD_IMAGE_BYTES) return { uri: prepared.uri, blob };
    } else if (new File(prepared.uri).size <= MAX_FOOD_IMAGE_BYTES) {
      return { uri: prepared.uri };
    }
  }

  throw new FoodImageTooLargeError();
}

export async function classifyFoodImage(asset: ImagePickerAsset, onPhase?: (phase: FoodAnalysisPhase) => void) {
  const prepared = await prepareFoodImage(asset, onPhase);
  const formData = new FormData();
  if (Platform.OS === 'web') {
    formData.append('image', prepared.blob!, 'dish-analysis.jpg');
  } else {
    formData.append('image', {
      uri: prepared.uri,
      name: 'dish-analysis.jpg',
      type: 'image/jpeg',
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
