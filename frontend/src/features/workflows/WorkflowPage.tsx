import { RowActions } from '../../shared/ui/RowActions';
import { useRef, useState } from 'react';
import {
  Alert,
  Box,
  Chip,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Typography,
  TextField,
} from '@mui/material';
import {
  directories,
  singular,
  headings,
  Value,
  Metrics,
  fieldSection,
} from './presentation';
import { FormInput, useFormLookups, validateRelations } from './formRelations';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, normalizeError } from '../../shared/api';
import { useAuth } from '../auth/AuthProvider';
import { PageHeader } from '../../shared/ui/Layout';
import { DataTableShell, ConfirmDialog } from '../../shared/ui/DataTableShell';
import { InvoiceDetails } from './InvoiceDetails';
import {
  FilterToolbar,
  SearchField,
  TableToolbar,
} from '../../shared/ui/Filters';
import { EmptyState, ErrorState, LoadingState } from '../../shared/States';
import { contracts, type Field, type Action, type Contract } from './contracts';
export type Row = Record<string, string | number | boolean | null>;
export const label = (key: string) =>
  key.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase());
export function RecordForm({
  fields,
  initial = {},
  title,
  submit,
  close,
  resource,
}: {
  resource?: string;
  fields: Field[];
  initial?: Row;
  title: string;
  submit: (body: Row) => Promise<unknown>;
  close: () => void;
}) {
  const lock = useRef(false);
  const {
    watch,
    setValue,
    handleSubmit,
    setError,
    clearErrors,
    formState: { errors },
  } = useForm<Row>({
    defaultValues: Object.fromEntries(
      fields.map((f) => [f.key, initial[f.key] ?? '']),
    ),
  });
  const lookups = useFormLookups(fields, resource);
  const values = watch();
  const sectionFor = (key: string) =>
    resource === '/timesheets' && fieldSection(key) === 'Hours & pricing'
      ? 'Hours'
      : fieldSection(key);
  const mutation = useMutation({
    mutationFn: submit,
    onSuccess: close,
    onError: (e) => {
      for (const f of normalizeError(e).fields)
        setError(f.field, { message: f.message });
    },
  });
  return (
    <Dialog
      open
      fullWidth
      maxWidth="sm"
      onClose={() => {
        if (!lock.current) close();
      }}
      disableEscapeKeyDown={mutation.isPending}
      aria-labelledby="record-form-title"
    >
      <DialogTitle id="record-form-title">{title}</DialogTitle>
      {resource === '/invoices' && title === 'Revise' && (
        <Alert severity="info" sx={{ mx: 3 }}>
          Confirming creates a new draft revision and marks this invoice as
          revised. Review the values before creating the revision.
        </Alert>
      )}
      {title === 'Add Job Card' && (
        <Alert severity="info" sx={{ mx: 3 }}>
          You will be recorded as the salesperson. Operator assignment is
          optional.
        </Alert>
      )}
      <form
        onSubmit={handleSubmit(async (values) => {
          if (lock.current) return;
          const body: Row = {};
          let invalid = false;
          for (const f of fields) {
            const v = values[f.key];
            const text = String(v ?? '').trim();
            if (f.required && !text) {
              setError(f.key, { message: 'This field is required.' });
              invalid = true;
            }
            if (
              f.type === 'number' &&
              text &&
              (!Number.isFinite(Number(text)) ||
                (f.min === 0 ? Number(text) < 0 : Number(text) <= 0) ||
                (f.key.endsWith('Id') && !Number.isInteger(Number(text))))
            ) {
              setError(f.key, { message: 'Enter a valid positive number.' });
              invalid = true;
            }
            if (
              text &&
              ((f.type === 'email' &&
                !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(text)) ||
                (f.options && !f.options.includes(text)))
            ) {
              setError(f.key, { message: 'Enter a valid value.' });
              invalid = true;
            }
            body[f.key] =
              text === ''
                ? null
                : f.type === 'number'
                  ? Number(text)
                  : f.type === 'password'
                    ? String(v)
                    : text;
          }
          for (const [key, message] of Object.entries(
            validateRelations(resource, fields, values, initial, lookups),
          )) {
            setError(key, { message });
            invalid = true;
          }
          if (invalid) return;
          lock.current = true;
          try {
            await mutation.mutateAsync(body);
          } catch {
            /* Keep form and normalized error. */
          } finally {
            lock.current = false;
          }
        })}
        noValidate
      >
        <DialogContent>
          <Stack
            spacing={2}
            component="fieldset"
            disabled={mutation.isPending}
            sx={{ border: 0, p: 0, m: 0 }}
          >
            {mutation.isError && <ErrorState error={mutation.error} />}{' '}
            {(resource === '/quotations' || resource === '/job-card') &&
              !fields.some((f) => f.key === 'customerId') &&
              initial.customerId != null && (
                <TextField
                  label="Customer"
                  value={
                    initial.customerName ?? initial.customer ?? 'Not provided'
                  }
                  slotProps={{ input: { readOnly: true } }}
                  helperText="The customer cannot be changed on this existing record."
                />
              )}
            {resource === '/timesheets' &&
              !fields.some((f) => f.key === 'jobCardId') &&
              initial.jobCardId != null && (
                <TextField
                  label="Job card"
                  value={initial.jobCardNumber ?? 'Not provided'}
                  slotProps={{ input: { readOnly: true } }}
                  helperText="This timesheet remains linked to its original job card."
                />
              )}
            {Array.from(new Set(fields.map((f) => sectionFor(f.key)))).map(
              (section) => (
                <Box key={section} component="section">
                  <Typography
                    variant="overline"
                    color="text.secondary"
                    sx={{ display: 'block', mb: 1.5 }}
                  >
                    {section}
                  </Typography>
                  <Box
                    sx={{
                      display: 'grid',
                      gridTemplateColumns: {
                        xs: '1fr',
                        sm: 'repeat(2,minmax(0,1fr))',
                      },
                      gap: 2,
                    }}
                  >
                    {fields
                      .filter((f) => sectionFor(f.key) === section)
                      .map((f) => (
                        <Box
                          key={f.key}
                          sx={
                            f.key === 'description'
                              ? { gridColumn: '1 / -1' }
                              : undefined
                          }
                        >
                          <FormInput
                            resource={resource}
                            values={values}
                            initial={initial}
                            lookups={lookups}
                            pending={mutation.isPending}
                            field={f}
                            value={watch(f.key)}
                            onChange={(v) => {
                              setValue(f.key, v);
                              if (
                                f.key === 'customerId' &&
                                String(values.customerId) !== v
                              ) {
                                const quote = lookups[
                                  '/lookups/quotations'
                                ]?.data?.find(
                                  (r) =>
                                    String(r.id) === String(values.quotationId),
                                );
                                if (!quote || String(quote.customerId) !== v)
                                  setValue('quotationId', '');
                              }
                              clearErrors();
                            }}
                            error={errors[f.key]?.message}
                          />
                        </Box>
                      ))}
                  </Box>
                </Box>
              ),
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button disabled={mutation.isPending} onClick={close}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={mutation.isPending}
          >
            {mutation.isPending
              ? 'Saving…'
              : resource === '/invoices' && title === 'Revise'
                ? 'Create revision'
                : 'Save'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
function RecordDialog({
  contract,
  id,
  mode,
  refresh,
  close,
}: {
  contract: Contract;
  id: number;
  mode: 'view' | 'edit' | Action;
  refresh: () => Promise<void>;
  close: () => void;
}) {
  const q = useQuery({
    queryKey: [contract.path, id],
    queryFn: async () => (await api.get<Row>(`${contract.path}/${id}`)).data,
    staleTime: 0,
    gcTime: 0,
  });
  if (q.data && mode !== 'view')
    return (
      <RecordForm
        resource={contract.path}
        title={typeof mode === 'string' ? `Edit ${contract.title}` : mode.label}
        fields={typeof mode === 'string' ? contract.editFields : mode.fields!}
        initial={q.data}
        close={close}
        submit={async (body) => {
          await api.request({
            url: `${contract.path}/${id}${typeof mode === 'string' ? '' : mode.path}`,
            method: typeof mode === 'string' ? 'put' : mode.method,
            data: body,
          });
          await refresh();
        }}
      />
    );
  const detailColumns = contract.columns.filter(
    (k) => k !== 'id' && !k.endsWith('Id'),
  );
  return (
    <Dialog open fullWidth onClose={close} aria-labelledby="detail-title">
      <DialogTitle id="detail-title">
        {contract.path === '/admin/users'
          ? 'User'
          : contract.path === '/invoices'
            ? 'Invoice'
            : contract.title}{' '}
        details
      </DialogTitle>
      <DialogContent>
        {q.isPending ? (
          <LoadingState />
        ) : q.isError ? (
          <ErrorState error={q.error} retry={() => void q.refetch()} />
        ) : contract.path === '/invoices' ? (
          <InvoiceDetails invoice={q.data} />
        ) : (
          <Stack spacing={3}>
            {Array.from(new Set(detailColumns.map(fieldSection))).map(
              (section) => (
                <Box key={section}>
                  <Typography variant="overline" color="text.secondary">
                    {section}
                  </Typography>
                  <Box
                    component="dl"
                    sx={{
                      display: 'grid',
                      gridTemplateColumns: {
                        xs: '1fr',
                        sm: 'repeat(2,minmax(0,1fr))',
                      },
                      gap: 2,
                      m: 0,
                      mt: 1,
                    }}
                  >
                    {detailColumns
                      .filter((k) => fieldSection(k) === section)
                      .map((k) => (
                        <Box
                          key={k}
                          sx={
                            k === 'description'
                              ? { gridColumn: '1 / -1', whiteSpace: 'pre-wrap' }
                              : undefined
                          }
                        >
                          <Typography
                            component="dt"
                            variant="caption"
                            color="text.secondary"
                          >
                            {headings[k] ?? label(k)}
                          </Typography>
                          <Box
                            component="dd"
                            sx={{ m: 0, overflowWrap: 'anywhere' }}
                          >
                            {k === 'description' ? (
                              String(q.data[k] ?? '—')
                            ) : (
                              <Value field={k} row={q.data} />
                            )}
                          </Box>
                        </Box>
                      ))}
                  </Box>
                </Box>
              ),
            )}
          </Stack>
        )}
      </DialogContent>
      <DialogActions>
        <Button autoFocus onClick={close}>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
}
export function WorkflowPage({ module }: { module: string }) {
  const c = contracts[module];
  const { user } = useAuth();
  const cache = useQueryClient();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);
  const [notice, setNotice] = useState('');
  const [create, setCreate] = useState(false);
  const [selected, setSelected] = useState<{
    id: number;
    mode: 'view' | 'edit' | Action;
  } | null>(null);
  const [confirm, setConfirm] = useState<{ row: Row; action: Action } | null>(
    null,
  );
  const lock = useRef(false);
  const actionTrigger = useRef<HTMLElement | null>(null);
  const fallback = useRef<HTMLButtonElement>(null);
  const restore = () =>
    requestAnimationFrame(() =>
      (actionTrigger.current?.isConnected
        ? actionTrigger.current
        : fallback.current
      )?.focus(),
    );
  const parents = useQuery({
    queryKey: ['/job-card'],
    queryFn: async ({ signal }) =>
      (await api.get<Row[]>('/job-card', { signal })).data,
    enabled: module === 'timesheets' && !!user && c.roles.includes(user.role),
    staleTime: 0,
  });
  const canChange = (r: Row) =>
    module !== 'timesheets' ||
    (!parents.isError &&
      !parents.isFetching &&
      parents.data?.some(
        (job) => job.id === r.jobCardId && job.status === 'PENDING',
      ));
  const q = useQuery({
    queryKey: [c.path],
    queryFn: async ({ signal }) =>
      (await api.get<Row[]>(c.path, { signal })).data,
    enabled: !!user && c.roles.includes(user.role),
  });
  const refresh = async () => {
    await Promise.all(
      [
        c.path,
        '/job-card',
        '/timesheets',
        '/invoices',
        '/maintenance',
        '/quotations',
        'dashboard',
        'lookups',
        'reports',
        'cranes',
      ].map((key) => cache.invalidateQueries({ queryKey: [key] })),
    );
    setNotice('Changes saved.');
  };
  const mutation = useMutation({
    mutationFn: async () => {
      if (confirm)
        await api.request({
          url: `${c.path}/${confirm.row.id}${confirm.action.path}`,
          method: confirm.action.method,
        });
    },
    onSuccess: async () => {
      await refresh();
      setConfirm(null);
      restore();
    },
  });
  if (!user || !c.roles.includes(user.role))
    return (
      <EmptyState
        title="Access denied"
        description="Your role does not permit access to this page."
      />
    );
  const rows = (q.data ?? []).filter(
    (r) =>
      (!status || r.status === status) &&
      Object.values(r).some((v) =>
        String(v ?? '')
          .toLowerCase()
          .includes(search.trim().toLowerCase()),
      ),
  );
  const bounded = Math.min(
    page,
    Math.max(0, Math.ceil(rows.length / size) - 1),
  );
  return (
    <Stack spacing={3}>
      <PageHeader
        title={c.title}

        actions={
          c.createRoles.includes(user.role) ? (
            <Button variant="contained" onClick={() => setCreate(true)}>
              Add {singular[module]}
            </Button>
          ) : undefined
        }
      />
      {notice && <Alert onClose={() => setNotice('')}>{notice}</Alert>}
      {q.data && !q.isError && (
        <Metrics
          items={[
            {
              label:
                module === 'jobs'
                  ? 'Total Jobs'
                  : `Total ${c.title.toLowerCase()}`,
              value: q.data.length,
            },
            ...(module === 'jobs'
              ? ['PENDING', 'COMPLETED', 'CANCELED']
              : module === 'invoices'
                ? ['DRAFT', 'COMPLETED', 'CANCELED']
                : module === 'maintenance'
                  ? ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED']
                  : []
            ).map((state) => ({
              label: label(state.toLowerCase().replaceAll('_', ' ')),
              value: q.data.filter((r) => r.status === state).length,
            })),
          ]}
        />
      )}
      {module === 'timesheets' && parents.isError && (
        <ErrorState
          error={parents.error}
          retry={() => void parents.refetch()}
        />
      )}
      {q.isError ? (
        <ErrorState error={q.error} retry={() => void q.refetch()} />
      ) : (
        <DataTableShell
          label={c.title}
          columns={[
            ...directories[module].map((k) => ({
              key: k,
              label: headings[k] ?? label(k),
              render: (r: Row) => <Value field={k} row={r} />,
            })),
            {
              key: 'actions',
              label: 'Actions',
              align: 'right',
              render: (r: Row) => (
                <RowActions
                  reference={String(r[directories[module][0]] ?? r.id)}
                  disabled={mutation.isPending}
                  view={(element) => {
                    actionTrigger.current = element;
                    setSelected({ id: Number(r.id), mode: 'view' });
                  }}
                  edit={
                    canChange(r) &&
                    c.editRoles.includes(user.role) &&
                    (!c.editStates || c.editStates.includes(String(r.status)))
                      ? (element) => {
                          actionTrigger.current = element;
                          setSelected({ id: Number(r.id), mode: 'edit' });
                        }
                      : undefined
                  }
                  actions={[
                    ...c.actions
                      .filter(
                        (a) =>
                          canChange(r) &&
                          a.roles.includes(user.role) &&
                          (!a.states || a.states.includes(String(r.status))) &&
                          !(module === 'users' && r.role === 'ADMIN'),
                      )
                      .map((a) => ({
                        label: a.label,
                        run: (element: HTMLElement) => {
                          actionTrigger.current = element;
                          mutation.reset();
                          if (a.fields)
                            setSelected({ id: Number(r.id), mode: a });
                          else setConfirm({ row: r, action: a });
                        },
                      })),
                  ]}
                />
              ),
            },
          ]}
          rows={rows.slice(bounded * size, (bounded + 1) * size)}
          rowKey={(r) => String(r.id)}
          loading={q.isPending}
          page={bounded}
          count={rows.length}
          rowsPerPage={size}
          onPageChange={setPage}
          onRowsPerPageChange={(n) => {
            setSize(n);
            setPage(0);
          }}
          toolbar={
            <TableToolbar title={`${c.title} directory`}>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                <SearchField
                  value={search}
                  onChange={(v) => {
                    setSearch(v);
                    setPage(0);
                  }}
                />
                <Button
                  ref={fallback}
                  onClick={() => {
                    void q.refetch();
                    if (module === 'timesheets') void parents.refetch();
                  }}
                  disabled={q.isFetching}
                >
                  Refresh {c.title}
                </Button>
              </Stack>
            </TableToolbar>
          }
          filters={
            c.columns.includes('status') ? (
              <FilterToolbar
                onClear={
                  status || search
                    ? () => {
                        setStatus('');
                        setSearch('');
                        setPage(0);
                      }
                    : undefined
                }
              >
                {[
                  '',
                  ...Array.from(
                    new Set((q.data ?? []).map((r) => String(r.status))),
                  ),
                ].map((state) => (
                  <Chip
                    key={state}
                    label={
                      state
                        ? label(state.toLowerCase().replaceAll('_', ' '))
                        : 'All statuses'
                    }
                    variant={status === state ? 'filled' : 'outlined'}
                    color={status === state ? 'primary' : 'default'}
                    onClick={() => {
                      setStatus(state);
                      setPage(0);
                    }}
                  />
                ))}
              </FilterToolbar>
            ) : undefined
          }
          empty={
            <EmptyState
              title="No matching records"
              description="Add a record or adjust your search."
            />
          }
        />
      )}
      {create && (
        <RecordForm
          resource={c.path}
          title={`Add ${singular[module]}`}
          fields={c.createFields}
          close={() => setCreate(false)}
          submit={async (body) => {
            await api.post(c.path, body);
            await refresh();
          }}
        />
      )}
      {selected && (
        <RecordDialog
          contract={c}
          {...selected}
          refresh={refresh}
          close={() => {
            setSelected(null);
            restore();
          }}
        />
      )}
      <ConfirmDialog
        open={!!confirm}
        title={
          module === 'operators'
            ? `Delete ${confirm?.row.operatorName ?? 'operator'}?`
            : `${confirm?.action.label ?? ''} record?`
        }
        description={
          module === 'operators'
            ? 'Permanently delete this operator? This cannot be undone. If the operator is referenced by existing records, deletion may be blocked and the record will be retained.'
            : module === 'timesheets'
              ? 'Permanently delete this timesheet? This cannot be undone. The parent job must still be pending.'
              : module === 'jobs' && confirm?.action.label === 'Complete'
                ? 'Completion requires timesheets and generates an invoice. Review the recorded hours first.'
                : confirm?.action.label === 'Deactivate'
                  ? 'This record will no longer be available for new assignments. Existing records are retained.'
                  : `Review this ${singular[module].toLowerCase()} before confirming ${confirm?.action.label.toLowerCase() ?? 'the change'}. This changes its workflow state.`
        }
        pending={mutation.isPending}
        returnFocusRef={actionTrigger}
        onClose={() => {
          setConfirm(null);
          restore();
        }}
        onConfirm={() => {
          if (lock.current) return;
          lock.current = true;
          void mutation
            .mutateAsync()
            .catch(() => {})
            .finally(() => {
              lock.current = false;
            });
        }}
        feedback={
          mutation.isError ? <ErrorState error={mutation.error} /> : undefined
        }
      />
    </Stack>
  );
}
