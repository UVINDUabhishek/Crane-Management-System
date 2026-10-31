import { useState } from 'react';
import { Button, Stack, Tabs, Tab, Box } from '@mui/material';
import { WorkflowPage, RecordForm } from './WorkflowPage';
import { api } from '../../shared/api';
export function UsersPage() {
  const [module, setModule] = useState('users');
  const [password, setPassword] = useState(false);
  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <Tabs
          value={module}
          onChange={(_, value) => setModule(value)}
          aria-label="Administration"
          sx={{ flex: 1 }}
        >
          <Tab
            value="users"
            label="Users"
            id="users-tab"
            aria-controls="administration-panel"
          />
          <Tab
            value="operators"
            label="Operators"
            id="operators-tab"
            aria-controls="administration-panel"
          />
        </Tabs>
        <Button onClick={() => setPassword(true)}>Change my password</Button>
      </Stack>
      <Box
        role="tabpanel"
        id="administration-panel"
        aria-labelledby={`${module}-tab`}
      >
        <WorkflowPage key={module} module={module} />
      </Box>
      {password && (
        <RecordForm
          title="Change my password"
          fields={[
            {
              key: 'currentPassword',
              label: 'Current password',
              type: 'password',
              required: true,
            },
            {
              key: 'newPassword',
              label: 'New password',
              type: 'password',
              required: true,
            },
          ]}
          close={() => setPassword(false)}
          submit={(body) => api.put('/admin/me/password', body)}
        />
      )}
    </Stack>
  );
}
