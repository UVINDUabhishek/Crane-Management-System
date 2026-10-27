import { ActivationControl } from '../../shared/ui/ActivationControl';
import { useRef, useState } from 'react';
import {
  Alert,
  Button,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Snackbar,
  Stack,
  useMediaQuery,
} from '@mui/material';
import MoreHoriz from '@mui/icons-material/MoreHoriz';
import AddOutlined from '@mui/icons-material/AddOutlined';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../auth/AuthProvider';
import { can } from '../../shared/permissions';
import { PageHeader } from '../../shared/ui/Layout';
import {
  DataTableShell,
  StatusChip,
  ConfirmDialog,
  type Column,
} from '../../shared/ui/DataTableShell';
import {
  FilterToolbar,
  SearchField,
  TableToolbar,
} from '../../shared/ui/Filters';
import { EmptyState, ErrorState } from '../../shared/States';
import { cranesApi, craneKeys, type CraneResponse } from './cranesApi';
import { CraneFormDialog } from './CraneFormDialog';
import { CraneDetailsDialog } from './CraneDetailsDialog';
type Selection = {
  kind: 'view' | 'edit' | 'activation';
  crane: CraneResponse;
};
export function CranesPage() {
  const { user } = useAuth(),
    cache = useQueryClient();
  const write = !!user && can(user.role, 'cranes');
  const reduced = useMediaQuery('(prefers-reduced-motion: reduce)');
  const [status, setStatus] = useState('All');
  const [search, setSearch] = useState(''),
    [operational, setOperational] = useState('All'),
    [page, setPage] = useState(0),
    [size, setSize] = useState(5);
  const [menu, setMenu] = useState<{
      anchor: HTMLElement;
      crane: CraneResponse;
    } | null>(null),
    [selection, setSelection] = useState<Selection | null>(null),
    [create, setCreate] = useState(false),
    [notice, setNotice] = useState('');
  const queued = useRef<Selection | null>(null),
    trigger = useRef<HTMLElement | null>(null),
    fallback = useRef<HTMLButtonElement>(null),
    lock = useRef(false);
  const query = useQuery({
    queryKey: craneKeys.all,
    queryFn: ({ signal }) => cranesApi.list(signal),
  });
  const restore = () => {
    const target = trigger.current?.isConnected
      ? trigger.current
      : fallback.current;
    target?.focus();
  };
  const close = () => {
    setSelection(null);
    setCreate(false);
    requestAnimationFrame(restore);
  };
  const saved = () => {
    setNotice(create ? 'Crane created.' : 'Crane updated.');
    close();
  };
  const activation = useMutation({
    mutationFn: cranesApi.activation,
    onSuccess: async () => {
      await cache.invalidateQueries({ queryKey: craneKeys.all });
      setNotice('Crane activation updated.');
      await cache.invalidateQueries({ queryKey: ['lookups'] });
      close();
    },
  });
  const term = search.trim().toLowerCase();
  const rows = (query.data ?? []).filter(
    (c) =>
      (status === 'All' || c.active === (status === 'Active')) &&
      (operational === 'All' || c.status === operational) &&
      [c.craneNumber, c.type, c.manufacturer, c.serialNumber, c.location].some(
        (v) => v?.toLowerCase().includes(term),
      ),
  );
  const currentPage = Math.min(
    page,
    Math.max(0, Math.ceil(rows.length / size) - 1),
  );
  const clear = () => {
    setSearch('');
    setStatus('All');
    setOperational('All');
    setPage(0);
  };
  const columns: Column<CraneResponse>[] = [
    {
      key: 'active',
      label: 'Activation',
      render: (c) => (
        <ActivationControl
          active={c.active}
          name={c.craneNumber}
          disabled={!write || activation.isPending}
          onChange={(element) => {
            trigger.current = element;
            activation.reset();
            setSelection({ kind: 'activation', crane: c });
          }}
        />
      ),
    },
    ...(
      [
        'craneNumber',
        'type',
        'capacity',
        'manufacturer',
        'serialNumber',
        'year',
        'location',
      ] as const
    ).map((key) => ({
      key,
      label: {
        craneNumber: 'Crane number',
        type: 'Type',
        capacity: 'Capacity',
        manufacturer: 'Manufacturer',
        serialNumber: 'Serial number',
        year: 'Year',
        location: 'Location',
      }[key],
      render: (c: CraneResponse) => c[key] ?? '—',
    })),
    {
      key: 'status',
      label: 'Operational status',
      render: (c) => <StatusChip status={c.status} />,
    },
    {
      key: 'actions',
      label: 'Actions',
      align: 'right',
      render: (c) => (
        <IconButton
          aria-label={`Actions for ${c.craneNumber}`}
          aria-haspopup="menu"
          aria-expanded={menu?.crane.id === c.id}
          onClick={(e) => {
            trigger.current = e.currentTarget;
            setMenu({ anchor: e.currentTarget, crane: c });
          }}
        >
          <MoreHoriz />
        </IconButton>
      ),
    },
  ];
  return (
    <Stack spacing={3}>
      <PageHeader
        eyebrow="FLEET"
        title="Cranes"
        description="Fleet specifications, activation and operational status."
        actions={
          write ? (
            <Button
              variant="contained"
              startIcon={<AddOutlined />}
              onClick={(e) => {
                trigger.current = e.currentTarget;
                setCreate(true);
              }}
            >
              Add crane
            </Button>
          ) : undefined
        }
      />
      <Button
        ref={fallback}
        sx={{ alignSelf: 'flex-start' }}
        onClick={() => void query.refetch()}
        disabled={query.isFetching}
      >
        Refresh cranes
      </Button>
      {query.isError ? (
        <ErrorState error={query.error} retry={() => void query.refetch()} />
      ) : (
        <DataTableShell
          label="Cranes"
          columns={columns}
          rows={rows.slice(currentPage * size, (currentPage + 1) * size)}
          rowKey={(c) => String(c.id)}
          loading={query.isPending}
          loadingLabel="Loading cranes…"
          page={currentPage}
          rowsPerPage={size}
          count={rows.length}
          onPageChange={setPage}
          onRowsPerPageChange={(n) => {
            setSize(n);
            setPage(0);
          }}
          toolbar={
            <TableToolbar
              title="Crane directory"
              description={
                query.data
                  ? `${query.data.length} cranes · Includes deactivated records`
                  : 'Loading directory'
              }
            >
              <SearchField
                label="Search cranes"
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
              onClear={
                search || operational !== 'All' || status !== 'All'
                  ? clear
                  : undefined
              }
            >
              {['All', 'Active', 'Inactive'].map((value) => (
                <Chip
                  key={value}
                  label={value}
                  aria-pressed={status === value}
                  color={status === value ? 'primary' : 'default'}
                  onClick={() => {
                    setStatus(value);
                    setPage(0);
                  }}
                />
              ))}
              {(['All', 'AVAILABLE', 'IN_USE', 'MAINTENANCE'] as const).map(
                (value) => (
                  <Chip
                    key={value}
                    label={
                      value === 'All'
                        ? 'All operational statuses'
                        : value === 'IN_USE'
                          ? 'In use'
                          : value === 'AVAILABLE'
                            ? 'Available'
                            : 'Maintenance'
                    }
                    aria-pressed={operational === value}
                    color={operational === value ? 'primary' : 'default'}
                    onClick={() => {
                      setOperational(value);
                      setPage(0);
                    }}
                  />
                ),
              )}
            </FilterToolbar>
          }
          empty={
            <EmptyState
              title={
                query.data?.length ? 'No matching cranes' : 'No cranes yet'
              }
              description={
                query.data?.length
                  ? 'Try another search or clear the filters.'
                  : 'Crane records will appear here when added.'
              }
              action={
                query.data?.length ? (
                  <Button onClick={clear}>Clear filters</Button>
                ) : undefined
              }
            />
          }
        />
      )}
      <Menu
        anchorEl={menu?.anchor}
        open={!!menu}
        onClose={() => setMenu(null)}
        transitionDuration={reduced ? 0 : undefined}
        slotProps={{
          transition: {
            onExited: () => {
              if (queued.current) {
                setSelection(queued.current);
                queued.current = null;
              }
            },
          },
        }}
      >
        {(['view', ...(write ? (['edit'] as const) : [])] as const).map(
          (kind) => (
            <MenuItem
              key={kind}
              onClick={() => {
                if (menu) queued.current = { kind, crane: menu.crane };
                activation.reset();
                setMenu(null);
              }}
            >
              {kind === 'view' ? 'View details' : 'Edit'}
            </MenuItem>
          ),
        )}
      </Menu>
      {create && write && <CraneFormDialog onClose={close} onSaved={saved} />}
      {selection && selection.kind !== 'activation' && (
        <CraneDetailsDialog
          id={selection.crane.id}
          edit={selection.kind === 'edit' && write}
          onClose={close}
          onSaved={saved}
        />
      )}
      <ConfirmDialog
        open={selection?.kind === 'activation' && write}
        title={`${selection?.crane.active ? 'Deactivate' : 'Activate'} ${selection?.crane.craneNumber ?? 'crane'}?`}
        description="Inactive cranes are excluded from active selection lists. Historical records and operational status are preserved. Reactivation is supported. Unfinished jobs or maintenance can prevent deactivation."
        confirmLabel={
          selection?.crane.active ? 'Deactivate crane' : 'Activate crane'
        }
        pending={activation.isPending}
        pendingLabel={selection?.crane.active ? 'Deactivating…' : 'Activating…'}
        returnFocusRef={trigger}
        onClose={close}
        onConfirm={() => {
          if (!write || !selection || lock.current) return;
          lock.current = true;
          void activation
            .mutateAsync({
              id: selection.crane.id,
              active: !selection.crane.active,
            })
            .catch(() => {})
            .finally(() => {
              lock.current = false;
            });
        }}
        feedback={
          activation.isError ? (
            <ErrorState error={activation.error} />
          ) : undefined
        }
      />
      <Snackbar
        open={!!notice}
        autoHideDuration={8000}
        onClose={() => setNotice('')}
      >
        <Alert severity="success" onClose={() => setNotice('')}>
          {notice}
        </Alert>
      </Snackbar>
    </Stack>
  );
}
