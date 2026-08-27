import { useCallback, useEffect, useRef, useState } from 'react';
import * as Location from 'expo-location';

import { getNearbyStores } from '@/lib/stores';
import type { Store } from '@/types/store';
import type { MapBounds } from '@/components/map-canvas.types';
import { distanceKm, isWithinBounds, radiusForBounds } from '@/lib/map-viewport';

// 위치 권한·GPS·최근 위치가 모두 없을 때 앱과 웹이 공유하는 기준점입니다.
const defaultLocation = { latitude: 37.485026405, longitude: 127.016271761 };
type Coordinate = typeof defaultLocation;

export function useNearbyStores(radiusKm = 5, onlyAvailable = true) {
  const [stores, setStores] = useState<Store[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [location, setLocation] = useState(defaultLocation);
  const [locationResolved, setLocationResolved] = useState(false);
  const [heading, setHeading] = useState(0);
  const [locationGranted, setLocationGranted] = useState(false);
  const locationRef = useRef<Coordinate>(defaultLocation);
  const lastStoreLocationRef = useRef<Coordinate>(defaultLocation);
  const requestIdRef = useRef(0);
  const requestControllerRef = useRef<AbortController | undefined>(undefined);
  const highAccuracyAppliedRef = useRef(false);
  const loadStoresRef = useRef<(target: Coordinate, silent?: boolean, bounds?: MapBounds) => Promise<void>>(async () => undefined);

  const loadStores = useCallback(async (searchLocation: Coordinate, silent = false, bounds?: MapBounds) => {
    const requestId = ++requestIdRef.current;
    requestControllerRef.current?.abort();
    const controller = new AbortController();
    requestControllerRef.current = controller;
    if (!silent) setLoading(true);
    if (!silent) setError(false);
    try {
      const queryRadius = radiusForBounds(searchLocation, bounds, radiusKm);
      const nearby = await getNearbyStores(searchLocation.latitude, searchLocation.longitude, queryRadius, 80, onlyAvailable, controller.signal);
      if (requestId !== requestIdRef.current) return;
      lastStoreLocationRef.current = searchLocation;
      setStores(nearby
        .filter((store) => bounds ? isWithinBounds(store, bounds) : distanceKm(searchLocation, store) <= radiusKm)
        .sort((left, right) => distanceKm(searchLocation, left) - distanceKm(searchLocation, right)));
    } catch {
      if (controller.signal.aborted) return;
      if (!silent && requestId === requestIdRef.current) setError(true);
    } finally {
      if (requestId === requestIdRef.current) setLoading(false);
    }
  }, [onlyAvailable, radiusKm]);

  loadStoresRef.current = loadStores;
  const reload = useCallback(async (target?: Coordinate, silent = false, bounds?: MapBounds) => loadStores(target ?? locationRef.current, silent, bounds), [loadStores]);

  useEffect(() => { void loadStores(locationRef.current); }, [loadStores]);
  useEffect(() => {
    let cancelled = false;
    const updateLocation = (next: Coordinate, refreshStores: boolean) => {
      if (cancelled) return;
      locationRef.current = next;
      setLocation(next);
      setLocationResolved(true);
      if (refreshStores) void loadStoresRef.current(next, true);
    };
    void (async () => {
      const [existingPermission, cached] = await Promise.all([
        Location.getForegroundPermissionsAsync(),
        Location.getLastKnownPositionAsync({ maxAge: 30 * 60 * 1000, requiredAccuracy: 1000 }).catch(() => null),
      ]);
      if (cached) {
        updateLocation({ latitude: cached.coords.latitude, longitude: cached.coords.longitude }, true);
      }
      let granted = existingPermission.granted;
      if (!granted) granted = (await Location.requestForegroundPermissionsAsync()).granted;
      if (cancelled) return;
      setLocationGranted(granted);
      if (!granted) return;
      const balanced = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced }).catch(() => null);
      if (!balanced || cancelled) return;
      const next = { latitude: balanced.coords.latitude, longitude: balanced.coords.longitude };
      updateLocation(next, distanceKm(lastStoreLocationRef.current, next) >= 0.05);
    })();
    return () => { cancelled = true; requestControllerRef.current?.abort(); };
  }, []);

  useEffect(() => {
    if (!locationGranted) return;
    let headingSubscription: Location.LocationSubscription | undefined;
    let positionSubscription: Location.LocationSubscription | undefined;
    void Location.watchHeadingAsync((next) => {
      const value = next.trueHeading >= 0 ? next.trueHeading : next.magHeading;
      setHeading(value >= 0 ? value : 0);
    }).then((next) => { headingSubscription = next; }).catch(() => undefined);
    void Location.watchPositionAsync({
      accuracy: Location.Accuracy.High,
      distanceInterval: 5,
      timeInterval: 3000,
    }, (next) => {
      const coordinate = { latitude: next.coords.latitude, longitude: next.coords.longitude };
      locationRef.current = coordinate;
      setLocation(coordinate);
      setLocationResolved(true);
      if (!highAccuracyAppliedRef.current) {
        highAccuracyAppliedRef.current = true;
        if (distanceKm(lastStoreLocationRef.current, coordinate) >= 0.05) void loadStoresRef.current(coordinate, true);
      }
    }).then((next) => { positionSubscription = next; }).catch(() => undefined);
    return () => {
      headingSubscription?.remove();
      positionSubscription?.remove();
    };
  }, [locationGranted]);

  return { stores, loading, error, reload, location, locationResolved, heading };
}
