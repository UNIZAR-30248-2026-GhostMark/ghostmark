/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const ts = require('typescript');

function load(file, imports = {}) {
  const source = fs.readFileSync(path.join(__dirname, '../src', file), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true } }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)((id) => imports[id] ?? require(id), module, module.exports);
  return module.exports;
}

test('la app usa las rutas privadas de mi código y eliminar de la partida', async () => {
  const calls = [];
  const game = load('api/game.ts', {
    './client': { request: (requestPath, options) => { calls.push([requestPath, options]); return Promise.resolve(); } },
  });

  await game.getMyLifeCode('partida con espacio');
  await game.eliminateTarget('partida con espacio', '482913');

  assert.deepEqual(calls, [
    ['/api/v1/partidas/partida%20con%20espacio/yo/codigo', undefined],
    ['/api/v1/partidas/partida%20con%20espacio/eliminaciones', { body: { codigo: '482913' }, method: 'POST' }],
  ]);
});

test('la cuenta atrás del código no baja de cero', () => {
  const { lifeCodeSecondsRemaining } = load('utils/lifeCode.ts');
  assert.equal(lifeCodeSecondsRemaining('2026-11-05T17:31:00Z', Date.parse('2026-11-05T17:30:00.100Z')), 60);
  assert.equal(lifeCodeSecondsRemaining('2026-11-05T17:31:00Z', Date.parse('2026-11-05T17:31:01Z')), 0);
});

test('la carta abre Mi código y Eliminar para su propia partida', () => {
  const source = fs.readFileSync(path.join(__dirname, '../src/screens/MissionCardScreen.tsx'), 'utf8');
  assert.match(source, /navigation\.navigate\("LifeCode", \{ gameId \}\)/);
  assert.match(source, /navigation\.navigate\("Eliminate", \{ gameId, targetAlias: card\?\.aliasObjetivo \}\)/);
});
