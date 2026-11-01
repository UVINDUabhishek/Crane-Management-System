import type { ReportView } from './reportPresentation';
import type { Role } from '../../shared/permissions';
export type ReportCategory =
  | 'Job Cards'
  | 'Invoices'
  | 'Crane'
  | 'Revenue Summary'
  | 'Revenue'
  | 'Maintenance';
export interface ReportContract {
  path: string;
  title: string;
  category: ReportCategory;
  view: ReportView;
  roles: Role[];
  fields: string[];
}
export const reportContracts: ReportContract[] = [
  {
    path: '/job-cards',
    title: 'Job Cards · General',
    category: 'Job Cards',
    view: 'jobs',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE'],
    fields: ['fromDate', 'toDate'],
  },
  {
    path: '/job-cards/customer/{customerId}',
    title: 'Job Cards · Customer',
    category: 'Job Cards',
    view: 'jobsCustomer',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE'],
    fields: ['customerId'],
  },
  {
    path: '/job-cards/crane/{craneId}',
    title: 'Job Cards · Crane',
    category: 'Job Cards',
    view: 'jobsCrane',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE'],
    fields: ['craneId'],
  },
  {
    path: '/job-cards/status/{status}',
    title: 'Job Cards · Status',
    category: 'Job Cards',
    view: 'jobsStatus',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE'],
    fields: ['status'],
  },
  {
    path: '/job-cards/sales-person/{salesPersonId}',
    title: 'Job Cards · Sales Person',
    category: 'Job Cards',
    view: 'jobsSales',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE'],
    fields: ['salesPersonId'],
  },
  {
    path: '/invoices',
    title: 'Invoices · General',
    category: 'Invoices',
    view: 'invoices',
    roles: ['ADMIN', 'FINANCE'],
    fields: [],
  },
  {
    path: '/invoices/date',
    title: 'Invoices · Date',
    category: 'Invoices',
    view: 'invoices',
    roles: ['ADMIN', 'FINANCE'],
    fields: ['fromDate', 'toDate'],
  },
  {
    path: '/invoices/customer/{customerId}',
    title: 'Invoices · Customer',
    category: 'Invoices',
    view: 'invoicesCustomer',
    roles: ['ADMIN', 'FINANCE'],
    fields: ['customerId'],
  },
  {
    path: '/invoices/status/{status}',
    title: 'Invoices · Status',
    category: 'Invoices',
    view: 'invoices',
    roles: ['ADMIN', 'FINANCE'],
    fields: ['status'],
  },
  {
    path: '/crane-utilization',
    title: 'Crane · Utilization',
    category: 'Crane',
    view: 'crane',
    roles: ['ADMIN', 'SALES', 'OPERATIONS'],
    fields: [],
  },
  {
    path: '/revenue-summary',
    title: 'Revenue Summary · Overall',
    category: 'Revenue Summary',
    view: 'summary',
    roles: ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE', 'MAINTENANCE'],
    fields: [],
  },
  {
    path: '/revenue-summary/date',
    title: 'Revenue Summary · Date',
    category: 'Revenue Summary',
    view: 'summary',
    roles: ['ADMIN', 'SALES', 'FINANCE'],
    fields: ['fromDate', 'toDate'],
  },
  {
    path: '/revenue/monthly',
    title: 'Revenue · Monthly',
    category: 'Revenue',
    view: 'monthly',
    roles: ['ADMIN', 'SALES', 'FINANCE'],
    fields: [],
  },
  {
    path: '/revenue/customer',
    title: 'Revenue · Customer',
    category: 'Revenue',
    view: 'customerRevenue',
    roles: ['ADMIN', 'SALES', 'FINANCE'],
    fields: [],
  },
  {
    path: '/revenue/crane',
    title: 'Revenue · Crane',
    category: 'Revenue',
    view: 'craneRevenue',
    roles: ['ADMIN', 'SALES', 'FINANCE'],
    fields: [],
  },
  {
    path: '/maintenance',
    title: 'Maintenance · General',
    category: 'Maintenance',
    view: 'maintenance',
    roles: ['ADMIN', 'OPERATIONS', 'MAINTENANCE'],
    fields: [],
  },
  {
    path: '/maintenance/summary',
    title: 'Maintenance · Summary',
    category: 'Maintenance',
    view: 'maintenanceSummary',
    roles: ['ADMIN', 'OPERATIONS', 'MAINTENANCE'],
    fields: [],
  },
  {
    path: '/maintenance/status',
    title: 'Maintenance · Status',
    category: 'Maintenance',
    view: 'maintenanceStatus',
    roles: ['ADMIN', 'OPERATIONS', 'MAINTENANCE'],
    fields: [],
  },
];

export function reportStatuses(spec: ReportContract) {
  return spec.category === 'Job Cards'
    ? ['PENDING', 'COMPLETED', 'CANCELED']
    : ['DRAFT', 'COMPLETED', 'CANCELED', 'REVISED'];
}
export function reportFilterErrors(
  spec: ReportContract,
  values: Record<string, string>,
  submitted: boolean,
) {
  const errors: Record<string, string> = {};
  if (spec.fields.includes('fromDate')) {
    const from = values.fromDate ?? '',
      to = values.toDate ?? '';
    const required = spec.path !== '/job-cards';
    if (!from && (to || (required && submitted)))
      errors.fromDate = 'Enter From Date to complete the date range.';
    if (!to && (from || (required && submitted)))
      errors.toDate = 'Enter To Date to complete the date range.';
    if (from && to && to < from)
      errors.toDate = 'To Date cannot be earlier than From Date.';
  }
  for (const field of spec.fields.filter((f) => f.endsWith('Id')))
    if ((values[field] || submitted) && !/^[1-9]\d*$/.test(values[field] ?? ''))
      errors[field] = 'Select a record or enter a valid positive ID.';
  if (
    spec.fields.includes('status') &&
    (values.status || submitted) &&
    !reportStatuses(spec).includes(values.status ?? '')
  )
    errors.status = 'Select a status.';
  return errors;
}
export function reportRequestUrl(
  spec: ReportContract,
  values: Record<string, string>,
) {
  let path = spec.path;
  const params = new URLSearchParams();
  for (const field of spec.fields) {
    if (path.includes(`{${field}}`))
      path = path.replace(`{${field}}`, encodeURIComponent(values[field]));
    else if (values[field]) params.set(field, values[field]);
  }
  return path + (params.size ? '?' + params.toString() : '');
}
