/** Hold an MSW response until pending-state assertions finish. Release in finally. */
export function createRequestGate() {
  let release!: () => void;
  let markStarted!: () => void;
  const response = new Promise<void>((resolve) => {
    release = resolve;
  });
  const started = new Promise<void>((resolve) => {
    markStarted = resolve;
  });
  return {
    started,
    release,
    wait: () => {
      markStarted();
      return response;
    },
  };
}
