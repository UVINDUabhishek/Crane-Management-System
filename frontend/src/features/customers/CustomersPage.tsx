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
import { canWriteCustomers } from '../../shared/permissions';
import { PageHeader } from '../../shared/ui/Layout';
import {
  DataTableShell,
  ConfirmDialog,
  type Column,
} from '../../shared/ui/DataTableShell';
import {
  FilterToolbar,
  SearchField,
  TableToolbar,
} from '../../shared/ui/Filters';
import { EmptyState, ErrorState } from '../../shared/States';
import {
  customersApi,
  customerKeys,
  type CustomerResponse,
} from './customersApi';
import { CustomerFormDialog } from './CustomerFormDialog';
import { CustomerDetailsDialog } from './CustomerDetailsDialog';
type Selection = {
  kind: 'view' | 'edit' | 'deactivate';
  customer: CustomerResponse;
};
export function CustomersPage() {
  const { user } = useAuth(),
    cache = useQueryClient();
  const write = !!user && canWriteCustomers(user.role);
  const reduced = useMediaQuery('(prefers-reduced-motion: reduce)');
  const [status, setStatus] = useState('All');
  const [search, setSearch] = useState(''),
    [emailOnly, setEmailOnly] = useState(false),
    [page, setPage] = useState(0),
    [size, setSize] = useState(5);
  const [menu, setMenu] = useState<{
      anchor: HTMLElement;
      customer: CustomerResponse;
    } | null>(null),
    [selection, setSelection] = useState<Selection | null>(null),
    [create, setCreate] = useState(false),
    [notice, setNotice] = useState('');
  const queued = useRef<Selection | null>(null),
    trigger = useRef<HTMLElement | null>(null),
    fallback = useRef<HTMLButtonElement>(null),
    lock = useRef(false);
  const query = useQuery({
    queryKey: customerKeys.all,
    queryFn: ({ signal }) => customersApi.list(signal),
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
    setNotice(create ? 'Customer created.' : 'Customer updated.');
    close();
  };
  const deactivate = useMutation({
    mutationFn: customersApi.activation,
    onSuccess: async () => {
      await cache.invalidateQueries({ queryKey: customerKeys.all });
      setNotice('Customer activation updated.');
      await cache.invalidateQueries({ queryKey: ['lookups'] });
      close();
    },
  });
  const term = search.trim().toLowerCase();
  const rows = (query.data ?? []).filter(
    (c) =>
      (status === 'All' || c.active === (status === 'Active')) &&
      (!emailOnly || !!c.customerEmail?.trim()) &&
      [
        c.companyName,
        c.registrationNo,
        c.vatNo,
        c.contactPerson,
        c.contactNo,
        c.customerEmail,
      ].some((v) => v?.toLowerCase().includes(term)),
  );
  const currentPage = Math.min(
    page,
    Math.max(0, Math.ceil(rows.length / size) - 1),
  );
  const clear = () => {
    setSearch('');
    setStatus('All');
    setEmailOnly(false);
    setPage(0);
  };
  const columns: Column<CustomerResponse>[] = [
    {
      key: 'active',
      label: 'Activation',
      render: (c) => (
        <ActivationControl
          active={c.active}
          name={c.companyName}
          disabled={!write || deactivate.isPending}
          onChange={(element) => {
            trigger.current = element;
            deactivate.reset();
            setSelection({ kind: 'deactivate', customer: c });
          }}
        />
      ),
    },
    ...(['companyName', 'vatNo', 'contactPerson', 'contactNo'] as const).map(
      (key, index) => ({
        key,
        label: [
          'Company name',
          'VAT number',
          'Contact person',
          'Contact number',
        ][index],
        render: (c: CustomerResponse) => c[key] || '—',
      }),
    ),
    {
      key: 'actions',
      label: 'Actions',
      align: 'right',
      render: (c) => (
        <IconButton
          aria-label={`Actions for ${c.companyName}`}
          aria-haspopup="menu"
          aria-expanded={menu?.customer.id === c.id}
          onClick={(e) => {
            trigger.current = e.currentTarget;
            setMenu({ anchor: e.currentTarget, customer: c });
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
        title="Customers"
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
              Add customer
            </Button>
          ) : undefined
        }
      />
      {query.isError ? (
        <ErrorState error={query.error} retry={() => void query.refetch()} />
      ) : (
        <DataTableShell
          label="Customers"
          columns={columns}
          rows={rows.slice(currentPage * size, (currentPage + 1) * size)}
          rowKey={(c) => String(c.id)}
          isInactive={(c) => !c.active}
          loading={query.isPending}
          loadingLabel="Loading customers…"
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
              title="Customer directory"
              description={
                query.data
                  ? `${query.data.length} customers · Includes deactivated records`
                  : 'Loading directory'
              }
            >
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                <SearchField
                  label="Search customers"
                  value={search}
                  onChange={(v) => {
                    setSearch(v);
                    setPage(0);
                  }}
                />
                <Button
                  ref={fallback}
                  onClick={() => void query.refetch()}
                  disabled={query.isFetching}
                >
                  Refresh customers
                </Button>
              </Stack>
            </TableToolbar>
          }
          filters={
            <FilterToolbar
              onClear={
                search || emailOnly || status !== 'All' ? clear : undefined
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
              <Chip
                label="Has contact email"
                variant="outlined"
                color={emailOnly ? 'primary' : 'default'}
                aria-pressed={emailOnly}
                onClick={() => {
                  setEmailOnly((v) => !v);
                  setPage(0);
                }}
              />
            </FilterToolbar>
          }
          empty={
            <EmptyState
              title={
                query.data?.length
                  ? 'No matching customers'
                  : 'No customers yet'
              }
              description={
                query.data?.length
                  ? 'Try another search or clear the email filter.'
                  : 'Customer records will appear here when added.'
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
                if (menu) queued.current = { kind, customer: menu.customer };
                deactivate.reset();
                setMenu(null);
              }}
            >
              {kind === 'view'
                ? 'View details'
                : kind === 'edit'
                  ? 'Edit'
                  : 'Deactivate'}
            </MenuItem>
          ),
        )}
      </Menu>
      {create && write && (
        <CustomerFormDialog onClose={close} onSaved={saved} />
      )}
      {selection && selection.kind !== 'deactivate' && (
        <CustomerDetailsDialog
          id={selection.customer.id}
          edit={selection.kind === 'edit' && write}
          onClose={close}
          onSaved={saved}
        />
      )}
      <ConfirmDialog
        open={selection?.kind === 'deactivate' && write}
        title={`${selection?.customer.active ? 'Deactivate' : 'Activate'} ${selection?.customer.companyName ?? 'customer'}?`}
        description="Inactive customers are excluded from active customer lookups. Existing records and relationships are retained. You can change activation again later."
        confirmLabel={
          selection?.customer.active
            ? 'Deactivate customer'
            : 'Activate customer'
        }
        pending={deactivate.isPending}
        pendingLabel={
          selection?.customer.active ? 'Deactivating…' : 'Activating…'
        }
        returnFocusRef={trigger}
        onClose={close}
        onConfirm={() => {
          if (!write || !selection || lock.current) return;
          lock.current = true;
          void deactivate
            .mutateAsync({
              id: selection.customer.id,
              active: !selection.customer.active,
            })
            .catch(() => {})
            .finally(() => {
              lock.current = false;
            });
        }}
        feedback={
          deactivate.isError ? (
            <ErrorState error={deactivate.error} />
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
