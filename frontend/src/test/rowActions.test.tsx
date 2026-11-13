import { expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RowActions } from '../shared/ui/RowActions';
import { ConfirmDialog, DataTableShell } from '../shared/ui/DataTableShell';
it('exposes named 44px primary controls and keyboard menu actions without automatic mutation', async () => {
  const view = vi.fn(),
    edit = vi.fn(),
    complete = vi.fn();
  render(
    <RowActions
      reference="JC-42"
      view={view}
      edit={edit}
      actions={[{ label: 'Complete', run: complete }]}
    />,
  );
  const user = userEvent.setup();
  await user.tab();
  const eye = screen.getByRole('button', { name: 'View details for JC-42' });
  expect(eye).toHaveFocus();
  expect(eye).toHaveStyle({ width: '44px', height: '44px' });
  await user.keyboard('{Enter}');
  expect(view).toHaveBeenCalledExactlyOnceWith(eye);
  await user.tab();
  await user.keyboard(' ');
  expect(edit).toHaveBeenCalledTimes(1);
  await user.tab();
  const more = screen.getByRole('button', { name: 'Actions for JC-42' });
  expect(more).toHaveFocus();
  await user.keyboard('{Enter}');
  expect(complete).not.toHaveBeenCalled();
  expect(
    screen.queryByRole('menuitem', { name: 'Edit' }),
  ).not.toBeInTheDocument();
  await user.keyboard('{Escape}');
  await waitFor(() => expect(more).toHaveFocus());
  await user.keyboard('{Enter}');
  await user.click(screen.getByRole('menuitem', { name: 'Complete' }));
  await waitFor(() => expect(complete).toHaveBeenCalledExactlyOnceWith(more));
  expect(more).toHaveFocus();
});
it('omits unavailable actions and disables interaction while pending', async () => {
  const run = vi.fn();
  const { rerender } = render(<RowActions reference="JC-1" view={run} />);
  expect(screen.getAllByRole('button')).toHaveLength(1);
  rerender(
    <RowActions
      reference="JC-1"
      view={run}
      edit={run}
      actions={[{ label: 'Cancel', run }]}
      disabled
    />,
  );
  const user = userEvent.setup();
  for (const button of screen.getAllByRole('button')) {
    expect(button).toBeDisabled();
    await user.tab();
    expect(button).not.toHaveFocus();
  }
  expect(run).not.toHaveBeenCalled();
});

it('reserves the same noninteractive action slots for different row permissions', () => {
  const run = vi.fn();
  const { rerender } = render(<RowActions reference="JC-1" view={run} />);
  const view = screen.getByRole('button', { name: 'View details for JC-1' });
  const group = view.parentElement!.parentElement!;
  expect(group).toHaveStyle({
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 44px)',
  });
  expect(group.children[1]).toHaveAttribute('aria-hidden', 'true');
  expect(screen.getAllByRole('button')).toHaveLength(1);
  rerender(
    <RowActions
      reference="JC-1"
      view={run}
      actions={[{ label: 'Delete', run }]}
    />,
  );
  expect(group.children[0]).toContainElement(view);
  expect(group.children[2]).toContainElement(
    screen.getByRole('button', { name: 'Actions for JC-1' }),
  );
});
it('keeps revalidating actions focusable while blocking mouse and keyboard activation', async () => {
  const run = vi.fn();
  render(
    <RowActions
      reference="TS-1"
      view={run}
      edit={run}
      changesDisabled
      actions={[{ label: 'Delete', run }]}
    />,
  );
  const user = userEvent.setup();
  const edit = screen.getByRole('button', { name: 'Edit TS-1' });
  await user.click(edit);
  await user.keyboard('{Enter}');
  expect(edit).toHaveFocus();
  await user.click(screen.getByRole('button', { name: 'Actions for TS-1' }));
  expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  expect(run).not.toHaveBeenCalled();
});

it('cancels a queued menu action when eligibility is withdrawn before menu exit', async () => {
  const run = vi.fn();
  const { rerender } = render(
    <RowActions
      reference="TS-1"
      view={run}
      actions={[{ label: 'Delete', run }]}
    />,
  );
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: 'Actions for TS-1' }));
  fireEvent.click(screen.getByRole('menuitem', { name: 'Delete' }));
  rerender(
    <RowActions
      reference="TS-1"
      view={run}
      changesDisabled
      actions={[{ label: 'Delete', run }]}
    />,
  );
  await waitFor(() =>
    expect(screen.queryByRole('menu')).not.toBeInTheDocument(),
  );
  expect(run).not.toHaveBeenCalled();
});

it('restores confirmation focus to the fallback when its original trigger is detached', async () => {
  const original = { current: document.createElement('button') };
  const fallback = { current: null as HTMLButtonElement | null };
  const close = vi.fn();
  const content = (open: boolean) => (
    <>
      <button ref={fallback}>Refresh directory</button>
      <ConfirmDialog
        open={open}
        title="Delete record?"
        description="Confirm deletion"
        returnFocusRef={original}
        fallbackFocusRef={fallback}
        onClose={close}
        onConfirm={vi.fn()}
      />
    </>
  );
  const { rerender } = render(content(true));
  await userEvent.setup().click(screen.getByRole('button', { name: 'Cancel' }));
  expect(close).toHaveBeenCalledTimes(1);
  rerender(content(false));
  await waitFor(() => expect(fallback.current).toHaveFocus());
});

it('aligns the compact Actions heading with its controls while preserving numeric alignment', () => {
  render(
    <DataTableShell
      label="Action alignment"
      columns={[
        { key: 'amount', label: 'Amount', align: 'right', render: () => 10 },
        {
          key: 'actions',
          label: 'Actions',
          align: 'right',
          render: () => <RowActions reference="QA-1" view={vi.fn()} />,
        },
      ]}
      rows={[{ id: 1 }]}
      rowKey={(r) => String(r.id)}
      page={0}
      count={1}
      rowsPerPage={5}
      onPageChange={vi.fn()}
      onRowsPerPageChange={vi.fn()}
      empty={null}
    />,
  );
  expect(screen.getByRole('columnheader', { name: 'Actions' })).toHaveStyle({
    textAlign: 'left',
  });
  expect(screen.getByRole('columnheader', { name: 'Actions' })).toHaveStyle({
    width: '172px',
    minWidth: '172px',
  });
  expect(screen.getByRole('columnheader', { name: 'Amount' })).toHaveStyle({
    textAlign: 'right',
  });
});
