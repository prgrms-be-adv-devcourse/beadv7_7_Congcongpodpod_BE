import { useCallback, useEffect, useRef, useState } from 'react';
import * as Location from 'expo-location';

import { getNearbyStores } from '@/lib/stores';
import type { Store } from '@/types/store';

const defaultLocation = { latitude: 37.49972, longitude: 126.92825 };
type Coordinate = typeof defaultLocation;

const distanceKm = (from: Coordinate, to: Coordinate) => {
  const radians = (degrees: number) => degrees * Math.PI / 180;
  const latitudeDelta = radians(to.latitude - from.latitude);
  const longitudeDelta = radians(to.longitude - from.longitude);
  const value = Math.sin(latitudeDelta / 2) ** 2
    + Math.cos(radians(from.latitude)) * Math.cos(radians(to.latitude)) * Math.sin(longitudeDelta / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
};

export function useNearbyStores(radiusKm = 5) {
  const [stores, setStores] = useState<Store[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [location, setLocation] = useState(defaultLocation);
  const [heading, setHeading] = useState(0);
  const [locationGranted, setLocationGranted] = useState(false);
  const locationRef = useRef<Coordinate>(defaultLocation);
  const lastStoreLocationRef = useRef<Coordinate>(defaultLocation);
  const requestIdRef = useRef(0);
  const highAccuracyAppliedRef = useRef(false);
  const loadStoresRef = useRef<(target: Coordinate, silent?: boolean) => Promise<void>>(async () => undefined);

  const loadStores = useCallback(async (searchLocation: Coordinate, silent = false) => {
    const requestId = ++requestIdRef.current;
    if (!silent) setLoading(true);
    if (!silent) setError(false);
    try {
      const nearby = await getNearbyStores(searchLocation.latitude, searchLocation.longitude, radiusKm);
      if (requestId !== requestIdRef.current) return;
      lastStoreLocationRef.current = searchLocation;
      setStores(nearby
        .filter((store) => distanceKm(searchLocation, store) <= radiusKm)
        .sort((left, right) => distanceKm(searchLocation, left) - distanceKm(searchLocation, right)));
    } catch {
      if (!silent && requestId === requestIdRef.current) setError(true);
    } finally {
      if (requestId === requestIdRef.current) setLoading(false);
    }
  }, [radiusKm]);

  loadStoresRef.current = loadStores;
  const reload = useCallback(async (target?: Coordinate, silent = false) => loadStores(target ?? locationRef.current, silent), [loadStores]);

  useEffect(() => { void loadStores(locationRef.current); }, [loadStores]);
  useEffect(() => {
    let cancelled = false;
    const updateLocation = (next: Coordinate, refreshStores: boolean) => {
      if (cancelled) return;
      locationRef.current = next;
      setLocation(next);
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
    return () => { cancelled = true; };
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

  return { stores, loading, error, reload, location, heading };
}
