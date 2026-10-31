import { useId, useState, type ReactNode } from 'react';
import {
  Box,
  IconButton,
  Menu,
  MenuItem,
  Stack,
  Typography,
} from '@mui/material';
import MoreHoriz from '@mui/icons-material/MoreHoriz';
import {
  StatusChip,
  type BusinessStatus,
} from '../../shared/ui/DataTableShell';
import type { Row } from './WorkflowPage';

export const singular: Record<string, string> = {
  jobs: 'Job Card',
  quotations: 'Quotation',
  timesheets: 'Timesheet',
  maintenance: 'Maintenance Record',
  invoices: 'Invoice',
  users: 'User',
  operators: 'Operator',
};
export const directories: Record<string, string[]> = {
  jobs: ['jobCardNumber', 'customer', 'crane', 'schedule', 'hours', 'status'],
  quotations: [
    'quotationNumber',
    'customerName',
    'type',
    'validity',
    'minHours',
    'minRate',
    'mobilization',
  ],
  timesheets: [
    'timesheetNumber',
    'jobCardNumber',
    'customerName',
    'dateOfWork',
    'workedHours',
  ],
  maintenance: [
    'maintenanceNumber',
    'crane',
    'maintenanceType',
    'maintenanceSchedule',
    'maintenancePerson',
    'status',
  ],
  invoices: [
    'invoiceNumber',
    'customer',
    'invoiceDate',
    'amount',
    'status',
    'version',
  ],
  users: ['username', 'email', 'role'],
  operators: ['operatorName'],
};
export const headings: Record<string, string> = {
  schedule: 'Scheduled dates',
  hours: 'Hours',
  validity: 'Valid Period',
  maintenanceSchedule: 'Scheduled dates',
  amount: 'Total',
  minRate: 'Minimum rate',
  minHours: 'Minimum hours',
  qty: 'Quantity',
  jobCardNumber: 'Job card',
  maintenancePerson: 'Assigned to',
};
const statuses = new Set([
  'ACTIVE',
  'INACTIVE',
  'AVAILABLE',
  'IN_USE',
  'MAINTENANCE',
  'PENDING',
  'COMPLETED',
  'CANCELED',
  'SCHEDULED',
  'IN_PROGRESS',
  'DRAFT',
  'REVISED',
]);
export const formatValue = (key: string, value: Row[string]): string => {
  if (value === null || value === undefined || value === '') return '—';
  if (
    key === 'version' &&
    typeof value === 'number' &&
    Number.isInteger(value) &&
    value >= 1
  )
    return value === 1 ? 'Original' : `Revision ${value - 1}`;
  if (typeof value === 'number')
    return new Intl.NumberFormat('en', {
      maximumFractionDigits: 2,
      minimumFractionDigits: /amount|rate|revenue|mobilization/i.test(key)
        ? 2
        : 0,
    }).format(value);
  if (
    /date|validFrom|validTo/i.test(key) &&
    /^\d{4}-\d{2}-\d{2}/.test(String(value))
  ) {
    const date = new Date(String(value).slice(0, 10) + 'T12:00:00');
    if (!Number.isNaN(date.getTime()))
      return new Intl.DateTimeFormat('en-GB', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      }).format(date);
  }
  return String(value);
};
export function Value({ field, row }: { field: string; row: Row }) {
  if (field === 'status' && statuses.has(String(row[field])))
    return <StatusChip status={row[field] as BusinessStatus} />;
  if (['schedule', 'validity', 'maintenanceSchedule'].includes(field)) {
    const keys =
      field === 'schedule'
        ? ['startDate', 'endDate']
        : field === 'validity'
          ? ['validFrom', 'validTo']
          : ['maintenanceStartDate', 'maintenanceEndDate'];
    return (
      <Stack sx={{ whiteSpace: 'nowrap' }}>
        <Typography variant="body2">
          {formatValue(keys[0], row[keys[0]])}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          to {row[keys[1]] ? formatValue(keys[1], row[keys[1]]) : 'Open-ended'}
        </Typography>
      </Stack>
    );
  }
  if (field === 'hours')
    return (
      <Stack sx={{ whiteSpace: 'nowrap' }}>
        <Typography variant="body2">
          {formatValue('totalWorkedHours', row.totalWorkedHours)} worked
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {formatValue('totalBillableHours', row.totalBillableHours)} billable
        </Typography>
      </Stack>
    );
  return (
    <Typography
      variant="body2"
      title={formatValue(field, row[field])}
      sx={{
        maxWidth: 240,
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        fontWeight: /Number$|amount/.test(field) ? 650 : 400,
        fontVariantNumeric: 'tabular-nums',
      }}
    >
      {formatValue(field, row[field])}
    </Typography>
  );
}
export function RowMenu({
  reference,
  actions,
}: {
  reference: string;
  actions: { label: string; run: () => void }[];
}) {
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const id = useId();
  return (
    <>
      <IconButton
        aria-label={`Actions for ${reference}`}
        aria-haspopup="menu"
        aria-controls={anchor ? id : undefined}
        aria-expanded={!!anchor}
        onClick={(e) => setAnchor(e.currentTarget)}
      >
        <MoreHoriz />
      </IconButton>
      <Menu
        id={id}
        anchorEl={anchor}
        open={!!anchor}
        onClose={() => setAnchor(null)}
      >
        {actions.map((action) => (
          <MenuItem
            key={action.label}
            onClick={() => {
              setAnchor(null);
              action.run();
            }}
          >
            {action.label}
          </MenuItem>
        ))}
      </Menu>
    </>
  );
}
export function Metrics({
  items,
}: {
  items: { label: string; value: ReactNode }[];
}) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: {
          xs: 'repeat(2,minmax(0,1fr))',
          md: `repeat(${Math.min(items.length, 4)},minmax(0,1fr))`,
        },
        gap: 1.5,
      }}
    >
      {items.map((item) => (
        <Box
          key={item.label}
          sx={{
            px: 2,
            py: 1.5,
            bgcolor: 'background.paper',
            border: 1,
            borderColor: 'divider',
            borderRadius: '10px',
          }}
        >
          <Typography variant="caption" color="text.secondary">
            {item.label}
          </Typography>
          <Typography
            sx={{
              fontSize: '1.5rem',
              fontWeight: 700,
              fontVariantNumeric: 'tabular-nums',
              overflowWrap: 'anywhere',
            }}
          >
            {item.value}
          </Typography>
        </Box>
      ))}
    </Box>
  );
}
export function fieldSection(key: string) {
  if (/password/i.test(key)) return 'Security';
  if (/Id$|customer|crane|operator|salesPerson|username|email|role/.test(key))
    return 'People & assignments';
  if (/date|valid/i.test(key)) return 'Schedule';
  if (/hours|rate|amount|qty|mobilization/i.test(key)) return 'Hours & pricing';
  return 'Record information';
}
