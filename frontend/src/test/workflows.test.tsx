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
async function mount(
  module: string,
  role: Role = 'ADMIN',
  parentJobs: object[] = [],
  parentRead: () => Promise<void> = async () => {},
) {
  if (module === 'timesheets')
    server.use(
      http.get('/api/job-card', async () => {
        await parentRead();
        return HttpResponse.json(parentJobs);
      }),
    );
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
  await user.click(
    await screen.findByRole('button', { name: /View details for/ }),
  );
  expect(await screen.findByRole('dialog')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Close' }));
  await waitFor(() =>
    expect(
      screen.getByRole('button', { name: /View details for/ }),
    ).toHaveFocus(),
  );
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
  expect(
    screen.queryByRole('button', { name: /Actions for/ }),
  ).not.toBeInTheDocument();
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
  await user.click(screen.getByRole('combobox', { name: 'Report Category' }));
  expect(
    screen.queryByRole('option', { name: 'Revenue' }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole('option', { name: 'Revenue Summary' }));
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  expect(await screen.findAllByText('8,500.00')).toHaveLength(2);
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
  await user.click(screen.getByRole('combobox', { name: 'Report Type' }));
  await user.click(
    screen.getByRole('option', { name: 'Job Cards · Customer' }),
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
      await screen.findByRole('button', { name: /View details for/ }),
    );
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
      screen.getByRole('button', { name: 'View details for INV-readable' }),
    );
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

// Independent role/status expectations checked against controller/service rules.
it.each(['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE', 'MAINTENANCE'] as Role[])(
  'offers the exact workflow action matrix for %s',
  async (role) => {
    // Job lifecycle has no SALES completion permission.
    const states = ['PENDING', 'COMPLETED', 'CANCELED'];
    server.use(
      http.get('/api/job-card', () =>
        HttpResponse.json(
          states.map((status, i) => ({
            id: i + 1,
            jobCardNumber: `JC-${status}`,
            status,
          })),
        ),
      ),
    );
    const user = await mount('jobs', role);
    if (!['ADMIN', 'SALES', 'OPERATIONS'].includes(role)) {
      expect(await screen.findByText('Access denied')).toBeVisible();
      return;
    }
    for (const status of states) {
      await screen.findByRole('button', {
        name: `View details for JC-${status}`,
      });
      expect(
        !!screen.queryByRole('button', { name: `Edit JC-${status}` }),
      ).toBe(status === 'PENDING');
      const menu = screen.queryByRole('button', {
        name: `Actions for JC-${status}`,
      });
      if (status !== 'PENDING') {
        expect(menu).not.toBeInTheDocument();
        continue;
      }
      await user.click(menu!);
      expect(screen.getAllByRole('menuitem').map((e) => e.textContent)).toEqual(
        role === 'SALES' ? ['Cancel'] : ['Complete', 'Cancel'],
      );
      await user.keyboard('{Escape}');
    }
  },
);
it.each(['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE', 'MAINTENANCE'] as Role[])(
  'preserves invoice version/lifecycle actions for %s',
  async (role) => {
    const states = ['DRAFT', 'COMPLETED', 'REVISED', 'CANCELED'];
    server.use(
      http.get('/api/invoices', () =>
        HttpResponse.json(
          states.map((status, i) => ({
            id: i + 1,
            invoiceNumber: `INV-${status}`,
            status,
          })),
        ),
      ),
    );
    const user = await mount('invoices', role);
    if (!['ADMIN', 'FINANCE'].includes(role)) {
      expect(await screen.findByText('Access denied')).toBeVisible();
      return;
    }
    for (const status of states) {
      await screen.findByRole('button', {
        name: `View details for INV-${status}`,
      });
      expect(
        !!screen.queryByRole('button', { name: `Edit INV-${status}` }),
      ).toBe(status === 'DRAFT');
      const expected =
        status === 'DRAFT'
          ? ['Complete', 'Cancel']
          : status === 'COMPLETED'
            ? ['Cancel', 'Revise']
            : [];
      const menu = screen.queryByRole('button', {
        name: `Actions for INV-${status}`,
      });
      if (!expected.length) {
        expect(menu).not.toBeInTheDocument();
        continue;
      }
      await user.click(menu!);
      expect(screen.getAllByRole('menuitem').map((e) => e.textContent)).toEqual(
        expected,
      );
      await user.keyboard('{Escape}');
    }
  },
);
it.each(['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE', 'MAINTENANCE'] as Role[])(
  'enforces maintenance terminal immutability and role restrictions for %s',
  async (role) => {
    const states = ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELED'];
    server.use(
      http.get('/api/maintenance', () =>
        HttpResponse.json(
          states.map((status, i) => ({
            id: i + 1,
            maintenanceNumber: `MT-${status}`,
            status,
          })),
        ),
      ),
    );
    const user = await mount('maintenance', role);
    if (!['ADMIN', 'OPERATIONS', 'MAINTENANCE'].includes(role)) {
      expect(await screen.findByText('Access denied')).toBeVisible();
      return;
    }
    for (const status of states) {
      await screen.findByRole('button', {
        name: `View details for MT-${status}`,
      });
      expect(
        !!screen.queryByRole('button', { name: `Edit MT-${status}` }),
      ).toBe(['SCHEDULED', 'IN_PROGRESS'].includes(status));
      const expected =
        role === 'OPERATIONS'
          ? []
          : status === 'SCHEDULED'
            ? ['Start', 'Complete', 'Cancel']
            : status === 'IN_PROGRESS'
              ? ['Complete', 'Cancel']
              : [];
      const menu = screen.queryByRole('button', {
        name: `Actions for MT-${status}`,
      });
      if (!expected.length) {
        expect(menu).not.toBeInTheDocument();
        continue;
      }
      await user.click(menu!);
      expect(screen.getAllByRole('menuitem').map((e) => e.textContent)).toEqual(
        expected,
      );
      await user.keyboard('{Escape}');
    }
  },
);
it.each(['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE', 'MAINTENANCE'] as Role[])(
  'restricts timesheet changes by parent status for %s',
  async (role) => {
    const states = ['PENDING', 'COMPLETED', 'CANCELED', 'UNKNOWN'];
    server.use(
      http.get('/api/timesheets', () =>
        HttpResponse.json(
          states.map((state, i) => ({
            id: i + 1,
            jobCardId: i + 1,
            timesheetNumber: `TS-${state}`,
          })),
        ),
      ),
    );
    const user = await mount(
      'timesheets',
      role,
      states.slice(0, 3).map((status, i) => ({ id: i + 1, status })),
    );
    if (!['ADMIN', 'OPERATIONS'].includes(role)) {
      expect(await screen.findByText('Access denied')).toBeVisible();
      return;
    }
    await screen.findByRole('button', { name: 'Edit TS-PENDING' });
    for (const state of states) {
      expect(
        screen.getByRole('button', { name: `View details for TS-${state}` }),
      ).toBeVisible();
      expect(!!screen.queryByRole('button', { name: `Edit TS-${state}` })).toBe(
        state === 'PENDING',
      );
      const menu = screen.queryByRole('button', {
        name: `Actions for TS-${state}`,
      });
      if (state !== 'PENDING') {
        expect(menu).not.toBeInTheDocument();
        continue;
      }
      await user.click(menu!);
      expect(screen.getAllByRole('menuitem').map((e) => e.textContent)).toEqual(
        ['Delete'],
      );
      await user.keyboard('{Escape}');
    }
  },
);
it.each(
  ['quotations', 'users', 'operators'].flatMap((module) =>
    (['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE', 'MAINTENANCE'] as Role[]).map(
      (role) => ({ module, role }),
    ),
  ),
)(
  '$role keeps explicit account/commercial actions in $module',
  async ({ module, role }) => {
    if (module === 'operators')
      server.use(http.get('/api/admin/users', () => HttpResponse.json([])));
    const row =
      module === 'quotations'
        ? { id: 1, quotationNumber: 'QT-1' }
        : module === 'users'
          ? { id: 1, username: 'Sales user', role: 'SALES' }
          : { id: 1, operatorName: 'Morgan' };
    server.use(
      http.get('/api' + contracts[module].path, () => HttpResponse.json([row])),
    );
    const user = await mount(module === 'operators' ? 'users' : module, role);
    if (
      !(module === 'quotations' ? ['ADMIN', 'SALES'] : ['ADMIN']).includes(role)
    ) {
      expect(await screen.findByText('Access denied')).toBeVisible();
      return;
    }
    if (module === 'operators')
      await user.click(screen.getByRole('tab', { name: 'Operators' }));
    await screen.findByRole('button', { name: /View details for/ });
    expect(!!screen.queryByRole('button', { name: /^Edit / })).toBe(
      module !== 'operators',
    );
    await user.click(screen.getByRole('button', { name: /Actions for/ }));
    expect(screen.getAllByRole('menuitem').map((e) => e.textContent)).toEqual([
      module === 'operators' ? 'Delete' : 'Deactivate',
    ]);
  },
);
it('does not offer admin account deactivation', async () => {
  server.use(
    http.get('/api/admin/users', () =>
      HttpResponse.json([{ id: 1, username: 'Admin', role: 'ADMIN' }]),
    ),
  );
  await mount('users');
  await screen.findByRole('button', { name: 'View details for Admin' });
  expect(screen.getByRole('button', { name: 'Edit Admin' })).toBeVisible();
  expect(
    screen.queryByRole('button', { name: 'Actions for Admin' }),
  ).not.toBeInTheDocument();
});

it('loads parent jobs once for all timesheet rows and fails closed with retry', async () => {
  const { createRequestGate } = await import('./requestGate');
  const gate = createRequestGate();
  const read = vi.fn(() => gate.wait());
  const jobs = [1, 2, 3].map((id) => ({ id, status: 'PENDING' }));
  server.use(
    http.get('/api/timesheets', () =>
      HttpResponse.json(
        jobs.map((job) => ({
          id: job.id,
          jobCardId: job.id,
          timesheetNumber: `TS-${job.id}`,
        })),
      ),
    ),
  );
  const user = await mount('timesheets', 'ADMIN', jobs, read);
  try {
    await gate.started;
    await screen.findByRole('button', { name: 'View details for TS-1' });
    expect(
      screen.getAllByRole('button', { name: /View details for TS-/ }),
    ).toHaveLength(3);
    expect(
      screen.queryByRole('button', { name: /^Edit TS-/ }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /^Actions for TS-/ }),
    ).not.toBeInTheDocument();
    expect(read).toHaveBeenCalledTimes(1);
  } finally {
    gate.release();
  }
  await screen.findByRole('button', { name: 'Edit TS-1' });
  expect(screen.getAllByRole('button', { name: /^Edit TS-/ })).toHaveLength(3);
  server.use(
    http.get('/api/job-card', () =>
      HttpResponse.json({ detail: 'Unavailable' }, { status: 503 }),
    ),
  );
  await user.click(screen.getByRole('button', { name: 'Refresh Timesheets' }));
  expect(await screen.findByText('Unable to load this content')).toBeVisible();
  expect(
    screen.queryByRole('button', { name: /^Edit TS-/ }),
  ).not.toBeInTheDocument();
  expect(
    screen.getAllByRole('button', { name: /View details for TS-/ }),
  ).toHaveLength(3);
  server.use(http.get('/api/job-card', () => HttpResponse.json(jobs)));
  await user.click(screen.getByRole('button', { name: 'Retry' }));
  await screen.findByRole('button', { name: 'Edit TS-1' });
});
it('opens revision review without mutation and preserves the versioned endpoint on confirmation', async () => {
  const revise = vi.fn();
  const invoice = {
    id: 1,
    invoiceNumber: 'INV-1',
    status: 'COMPLETED',
    version: 1,
    qty: 8,
    rate: 100,
    mobilization: 50,
    description: 'Lift',
  };
  server.use(
    http.get('/api/invoices', () => HttpResponse.json([invoice])),
    http.get('/api/invoices/1', () => HttpResponse.json(invoice)),
    http.patch('/api/invoices/1/revise', async ({ request }) => {
      revise(await request.json());
      return HttpResponse.json({
        ...invoice,
        id: 2,
        version: 2,
        status: 'DRAFT',
      });
    }),
  );
  const user = await mount('invoices', 'FINANCE');
  await user.click(
    await screen.findByRole('button', { name: 'Actions for INV-1' }),
  );
  await user.click(screen.getByRole('menuitem', { name: 'Revise' }));
  const create = await screen.findByRole('button', { name: 'Create revision' });
  expect(screen.getByText(/creates a new draft revision/)).toBeVisible();
  expect(revise).not.toHaveBeenCalled();
  await user.click(create);
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(revise).toHaveBeenCalledExactlyOnceWith({
    qty: 8,
    rate: 100,
    mobilization: 50,
    description: 'Lift',
  });
});

it('restores focus when canceling deletion during parent revalidation', async () => {
  // Keep dialog transitions deterministic in this accessibility regression.
  // Preserve the existing viewport media queries.
  const originalMatchMedia = window.matchMedia;

  vi.stubGlobal('matchMedia', (query: string) => {
    const result = originalMatchMedia(query);

    return {
      ...result,
      matches: query === '(prefers-reduced-motion: reduce)' || result.matches,
    };
  });

  const { createRequestGate } = await import('./requestGate');
  const gate = createRequestGate(),
    remove = vi.fn();
  server.use(
    http.get('/api/timesheets', () =>
      HttpResponse.json([{ id: 1, jobCardId: 1, timesheetNumber: 'TS-1' }]),
    ),
    http.delete('/api/timesheets/1', async () => {
      remove();
      await gate.wait();
      return HttpResponse.json(
        { detail: 'Parent job is no longer pending.' },
        { status: 409 },
      );
    }),
  );
  const user = await mount('timesheets', 'OPERATIONS', [
    { id: 1, status: 'PENDING' },
  ]);
  await user.click(
    await screen.findByRole('button', { name: 'Actions for TS-1' }),
  );
  await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
  const dialog = await screen.findByRole('dialog');
  expect(
    within(dialog).getByText(/Permanently delete this timesheet/),
  ).toBeVisible();
  const parentGate = createRequestGate();
  const trigger = screen.getByRole('button', {
    name: 'Actions for TS-1',
    hidden: true,
  });
  server.use(
    http.get('/api/job-card', async () => {
      await parentGate.wait();
      return HttpResponse.json([{ id: 1, status: 'PENDING' }]);
    }),
  );
  // Simulate background revalidation while the confirmation owns focus.
  fireEvent.click(
    screen.getByRole('button', { name: 'Refresh Timesheets', hidden: true }),
  );
  try {
    await parentGate.started;
    await waitFor(() =>
      expect(trigger).toHaveAttribute('aria-disabled', 'true'),
    );
    expect(trigger).toBeInTheDocument();
    expect(
      screen.getByRole('button', {
        name: 'View details for TS-1',
        hidden: true,
      }),
    ).toBeEnabled();

    expect(
      within(dialog).getByRole('button', { name: 'Confirm' }),
    ).toBeDisabled();
    // Cancel while the parent request is still pending.
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));
    expect(remove).not.toHaveBeenCalled();

    await waitFor(() => expect(dialog).not.toBeInTheDocument(), {
      timeout: 5000,
    });
    await waitFor(() => expect(trigger).toHaveFocus());
    // ARIA-disabled keeps focusability, but activation must remain blocked.
    fireEvent.click(trigger);
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  } finally {
    parentGate.release();
  }
});

