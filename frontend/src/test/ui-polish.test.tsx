import { beforeEach, expect, it } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import App from '../app/App';
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
    http.get('/api/customers', () => HttpResponse.json([])),
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
  expect(await screen.findByText('12')).toBeVisible();
  expect(
    getComputedStyle(screen.getByRole('link', { name: 'Overview' })).boxShadow,
  ).not.toContain('inset');
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
