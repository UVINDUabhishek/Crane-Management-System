import {
  Box,
  Button,
  Divider,
  InputAdornment,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import SearchOutlined from '@mui/icons-material/SearchOutlined';
import TuneOutlined from '@mui/icons-material/TuneOutlined';
import type { ReactNode } from 'react';
export function SearchField({
  value,
  onChange,
  label = 'Search records',
}: {
  value: string;
  onChange: (value: string) => void;
  label?: string;
}) {
  return (
    <TextField
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={label}
      slotProps={{
        htmlInput: { 'aria-label': label },
        input: {
          startAdornment: (
            <InputAdornment position="start">
              <SearchOutlined fontSize="small" />
            </InputAdornment>
          ),
        },
      }}
      sx={{ width: { xs: '100%', sm: 320 }, maxWidth: '100%', flexShrink: 0 }}
    />
  );
}
export function FilterToolbar({
  children,
  onMore,
  onClear,
  activeCount = 0,
}: {
  children: ReactNode;
  onMore?: () => void;
  onClear?: () => void;
  activeCount?: number;
}) {
  return (
    <Stack
      direction="row"
      spacing={1.5}
      sx={{
        p: 2.5,
        alignItems: 'flex-start',
        bgcolor: 'surface.muted',
      }}
    >
      <Box
        sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', flex: 1, minWidth: 0 }}
      >
        {children}
      </Box>
      <Button
        size="small"
        onClick={onClear}
        disabled={!onClear}
        sx={{ flexShrink: 0 }}
      >
        Clear filters
      </Button>
      {onMore && (
        <Button
          variant="outlined"
          startIcon={<TuneOutlined />}
          onClick={onMore}
          aria-label={
            activeCount ? `More filters, ${activeCount} active` : 'More filters'
          }
        >
          More filters{activeCount > 0 ? ` (${activeCount})` : ''}
        </Button>
      )}
    </Stack>
  );
}
export function TableToolbar({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children?: ReactNode;
}) {
  return (
    <>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{
          p: 2.5,
          justifyContent: 'space-between',
          alignItems: { sm: 'center' },
        }}
      >
        <Box>
          <Typography variant="h6" component="h2">
            {title}
          </Typography>
          {description && (
            <Typography variant="caption" color="text.secondary">
              {description}
            </Typography>
          )}
        </Box>
        {children}
      </Stack>
      <Divider />
    </>
  );
}
