import { useId, useRef } from 'react';
import {
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Stack,
  TextField,
  useMediaQuery,
} from '@mui/material';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  craneSchema,
  craneFields,
  fieldNames,
  formDefaults,
  type CraneValues,
} from './craneSchema';
import { cranesApi, craneKeys, type CraneResponse } from './cranesApi';
import { normalizeError } from '../../shared/api';
import { ErrorState } from '../../shared/States';
export function CraneFormDialog({
  crane,
  onClose,
  onSaved,
}: {
  crane?: CraneResponse;
  onClose: () => void;
  onSaved: () => void;
}) {
  const id = useId(),
    lock = useRef(false),
    cache = useQueryClient();
  const reduced = useMediaQuery('(prefers-reduced-motion: reduce)');
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<CraneValues>({
    resolver: zodResolver(craneSchema),
    defaultValues: formDefaults(crane),
  });
  const mutation = useMutation({
    mutationFn: (values: CraneValues) =>
      crane ? cranesApi.update(crane.id, values) : cranesApi.create(values),
    onSuccess: async () => {
      await cache.invalidateQueries({ queryKey: craneKeys.all });
      await cache.invalidateQueries({ queryKey: ['lookups'] });
      onSaved();
    },
    onError: (error) => {
      for (const field of normalizeError(error).fields) {
        if (fieldNames.includes(field.field as keyof CraneValues))
          setError(field.field as keyof CraneValues, {
            message: field.message,
          });
      }
    },
  });
  return (
    <Dialog
      open
      fullWidth
      maxWidth="sm"
      onClose={() => {
        if (!lock.current) onClose();
      }}
      disableEscapeKeyDown={mutation.isPending}
      transitionDuration={reduced ? 0 : undefined}
      aria-labelledby={`${id}-title`}
      aria-describedby={`${id}-description`}
    >
      <DialogTitle id={`${id}-title`}>
        {crane ? 'Edit crane' : 'Add crane'}
      </DialogTitle>
      <form
        noValidate
        onSubmit={handleSubmit(async (values) => {
          if (lock.current) return;
          lock.current = true;
          try {
            await mutation.mutateAsync(values);
          } catch {
            /* Render normalized mutation errors without closing the form. */
          } finally {
            lock.current = false;
          }
        })}
      >
        <DialogContent>
          <DialogContentText id={`${id}-description`} sx={{ mb: 3 }}>
            Enter crane specifications. Number, operational status and
            activation are controlled separately.
          </DialogContentText>
          <Stack spacing={2.5}>
            {mutation.isError && <ErrorState error={mutation.error} />}
            {fieldNames.map((key, index) => (
              <TextField
                key={key}
                autoFocus={index === 0}
                id={`${id}-${key}`}
                label={craneFields[key]}
                required
                {...register(
                  key,
                  key === 'capacity' || key === 'year'
                    ? { valueAsNumber: true }
                    : {},
                )}
                disabled={mutation.isPending}
                error={!!errors[key]}
                helperText={errors[key]?.message}
                type={key === 'capacity' || key === 'year' ? 'number' : 'text'}
                slotProps={{
                  htmlInput: { step: key === 'capacity' ? 'any' : 1 },
                }}
              />
            ))}
          </Stack>
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <Button disabled={mutation.isPending} onClick={onClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={mutation.isPending}
          >
            {mutation.isPending ? 'Saving…' : 'Save crane'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
