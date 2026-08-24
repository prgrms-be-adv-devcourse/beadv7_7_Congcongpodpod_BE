import type { Store } from '@/types/store';

export type MapCoordinate = { latitude: number; longitude: number };
export type MapBounds = { southWest: MapCoordinate; northEast: MapCoordinate };
export type MapCamera = MapCoordinate & { zoom: number; bearing: number; bounds?: MapBounds };
export type MapCameraEventSource = 'idle' | 'gesture' | 'move' | 'zoom';
export type MapCameraCommand =
  | { id: number; type: 'location' | 'zoomIn' | 'zoomOut' }
  | { id: number; type: 'heading'; bearing: number }
  | { id: number; type: 'focus'; latitude: number; longitude: number; zoom?: number };

export type MapCanvasProps = {
  stores: Store[];
  center: MapCoordinate;
  userLocation?: MapCoordinate;
  cameraCommand?: MapCameraCommand;
  zoom?: number;
  showUserLocation?: boolean;
  selectedStoreId?: number;
  userHeading?: number;
  onCameraIdle?: (camera: MapCamera, source?: MapCameraEventSource) => void;
  onSelect: (store: Store | null) => void;
};

export function nextMapZoom(current: number, direction: 'zoomIn' | 'zoomOut', min: number, max: number) {
  return Math.max(min, Math.min(max, Math.round(current) + (direction === 'zoomIn' ? 1 : -1)));
}
