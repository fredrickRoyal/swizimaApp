import { IconName } from '../shared/icon/icon';

export interface NavItem {
  label: string;
  path: string;
  icon: IconName;
}

export interface NavGroup {
  heading: string;
  items: NavItem[];
}

/** Left-hand navigation. Order here = order in the sidebar. */
export const NAV_GROUPS: NavGroup[] = [
  {
    heading: 'Overview',
    items: [{ label: 'Dashboard', path: '/dashboard', icon: 'dashboard' }],
  },
  {
    heading: 'Operations',
    items: [
      { label: 'Clients', path: '/clients', icon: 'clients' },
      { label: 'Services offered', path: '/services', icon: 'car' },
      { label: 'Payments', path: '/payments', icon: 'payments' },
    ],
  },
  {
    heading: 'Administration',
    items: [
      { label: 'Users', path: '/users', icon: 'users' },
      { label: 'Configurations', path: '/configurations', icon: 'settings' },
    ],
  },
];
