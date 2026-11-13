import {
  Box,
  Card,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
} from '@mui/material';
import { useId, useRef, type ReactNode, type RefObject } from 'react';
import { alpha } from '@mui/material/styles';
import { useMediaQuery } from '@mui/material';
import { LoadingState } from '../States';
export interface Column<T> {
  key: string;
  label: string;
  render: (row: T) => ReactNode;
  align?: 'left' | 'right';
}
export function DataTableShell<T>({
  label,
  columns,
  rows,
  rowKey,
  isInactive,
  toolbar,
  filters,
  empty,
  page,
  rowsPerPage,
  count,
  loading = false,
  loadingLabel = 'Loading records…',
  onPageChange,
  onRowsPerPageChange,
}: {
  label: string;
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  /** Visually distinguish inactive records without disabling row actions. */
  isInactive?: (row: T) => boolean;
  toolbar?: ReactNode;
  filters?: ReactNode;
  empty: ReactNode;
  page: number;
  rowsPerPage: number;
  count: number;
  loading?: boolean;
  loadingLabel?: string;
  onPageChange: (page: number) => void;
  onRowsPerPageChange: (size: number) => void;
}) {
  return (
    <Card>
      {toolbar}
      {filters}
      <TableContainer
        role="region"
        tabIndex={0}
        aria-label={`${label} scroll area`}
        sx={{ overflowX: 'auto' }}
      >
        <Table aria-busy={loading} aria-label={label} sx={{ minWidth: 640 }}>
          <TableHead>
            <TableRow>
              {columns.map((c) => (
                <TableCell
                  component="th"
                  scope="col"
                  key={c.key}
                  align={c.key === 'actions' ? 'left' : c.align}
                  sx={
                    c.key === 'actions'
                      ? { width: 172, minWidth: 172 }
                      : undefined
                  }
                >
                  {c.label}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {!loading &&
              rows.map((row) => (
                <TableRow
                  hover
                  key={rowKey(row)}
                  sx={
                    isInactive?.(row)
                      ? (theme) => ({
                          bgcolor: alpha(
                            theme.palette.text.primary,
                            theme.palette.mode === 'dark' ? 0.09 : 0.045,
                          ),
                          '& > td': { color: 'text.secondary' },
                          '&:hover': {
                            bgcolor: alpha(
                              theme.palette.text.primary,
                              theme.palette.mode === 'dark' ? 0.14 : 0.075,
                            ),
                          },
                        })
                      : undefined
                  }
                >
                  {columns.map((c) => (
                    <TableCell key={c.key} align={c.align}>
                      {c.render(row)}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
          </TableBody>
        </Table>
      </TableContainer>
      {loading ? (
        <LoadingState label={loadingLabel} />
      ) : (
        rows.length === 0 && <Box>{empty}</Box>
      )}
      <TablePagination
        component="div"
        disabled={loading}
        count={count}
        page={page}
        rowsPerPage={rowsPerPage}
        rowsPerPageOptions={[5, 10, 25]}
        onPageChange={(_, p) => onPageChange(p)}
        onRowsPerPageChange={(e) => onRowsPerPageChange(Number(e.target.value))}
        sx={{
          borderTop: 1,
          borderColor: 'divider',
          '& .MuiTablePagination-toolbar': {
            flexWrap: 'wrap',
            justifyContent: 'flex-end',
            px: 2,
          },
          '& .MuiTablePagination-spacer': { display: 'none' },
        }}
      />
    </Card>
  );
}
const statuses = {
  ACTIVE: ['Active', 'success'],
  INACTIVE: ['Inactive', 'default'],
  AVAILABLE: ['Available', 'success'],
  IN_USE: ['In use', 'info'],
  MAINTENANCE: ['Maintenance', 'warning'],
  PENDING: ['Pending', 'warning'],
  COMPLETED: ['Completed', 'success'],
  CANCELED: ['Canceled', 'default'],
  SCHEDULED: ['Scheduled', 'info'],
  IN_PROGRESS: ['In progress', 'info'],
  DRAFT: ['Draft', 'default'],
  REVISED: ['Revised', 'warning'],
} as const;
export type BusinessStatus = keyof typeof statuses;
type ChipTone = 'neutral' | 'success' | 'warning' | 'info' | 'error';
export function StatusChip(
  props:
    | { status: BusinessStatus; label?: never; tone?: never }
    | { status?: never; label: string; tone?: ChipTone },
) {
  const [label, color] = props.status
    ? statuses[props.status]
    : ([
        props.label,
        props.tone === 'neutral' || !props.tone ? 'default' : props.tone,
      ] as const);
  return (
    <Chip
      label={label}
      variant="outlined"
      color={color}
      sx={{ '& .MuiChip-label': { px: 1.25 } }}
    />
  );
}
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Confirm',
  pending = false,
  confirmDisabled = false,
  pendingLabel = 'Working…',
  returnFocusRef,
  fallbackFocusRef,
  feedback,
  onConfirm,
  onClose,
}: {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  pending?: boolean;
  confirmDisabled?: boolean;
  pendingLabel?: string;
  returnFocusRef?: RefObject<HTMLElement | null>;
  fallbackFocusRef?: RefObject<HTMLElement | null>;
  feedback?: ReactNode;
  onConfirm: () => void;
  onClose: () => void;
}) {
  const id = useId();
  const cancelRef = useRef<HTMLButtonElement>(null);
  const reducedMotion = useMediaQuery('(prefers-reduced-motion: reduce)');
  return (
    <Dialog
      open={open}
      onClose={() => {
        if (!pending) onClose();
      }}
      disableEscapeKeyDown={pending}
      transitionDuration={reducedMotion ? 0 : undefined}
      slotProps={{
        transition: {
          onEntered: () => {
            if (!pending) cancelRef.current?.focus();
          },
          onExited: () => {
            const target = returnFocusRef?.current;
            if (target?.isConnected && !target.matches(':disabled'))
              target.focus();
            else fallbackFocusRef?.current?.focus();
          },
        },
        paper: { 'aria-busy': pending },
      }}
      aria-labelledby={`${id}-title`}
      aria-describedby={`${id}-description`}
      maxWidth="xs"
      fullWidth
    >
      <DialogTitle id={`${id}-title`}>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText id={`${id}-description`}>
          {description}
        </DialogContentText>
        {feedback}
      </DialogContent>
      <DialogActions sx={{ p: 2 }}>
        <Button ref={cancelRef} autoFocus disabled={pending} onClick={onClose}>
          Cancel
        </Button>
        <Button
          variant="contained"
          disabled={pending || confirmDisabled}
          onClick={onConfirm}
        >
          {pending ? pendingLabel : confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
