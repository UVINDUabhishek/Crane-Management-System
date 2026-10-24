import { Box, Card, Divider, Stack, Typography } from '@mui/material';
import type { ReactNode } from 'react';
export function PageHeader({
  title,
  description,
  eyebrow,
  actions,
}: {
  title: string;
  description: string;
  eyebrow?: string;
  actions?: ReactNode;
}) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={2}
      sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
    >
      <Box sx={{ minWidth: 0 }}>
        {eyebrow && (
          <Typography variant="overline" color="primary.main">
            {eyebrow}
          </Typography>
        )}
        <Typography variant="h4" component="h1" sx={{ mt: 0.5 }}>
          {title}
        </Typography>
        <Typography
          color="text.secondary"
          variant="body2"
          sx={{ mt: 1, maxWidth: 680 }}
        >
          {description}
        </Typography>
      </Box>
      {actions && <Box sx={{ flexShrink: 0 }}>{actions}</Box>}
    </Stack>
  );
}
export function SectionCard({
  title,
  description,
  actions,
  children,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
}) {
  return (
    <Card component="section">
      <Stack
        direction="row"
        sx={{
          p: 3,
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 2,
        }}
      >
        <Box>
          <Typography variant="h6" component="h2">
            {title}
          </Typography>
          {description && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {description}
            </Typography>
          )}
        </Box>
        {actions}
      </Stack>
      <Divider />
      {children}
    </Card>
  );
}
export function StatCard({
  label,
  value,
  description,
  icon,
  accent = false,
}: {
  label: string;
  value: number | string;
  description: string;
  icon: ReactNode;
  accent?: boolean;
}) {
  return (
    <Card
      sx={{
        p: { xs: 2.5, lg: 3 },
        height: '100%',
        borderColor: accent ? 'primary.main' : 'divider',
      }}
    >
      <Stack
        direction="row"
        sx={{ justifyContent: 'space-between', alignItems: 'center', gap: 1 }}
      >
        <Typography variant="body2" fontWeight={650} color="text.secondary">
          {label}
        </Typography>
        <Box
          sx={{
            display: 'grid',
            placeItems: 'center',
            p: 1,
            borderRadius: 2,
            bgcolor: (theme) =>
              theme.palette.mode === 'dark' ? '#223c42' : '#edf6f3',
            color: 'primary.main',
          }}
        >
          {icon}
        </Box>
      </Stack>
      <Typography
        sx={{
          fontSize: '2.4rem',
          fontWeight: 750,
          letterSpacing: '-.055em',
          fontVariantNumeric: 'tabular-nums',
          lineHeight: 1.3,
          my: 1,
        }}
      >
        {value}
      </Typography>
      <Typography variant="caption" color="text.secondary">
        {description}
      </Typography>
    </Card>
  );
}
