export const roles = [
  'ADMIN',
  'SALES',
  'OPERATIONS',
  'FINANCE',
  'MAINTENANCE',
] as const;
export type Role = (typeof roles)[number];
// Full-record read permissions from controller annotations, NOT lookup permissions.
export const permissions = {
  dashboard: roles,
  reports: roles,
  cranes: ['ADMIN', 'SALES'],
  customers: ['ADMIN', 'SALES', 'OPERATIONS'],
  quotations: ['ADMIN', 'SALES'],
  jobs: ['ADMIN', 'SALES', 'OPERATIONS'],
  timesheets: ['ADMIN', 'OPERATIONS'],
  maintenance: ['ADMIN', 'OPERATIONS', 'MAINTENANCE'],
  invoices: ['ADMIN', 'FINANCE'],
  users: ['ADMIN'],
} satisfies Record<string, readonly Role[]>;
export type Permission = keyof typeof permissions;
export const can = (role: Role, permission: Permission) =>
  (permissions[permission] as readonly Role[]).includes(role);
export const modules: { key: Permission; label: string }[] = [
  { key: 'dashboard', label: 'Overview' },
  { key: 'cranes', label: 'Cranes' },
  { key: 'customers', label: 'Customers' },
  { key: 'quotations', label: 'Quotations' },
  { key: 'jobs', label: 'Job cards' },
  { key: 'timesheets', label: 'Timesheets' },
  { key: 'maintenance', label: 'Maintenance' },
  { key: 'invoices', label: 'Invoices' },
  { key: 'users', label: 'Users' },
  { key: 'reports', label: 'Reports' },
];

export const canWriteCustomers = (role: Role) =>
  role === 'ADMIN' || role === 'SALES';
