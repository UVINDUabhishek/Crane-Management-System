import { expect, it, vi } from 'vitest';
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { ThemeProvider } from '@mui/material';
import { MemoryRouter, Link } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { AppRoutes, createQueryClient } from '../app/App';
import { createAppTheme } from '../app/theme';
import { AuthProvider } from '../features/auth/AuthProvider';
import type { Role } from '../shared/permissions';
import { api } from '../shared/api';
import { server } from './setup';
import { createRequestGate } from './requestGate';

// Independent expectations from ReportController and the Java response DTOs.
const allRoles: Role[] = [
  'ADMIN',
  'SALES',
  'OPERATIONS',
  'FINANCE',
  'MAINTENANCE',
];
const jobsRoles: Role[] = ['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE'];
const financeRoles: Role[] = ['ADMIN', 'FINANCE'];
const revenueRoles: Role[] = ['ADMIN', 'SALES', 'FINANCE'];
const maintenanceRoles: Role[] = ['ADMIN', 'OPERATIONS', 'MAINTENANCE'];
const jobHeaders = [
  'Job Card Number',
  'Crane Number',
  'Customer Name',
  'Location',
  'Start Date',
  'End Date',
  'Sales Person',
  'Operator',
  'Status',
  'Total Worked Hours',
];
const invoiceHeaders = [
  'Invoice Date',
  'Invoice Number',
  'Job Card Number',
  'Crane Number',
  'Customer Name',
  'Description',
  'Amount',
];
const summaryHeaders = [
  'Total Invoices',
  'Total Amount',
  'Total Mobilization',
  'Total Revenue',
];
const maintenanceHeaders = [
  'Maintenance Number',
  'Crane Number',
  'Crane Type',
  'Repair Type',
  'Start Date',
  'End Date',
  'Maintenance Person',
  'Status',
];
const summary = {
  totalRevenue: 8500,
  totalInvoices: 2,
  totalMobilization: 500,
  totalAmount: 8500,
};
interface ReportCase {
  category: string;
  title: string;
  url: string;
  roles: Role[];
  headers: string[];
  filter?: 'Customer' | 'Crane' | 'Sales Person' | 'Status' | 'Date';
  summary?: boolean;
}
const cases: ReportCase[] = [
  {
    category: 'Job Cards',
    title: 'Job Cards · General',
    url: '/job-cards',
    roles: jobsRoles,
    headers: jobHeaders,
  },
  {
    category: 'Job Cards',
    title: 'Job Cards · Customer',
    url: '/job-cards/customer/42',
    roles: jobsRoles,
    headers: jobHeaders.filter((h) => h !== 'Customer Name'),
    filter: 'Customer',
  },
  {
    category: 'Job Cards',
    title: 'Job Cards · Crane',
    url: '/job-cards/crane/42',
    roles: jobsRoles,
    headers: jobHeaders.filter((h) => h !== 'Crane Number'),
    filter: 'Crane',
  },
  {
    category: 'Job Cards',
    title: 'Job Cards · Status',
    url: '/job-cards/status/COMPLETED',
    roles: jobsRoles,
    headers: jobHeaders.filter((h) => h !== 'Status'),
    filter: 'Status',
  },
  {
    category: 'Job Cards',
    title: 'Job Cards · Sales Person',
    url: '/job-cards/sales-person/42',
    roles: jobsRoles,
    headers: jobHeaders.filter((h) => h !== 'Sales Person'),
    filter: 'Sales Person',
  },
  {
    category: 'Invoices',
    title: 'Invoices · General',
    url: '/invoices',
    roles: financeRoles,
    headers: invoiceHeaders,
  },
  {
    category: 'Invoices',
    title: 'Invoices · Date',
    url: '/invoices/date?fromDate=2026-10-01&toDate=2026-10-31',
    roles: financeRoles,
    headers: invoiceHeaders,
    filter: 'Date',
  },
  {
    category: 'Invoices',
    title: 'Invoices · Customer',
    url: '/invoices/customer/42',
    roles: financeRoles,
    headers: invoiceHeaders.filter((h) => h !== 'Customer Name'),
    filter: 'Customer',
  },
  {
    category: 'Invoices',
    title: 'Invoices · Status',
    url: '/invoices/status/COMPLETED',
    roles: financeRoles,
    headers: invoiceHeaders,
    filter: 'Status',
  },
  {
    category: 'Crane',
    title: 'Crane · Utilization',
    url: '/crane-utilization',
    roles: ['ADMIN', 'SALES', 'OPERATIONS'],
    headers: ['Crane Number', 'Crane Type', 'Total Jobs', 'Total Worked Hours'],
  },
  {
    category: 'Revenue Summary',
    title: 'Revenue Summary · Overall',
    url: '/revenue-summary',
    roles: allRoles,
    headers: summaryHeaders,
    summary: true,
  },
  {
    category: 'Revenue Summary',
    title: 'Revenue Summary · Date',
    url: '/revenue-summary/date?fromDate=2026-10-01&toDate=2026-10-31',
    roles: revenueRoles,
    headers: summaryHeaders,
    filter: 'Date',
    summary: true,
  },
  {
    category: 'Revenue',
    title: 'Revenue · Monthly',
    url: '/revenue/monthly',
    roles: revenueRoles,
    headers: ['Year', 'Month Number', 'Month', ...summaryHeaders],
  },
  {
    category: 'Revenue',
    title: 'Revenue · Customer',
    url: '/revenue/customer',
    roles: revenueRoles,
    headers: ['Customer ID', 'Customer Name', ...summaryHeaders],
  },
  {
    category: 'Revenue',
    title: 'Revenue · Crane',
    url: '/revenue/crane',
    roles: revenueRoles,
    headers: ['Crane ID', 'Crane Number', 'Crane Type', ...summaryHeaders],
  },
  {
    category: 'Maintenance',
    title: 'Maintenance · General',
    url: '/maintenance',
    roles: maintenanceRoles,
    headers: maintenanceHeaders,
  },
  {
    category: 'Maintenance',
    title: 'Maintenance · Summary',
    url: '/maintenance/summary',
    roles: maintenanceRoles,
    headers: [
      'Crane Number',
      'Crane Type',
      'Total Maintenance Records',
      'Total Maintenance Days',
    ],
  },
  {
    category: 'Maintenance',
    title: 'Maintenance · Status',
    url: '/maintenance/status',
    roles: maintenanceRoles,
    headers: ['Status', 'Total'],
  },
];

