import { beforeEach, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from '@mui/material';
import { QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { createAppTheme } from '../app/theme';
import { createQueryClient } from '../app/App';
import { RecordForm, type Row } from '../features/workflows/WorkflowPage';
import { contracts } from '../features/workflows/contracts';
import { businessToday } from '../features/workflows/formRelations';
import { server } from './setup';
import { createRequestGate } from './requestGate';
const options: Record<string, Row[]> = {
  customers: [
    { id: 1, label: 'Atlas' },
    { id: 2, label: 'Harbor' },
  ],
  quotations: [
    {
      id: 10,
      label: 'QT-Atlas',
      customerId: 1,
      type: '080T',
      validFrom: '2026-01-01',
      validTo: '2026-12-31',
    },
    {
      id: 20,
      label: 'QT-Harbor',
      customerId: 2,
      type: '100T',
      validFrom: '2026-01-01',
      validTo: '2026-12-31',
    },
  ],
  cranes: [
    { id: 30, label: 'CR-080', type: '080T' },
    { id: 31, label: 'CR-100', type: '100T' },
  ],
  operators: [{ id: 40, label: 'Morgan' }],
  'maintenance-people': [{ id: 50, label: 'Sam' }],
};
beforeEach(() => {
  for (const [key, data] of Object.entries(options))
    server.use(http.get('/api/lookups/' + key, () => HttpResponse.json(data)));
  server.use(
    http.get('/api/job-card', () =>
      HttpResponse.json([
        {
          id: 60,
          jobCardNumber: 'JC-pending',
          customer: 'Atlas',
          status: 'PENDING',
        },
        {
          id: 61,
          jobCardNumber: 'JC-complete',
          customer: 'Harbor',
          status: 'COMPLETED',
        },
      ]),
    ),
  );
});
function mount(
  module: string,
  initial: Row = {},
  edit = false,
  submit = vi.fn().mockResolvedValue(undefined),
) {
  const c = contracts[module];
  const close = vi.fn();
  render(
    <ThemeProvider theme={createAppTheme(false)}>
      <QueryClientProvider client={createQueryClient()}>
        <RecordForm
          title={edit ? 'Edit record' : 'Add record'}
          resource={c.path}
          fields={edit ? c.editFields : c.createFields}
          initial={initial}
          submit={submit}
          close={close}
        />
      </QueryClientProvider>
    </ThemeProvider>,
  );
  return { user: userEvent.setup(), submit, close };
}
async function select(
  user: ReturnType<typeof userEvent.setup>,
  name: string,
  option: string,
) {
  await user.click(
    screen.getByRole('combobox', { name: new RegExp('^' + name) }),
  );
  await user.click(await screen.findByRole('option', { name: option }));
}
function input(name: string, value: string) {
  fireEvent.change(screen.getByLabelText(name, { exact: false }), {
    target: { value },
  });
}
const validJob: Row = {
  customerId: 1,
  quotationId: 10,
  craneId: 30,
  startDate: '2026-06-01',
  endDate: '2026-06-01',
  location: 'Harbor',
  description: 'Lift',
};

it('filters quotations by customer and clears an incompatible selection on change', async () => {
  const { user } = mount('jobs');
  expect(screen.getByRole('combobox', { name: /Quotation/ })).toBeDisabled();
  await select(user, 'Customer', 'Atlas');
  await select(user, 'Quotation', 'QT-Atlas');
  await select(user, 'Customer', 'Harbor');
  expect(screen.getByRole('combobox', { name: /Quotation/ })).toHaveValue('');
  await user.click(screen.getByRole('combobox', { name: /Quotation/ }));
  expect(
    await screen.findByRole('option', { name: 'QT-Harbor' }),
  ).toBeVisible();
  expect(
    screen.queryByRole('option', { name: 'QT-Atlas' }),
  ).not.toBeInTheDocument();
});
it('submits exact selected IDs, a same-day schedule and an empty optional operator', async () => {
  const { user, submit } = mount('jobs');
  await select(user, 'Customer', 'Atlas');
  await select(user, 'Quotation', 'QT-Atlas');
  await select(user, 'Crane', 'CR-080');
  input('Start date', '2026-06-01');
  input('End date', '2026-06-01');
  input('Location', 'Harbor');
  input('Description', 'Lift');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() =>
    expect(submit.mock.calls[0]?.[0]).toEqual({
      ...validJob,
      operatorId: null,
    }),
  );
});
it('blocks mismatched types, invalid ranges and missing finite quotation coverage', async () => {
  const { user, submit } = mount('jobs', validJob);
  await select(user, 'Crane', 'CR-100');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  expect(
    await screen.findByText('Crane type must match the quotation type.'),
  ).toBeVisible();
  await select(user, 'Crane', 'CR-080');
  input('End date', '2026-05-31');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  expect(
    await screen.findByText('End date cannot be earlier than start date.'),
  ).toBeVisible();
  input('End date', '2027-01-01');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  expect(
    await screen.findByText(
      'Job dates must fall within the quotation validity period.',
    ),
  ).toBeVisible();
  input('End date', '');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  expect(
    await screen.findByText(
      'An end date is required to validate quotation coverage.',
    ),
  ).toBeVisible();
  expect(submit).not.toHaveBeenCalled();
});
it('uses the saved customer for job edit filtering and retains an unlisted optional operator', async () => {
  const { user, submit } = mount(
    'jobs',
    { ...validJob, operatorId: 99, operator: 'Former operator' },
    true,
  );
  expect(
    screen.queryByRole('combobox', { name: /Customer/ }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole('combobox', { name: /Quotation/ }));
  expect(await screen.findByRole('option', { name: 'QT-Atlas' })).toBeVisible();
  expect(
    screen.queryByRole('option', { name: 'QT-Harbor' }),
  ).not.toBeInTheDocument();
  await user.keyboard('{Escape}');
  expect(screen.getByRole('combobox', { name: /Operator/ })).toHaveValue(
    'Former operator (current assignment)',
  );
  await user.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() => expect(submit).toHaveBeenCalled());
  expect(submit.mock.calls[0][0]).toEqual({
    quotationId: 10,
    craneId: 30,
    operatorId: 99,
    startDate: '2026-06-01',
    endDate: '2026-06-01',
    location: 'Harbor',
    description: 'Lift',
  });
});
it('does not submit typed lookup labels and supports keyboard selection', async () => {
  const { user, submit } = mount('timesheets');
  const job = screen.getByRole('combobox', { name: /Job card/ });
  await user.type(job, 'arbitrary');
  input('Date of work', businessToday());
  input('Worked hours', '3');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  expect(submit).not.toHaveBeenCalled();
  expect(await screen.findByText('This field is required.')).toBeVisible();
  await user.clear(job);
  await user.type(job, 'JC-pending');
  await user.keyboard('{ArrowDown}{Enter}');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() =>
    expect(submit.mock.calls[0]?.[0]).toEqual({
      jobCardId: 60,
      dateOfWork: businessToday(),
      workedHours: 3,
      description: null,
    }),
  );
});
it.each([false, true])(
  'rejects future timesheets and accepts business today (edit=%s)',
  async (edit) => {
    const { user, submit } = mount(
      'timesheets',
      {
        jobCardId: 60,
        jobCardNumber: 'JC-pending',
        dateOfWork: businessToday(),
        workedHours: 3,
        description: 'Day',
      },
      edit,
    );
    if (!edit) {
      await user.click(screen.getByRole('combobox', { name: /Job card/ }));
      expect(
        await screen.findByRole('option', { name: 'JC-pending · Atlas' }),
      ).toBeVisible();
      expect(
        screen.queryByRole('option', { name: 'JC-complete · Harbor' }),
      ).not.toBeInTheDocument();
      await user.keyboard('{Escape}');
    }
    expect(screen.getByText('Hours')).toBeVisible();
    expect(screen.getByLabelText(/Description/).tagName).toBe('TEXTAREA');
    input('Date of work', '2999-01-01');
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(
      await screen.findByText(
        'Date of work cannot be later than today (Asia/Colombo).',
      ),
    ).toBeVisible();
    expect(submit).not.toHaveBeenCalled();
    input('Date of work', businessToday());
    await user.click(screen.getByRole('button', { name: 'Save' }));
    await waitFor(() => expect(submit).toHaveBeenCalled());
  },
);
it('computes business today across a UTC midnight boundary', () => {
  expect(businessToday(new Date('2026-10-31T20:00:00Z'))).toBe('2026-11-01');
  expect(businessToday(new Date('2026-11-01T01:00:00Z'))).toBe('2026-11-01');
});
it('preserves a historical admin assignee but excludes it from new selections', async () => {
  const initial = {
    craneId: 30,
    maintenancePersonId: 99,
    maintenancePerson: 'Former Admin',
    maintenanceStartDate: '2026-06-01',
    maintenanceEndDate: '2026-06-01',
    maintenanceType: 'SERVICE',
    description: 'Service',
  };
  const { user, submit } = mount('maintenance', initial, true);
  expect(
    screen.getByRole('combobox', { name: /Maintenance person/ }),
  ).toHaveValue('Former Admin (current assignment)');
  input('Maintenance end date', '2026-05-01');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  expect(
    await screen.findByText('End date cannot be earlier than start date.'),
  ).toBeVisible();
  input('Maintenance end date', '2026-06-01');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() =>
    expect(submit.mock.calls[0]?.[0]).toEqual({
      craneId: 30,
      maintenancePersonId: 99,
      maintenanceStartDate: '2026-06-01',
      maintenanceEndDate: '2026-06-01',
      maintenanceType: 'SERVICE',
      description: 'Service',
    }),
  );
});
it('offers real crane type strings and validates quotation date order', async () => {
  const { user, submit } = mount('quotations', {
    customerId: 1,
    minHours: 8,
    minRate: 1000,
    description: 'Quote',
  });
  await select(user, 'Crane type / rated capacity', '080T');
  input('Valid from', '2026-06-01');
  input('Valid to', '2026-05-01');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  expect(
    await screen.findByText('End date cannot be earlier than start date.'),
  ).toBeVisible();
  input('Valid to', '2026-06-01');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() =>
    expect(submit.mock.calls[0]?.[0]).toEqual({
      customerId: 1,
      minHours: 8,
      minRate: 1000,
      description: 'Quote',
      type: '080T',
      validFrom: '2026-06-01',
      validTo: '2026-06-01',
      mobilization: null,
    }),
  );
});
it('shows lookup failure, retries, and preserves entered form values', async () => {
  let fail = true;
  server.use(
    http.get('/api/lookups/customers', () =>
      fail
        ? HttpResponse.json({ detail: 'Lookup unavailable' }, { status: 503 })
        : HttpResponse.json(options.customers),
    ),
  );
  const { user } = mount('jobs');
  input('Location', 'Keep this');
  expect(
    await screen.findByText('The service is unavailable. Please try again.'),
  ).toBeVisible();
  fail = false;
  await user.click(screen.getByRole('button', { name: /retry/i }));
  await select(user, 'Customer', 'Atlas');
  expect(screen.getByLabelText(/Location/)).toHaveValue('Keep this');
});
it('locks pending submissions and retains values on a recoverable error', async () => {
  const gate = createRequestGate();
  const submit = vi.fn(async () => {
    await gate.wait();
    throw new Error('Recoverable');
  });
  const { user } = mount(
    'maintenance',
    {
      craneId: 30,
      maintenanceStartDate: '2026-06-01',
      maintenanceType: 'SERVICE',
      description: 'Keep this',
    },
    false,
    submit,
  );
  await select(user, 'Crane', 'CR-080');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  try {
    await gate.started;
    expect(screen.getByRole('button', { name: 'Saving…' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    expect(screen.getByRole('combobox', { name: /Crane/ })).toBeDisabled();
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeVisible();
  } finally {
    gate.release();
  }
  await screen.findByRole('button', { name: 'Save' });
  expect(screen.getByLabelText(/Description/)).toHaveValue('Keep this');
  expect(submit).toHaveBeenCalledTimes(1);
});

it('offers only eligible maintenance people and submits a cleared optional assignee as null', async () => {
  const { user, submit } = mount('maintenance', {
    craneId: 30,
    maintenanceStartDate: '2026-06-01',
    maintenanceType: 'SERVICE',
    description: 'Service',
  });
  await select(user, 'Crane', 'CR-080');
  await user.click(
    screen.getByRole('combobox', { name: 'Maintenance person' }),
  );
  expect(await screen.findByRole('option', { name: 'Sam' })).toBeVisible();
  expect(
    screen.queryByRole('option', { name: /Admin/ }),
  ).not.toBeInTheDocument();
  await user.click(screen.getByRole('option', { name: 'Sam' }));
  const person = screen.getByRole('combobox', { name: 'Maintenance person' });
  const clear = person
    .closest('.MuiAutocomplete-root')!
    .querySelector('button[title="Clear"]') as HTMLElement;
  await user.click(clear);
  await user.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() =>
    expect(submit.mock.calls[0]?.[0]).toEqual({
      craneId: 30,
      maintenancePersonId: null,
      maintenanceStartDate: '2026-06-01',
      maintenanceEndDate: null,
      maintenanceType: 'SERVICE',
      description: 'Service',
    }),
  );
});
it('explains an empty lookup without allowing invented records', async () => {
  server.use(http.get('/api/lookups/customers', () => HttpResponse.json([])));
  const { user, submit } = mount('jobs');
  await user.click(screen.getByRole('combobox', { name: /Customer/ }));
  expect(await screen.findByText('No eligible records')).toBeVisible();
  await user.keyboard('{Escape}');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  expect(submit).not.toHaveBeenCalled();
});

it('shows the immutable quotation customer on edit and sends only update fields', async () => {
  const initial = {
    customerId: 1,
    customerName: 'Atlas',
    type: '080T',
    validFrom: '2026-06-01',
    validTo: '2026-06-01',
    minHours: 8,
    minRate: 1000,
    mobilization: 500,
    description: 'Quote',
  };
  const { user, submit } = mount('quotations', initial, true);
  expect(screen.getByLabelText('Customer')).toHaveValue('Atlas');
  expect(screen.getByLabelText('Customer')).toHaveAttribute('readonly');
  await select(user, 'Crane type / rated capacity', '080T');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  const { customerId, customerName, ...body } = initial;
  expect(customerId).toBe(1);
  expect(customerName).toBe('Atlas');
  await waitFor(() => expect(submit.mock.calls[0]?.[0]).toEqual(body));
});

it('shows the immutable timesheet job on edit and submits only update fields', async () => {
  const today = businessToday();
  const { user, submit } = mount(
    'timesheets',
    {
      id: 70,
      jobCardId: 60,
      jobCardNumber: 'JC-pending',
      customerId: 1,
      customerName: 'Atlas',
      dateOfWork: today,
      workedHours: 3,
      description: 'Original work',
    },
    true,
  );
  expect(screen.getByLabelText('Job card')).toHaveValue('JC-pending');
  expect(screen.getByLabelText('Job card')).toHaveAttribute('readonly');
  expect(
    screen.queryByRole('combobox', { name: /Job card/ }),
  ).not.toBeInTheDocument();
  input('Worked hours', '4');
  input('Description', 'Updated work');
  await user.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() =>
    expect(submit.mock.calls[0]?.[0]).toEqual({
      dateOfWork: today,
      workedHours: 4,
      description: 'Updated work',
    }),
  );
  expect(submit).toHaveBeenCalledTimes(1);
});
