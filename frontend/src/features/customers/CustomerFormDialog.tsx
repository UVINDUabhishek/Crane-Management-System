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
  customerSchema,
  customerFields,
  fieldNames,
  formDefaults,
  type CustomerValues,
} from './customerSchema';
import {
  customersApi,
  customerKeys,
  type CustomerResponse,
} from './customersApi';
import { normalizeError } from '../../shared/api';
import { ErrorState } from '../../shared/States';
export function CustomerFormDialog({
  customer,
  onClose,
  onSaved,
}: {
  customer?: CustomerResponse;
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
  } = useForm<CustomerValues>({
    resolver: zodResolver(customerSchema),
    defaultValues: formDefaults(customer),
  });
  const mutation = useMutation({
    mutationFn: (values: CustomerValues) =>
      customer
        ? customersApi.update(customer.id, values)
        : customersApi.create(values),
    onSuccess: async () => {
      await cache.invalidateQueries({ queryKey: customerKeys.all });
      onSaved();
    },
    onError: (error) => {
      for (const field of normalizeError(error).fields) {
        if (fieldNames.includes(field.field as keyof CustomerValues))
          setError(field.field as keyof CustomerValues, {
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
        {customer ? 'Edit customer' : 'Add customer'}
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
            Enter company, billing and contact information. Email is optional.
          </DialogContentText>
          <Stack spacing={2.5}>
            {mutation.isError && <ErrorState error={mutation.error} />}
            {fieldNames.map((key, index) => (
              <TextField
                key={key}
                autoFocus={index === 0}
                id={`${id}-${key}`}
                label={customerFields[key]}
                required={key !== 'customerEmail'}
                {...register(key)}
                disabled={mutation.isPending}
                error={!!errors[key]}
                helperText={errors[key]?.message}
                multiline={key === 'companyAddress' || key === 'billAddress'}
                minRows={key.endsWith('Address') ? 2 : undefined}
                type={key === 'customerEmail' ? 'email' : 'text'}
                slotProps={{
                  htmlInput: {
                    inputMode:
                      key === 'contactNo'
                        ? 'tel'
                        : key === 'customerEmail'
                          ? 'email'
                          : 'text',
                    autoComplete:
                      key === 'contactNo'
                        ? 'tel'
                        : key === 'customerEmail'
                          ? 'email'
                          : 'off',
                  },
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
            {mutation.isPending ? 'Saving…' : 'Save customer'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}