function renderApp(initial = '/') {
  render(
    <ThemeProvider theme={createAppTheme(false)}>
      <QueryClientProvider client={createQueryClient()}>
        <AuthProvider>
          <MemoryRouter initialEntries={[initial]}>
            <AppRoutes />
            <Link to="/workspace/reports">Open report workspace</Link>
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>,
  );
}
async function mount(role: Role = 'ADMIN') {
  server.use(
    http.post('/api/auth/login', () =>
      HttpResponse.text(
        `header.${btoa(JSON.stringify({ exp: Date.now() / 1000 + 3600 }))}.signature`,
      ),
    ),
    http.get('/api/auth/me', () =>
      HttpResponse.json({
        id: 1,
        username: 'Alex',
        email: 'alex@example.com',
        role,
      }),
    ),
    http.get('/api/dashboard', () => HttpResponse.json({ totalCranes: 1 })),
    http.get('/api/lookups/:entity', () =>
      HttpResponse.json([{ id: 42, label: 'Historical admin' }]),
    ),
  );
  renderApp();
  const user = userEvent.setup();
  fireEvent.change(screen.getByLabelText('Email address'), {
    target: { value: 'alex@example.com' },
  });
  fireEvent.change(screen.getByLabelText('Password'), {
    target: { value: 'password' },
  });
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
  await screen.findByRole('heading', { name: 'Operations at a glance' });
  await user.click(screen.getByRole('link', { name: 'Open report workspace' }));
  await screen.findByRole('combobox', { name: 'Report Category' });
  return user;
}
type User = ReturnType<typeof userEvent.setup>;
async function choose(user: User, label: string, value: string) {
  const field = screen.getByRole('combobox', { name: label });
  if (field.textContent === value) return;
  await user.click(field);
  await user.click(screen.getByRole('option', { name: value }));
}
async function select(user: User, category: string, title: string) {
  await choose(user, 'Report Category', category);
  await choose(user, 'Report Type', title);
}
function changeDate(label: string, value: string) {
  fireEvent.change(screen.getByLabelText(label, { exact: false }), {
    target: { value },
  });
}
async function fillFilter(user: User, item: ReportCase) {
  if (item.filter === 'Date') {
    changeDate('From Date', '2026-10-01');
    changeDate('To Date', '2026-10-31');
  } else if (item.filter === 'Status') {
    await choose(user, 'Status', 'Completed');
  } else if (item.filter) {
    await user.click(screen.getByRole('combobox', { name: item.filter }));
    await user.click(
      await screen.findByRole('option', { name: 'Historical admin (#42)' }),
    );
  }
}
function headers() {
  return within(screen.getByRole('table', { name: 'Report results' }))
    .getAllByRole('columnheader')
    .map((h) => h.textContent);
}
function captureReports(
  body: Record<string, unknown> | Record<string, unknown>[] = [],
) {
  const read = vi.fn();
  server.use(
    http.get('/api/reports/*', ({ request }) => {
      const url = new URL(request.url);
      read(url.pathname + url.search, request.headers.get('authorization'));
      return HttpResponse.json(body);
    }),
  );
  return read;
}

