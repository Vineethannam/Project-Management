// Root-level convenience commands. Dependencies and application code live in frontend/.
import { spawnSync } from 'node:child_process';
import { cpSync, existsSync, mkdirSync, rmSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const frontend = fileURLToPath(new URL('../', import.meta.url));
const repository = path.dirname(frontend.replace(/[\\/]$/, ''));
const command = process.argv[2];
if (!['dev', 'build'].includes(command)) throw new Error('Expected dev or build');
const result = spawnSync(process.execPath, [path.join(frontend, 'scripts/run-framework.mjs'), command], { cwd: frontend, stdio: 'inherit', env: process.env });
if (result.error) throw result.error;
if (result.status !== 0) process.exit(result.status ?? 1);
if (command === 'build') {
  const source = path.join(frontend, 'dist');
  if (!existsSync(path.join(source, 'server/index.js'))) throw new Error('Frontend Worker build output is missing');
  // Only the generated root output is replaced, never application source.
  const destination = path.join(repository, 'dist');
  rmSync(destination, { recursive: true, force: true });
  mkdirSync(destination, { recursive: true });
  cpSync(source, destination, { recursive: true });
  console.log('Frontend output staged in root dist/ for Sites packaging.');
}
