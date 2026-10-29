import { useState } from 'react';
import { Button, MenuItem, Stack, TextField } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../shared/api';
import { useAuth } from '../auth/AuthProvider';
import { PageHeader } from '../../shared/ui/Layout';
import { DataTableShell } from '../../shared/ui/DataTableShell';
import { EmptyState, ErrorState } from '../../shared/States';
import { label, type Row } from './WorkflowPage';
import { reportContracts } from './reports';
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
  return (
    <Stack spacing={3}>
      <PageHeader
        title="Reports"
        description="Current server reports. Revenue reflects completed invoices."
      />
      <Stack
        component="form"
        spacing={2}
        onSubmit={(e) => {
          e.preventDefault();
          let url = path;
          const params = new URLSearchParams();
          for (const f of spec.fields) {
            if (path.includes(`{${f}}`))
              url = url.replace(`{${f}}`, encodeURIComponent(values[f] ?? ''));
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
              {r.title}
            </MenuItem>
          ))}
        </TextField>
        {spec.fields.map((f) => (
          <TextField
            key={f}
            label={label(f)}
            type={
              f.endsWith('Date') ? 'date' : f.endsWith('Id') ? 'number' : 'text'
            }
            required={path !== '/job-cards'}
            value={values[f] ?? ''}
            onChange={(e) => setValues({ ...values, [f]: e.target.value })}
            helperText={
              f === 'status'
                ? path.startsWith('/job')
                  ? 'PENDING, COMPLETED or CANCELED'
                  : 'DRAFT, COMPLETED, CANCELED or REVISED'
                : undefined
            }
            slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: 1 } }}
          />
        ))}
        <Button type="submit" variant="contained" disabled={q.isFetching}>
          Run report
        </Button>
      </Stack>
      {q.isError ? (
        <ErrorState error={q.error} retry={() => void q.refetch()} />
      ) : (
        <DataTableShell
          label="Report results"
          columns={keys.map((k) => ({
            key: k,
            label: label(k),
            render: (r: Row) => String(r[k] ?? '—'),
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
