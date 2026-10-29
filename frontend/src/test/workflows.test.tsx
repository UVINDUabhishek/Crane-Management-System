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
  await user.click(await screen.findByRole('button', { name: 'Details' }));
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
  await user.click(await screen.findByRole('button', { name: 'Complete' }));
  await user.click(
    within(screen.getByRole('dialog')).getByRole('button', { name: 'Confirm' }),
  );
  await screen.findByText('COMPLETED');
  expect(done).toHaveBeenCalledTimes(1);
  expect(
    screen.queryByRole('button', { name: 'Complete' }),
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
  await user.click(await screen.findByRole('button', { name: 'Complete' }));
  await user.click(screen.getByRole('button', { name: 'Confirm' }));
  expect(await screen.findByText('Conflict from server')).toBeVisible();
  expect(screen.getByText('DRAFT')).toBeInTheDocument();
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
  await user.click(await screen.findByRole('button', { name: 'Add Users' }));
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
    screen.queryByRole('option', { name: 'revenue monthly' }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole('option', { name: 'revenue summary' }));
  await user.click(screen.getByRole('button', { name: 'Run report' }));
  expect(await screen.findAllByText('8500')).toHaveLength(2);
});
