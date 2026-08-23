import { api } from './api';

type Envelope<T> = { data?: T };

export type GeocodingAddress = {
  roadAddress: string;
  jibunAddress: string;
  englishAddress: string;
  latitude: number;
  longitude: number;
};

type RawGeocodingAddress = Omit<GeocodingAddress, 'latitude' | 'longitude'> & {
  latitude: number | string;
  longitude: number | string;
};

type GeocodingResponse = { addresses: RawGeocodingAddress[] };

const unwrap = <T,>(value: T | Envelope<T>): T =>
  value && typeof value === 'object' && 'data' in value && value.data !== undefined
    ? value.data
    : value as T;

export async function searchStoreAddresses(query: string): Promise<GeocodingAddress[]> {
  const result = unwrap(await api<GeocodingResponse | Envelope<GeocodingResponse>>(
    `/locations/geocode?query=${encodeURIComponent(query.trim())}`,
    undefined,
    { globalLoading: false, timeoutMs: 8_000 },
  ));

  return result.addresses.map((address) => ({
    ...address,
    latitude: Number(address.latitude),
    longitude: Number(address.longitude),
  }));
}
