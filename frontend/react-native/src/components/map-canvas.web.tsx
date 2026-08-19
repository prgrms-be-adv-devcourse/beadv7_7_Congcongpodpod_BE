import { useEffect, useRef, useState } from 'react';
import Constants from 'expo-constants';
import { StyleSheet, Text, View } from 'react-native';

import { colors } from '@/constants/theme';
import { getStoreCategoryVisual } from '@/lib/store-category';
import { nextMapZoom, type MapCameraCommand, type MapCameraEventSource, type MapCanvasProps } from './map-canvas.types';

type MapInstance = { getCenter(): { lat(): number; lng(): number }; getZoom(): number; getHeading?: () => number; refresh(): void; setCenter(value: unknown): void; setZoom(value: number, animate?: boolean): void; setHeading?: (value: number) => void; panTo(value: unknown): void };
type MarkerInstance = { setMap(map: MapInstance | null): void; setVisible(visible: boolean): void };
type MapsApi = { Map: new (node: HTMLElement, options: object) => MapInstance; Marker: new (options: object) => MarkerInstance; LatLng: new (lat: number, lng: number) => unknown; Point: new (x: number, y: number) => unknown; Event: { addListener(target: object, name: string, listener: () => void): unknown; removeListener(listener: unknown): void } };

let loader: Promise<MapsApi> | undefined;

function getMapHeading(instance: MapInstance) {
  return typeof instance.getHeading === 'function' ? instance.getHeading() : 0;
}

