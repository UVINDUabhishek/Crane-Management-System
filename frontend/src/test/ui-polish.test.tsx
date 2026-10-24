import { beforeEach, expect, it } from 'vitest';
import { render, screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import App from '../app/App';
import { ManagementTemplate } from '../features/management/ManagementTemplate';
import { server, setViewport } from './setup';
beforeEach(() => {
  window.history.replaceState({}, '', '/');
  server.use(
    http.post('/api/auth/login', () =>
      HttpResponse.text(
        `header.${btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }))}.signature`,
      ),
    ),
    http.get('/api/auth/me', () =>
      HttpResponse.json({
        id: 1,
        username: 'Alex',
        email: 'alex@example.com',
        role: 'ADMIN',
      }),
    ),
    http.get('/api/dashboard', () => HttpResponse.json({ totalCranes: 12 })),
  );
});
async function login() {
  const user = userEvent.setup();
  render(<App />);
  await user.type(screen.getByLabelText('Email address'), 'alex@example.com');
  await user.type(screen.getByLabelText('Password'), 'password');
  await user.click(screen.getByRole('button', { name: 'Sign in' }));
  await screen.findByRole('heading', { name: 'Operations at a glance' });
  return user;
}
it('renders grouped sidebar, current page, breadcrumbs and identity', async () => {
  await login();
  expect(
    screen.getByRole('navigation', { name: 'Main navigation' }),
  ).toBeVisible();
  expect(
    screen.getByRole('list', { name: 'Operations navigation' }),
  ).toBeVisible();
  expect(
    screen.getByRole('list', { name: 'Commercial navigation' }),
  ).toBeVisible();
  expect(screen.getByRole('link', { name: 'Overview' })).toHaveAttribute(
    'aria-current',
    'page',
  );
  expect(
    screen.getByRole('navigation', { name: 'Breadcrumb' }),
  ).toHaveTextContent('Overview');
  expect(screen.getByText('Alex')).toBeVisible();
  expect(screen.getByText('12')).toBeVisible();
});
it('toggles actual theme and keeps the session', async () => {
  const user = await login();
  const button = screen.getByRole('button', { name: 'Toggle color theme' });
  expect(button).toHaveAttribute('aria-pressed', 'false');
  const light = getComputedStyle(document.body).backgroundColor;
  await user.click(button);
  expect(button).toHaveAttribute('aria-pressed', 'true');
  await waitFor(() =>
    expect(getComputedStyle(document.body).backgroundColor).not.toBe(light),
  );
  expect(screen.getByText('Alex')).toBeVisible();
  await user.click(button);
  expect(button).toHaveAttribute('aria-pressed', 'false');
});
it('opens and closes mobile navigation and closes it on route selection', async () => {
  setViewport(390);
  const user = await login();
  const open = screen.getByRole('button', { name: 'Open navigation' });
  expect(open).toHaveAttribute('aria-expanded', 'false');
  await user.click(open);
  await user.click(screen.getByRole('button', { name: 'Close navigation' }));
  await waitFor(() =>
    expect(
      screen.queryByRole('button', { name: 'Close navigation' }),
    ).not.toBeInTheDocument(),
  );
  await user.click(open);
  await user.click(screen.getByRole('link', { name: /Customers/ }));
  expect(
    await screen.findByRole('heading', { name: 'Customers' }),
  ).toBeVisible();
  await waitFor(() =>
    expect(
      screen.queryByRole('button', { name: 'Close navigation' }),
    ).not.toBeInTheDocument(),
  );
  expect(open).toHaveAttribute('aria-expanded', 'false');
});
it('starts with honest empty state and disabled mutation action', () => {
  render(<ManagementTemplate />);
  expect(screen.getByRole('button', { name: 'Add customer' })).toBeDisabled();
  expect(screen.getByText('Your customer workspace starts here')).toBeVisible();
  expect(screen.queryByText('Example company A')).not.toBeInTheDocument();
});
it('searches sample rows and resets filter chips', async () => {
  const user = userEvent.setup();
  render(<ManagementTemplate />);
  await user.click(screen.getByRole('button', { name: 'Preview sample rows' }));
  expect(screen.getByText('Example company B')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'All statuses' }));
  expect(screen.queryByText('Example company B')).not.toBeInTheDocument();
  await user.type(
    screen.getByRole('textbox', { name: 'Search company or reference' }),
    'missing',
  );
  expect(screen.getByText('No matching examples')).toBeVisible();
  await user.click(screen.getAllByRole('button', { name: 'Clear filters' })[0]);
  expect(screen.getByText('Example company B')).toBeVisible();
});
it('opens filters and applies status without making business requests', async () => {
  const user = userEvent.setup();
  render(<ManagementTemplate />);
  await user.click(
    screen.getByRole('checkbox', { name: 'Show sample layout' }),
  );
  await user.click(screen.getByRole('button', { name: 'More filters' }));
  const panel = screen.getByRole('dialog', { name: 'Customer filters' });
  await user.click(
    within(panel).getByRole('checkbox', { name: 'Active examples only' }),
  );
  await user.click(within(panel).getByRole('button', { name: 'Show results' }));
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(screen.queryByText('Example company B')).not.toBeInTheDocument();
  expect(screen.getByText('Status: Active')).toBeVisible();
});
it('row menu offers no mutations and opens a dismissible explanation', async () => {
  const user = userEvent.setup();
  render(<ManagementTemplate />);
  await user.click(screen.getByRole('button', { name: 'Preview sample rows' }));
  await user.click(
    screen.getByRole('button', { name: 'Actions for Example company A' }),
  );
  expect(
    screen.getByRole('menuitem', { name: /Edit customer/ }),
  ).toHaveAttribute('aria-disabled', 'true');
  await user.click(
    screen.getByRole('menuitem', { name: 'About this example' }),
  );
  expect(screen.getByRole('dialog', { name: 'Sample record' })).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Got it' }));
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(screen.getByText('Example company A')).toBeVisible();
});
it('exposes rows-per-page selection and bounded pagination', async () => {
  const user = userEvent.setup();
  render(<ManagementTemplate />);
  await user.click(screen.getByRole('button', { name: 'Preview sample rows' }));
  expect(screen.getByText('1–3 of 3')).toBeVisible();
  expect(
    screen.getByRole('button', { name: 'Go to next page' }),
  ).toBeDisabled();
  await user.click(screen.getByRole('combobox', { name: 'Rows per page:' }));
  await user.click(screen.getByRole('option', { name: '10' }));
  expect(
    screen.getByRole('combobox', { name: 'Rows per page:' }),
  ).toHaveTextContent('10');
});
