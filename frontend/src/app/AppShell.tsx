import { useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Box,
  Breadcrumbs,
  Button,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import {
  Menu,
  Close,
  DarkModeOutlined,
  LightModeOutlined,
  PrecisionManufacturingOutlined,
  DashboardOutlined,
  ConstructionOutlined,
  PeopleOutline,
  DescriptionOutlined,
  AssignmentOutlined,
  ScheduleOutlined,
  BuildOutlined,
  ReceiptLongOutlined,
  ManageAccountsOutlined,
  ChevronRight,
  LogoutOutlined,
} from '@mui/icons-material';
import { useAuth } from '../features/auth/AuthProvider';
import { can, modules, type Permission } from '../shared/permissions';
const icons = {
  dashboard: DashboardOutlined,
  cranes: ConstructionOutlined,
  customers: PeopleOutline,
  quotations: DescriptionOutlined,
  jobs: AssignmentOutlined,
  timesheets: ScheduleOutlined,
  maintenance: BuildOutlined,
  invoices: ReceiptLongOutlined,
  users: ManageAccountsOutlined,
};
const groups: { title: string; keys: Permission[] }[] = [
  { title: 'Workspace', keys: ['dashboard'] },
  {
    title: 'Operations',
    keys: ['cranes', 'jobs', 'timesheets', 'maintenance'],
  },
  { title: 'Commercial', keys: ['customers', 'quotations', 'invoices'] },
  { title: 'Administration', keys: ['users'] },
];
export function AppShell({
  dark,
  toggle,
}: {
  dark: boolean;
  toggle: () => void;
}) {
  const { user, logout } = useAuth();
  const [mobile, setMobile] = useState(false);
  const theme = useTheme();
  const desktop = useMediaQuery(theme.breakpoints.up('md'));
  const location = useLocation();
  const current = modules.find(
    (m) =>
      location.pathname ===
      (m.key === 'dashboard' ? '/' : `/workspace/${m.key}`),
  );
  const title =
    current?.label ??
    (location.pathname === '/access-denied'
      ? 'Access denied'
      : 'Page not found');
  const nav = (
    <Stack sx={{ height: '100%', color: '#d8e3ec' }}>
      <Stack
        direction="row"
        alignItems="center"
        spacing={1.5}
        sx={{ px: 3, py: 3.5 }}
      >
        <Box
          sx={{
            p: 1,
            bgcolor: '#214b52',
            borderRadius: 2,
            display: 'flex',
            color: '#9de2d6',
          }}
        >
          <PrecisionManufacturingOutlined />
        </Box>
        <Box sx={{ flex: 1 }}>
          <Typography
            sx={{
              fontWeight: 800,
              fontSize: '1.25rem',
              letterSpacing: '.04em',
              color: '#fff',
            }}
          >
            CHMS
          </Typography>
          <Typography variant="caption" sx={{ color: '#a9bac9' }}>
            Crane hire operations
          </Typography>
        </Box>
        {!desktop && (
          <IconButton
            aria-label="Close navigation"
            onClick={() => setMobile(false)}
            sx={{ color: 'inherit' }}
          >
            <Close />
          </IconButton>
        )}
      </Stack>
      <Divider sx={{ borderColor: '#2a4052', mx: 3 }} />
      <Box sx={{ p: 2, overflowY: 'auto', flex: 1 }}>
        {groups.map((group) => {
          const visible = group.keys.filter(
            (key) => user && can(user.role, key),
          );
          return visible.length ? (
            <Box key={group.title} sx={{ mt: 2, mb: 2.5 }}>
              <Typography variant="overline" sx={{ px: 1.5, color: '#9bafbf' }}>
                {group.title}
              </Typography>
              <List
                aria-label={`${group.title} navigation`}
                disablePadding
                sx={{ mt: 0.75 }}
              >
                {visible.map((key) => {
                  const item = modules.find((m) => m.key === key)!;
                  const Icon = icons[key];
                  const selected = current?.key === key;
                  return (
                    <ListItemButton
                      key={key}
                      component={Link}
                      to={key === 'dashboard' ? '/' : `/workspace/${key}`}
                      aria-current={selected ? 'page' : undefined}
                      selected={selected}
                      onClick={() => setMobile(false)}
                      sx={{
                        borderRadius: 2,
                        mb: 0.5,
                        minHeight: 45,
                        px: 1.5,
                        color: selected ? '#fff' : '#c5d3df',
                        '&:hover': { bgcolor: '#223c50' },
                        '&.Mui-selected': {
                          bgcolor: '#234d56',
                          boxShadow: 'inset 3px 0 #75d1bd',
                          '&:hover': { bgcolor: '#2b5961' },
                        },
                      }}
                    >
                      <ListItemIcon
                        sx={{
                          minWidth: 34,
                          color: selected ? '#98dfce' : '#9fb5c5',
                        }}
                      >
                        <Icon fontSize="small" />
                      </ListItemIcon>
                      <ListItemText
                        primary={item.label}
                        slotProps={{
                          primary: {
                            fontSize: '.85rem',
                            fontWeight: selected ? 700 : 500,
                          },
                        }}
                      />
                      {key !== 'dashboard' && (
                        <Box
                          component="span"
                          sx={{
                            fontSize: '.62rem',
                            color: '#bbccd8',
                            border: '1px solid #496273',
                            borderRadius: 1,
                            px: 0.6,
                            py: 0.15,
                          }}
                        >
                          Planned
                        </Box>
                      )}
                    </ListItemButton>
                  );
                })}
              </List>
            </Box>
          ) : null;
        })}
      </Box>
      <Box sx={{ m: 2, p: 2, borderRadius: 2, bgcolor: '#1c3548' }}>
        <Typography variant="caption" fontWeight={700}>
          One workspace. Clear oversight.
        </Typography>
        <Typography
          variant="caption"
          display="block"
          sx={{ color: '#a9bdcb', mt: 0.5 }}
        >
          Fleet · People · Operations
        </Typography>
      </Box>
    </Stack>
  );
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Box
        component="a"
        href="#main-content"
        sx={{
          position: 'fixed',
          left: 16,
          top: -100,
          zIndex: 2000,
          bgcolor: 'background.paper',
          p: 2,
          '&:focus': { top: 8 },
        }}
      >
        Skip to content
      </Box>
      <Box
        component="nav"
        aria-label="Main navigation"
        sx={{ width: { md: 264 }, flexShrink: 0 }}
      >
        {desktop ? (
          <Drawer
            variant="permanent"
            slotProps={{
              paper: { sx: { width: 264, bgcolor: '#132c40', border: 0 } },
            }}
          >
            {nav}
          </Drawer>
        ) : (
          <Drawer
            open={mobile}
            onClose={() => setMobile(false)}
            slotProps={{
              paper: {
                sx: {
                  width: 280,
                  maxWidth: '90vw',
                  bgcolor: '#132c40',
                  border: 0,
                },
              },
            }}
          >
            {nav}
          </Drawer>
        )}
      </Box>
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <AppBar position="sticky" color="inherit">
          <Toolbar
            sx={{
              gap: { xs: 1, sm: 2 },
              px: { xs: 2, md: 4 },
              minHeight: { xs: 68, sm: 76 },
            }}
          >
            {!desktop && (
              <IconButton
                aria-label="Open navigation"
                aria-expanded={mobile}
                onClick={() => setMobile(true)}
              >
                <Menu />
              </IconButton>
            )}
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="body2" fontWeight={700}>
                Operations workspace
              </Typography>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ display: { xs: 'none', sm: 'block' } }}
              >
                Crane Hire Management System
              </Typography>
            </Box>
            <Tooltip
              title={dark ? 'Switch to light theme' : 'Switch to dark theme'}
            >
              <IconButton
                aria-label="Toggle color theme"
                aria-pressed={dark}
                onClick={toggle}
              >
                {dark ? (
                  <LightModeOutlined fontSize="small" />
                ) : (
                  <DarkModeOutlined fontSize="small" />
                )}
              </IconButton>
            </Tooltip>
            <Divider
              orientation="vertical"
              flexItem
              sx={{ my: 2, display: { xs: 'none', sm: 'block' } }}
            />
            <Avatar
              sx={{
                width: 34,
                height: 34,
                bgcolor: dark ? '#254f50' : '#e4f1ec',
                color: dark ? '#a5e5d3' : '#286455',
                fontSize: '.85rem',
                fontWeight: 700,
              }}
            >
              {user?.username.slice(0, 1).toUpperCase()}
            </Avatar>
            <Box sx={{ maxWidth: 160, display: { xs: 'none', sm: 'block' } }}>
              <Typography variant="body2" fontWeight={650} noWrap>
                {user?.username}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {user?.role}
              </Typography>
            </Box>
            <Button
              onClick={logout}
              size="small"
              color="inherit"
              startIcon={<LogoutOutlined fontSize="small" />}
            >
              Logout
            </Button>
          </Toolbar>
        </AppBar>
        <Box
          component="main"
          id="main-content"
          tabIndex={-1}
          sx={{ p: { xs: 2, md: 4 }, maxWidth: 1640, mx: 'auto', outline: 0 }}
        >
          <Breadcrumbs
            aria-label="Breadcrumb"
            separator={<ChevronRight sx={{ fontSize: 14 }} />}
            sx={{ mb: 3, fontSize: '.75rem' }}
          >
            <Link to="/" style={{ color: 'inherit', textDecoration: 'none' }}>
              Workspace
            </Link>
            <Typography
              variant="caption"
              color="text.primary"
              aria-current="page"
            >
              {title}
            </Typography>
          </Breadcrumbs>
          <Outlet />
        </Box>
      </Box>
    </Box>
  );
}
