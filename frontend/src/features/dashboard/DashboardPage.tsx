import { useQuery } from '@tanstack/react-query';
import { Alert, Box, Chip, Paper, Stack, Typography } from '@mui/material';
import { api } from '../../shared/api';
import { ErrorState, Loading } from '../../shared/States';
const sections = [
  {
    title: 'Crane fleet',
    items: [
      ['totalCranes', 'Total cranes'],
      ['availableCranes', 'Available'],
      ['inUseCranes', 'In use'],
      ['maintenanceCranes', 'In maintenance'],
    ],
  },
  {
    title: 'Job cards',
    items: [
      ['pendingJobs', 'Pending jobs'],
      ['completedJobs', 'Completed jobs'],
      ['canceledJobs', 'Canceled jobs'],
    ],
  },
  {
    title: 'Invoices',
    items: [
      ['totalInvoices', 'Total invoices'],
      ['draftInvoices', 'Draft'],
      ['completedInvoices', 'Completed'],
      ['revisedInvoices', 'Revised'],
      ['canceledInvoices', 'Canceled'],
    ],
  },
] as const;
type CountKey = (typeof sections)[number]['items'][number][0];
export type DashboardResponse = Record<CountKey, number>;
export function DashboardPage() {
  const query = useQuery({
    queryKey: ['dashboard'],
    queryFn: async ({ signal }) =>
      (
        await api.get<Partial<DashboardResponse> | null>('/dashboard', {
          signal,
        })
      ).data,
  });
  if (query.isPending) return <Loading label="Loading dashboard…" />;
  if (query.isError)
    return (
      <ErrorState error={query.error} retry={() => void query.refetch()} />
    );
  if (!query.data)
    return <Alert severity="info">No dashboard data is available.</Alert>;
  return (
    <Stack spacing={4}>
      <Box>
        <Chip
          label="WORKSPACE OVERVIEW"
          size="small"
          color="primary"
          variant="outlined"
        />
        <Typography variant="h4" component="h1" sx={{ mt: 2 }}>
          Operations at a glance
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 1 }}>
          Current fleet, job-card and invoice counts.
        </Typography>
      </Box>
      {sections.map((section) => (
        <Box component="section" key={section.title}>
          <Typography variant="h6" sx={{ mb: 2 }}>
            {section.title}
          </Typography>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: {
                xs: '1fr',
                sm: 'repeat(2,1fr)',
                lg: `repeat(${section.items.length},1fr)`,
              },
              gap: 2,
            }}
          >
            {section.items.map(([key, label]) => (
              <Paper
                key={key}
                variant="outlined"
                sx={{
                  p: 3,
                  borderTop: '3px solid',
                  borderTopColor: 'primary.main',
                }}
              >
                <Typography color="text.secondary">{label}</Typography>
                <Typography
                  variant="h3"
                  sx={{ mt: 1, fontVariantNumeric: 'tabular-nums' }}
                >
                  {typeof query.data?.[key] === 'number' &&
                  Number.isFinite(query.data[key])
                    ? query.data[key]
                    : '—'}
                </Typography>
              </Paper>
            ))}
          </Box>
        </Box>
      ))}
    </Stack>
  );
}
