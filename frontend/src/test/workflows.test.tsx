import { expect, it, vi } from 'vitest';
import {
  render,
  screen,
  within,
  waitFor,
  fireEvent,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { ThemeProvider } from '@mui/material';
import { MemoryRouter, Link } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { AppRoutes, createQueryClient } from '../app/App';
import { createAppTheme } from '../app/theme';
import { AuthProvider } from '../features/auth/AuthProvider';
import { server } from './setup';
import type { Role } from '../shared/permissions';
import { contracts } from '../features/workflows/contracts';
async function mount(module: string, role: Role = 'ADMIN') {
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
  );
  const cache = createQueryClient();
  render(
    <ThemeProvider theme={createAppTheme(false)}>
      <QueryClientProvider client={cache}>
        <AuthProvider>
          <MemoryRouter>
            <AppRoutes />
            <Link to={`/workspace/${module}`}>Direct crane route</Link>
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>,
  );
  const user = userEvent.setup();
  // Exercise real authentication, without replaying every setup keystroke.
  fireEvent.change(screen.getByLabelText('Email address'), {
    target: { value: 'alex@example.com' },
  });
  fireEvent.change(screen.getByLabelText('Password'), {
    target: { value: 'password' },
  });
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
  await screen.findByRole('heading', { name: 'Operations at a glance' });
  await user.click(screen.getByRole('link', { name: 'Direct crane route' }));
  return user;
}

