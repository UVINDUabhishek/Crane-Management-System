import { FormControlLabel, Switch } from '@mui/material';
export function ActivationControl({
  active,
  name,
  disabled,
  onChange,
}: {
  active: boolean;
  name: string;
  disabled: boolean;
  onChange: (trigger: HTMLElement) => void;
}) {
  return (
    <FormControlLabel
      label={active ? 'Active' : 'Inactive'}
      control={
        <Switch
          checked={active}
          disabled={disabled}
          onChange={(event) => onChange(event.target)}
          slotProps={{ input: { 'aria-label': `Activation for ${name}` } }}
        />
      }
    />
  );
}
