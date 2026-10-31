import { useState } from 'react';
import {
  Button,
  MenuItem,
  Stack,
  TextField,
  Box,
  Card,
  Typography,
  Autocomplete,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../shared/api';
import { useAuth } from '../auth/AuthProvider';
import { PageHeader } from '../../shared/ui/Layout';
import { DataTableShell } from '../../shared/ui/DataTableShell';
import { EmptyState, ErrorState } from '../../shared/States';
import { label, type Row } from './WorkflowPage';
import { Metrics, Value, formatValue } from './presentation';
import { reportContracts } from './reports';
function RelationshipFilter({
  field,
  value,
  change,
}: {
  field: string;
  value: string;
  change: (value: string) => void;
}) {
  const endpoint =
    field === 'customerId'
      ? '/lookups/customers'
      : field === 'craneId'
        ? '/lookups/cranes'
        : '/lookups/sales-people';
  const q = useQuery({
    queryKey: ['lookups', endpoint],
    queryFn: async () =>
      (await api.get<{ id: number; label: string }[]>(endpoint)).data,
  });
  return (
    <Stack spacing={1}>
      <Autocomplete
        freeSolo
        options={q.data ?? []}
        getOptionLabel={(option) =>
          typeof option === 'string'
            ? option
            : `${option.label} (#${option.id})`
        }
        value={q.data?.find((r) => String(r.id) === value) ?? value}
        onChange={(_, option) =>
          change(
            typeof option === 'string'
              ? option
              : option
                ? String(option.id)
                : '',
          )
        }
        onInputChange={(_, text, reason) => {
          if (reason === 'input') change(text);
        }}
        renderInput={(params) => (
          <TextField
            {...params}
            label={label(field.replace(/Id$/, ''))}
            required
            helperText="Select an active record, or enter an ID for historical records."
            error={!!value && !/^\d+$/.test(value)}
          />
        )}
      />
      {q.isError && (
        <ErrorState error={q.error} retry={() => void q.refetch()} />
      )}
    </Stack>
  );
}
export function ReportsPage() {
  const { user } = useAuth();
  const allowed = reportContracts.filter(
    (r) => user && r.roles.includes(user.role),
  );
  const [path, setPath] = useState(allowed[0].path);
  const spec = allowed.find((r) => r.path === path)!;
  const [values, setValues] = useState<Record<string, string>>({});
  const [request, setRequest] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);
  const q = useQuery({
    queryKey: ['reports', request],
    queryFn: async ({ signal }) =>
      (await api.get<Row | Row[]>(`/reports${request}`, { signal })).data,
    enabled: !!request,
  });
  const rows = q.data ? (Array.isArray(q.data) ? q.data : [q.data]) : [];
  const keys = Array.from(new Set(rows.flatMap(Object.keys)));
  const summary =
    !!q.data && !Array.isArray(q.data)
      ? Object.entries(q.data).filter(([, v]) => typeof v === 'number')
      : [];
  return (
    <Stack spacing={3}>
      <PageHeader
        title="Reports"
        description="Current server reports. Revenue reflects completed invoices."
      />
      <Card sx={{ p: 2.5 }}>
        <Typography variant="h6" component="h2" sx={{ mb: 2 }}>
          Report builder
        </Typography>
        <Stack
          component="form"
          spacing={2}
          onSubmit={(e) => {
            e.preventDefault();
            if (
              spec.fields.some(
                (f) => f.endsWith('Id') && !/^[1-9]\d*$/.test(values[f] ?? ''),
              )
            )
              return;
            let url = path;
            const params = new URLSearchParams();
            for (const f of spec.fields) {
              if (path.includes(`{${f}}`))
                url = url.replace(
                  `{${f}}`,
                  encodeURIComponent(values[f] ?? ''),
                );
              else if (values[f]) params.set(f, values[f]);
            }
            setPage(0);
            const target = url + (params.size ? '?' + params : '');
            setRequest(target);
            if (target === request) void q.refetch();
          }}
        >
          <TextField
            select
            label="Report"
            value={path}
            onChange={(e) => {
              setPath(e.target.value);
              setValues({});
              setRequest('');
            }}
          >
            {allowed.map((r) => (
              <MenuItem key={r.path} value={r.path}>
                {label(
                  r.title.replace(
                    / · (customerId|craneId|salesPersonId|status)$/g,
                    '',
                  ),
                )}
              </MenuItem>
            ))}
          </TextField>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', md: 'repeat(2,minmax(0,1fr))' },
              gap: 2,
            }}
          >
            {spec.fields.map((f) =>
              f.endsWith('Id') ? (
                <RelationshipFilter
                  key={`${path}-${f}`}
                  field={f}
                  value={values[f] ?? ''}
                  change={(v) => setValues({ ...values, [f]: v })}
                />
              ) : (
                <TextField
                  key={f}
                  label={label(f)}
                  select={f === 'status'}
                  type={
                    f.endsWith('Date')
                      ? 'date'
                      : f.endsWith('Id')
                        ? 'number'
                        : 'text'
                  }
                  required={path !== '/job-cards'}
                  value={values[f] ?? ''}
                  onChange={(e) =>
                    setValues({ ...values, [f]: e.target.value })
                  }
                  helperText={
                    f === 'status'
                      ? path.startsWith('/job')
                        ? 'PENDING, COMPLETED or CANCELED'
                        : 'DRAFT, COMPLETED, CANCELED or REVISED'
                      : undefined
                  }
                  slotProps={{
                    inputLabel: { shrink: true },
                    htmlInput: { min: 1 },
                  }}
                >
                  {f === 'status' &&
                    (path.startsWith('/job')
                      ? ['PENDING', 'COMPLETED', 'CANCELED']
                      : ['DRAFT', 'COMPLETED', 'CANCELED', 'REVISED']
                    ).map((state) => (
                      <MenuItem key={state} value={state}>
                        {label(state.toLowerCase())}
                      </MenuItem>
                    ))}
                </TextField>
              ),
            )}
          </Box>
          <Button type="submit" variant="contained" disabled={q.isFetching}>
            Run report
          </Button>
        </Stack>
      </Card>
      {!q.isError && summary.length > 0 && (
        <Metrics
          items={summary.map(([key, value]) => ({
            label: label(key),
            value: formatValue(key, value),
          }))}
        />
      )}
      {q.isError ? (
        <ErrorState error={q.error} retry={() => void q.refetch()} />
      ) : (
        <DataTableShell
          label="Report results"
          columns={keys.map((k) => ({
            key: k,
            label: label(k),
            render: (r: Row) => <Value field={k} row={r} />,
          }))}
          rows={rows.slice(page * size, (page + 1) * size)}
          rowKey={(r) => String(rows.indexOf(r))}
          page={page}
          rowsPerPage={size}
          count={rows.length}
          onPageChange={setPage}
          onRowsPerPageChange={(n) => {
            setSize(n);
            setPage(0);
          }}
          loading={q.isFetching}
          empty={
            <EmptyState
              title={request ? 'No report records' : 'Choose a report'}
              description="Select the report and run it to load current data."
            />
          }
        />
      )}
    </Stack>
  );
}