it.each([
  'quotations',
  'jobs',
  'timesheets',
  'maintenance',
  'invoices',
  'users',
])('loads real %s rows and details', async (module) => {
  const c = contracts[module];
  server.use(
    http.get('/api' + c.path, () =>
      HttpResponse.json([
        {
          id: 1,
          description: 'Record detail',
          username: 'Example',
          status: 'PENDING',
        },
      ]),
    ),
    http.get('/api' + c.path + '/1', () =>
      HttpResponse.json({
        id: 1,
        description: 'Record detail',
        username: 'Example',
        status: 'PENDING',
      }),
    ),
  );
  const user = await mount(module);
  await user.click(await screen.findByRole('button', { name: /Actions for/ }));
  await user.click(screen.getByRole('menuitem', { name: 'Details' }));
  expect(await screen.findByRole('dialog')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Close' }));
});
it.each(['quotations', 'jobs', 'timesheets', 'invoices', 'users'])(
  'rejects maintenance role for %s without fetching',
  async (module) => {
    const get = vi.fn();
    server.use(
      http.get('/api' + contracts[module].path, () => {
        get();
        return HttpResponse.json([]);
      }),
    );
    await mount(module, 'MAINTENANCE');
    expect(await screen.findByText('Access denied')).toBeVisible();
    expect(get).not.toHaveBeenCalled();
  },
);
it('completes a job only through the dedicated endpoint and refreshes', async () => {
  let status = 'PENDING';
  const done = vi.fn();
  server.use(
    http.get('/api/job-card', () =>
      HttpResponse.json([{ id: 4, status, jobCardNumber: 'JC-4' }]),
    ),
    http.patch('/api/job-card/4/complete', () => {
      done();
      status = 'COMPLETED';
      return HttpResponse.json({ id: 4, status });
    }),
  );
  const user = await mount('jobs');
  await user.click(await screen.findByRole('button', { name: /Actions for/ }));
  await user.click(screen.getByRole('menuitem', { name: 'Complete' }));
  await user.click(
    within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }),
  );
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  await within(screen.getByRole('table')).findByText('Completed');
  await user.click(screen.getByRole('button', { name: /Actions for/ }));
  expect(done).toHaveBeenCalledTimes(1);
  expect(
    screen.queryByRole('menuitem', { name: 'Complete' }),
  ).not.toBeInTheDocument();
});
it('retains lifecycle error and record on server conflict', async () => {
  server.use(
    http.get('/api/invoices', () =>
      HttpResponse.json([{ id: 2, status: 'DRAFT' }]),
    ),
    http.patch('/api/invoices/2/complete', () =>
      HttpResponse.json({ detail: 'Conflict from server' }, { status: 409 }),
    ),
  );
  const user = await mount('invoices');
  await user.click(await screen.findByRole('button', { name: /Actions for/ }));
  await user.click(screen.getByRole('menuitem', { name: 'Complete' }));
  await user.click(screen.getByRole('button', { name: 'Confirm' }));
  expect(await screen.findByText('Conflict from server')).toBeVisible();
  expect(
    within(screen.getByRole('table', { hidden: true })).getByText('Draft'),
  ).toBeInTheDocument();
});
it('validates and submits user fields without server controlled attributes', async () => {
  const post = vi.fn();
  server.use(
    http.get('/api/admin/users', () => HttpResponse.json([])),
    http.post('/api/admin/users', async ({ request }) => {
      post(await request.json());
      return HttpResponse.json({ id: 3 });
    }),
  );
  const user = await mount('users');
  await user.click(await screen.findByRole('button', { name: 'Add User' }));
  await user.click(screen.getByRole('button', { name: 'Save' }));
  expect(await screen.findAllByText('This field is required.')).toHaveLength(4);
  expect(post).not.toHaveBeenCalled();
  for (const [name, value] of [
    ['Username', 'Alex'],
    ['Email', 'alex@example.com'],
    ['Password', 'Secret123!'],
  ])
    fireEvent.change(screen.getByLabelText(name, { exact: false }), {
      target: { value },
    });
  await user.click(screen.getByRole('combobox', { name: /Role/ }));
  await user.click(screen.getByRole('option', { name: 'Sales' }));
  await user.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(post).toHaveBeenCalledWith({
    username: 'Alex',
    email: 'alex@example.com',
    password: 'Secret123!',
    role: 'SALES',
  });
});
it('renders server revenue and restricts report options', async () => {
  server.use(
    http.get('/api/reports/job-cards', () => HttpResponse.json([])),
    http.get('/api/reports/revenue-summary', () =>
      HttpResponse.json({
        totalInvoices: 1,
        totalAmount: 8500,
        totalMobilization: 500,
        totalRevenue: 8500,
      }),
    ),
  );
  const user = await mount('reports', 'OPERATIONS');
  await user.click(screen.getByRole('combobox', { name: 'Report' }));
  expect(
    screen.queryByRole('option', { name: 'Revenue · monthly' }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole('option', { name: 'Revenue summary' }));
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  expect(await screen.findAllByText('8,500.00')).toHaveLength(4);
});

it('filters and resets the job directory while summaries retain server totals', async () => {
  server.use(
    http.get('/api/job-card', () =>
      HttpResponse.json([
        {
          id: 1,
          jobCardNumber: 'JC-1',
          customer: 'Harbor works',
          status: 'PENDING',
        },
        {
          id: 2,
          jobCardNumber: 'JC-2',
          customer: 'Tower works',
          status: 'COMPLETED',
        },
      ]),
    ),
  );
  const user = await mount('jobs');
  expect(await screen.findByText('JC-1')).toBeVisible();
  expect(screen.getByText('Total Jobs')).toBeVisible();
  expect(screen.getByRole('button', { name: 'Clear filters' })).toBeDisabled();
  expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  expect(
    screen.queryByRole('columnheader', { name: 'Id' }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Completed' }));
  expect(screen.queryByText('JC-1')).not.toBeInTheDocument();
  expect(screen.getByText('JC-2')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Clear filters' }));
  expect(screen.getByRole('button', { name: 'Clear filters' })).toBeDisabled();
  fireEvent.change(screen.getByRole('textbox', { name: 'Search records' }), {
    target: { value: 'Harbor' },
  });
  expect(screen.getByText('JC-1')).toBeVisible();
  expect(screen.queryByText('JC-2')).not.toBeInTheDocument();
});

it('restricts sales job menus and preserves safe cancellation', async () => {
  const cancel = vi.fn();
  server.use(
    http.get('/api/job-card', () =>
      HttpResponse.json([{ id: 1, jobCardNumber: 'JC-1', status: 'PENDING' }]),
    ),
    http.patch('/api/job-card/1/cancel', () => {
      cancel();
      return HttpResponse.json({});
    }),
  );
  const user = await mount('jobs', 'SALES');
  await user.click(
    await screen.findByRole('button', { name: 'Actions for JC-1' }),
  );
  expect(
    screen.queryByRole('menuitem', { name: 'Complete' }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole('menuitem', { name: 'Cancel' }));
  await user.click(
    within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }),
  );
  expect(cancel).not.toHaveBeenCalled();
  expect(screen.getByText('JC-1')).toBeVisible();
});

it('uses named report lookups and sends the selected stable ID', async () => {
  const read = vi.fn();
  server.use(
    http.get('/api/lookups/customers', () =>
      HttpResponse.json([{ id: 42, label: 'Harbor works' }]),
    ),
    http.get('/api/reports/job-cards/customer/42', () => {
      read();
      return HttpResponse.json([]);
    }),
  );
  const user = await mount('reports', 'OPERATIONS');
  await user.click(screen.getByRole('combobox', { name: 'Report' }));
  await user.click(
    screen.getByRole('option', { name: 'Job cards · customer' }),
  );
  await user.click(screen.getByRole('combobox', { name: 'Customer' }));
  await user.click(
    await screen.findByRole('option', { name: 'Harbor works (#42)' }),
  );
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  await waitFor(() => expect(read).toHaveBeenCalledTimes(1));
});

it('switches administration tabs without sharing directory state', async () => {
  server.use(
    http.get('/api/admin/users', () => HttpResponse.json([])),
    http.get('/api/admin/operators', () =>
      HttpResponse.json([{ id: 9, operatorName: 'Morgan' }]),
    ),
  );
  const user = await mount('users');
  await user.click(screen.getByRole('tab', { name: 'Operators' }));
  expect(await screen.findByText('Morgan')).toBeVisible();
  expect(screen.getByRole('tab', { name: 'Operators' })).toHaveAttribute(
    'aria-selected',
    'true',
  );
  expect(screen.getByRole('button', { name: 'Add Operator' })).toBeVisible();
});

it('locks job completion while the request is pending and refreshes after success', async () => {
  const { createRequestGate } = await import('./requestGate');
  const gate = createRequestGate();
  let status = 'PENDING';
  const done = vi.fn();
  server.use(
    http.get('/api/job-card', () =>
      HttpResponse.json([{ id: 1, jobCardNumber: 'JC-1', status }]),
    ),
    http.patch('/api/job-card/1/complete', async () => {
      done();
      await gate.wait();
      status = 'COMPLETED';
      return HttpResponse.json({ id: 1, status });
    }),
  );
  const user = await mount('jobs');
  await user.click(
    await screen.findByRole('button', { name: 'Actions for JC-1' }),
  );
  await user.click(screen.getByRole('menuitem', { name: 'Complete' }));
  const dialog = screen.getByRole('dialog');
  expect(within(dialog).getByText(/generates an invoice/)).toBeVisible();
  await user.click(within(dialog).getByRole('button', { name: 'Confirm' }));
  try {
    await gate.started;
    expect(
      within(dialog).getByRole('button', { name: 'Working…' }),
    ).toBeDisabled();
    expect(
      within(dialog).getByRole('button', { name: 'Cancel' }),
    ).toBeDisabled();
    await user.keyboard('{Escape}');
    expect(dialog).toBeVisible();
    expect(done).toHaveBeenCalledTimes(1);
  } finally {
    gate.release();
  }
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(
    within(screen.getByRole('table')).getByText('Completed'),
  ).toBeVisible();
});

it.each(['jobs', 'timesheets', 'maintenance', 'quotations', 'users'])(
  'hides internal IDs but retains readable %s detail references',
  async (module) => {
    const c = contracts[module];
    const row = {
      id: 99101,
      customerId: 99102,
      jobCardId: 99103,
      craneId: 99104,
      quotationId: 99105,
      operatorId: 99106,
      salesPersonId: 99107,
      maintenancePersonId: 99108,
      jobCardNumber: 'JC-readable',
      customer: 'Atlas',
      customerName: 'Atlas',
      crane: 'CR-readable',
      operator: 'Operator readable',
      maintenancePerson: 'Assignee readable',
      quotation: 'QT-readable',
      username: 'User readable',
      description: 'Full description\nSecond line',
      status: 'PENDING',
    };
    server.use(
      http.get('/api' + c.path, () => HttpResponse.json([row])),
      http.get('/api' + c.path + '/99101', () => HttpResponse.json(row)),
    );
    const user = await mount(module);
    if (module === 'jobs')
      expect(
        screen.queryByRole('columnheader', { name: 'Operator' }),
      ).not.toBeInTheDocument();
    if (module === 'quotations')
      expect(
        screen.getByRole('columnheader', { name: 'Valid Period' }),
      ).toBeVisible();
    await user.click(
      await screen.findByRole('button', { name: /Actions for/ }),
    );
    await user.click(screen.getByRole('menuitem', { name: 'Details' }));
    const dialog = await screen.findByRole('dialog');
    await within(dialog).findByText(
      module === 'users' ? 'User readable' : /Full description/,
    );
    for (const value of Object.values(row).filter((v) => typeof v === 'number'))
      expect(within(dialog).queryByText(String(value))).not.toBeInTheDocument();
    for (const key of c.columns.filter((k) => k === 'id' || k.endsWith('Id')))
      expect(
        within(dialog).queryByText(
          key.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase()),
        ),
      ).not.toBeInTheDocument();
    if (module === 'jobs')
      expect(within(dialog).getByText('Operator readable')).toBeVisible();
    if (module === 'timesheets')
      expect(within(dialog).getByText('JC-readable')).toBeVisible();
    if (module === 'maintenance')
      expect(within(dialog).getByText('Assignee readable')).toBeVisible();
    if (module === 'users') expect(dialog).toHaveAccessibleName('User details');
  },
);

it.each([
  [1, 'Original'],
  [2, 'Revision 1'],
  [3, 'Revision 2'],
] as const)(
  'renders invoice version %s as %s separately from status',
  async (version, versionLabel) => {
    const row = {
      id: 99101,
      customerId: 99102,
      craneId: 99103,
      jobCardId: 99104,
      quotationId: 99105,
      invoiceNumber: 'INV-readable',
      invoiceDate: '2026-10-31',
      status: 'COMPLETED',
      version,
      customer: 'Atlas',
      crane: 'CR-readable',
      jobCard: 'JC-readable',
      quotation: 'QT-readable',
      qty: 8,
      rate: 1000,
      mobilization: 500,
      amount: 8500,
      description: 'Lift installation',
    };
    server.use(
      http.get('/api/invoices', () => HttpResponse.json([row])),
      http.get('/api/invoices/99101', () => HttpResponse.json(row)),
    );
    const user = await mount('invoices');
    expect(await screen.findByText(versionLabel)).toBeVisible();
    await user.click(
      screen.getByRole('button', { name: 'Actions for INV-readable' }),
    );
    await user.click(screen.getByRole('menuitem', { name: 'Details' }));
    const dialog = await screen.findByRole('dialog');
    await within(dialog).findByText('INV-readable');
    for (const value of [
      'Atlas',
      'CR-readable',
      'JC-readable',
      'QT-readable',
      versionLabel,
      'Completed',
      '31 Oct 2026',
      '8',
      '1,000.00',
      '500.00',
      '8,500.00',
      'Lift installation',
    ])
      expect(within(dialog).getByText(value)).toBeVisible();
    for (const id of [99101, 99102, 99103, 99104, 99105])
      expect(within(dialog).queryByText(String(id))).not.toBeInTheDocument();
    expect(within(dialog).getByText('Includes mobilization')).toBeVisible();
    expect(within(dialog).queryByRole('textbox')).not.toBeInTheDocument();
  },
);

it('confirms permanent operator deletion safely and retains referenced operators on conflict', async () => {
  const remove = vi.fn();
  server.use(
    http.get('/api/admin/users', () => HttpResponse.json([])),
    http.get('/api/admin/operators', () =>
      HttpResponse.json([{ id: 7, operatorName: 'Morgan' }]),
    ),
    http.delete('/api/admin/operators/7', () => {
      remove();
      return HttpResponse.json(
        { detail: 'Operator is referenced by a job card.' },
        { status: 409 },
      );
    }),
  );
  const user = await mount('users');
  await user.click(screen.getByRole('tab', { name: 'Operators' }));
  await user.click(
    await screen.findByRole('button', { name: 'Actions for Morgan' }),
  );
  expect(
    screen.queryByRole('menuitem', { name: 'Deactivate' }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
  let dialog = screen.getByRole('dialog', { name: 'Delete Morgan?' });
  expect(within(dialog).getByText(/cannot be undone/)).toBeVisible();
  await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));
  expect(remove).not.toHaveBeenCalled();
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  await user.click(screen.getByRole('button', { name: 'Actions for Morgan' }));
  await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
  dialog = screen.getByRole('dialog');
  await user.click(within(dialog).getByRole('button', { name: 'Confirm' }));
  expect(
    await within(dialog).findByText('Operator is referenced by a job card.'),
  ).toBeVisible();
  expect(remove).toHaveBeenCalledTimes(1);
  expect(
    within(screen.getByRole('table', { hidden: true })).getByText('Morgan'),
  ).toBeInTheDocument();
});

it('retains quotation soft-deactivation wording', async () => {
  server.use(
    http.get('/api/quotations', () =>
      HttpResponse.json([{ id: 1, quotationNumber: 'QT-1' }]),
    ),
  );
  const user = await mount('quotations');
  await user.click(
    await screen.findByRole('button', { name: 'Actions for QT-1' }),
  );
  expect(screen.getByRole('menuitem', { name: 'Deactivate' })).toBeVisible();
  expect(
    screen.queryByRole('menuitem', { name: 'Delete' }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole('menuitem', { name: 'Deactivate' }));
  expect(
    within(screen.getByRole('dialog')).getByText(
      /Existing records are retained/,
    ),
  ).toBeVisible();
});

it('removes a successfully deleted operator from the refreshed directory', async () => {
  let rows = [{ id: 7, operatorName: 'Morgan' }];
  const deleted = vi.fn();
  server.use(
    http.get('/api/admin/users', () => HttpResponse.json([])),
    http.get('/api/admin/operators', () => HttpResponse.json(rows)),
    http.delete('/api/admin/operators/7', () => {
      deleted();
      rows = [];
      return new HttpResponse(null, { status: 204 });
    }),
  );
  const user = await mount('users');
  await user.click(screen.getByRole('tab', { name: 'Operators' }));
  await user.click(
    await screen.findByRole('button', { name: 'Actions for Morgan' }),
  );
  await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
  await user.click(
    within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }),
  );
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(deleted).toHaveBeenCalledTimes(1);
  expect(screen.queryByText('Morgan')).not.toBeInTheDocument();
  expect(screen.getByText('No matching records')).toBeVisible();
});
