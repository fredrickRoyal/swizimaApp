/** Dialling codes offered in phone inputs. East Africa first, then other common countries. */
export interface DialCountry {
  iso: string;
  name: string;
  dial: string;
  flag: string;
}

export const DIAL_COUNTRIES: readonly DialCountry[] = [
  { iso: 'UG', name: 'Uganda', dial: '+256', flag: '🇺🇬' },
  { iso: 'KE', name: 'Kenya', dial: '+254', flag: '🇰🇪' },
  { iso: 'TZ', name: 'Tanzania', dial: '+255', flag: '🇹🇿' },
  { iso: 'RW', name: 'Rwanda', dial: '+250', flag: '🇷🇼' },
  { iso: 'BI', name: 'Burundi', dial: '+257', flag: '🇧🇮' },
  { iso: 'SS', name: 'South Sudan', dial: '+211', flag: '🇸🇸' },
  { iso: 'CD', name: 'DR Congo', dial: '+243', flag: '🇨🇩' },
  { iso: 'ET', name: 'Ethiopia', dial: '+251', flag: '🇪🇹' },
  { iso: 'SO', name: 'Somalia', dial: '+252', flag: '🇸🇴' },
  { iso: 'NG', name: 'Nigeria', dial: '+234', flag: '🇳🇬' },
  { iso: 'GH', name: 'Ghana', dial: '+233', flag: '🇬🇭' },
  { iso: 'ZA', name: 'South Africa', dial: '+27', flag: '🇿🇦' },
  { iso: 'EG', name: 'Egypt', dial: '+20', flag: '🇪🇬' },
  { iso: 'AE', name: 'United Arab Emirates', dial: '+971', flag: '🇦🇪' },
  { iso: 'IN', name: 'India', dial: '+91', flag: '🇮🇳' },
  { iso: 'CN', name: 'China', dial: '+86', flag: '🇨🇳' },
  { iso: 'GB', name: 'United Kingdom', dial: '+44', flag: '🇬🇧' },
  { iso: 'US', name: 'United States', dial: '+1', flag: '🇺🇸' },
];

const ALIASES: Record<string, string> = {
  'democratic republic of the congo': 'CD',
  drc: 'CD',
  uae: 'AE',
  uk: 'GB',
  usa: 'US',
  'united states of america': 'US',
};

/** Finds a country by name, alias or ISO code (case-insensitive). */
export function findDialCountry(nameOrIso?: string | null): DialCountry | undefined {
  const key = nameOrIso?.trim().toLowerCase();
  if (!key) return undefined;
  const iso = ALIASES[key] ?? key.toUpperCase();
  return DIAL_COUNTRIES.find((c) => c.iso === iso || c.name.toLowerCase() === key);
}

/** Splits "+256 700123456" into its country and local part (longest matching code wins). */
export function splitPhone(value?: string | null): { country?: DialCountry; local: string } {
  const v = (value ?? '').trim();
  if (!v.startsWith('+')) return { local: v };
  const compact = v.replace(/[\s()-]/g, '');
  const match = [...DIAL_COUNTRIES]
    .sort((a, b) => b.dial.length - a.dial.length)
    .find((c) => compact.startsWith(c.dial));
  return match ? { country: match, local: compact.slice(match.dial.length) } : { local: v };
}
