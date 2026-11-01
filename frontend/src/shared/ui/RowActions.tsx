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
}: {
  reference: string;
  view: RowAction['run'];
  edit?: RowAction['run'];
  actions?: RowAction[];
  disabled?: boolean;
}) {
  const id = useId();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);
  const queued = useRef<RowAction | null>(null);
  const trigger = useRef<HTMLElement | null>(null);
  const reduced = useMediaQuery('(prefers-reduced-motion: reduce)');
  return (
    <Stack
      direction="row"
      spacing={0.5}
      sx={{ justifyContent: 'flex-end', whiteSpace: 'nowrap' }}
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
      {edit && (
        <Tooltip title="Edit">
          <span>
            <IconButton
              disabled={disabled}
              aria-label={`Edit ${reference}`}
              sx={{ width: 44, height: 44 }}
              onClick={(e) => edit(e.currentTarget)}
            >
              <EditOutlined fontSize="small" />
            </IconButton>
          </span>
        </Tooltip>
      )}
      {actions.length > 0 && (
        <Tooltip title="More actions">
          <span>
            <IconButton
              disabled={disabled}
              aria-label={`Actions for ${reference}`}
              aria-haspopup="menu"
              aria-expanded={!!anchor}
              aria-controls={anchor ? id : undefined}
              sx={{ width: 44, height: 44 }}
              onClick={(e) => {
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
              if (action && trigger.current) {
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
            disabled={disabled || action.disabled}
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
