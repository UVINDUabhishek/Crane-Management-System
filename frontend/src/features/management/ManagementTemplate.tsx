import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Drawer,
  FormControlLabel,
  Checkbox,
  IconButton,
  Menu,
  MenuItem,
  Stack,
  Typography,
} from '@mui/material';
import AddOutlined from '@mui/icons-material/AddOutlined';
import CloseOutlined from '@mui/icons-material/CloseOutlined';
import MoreHoriz from '@mui/icons-material/MoreHoriz';
import { PageHeader } from '../../shared/ui/Layout';
import {
  SearchField,
  FilterToolbar,
  TableToolbar,
} from '../../shared/ui/Filters';
import {
  ConfirmDialog,
  DataTableShell,
  StatusChip,
  type Column,
} from '../../shared/ui/DataTableShell';
import { EmptyState } from '../../shared/States';
type Example = { id: string; name: string; active: boolean };
// Presentation-only fixtures: deliberately not customer API DTOs or actual records.
const examples: Example[] = [
  { id: 'EXAMPLE-01', name: 'Example company A', active: true },
  { id: 'EXAMPLE-02', name: 'Example company B', active: false },
  { id: 'EXAMPLE-03', name: 'Example company C', active: true },
];
export function ManagementTemplate() {
  const [search, setSearch] = useState(''),
    [activeOnly, setActiveOnly] = useState(false),
    [preview, setPreview] = useState(false),
    [drawer, setDrawer] = useState(false),
    [page, setPage] = useState(0),
    [size, setSize] = useState(5),
    [anchor, setAnchor] = useState<HTMLElement | null>(null),
    [info, setInfo] = useState(false);
  const filtered = (preview ? examples : []).filter(
    (r) =>
      (!activeOnly || r.active) &&
      `${r.name} ${r.id}`.toLowerCase().includes(search.toLowerCase()),
  );
  const clear = () => {
    setSearch('');
    setActiveOnly(false);
    setPage(0);
  };
  const columns: Column<Example>[] = [
    {
      key: 'name',
      label: 'Company',
      render: (r) => (
        <Box>
          <Typography variant="body2" fontWeight={650}>
            {r.name}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            Layout example only
          </Typography>
        </Box>
      ),
    },
    {
      key: 'reference',
      label: 'Reference',
      render: (r) => (
        <Typography variant="body2" color="text.secondary">
          {r.id}
        </Typography>
      ),
    },
    {
      key: 'status',
      label: 'Status',
      render: (r) => (
        <StatusChip
          label={r.active ? 'Active' : 'Inactive'}
          tone={r.active ? 'success' : 'neutral'}
        />
      ),
    },
    {
      key: 'actions',
      label: 'Actions',
      align: 'right',
      render: (r) => (
        <IconButton
          aria-label={`Actions for ${r.name}`}
          aria-haspopup="menu"
          onClick={(e) => setAnchor(e.currentTarget)}
        >
          <MoreHoriz />
        </IconButton>
      ),
    },
  ];
  return (
    <Stack spacing={3}>
      <PageHeader
        eyebrow="RELATIONSHIPS"
        title="Customers"
        description="A consistent workspace for your customer directory."
        actions={
          <Button variant="contained" startIcon={<AddOutlined />} disabled>
            Add customer
          </Button>
        }
      />
      <Alert severity="info">
        Layout preview · Customer records and actions are not connected yet.
        Sample rows are examples only.
      </Alert>
      <DataTableShell
        label="Customer layout preview"
        columns={columns}
        rows={filtered.slice(page * size, (page + 1) * size)}
        rowKey={(r) => r.id}
        page={page}
        rowsPerPage={size}
        count={filtered.length}
        onPageChange={setPage}
        onRowsPerPageChange={(n) => {
          setSize(n);
          setPage(0);
        }}
        toolbar={
          <TableToolbar
            title="Customer directory"
            description={
              preview
                ? 'Sample layout · Not live customer data'
                : 'Ready for the customer module'
            }
          >
            <SearchField
              label="Search company or reference"
              value={search}
              onChange={(v) => {
                setSearch(v);
                setPage(0);
              }}
            />
          </TableToolbar>
        }
        filters={
          <FilterToolbar
            onMore={() => setDrawer(true)}
            onClear={search || activeOnly ? clear : undefined}
          >
            <Chip
              label={activeOnly ? 'Status: Active' : 'All statuses'}
              color={activeOnly ? 'primary' : 'default'}
              variant="outlined"
              onClick={() => {
                setActiveOnly((v) => !v);
                setPage(0);
              }}
              onDelete={
                activeOnly
                  ? () => {
                      setActiveOnly(false);
                      setPage(0);
                    }
                  : undefined
              }
            />
            <FormControlLabel
              sx={{ ml: 0 }}
              control={
                <Checkbox
                  size="small"
                  checked={preview}
                  onChange={(e) => {
                    setPreview(e.target.checked);
                    setPage(0);
                  }}
                />
              }
              label={
                <Typography variant="body2">Show sample layout</Typography>
              }
            />
          </FilterToolbar>
        }
        empty={
          <EmptyState
            title={
              preview
                ? 'No matching examples'
                : 'Your customer workspace starts here'
            }
            description={
              preview
                ? 'Try another search or clear your filters.'
                : 'Customer data will appear here when the module is connected. Preview the layout to explore the table pattern.'
            }
            action={
              preview ? (
                <Button onClick={clear}>Clear filters</Button>
              ) : (
                <Button variant="outlined" onClick={() => setPreview(true)}>
                  Preview sample rows
                </Button>
              )
            }
          />
        }
      />
      <Drawer
        anchor="right"
        open={drawer}
        onClose={() => setDrawer(false)}
        slotProps={{
          paper: {
            sx: { width: { xs: '100%', sm: 360 }, p: 3 },
            role: 'dialog',
            'aria-label': 'Customer filters',
          },
        }}
      >
        <Stack
          direction="row"
          justifyContent="space-between"
          alignItems="center"
        >
          <Typography variant="h6">Filters</Typography>
          <IconButton
            aria-label="Close filters"
            onClick={() => setDrawer(false)}
          >
            <CloseOutlined />
          </IconButton>
        </Stack>
        <Typography variant="body2" color="text.secondary" sx={{ my: 2 }}>
          Refine the sample layout. No live records are queried.
        </Typography>
        <FormControlLabel
          control={
            <Checkbox
              checked={activeOnly}
              onChange={(e) => {
                setActiveOnly(e.target.checked);
                setPage(0);
              }}
            />
          }
          label="Active examples only"
        />
        <Button
          sx={{ mt: 3 }}
          variant="contained"
          onClick={() => setDrawer(false)}
        >
          Show results
        </Button>
      </Drawer>
      <Menu anchorEl={anchor} open={!!anchor} onClose={() => setAnchor(null)}>
        <MenuItem disabled>View customer · Coming later</MenuItem>
        <MenuItem disabled>Edit customer · Coming later</MenuItem>
        <MenuItem
          onClick={() => {
            setAnchor(null);
            setInfo(true);
          }}
        >
          About this example
        </MenuItem>
      </Menu>
      <ConfirmDialog
        open={info}
        title="Sample record"
        description="This row demonstrates the table layout. It is not a customer record and cannot be edited or saved."
        confirmLabel="Got it"
        onClose={() => setInfo(false)}
        onConfirm={() => setInfo(false)}
      />
    </Stack>
  );
}
