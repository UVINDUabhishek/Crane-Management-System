import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { parseCLI, resolveConfig } from 'vitest/node';

// Read-only diagnostics: never fetch, switch branches or change the working tree.
const frontend = fileURLToPath(new URL('../', import.meta.url));
const invokedFrom = process.cwd();
process.chdir(frontend);
const { scripts } = JSON.parse(readFileSync('package.json', 'utf8'));
function git(...args) {
  try {
    return execFileSync('git', args, {
      cwd: frontend,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore'],
    }).trim();
  } catch {
    return null;
  }
}
function summary({ vitestConfig, viteConfig }) {
  return {
    root: vitestConfig.root,
    configFile: viteConfig.configFile,
    testTimeout: vitestConfig.testTimeout,
    maxWorkers: vitestConfig.maxWorkers ?? 'Vitest default',
    environment: vitestConfig.environment,
    setupFiles: vitestConfig.setupFiles,
  };
}
const configOnly = await resolveConfig();
const npmTest = await resolveConfig(parseCLI(scripts.test).options);
console.log(
  JSON.stringify(
    {
      node: process.version,
      platform: process.platform,
      invokedFrom,
      git: {
        root: git('rev-parse', '--show-toplevel'),
        branch: git('branch', '--show-current'),
        commit: git('rev-parse', 'HEAD'),
        upstream: git('rev-parse', '--abbrev-ref', '@{upstream}'),
        fetchRefspecs: git('config', '--get-all', 'remote.origin.fetch'),
        aheadBehind: git(
          'rev-list',
          '--left-right',
          '--count',
          'HEAD...@{upstream}',
        ),
        workingTree: git('status', '--short'),
      },
      npmTest: { command: scripts.test, ...summary(npmTest) },
      directVitest: summary(configOnly),
    },
    null,
    2,
  ),
);
console.log(
  'Remote tracking information reflects your last git fetch. IDE/CLI overrides can change these defaults.',
);
