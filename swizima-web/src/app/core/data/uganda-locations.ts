/**
 * Uganda locations for address pickers (client registration, etc.).
 *
 * - `UGANDA_DISTRICTS`: districts grouped by region.
 * - `UGANDA_TOWNS`: main cities / municipalities / towns, keyed by district.
 *   Districts not listed fall back to a town with the district's own name (usually the district HQ).
 *
 * Forms keep an "Other…" option, so a missing entry never blocks registration.
 * Update this file when new districts or cities are gazetted.
 */

export interface DistrictGroup {
  region: string;
  districts: readonly string[];
}

export const UGANDA_DISTRICTS: readonly DistrictGroup[] = [
  {
    region: 'Central',
    districts: [
      'Buikwe', 'Bukomansimbi', 'Butambala', 'Buvuma', 'Gomba', 'Kalangala', 'Kalungu', 'Kampala',
      'Kassanda', 'Kayunga', 'Kiboga', 'Kyankwanzi', 'Kyotera', 'Luwero', 'Lwengo', 'Lyantonde',
      'Masaka', 'Mityana', 'Mpigi', 'Mubende', 'Mukono', 'Nakaseke', 'Nakasongola', 'Rakai',
      'Sembabule', 'Wakiso',
    ],
  },
  {
    region: 'Eastern',
    districts: [
      'Amuria', 'Budaka', 'Bududa', 'Bugiri', 'Bugweri', 'Bukedea', 'Bukwo', 'Bulambuli', 'Busia',
      'Butaleja', 'Butebo', 'Buyende', 'Iganga', 'Jinja', 'Kaberamaido', 'Kalaki', 'Kaliro', 'Kamuli',
      'Kapchorwa', 'Kapelebyong', 'Katakwi', 'Kibuku', 'Kumi', 'Kween', 'Luuka', 'Manafwa', 'Mayuge',
      'Mbale', 'Namayingo', 'Namisindwa', 'Namutumba', 'Ngora', 'Pallisa', 'Serere', 'Sironko',
      'Soroti', 'Tororo',
    ],
  },
  {
    region: 'Northern',
    districts: [
      'Abim', 'Adjumani', 'Agago', 'Alebtong', 'Amolatar', 'Amudat', 'Amuru', 'Apac', 'Arua', 'Dokolo',
      'Gulu', 'Kaabong', 'Karenga', 'Kitgum', 'Koboko', 'Kole', 'Kotido', 'Kwania', 'Lamwo', 'Lira',
      'Madi-Okollo', 'Maracha', 'Moroto', 'Moyo', 'Nabilatuk', 'Nakapiripirit', 'Napak', 'Nebbi',
      'Nwoya', 'Obongi', 'Omoro', 'Otuke', 'Oyam', 'Pader', 'Pakwach', 'Terego', 'Yumbe', 'Zombo',
    ],
  },
  {
    region: 'Western',
    districts: [
      'Buhweju', 'Buliisa', 'Bundibugyo', 'Bunyangabu', 'Bushenyi', 'Hoima', 'Ibanda', 'Isingiro',
      'Kabale', 'Kabarole', 'Kagadi', 'Kakumiro', 'Kamwenge', 'Kanungu', 'Kasese', 'Kazo', 'Kibaale',
      'Kikuube', 'Kiruhura', 'Kiryandongo', 'Kisoro', 'Kitagwenda', 'Kyegegwa', 'Kyenjojo', 'Masindi',
      'Mbarara', 'Mitooma', 'Ntoroko', 'Ntungamo', 'Rubanda', 'Rubirizi', 'Rukiga', 'Rukungiri',
      'Rwampara', 'Sheema',
    ],
  },
];

/** Main cities / towns per district (first entry = main town). */
const TOWNS_BY_DISTRICT: Readonly<Record<string, readonly string[]>> = {
  // Central
  Kampala: ['Kampala'],
  Wakiso: ['Entebbe', 'Nansana', 'Kira', 'Makindye-Ssabagabo', 'Wakiso', 'Kasangati', 'Kajjansi', 'Matugga', 'Gayaza', 'Namugongo'],
  Mukono: ['Mukono', 'Seeta', 'Namataba'],
  Buikwe: ['Lugazi', 'Njeru', 'Buikwe', 'Nkokonjeru'],
  Luwero: ['Luwero', 'Bombo', 'Wobulenzi'],
  Masaka: ['Masaka'],
  Mityana: ['Mityana'],
  Mubende: ['Mubende'],
  Mpigi: ['Mpigi'],
  Kyotera: ['Kyotera', 'Mutukula', 'Kalisizo'],
  Kalangala: ['Kalangala'],
  Kayunga: ['Kayunga'],
  Nakasongola: ['Nakasongola'],
  Lyantonde: ['Lyantonde'],
  Sembabule: ['Sembabule'],
  // Eastern
  Jinja: ['Jinja', 'Bugembe', 'Kakira'],
  Mbale: ['Mbale'],
  Soroti: ['Soroti'],
  Tororo: ['Tororo', 'Malaba'],
  Busia: ['Busia'],
  Iganga: ['Iganga'],
  Bugiri: ['Bugiri'],
  Kamuli: ['Kamuli'],
  Kumi: ['Kumi'],
  Pallisa: ['Pallisa'],
  Kapchorwa: ['Kapchorwa'],
  Mayuge: ['Mayuge'],
  // Northern
  Gulu: ['Gulu'],
  Lira: ['Lira'],
  Arua: ['Arua'],
  Kitgum: ['Kitgum'],
  Moroto: ['Moroto'],
  Koboko: ['Koboko'],
  Nebbi: ['Nebbi'],
  Apac: ['Apac'],
  Adjumani: ['Adjumani'],
  Yumbe: ['Yumbe'],
  // Western
  Mbarara: ['Mbarara'],
  Kabarole: ['Fort Portal'],
  Hoima: ['Hoima'],
  Kasese: ['Kasese', 'Hima'],
  Kabale: ['Kabale'],
  Masindi: ['Masindi'],
  Bushenyi: ['Bushenyi-Ishaka'],
  Sheema: ['Kabwohe'],
  Ntungamo: ['Ntungamo'],
  Rukungiri: ['Rukungiri'],
  Kisoro: ['Kisoro'],
  Ibanda: ['Ibanda'],
  Isingiro: ['Isingiro'],
  Kiryandongo: ['Kigumba', 'Bweyale'],
  Kagadi: ['Kagadi'],
  Kyenjojo: ['Kyenjojo'],
  Kamwenge: ['Kamwenge'],
  Kanungu: ['Kanungu', 'Kihihi'],
  Bundibugyo: ['Bundibugyo'],
};

export const ALL_DISTRICTS: readonly string[] = UGANDA_DISTRICTS.flatMap((g) => g.districts);

/** Towns for a district (falls back to the district name, usually its HQ town). */
export function townsForDistrict(district: string): readonly string[] {
  if (!district) return [];
  return TOWNS_BY_DISTRICT[district] ?? [district];
}

/** Every known town with its district, sorted by town name (used when no district is chosen yet). */
export const UGANDA_TOWNS: readonly { town: string; district: string }[] = ALL_DISTRICTS
  .flatMap((district) => townsForDistrict(district).map((town) => ({ town, district })))
  .sort((a, b) => a.town.localeCompare(b.town));

/** District of a known town, or undefined. */
export function districtOfTown(town: string): string | undefined {
  return UGANDA_TOWNS.find((t) => t.town === town)?.district;
}
