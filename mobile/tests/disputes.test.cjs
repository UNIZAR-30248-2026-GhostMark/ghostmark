/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const ts = require('typescript');

function loadGame(request) {
  const source = fs.readFileSync(path.join(__dirname, '../src/api/game.ts'), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true } }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)((id) => id === './client' ? { request } : {}, module, module.exports);
  return module.exports;
}

test('el cliente sigue el contrato de abrir, leer, responder y decidir disputas', async () => {
  const calls = []; const api = loadGame(async (...args) => { calls.push(args); return { id: 'd1' }; });
  await api.getDisputes('p 1'); await api.getDispute('p 1', 'd/1'); await api.openDispute('p 1', 'ELIMINACION', 'No fue limpio');
  await api.answerDispute('p 1', 'd/1', 'Sí lo fue'); await api.decideDispute('p 1', 'd/1', 'VICTIMA');
  assert.deepEqual(calls, [
    ['/api/v1/partidas/p%201/disputas'], ['/api/v1/partidas/p%201/disputas/d%2F1'],
    ['/api/v1/partidas/p%201/disputas', { body: { tipo: 'ELIMINACION', texto: 'No fue limpio' }, method: 'POST' }],
    ['/api/v1/partidas/p%201/disputas/d%2F1/respuesta', { body: { texto: 'Sí lo fue' }, method: 'POST' }],
    ['/api/v1/partidas/p%201/disputas/d%2F1/decision', { body: { razon: 'VICTIMA' }, method: 'POST' }],
  ]);
});

test('la app ofrece disputa a la víctima, bloquea eliminar y muestra el contador del árbitro', () => {
  const read = (file) => fs.readFileSync(path.join(__dirname, '../src', file), 'utf8');
  assert.match(read('screens/CaughtScreen.tsx'), /ABRIR DISPUTA/);
  assert.match(read('screens/MissionCardScreen.tsx'), /Tienes una disputa abierta/);
  assert.match(read('screens/EliminateScreen.tsx'), /DISPUTA ABIERTA/);
  assert.match(read('screens/GameTabScreen.tsx'), /disputasPorArbitrar/);
  assert.match(read('screens/DisputeScreen.tsx'), /CONFIRMAR ELIMINACIÓN/);
  assert.match(read('screens/DisputeScreen.tsx'), /ANULAR/);
});
