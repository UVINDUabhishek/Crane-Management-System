import { beforeEach, expect, it, vi } from 'vitest';
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
import { createRequestGate } from './requestGate';
import type { Role } from '../shared/permissions';
import {
  craneFields,
  craneSchema,
  formDefaults,
} from '../features/cranes/craneSchema';
import type { CraneResponse } from '../features/cranes/cranesApi';
const crane: CraneResponse = {
  id: 42,
  craneNumber: 'CR080T001',
  type: '080T',
  capacity: 80,
  manufacturer: 'Liebherr',
  serialNumber: 'SN-42',
  year: 2022,
  location: 'Colombo',
  active: true,
  status: 'AVAILABLE',
};
let records: CraneResponse[], calls: number;
beforeEach(() => {
  records = [{ ...crane }];
  calls = 0;
  server.use(
    http.get('/api/cranes', ({ request }) => {
      expect(new URL(request.url).search).toBe('');
      calls++;
      return HttpResponse.json(records);
    }),
    http.get('/api/cranes/:id', () => HttpResponse.json(records[0])),
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
            <Link to="/workspace/cranes">Direct crane route</Link>
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
async function action(user: ReturnType<typeof userEvent.setup>, name: string) {
  await user.click(
    await screen.findByRole('button', { name: 'Actions for CR080T001' }),
  );
  await user.click(screen.getByRole('menuitem', { name }));
  return await screen.findByRole('dialog');
}
it.each(['ADMIN', 'SALES', 'OPERATIONS', 'FINANCE', 'MAINTENANCE'] as Role[])(
  'matches backend role %s',
  async (role) => {
    await mount(role);
    if (role === 'ADMIN' || role === 'SALES') {
      expect(await screen.findByText('CR080T001')).toBeVisible();
      expect(screen.getByRole('button', { name: 'Add crane' })).toBeVisible();
    } else {
      expect(await screen.findByText('Access denied')).toBeVisible();
      expect(calls).toBe(0);
    }
  },
);
it('loads actual details and restores focus without editable number/status', async () => {
  const user = await mount();
  const dialog = await action(user, 'View details');
  expect(await within(dialog).findByText('CR080T001')).toBeVisible();
  expect(within(dialog).getByText('Available')).toBeVisible();
  expect(within(dialog).getByText('Active')).toBeVisible();
  await user.click(within(dialog).getByRole('button', { name: 'Close' }));
  await waitFor(() =>
    expect(
      screen.getByRole('button', { name: 'Actions for CR080T001' }),
    ).toHaveFocus(),
  );
});
it.each([
  'craneNumber',
  'type',
  'manufacturer',
  'serialNumber',
  'location',
] as const)('searches %s', async (key) => {
  const user = await mount();
  await screen.findByText('CR080T001');
  fireEvent.change(screen.getByRole('textbox', { name: 'Search cranes' }), {
    target: { value: ` ${crane[key]!.toUpperCase()} ` },
  });
  expect(screen.getByText('CR080T001')).toBeVisible();
  fireEvent.change(screen.getByRole('textbox', { name: 'Search cranes' }), {
    target: { value: 'absent' },
  });
  expect(screen.getByText('No matching cranes')).toBeVisible();
  await user.click(screen.getAllByRole('button', { name: 'Clear filters' })[0]);
  expect(screen.getByText('CR080T001')).toBeVisible();
});
it('combines activation and operational filters and resets pagination', async () => {
  records = Array.from({ length: 6 }, (_, i) => ({
    ...crane,
    id: i,
    craneNumber: `CR080T00${i}`,
    active: i !== 5,
    status: i === 5 ? 'MAINTENANCE' : 'AVAILABLE',
  }));
  const user = await mount();
  await screen.findByText('CR080T000');
  await user.click(screen.getByRole('button', { name: 'Go to next page' }));
  expect(screen.getByText('CR080T005')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Active' }));
  expect(screen.getByText('1–5 of 5')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Maintenance' }));
  expect(screen.getByText('No matching cranes')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Inactive' }));
  expect(screen.getByText('CR080T005')).toBeVisible();
});
it('shows loading and empty API results', async () => {
  const gate = createRequestGate();
  server.use(
    http.get('/api/cranes', async () => {
      await gate.wait();
      return HttpResponse.json([]);
    }),
  );
  try {
    await mount();
    await gate.started;
    expect(await screen.findByText('Loading cranes…')).toBeVisible();
  } finally {
    gate.release();
  }
  expect(await screen.findByText('No cranes yet')).toBeVisible();
});

it('retries list failures', async () => {
  server.use(
    http.get('/api/cranes', () => new HttpResponse(null, { status: 500 })),
  );
  const user = await mount();
  await screen.findByText('The service is unavailable. Please try again.');
  server.use(http.get('/api/cranes', () => HttpResponse.json(records)));
  await user.click(screen.getByRole('button', { name: 'Retry' }));
  expect(await screen.findByText('CR080T001')).toBeVisible();
});
it.each(['create', 'edit'])(
  '%s sends only editable fields and refreshes',
  async (mode) => {
    const saved = vi.fn();
    const handler = async ({ request }: { request: Request }) => {
      const values = await request.json();
      saved(values);
      records = [{ ...crane, ...(values as object) }];
      return HttpResponse.json(records[0], {
        status: mode === 'create' ? 201 : 200,
      });
    };
    server.use(
      http.post('/api/cranes', handler),
      http.put('/api/cranes/42', handler),
    );
    const user = await mount();
    await screen.findByText('CR080T001');
    if (mode === 'edit') await action(user, 'Edit');
    else await user.click(screen.getByRole('button', { name: 'Add crane' }));
    await screen.findByLabelText(/Serial number/);
    for (const [key, label] of Object.entries(craneFields)) {
      fireEvent.change(screen.getByLabelText(label, { exact: false }), {
        target: {
          value: String(
            formDefaults(crane)[key as keyof ReturnType<typeof formDefaults>],
          ),
        },
      });
    }
    expect(
      screen.queryByRole('textbox', { name: 'Crane number' }),
    ).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Save crane' }));
    await screen.findByText(
      mode === 'create' ? 'Crane created.' : 'Crane updated.',
    );
    expect(saved).toHaveBeenCalledWith(formDefaults(crane));
    expect(calls).toBeGreaterThan(1);
  },
);
it.each([400, 404, 409, 500])(
  'preserves edits and displays save failure %s',
  async (status) => {
    server.use(
      http.put('/api/cranes/42', () =>
        HttpResponse.json(
          {
            detail: 'Cannot update',
            errors: [{ field: 'serialNumber', message: 'Duplicate serial' }],
          },
          { status },
        ),
      ),
    );
    const user = await mount();
    await action(user, 'Edit');
    await screen.findByDisplayValue('SN-42');
    await user.click(screen.getByRole('button', { name: 'Save crane' }));
    expect(
      await screen.findByText(
        status === 500
          ? 'The service is unavailable. Please try again.'
          : 'Cannot update',
      ),
    ).toBeVisible();
    expect(screen.getByRole('button', { name: 'Save crane' })).toBeEnabled();
    expect(screen.getByDisplayValue('SN-42')).toBeVisible();
    if (status === 400)
      expect(await screen.findByText('Duplicate serial')).toBeVisible();
    expect(screen.queryByText('Crane updated.')).not.toBeInTheDocument();
  },
);
it('confirms, cancels, blocks pending and reverses activation with server refresh', async () => {
  const patch = vi.fn();
  const user = await mount();
  const control = await screen.findByRole('switch', {
    name: 'Activation for CR080T001',
  });
  server.use(
    http.patch('/api/cranes/42/activation', () => {
      patch();
      return HttpResponse.json(crane);
    }),
  );
  await user.click(control);
  await user.click(screen.getByRole('button', { name: 'Cancel' }));
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(patch).not.toHaveBeenCalled();
  for (const active of [false, true]) {
    const gate = createRequestGate();
    server.use(
      http.patch('/api/cranes/42/activation', async ({ request }) => {
        const body = (await request.json()) as { active: boolean };
        patch(body);
        await gate.wait();
        records = [{ ...crane, active: body.active }];
        return HttpResponse.json(records[0]);
      }),
    );
    const previousCalls = patch.mock.calls.length;
    try {
      await user.click(control);
      const dialog = await screen.findByRole('dialog');
      await user.dblClick(
        within(dialog).getByRole('button', {
          name: active ? 'Activate crane' : 'Deactivate crane',
        }),
      );
      await gate.started;
      const pending = within(dialog).getByRole('button', {
        name: active ? 'Activating…' : 'Deactivating…',
      });
      const cancel = within(dialog).getByRole('button', { name: 'Cancel' });
      expect(pending).toBeDisabled();
      expect(cancel).toBeDisabled();
      expect(control).toBeDisabled();
      expect(control).toHaveProperty('checked', !active);
      await user.keyboard('{Escape}');
      expect(dialog).toBeVisible();
      expect(patch).toHaveBeenCalledTimes(previousCalls + 1);
    } finally {
      gate.release();
    }
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(control).toHaveProperty('checked', active);
    expect(patch).toHaveBeenLastCalledWith({ active });
  }
  expect(patch).toHaveBeenCalledTimes(2);
  expect(calls).toBeGreaterThanOrEqual(3);
});

it('preserves activation on conflict and permits retry', async () => {
  server.use(
    http.patch('/api/cranes/42/activation', () =>
      HttpResponse.json({ detail: 'Unfinished work' }, { status: 409 }),
    ),
  );
  const user = await mount();
  await user.click(
    await screen.findByRole('switch', { name: 'Activation for CR080T001' }),
  );
  await user.click(screen.getByRole('button', { name: 'Deactivate crane' }));
  expect(await screen.findByText('Unfinished work')).toBeVisible();
  expect(
    screen.getByRole('switch', {
      name: 'Activation for CR080T001',
      hidden: true,
    }),
  ).toBeChecked();
  expect(
    screen.getByRole('button', { name: 'Deactivate crane' }),
  ).toBeEnabled();
});
it('prevents invalid create submissions and duplicate pending saves', async () => {
  const save = vi.fn();
  const gate = createRequestGate();
  server.use(
    http.post('/api/cranes', async ({ request }) => {
      save(await request.json());
      await gate.wait();
      return HttpResponse.json(crane, { status: 201 });
    }),
  );
  try {
    const user = await mount();
    await user.click(screen.getByRole('button', { name: 'Add crane' }));
    await user.click(screen.getByRole('button', { name: 'Save crane' }));
    expect(await screen.findAllByText('This field is required.')).toHaveLength(
      4,
    );
    expect(save).not.toHaveBeenCalled();
    for (const [key, label] of Object.entries(craneFields)) {
      fireEvent.change(screen.getByLabelText(label, { exact: false }), {
        target: {
          value: String(
            formDefaults(crane)[key as keyof ReturnType<typeof formDefaults>],
          ),
        },
      });
    }
    await user.dblClick(screen.getByRole('button', { name: 'Save crane' }));
    await gate.started;
    expect(screen.getByRole('button', { name: 'Saving…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeVisible();
    expect(save).toHaveBeenCalledTimes(1);
  } finally {
    gate.release();
  }
  await screen.findByText('Crane created.');
  expect(save).toHaveBeenCalledTimes(1);
});
it.each([404, 500])(
  'handles detail failure %s without stale fields',
  async (status) => {
    server.use(
      http.get('/api/cranes/42', () => new HttpResponse(null, { status })),
    );
    const user = await mount();
    const dialog = await action(user, 'View details');
    expect(
      await within(dialog).findByText(
        status === 404
          ? 'Crane not found'
          : 'The service is unavailable. Please try again.',
      ),
    ).toBeVisible();
    expect(within(dialog).queryByText('SN-42')).not.toBeInTheDocument();
    if (status === 500) {
      server.use(http.get('/api/cranes/42', () => HttpResponse.json(crane)));
      await user.click(within(dialog).getByRole('button', { name: 'Retry' }));
      expect(await within(dialog).findByText('SN-42')).toBeVisible();
    }
  },
);
it('clamps the page after a successful filtered activation and retries a failed request', async () => {
  records = Array.from({ length: 6 }, (_, i) => ({
    ...crane,
    id: i + 1,
    craneNumber: `CR080T00${i + 1}`,
  }));
  server.use(
    http.patch('/api/cranes/6/activation', () =>
      HttpResponse.json({ detail: 'Try again' }, { status: 409 }),
    ),
  );
  const user = await mount();
  await screen.findByText('CR080T001');
  await user.click(screen.getByRole('button', { name: 'Active' }));
  await user.click(screen.getByRole('button', { name: 'Go to next page' }));
  await user.click(
    screen.getByRole('switch', { name: 'Activation for CR080T006' }),
  );
  await user.click(screen.getByRole('button', { name: 'Deactivate crane' }));
  expect(await screen.findByText('Try again')).toBeVisible();
  server.use(
    http.patch('/api/cranes/6/activation', () => {
      records = records.map((r) => (r.id === 6 ? { ...r, active: false } : r));
      return HttpResponse.json(records[5]);
    }),
  );
  await user.click(screen.getByRole('button', { name: 'Deactivate crane' }));
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(screen.getByText('1–5 of 5')).toBeVisible();
  expect(screen.queryByText('CR080T006')).not.toBeInTheDocument();
  await waitFor(() =>
    expect(
      screen.getByRole('button', { name: 'Refresh cranes' }),
    ).toHaveFocus(),
  );
});
it.each([
  { capacity: 0 },
  { capacity: -1 },
  { capacity: NaN },
  { year: 0 },
  { year: 2.5 },
  { type: ' ' },
  { serialNumber: '' },
  { manufacturer: '' },
  { location: '' },
])('rejects invalid specification %j', (invalid) => {
  expect(
    craneSchema.safeParse({ ...formDefaults(crane), ...invalid }).success,
  ).toBe(false);
});
