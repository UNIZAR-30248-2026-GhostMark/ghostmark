/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { afterEach, test } = require('node:test');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');

global.IS_REACT_ACT_ENVIRONMENT = true;

function load(file, imports = {}) {
  const source = fs.readFileSync(path.join(__dirname, '../src', file), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true } }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)((id) => (id in imports ? imports[id] : require(id)), module, module.exports);
  return module.exports;
}

const geometry = () => load('utils/safeZoneGeometry.ts');

class ApiError extends Error { constructor(message, status, fieldErrors = [], code) { super(message); this.status = status; this.fieldErrors = fieldErrors; this.code = code; } }
const common = {
  'react-native': { Modal: 'Modal', Pressable: 'Pressable', ScrollView: 'ScrollView', Text: 'Text', TextInput: 'TextInput', View: 'View', StyleSheet: { create: (styles) => styles } },
  '../api/client': { ApiError },
  '../components/MapZones': { MapZones: 'MapZones' },
  '../components/GameWizardHeader': { GameWizardHeader: 'GameWizardHeader' },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../components/Screen': { Screen: 'Screen' },
  '../theme/bangers': { bangers: () => ({}) },
  '../theme/colors': { colors: {} },
};
const zone = (id, nombre, base = 0) => ({ id, nombre, puntos: [{ lat: 41.68 + base, lon: -0.88 }, { lat: 41.681 + base, lon: -0.88 }, { lat: 41.681 + base, lon: -0.881 }] });

let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; });
const texts = () => tree.root.findAllByType('Text').map((node) => [node.props.children].flat().join('')).join(' | ');
const map = () => tree.root.findByType('MapZones');
const tap = async (point) => act(async () => map().props.onAddPoint(point));
const pressLabel = async (label) => act(async () => tree.root.findAll((node) => node.props.accessibilityLabel === label && typeof node.props.onPress === 'function')[0].props.onPress());
const primary = () => tree.root.findByType('PrimaryButton');
const finish = () => tree.root.findAll((node) => node.props.accessibilityLabel === 'Terminar zona' && typeof node.props.onPress === 'function')[0];

async function mountEditor(params, api = {}) {
  const calls = [];
  const navigation = { back: 0, goBack() { this.back += 1; }, reset() {} };
  const { CreateGameZonesScreen } = load('screens/CreateGameZonesScreen.tsx', { ...common,
    '../api/game': { createGame: async () => ({ id: 'nueva' }), getSafeZones: async (id) => { calls.push(['get', id]); return { zonas: api.zones ?? [] }; }, saveSafeZones: async (...args) => { calls.push(['put', ...args]); if (api.saveError) throw api.saveError; return { zonas: args[1] }; } },
    '../utils/safeZoneGeometry': geometry(),
  });
  await act(async () => { tree = create(React.createElement(CreateGameZonesScreen, { navigation, route: { params } })); });
  return { calls, navigation };
}

test('un punto ya marcado no se repite y avisa', async () => {
  await mountEditor({ configuration: {} });
  await tap({ lat: 41.68, lon: -0.88 });
  await tap({ lat: 41.68, lon: -0.88 });
  assert.equal(map().props.draftPoints.length, 1);
  assert.equal(texts().includes('Ese punto ya está marcado.'), true);
});

test('no deja terminar una zona cuyos lados se cruzan y explica el motivo (#344)', async () => {
  await mountEditor({ configuration: {} });
  // Rectángulo recorrido en cruz: 1-3-2-4.
  for (const point of [{ lat: 0, lon: 0 }, { lat: 2, lon: 2 }, { lat: 0, lon: 2 }, { lat: 2, lon: 0 }]) await tap(point);
  await act(async () => finish().props.onPress());
  assert.equal(texts().includes('Los lados de esta zona se cruzan. Corrige los puntos antes de terminar.'), true);
  assert.equal(tree.root.findAllByType('Modal').some((node) => node.props.visible), false, 'la pantalla conserva el editor y no abre el nombre');
});

test('no deja terminar una zona que se solapa con otra y dice cuál (#344)', async () => {
  const biblioteca = { id: 'biblioteca', nombre: 'Biblioteca', puntos: [{ lat: 0, lon: 0 }, { lat: 2, lon: 0 }, { lat: 2, lon: 2 }, { lat: 0, lon: 2 }] };
  await mountEditor({ editGameId: 'p1' }, { zones: [biblioteca] });
  for (const point of [{ lat: .5, lon: .5 }, { lat: 1, lon: .5 }, { lat: .5, lon: 1 }]) await tap(point);
  await act(async () => finish().props.onPress());
  assert.equal(texts().includes('Esta zona se solapa con «Biblioteca». Cambia sus puntos antes de terminar.'), true);
});

test('con 10 zonas no deja empezar otra, pero sí editar una de ellas', async () => {
  const ten = Array.from({ length: 10 }, (_, index) => zone(`z${index}`, `Zona ${index}`, index / 100));
  await mountEditor({ editGameId: 'p1' }, { zones: ten });
  await tap({ lat: 41.7, lon: -0.9 });
  assert.equal(map().props.draftPoints.length, 0);
  assert.equal(texts().includes('Como máximo hay 10 zonas por partida.'), true);
  await pressLabel('Editar Zona 3');
  await tap({ lat: 41.7, lon: -0.9 });
  assert.equal(map().props.draftPoints.length, 4, 'al editar se pueden añadir puntos a la zona');
});

test('si se deshacen todos los puntos de la zona que se editaba, vuelve a la lista tal como estaba', async () => {
  const biblioteca = zone('z1', 'Biblioteca');
  await mountEditor({ editGameId: 'p1' }, { zones: [biblioteca] });
  await pressLabel('Editar Biblioteca');
  assert.equal(map().props.zones.length, 0);
  for (let i = 0; i < 3; i += 1) await pressLabel('Deshacer último punto');
  assert.deepEqual(map().props.zones, [biblioteca]);
  assert.equal(map().props.draftPoints.length, 0);
});

test('desde la sala de espera carga las zonas de la partida, «GUARDAR» las guarda y vuelve (R12-32)', async () => {
  const biblioteca = zone('z1', 'Biblioteca');
  const { calls, navigation } = await mountEditor({ editGameId: 'p1' }, { zones: [biblioteca] });
  assert.deepEqual(calls, [['get', 'p1']]);
  assert.equal(tree.root.findAllByType('GameWizardHeader').length, 0, 'sin la barra de pasos del asistente');
  assert.equal(primary().props.children, 'GUARDAR');
  await act(async () => primary().props.onPress());
  assert.deepEqual(calls.at(-1), ['put', 'p1', [biblioteca]]);
  assert.equal(navigation.back, 1);
});

test('si la partida ya arrancó, «GUARDAR» lo explica y no vuelve', async () => {
  const { navigation } = await mountEditor({ editGameId: 'p1' }, { zones: [], saveError: new ApiError('La partida no se puede editar', 409, [], 'PARTIDA_NO_EDITABLE') });
  await act(async () => primary().props.onPress());
  assert.equal(texts().includes('La partida ya ha arrancado: las zonas no se pueden cambiar.'), true);
  assert.equal(navigation.back, 0);
});

test('el nombre de la zona se pinta en el mapa como texto, no como HTML, y las teselas llevan el Referer del servidor', () => {
  const source = fs.readFileSync(path.join(__dirname, '../src/components/MapZones.tsx'), 'utf8');
  assert.doesNotMatch(source, /bindTooltip\(zone\.nombre/);
  assert.match(source, /label\.textContent=zone\.nombre/);
  assert.match(source, /baseUrl: getConfiguredApiUrl\(\)/);
});
