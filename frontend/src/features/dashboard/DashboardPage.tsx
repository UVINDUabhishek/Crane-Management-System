import { useQuery } from '@tanstack/react-query';
import { Box, Button, Stack } from '@mui/material';
import {
  ConstructionOutlined,
  AssignmentOutlined,
  ReceiptLongOutlined,
  RefreshOutlined,
} from '@mui/icons-material';
import { PageHeader, SectionCard, StatCard } from '../../shared/ui/Layout';
import { EmptyState } from '../../shared/States';
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
    return (
      <EmptyState
        title="No dashboard data is available."
        description="Try refreshing the overview to load the latest counts."
        action={
          <Button onClick={() => void query.refetch()}>Refresh overview</Button>
        }
      />
    );
  const icon = [
    <ConstructionOutlined fontSize="small" />,
    <AssignmentOutlined fontSize="small" />,
    <ReceiptLongOutlined fontSize="small" />,
  ];
  const descriptions = [
    'Fleet availability and current crane status',
    'Current job-card status across your operations',
    'Invoice counts by document status',
  ];
  return (
    <Stack spacing={3}>
      <PageHeader
        eyebrow="OPERATIONS OVERVIEW"
        title="Operations at a glance"
        description="A clear view of your fleet, active work and invoice records."
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshOutlined />}
            disabled={query.isFetching}
            onClick={() => void query.refetch()}
          >
            {query.isFetching ? 'Refreshing…' : 'Refresh overview'}
          </Button>
        }
      />
      {sections.map((section, index) => (
        <SectionCard
          key={section.title}
          title={section.title}
          description={descriptions[index]}
        >
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: {
                xs: '1fr',
                sm: 'repeat(2,minmax(0,1fr))',
                lg: `repeat(${section.items.length},minmax(0,1fr))`,
              },
              gap: 2,
              p: { xs: 2, md: 3 },
            }}
          >
            {section.items.map(([key, label], itemIndex) => (
              <StatCard
                key={key}
                label={label}
                value={
                  typeof query.data?.[key] === 'number' &&
                  Number.isFinite(query.data[key])
                    ? query.data[key]
                    : '—'
                }
                description={
                  index === 0
                    ? 'Current fleet count'
                    : index === 1
                      ? 'Job-card records'
                      : 'Invoice records'
                }
                icon={icon[index]}
                accent={itemIndex === 0}
              />
            ))}
          </Box>
        </SectionCard>
      ))}
    </Stack>
  );
}
