import type { Role } from '../../shared/permissions';
export const reportContracts: {
  path: string;
  title: string;
  roles: Role[];
  fields: string[];
}[] = [
  {
    path: '/job-cards',
    title: 'job cards',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE'],
    fields: ['fromDate', 'toDate'],
  },
  {
    path: '/job-cards/customer/{customerId}',
    title: 'job cards \u00b7 customer \u00b7 customerId',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE'],
    fields: ['customerId'],
  },
  {
    path: '/job-cards/crane/{craneId}',
    title: 'job cards \u00b7 crane \u00b7 craneId',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE'],
    fields: ['craneId'],
  },
  {
    path: '/job-cards/status/{status}',
    title: 'job cards \u00b7 status \u00b7 status',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE'],
    fields: ['status'],
  },
  {
    path: '/job-cards/sales-person/{salesPersonId}',
    title: 'job cards \u00b7 sales person \u00b7 salesPersonId',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE'],
    fields: ['salesPersonId'],
  },
  {
    path: '/invoices',
    title: 'invoices',
    roles: ['ADMIN', 'FINANCE'],
    fields: [],
  },
  {
    path: '/invoices/date',
    title: 'invoices \u00b7 date',
    roles: ['ADMIN', 'FINANCE'],
    fields: ['fromDate', 'toDate'],
  },
  {
    path: '/invoices/customer/{customerId}',
    title: 'invoices \u00b7 customer \u00b7 customerId',
    roles: ['ADMIN', 'FINANCE'],
    fields: ['customerId'],
  },
  {
    path: '/invoices/status/{status}',
    title: 'invoices \u00b7 status \u00b7 status',
    roles: ['ADMIN', 'FINANCE'],
    fields: ['status'],
  },
  {
    path: '/crane-utilization',
    title: 'crane utilization',
    roles: ['ADMIN', 'SALES', 'OPERATIONS'],
    fields: [],
  },
  {
    path: '/revenue-summary',
    title: 'revenue summary',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE', 'MAINTENANCE'],
    fields: [],
  },
  {
    path: '/revenue-summary/date',
    title: 'revenue summary \u00b7 date',
    roles: ['ADMIN', 'SALES', 'FINANCE'],
    fields: ['fromDate', 'toDate'],
  },
  {
    path: '/revenue/monthly',
    title: 'revenue \u00b7 monthly',
    roles: ['ADMIN', 'SALES', 'FINANCE'],
    fields: [],
  },
  {
    path: '/revenue/customer',
    title: 'revenue \u00b7 customer',
    roles: ['ADMIN', 'SALES', 'FINANCE'],
    fields: [],
  },
  {
    path: '/revenue/crane',
    title: 'revenue \u00b7 crane',
    roles: ['ADMIN', 'SALES', 'FINANCE'],
    fields: [],
  },
  {
    path: '/maintenance',
    title: 'maintenance',
    roles: ['ADMIN', 'OPERATIONS', 'MAINTENANCE'],
    fields: [],
  },
  {
    path: '/maintenance/summary',
    title: 'maintenance \u00b7 summary',
    roles: ['ADMIN', 'OPERATIONS', 'MAINTENANCE'],
    fields: [],
  },
  {
    path: '/maintenance/status',
    title: 'maintenance \u00b7 status',
    roles: ['ADMIN', 'OPERATIONS', 'MAINTENANCE'],
    fields: [],
  },
];
