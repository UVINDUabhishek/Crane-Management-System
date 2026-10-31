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
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import {
  directories,
  singular,
  headings,
  Value,
  RowMenu,
  Metrics,
  fieldSection,
} from './presentation';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, normalizeError } from '../../shared/api';
import { useAuth } from '../auth/AuthProvider';
import { PageHeader } from '../../shared/ui/Layout';
import { DataTableShell, ConfirmDialog } from '../../shared/ui/DataTableShell';
import { SearchField, TableToolbar } from '../../shared/ui/Filters';
import { EmptyState, ErrorState, LoadingState } from '../../shared/States';
import { contracts, type Field, type Action, type Contract } from './contracts';
export type Row = Record<string, string | number | boolean | null>;
export const label = (key: string) =>
  key.replace(/([A-Z])/g, ' $1').replace(/^./, (c) => c.toUpperCase());
function Input({
  field,
  value,
  onChange,
  error,
}: {
  field: Field;
  value: unknown;
  onChange: (v: string) => void;
  error?: string;
}) {
  const options = useQuery({
    queryKey: ['lookups', field.lookup],
    queryFn: async () => (await api.get<Row[]>(field.lookup!)).data,
    enabled: !!field.lookup,
  });
  const items =
    options.data
      ?.filter(
        (r) =>
          field.lookup !== '/job-card' ||
          r.status === 'PENDING' ||
          String(r.id) === String(value),
      )
      .map((r) => ({
        id: String(r.id),
        label: String(
          r.label ??
            (r.jobCardNumber
              ? `${r.jobCardNumber} · ${r.customer ?? ''}`
              : r.id),
        ),
      })) ?? [];
  const current = String(value ?? '');
  return (
    <Stack spacing={1}>
      {options.isError && (
        <ErrorState
          error={options.error}
          retry={() => void options.refetch()}
        />
      )}
      <TextField
        fullWidth
        label={field.label}
        required={field.required}
        type={field.lookup || field.options ? 'text' : field.type}
        select={!!(field.lookup || field.options)}
        value={current}
        onChange={(e) => onChange(e.target.value)}
        error={!!error}
        helperText={
          error ?? (options.isFetching ? 'Loading options…' : undefined)
        }
        slotProps={{
          inputLabel: { shrink: true },
          htmlInput: { step: 'any', min: field.min },
        }}
      >
        {(field.lookup || field.options) && (
          <MenuItem value="">Select {field.label.toLowerCase()}</MenuItem>
        )}
        {field.options?.map((v) => (
          <MenuItem key={v} value={v}>
            {label(v.toLowerCase())}
          </MenuItem>
        ))}
        {field.lookup && current && !items.some((i) => i.id === current) && (
          <MenuItem value={current}>Current record #{current}</MenuItem>
        )}
        {items.map((o) => (
          <MenuItem key={o.id} value={o.id}>
            {o.label}
          </MenuItem>
        ))}
      </TextField>
    </Stack>
  );
}
export function RecordForm({
  fields,
  initial = {},
  title,
  submit,
  close,
}: {
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
            {Array.from(new Set(fields.map((f) => fieldSection(f.key)))).map(
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
                      .filter((f) => fieldSection(f.key) === section)
                      .map((f) => (
                        <Input
                          key={f.key}
                          field={f}
                          value={watch(f.key)}
                          onChange={(v) => {
                            setValue(f.key, v);
                            clearErrors(f.key);
                          }}
                          error={errors[f.key]?.message}
                        />
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
            {mutation.isPending ? 'Saving…' : 'Save'}
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
  return (
    <Dialog open fullWidth onClose={close} aria-labelledby="detail-title">
      <DialogTitle id="detail-title">{contract.title} details</DialogTitle>
      <DialogContent>
        {q.isPending ? (
          <LoadingState />
        ) : q.isError ? (
          <ErrorState error={q.error} retry={() => void q.refetch()} />
        ) : (
          <Stack spacing={3}>
            {Array.from(new Set(contract.columns.map(fieldSection))).map(
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
                    {contract.columns
                      .filter((k) => fieldSection(k) === section)
                      .map((k) => (
                        <Box key={k}>
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
              render: (r: Row) => (
                <RowMenu
                  reference={String(r[directories[module][0]] ?? r.id)}
                  actions={[
                    {
                      label: 'Details',
                      run: () =>
                        setSelected({ id: Number(r.id), mode: 'view' }),
                    },
                    ...(c.editRoles.includes(user.role) &&
                    (!c.editStates || c.editStates.includes(String(r.status)))
                      ? [
                          {
                            label: 'Edit',
                            run: () =>
                              setSelected({ id: Number(r.id), mode: 'edit' }),
                          },
                        ]
                      : []),
                    ...c.actions
                      .filter(
                        (a) =>
                          a.roles.includes(user.role) &&
                          (!a.states || a.states.includes(String(r.status))) &&
                          !(module === 'users' && r.role === 'ADMIN'),
                      )
                      .map((a) => ({
                        label: a.label,
                        run: () => {
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
                  onClick={() => void q.refetch()}
                  disabled={q.isFetching}
                >
                  Refresh {c.title}
                </Button>
              </Stack>
            </TableToolbar>
          }
          filters={
            c.columns.includes('status') ? (
              <Stack
                direction="row"
                spacing={1}
                sx={{ p: 2, flexWrap: 'wrap', gap: 1 }}
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
                {(status || search) && (
                  <Button
                    size="small"
                    onClick={() => {
                      setStatus('');
                      setSearch('');
                      setPage(0);
                    }}
                  >
                    Clear filters
                  </Button>
                )}
              </Stack>
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
          close={() => setSelected(null)}
        />
      )}
      <ConfirmDialog
        open={!!confirm}
        title={`${confirm?.action.label ?? ''} record?`}
        description={
          module === 'jobs' && confirm?.action.label === 'Complete'
            ? 'Completion requires timesheets and generates an invoice. Review the recorded hours first.'
            : confirm?.action.label === 'Deactivate'
              ? 'This record will no longer be available for new assignments. Existing records are retained.'
              : `Review this ${singular[module].toLowerCase()} before confirming ${confirm?.action.label.toLowerCase() ?? 'the change'}. This changes its workflow state.`
        }
        pending={mutation.isPending}
        onClose={() => setConfirm(null)}
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
