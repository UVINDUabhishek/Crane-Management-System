import {
  Alert,
  AlertTitle,
  Box,
  Button,
  CircularProgress,
  Stack,
  Typography,
} from '@mui/material';
import InboxOutlined from '@mui/icons-material/InboxOutlined';
import LockOutlined from '@mui/icons-material/LockOutlined';
import { normalizeError } from './api';
import type { ReactNode } from 'react';
export function EmptyState({
  title,
  description,
  action,
  icon = <InboxOutlined />,
}: {
  title: string;
  description: string;
  action?: ReactNode;
  icon?: ReactNode;
}) {
  return (
    <Stack
      alignItems="center"
      spacing={1.5}
      sx={{ px: 3, py: 7, textAlign: 'center' }}
    >
      <Box
        sx={{
          p: 2,
          borderRadius: 3,
          bgcolor: 'action.hover',
          color: 'text.secondary',
          display: 'flex',
        }}
      >
        {icon}
      </Box>
      <Typography variant="h6" component="h2">
        {title}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ maxWidth: 420 }}>
        {description}
      </Typography>
      {action}
    </Stack>
  );
}
export function LoadingState({
  label = 'Loading your workspace…',
}: {
  label?: string;
}) {
  return (
    <Stack
      role="status"
      aria-live="polite"
      alignItems="center"
      spacing={2}
      sx={{ py: 8 }}
    >
      <CircularProgress size={28} />
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
    </Stack>
  );
}
export const Loading = LoadingState;
export function AccessDenied() {
  return (
    <EmptyState
      title="Access denied"
      description="Your role does not permit access to this page."
      icon={<LockOutlined />}
    />
  );
}
export function ErrorState({
  error,
  retry,
}: {
  error: unknown;
  retry?: () => void;
}) {
  const problem = normalizeError(error);
  return problem.status === 403 ? (
    <AccessDenied />
  ) : (
    <Alert
      severity={
        problem.status === 409
          ? 'warning'
          : problem.status === 401
            ? 'info'
            : 'error'
      }
      sx={{ p: 2, alignItems: 'center' }}
      action={
        problem.status === 401 ? (
          <Button href="/login" color="inherit">
            Sign in
          </Button>
        ) : retry ? (
          <Button color="inherit" onClick={retry}>
            Retry
          </Button>
        ) : undefined
      }
    >
      <AlertTitle>
        {problem.status === 401
          ? 'Session ended'
          : problem.status === 409
            ? 'Action needs review'
            : problem.status === 400
              ? 'Check your information'
              : 'Unable to load this content'}
      </AlertTitle>
      {problem.message}
    </Alert>
  );
}