it.each(cases.flatMap((item) => item.roles.map((role) => ({ ...item, role }))))(
  '$role runs $title with its exact endpoint and stable columns',
  async (item) => {
    const read = captureReports(item.summary ? summary : []);
    const user = await mount(item.role);
    await select(user, item.category, item.title);
    expect(headers()).toEqual(item.headers);
    expect(screen.queryByText('Report builder')).not.toBeInTheDocument();
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    await fillFilter(user, item);
    expect(read).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Run report' }));
    if (item.summary) {
      const table = screen.getByRole('table', { name: 'Report results' });
      expect(await within(table).findAllByText('8,500.00')).toHaveLength(2);
      expect(within(table).getByText('500.00')).toBeVisible();
      expect(within(table).getByText('2')).toBeVisible();
      // No duplicate KPI rendering or list pagination for an object summary.
      expect(screen.getAllByText('Total Revenue')).toHaveLength(1);
      expect(screen.getAllByText('8,500.00')).toHaveLength(2);
      expect(screen.queryByText('Rows per page:')).not.toBeInTheDocument();
    } else {
      expect(await screen.findByText('No report records')).toBeVisible();
    }
    expect(headers()).toEqual(item.headers);
    expect(read).toHaveBeenCalledExactlyOnceWith(
      '/api/reports' + item.url,
      expect.stringMatching(/^Bearer /),
    );
  },
);

it.each(allRoles)(
  'only offers permitted categories and report types for %s',
  async (role) => {
    const read = captureReports();
    const user = await mount(role);
    const permitted = cases.filter((item) => item.roles.includes(role));
    const categories = [...new Set(permitted.map((item) => item.category))];
    await user.click(screen.getByRole('combobox', { name: 'Report Category' }));
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(
      categories,
    );
    await user.keyboard('{Escape}');
    for (const category of categories) {
      await choose(user, 'Report Category', category);
      await user.click(screen.getByRole('combobox', { name: 'Report Type' }));
      expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(
        permitted
          .filter((item) => item.category === category)
          .map((item) => item.title),
      );
      await user.keyboard('{Escape}');
    }
    expect(read).not.toHaveBeenCalled();
  },
);

