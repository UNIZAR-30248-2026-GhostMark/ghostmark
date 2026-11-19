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

test('el cliente lee y sustituye las zonas completas por partida', async () => {
  const calls = [];
  const { getSafeZones, saveSafeZones } = load('api/game.ts', { './client': { request: async (...args) => { calls.push(args); return { zonas: [] }; } } });
  const zones = [{ nombre: 'Biblioteca', puntos: [{ lat: 41.68, lon: -0.88 }, { lat: 41.681, lon: -0.88 }, { lat: 41.68, lon: -0.881 }] }];
  await getSafeZones('p 1');
  await saveSafeZones('p 1', zones);
  assert.deepEqual(calls, [
    ['/api/v1/partidas/p%201/zonas'],
    ['/api/v1/partidas/p%201/zonas', { body: { zonas: zones }, method: 'PUT' }],
  ]);
});

test('el mapa no consulta la API ni la ubicación y comunica al móvil solo puntos del dibujo', () => {
  const source = fs.readFileSync(path.join(__dirname, '../src/components/MapZones.tsx'), 'utf8');
  assert.match(source, /react-native-webview/);
  assert.match(source, /tile\.openstreetmap\.org/);
  assert.match(source, /ReactNativeWebView\.postMessage/);
  assert.doesNotMatch(source, /expo-location|navigator\.geolocation|fetch\(/);
});

test('el paso de zonas permite deshacer, terminar, editar y borrar antes de guardar', () => {
  const source = fs.readFileSync(path.join(__dirname, '../src/screens/CreateGameZonesScreen.tsx'), 'utf8');
  for (const label of ['DESHACER PUNTO', 'TERMINAR', 'EDITAR', 'ELIMINAR', '¿BORRAR ZONA?', 'saveSafeZones']) assert.match(source, new RegExp(label));
  assert.match(source, /draftPoints\.length < 3/);
});

test('el mapa muestra los vértices, anticipa el cierre de la zona y no reajusta el zoom al editar', () => {
  const source = fs.readFileSync(path.join(__dirname, '../src/components/MapZones.tsx'), 'utf8');
  assert.match(source, /circleMarker/);
  assert.match(source, /draftPulse/);
  assert.match(source, /!data\.editable&&!hasFitted/);
});
