import { useMemo, useState, type ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  BrowserRouter,
  Link,
  Navigate,
  Outlet,
  Route,
  Routes,
  useLocation,
} from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  Breadcrumbs,
  Button,
  Chip,
  CssBaseline,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Stack,
  ThemeProvider,
  Toolbar,
  Typography,
  createTheme,
} from '@mui/material';
import {
  Menu,
  DarkMode,
  LightMode,
  DashboardOutlined,
  PrecisionManufacturingOutlined,
} from '@mui/icons-material';
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
function Layout({ dark, toggle }: { dark: boolean; toggle: () => void }) {
  const { user, logout } = useAuth();
  const [mobile, setMobile] = useState(false);
  const location = useLocation();
  const nav = (
    <Box sx={{ p: 2 }}>
      <Stack direction="row" spacing={1.5} sx={{ p: 2, mb: 3 }}>
        <PrecisionManufacturingOutlined color="primary" />
        <Typography fontWeight={800}>CHMS</Typography>
      </Stack>
      <Typography variant="overline" color="text.secondary" sx={{ px: 2 }}>
        Workspace
      </Typography>
      <List aria-label="Workspace navigation">
        {modules
          .filter((m) => user && can(user.role, m.key))
          .map((m) => (
            <ListItemButton
              key={m.key}
              component={Link}
              to={m.key === 'dashboard' ? '/' : `/workspace/${m.key}`}
              selected={
                m.key === 'dashboard'
                  ? location.pathname === '/'
                  : location.pathname.endsWith('/' + m.key)
              }
              onClick={() => setMobile(false)}
              sx={{ borderRadius: 2, mb: 0.5 }}
            >
              <ListItemText primary={m.label} />
              {m.key !== 'dashboard' && (
                <Chip size="small" label="Planned" variant="outlined" />
              )}
            </ListItemButton>
          ))}
      </List>
    </Box>
  );
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Box component="nav" sx={{ width: { md: 250 }, flexShrink: 0 }}>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { width: 250 },
          }}
        >
          {nav}
        </Drawer>
        <Drawer
          open={mobile}
          onClose={() => setMobile(false)}
          sx={{ display: { md: 'none' }, '& .MuiDrawer-paper': { width: 280 } }}
        >
          {nav}
        </Drawer>
      </Box>
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <AppBar
          position="sticky"
          color="inherit"
          elevation={0}
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          <Toolbar sx={{ gap: 1 }}>
            <IconButton
              aria-label="Open navigation"
              onClick={() => setMobile(true)}
              sx={{ display: { md: 'none' } }}
            >
              <Menu />
            </IconButton>
            <Typography
              sx={{
                flex: 1,
                fontWeight: 600,
                display: { xs: 'none', sm: 'block' },
              }}
            >
              Crane Hire Management
            </Typography>
            <IconButton aria-label="Toggle color theme" onClick={toggle}>
              {dark ? <LightMode /> : <DarkMode />}
            </IconButton>
            <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}>
              {user?.username.slice(0, 1)}
            </Avatar>
            <Box sx={{ mr: 1 }}>
              <Typography variant="body2">{user?.username}</Typography>
              <Typography variant="caption" color="text.secondary">
                {user?.role}
              </Typography>
            </Box>
            <Button onClick={logout}>Logout</Button>
          </Toolbar>
        </AppBar>
        <Box
          component="main"
          sx={{ p: { xs: 2, md: 4 }, maxWidth: 1600, mx: 'auto' }}
        >
          <Breadcrumbs sx={{ mb: 3 }}>
            <Link to="/" style={{ color: 'inherit' }}>
              Workspace
            </Link>
            <Typography color="text.primary">
              {modules.find((m) => location.pathname.endsWith('/' + m.key))
                ?.label ?? 'Overview'}
            </Typography>
          </Breadcrumbs>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
function Planned({ label }: { label: string }) {
  return (
    <Stack spacing={2} sx={{ py: 5 }}>
      <DashboardOutlined color="primary" sx={{ fontSize: 40 }} />
      <Typography variant="h4" component="h1">
        {label}
      </Typography>
      <Typography color="text.secondary">
        This workspace is planned for a later phase. No records can be managed
        here yet.
      </Typography>
      <Button component={Link} to="/" sx={{ alignSelf: 'start' }}>
        Back to overview
      </Button>
    </Stack>
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
        <Route element={<Layout dark={dark} toggle={toggle} />}>
          <Route index element={<DashboardPage />} />
          {modules
            .filter((m) => m.key !== 'dashboard')
            .map((m) => (
              <Route
                key={m.key}
                path={`workspace/${m.key}`}
                element={
                  <Protected permission={m.key}>
                    <Planned label={m.label} />
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
  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode: dark ? 'dark' : 'light',
          primary: { main: dark ? '#78d8ca' : '#12685f' },
          background: {
            default: dark ? '#101a21' : '#f4f6f8',
            paper: dark ? '#18262f' : '#ffffff',
          },
        },
        shape: { borderRadius: 10 },
        typography: {
          fontFamily: 'Inter, system-ui, sans-serif',
          h4: { fontWeight: 700 },
          h3: { fontWeight: 650 },
        },
        components: {
          MuiButton: { styleOverrides: { root: { textTransform: 'none' } } },
          MuiPaper: { defaultProps: { elevation: 0 } },
        },
      }),
    [dark],
  );
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
