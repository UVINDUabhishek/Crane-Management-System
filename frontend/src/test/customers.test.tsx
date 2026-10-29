import { beforeEach, expect, it, vi } from 'vitest';
import { render, screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse, delay } from 'msw';
import { ThemeProvider } from '@mui/material';
import { MemoryRouter, Link } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { AppRoutes, createQueryClient } from '../app/App';
import { createAppTheme } from '../app/theme';
import { AuthProvider } from '../features/auth/AuthProvider';
import { server } from './setup';
import {
  customerSchema,
  customerFields,
  formDefaults,
} from '../features/customers/customerSchema';
import {
  customersApi,
  type CustomerResponse,
} from '../features/customers/customersApi';
import { session } from '../shared/api';
import type { Role } from '../shared/permissions';
const customer: CustomerResponse = {
  id: 42,
  active: true,
  companyName: 'Atlas Lifting',
  registrationNo: 'REG-42',
  vatNo: 'VAT-42',
  companyAddress: 'Colombo',
  billAddress: 'Galle',
  contactPerson: 'Sam',
  contactNo: '0771234567',
  customerEmail: 'sam@example.com',
};
let records: CustomerResponse[];
let listRequests: number;
beforeEach(() => {
  records = [{ ...customer }];
  listRequests = 0;
  server.use(
    http.get('/api/customers', ({ request }) => {
      expect(new URL(request.url).search).toBe('');
      listRequests++;
      return HttpResponse.json(records);
    }),
    http.get('/api/customers/:id', ({ params }) => {
      const c = records.find((c) => c.id === Number(params.id));
      return c
        ? HttpResponse.json(c)
        : HttpResponse.json({ detail: 'Customer not found' }, { status: 404 });
    }),
  );
});
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
  );
  const cache = createQueryClient();
  render(
    <ThemeProvider theme={createAppTheme(false)}>
      <QueryClientProvider client={cache}>
        <AuthProvider>
          <MemoryRouter>
            <AppRoutes />
            <Link to="/workspace/customers">Direct customer route</Link>
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>,
  );
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('Email address'), 'alex@example.com');
  await user.type(screen.getByLabelText('Password'), 'password');
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
  await screen.findByRole('heading', { name: 'Operations at a glance' });
  await user.click(screen.getByRole('link', { name: 'Direct customer route' }));
  return user;
}
async function action(user: ReturnType<typeof userEvent.setup>, name: string) {
  if (name === 'Deactivate') {
    await user.click(
      await screen.findByRole('switch', {
        name: 'Activation for Atlas Lifting',
      }),
    );
    return await screen.findByRole('dialog');
  }
  await user.click(
    await screen.findByRole('button', { name: 'Actions for Atlas Lifting' }),
  );
  await user.click(screen.getByRole('menuitem', { name }));
  return await screen.findByRole('dialog');
}
async function fill(user: ReturnType<typeof userEvent.setup>) {
  for (const [key, label] of Object.entries(customerFields)) {
    await user.type(
      screen.getByLabelText(label, { exact: false }),
      String(customer[key as keyof CustomerResponse]),
    );
  }
}
it.each(['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE', 'MAINTENANCE'] as Role[])(
  'enforces customer access for %s',
  async (role) => {
    const user = await mount(role);
    if (role === 'FINANCE' || role === 'MAINTENANCE') {
      expect(await screen.findByText('Access denied')).toBeVisible();
      expect(listRequests).toBe(0);
      expect(
        screen.queryByRole('button', { name: 'Add customer' }),
      ).not.toBeInTheDocument();
      return;
    }
    await screen.findByText('Atlas Lifting');
    expect(
      screen.queryByRole('button', { name: 'Add customer' }) !== null,
    ).toBe(role !== 'OPERATIONS');
    await user.click(
      screen.getByRole('button', { name: 'Actions for Atlas Lifting' }),
    );
    expect(
      screen.getByRole('menuitem', { name: 'View details' }),
    ).toBeVisible();
    expect(screen.queryByRole('menuitem', { name: 'Edit' }) !== null).toBe(
      role !== 'OPERATIONS',
    );
    expect(
      !screen
        .getByRole('switch', {
          name: 'Activation for Atlas Lifting',
          hidden: true,
        })
        .hasAttribute('disabled'),
    ).toBe(role !== 'OPERATIONS');
  },
);
it('loads nullable rows, displays details and restores row focus', async () => {
  records = [
    {
      ...customer,
      contactPerson: null,
      contactNo: null,
      customerEmail: null,
      billAddress: null,
    },
  ];
  const user = await mount();
  const dialog = await action(user, 'View details');
  expect(await within(dialog).findAllByText('Not provided')).toHaveLength(4);
  expect(within(dialog).getByText('Identification')).toBeVisible();
  await user.click(within(dialog).getByRole('button', { name: 'Close' }));
  await waitFor(() =>
    expect(
      screen.getByRole('button', { name: 'Actions for Atlas Lifting' }),
    ).toHaveFocus(),
  );
  expect(screen.queryByText(/Example company/)).not.toBeInTheDocument();
  expect(
    screen.getByRole('switch', { name: 'Activation for Atlas Lifting' }),
  ).toBeChecked();
});
it.each([
  'companyName',
  'registrationNo',
  'vatNo',
  'contactPerson',
  'contactNo',
  'customerEmail',
] as const)('searches %s with trimming and case folding', async (key) => {
  const user = await mount();
  await screen.findByText('Atlas Lifting');
  await user.type(
    screen.getByRole('textbox', { name: 'Search customers' }),
    `  ${customer[key]!.toUpperCase()}  `,
  );
  expect(screen.getByText('Atlas Lifting')).toBeVisible();
  await user.clear(screen.getByRole('textbox', { name: 'Search customers' }));
  await user.type(
    screen.getByRole('textbox', { name: 'Search customers' }),
    'absent',
  );
  expect(screen.getByText('No matching customers')).toBeVisible();
  await user.click(screen.getAllByRole('button', { name: 'Clear filters' })[0]);
  expect(screen.getByText('Atlas Lifting')).toBeVisible();
});
it('paginates, resets filters and bounds pages after refreshed data shrinks', async () => {
  records = Array.from({ length: 6 }, (_, i) => ({
    ...customer,
    id: i + 1,
    companyName: `Company ${i + 1}`,
    customerEmail: i === 5 ? null : 'a@example.com',
  }));
  const user = await mount();
  await screen.findByText('Company 1');
  await user.click(screen.getByRole('button', { name: 'Go to next page' }));
  expect(screen.getByText('Company 6')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Has contact email' }));
  expect(screen.queryByText('Company 6')).not.toBeInTheDocument();
  expect(screen.getByText('1–5 of 5')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Clear filters' }));
  await user.click(screen.getByRole('button', { name: 'Go to next page' }));
  records = [records[0]];
  await user.click(screen.getByRole('button', { name: 'Refresh customers' }));
  expect(await screen.findByText('1–1 of 1')).toBeVisible();
});
it('shows empty and loading states', async () => {
  server.use(
    http.get('/api/customers', async () => {
      await delay(200);
      return HttpResponse.json([]);
    }),
  );
  await mount();
  expect(await screen.findByText('Loading customers…')).toBeVisible();
  expect(await screen.findByText('No customers yet')).toBeVisible();
});
it('retries safe fetch errors', async () => {
  server.use(
    http.get('/api/customers', () =>
      HttpResponse.json({ detail: 'SQL secret' }, { status: 500 }),
    ),
  );
  const user = await mount();
  await screen.findByText('The service is unavailable. Please try again.');
  expect(screen.queryByText(/SQL/)).not.toBeInTheDocument();
  server.use(http.get('/api/customers', () => HttpResponse.json(records)));
  await user.click(screen.getByRole('button', { name: 'Retry' }));
  expect(await screen.findByText('Atlas Lifting')).toBeVisible();
});
it('creates with exact trimmed fields, preserves zero and refreshes cache', async () => {
  const posted = vi.fn();
  server.use(
    http.post('/api/customers', async ({ request }) => {
      expect(request.headers.get('Authorization')).toMatch(/^Bearer /);
      const body = await request.json();
      posted(body);
      records.push({ ...customer, id: 43, companyName: 'New company' });
      return HttpResponse.json(records[1], { status: 201 });
    }),
  );
  const user = await mount();
  await screen.findByText('Atlas Lifting');
  await user.click(screen.getByRole('button', { name: 'Add customer' }));
  await fill(user);
  await user.clear(screen.getByLabelText(/Company name/));
  await user.type(screen.getByLabelText(/Company name/), ' New company ');
  await user.clear(screen.getByLabelText(/Customer email/));
  await user.click(screen.getByRole('button', { name: 'Save customer' }));
  await screen.findByText('Customer created.');
  expect(posted).toHaveBeenCalledWith({
    ...formDefaults(customer),
    companyName: 'New company',
    customerEmail: '',
  });
  expect(await screen.findByText('New company')).toBeVisible();
  expect(listRequests).toBeGreaterThan(1);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});
it('edits fetched details, sends only request fields and refreshes detail data', async () => {
  const put = vi.fn();
  server.use(
    http.put('/api/customers/42', async ({ request }) => {
      const body = await request.json();
      put(body);
      records = [{ ...customer, ...(body as object) }];
      return HttpResponse.json(records[0]);
    }),
  );
  const user = await mount();
  await action(user, 'Edit');
  expect(await screen.findByDisplayValue('Colombo')).toBeVisible();
  await user.clear(screen.getByLabelText(/Company address/));
  await user.type(screen.getByLabelText(/Company address/), 'Kandy');
  await user.click(screen.getByRole('button', { name: 'Save customer' }));
  await screen.findByText('Customer updated.');
  expect(put).toHaveBeenCalledWith({
    ...formDefaults(customer),
    companyAddress: 'Kandy',
  });
  const dialog = await action(user, 'View details');
  expect(await within(dialog).findByText('Kandy')).toBeVisible();
});
it.each([400, 403, 404, 409, 500])(
  'retains edits on failed save %s',
  async (status) => {
    server.use(
      http.put('/api/customers/42', () =>
        HttpResponse.json(
          {
            detail: 'Cannot save',
            errors: [
              { field: 'registrationNo', message: 'Registration rejected' },
              { field: 'unknown', message: 'Unknown rejection' },
            ],
          },
          { status },
        ),
      ),
    );
    const user = await mount();
    await action(user, 'Edit');
    await screen.findByDisplayValue('Atlas Lifting');
    await user.clear(screen.getByLabelText(/Company name/));
    await user.type(screen.getByLabelText(/Company name/), 'Changed');
    await user.click(screen.getByRole('button', { name: 'Save customer' }));
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Save customer' }),
      ).toBeEnabled(),
    );
    expect(screen.getByDisplayValue('Changed')).toBeVisible();
    expect(screen.queryByText('Customer updated.')).not.toBeInTheDocument();
    if (status === 400)
      expect(await screen.findByText('Registration rejected')).toBeVisible();
  },
);

