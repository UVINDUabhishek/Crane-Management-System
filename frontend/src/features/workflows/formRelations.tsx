import { Autocomplete, MenuItem, Stack, TextField } from '@mui/material';
import { useQueries } from '@tanstack/react-query';
import { api } from '../../shared/api';
import { ErrorState } from '../../shared/States';
import type { Field } from './contracts';
import type { Row } from './WorkflowPage';

export function businessToday(now = new Date()) {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone: 'Asia/Colombo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now);
  return ['year', 'month', 'day']
    .map((key) => parts.find((p) => p.type === key)!.value)
    .join('-');
}
export function useFormLookups(fields: Field[], resource?: string) {
  const paths = Array.from(
    new Set([
      ...fields.flatMap((f) => (f.lookup ? [f.lookup] : [])),
      ...(resource === '/quotations' ? ['/lookups/cranes'] : []),
    ]),
  );
  const results = useQueries({
    queries: paths.map((path) => ({
      queryKey: ['lookups', path],
      queryFn: async ({ signal }: { signal: AbortSignal }) =>
        (await api.get<Row[]>(path, { signal })).data,
      staleTime: 30_000,
    })),
  });
  return Object.fromEntries(paths.map((path, index) => [path, results[index]]));
}
export type FormLookups = ReturnType<typeof useFormLookups>;
const names: Record<string, string> = {
  customerId: 'Customer',
  quotationId: 'Quotation',
  craneId: 'Crane',
  operatorId: 'Operator',
  jobCardId: 'Job card',
  maintenancePersonId: 'Maintenance person',
};
const references: Record<string, string> = {
  customerId: 'customer',
  quotationId: 'quotation',
  craneId: 'crane',
  operatorId: 'operator',
  jobCardId: 'jobCardNumber',
  maintenancePersonId: 'maintenancePerson',
};
export function relationOptions(
  field: Field,
  lookups: FormLookups,
  values: Row,
  initial: Row,
) {
  const rows = lookups[field.lookup!]?.data ?? [];
  return rows.filter(
    (r) =>
      (field.key !== 'quotationId' ||
        String(r.customerId) ===
          String(values.customerId ?? initial.customerId ?? '')) &&
      (field.lookup !== '/job-card' || r.status === 'PENDING'),
  );
}
export function validateRelations(
  resource: string | undefined,
  fields: Field[],
  values: Row,
  initial: Row,
  lookups: FormLookups,
) {
  const errors: Record<string, string> = {};
  for (const f of fields.filter((f) => f.lookup)) {
    const value = values[f.key];
    if (value === null || value === undefined || value === '') continue;
    const current =
      initial[f.key] != null && String(initial[f.key]) === String(value);
    if (
      !current &&
      !relationOptions(f, lookups, values, initial).some(
        (r) => String(r.id) === String(value),
      )
    )
      errors[f.key] = 'Choose an available record from the list.';
  }
  const datePairs = [
    ['startDate', 'endDate'],
    ['validFrom', 'validTo'],
    ['maintenanceStartDate', 'maintenanceEndDate'],
  ];
  for (const [start, end] of datePairs)
    if (
      values[start] &&
      values[end] &&
      String(values[end]) < String(values[start])
    )
      errors[end] = 'End date cannot be earlier than start date.';
  if (values.dateOfWork && String(values.dateOfWork) > businessToday())
    errors.dateOfWork =
      'Date of work cannot be later than today (Asia/Colombo).';
  if (resource === '/job-card') {
    if (!values.endDate)
      errors.endDate =
        'An end date is required to validate quotation coverage.';
    const quote = lookups['/lookups/quotations']?.data?.find(
      (r) => String(r.id) === String(values.quotationId),
    );
    const crane = lookups['/lookups/cranes']?.data?.find(
      (r) => String(r.id) === String(values.craneId),
    );
    if (!quote)
      errors.quotationId =
        'Choose an active quotation to validate the booking.';
    else {
      if (
        String(quote.customerId) !==
        String(values.customerId ?? initial.customerId)
      )
        errors.quotationId = 'Quotation must belong to the selected customer.';
      if (crane && quote.type !== crane.type)
        errors.craneId = 'Crane type must match the quotation type.';
      if (
        !quote.validFrom ||
        !quote.validTo ||
        (values.startDate &&
          String(values.startDate) < String(quote.validFrom)) ||
        (values.endDate && String(values.endDate) > String(quote.validTo))
      )
        errors.endDate =
          'Job dates must fall within the quotation validity period.';
    }
    if (!crane)
      errors.craneId = 'Choose an active crane to validate the booking.';
  }
  if (
    resource === '/quotations' &&
    values.type !== initial.type &&
    !lookups['/lookups/cranes']?.data?.some((r) => r.type === values.type)
  )
    errors.type = 'Select an existing crane type.';
  return errors;
}
export function FormInput({
  field,
  value,
  onChange,
  error,
  values,
  initial,
  lookups,
  resource,
  pending,
}: {
  field: Field;
  value: unknown;
  onChange: (value: string) => void;
  error?: string;
  values: Row;
  initial: Row;
  lookups: FormLookups;
  resource?: string;
  pending: boolean;
}) {
  const typeSelector = resource === '/quotations' && field.key === 'type';
  const query =
    lookups[typeSelector ? '/lookups/cranes' : (field.lookup ?? '')];
  const current = String(value ?? '');
  const title = typeSelector
    ? 'Crane type / rated capacity'
    : (names[field.key] ?? field.label);
  let items = typeSelector
    ? Array.from(
        new Set(
          query?.data?.flatMap((r) =>
            typeof r.type === 'string' && r.type ? [r.type] : [],
          ) ?? [],
        ),
      ).map((type) => ({ id: type, label: type }))
    : field.lookup
      ? relationOptions(field, lookups, values, initial).map((r) => ({
          id: String(r.id),
          label: String(r.label ?? `${r.jobCardNumber} · ${r.customer ?? ''}`),
        }))
      : [];
  const original = String(initial[field.key] ?? '');
  // Preserve the saved identity even when active-only lookups no longer include it.
  if (current && current === original && !items.some((o) => o.id === current))
    items = [
      {
        id: current,
        label: typeSelector
          ? current
          : `${initial[references[field.key]] ?? initial.customerName ?? `Record #${current}`} (current assignment)`,
      },
      ...items,
    ];
  const disabled =
    pending ||
    (field.key === 'quotationId' && !(values.customerId ?? initial.customerId));
  if (field.lookup || typeSelector)
    return (
      <Stack spacing={1}>
        {query?.isError && (
          <ErrorState error={query.error} retry={() => void query.refetch()} />
        )}
        <Autocomplete
          options={items}
          value={items.find((o) => o.id === current) ?? null}
          isOptionEqualToValue={(a, b) => a.id === b.id}
          getOptionLabel={(o) => o.label}
          getOptionKey={(o) => o.id}
          onChange={(_, option) => onChange(option?.id ?? '')}
          disabled={!!disabled}
          loading={query?.isFetching}
          noOptionsText={
            query?.isError
              ? 'Options unavailable. Retry loading.'
              : 'No eligible records'
          }
          renderInput={(params) => (
            <TextField
              {...params}
              label={title}
              required={field.required}
              error={!!error}
              helperText={
                error ??
                (disabled && !pending
                  ? 'Select a customer first.'
                  : query?.isFetching
                    ? 'Loading options…'
                    : current === original &&
                        original &&
                        !query?.data?.some((r) => String(r.id) === current) &&
                        !typeSelector
                      ? 'Saved assignment retained; select a replacement only if needed.'
                      : typeSelector
                        ? 'Uses the stored crane type, not numeric capacity.'
                        : undefined)
              }
            />
          )}
        />
      </Stack>
    );
  const minDate =
    field.key === 'endDate'
      ? values.startDate
      : field.key === 'validTo'
        ? values.validFrom
        : field.key === 'maintenanceEndDate'
          ? values.maintenanceStartDate
          : undefined;
  const endKey =
    field.key === 'startDate'
      ? 'endDate'
      : field.key === 'validFrom'
        ? 'validTo'
        : field.key === 'maintenanceStartDate'
          ? 'maintenanceEndDate'
          : undefined;
  return (
    <TextField
      fullWidth
      label={title}
      required={
        field.required || (resource === '/job-card' && field.key === 'endDate')
      }
      type={field.type}
      multiline={field.key === 'description'}
      minRows={field.key === 'description' ? 4 : undefined}
      select={!!field.options}
      disabled={pending}
      value={current}
      onChange={(e) => onChange(e.target.value)}
      error={!!error}
      helperText={
        error ??
        (field.key === 'dateOfWork'
          ? 'Use the work date in Asia/Colombo.'
          : resource === '/job-card' && field.key === 'endDate'
            ? 'Required by the current quotation validation rules.'
            : undefined)
      }
      slotProps={{
        inputLabel: { shrink: true },
        htmlInput: {
          step: 'any',
          min: field.type === 'date' ? (minDate ?? undefined) : field.min,
          max:
            field.key === 'dateOfWork'
              ? businessToday()
              : endKey
                ? values[endKey] || undefined
                : undefined,
        },
      }}
    >
      {field.options?.map((option) => (
        <MenuItem key={option} value={option}>
          {option.charAt(0) + option.slice(1).toLowerCase()}
        </MenuItem>
      ))}
    </TextField>
  );
}
