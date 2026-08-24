import { Image, type ImageProps } from 'expo-image';
import { useState } from 'react';
import type { ImageSourcePropType } from 'react-native';

const fallback = require('../../assets/images/food/korean-meal.png');

type Props = Omit<ImageProps, 'source' | 'contentFit'> & {
  source: ImageSourcePropType;
  resizeMode?: 'cover' | 'contain' | 'fill' | 'center';
};

export function OptimizedImage({ source, resizeMode = 'cover', onError, ...props }: Props) {
  const [failed, setFailed] = useState(false);
  return <Image
    {...props}
    cachePolicy="memory-disk"
    contentFit={resizeMode === 'center' ? 'contain' : resizeMode}
    recyclingKey={typeof source === 'object' && source && 'uri' in source ? source.uri : undefined}
    source={failed ? fallback : source as ImageProps['source']}
    transition={120}
    onError={(event) => { setFailed(true); onError?.(event); }}
  />;
}
