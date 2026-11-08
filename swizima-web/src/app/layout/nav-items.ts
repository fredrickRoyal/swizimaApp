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
      { label: 'Top-up requests', path: '/topup-requests', icon: 'clock' },
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

/** Sidebar navigation for a signed-in Client User (Client Portal module). */
export const CLIENT_PORTAL_NAV_GROUPS: NavGroup[] = [
  {
    heading: 'Client Portal',
    items: [
      { label: 'View details', path: '/portal/details', icon: 'eye' },
      { label: 'Dashboard', path: '/portal/dashboard', icon: 'dashboard' },
      { label: 'Service catalog', path: '/portal/services', icon: 'catalog' },
      { label: 'Staff', path: '/portal/staff', icon: 'users' },
      { label: 'Transactions', path: '/portal/transactions', icon: 'transfer' },
      { label: 'Top-up requests', path: '/portal/topup-requests', icon: 'clock' },
      { label: 'Billing', path: '/portal/billing', icon: 'receipt' },
    ],
  },
];
