import { useState } from 'react';
import {
  Autocomplete,
  Box,
  Button,
  Card,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { api } from '../../shared/api';
import { useAuth } from '../auth/AuthProvider';
import { PageHeader } from '../../shared/ui/Layout';
import { AccessDenied, ErrorState } from '../../shared/States';
import { label } from './WorkflowPage';
import { ReportResults } from './ReportResults';
import type { ReportRow } from './reportPresentation';
import {
  reportContracts,
  reportFilterErrors,
  reportRequestUrl,
  reportStatuses,
  type ReportCategory,
} from './reports';
function RelationshipFilter({
  field,
  value,
  change,
  error,
}: {
  error?: string;
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
        loading={q.isFetching}
        noOptionsText="No active records. Enter a historical record ID if needed."
        getOptionKey={(option) =>
          typeof option === 'string' ? option : option.id
        }
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
            label={
              field === 'salesPersonId'
                ? 'Sales Person'
                : label(field.replace(/Id$/, ''))
            }
            required
            helperText={
              error ??
              'Select an active record, or enter an ID for historical records.'
            }
            error={!!error}
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
  const [path, setPath] = useState(allowed[0]?.path ?? '');
  const spec = allowed.find((r) => r.path === path) ?? allowed[0];
  const [values, setValues] = useState<Record<string, string>>({});
  const [submitted, setSubmitted] = useState(false);
  // An applied request is separate from draft filters. Changes discard it, never rerun it.
  const [request, setRequest] = useState<{ path: string; url: string } | null>(
    null,
  );
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(5);
  const validRequest = !!request && request.path === spec?.path;
  const q = useQuery({
    queryKey: ['reports', validRequest ? request.url : null],
    queryFn: async ({ signal }) =>
      (
        await api.get<ReportRow | ReportRow[]>(`/reports${request!.url}`, {
          signal,
        })
      ).data,
    enabled: validRequest,
  });
  if (!spec) return <AccessDenied />;
  const categories = Array.from(new Set(allowed.map((r) => r.category)));
  const types = allowed.filter((r) => r.category === spec.category);
  const errors = reportFilterErrors(spec, values, submitted);
  const rows =
    validRequest && q.data ? (Array.isArray(q.data) ? q.data : [q.data]) : [];
  const resetResults = () => {
    setRequest(null);
    setPage(0);
    setSubmitted(false);
  };
  const selectType = (next: string) => {
    setPath(next);
    setValues({});
    resetResults();
  };
  const update = (field: string, value: string) => {
    setValues((current) => ({ ...current, [field]: value }));
    resetResults();
  };
  const jobDates = spec.path === '/job-cards';
  return (
    <Stack spacing={3}>
      <PageHeader
        title="Reports"
        description="Current server reports. Revenue reflects completed invoices."
      />
      <Card sx={{ p: 2.5 }}>
        <Stack
          component="form"
          aria-label="Report filters"
          noValidate
          spacing={2}
          onSubmit={(e) => {
            e.preventDefault();
            setSubmitted(true);
            if (Object.keys(reportFilterErrors(spec, values, true)).length)
              return;
            setPage(0);
            const url = reportRequestUrl(spec, values);
            if (validRequest && request.url === url) void q.refetch();
            else setRequest({ path: spec.path, url });
          }}
        >
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: 'repeat(2,minmax(0,1fr))' },
              gap: 2,
            }}
          >
            <TextField
              select
              label="Report Category"
              value={spec.category}
              onChange={(e) =>
                selectType(
                  allowed.find(
                    (r) => r.category === (e.target.value as ReportCategory),
                  )!.path,
                )
              }
            >
              {categories.map((category) => (
                <MenuItem key={category} value={category}>
                  {category}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label="Report Type"
              value={spec.path}
              onChange={(e) => selectType(e.target.value)}
            >
              {types.map((type) => (
                <MenuItem key={type.path} value={type.path}>
                  {type.title}
                </MenuItem>
              ))}
            </TextField>
          </Box>
          {spec.fields.length > 0 && (
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
              {spec.fields.map((field) =>
                field.endsWith('Id') ? (
                  <RelationshipFilter
                    key={`${spec.path}-${field}`}
                    field={field}
                    value={values[field] ?? ''}
                    change={(v) => update(field, v)}
                    error={errors[field]}
                  />
                ) : (
                  <TextField
                    key={`${spec.path}-${field}`}
                    label={
                      field === 'status'
                        ? 'Status'
                        : `${jobDates ? 'Job Date ' : ''}${field === 'fromDate' ? 'From' : 'To'}${jobDates ? '' : ' Date'}`
                    }
                    value={values[field] ?? ''}
                    onChange={(e) => update(field, e.target.value)}
                    select={field === 'status'}
                    type={field === 'status' ? 'text' : 'date'}
                    required={!jobDates}
                    error={!!errors[field]}
                    helperText={errors[field]}
                    slotProps={{
                      inputLabel: { shrink: true },
                      htmlInput: {
                        min: field === 'toDate' ? values.fromDate : undefined,
                        max: field === 'fromDate' ? values.toDate : undefined,
                      },
                    }}
                  >
                    {field === 'status' &&
                      reportStatuses(spec).map((state) => (
                        <MenuItem key={state} value={state}>
                          {label(state.toLowerCase())}
                        </MenuItem>
                      ))}
                  </TextField>
                ),
              )}
            </Box>
          )}
          {jobDates && (
            <Typography variant="caption" color="text.secondary">
              Filters job card creation dates (jobDate), not scheduled or worked
              dates. Leave both dates empty for all records.
            </Typography>
          )}
          <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
            <Button
              type="submit"
              variant="contained"
              disabled={validRequest && q.isFetching}
            >
              Run report
            </Button>
            <Button
              disabled={!Object.values(values).some(Boolean)}
              onClick={() => {
                setValues({});
                resetResults();
              }}
            >
              Clear filters
            </Button>
          </Stack>
        </Stack>
      </Card>
      {validRequest && q.isError ? (
        <ErrorState error={q.error} retry={() => void q.refetch()} />
      ) : (
        <ReportResults
          spec={spec}
          rows={rows}
          loading={validRequest && q.isFetching}
          requested={validRequest}
          page={page}
          size={size}
          setPage={setPage}
          setSize={setSize}
        />
      )}
    </Stack>
  );
}
