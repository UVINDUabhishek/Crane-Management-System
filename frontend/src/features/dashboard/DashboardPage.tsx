import { useQuery } from '@tanstack/react-query';
import { Box, Button, Stack } from '@mui/material';
import ConstructionOutlined from '@mui/icons-material/ConstructionOutlined';
import AssignmentOutlined from '@mui/icons-material/AssignmentOutlined';
import ReceiptLongOutlined from '@mui/icons-material/ReceiptLongOutlined';
import RefreshOutlined from '@mui/icons-material/RefreshOutlined';
import CheckCircleOutline from '@mui/icons-material/CheckCircleOutline';
import HourglassEmptyOutlined from '@mui/icons-material/HourglassEmptyOutlined';
import BuildOutlined from '@mui/icons-material/BuildOutlined';
import CancelOutlined from '@mui/icons-material/CancelOutlined';
import EditNoteOutlined from '@mui/icons-material/EditNoteOutlined';
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
  const header = (
    <PageHeader
      title="Operations at a glance"
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
  );
  if (query.isPending)
    return (
      <Stack spacing={3}>
        {header}
        <Loading label="Loading dashboard…" />
      </Stack>
    );
  if (query.isError)
    return (
      <Stack spacing={3}>
        {header}
        <ErrorState error={query.error} retry={() => void query.refetch()} />
      </Stack>
    );
  if (!query.data)
    return (
      <Stack spacing={3}>
        {header}
        <EmptyState
          title="No dashboard data is available."
          description="Try refreshing the overview to load the latest counts."
          action={
            <Button onClick={() => void query.refetch()}>
              Refresh overview
            </Button>
          }
        />
      </Stack>
    );
  const detail: Record<
    CountKey,
    {
      description: string;
      icon: React.ReactNode;
      tone?: 'primary' | 'success' | 'warning';
    }
  > = {
    totalCranes: {
      description: 'All cranes in the fleet',
      icon: <ConstructionOutlined />,
    },
    availableCranes: {
      description: 'Marked available',
      icon: <CheckCircleOutline />,
      tone: 'success',
    },
    inUseCranes: {
      description: 'Currently marked in use',
      icon: <ConstructionOutlined />,
    },
    maintenanceCranes: {
      description: 'Marked under maintenance',
      icon: <BuildOutlined />,
      tone: 'warning',
    },
    pendingJobs: {
      description: 'Awaiting completion',
      icon: <HourglassEmptyOutlined />,
      tone: 'warning',
    },
    completedJobs: {
      description: 'Completed job cards',
      icon: <CheckCircleOutline />,
      tone: 'success',
    },
    canceledJobs: {
      description: 'Canceled job cards',
      icon: <CancelOutlined />,
    },
    totalInvoices: {
      description: 'All invoice records',
      icon: <ReceiptLongOutlined />,
    },
    draftInvoices: {
      description: 'Draft invoice records',
      icon: <EditNoteOutlined />,
    },
    completedInvoices: {
      description: 'Completed invoices',
      icon: <CheckCircleOutline />,
      tone: 'success',
    },
    revisedInvoices: {
      description: 'Revised invoice records',
      icon: <AssignmentOutlined />,
    },
    canceledInvoices: {
      description: 'Canceled invoices',
      icon: <CancelOutlined />,
    },
  };
  const descriptions = [
    'Fleet availability and current crane status',
    'Current job-card status across your operations',
    'Invoice counts by document status',
  ];
  return (
    <Stack spacing={3}>
      {header}
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
                description={detail[key].description}
                icon={detail[key].icon}
                tone={detail[key].tone}
                accent={itemIndex === 0}
              />
            ))}
          </Box>
        </SectionCard>
      ))}
    </Stack>
  );
}
