import {
  Alert,
  Box,
  Button,
  Container,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Navigate } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from './AuthProvider';
import { normalizeError } from '../../shared/api';
const schema = z.object({
  email: z.email('Enter a valid email address.'),
  password: z.string().min(1, 'Enter your password.'),
});
export function LoginPage() {
  const auth = useAuth();
  const [error, setError] = useState('');
  const {
    register,
    handleSubmit,
    setError: fieldError,
    formState: { errors },
  } = useForm<z.infer<typeof schema>>({ resolver: zodResolver(schema) });
  if (auth.user) return <Navigate to="/" replace />;
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'grid',
        alignItems: 'center',
        bgcolor: 'navigation.background',
      }}
    >
      <Container maxWidth="sm">
        <Typography
          sx={{
            color: 'navigation.accent',
            mb: 2,
            fontWeight: 700,
            letterSpacing: 4,
          }}
        >
          CHMS / OPERATIONS
        </Typography>
        <Paper sx={{ p: { xs: 3, sm: 5 } }}>
          <Typography variant="h4" component="h1">
            Welcome back
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 1, mb: 4 }}>
            Sign in to your crane hire workspace.
          </Typography>
          <Stack
            component="form"
            noValidate
            spacing={3}
            onSubmit={handleSubmit(async (values) => {
              setError('');
              try {
                await auth.login(values.email, values.password);
              } catch (e) {
                const problem = normalizeError(e);
                let mapped = false;
                for (const field of problem.fields) {
                  if (field.field === 'email' || field.field === 'password') {
                    fieldError(field.field, { message: field.message });
                    mapped = true;
                  }
                }
                if (!mapped)
                  setError(
                    problem.status === 401
                      ? 'Sign-in failed. Check your credentials and try again.'
                      : problem.message,
                  );
              }
            })}
          >
            {error ? (
              <Alert severity="error">{error}</Alert>
            ) : auth.expired ? (
              <Alert severity="info">
                Your session has ended. Please sign in.
              </Alert>
            ) : null}
            <TextField
              label="Email address"
              autoComplete="username"
              type="email"
              {...register('email')}
              error={!!errors.email}
              helperText={errors.email?.message}
            />
            <TextField
              label="Password"
              type="password"
              autoComplete="current-password"
              {...register('password')}
              error={!!errors.password}
              helperText={errors.password?.message}
            />
            <Button
              variant="contained"
              size="large"
              type="submit"
              disabled={auth.busy}
            >
              {auth.busy ? 'Signing in…' : 'Sign in'}
            </Button>
          </Stack>
          <Typography
            variant="caption"
            color="text.secondary"
            display="block"
            sx={{ mt: 3 }}
          >
            Internal access only · Sign in again after refreshing this page.
          </Typography>
        </Paper>
      </Container>
    </Box>
  );
}
