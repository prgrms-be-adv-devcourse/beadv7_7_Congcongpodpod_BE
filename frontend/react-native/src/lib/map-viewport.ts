import type { MapBounds, MapCoordinate } from '@/components/map-canvas.types';

export function distanceKm(from: MapCoordinate, to: MapCoordinate) {
  const radians = (degrees: number) => degrees * Math.PI / 180;
  const latitudeDelta = radians(to.latitude - from.latitude);
  const longitudeDelta = radians(to.longitude - from.longitude);
  const value = Math.sin(latitudeDelta / 2) ** 2
    + Math.cos(radians(from.latitude)) * Math.cos(radians(to.latitude)) * Math.sin(longitudeDelta / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
}

export function radiusForBounds(center: MapCoordinate, bounds?: MapBounds, fallbackKm = 5) {
  if (!bounds) return fallbackKm;
  const corners = [
    bounds.southWest,
    bounds.northEast,
    { latitude: bounds.southWest.latitude, longitude: bounds.northEast.longitude },
    { latitude: bounds.northEast.latitude, longitude: bounds.southWest.longitude },
  ];
  return Math.max(0.1, Math.min(25, Math.max(...corners.map((corner) => distanceKm(center, corner))) * 1.08));
}

export function isWithinBounds(point: MapCoordinate, bounds?: MapBounds) {
  if (!bounds) return true;
  const latitudeMatches = point.latitude >= bounds.southWest.latitude && point.latitude <= bounds.northEast.latitude;
  const crossesDateLine = bounds.southWest.longitude > bounds.northEast.longitude;
  const longitudeMatches = crossesDateLine
    ? point.longitude >= bounds.southWest.longitude || point.longitude <= bounds.northEast.longitude
    : point.longitude >= bounds.southWest.longitude && point.longitude <= bounds.northEast.longitude;
  return latitudeMatches && longitudeMatches;
}
