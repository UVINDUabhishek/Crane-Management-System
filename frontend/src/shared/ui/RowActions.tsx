import { useId, useRef, useState } from 'react';
import {
  IconButton,
  Menu,
  MenuItem,
  Stack,
  Tooltip,
  useMediaQuery,
} from '@mui/material';
import VisibilityOutlined from '@mui/icons-material/VisibilityOutlined';
import EditOutlined from '@mui/icons-material/EditOutlined';
import MoreHoriz from '@mui/icons-material/MoreHoriz';
export interface RowAction {
  label: string;
  run: (trigger: HTMLElement) => void;
  disabled?: boolean;
}
export function RowActions({
  reference,
  view,
  edit,
  actions = [],
  disabled = false,
  changesDisabled = false,
}: {
  reference: string;
  view: RowAction['run'];
  edit?: RowAction['run'];
  actions?: RowAction[];
  disabled?: boolean;
  changesDisabled?: boolean;
}) {
  const id = useId();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const queued = useRef<RowAction | null>(null);
  const trigger = useRef<HTMLElement | null>(null);
  const eligibility = useRef({ disabled, changesDisabled, actions });
  eligibility.current = { disabled, changesDisabled, actions };
  const reduced = useMediaQuery('(prefers-reduced-motion: reduce)');
  return (
    <Stack
      sx={{
        display: 'grid',
        gridTemplateColumns: 'repeat(3, 44px)',
        gap: 0.5,
        whiteSpace: 'nowrap',
        '& > :nth-of-type(1)': { gridColumn: 1 },
      }}
    >
      <Tooltip title="View details">
        <span>
          <IconButton
            disabled={disabled}
            aria-label={`View details for ${reference}`}
            sx={{ width: 44, height: 44 }}
            onClick={(e) => view(e.currentTarget)}
          >
            <VisibilityOutlined fontSize="small" />
          </IconButton>
        </span>
      </Tooltip>
      {edit ? (
        <Tooltip title={changesDisabled ? 'Checking job status…' : 'Edit'}>
          <span>
            <IconButton
              disabled={disabled}
              aria-disabled={changesDisabled || undefined}
              aria-label={`Edit ${reference}`}
              sx={{
                width: 44,
                height: 44,
                ...(changesDisabled && {
                  color: 'action.disabled',
                  cursor: 'not-allowed',
                }),
              }}
              onClick={(e) => {
                if (!changesDisabled) edit(e.currentTarget);
              }}
            >
              <EditOutlined fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
      ) : (
        <span aria-hidden="true" />
      )}
      {actions.length > 0 && (
        <Tooltip
          title={changesDisabled ? 'Checking job status…' : 'More actions'}
        >
          <span>
            <IconButton
              disabled={disabled}
              aria-disabled={changesDisabled || undefined}
              aria-label={`Actions for ${reference}`}
              aria-haspopup="menu"
              aria-expanded={!!anchor}
              aria-controls={anchor ? id : undefined}
              sx={{
                width: 44,
                height: 44,
                ...(changesDisabled && {
                  color: 'action.disabled',
                  cursor: 'not-allowed',
                }),
              }}
              onClick={(e) => {
                if (changesDisabled) return;
                trigger.current = e.currentTarget;
                setAnchor(e.currentTarget);
              }}
            >
              <MoreHoriz fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
      )}
      <Menu
        id={id}
        anchorEl={anchor}
        open={!!anchor}
        onClose={() => setAnchor(null)}
        transitionDuration={reduced ? 0 : undefined}
        slotProps={{
          transition: {
            onExited: () => {
              const action = queued.current;
              queued.current = null;
              if (
                action &&
                trigger.current?.isConnected &&
                !eligibility.current.disabled &&
                !eligibility.current.changesDisabled &&
                eligibility.current.actions.some(
                  (current) =>
                    current.label === action.label && !current.disabled,
                )
              ) {
                trigger.current.focus();
                action.run(trigger.current);
              }
            },
          },
        }}
      >
        {actions.map((action) => (
          <MenuItem
            key={action.label}
            disabled={disabled || changesDisabled || action.disabled}
            sx={{ minHeight: 44 }}
            onClick={() => {
              queued.current = action;
              setAnchor(null);
            }}
          >
            {action.label}
          </MenuItem>
        ))}
      </Menu>
    </Stack>
  );
}