it('requires confirmation, blocks pending duplicates, refreshes the response and retains directory row', async () => {
  const deleted = vi.fn();

  // Keep the API request pending until the test explicitly releases it.
  let finishRequest!: () => void;

  const requestGate = new Promise<void>((resolve) => {
    finishRequest = resolve;
  });

  server.use(
    http.patch('/api/customers/42/activation', async ({ request }) => {
      expect(await request.json()).toEqual({ active: false });

      deleted();

      // Wait until pending-state assertions have finished.
      await requestGate;

      records = [{ ...customer, active: false }];

      return HttpResponse.json(records[0]);
    }),
  );

  const user = await mount();

  // Cancel must not send a request.
  let dialog = await action(user, 'Deactivate');

  expect(
    within(dialog).getByText(/change activation again/),
  ).toBeVisible();

  await user.click(
    within(dialog).getByRole('button', { name: 'Cancel' }),
  );

  expect(deleted).not.toHaveBeenCalled();

  // Confirm deactivation.
  dialog = await action(user, 'Deactivate');

  await user.click(
    within(dialog).getByRole('button', {
      name: 'Deactivate customer',
    }),
  );

  // The API is still pending at this point.
  expect(
    screen.getByRole('button', { name: 'Deactivating…' }),
  ).toBeDisabled();

  expect(
    screen.getByRole('button', { name: 'Cancel' }),
  ).toBeDisabled();

  // Escape must not close the dialog while the request is pending.
  await user.keyboard('{Escape}');

  expect(screen.getByRole('dialog')).toBeVisible();

  // Allow the simulated backend to finish.
  finishRequest();

  // Verify successful completion.
  await screen.findByText(/Customer activation updated\./);

  expect(deleted).toHaveBeenCalledTimes(1);

  expect(screen.getByText('Atlas Lifting')).toBeVisible();

  expect(
    screen.queryByRole('button', { name: /Reactivate/ }),
  ).not.toBeInTheDocument();
});

