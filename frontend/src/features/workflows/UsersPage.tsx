import { useState } from 'react';
import { Button, Stack } from '@mui/material';
import { WorkflowPage, RecordForm } from './WorkflowPage';
import { api } from '../../shared/api';
export function UsersPage() {
  const [module, setModule] = useState('users');
  const [password, setPassword] = useState(false);
  return (
    <Stack spacing={3}>
      <Stack direction="row" spacing={2}>
        <Button onClick={() => setModule('users')}>Users</Button>
        <Button onClick={() => setModule('operators')}>Operators</Button>
        <Button onClick={() => setPassword(true)}>Change my password</Button>
      </Stack>
      <WorkflowPage key={module} module={module} />
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
