import {
  NaverMapMarkerOverlay,
  NaverMapView,
  type NaverMapViewRef,
} from '@mj-studio/react-native-naver-map';
import { useEffect, useRef, useState } from 'react';
import { StyleSheet } from 'react-native';

import { getStoreCategoryVisual } from '@/lib/store-category';
import { nextMapZoom, type MapCanvasProps } from './map-canvas.types';

const directionMarker = require('../../assets/images/map-icons/current-location-blue.png');

export function MapCanvas({ stores, center, userLocation = center, cameraCommand, zoom = 14.5, showUserLocation = true, selectedStoreId, userHeading = 0, onCameraIdle, onSelect }: MapCanvasProps) {
  const mapRef = useRef<NaverMapViewRef>(null);
  const camera = useRef({ ...center, zoom, bearing: 0 });
  const handledCommandId = useRef<number | undefined>(undefined);
  const [cameraTarget, setCameraTarget] = useState({ ...center, zoom, bearing: 0 });
  const [mapBearing, setMapBearing] = useState(0);

  useEffect(() => {
    if (!cameraCommand || handledCommandId.current === cameraCommand.id) return;
    handledCommandId.current = cameraCommand.id;
    if (cameraCommand.type === 'location') {
      const target = { ...userLocation, zoom: 15, bearing: camera.current.bearing };
      setCameraTarget(target);
      mapRef.current?.animateCameraTo({ ...userLocation, zoom: 15, duration: 240 });
      return;
    }
    if (cameraCommand.type === 'focus') {
      const targetZoom = cameraCommand.zoom ?? 16;
      setCameraTarget({ latitude: cameraCommand.latitude, longitude: cameraCommand.longitude, zoom: targetZoom, bearing: camera.current.bearing });
      mapRef.current?.animateCameraTo({ latitude: cameraCommand.latitude, longitude: cameraCommand.longitude, zoom: targetZoom, duration: 240 });
      return;
    }
    if (cameraCommand.type === 'heading') {
      setCameraTarget({ latitude: camera.current.latitude, longitude: camera.current.longitude, zoom: camera.current.zoom, bearing: cameraCommand.bearing });
      return;
    }
    const nextZoom = nextMapZoom(camera.current.zoom, cameraCommand.type, 6, 19);
    setCameraTarget({ latitude: camera.current.latitude, longitude: camera.current.longitude, zoom: nextZoom, bearing: camera.current.bearing });
    mapRef.current?.animateCameraTo({ latitude: camera.current.latitude, longitude: camera.current.longitude, zoom: nextZoom, duration: 200 });
  }, [cameraCommand, userLocation]);
  return (
    <NaverMapView
      ref={mapRef}
      style={StyleSheet.absoluteFill}
      camera={cameraTarget}
      animationDuration={240}
      onTapMap={() => onSelect(null)}
      onCameraChanged={({ latitude, longitude, zoom, bearing }) => { camera.current = { latitude, longitude, zoom: zoom ?? camera.current.zoom, bearing: bearing ?? camera.current.bearing }; }}
      onCameraIdle={({ latitude, longitude, zoom, bearing, region }) => {
        const nextBearing = bearing ?? camera.current.bearing;
        const settledCamera = {
          latitude, longitude, zoom: zoom ?? camera.current.zoom, bearing: nextBearing,
          bounds: {
            southWest: { latitude: region.latitude - region.latitudeDelta / 2, longitude: region.longitude - region.longitudeDelta / 2 },
            northEast: { latitude: region.latitude + region.latitudeDelta / 2, longitude: region.longitude + region.longitudeDelta / 2 },
          },
        };
        camera.current = settledCamera;
        setCameraTarget(settledCamera);
        setMapBearing(nextBearing);
        onCameraIdle?.(settledCamera);
      }}
      isShowCompass={false}
      isShowLocationButton={false}
      isShowScaleBar={false}
      isShowZoomControls={false}
    >
      {stores.map((store) => {
        const selected = store.storeId === selectedStoreId;
        const category = getStoreCategoryVisual(store.category);
        const size = selected ? 40 : 34;
        return <NaverMapMarkerOverlay
          key={store.storeId}
          latitude={store.latitude}
          longitude={store.longitude}
          width={size}
          height={size}
          image={category.markerImage}
          anchor={{ x: 0.5, y: 0.96 }}
          zIndex={selected ? 100 : 10}
          minZoom={13.5}
          caption={{
            text: store.storeName,
            align: 'Bottom',
            offset: 4,
            textSize: selected ? 12 : 11,
            color: '#171A18',
            haloColor: 'rgba(255,255,255,0.96)',
            requestedWidth: 112,
            minZoom: selected ? 13.5 : 14.5,
            maxZoom: 19,
          }}
          isHideCollidedMarkers={false}
          isHideCollidedCaptions={!selected}
          isForceShowIcon={selected}
          onTap={() => onSelect(store)}
        />;
      })}
      {showUserLocation && <NaverMapMarkerOverlay
        latitude={userLocation.latitude}
        longitude={userLocation.longitude}
        width={46}
        height={46}
        image={directionMarker}
        anchor={{ x: 0.5, y: 0.5 }}
        zIndex={200}
        angle={(userHeading - mapBearing + 360) % 360}
      />}
    </NaverMapView>
  );
}