const job = {
  totalBillableHours: 99999,
  jobDate: '1999-01-01',
  operatorName: 'Morgan',
  status: 'PENDING',
  jobCardNumber: 'JC-01',
  craneNumber: 'CR-01',
  customerName: 'Atlas',
  location: 'Harbor',
  startDate: '2026-10-01',
  endDate: '2026-10-02',
  salesPerson: 'Admin Alex',
  totalWorkedHours: 12.5,
};
const invoice = {
  status: 'REVISED',
  mobilization: 123456,
  rate: 456789,
  qty: 987654,
  amount: 8500,
  description: 'Crane hire\nHarbor lift',
  customerName: 'Atlas',
  craneNumber: 'CR-01',
  jobCardNumber: 'JC-01',
  invoiceNumber: 'INV-01',
  invoiceDate: '2026-10-01',
};
const maintenance = {
  maintenanceDate: '1999-01-01',
  status: 'SCHEDULED',
  maintenancePerson: null,
  endDate: null,
  startDate: '2026-10-01',
  repairType: 'Hydraulics',
  craneType: 'Mobile',
  craneNumber: 'CR-01',
  maintenanceNumber: 'MT-01',
};
const dataCases = [
  {
    category: 'Job Cards',
    title: 'Job Cards · General',
    row: job,
    cells: [
      'JC-01',
      'CR-01',
      'Atlas',
      'Harbor',
      '01 Oct 2026',
      '02 Oct 2026',
      'Admin Alex',
      'Morgan',
      'Pending',
      '12.5',
    ],
  },
  {
    category: 'Invoices',
    title: 'Invoices · General',
    row: invoice,
    cells: [
      '01 Oct 2026',
      'INV-01',
      'JC-01',
      'CR-01',
      'Atlas',
      'Crane hireHarbor lift',
      '8,500.00',
    ],
  },
  {
    category: 'Crane',
    title: 'Crane · Utilization',
    row: {
      totalWorkedHours: 12.5,
      totalJobs: 2,
      craneType: 'Mobile',
      craneNumber: 'CR-01',
    },
    cells: ['CR-01', 'Mobile', '2', '12.5'],
  },
  {
    category: 'Revenue',
    title: 'Revenue · Monthly',
    row: { ...summary, year: 2026, month: 10, monthName: 'October' },
    cells: ['2026', '10', 'October', '2', '8,500.00', '500.00', '8,500.00'],
  },
  {
    category: 'Revenue',
    title: 'Revenue · Customer',
    row: { ...summary, customerId: 1234, customerName: 'Atlas' },
    cells: ['1234', 'Atlas', '2', '8,500.00', '500.00', '8,500.00'],
  },
  {
    category: 'Revenue',
    title: 'Revenue · Crane',
    row: {
      ...summary,
      craneId: 1234,
      craneNumber: 'CR-01',
      craneType: 'Mobile',
    },
    cells: ['1234', 'CR-01', 'Mobile', '2', '8,500.00', '500.00', '8,500.00'],
  },
  {
    category: 'Maintenance',
    title: 'Maintenance · General',
    row: maintenance,
    cells: [
      'MT-01',
      'CR-01',
      'Mobile',
      'Hydraulics',
      '01 Oct 2026',
      '—',
      '—',
      'Scheduled',
    ],
  },
  {
    category: 'Maintenance',
    title: 'Maintenance · Summary',
    row: {
      craneId: 99999,
      totalMaintenanceDays: 3,
      totalMaintenanceRecords: 1,
      craneType: 'Mobile',
      craneNumber: 'CR-01',
    },
    cells: ['CR-01', 'Mobile', '1', '3'],
  },
  {
    category: 'Maintenance',
    title: 'Maintenance · Status',
    row: { total: 3, status: 'COMPLETED' },
    cells: ['Completed', '3'],
  },
];
it.each(dataCases)(
  'renders actual DTO values in explicit $title order, regardless of JSON key order',
  async (item) => {
    captureReports([item.row]);
    const user = await mount();
    await select(user, item.category, item.title);
    await user.click(screen.getByRole('button', { name: 'Run report' }));
    const table = screen.getByRole('table', { name: 'Report results' });
    await waitFor(() =>
      expect(within(table).getAllByRole('row')).toHaveLength(2),
    );
    expect(
      within(table)
        .getAllByRole('cell')
        .map((c) => c.textContent?.replace(/\n/g, '')),
    ).toEqual(item.cells);
  },
);

it.each([
  [
    'Job Cards',
    'Job Cards · General',
    'Job Date From',
    'Job Date To',
    '/job-cards',
    false,
  ],
  [
    'Invoices',
    'Invoices · Date',
    'From Date',
    'To Date',
    '/invoices/date',
    true,
  ],
  [
    'Revenue Summary',
    'Revenue Summary · Date',
    'From Date',
    'To Date',
    '/revenue-summary/date',
    true,
  ],
] as const)(
  'validates empty, partial, reversed and same-day ranges for %s',
  async (category, title, fromLabel, toLabel, path, required) => {
    const read = captureReports(category === 'Revenue Summary' ? summary : []);
    const user = await mount();
    await select(user, category, title);
    if (!required)
      expect(
        screen.getByText(/jobDate.*not scheduled or worked/),
      ).toBeVisible();
    if (required) {
      await user.click(screen.getByRole('button', { name: 'Run report' }));
      expect(
        screen.getByText('Enter From Date to complete the date range.'),
      ).toBeVisible();
      expect(
        screen.getByText('Enter To Date to complete the date range.'),
      ).toBeVisible();
      expect(read).not.toHaveBeenCalled();
    }
    changeDate(fromLabel, '2026-10-03');
    expect(
      screen.getByText('Enter To Date to complete the date range.'),
    ).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Run report' }));
    expect(read).not.toHaveBeenCalled();
    changeDate(fromLabel, '');
    changeDate(toLabel, '2026-10-02');
    expect(
      screen.getByText('Enter From Date to complete the date range.'),
    ).toBeVisible();
    await user.click(screen.getByRole('button', { name: 'Run report' }));
    expect(read).not.toHaveBeenCalled();
    changeDate(fromLabel, '2026-10-03');
    expect(
      screen.getByText('To Date cannot be earlier than From Date.'),
    ).toBeVisible();
    expect(screen.getByLabelText(toLabel, { exact: false })).toHaveAttribute(
      'aria-invalid',
      'true',
    );
    await user.click(screen.getByRole('button', { name: 'Run report' }));
    expect(read).not.toHaveBeenCalled();
    changeDate(toLabel, '2026-10-03');
    expect(
      screen.queryByText('To Date cannot be earlier than From Date.'),
    ).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Run report' }));
    await waitFor(() =>
      expect(read).toHaveBeenCalledExactlyOnceWith(
        `/api/reports${path}?fromDate=2026-10-03&toDate=2026-10-03`,
        expect.any(String),
      ),
    );
  },
);

