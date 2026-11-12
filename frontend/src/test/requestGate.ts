import { waitFor } from '@testing-library/react';
const pending = new Set<() => void>();
export function releaseRequestGates() {
  for (const release of pending) release();
}
/** Hold an MSW response until pending-state assertions finish. Release in finally. */
export function createRequestGate() {
  let release!: () => void;
  let hasStarted = false;
  const response = new Promise<void>((resolve) => {
    release = () => {
      pending.delete(release);
      resolve();
    };
  });
  pending.add(release);

  return {
    // Bounded, lazy expectation: a missing handler fails at the request boundary.
    get started() {
      return waitFor(() => {
        if (!hasStarted)
          throw new Error('Controlled request did not reach its handler');
      });
    },
    release,
    wait: () => {
      hasStarted = true;
      return response;
    },
  };
}
