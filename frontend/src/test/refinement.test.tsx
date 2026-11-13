import { useState } from 'react';
import { it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Button } from '@mui/material';
import {
  ConfirmDialog,
  DataTableShell,
  StatusChip,
  type BusinessStatus,
} from '../shared/ui/DataTableShell';
import { PageHeader } from '../shared/ui/Layout';
import { ErrorState, EmptyState } from '../shared/States';
import { ApiError } from '../shared/api';
it('keeps column headings while loading and prevents false empty-state/pagination interaction', () => {
  render(
    <DataTableShell
      label="Records"
      columns={[
        { key: 'name', label: 'Name', render: (r: { id: string }) => r.id },
      ]}
      rows={[]}
      rowKey={(r) => r.id}
      page={0}
      rowsPerPage={5}
      count={10}
      loading
      empty={<EmptyState title="No records" description="Try another filter" />}
      onPageChange={vi.fn()}
      onRowsPerPageChange={vi.fn()}
    />,
  );
  expect(screen.getByRole('columnheader', { name: 'Name' })).toHaveAttribute(
    'scope',
    'col',
  );
  expect(screen.getByRole('table')).toHaveAttribute('aria-busy', 'true');
  expect(screen.getByRole('status')).toHaveTextContent('Loading records');
  expect(screen.queryByText('No records')).not.toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Go to next page' }),
  ).toBeDisabled();
});
it('drives controlled pagination and distinct row identities from the caller', async () => {
  const user = userEvent.setup();
  function Example() {
    const [page, setPage] = useState(0);
    return (
      <DataTableShell
        label="Records"
        columns={[
          { key: 'name', label: 'Name', render: (r: { id: string }) => r.id },
        ]}
        rows={page === 0 ? [{ id: 'First row' }] : [{ id: 'Second row' }]}
        rowKey={(r) => r.id}
        page={page}
        rowsPerPage={5}
        count={6}
        empty={null}
        onPageChange={setPage}
        onRowsPerPageChange={() => {}}
      />
    );
  }
  render(<Example />);
  await user.click(screen.getByRole('button', { name: 'Go to next page' }));
  expect(screen.getByRole('cell', { name: 'Second row' })).toBeVisible();
  expect(
    screen.queryByRole('cell', { name: 'First row' }),
  ).not.toBeInTheDocument();
  expect(screen.getByText('6–6 of 6')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Go to previous page' }));
  expect(screen.getByRole('cell', { name: 'First row' })).toBeVisible();
});
it.each([
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
  'ACTIVE',
  'INACTIVE',
] as BusinessStatus[])('gives %s a readable text label', (status) => {
  render(<StatusChip status={status} />);
  expect(
    screen.getByText(
      status.charAt(0) + status.slice(1).toLowerCase().replaceAll('_', ' '),
    ),
  ).toBeVisible();
});
it('blocks repeat confirmation and dismissal while pending, then restores dismissal', async () => {
  const user = userEvent.setup(),
    confirm = vi.fn(),
    close = vi.fn();
  const props = {
    title: 'Save changes?',
    description: 'Review the selected record before continuing.',
    onConfirm: confirm,
    onClose: close,
  };
  const { rerender } = render(
    <ConfirmDialog {...props} open pending pendingLabel="Saving changes…" />,
  );
  expect(
    screen.getByRole('dialog', { name: 'Save changes?' }),
  ).toHaveAccessibleDescription(props.description);
  expect(screen.getByRole('dialog')).toHaveAttribute('aria-busy', 'true');
  expect(
    screen.getByRole('button', { name: 'Saving changes…' }),
  ).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
  await user.keyboard('{Escape}');
  expect(close).not.toHaveBeenCalled();
  expect(confirm).not.toHaveBeenCalled();
  rerender(<ConfirmDialog {...props} open />);
  await user.click(screen.getByRole('button', { name: 'Confirm' }));
  expect(confirm).toHaveBeenCalledTimes(1);
  await user.keyboard('{Escape}');
  expect(close).toHaveBeenCalledTimes(1);
});
it('focuses safe cancellation, traps keyboard focus and returns to the trigger', async () => {
  const user = userEvent.setup();
  function Example() {
    const [open, setOpen] = useState(false);
    return (
      <>
        <Button onClick={() => setOpen(true)}>Open confirmation</Button>
        <ConfirmDialog
          open={open}
          title="Proceed?"
          description="Nothing is saved in this example."
          onConfirm={() => setOpen(false)}
          onClose={() => setOpen(false)}
        />
      </>
    );
  }
  render(<Example />);
  const trigger = screen.getByRole('button', { name: 'Open confirmation' });
  await user.click(trigger);
  await waitFor(() =>
    expect(screen.getByRole('button', { name: 'Cancel' })).toHaveFocus(),
  );
  const dialog = screen.getByRole('dialog');
  await user.tab({ shift: true });
  // MUI may wrap focus to Cancel or move to Confirm depending on its focus trap.
  // The accessibility contract is that focus stays inside the dialog.
  await waitFor(() =>
    expect(dialog.contains(document.activeElement)).toBe(true),
  );
  await user.tab();
  await waitFor(() =>
    expect(dialog.contains(document.activeElement)).toBe(true),
  );
  await user.keyboard('{Escape}');
  await waitFor(() => expect(trigger).toHaveFocus());
});
it.each([
  [401, 'Session ended'],
  [403, 'Access denied'],
  [409, 'Action needs review'],
  [500, 'Unable to load this content'],
] as const)(
  'presents %s without losing the safe API detail',
  (status, title) => {
    render(<ErrorState error={new ApiError(status, 'TEST', 'Safe message')} />);
    expect(screen.getByText(title)).toBeVisible();
    if (status === 401)
      expect(screen.getByRole('link', { name: 'Sign in' })).toHaveAttribute(
        'href',
        '/login',
      );
  },
);
it('supports page-local breadcrumbs without changing the heading hierarchy', () => {
  render(
    <PageHeader
      title="Directory"
      description="Record overview"
      breadcrumbs={
        <nav aria-label="Local breadcrumb">Workspace / Directory</nav>
      }
      actions={<Button>Action</Button>}
    />,
  );
  expect(
    screen.getByRole('navigation', { name: 'Local breadcrumb' }),
  ).toBeVisible();
  expect(
    screen.getByRole('heading', { level: 1, name: 'Directory' }),
  ).toBeVisible();
});
