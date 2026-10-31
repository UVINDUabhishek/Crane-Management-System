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
  expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  expect(
    screen.queryByRole('columnheader', { name: 'Id' }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Completed' }));
  expect(screen.queryByText('JC-1')).not.toBeInTheDocument();
  expect(screen.getByText('JC-2')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Clear filters' }));
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
