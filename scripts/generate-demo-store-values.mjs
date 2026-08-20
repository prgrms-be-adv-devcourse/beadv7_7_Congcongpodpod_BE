import { readFile, writeFile } from 'node:fs/promises';

const stations = [
  { name: '남부터미널역', latitude: 37.484940, longitude: 127.016290 },
  { name: '교대역', latitude: 37.493415, longitude: 127.014080 },
  { name: '서초역', latitude: 37.491900, longitude: 127.007900 },
  { name: '방배역', latitude: 37.481456, longitude: 126.997535 },
  { name: '신대방삼거리역', latitude: 37.499700, longitude: 126.928200 },
  { name: '강남역', latitude: 37.497950, longitude: 127.027600 },
];

const escapeSql = (value) => value.replaceAll("'", "''");
const distance = (a, b) => {
  const toRadians = (degrees) => (degrees * Math.PI) / 180;
  const dLatitude = toRadians(b.latitude - a.latitude);
  const dLongitude = toRadians(b.longitude - a.longitude);
  const latitude1 = toRadians(a.latitude);
  const latitude2 = toRadians(b.latitude);
  const h = Math.sin(dLatitude / 2) ** 2
    + Math.cos(latitude1) * Math.cos(latitude2) * Math.sin(dLongitude / 2) ** 2;
  return 6371000 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
};

const category = (place) => {
  const value = `${place.tags.amenity ?? ''} ${place.tags.cuisine ?? ''} ${place.tags.name}`.toLowerCase();
  if (value.includes('cafe') || value.includes('coffee') || value.includes('dessert') || value.includes('카페')) return 'CAFE_DESSERT';
  if (value.includes('chicken') || value.includes('치킨')) return 'CHICKEN';
  if (value.includes('chinese') || value.includes('중식') || value.includes('짜장') || value.includes('마라')) return 'CHINESE';
  if (value.includes('pizza') || value.includes('피자')) return 'PIZZA';
  if (value.includes('japanese') || value.includes('sushi') || value.includes('돈까스') || value.includes('초밥')) return 'CUTLET_SUSHI';
  if (value.includes('burger') || value.includes('fast_food') || value.includes('버거')) return 'FAST_FOOD';
  if (value.includes('vietnamese') || value.includes('thai') || value.includes('indian')) return 'ASIAN';
  if (value.includes('italian') || value.includes('western')) return 'WESTERN';
  if (value.includes('meat') || value.includes('barbecue') || value.includes('고기')) return 'MEAT';
  return 'KOREAN';
};

const address = (tags, station) => {
  if (tags['addr:full']) return tags['addr:full'];
  const parts = [tags['addr:city'] ?? '서울특별시', tags['addr:district'], tags['addr:street'], tags['addr:housenumber']];
  const resolved = parts.filter(Boolean).join(' ');
  return resolved.includes(' ') ? resolved : `서울특별시 ${station.name} 인근`;
};

const query = `[out:json][timeout:120];(${stations.map(({ latitude, longitude }) =>
  `nwr(around:2500,${latitude},${longitude})[name][amenity~"restaurant|cafe|fast_food"];`).join('')});out center tags;`;
const response = await fetch('https://overpass-api.de/api/interpreter', {
  method: 'POST',
  headers: {
    'content-type': 'application/x-www-form-urlencoded',
    'user-agent': 'LastDish demo seed generator (https://github.com/prgrms-be-adv-devcourse/beadv7_7_Congcongpodpod_BE)',
  },
  body: new URLSearchParams({ data: query }),
});
if (!response.ok) throw new Error(`Overpass API failed: ${response.status} ${await response.text()}`);

const elements = (await response.json()).elements
  .filter((element) =>
    element.tags?.name
    && (element.lat || element.center?.lat)
    && (element.tags['addr:full'] || element.tags['addr:street']))
  .map((element) => ({
    sourceId: `${element.type}/${element.id}`,
    latitude: element.lat ?? element.center.lat,
    longitude: element.lon ?? element.center.lon,
    tags: element.tags,
  }));

const used = new Set();
const selected = [];
for (const station of stations) {
  const candidates = elements
    .map((place) => ({ ...place, meters: distance(station, place) }))
    .filter((place) => place.meters <= 2500 && !used.has(place.sourceId))
    .sort((a, b) => a.meters - b.meters || a.sourceId.localeCompare(b.sourceId));
  if (candidates.length < 50) throw new Error(`${station.name}: only ${candidates.length} unique places found`);
  for (const place of candidates.slice(0, 50)) {
    used.add(place.sourceId);
    selected.push({ ...place, station });
  }
}

const rows = selected.map((place, index) => {
  const values = [
    index + 1,
    place.sourceId,
    place.station.name,
    place.tags.name,
    address(place.tags, place.station),
    category(place),
    place.latitude.toFixed(6),
    place.longitude.toFixed(6),
  ];
  return `  (${values.map((value, valueIndex) => valueIndex === 0 || valueIndex >= 6 ? value : `'${escapeSql(String(value))}'`).join(', ')})`;
});

const storeValues = [
  '-- Generated from OpenStreetMap via Overpass API. Do not edit manually.',
  '-- Regenerate with: node scripts/generate-demo-store-values.mjs',
  "INSERT INTO demo_store_source (seed_id, source_id, source_area, store_name, store_address, category, latitude, longitude) VALUES",
  `${rows.join(',\n')};`,
  '',
].join('\n');
const outputPath = 'backend/services/core-service/src/main/resources/db/seed/R__demo_seed.sql';
const seed = await readFile(outputPath, 'utf8');
const marker = /-- BEGIN GENERATED DEMO STORES[\s\S]*?-- END GENERATED DEMO STORES/;
if (!marker.test(seed)) throw new Error(`Missing generated-store markers in ${outputPath}`);
await writeFile(outputPath, seed.replace(marker, `-- BEGIN GENERATED DEMO STORES\n${storeValues.trim()}\n-- END GENERATED DEMO STORES`));
console.log(`Generated ${selected.length} stores (${stations.map((station) => `${station.name} 50`).join(', ')})`);