it('prevents duplicate timesheet deletion and retains the server lifecycle conflict', async () => {
  const { createRequestGate } = await import('./requestGate');
  const gate = createRequestGate(),
    remove = vi.fn();
  server.use(
    http.get('/api/timesheets', () =>
      HttpResponse.json([{ id: 1, jobCardId: 1, timesheetNumber: 'TS-1' }]),
    ),
    http.delete('/api/timesheets/1', async () => {
      remove();
      await gate.wait();
      return HttpResponse.json(
        { detail: 'Parent job is no longer pending.' },
        { status: 409 },
      );
    }),
  );
  const user = await mount('timesheets', 'OPERATIONS', [
    { id: 1, status: 'PENDING' },
  ]);
  await user.click(
    await screen.findByRole('button', { name: 'Actions for TS-1' }),
  );
  await user.click(screen.getByRole('menuitem', { name: 'Delete' }));
  await user.click(await screen.findByRole('button', { name: 'Confirm' }));
  try {
    await gate.started;
    expect(screen.getByRole('button', { name: 'Working…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeVisible();
    expect(remove).toHaveBeenCalledTimes(1);
  } finally {
    gate.release();
  }
  expect(
    await screen.findByText('Parent job is no longer pending.'),
  ).toBeVisible();
  expect(
    within(screen.getByRole('table', { hidden: true })).getByText('TS-1'),
  ).toBeInTheDocument();
});

it('retains maintenance edits after a concurrent terminal-state conflict', async () => {
  const record = {
    id: 1,
    maintenanceNumber: 'MT-1',
    status: 'SCHEDULED',
    craneId: 1,
    crane: 'CR-1',
    maintenanceStartDate: '2026-10-01',
    maintenanceEndDate: '2026-10-02',
    maintenanceType: 'SERVICE',
    maintenancePersonId: null,
    description: 'Original',
  };
  const update = vi.fn();
  server.use(
    http.get('/api/maintenance', () => HttpResponse.json([record])),
    http.get('/api/maintenance/1', () => HttpResponse.json(record)),
    http.get('/api/lookups/cranes', () =>
      HttpResponse.json([{ id: 1, label: 'CR-1', type: '080T' }]),
    ),
    http.get('/api/lookups/maintenance-people', () => HttpResponse.json([])),
    http.put('/api/maintenance/1', async ({ request }) => {
      update(await request.json());
      return HttpResponse.json(
        { detail: 'Completed or canceled maintenance cannot be updated' },
        { status: 409 },
      );
    }),
  );
  const user = await mount('maintenance');
  await user.click(await screen.findByRole('button', { name: 'Edit MT-1' }));
  const description = await screen.findByLabelText('Description', {
    exact: false,
  });
  fireEvent.change(description, { target: { value: 'Retained edits' } });
  await user.click(screen.getByRole('button', { name: 'Save' }));
  expect(
    await screen.findByText(
      'Completed or canceled maintenance cannot be updated',
    ),
  ).toBeVisible();
  expect(description).toHaveValue('Retained edits');
  expect(screen.getByRole('dialog')).toBeVisible();
  expect(update).toHaveBeenCalledTimes(1);
});

it.each([
  { source: 'jobs', target: 'timesheets', link: 'Timesheets' },
  { source: 'timesheets', target: 'maintenance', link: 'Maintenance' },
  { source: 'maintenance', target: 'jobs', link: 'Job cards' },
])(
  'isolates search and filters from $source when navigating to $target',
  async ({ source, target, link }) => {
    server.use(
      http.get('/api/job-card', () =>
        HttpResponse.json([
          { id: 1, jobCardNumber: 'JC-1', status: 'PENDING' },
        ]),
      ),
      http.get('/api/timesheets', () => HttpResponse.json([])),
      http.get('/api/maintenance', () =>
        HttpResponse.json([
          { id: 1, maintenanceNumber: 'MT-1', status: 'SCHEDULED' },
        ]),
      ),
    );
    const user = await mount(source);
    fireEvent.change(screen.getByRole('textbox', { name: 'Search records' }), {
      target: { value: 'unique source search' },
    });
    if (source !== 'timesheets')
      await user.click(
        screen.getByRole('button', {
          name: source === 'jobs' ? 'Pending' : 'Scheduled',
        }),
      );
    await user.click(screen.getByRole('link', { name: link }));
    expect(
      await screen.findByRole('textbox', { name: 'Search records' }),
    ).toHaveValue('');
    if (target !== 'timesheets')
      expect(screen.getByRole('button', { name: 'All statuses' })).toHaveClass(
        'MuiChip-filled',
      );
  },
);