it('keeps admin salespeople selectable and supports validated historical IDs', async () => {
  const read = captureReports([job]);
  const user = await mount('SALES');
  await select(user, 'Job Cards', 'Job Cards · Sales Person');
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  expect(
    screen.getByText('Select a record or enter a valid positive ID.'),
  ).toBeVisible();
  const input = screen.getByRole('combobox', { name: 'Sales Person' });
  for (const value of ['-1', '0', '1.5', 'unknown']) {
    fireEvent.change(input, { target: { value } });
    await user.click(screen.getByRole('button', { name: 'Run report' }));
    expect(read).not.toHaveBeenCalled();
  }
  fireEvent.change(input, { target: { value: '' } });
  await user.click(input);
  await user.keyboard('{ArrowDown}');
  await user.click(
    await screen.findByRole('option', { name: 'Historical admin (#42)' }),
  );
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  expect(await screen.findByText('JC-01')).toBeVisible();
  expect(read).toHaveBeenLastCalledWith(
    '/api/reports/job-cards/sales-person/42',
    expect.any(String),
  );
  fireEvent.change(input, { target: { value: '99' } });
  expect(screen.queryByText('JC-01')).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  await waitFor(() =>
    expect(read).toHaveBeenLastCalledWith(
      '/api/reports/job-cards/sales-person/99',
      expect.any(String),
    ),
  );
});

it.each([
  ['Job Cards', 'Job Cards · Status', ['Pending', 'Completed', 'Canceled']],
  [
    'Invoices',
    'Invoices · Status',
    ['Draft', 'Completed', 'Canceled', 'Revised'],
  ],
] as const)(
  'requires a supported status for %s',
  async (category, title, statuses) => {
    const read = captureReports();
    const user = await mount();
    await select(user, category, title);
    await user.click(screen.getByRole('button', { name: 'Run report' }));
    expect(screen.getByText('Select a status.')).toBeVisible();
    expect(read).not.toHaveBeenCalled();
    await user.click(screen.getByRole('combobox', { name: 'Status' }));
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(
      statuses,
    );
  },
);

it('clears incompatible filters, results and pagination without executing draft changes', async () => {
  const rows = Array.from({ length: 12 }, (_, i) => ({
    ...job,
    jobCardNumber: `JC-${i + 1}`,
  }));
  const read = captureReports(rows);
  const user = await mount();
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  await screen.findByText('JC-1');
  await user.click(screen.getByRole('button', { name: 'Go to next page' }));
  expect(screen.getByText('JC-6')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  await screen.findByText('JC-1');
  expect(read).toHaveBeenCalledTimes(2);
  await user.click(screen.getByRole('button', { name: 'Go to next page' }));
  changeDate('Job Date From', '2026-10-01');
  expect(screen.queryByText('JC-6')).not.toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Go to previous page' }),
  ).toBeDisabled();
  changeDate('Job Date To', '2026-10-31');
  expect(read).toHaveBeenCalledTimes(2);
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  await screen.findByText('JC-1');
  expect(read).toHaveBeenLastCalledWith(
    '/api/reports/job-cards?fromDate=2026-10-01&toDate=2026-10-31',
    expect.any(String),
  );
  await user.click(screen.getByRole('button', { name: 'Go to next page' }));
  await choose(user, 'Report Type', 'Job Cards · Status');
  expect(screen.queryByText('JC-6')).not.toBeInTheDocument();
  expect(screen.queryByLabelText('Job Date From')).not.toBeInTheDocument();
  await choose(user, 'Status', 'Pending');
  await choose(user, 'Report Category', 'Invoices');
  expect(
    screen.getByRole('combobox', { name: 'Report Type' }),
  ).toHaveTextContent('Invoices · General');
  expect(
    screen.queryByRole('combobox', { name: 'Status' }),
  ).not.toBeInTheDocument();
  await select(user, 'Job Cards', 'Job Cards · General');
  expect(screen.getByLabelText('Job Date From')).toHaveValue('');
  expect(screen.getByLabelText('Job Date To')).toHaveValue('');
  expect(screen.getByText('Choose filters and run the report')).toBeVisible();
  expect(read).toHaveBeenCalledTimes(3);
  changeDate('Job Date From', '2026-10-01');
  await user.click(screen.getByRole('button', { name: 'Clear filters' }));
  expect(screen.getByLabelText('Job Date From')).toHaveValue('');
  expect(screen.getByRole('button', { name: 'Clear filters' })).toBeDisabled();
  expect(read).toHaveBeenCalledTimes(3);
});

