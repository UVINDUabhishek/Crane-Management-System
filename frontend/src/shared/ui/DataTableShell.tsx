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
import type { ReactNode } from 'react';
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
  toolbar,
  filters,
  empty,
  page,
  rowsPerPage,
  count,
  onPageChange,
  onRowsPerPageChange,
}: {
  label: string;
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  toolbar?: ReactNode;
  filters?: ReactNode;
  empty: ReactNode;
  page: number;
  rowsPerPage: number;
  count: number;
  onPageChange: (page: number) => void;
  onRowsPerPageChange: (size: number) => void;
}) {
  return (
    <Card>
      {toolbar}
      {filters}
      <TableContainer
        tabIndex={0}
        aria-label={`${label} scroll area`}
        sx={{ overflowX: 'auto' }}
      >
        <Table aria-label={label} sx={{ minWidth: 640 }}>
          <TableHead>
            <TableRow>
              {columns.map((c) => (
                <TableCell key={c.key} align={c.align}>
                  {c.label}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row) => (
              <TableRow hover key={rowKey(row)}>
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
      {rows.length === 0 && <Box>{empty}</Box>}
      <TablePagination
        component="div"
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
export function StatusChip({
  label,
  tone = 'neutral',
}: {
  label: string;
  tone?: 'neutral' | 'success' | 'warning';
}) {
  return (
    <Chip
      label={label}
      variant="outlined"
      color={tone === 'neutral' ? 'default' : tone}
      sx={{ '& .MuiChip-label': { px: 1.25 } }}
    />
  );
}
export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Confirm',
  onConfirm,
  onClose,
}: {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  onConfirm: () => void;
  onClose: () => void;
}) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      aria-labelledby="confirmation-title"
      maxWidth="xs"
      fullWidth
    >
      <DialogTitle id="confirmation-title">{title}</DialogTitle>
      <DialogContent>
        <DialogContentText>{description}</DialogContentText>
      </DialogContent>
      <DialogActions sx={{ p: 2 }}>
        <Button onClick={onClose}>Cancel</Button>
        <Button variant="contained" onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
