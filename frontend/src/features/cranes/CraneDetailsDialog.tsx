import { StatusChip } from '../../shared/ui/DataTableShell';
import { useId } from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  Typography,
  useMediaQuery,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { craneKeys, cranesApi } from './cranesApi';
import { craneFields } from './craneSchema';
import { CraneFormDialog } from './CraneFormDialog';
import { EmptyState, ErrorState, LoadingState } from '../../shared/States';
import { normalizeError } from '../../shared/api';
export function CraneDetailsDialog({
  id,
  edit = false,
  onClose,
  onSaved,
}: {
  id: number;
  edit?: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const titleId = useId();
  const reduced = useMediaQuery('(prefers-reduced-motion: reduce)');
  const query = useQuery({
    queryKey: craneKeys.detail(id),
    queryFn: ({ signal }) => cranesApi.detail(id, signal),
    staleTime: 0,
    gcTime: 0,
  });
  if (edit && query.isSuccess)
    return (
      <CraneFormDialog crane={query.data} onClose={onClose} onSaved={onSaved} />
    );
  const labels = {
    craneNumber: 'Crane number',
    ...craneFields,
    status: 'Operational status',
    active: 'Activation',
  };
  const groups = [
    {
      title: 'Crane specifications',
      fields: Object.keys(labels) as (keyof typeof labels)[],
    },
  ];
  return (
    <Dialog
      open
      fullWidth
      maxWidth="sm"
      onClose={onClose}
      transitionDuration={reduced ? 0 : undefined}
      aria-labelledby={titleId}
    >
      <DialogTitle id={titleId}>
        {edit ? 'Edit crane' : 'Crane details'}
      </DialogTitle>
      <DialogContent>
        {query.isPending ? (
          <LoadingState label="Loading crane…" />
        ) : query.isError ? (
          normalizeError(query.error).status === 404 ? (
            <EmptyState
              title="Crane not found"
              description="This record is no longer available. Close this view and refresh the directory."
            />
          ) : (
            <ErrorState
              error={query.error}
              retry={() => void query.refetch()}
            />
          )
        ) : (
          groups.map((group) => (
            <Box key={group.title} component="section" sx={{ mb: 3 }}>
              <Typography component="h2" variant="h6">
                {group.title}
              </Typography>
              <Box component="dl" sx={{ m: 0 }}>
                {group.fields.map((key) => (
                  <Box key={key} sx={{ mt: 2, overflowWrap: 'anywhere' }}>
                    <Typography
                      component="dt"
                      variant="caption"
                      color="text.secondary"
                    >
                      {labels[key]}
                    </Typography>
                    <Typography
                      component="dd"
                      sx={{ m: 0, whiteSpace: 'pre-wrap' }}
                    >
                      {key === 'status' ? (
                        <StatusChip status={query.data.status} />
                      ) : key === 'active' ? (
                        query.data.active ? (
                          'Active'
                        ) : (
                          'Inactive'
                        )
                      ) : (
                        (query.data[key] ?? 'Not provided')
                      )}
                    </Typography>
                  </Box>
                ))}
              </Box>
            </Box>
          ))
        )}
      </DialogContent>
      <DialogActions>
        <Button autoFocus onClick={onClose}>
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
}