it('cancels an in-flight report and never presents its late response under another selection', async () => {
  const gate = createRequestGate();
  const get = vi.spyOn(api, 'get');
  const invoiceRead = vi.fn();
  server.use(
    http.get('/api/reports/job-cards', async () => {
      await gate.wait();
      return HttpResponse.json([job]);
    }),
    http.get('/api/reports/invoices', () => {
      invoiceRead();
      return HttpResponse.json([invoice]);
    }),
  );
  const user = await mount();
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  try {
    await gate.started;
    expect(screen.getByText('Loading report…')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Run report' })).toBeDisabled();
    expect(headers()).toEqual(jobHeaders);
    await choose(user, 'Report Category', 'Invoices');
    expect(screen.getByRole('button', { name: 'Run report' })).toBeEnabled();
    expect(screen.getByText('Choose filters and run the report')).toBeVisible();
    expect(invoiceRead).not.toHaveBeenCalled();
    const signal = get.mock.calls.find(
      ([url]) => url === '/reports/job-cards',
    )?.[1]?.signal;
    expect(signal).toBeDefined();
    expect(signal?.aborted).toBe(true);
  } finally {
    await act(async () => gate.release());
    get.mockRestore();
  }
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  expect(await screen.findByText('INV-01')).toBeVisible();
  expect(headers()).toEqual(invoiceHeaders);
  expect(screen.queryByText('Morgan')).not.toBeInTheDocument();
  expect(invoiceRead).toHaveBeenCalledTimes(1);
});

it('keeps server failures actionable and clears old errors when the report changes', async () => {
  let failed = true;
  const read = vi.fn();
  server.use(
    http.get('/api/reports/job-cards', () => {
      read();
      return failed
        ? HttpResponse.json({ detail: 'Unavailable' }, { status: 503 })
        : HttpResponse.json([job]);
    }),
  );
  const user = await mount();
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  expect(await screen.findByText('Unable to load this content')).toBeVisible();
  failed = false;
  await user.click(screen.getByRole('button', { name: 'Retry' }));
  expect(await screen.findByText('JC-01')).toBeVisible();
  expect(read).toHaveBeenCalledTimes(2);
  failed = true;
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  expect(await screen.findByText('Unable to load this content')).toBeVisible();
  await choose(user, 'Report Category', 'Invoices');
  expect(
    screen.queryByText('Unable to load this content'),
  ).not.toBeInTheDocument();
  expect(screen.getByText('Choose filters and run the report')).toBeVisible();
});

it('honors a server 403 without exposing report data', async () => {
  server.use(
    http.get('/api/reports/job-cards', () =>
      HttpResponse.json({ detail: 'Forbidden' }, { status: 403 }),
    ),
  );
  const user = await mount();
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  expect(await screen.findByText('Access denied')).toBeVisible();
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
});

it('ends an expired report session on 401', async () => {
  server.use(
    http.get('/api/reports/job-cards', () =>
      HttpResponse.json({ detail: 'Unauthorized' }, { status: 401 }),
    ),
  );
  const user = await mount();
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  expect(await screen.findByRole('button', { name: 'Sign in' })).toBeVisible();
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
});

it('redirects anonymous report navigation to login without a report request', async () => {
  const read = captureReports();
  renderApp('/workspace/reports');
  expect(await screen.findByRole('button', { name: 'Sign in' })).toBeVisible();
  expect(
    screen.queryByRole('combobox', { name: 'Report Category' }),
  ).not.toBeInTheDocument();
  expect(read).not.toHaveBeenCalled();
});
