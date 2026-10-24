import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Typography,
} from '@mui/material';
import { normalizeError } from './api';
export function Loading({
  label = 'Loading your workspace…',
}: {
  label?: string;
}) {
  return (
    <Box role="status" sx={{ p: 6, textAlign: 'center' }}>
      <CircularProgress size={28} />
      <Typography sx={{ mt: 2 }}>{label}</Typography>
    </Box>
  );
}
export function AccessDenied() {
  return (
    <Alert severity="warning">
      <Typography variant="h6">Access denied</Typography>Your role does not
      permit access to this page.
    </Alert>
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
      severity="error"
      action={
        retry ? (
          <Button color="inherit" onClick={retry}>
            Retry
          </Button>
        ) : undefined
      }
    >
      {problem.message}
    </Alert>
  );
}
