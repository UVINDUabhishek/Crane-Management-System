import { expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RowActions } from '../shared/ui/RowActions';
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