function loadMaps() {
  const browser = window as typeof window & { naver?: { maps: MapsApi } };
  if (browser.naver?.maps) return Promise.resolve(browser.naver.maps);
  if (loader) return loader;
  const clientId = (process.env.EXPO_PUBLIC_NAVER_MAP_CLIENT_ID ?? Constants.expoConfig?.extra?.naverMapClientId as string | undefined)?.trim();
  if (!clientId) return Promise.reject(new Error('EXPO_PUBLIC_NAVER_MAP_CLIENT_ID 설정이 필요해요.'));
  loader = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId=${encodeURIComponent(clientId)}`;
    script.async = true;
    script.onload = () => browser.naver?.maps ? resolve(browser.naver.maps) : reject(new Error('네이버 지도 SDK 초기화에 실패했어요.'));
    script.onerror = () => reject(new Error('네이버 지도 SDK를 불러오지 못했어요.'));
    document.head.appendChild(script);
  });
  return loader;
}

function escapeHtml(value: string) {
  return value.replace(/[&<>'"]/g, character => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[character] ?? character);
}

function markerIcon(icon: string) {
  const paths: Record<string, string> = {
    restaurant: '<path d="M6 3v6M3.8 3v4.2A1.8 1.8 0 0 0 5.6 9H6m0 0v12M15 3v18m0-18c3 1.6 4.5 4.8 4.5 8.2H15"/>',
    'fast-food': '<path d="M4 10h16M5 10c.4-3.4 2.8-5 7-5s6.6 1.6 7 5M4 14h16M6 14l1 5h10l1-5"/>',
    fish: '<path d="M4 12c3.2-4 7.2-5.5 12-2l4-3v10l-4-3c-4.8 3.5-8.8 2-12-2Zm5 0h.01"/>',
    pizza: '<path d="m5 20 4-16c5.2.8 8.8 3.2 11 7L5 20Zm3.1-4.3 9.5-5.5M11 9h.01m2 5h.01"/>',
    cafe: '<path d="M4 7h12v7a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5V7Zm12 2h2a3 3 0 0 1 0 6h-2M7 3v2m4-2v2"/>',
    flame: '<path d="M13 3c1 4-2 5-1 8 1.3-1 2.2-2.3 2-4 3 2.4 5 5.1 5 8a7 7 0 0 1-14 0c0-3 1.7-5.5 5-8-.2 2.5.7 4 2 5 .3-3 3-5 1-9Z"/>',
    wine: '<path d="M6 3h12l-1 6a5 5 0 0 1-10 0L6 3Zm6 11v7m-4 0h8"/>',
    nutrition: '<path d="M5 6h14l-1 15H6L5 6Zm3-3h8M9 11h6m-3-3v6"/>',
  };
  return `<svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${paths[icon] ?? paths.restaurant}</svg>`;
}

export function MapCanvas({ stores, center, userLocation, cameraCommand, zoom = 14.5, showUserLocation = true, selectedStoreId, userHeading = 0, onCameraIdle, onSelect }: MapCanvasProps) {
  const container = useRef<HTMLElement | null>(null);
  const map = useRef<MapInstance | undefined>(undefined);
  const markers = useRef<MarkerInstance[]>([]);
  const listeners = useRef<unknown[]>([]);
  const userMarker = useRef<MarkerInstance | undefined>(undefined);
  const mapListeners = useRef<unknown[]>([]);
  const initialCenter = useRef(center);
  const initialZoom = useRef(zoom);
  const onCameraIdleRef = useRef(onCameraIdle);
  const onSelectRef = useRef(onSelect);
  const handledCommandId = useRef<number | undefined>(undefined);
  const commandRunnerRef = useRef<(command: MapCameraCommand) => void>(() => undefined);
  const [mapReady, setMapReady] = useState(false);
  const [error, setError] = useState<string>();

  onCameraIdleRef.current = onCameraIdle;
  onSelectRef.current = onSelect;

  const syncMarkerVisibility = (currentZoom: number) => {
    const visible = currentZoom >= 13.5;
    markers.current.forEach(marker => marker.setVisible(visible));
  };

  commandRunnerRef.current = (command: MapCameraCommand) => {
    if (!map.current || handledCommandId.current === command.id) return;
    handledCommandId.current = command.id;
    void loadMaps().then(maps => {
      const instance = map.current;
      if (!instance) return;
      if (command.type === 'zoomIn' || command.type === 'zoomOut') {
        const nextZoom = nextMapZoom(Number(instance.getZoom()), command.type, 10, 20);
        instance.setZoom(nextZoom);
      } else if (command.type === 'heading') {
        instance.setHeading?.(command.bearing);
      } else if (command.type === 'focus') {
        instance.setCenter(new maps.LatLng(command.latitude, command.longitude));
        instance.setZoom(Math.round(command.zoom ?? 16));
      } else {
        const target = userLocation ?? center;
        instance.setCenter(new maps.LatLng(target.latitude, target.longitude));
        instance.setZoom(15);
      }
      instance.refresh();
      window.setTimeout(() => {
        const current = map.current;
        if (!current) return;
        const position = current.getCenter();
        syncMarkerVisibility(current.getZoom());
        onCameraIdleRef.current?.({ latitude: position.lat(), longitude: position.lng(), zoom: current.getZoom(), bearing: getMapHeading(current) });
      }, 80);
    });
  };

  useEffect(() => {
    let active = true;
    let observer: ResizeObserver | undefined;
    let removePointerListeners: (() => void) | undefined;
    let centerChangeTimer: number | undefined;
    let gestureInProgress = false;
    let gestureJustEnded = false;
    void loadMaps().then(async maps => {
      await new Promise<void>(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve())));
      if (!active || !container.current || map.current) return;
      const start = initialCenter.current;
      const instance = new maps.Map(container.current, { center: new maps.LatLng(start.latitude, start.longitude), zoom: initialZoom.current, minZoom: 10, maxZoom: 20, scaleControl: false, draggable: true, pinchZoom: true, scrollWheel: true });
      map.current = instance;
      setMapReady(true);
      observer = new ResizeObserver(() => instance.refresh());
      observer.observe(container.current);
      instance.refresh();
      const reportCamera = (source: MapCameraEventSource = 'idle') => {
        const position = instance.getCenter();
        syncMarkerVisibility(instance.getZoom());
        onCameraIdleRef.current?.({ latitude: position.lat(), longitude: position.lng(), zoom: instance.getZoom(), bearing: getMapHeading(instance) }, source);
      };
      const mapNode = container.current;
      let pointerStart: { x: number; y: number } | undefined;
      let pointerMoved = false;
      const onPointerDown = (event: PointerEvent) => {
        pointerStart = { x: event.clientX, y: event.clientY };
        pointerMoved = false;
      };
      const onPointerMove = (event: PointerEvent) => {
        if (!pointerStart) return;
        pointerMoved ||= Math.hypot(event.clientX - pointerStart.x, event.clientY - pointerStart.y) >= 6;
      };
      const onPointerUp = () => {
        const moved = pointerMoved;
        pointerStart = undefined;
        pointerMoved = false;
        if (!moved) return;
        window.setTimeout(() => active && reportCamera('gesture'), 180);
      };
      mapNode.addEventListener('pointerdown', onPointerDown, { passive: true });
      window.addEventListener('pointermove', onPointerMove, { passive: true });
      window.addEventListener('pointerup', onPointerUp, { passive: true });
      window.addEventListener('pointercancel', onPointerUp, { passive: true });
      removePointerListeners = () => {
        mapNode.removeEventListener('pointerdown', onPointerDown);
        window.removeEventListener('pointermove', onPointerMove);
        window.removeEventListener('pointerup', onPointerUp);
        window.removeEventListener('pointercancel', onPointerUp);
      };
      mapListeners.current = [
        maps.Event.addListener(instance, 'click', () => onSelectRef.current(null)),
        maps.Event.addListener(instance, 'dragstart', () => {
          gestureInProgress = true;
          gestureJustEnded = false;
        }),
        maps.Event.addListener(instance, 'dragend', () => {
          gestureInProgress = false;
          gestureJustEnded = true;
          window.setTimeout(() => {
            if (!active || !gestureJustEnded) return;
            reportCamera('gesture');
            gestureJustEnded = false;
          }, 80);
        }),
        maps.Event.addListener(instance, 'idle', () => {
          reportCamera(gestureInProgress || gestureJustEnded ? 'gesture' : 'idle');
          gestureJustEnded = false;
        }),
        maps.Event.addListener(instance, 'center_changed', () => {
          window.clearTimeout(centerChangeTimer);
          centerChangeTimer = window.setTimeout(() => active && reportCamera('move'), 240);
        }),
        maps.Event.addListener(instance, 'bounds_changed', () => {
          window.clearTimeout(centerChangeTimer);
          centerChangeTimer = window.setTimeout(() => active && reportCamera('move'), 180);
        }),
        maps.Event.addListener(instance, 'zoomend', () => reportCamera('zoom')),
      ];
    }).catch(reason => active && setError(reason instanceof Error ? reason.message : '지도를 불러오지 못했어요.'));
    return () => {
      active = false;
      observer?.disconnect();
      window.clearTimeout(centerChangeTimer);
      removePointerListeners?.();
      void loadMaps().then(maps => mapListeners.current.forEach(listener => maps.Event.removeListener(listener)));
      mapListeners.current = [];
    };
  }, []);

  useEffect(() => {
    const handleCommand = (event: Event) => commandRunnerRef.current((event as CustomEvent<MapCameraCommand>).detail);
    window.addEventListener('lastdish-map-command', handleCommand);
    return () => window.removeEventListener('lastdish-map-command', handleCommand);
  }, []);

  useEffect(() => {
    if (mapReady && cameraCommand) commandRunnerRef.current(cameraCommand);
  }, [cameraCommand, mapReady]);

  useEffect(() => {
    if (!map.current) return;
    void loadMaps().then(maps => {
      const instance = map.current;
      if (!instance) return;
      listeners.current.forEach(listener => maps.Event.removeListener(listener));
      markers.current.forEach(marker => marker.setMap(null));
      listeners.current = [];
      markers.current = stores.map(store => {
        const visual = getStoreCategoryVisual(store.category);
        const selected = store.storeId === selectedStoreId;
        const content = `<div style="display:flex;flex-direction:column;align-items:center;transform:translate(-50%,-100%);cursor:pointer"><div style="width:${selected ? 40 : 34}px;height:${selected ? 40 : 34}px;display:flex;align-items:center;justify-content:center;border-radius:12px;background:${selected ? colors.ink900 : visual.color};border:3px solid white;box-shadow:0 5px 14px rgba(15,20,17,.22)">${markerIcon(visual.icon)}</div><div style="max-width:130px;margin-top:3px;padding:2px 6px;border-radius:6px;background:rgba(255,255,255,.94);font-size:11px;font-weight:800;white-space:nowrap;color:${colors.ink900}">${escapeHtml(store.storeName)}</div></div>`;
        const marker = new maps.Marker({ map: instance, position: new maps.LatLng(store.latitude, store.longitude), icon: { content, anchor: new maps.Point(0, 0) }, zIndex: selected ? 100 : 10 });
        marker.setVisible(instance.getZoom() >= 13.5);
        listeners.current.push(maps.Event.addListener(marker, 'click', () => onSelect(store)));
        return marker;
      });
    });
  }, [mapReady, onSelect, selectedStoreId, stores]);

  useEffect(() => {
    if (!map.current || !showUserLocation || !userLocation) return;
    void loadMaps().then(maps => {
      userMarker.current?.setMap(null);
      const content = `<div style="width:50px;height:50px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:rgba(8,119,255,.17);transform:translate(-50%,-50%) rotate(${userHeading}deg)"><div style="position:absolute;top:4px;width:13px;height:13px;background:#0877ff;clip-path:polygon(50% 0,100% 100%,50% 76%,0 100%)"></div><div style="width:20px;height:20px;border-radius:50%;background:#0877ff;border:3px solid white;box-shadow:0 2px 8px rgba(0,60,160,.3)"></div></div>`;
      userMarker.current = new maps.Marker({ map: map.current, position: new maps.LatLng(userLocation.latitude, userLocation.longitude), icon: { content, anchor: new maps.Point(0, 0) }, zIndex: 200 });
    });
  }, [mapReady, showUserLocation, userHeading, userLocation]);

  return <View style={styles.map}><View ref={node => { container.current = node as unknown as HTMLElement; }} style={[styles.canvas, styles.webInteraction]}/>{error ? <View style={styles.error}><Text style={styles.errorTitle}>지도를 표시하지 못했어요</Text><Text style={styles.errorBody}>{error}</Text></View> : null}</View>;
}

const styles = StyleSheet.create({ map: { flex: 1, minHeight: 420, overflow: 'hidden', backgroundColor: '#E9F0EB' }, canvas: { position: 'absolute', top: 0, right: 0, bottom: 0, left: 0, width: '100%', height: '100%' }, webInteraction: { touchAction: 'none', overscrollBehavior: 'none' } as never, error: { ...StyleSheet.absoluteFillObject, alignItems: 'center', justifyContent: 'center', padding: 24, backgroundColor: '#F3F5F2' }, errorTitle: { color: colors.ink900, fontSize: 16, fontWeight: '800' }, errorBody: { marginTop: 7, color: colors.ink700, fontSize: 12, textAlign: 'center' } });
