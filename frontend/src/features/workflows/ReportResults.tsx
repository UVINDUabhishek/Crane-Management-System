import {
  Box,
  Card,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { DataTableShell } from '../../shared/ui/DataTableShell';
import { TableToolbar } from '../../shared/ui/Filters';
import { EmptyState, LoadingState } from '../../shared/States';
import { Value, formatValue } from './presentation';
import {
  reportColumns,
  type ReportColumn,
  type ReportRow,
} from './reportPresentation';
import type { ReportContract } from './reports';
function Cell({ column, row }: { column: ReportColumn; row: ReportRow }) {
  const value = row[column.key];
  if (column.kind === 'status')
    return <Value field="status" row={{ status: value ?? null }} />;
  return (
    <Typography
      variant="body2"
      sx={{
        minWidth:
          column.key === 'description'
            ? 180
            : column.kind === 'text'
              ? 88
              : undefined,
        maxWidth: column.kind === 'text' ? 280 : undefined,
        whiteSpace:
          column.key === 'description'
            ? 'pre-wrap'
            : column.kind === 'date'
              ? 'nowrap'
              : undefined,
        overflowWrap: 'anywhere',
        fontVariantNumeric: 'tabular-nums',
      }}
    >
      {value == null
        ? '—'
        : column.kind === 'text'
          ? String(value)
          : formatValue(column.key, value)}
    </Typography>
  );
}
export function ReportResults({
  spec,
  rows,
  loading,
  requested,
  page,
  size,
  setPage,
  setSize,
}: {
  spec: ReportContract;
  rows: ReportRow[];
  loading: boolean;
  requested: boolean;
  page: number;
  size: number;
  setPage: (page: number) => void;
  setSize: (size: number) => void;
}) {
  const columns = reportColumns[spec.view];
  const empty = (
    <EmptyState
      title={
        requested ? 'No report records' : 'Choose filters and run the report'
      }
      description={
        requested
          ? 'No records were returned for this selection.'
          : 'Results appear here after you run the selected report.'
      }
    />
  );
  const toolbar = (
    <TableToolbar
      title={spec.title}
      description={
        spec.view === 'summary'
          ? 'Reported totals for the selected scope'
          : `${rows.length} ${rows.length === 1 ? 'record' : 'records'}`
      }
    />
  );
  if (spec.view === 'summary')
    return (
      <Card component="section">
        {toolbar}
        <TableContainer
          role="region"
          tabIndex={0}
          aria-label="Report results scroll area"
        >
          <Table
            aria-label="Report results"
            aria-busy={loading}
            sx={{ minWidth: 520 }}
          >
            <TableHead>
              <TableRow>
                {columns.map((c) => (
                  <TableCell
                    component="th"
                    scope="col"
                    key={c.key}
                    align="right"
                  >
                    {c.label}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {!loading &&
                rows.map((row, index) => (
                  <TableRow key={index}>
                    {columns.map((c) => (
                      <TableCell key={c.key} align="right">
                        <Cell column={c} row={row} />
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
            </TableBody>
          </Table>
        </TableContainer>
        {loading ? (
          <LoadingState label="Loading report…" />
        ) : !rows.length ? (
          <Box>{empty}</Box>
        ) : null}
      </Card>
    );
  const bounded = Math.min(
    page,
    Math.max(0, Math.ceil(rows.length / size) - 1),
  );
  return (
    <DataTableShell
      label="Report results"
      toolbar={toolbar}
      columns={columns.map((c) => ({
        key: c.key,
        label: c.label,
        align: c.kind === 'number' || c.kind === 'money' ? 'right' : 'left',
        render: (row: ReportRow) => <Cell column={c} row={row} />,
      }))}
      rows={rows.slice(bounded * size, (bounded + 1) * size)}
      rowKey={(row) => String(rows.indexOf(row))}
      loading={loading}
      loadingLabel="Loading report…"
      page={bounded}
      rowsPerPage={size}
      count={rows.length}
      onPageChange={setPage}
      onRowsPerPageChange={(n) => {
        setSize(n);
        setPage(0);
      }}
      empty={empty}
    />
  );
}
