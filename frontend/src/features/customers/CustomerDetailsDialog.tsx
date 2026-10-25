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
import { customerKeys, customersApi } from './customersApi';
import { customerFields, type CustomerValues } from './customerSchema';
import { CustomerFormDialog } from './CustomerFormDialog';
import { EmptyState, ErrorState, LoadingState } from '../../shared/States';
import { normalizeError } from '../../shared/api';
export function CustomerDetailsDialog({
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
    queryKey: customerKeys.detail(id),
    queryFn: ({ signal }) => customersApi.detail(id, signal),
    staleTime: 0,
    gcTime: 0,
  });
  if (edit && query.isSuccess)
    return (
      <CustomerFormDialog
        customer={query.data}
        onClose={onClose}
        onSaved={onSaved}
      />
    );
  const groups: { title: string; fields: (keyof CustomerValues)[] }[] = [
    {
      title: 'Identification',
      fields: ['companyName', 'registrationNo', 'vatNo'],
    },
    { title: 'Addresses', fields: ['companyAddress', 'billAddress'] },
    {
      title: 'Contact',
      fields: ['contactPerson', 'contactNo', 'customerEmail'],
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
        {edit ? 'Edit customer' : 'Customer details'}
      </DialogTitle>
      <DialogContent>
        {query.isPending ? (
          <LoadingState label="Loading customer…" />
        ) : query.isError ? (
          normalizeError(query.error).status === 404 ? (
            <EmptyState
              title="Customer not found"
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
                      {customerFields[key]}
                    </Typography>
                    <Typography
                      component="dd"
                      sx={{ m: 0, whiteSpace: 'pre-wrap' }}
                    >
                      {query.data[key] || 'Not provided'}
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