it.each([403, 404, 409, 500])(
  'keeps deactivation failures visible in confirmation: %s',
  async (status) => {
    server.use(
      http.patch('/api/customers/42/activation', () =>
        HttpResponse.json({ detail: 'Unable to deactivate' }, { status }),
      ),
    );
    const user = await mount();
    const dialog = await action(user, 'Deactivate');
    await user.click(
      within(dialog).getByRole('button', { name: 'Deactivate customer' }),
    );
    await waitFor(() =>
      expect(
        within(dialog).getByRole('button', { name: 'Deactivate customer' }),
      ).toBeEnabled(),
    );
    expect(screen.getByRole('dialog')).toBeVisible();
    expect(
      screen.getByRole('switch', {
        name: 'Activation for Atlas Lifting',
        hidden: true,
      }),
    ).toBeChecked();
    expect(
      screen.queryByText(/Customer activation updated\./),
    ).not.toBeInTheDocument();
  },
);
it('shows detail 404', async () => {
  server.use(
    http.get(
      '/api/customers/42',
      () => new HttpResponse(null, { status: 404 }),
    ),
  );
  const user = await mount();
  await action(user, 'View details');
  expect(await screen.findByText('Customer not found')).toBeVisible();
});
it('expires the existing session on customer 401', async () => {
  server.use(
    http.get('/api/customers', () => new HttpResponse(null, { status: 401 })),
  );
  await mount();
  expect(
    await screen.findByRole('heading', { name: 'Welcome back' }),
  ).toBeVisible();
});
it.each([
  'companyName',
  'registrationNo',
  'vatNo',
  'companyAddress',
  'billAddress',
  'contactPerson',
] as const)('rejects blank %s', (field) => {
  expect(
    customerSchema.safeParse({ ...formDefaults(customer), [field]: '   ' })
      .success,
  ).toBe(false);
});
it.each(['123', '1771234567', '07712345678'])(
  'rejects phone %s',
  (contactNo) => {
    expect(
      customerSchema.safeParse({ ...formDefaults(customer), contactNo })
        .success,
    ).toBe(false);
  },
);
it('validates email and null defaults without inventing attributes', () => {
  expect(
    customerSchema.safeParse({
      ...formDefaults(customer),
      customerEmail: 'bad',
    }).success,
  ).toBe(false);
  expect(
    customerSchema.parse({
      ...formDefaults(customer),
      customerEmail: ' ',
      active: true,
    }),
  ).not.toHaveProperty('active');
  expect(formDefaults({ ...customer, contactNo: null }).contactNo).toBe('');
});
it('normalizes network failures via existing API infrastructure', async () => {
  session.set('test');
  server.use(http.get('/api/customers', () => HttpResponse.error()));
  await expect(customersApi.list()).rejects.toMatchObject({ status: 0 });
});
it('announces required fields and prevents requests for invalid forms', async () => {
  const posted = vi.fn();
  server.use(
    http.post('/api/customers', () => {
      posted();
      return HttpResponse.json(customer, { status: 201 });
    }),
  );
  const user = await mount();
  await user.click(screen.getByRole('button', { name: 'Add customer' }));
  await user.click(screen.getByRole('button', { name: 'Save customer' }));
  expect(await screen.findAllByText('This field is required.')).toHaveLength(6);
  expect(posted).not.toHaveBeenCalled();
  await fill(user);
  await user.clear(screen.getByLabelText(/Customer email/));
  await user.type(screen.getByLabelText(/Customer email/), 'invalid');
  await user.click(screen.getByRole('button', { name: 'Save customer' }));
  expect(await screen.findByText('Enter a valid email address.')).toBeVisible();
  expect(posted).not.toHaveBeenCalled();
});
it('blocks duplicate create and cancellation while saving', async () => {
  const posted = vi.fn();
  server.use(
    http.post('/api/customers', async () => {
      posted();
      await delay(300);
      return HttpResponse.json(customer, { status: 201 });
    }),
  );
  const user = await mount();
  await user.click(screen.getByRole('button', { name: 'Add customer' }));
  await fill(user);
  await user.dblClick(screen.getByRole('button', { name: 'Save customer' }));
  expect(screen.getByRole('button', { name: 'Saving…' })).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
  await user.keyboard('{Escape}');
  expect(screen.getByRole('dialog')).toBeVisible();
  await screen.findByText('Customer created.');
  expect(posted).toHaveBeenCalledTimes(1);
});
it('preserves edits after a network failure', async () => {
  server.use(http.put('/api/customers/42', () => HttpResponse.error()));
  const user = await mount();
  await action(user, 'Edit');
  await screen.findByDisplayValue('Atlas Lifting');
  await user.click(screen.getByRole('button', { name: 'Save customer' }));
  expect(
    await screen.findByText('Unable to connect. Please try again.'),
  ).toBeVisible();
  expect(screen.getByDisplayValue('Atlas Lifting')).toBeVisible();
});
it('handles unexpected directory 403 without ending the session', async () => {
  server.use(
    http.get('/api/customers', () => new HttpResponse(null, { status: 403 })),
  );
  await mount();
  expect(await screen.findByText('Access denied')).toBeVisible();
  expect(screen.getByText('Alex')).toBeVisible();
});
it('deactivation 401 returns to login without success feedback', async () => {
  server.use(
    http.patch(
      '/api/customers/42/activation',
      () => new HttpResponse(null, { status: 401 }),
    ),
  );
  const user = await mount();
  await action(user, 'Deactivate');
  await user.click(screen.getByRole('button', { name: 'Deactivate customer' }));
  expect(
    await screen.findByRole('heading', { name: 'Welcome back' }),
  ).toBeVisible();
  expect(
    screen.queryByText(/Customer activation updated\./),
  ).not.toBeInTheDocument();
});
it('refreshes persisted activation, supports reactivation and filters actual state', async () => {
  server.use(
    http.patch('/api/customers/42/activation', async ({ request }) => {
      const body = (await request.json()) as { active: boolean };
      records = [{ ...customer, active: body.active }];
      return HttpResponse.json(records[0]);
    }),
  );
  const user = await mount();
  await action(user, 'Deactivate');
  await user.click(screen.getByRole('button', { name: 'Deactivate customer' }));
  await waitFor(() =>
    expect(
      screen.getByRole('switch', { name: 'Activation for Atlas Lifting' }),
    ).not.toBeChecked(),
  );
  await user.click(screen.getByRole('button', { name: 'Active' }));
  expect(screen.getByText('No matching customers')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Inactive' }));
  await user.click(
    screen.getByRole('switch', { name: 'Activation for Atlas Lifting' }),
  );
  await user.click(
    await screen.findByRole('button', { name: 'Activate customer' }),
  );
  await waitFor(() =>
    expect(screen.getByText('No matching customers')).toBeVisible(),
  );
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  await user.click(screen.getByRole('button', { name: 'All' }));
  expect(
    screen.getByRole('switch', { name: 'Activation for Atlas Lifting' }),
  ).toBeChecked();
});
