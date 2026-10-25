import { describe, it, expect, vi } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Link } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse, delay } from 'msw';
import { AppRoutes, createQueryClient } from '../app/App';
import { AuthProvider } from '../features/auth/AuthProvider';
import { api, ApiError, normalizeError } from '../shared/api';
import { roles, type Role } from '../shared/permissions';
import { ThemeProvider } from '@mui/material';
import { createAppTheme } from '../app/theme';
import { server } from './setup';
const jwt = (seconds = 3600) =>
  `header.${btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + seconds }))}.signature`;
function mock(role: Role = 'ADMIN', seconds = 3600) {
  const token = jwt(seconds);
  server.use(
    http.post('/api/auth/login', () => HttpResponse.text(token)),
    http.get('/api/auth/me', ({ request }) => {
      expect(request.headers.get('authorization')).toBe(`Bearer ${token}`);
      return HttpResponse.json({
        id: 1,
        username: 'Alex',
        email: 'alex@example.com',
        role,
      });
    }),
    http.get('/api/dashboard', () =>
      HttpResponse.json({
        totalCranes: 14,
        availableCranes: 8,
        inUseCranes: 4,
        maintenanceCranes: 2,
        pendingJobs: 7,
        completedJobs: 30,
        canceledJobs: 1,
        totalInvoices: 22,
        draftInvoices: 3,
        completedInvoices: 17,
        revisedInvoices: 1,
        canceledInvoices: 1,
      }),
    ),
  );
  return token;
}
function mount(path = '/') {
  const cache = createQueryClient();
  render(
    <ThemeProvider theme={createAppTheme(false)}>
      <QueryClientProvider client={cache}>
        <AuthProvider>
          <MemoryRouter initialEntries={[path]}>
            <AppRoutes />
            <Link to="/workspace/users">Test direct users route</Link>
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>,
  );
  return cache;
}
async function signIn() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('Email address'), 'alex@example.com');
  await user.type(screen.getByLabelText('Password'), 'password');
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
  return user;
}
describe('session and routing', () => {
  it('protects routes, parses the raw token, loads identity and logs out', async () => {
    mock();
    mount('/workspace/users');
    const user = await signIn();
    expect(await screen.findByText('Operations at a glance')).toBeVisible();
    expect(screen.getByText('Alex')).toBeVisible();
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
    await user.click(screen.getByRole('button', { name: 'Logout' }));
    expect(
      await screen.findByRole('heading', { name: 'Welcome back' }),
    ).toBeVisible();
  });
  it('shows credential failure without exposing credentials or token', async () => {
    mock();
    server.use(
      http.post('/api/auth/login', () =>
        HttpResponse.json(
          { code: 'UNAUTHORIZED', detail: 'Invalid credentials' },
          { status: 401 },
        ),
      ),
    );
    mount();
    await signIn();
    expect(await screen.findByText(/Sign-in failed/)).toBeVisible();
    expect(screen.queryByText('Alex')).not.toBeInTheDocument();
  });
  it('validates fields before submitting', async () => {
    mount();
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));
    expect(
      await screen.findByText('Enter a valid email address.'),
    ).toBeVisible();
    expect(screen.getByText('Enter your password.')).toBeVisible();
  });
  it('maps backend validation errors onto the form', async () => {
    mock();
    server.use(
      http.post('/api/auth/login', () =>
        HttpResponse.json(
          {
            code: 'VALIDATION_FAILED',
            errors: [{ field: 'email', message: 'Email is not valid' }],
          },
          { status: 400 },
        ),
      ),
    );
    mount();
    await signIn();
    expect(await screen.findByText('Email is not valid')).toBeVisible();
  });
  it('clears session and cache on an authenticated 401', async () => {
    mock();
    const cache = mount();
    await signIn();
    await screen.findByText('Operations at a glance');
    server.use(
      http.get('/api/dashboard', () => new HttpResponse(null, { status: 401 })),
    );
    await act(async () => {
      await cache.invalidateQueries({ queryKey: ['dashboard'] });
    });
    expect(
      await screen.findByRole('heading', { name: 'Welcome back' }),
    ).toBeVisible();
    expect(
      screen.getByText('Your session has ended. Please sign in.'),
    ).toBeVisible();
  });
  it('expires an established session without another request', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      mock('ADMIN', 10);
      mount();
      await signIn();
      await screen.findByText('Operations at a glance');
      await act(async () => {
        await vi.advanceTimersByTimeAsync(11000);
      });
      expect(
        screen.getByRole('heading', { name: 'Welcome back' }),
      ).toBeVisible();
    } finally {
      vi.useRealTimers();
    }
  });
  it('rejects already expired tokens', async () => {
    mock('ADMIN', -1);
    mount();
    await signIn();
    expect(await screen.findByText(/Sign-in failed/)).toBeVisible();
  });
  it('shows loading while current user is fetched', async () => {
    mock();
    server.use(
      http.get('/api/auth/me', async () => {
        await delay(150);
        return HttpResponse.json({
          id: 1,
          username: 'Alex',
          email: 'alex@example.com',
          role: 'ADMIN',
        });
      }),
    );
    mount();
    await signIn();
    expect(
      await screen.findByRole('button', { name: 'Signing in…' }),
    ).toBeDisabled();
    expect(await screen.findByText('Operations at a glance')).toBeVisible();
  });
  it('handles identity failure without opening the workspace', async () => {
    mock();
    server.use(
      http.get('/api/auth/me', () => new HttpResponse(null, { status: 500 })),
    );
    mount();
    await signIn();
    expect(
      await screen.findByText('The service is unavailable. Please try again.'),
    ).toBeVisible();
    expect(screen.queryByText('Alex')).not.toBeInTheDocument();
  });
});
const expected: Record<Role, string[]> = {
  ADMIN: [
    'Cranes',
    'Customers',
    'Quotations',
    'Job cards',
    'Timesheets',
    'Maintenance',
    'Invoices',
    'Users',
  ],
  SALES: ['Cranes', 'Customers', 'Quotations', 'Job cards'],
  OPERATIONS: ['Customers', 'Job cards', 'Timesheets', 'Maintenance'],
  FINANCE: ['Invoices'],
  MAINTENANCE: ['Maintenance'],
};
describe('role navigation', () => {
  it.each(roles)('matches backend read permissions for %s', async (role) => {
    mock(role);
    mount();
    await signIn();
    await screen.findByText('Operations at a glance');
    for (const label of expected.ADMIN) {
      const link = screen.queryByRole('link', {
        name: new RegExp('^' + label),
      });
      if (expected[role].includes(label)) expect(link).toBeVisible();
      else expect(link).not.toBeInTheDocument();
    }
  });
  it('blocks a forbidden route without logging out', async () => {
    mock('SALES');
    mount();
    const user = await signIn();
    await screen.findByText('Operations at a glance');
    await user.click(
      screen.getByRole('link', { name: 'Test direct users route' }),
    );
    expect(await screen.findByText('Access denied')).toBeVisible();
    expect(screen.getByText('Alex')).toBeVisible();
  });
});
describe('dashboard', () => {
  it('renders counts and missing counts without inventing data', async () => {
    mock();
    server.use(
      http.get('/api/dashboard', () => HttpResponse.json({ totalCranes: 14 })),
    );
    mount();
    await signIn();
    expect(await screen.findByText('14')).toBeVisible();
    expect(screen.getAllByText('—')).toHaveLength(11);
  });
  it('shows loading then counts', async () => {
    mock();
    server.use(
      http.get('/api/dashboard', async () => {
        await delay(200);
        return HttpResponse.json({ totalCranes: 9 });
      }),
    );
    mount();
    await signIn();
    expect(await screen.findByText('Loading dashboard…')).toBeVisible();
    expect(
      screen.getByRole('heading', { name: 'Operations at a glance' }),
    ).toBeVisible();
    expect(await screen.findByText('9')).toBeVisible();
  });
  it('handles empty data', async () => {
    mock();
    server.use(http.get('/api/dashboard', () => HttpResponse.json(null)));
    mount();
    await signIn();
    expect(
      await screen.findByText('No dashboard data is available.'),
    ).toBeVisible();
    expect(
      screen.getByRole('heading', { name: 'Operations at a glance' }),
    ).toBeVisible();
  });
  it('sanitizes server errors and supports retry', async () => {
    mock();
    server.use(
      http.get('/api/dashboard', () =>
        HttpResponse.json({ detail: 'SQL password=secret' }, { status: 500 }),
      ),
    );
    mount();
    const user = await signIn();
    expect(
      await screen.findByText('The service is unavailable. Please try again.'),
    ).toBeVisible();
    expect(screen.queryByText(/SQL/)).not.toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Operations at a glance' }),
    ).toBeVisible();
    server.use(
      http.get('/api/dashboard', () => HttpResponse.json({ totalCranes: 3 })),
    );
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByText('3')).toBeVisible();
  });
  it('403 displays access denied and retains identity', async () => {
    mock();
    server.use(
      http.get('/api/dashboard', () => new HttpResponse(null, { status: 403 })),
    );
    mount();
    await signIn();
    expect(await screen.findByText('Access denied')).toBeVisible();
    expect(screen.getByText('Alex')).toBeVisible();
  });
});
describe('API normalization', () => {
  it.each([400, 409, 500])(
    'normalizes text ProblemDetail for %s',
    async (status) => {
      server.use(
        http.get('/api/problem', () =>
          HttpResponse.text(
            JSON.stringify({
              code: 'TEST',
              detail: 'Business detail',
              errors: [{ field: 'email', message: 'Check email' }],
            }),
            { status, headers: { 'Content-Type': 'application/problem+json' } },
          ),
        ),
      );
      const error = await api.get('/problem').catch(normalizeError);
      expect(error).toBeInstanceOf(ApiError);
      if (!(error instanceof ApiError)) throw new Error('Expected API failure');
      expect(error.status).toBe(status);
      expect(error.code).toBe('TEST');
      expect(error.message).toBe(
        status === 500
          ? 'The service is unavailable. Please try again.'
          : 'Business detail',
      );
    },
  );
  it('does not expose unexpected exception text', () => {
    expect(normalizeError(new Error('secret token')).message).not.toContain(
      'secret',
    );
  });
});
