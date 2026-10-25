import { useMemo, useState, type ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  BrowserRouter,
  Link,
  Navigate,
  Outlet,
  Route,
  Routes,
} from 'react-router-dom';
import {
  Button,
  CssBaseline,
  Stack,
  ThemeProvider,
  Typography,
} from '@mui/material';
import { AppShell } from './AppShell';
import { createAppTheme } from './theme';
import { PageHeader, SectionCard } from '../shared/ui/Layout';
import { EmptyState } from '../shared/States';
import { ManagementTemplate } from '../features/management/ManagementTemplate';
import { AuthProvider, useAuth } from '../features/auth/AuthProvider';
import { LoginPage } from '../features/auth/LoginPage';
import { DashboardPage } from '../features/dashboard/DashboardPage';
import { can, modules, type Permission } from '../shared/permissions';
import { AccessDenied, Loading } from '../shared/States';
export function Protected({
  permission,
  children,
}: {
  permission?: Permission;
  children?: ReactNode;
}) {
  const { user, busy } = useAuth();
  if (busy) return <Loading />;
  if (!user) return <Navigate to="/login" replace />;
  if (permission && !can(user.role, permission)) return <AccessDenied />;
  return children ?? <Outlet />;
}
function Planned({ label }: { label: string }) {
  return (
    <>
      <PageHeader
        title={label}
        description="Your next operations workspace."
        eyebrow="PLANNED MODULE"
      />
      <SectionCard title="Workspace coming soon">
        <EmptyState
          title="A new workspace is on the way"
          description="This module is planned for a later phase. Records cannot be managed here yet."
          action={
            <Button component={Link} to="/">
              Back to overview
            </Button>
          }
        />
      </SectionCard>
    </>
  );
}
export function AppRoutes({
  dark = false,
  toggle = () => {},
}: {
  dark?: boolean;
  toggle?: () => void;
}) {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<Protected />}>
        <Route element={<AppShell dark={dark} toggle={toggle} />}>
          <Route index element={<DashboardPage />} />
          {modules
            .filter((m) => m.key !== 'dashboard')
            .map((m) => (
              <Route
                key={m.key}
                path={`workspace/${m.key}`}
                element={
                  <Protected permission={m.key}>
                    {m.key === 'customers' ? (
                      <ManagementTemplate />
                    ) : (
                      <Planned label={m.label} />
                    )}
                  </Protected>
                }
              />
            ))}
          <Route path="access-denied" element={<AccessDenied />} />
          <Route
            path="*"
            element={
              <Stack spacing={2}>
                <Typography variant="h4">Page not found</Typography>
                <Link to="/">Back to overview</Link>
              </Stack>
            }
          />
        </Route>
      </Route>
    </Routes>
  );
}
export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, staleTime: 30000, refetchOnWindowFocus: false },
    },
  });
}
const client = createQueryClient();
export default function App() {
  const [dark, setDark] = useState(false);
  const theme = useMemo(() => createAppTheme(dark), [dark]);
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={client}>
        <AuthProvider>
          <BrowserRouter>
            <AppRoutes dark={dark} toggle={() => setDark((v) => !v)} />
          </BrowserRouter>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}
