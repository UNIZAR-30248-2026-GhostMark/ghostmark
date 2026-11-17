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
  new Function('require', 'module', 'exports', code)((id) => {
    if (id in imports) return imports[id];
    if (id.endsWith('.png')) return 1;
    return require(id);
  }, module, module.exports);
  return module.exports;
}

const native = {
  ActivityIndicator: 'ActivityIndicator', Image: 'Image', Modal: 'Modal', Pressable: 'Pressable', ScrollView: 'ScrollView', Text: 'Text', View: 'View',
  useWindowDimensions: () => ({ height: 800, width: 360 }),
  Alert: { alert() {} },
  StyleSheet: { create: (styles) => styles, absoluteFillObject: {} },
};
const common = {
  'react-native': native,
  '@expo/vector-icons': { Ionicons: 'Ionicons' },
  '@react-navigation/native': { useFocusEffect: (effect) => React.useEffect(effect, [effect]) },
  '../api/client': { ApiError: class ApiError extends Error {} },
  '../api/auth': { getCurrentAccount: () => ({ alias: 'Marta' }) },
  '../api/inscription': { getPlayers: async () => [{ id: 'i1', alias: 'Marta', esMaster: false, estado: 'VIVO' }, { id: 'i2', alias: 'Pablo', esMaster: false, estado: 'VIVO' }] },
  '../components/ConfirmDialog': { ConfirmDialog: 'ConfirmDialog' },
  '../components/MapZones': { MapZones: 'MapZones' },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../components/Screen': { Screen: 'Screen' },
  '../theme/bangers': { bangers: () => ({}) },
  '../theme/colors': { colors: {} },
};

const game = { id: 'p1', nombre: 'Killer EINA', estado: 'EN_JUEGO', inicioEn: '2026-11-16T09:00:00+01:00', finEn: '2026-11-23T21:00:00+01:00', numInscritos: 2, soyMaster: false, soyJugador: true, motivoCancelacion: null, resultado: null };
const biblioteca = { id: 'z1', nombre: 'Biblioteca', puntos: [{ lat: 41.683, lon: -0.888 }, { lat: 41.684, lon: -0.888 }, { lat: 41.684, lon: -0.887 }] };
const aulario = { id: 'z2', nombre: 'Aulario B', puntos: [{ lat: 41.682, lon: -0.889 }, { lat: 41.683, lon: -0.889 }, { lat: 41.683, lon: -0.888 }] };
const comedor = { id: 'z3', nombre: 'Comedor', puntos: [{ lat: 41.681, lon: -0.886 }, { lat: 41.682, lon: -0.886 }, { lat: 41.682, lon: -0.885 }] };

let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; });
const texts = () => tree.root.findAllByType('Text').map((node) => [node.props.children].flat().join('')).join(' | ');

async function mountTab(zones) {
  const requested = [];
  const navigation = { destinations: [], navigate(...args) { this.destinations.push(args); }, replace() {} };
  const { GameTabScreen } = load('screens/GameTabScreen.tsx', { ...common,
    '../api/game': { getMyGames: async () => [game], closeGame: async () => game, getSafeZones: async (id) => { requested.push(id); if (zones instanceof Error) throw zones; return { zonas: zones }; } },
  });
  await act(async () => { tree = create(React.createElement(GameTabScreen, { navigation, route: { params: { gameId: 'p1' } } })); });
  return { navigation, requested };
}

test('sin zonas en la partida la pestaña Partida no enseña el bloque de zonas', async () => {
  const { requested } = await mountTab([]);
  assert.deepEqual(requested, ['p1'], 'pide las zonas de su partida');
  assert.equal(texts().includes('ZONAS SEGURAS'), false);
  assert.equal(tree.root.findAllByType('MapZones').length, 0);
});

test('con una zona enseña el mapa de solo lectura, «1 zona» y la nota de que nadie ve dónde estás', async () => {
  await mountTab([biblioteca]);
  assert.equal(texts().includes('ZONAS SEGURAS'), true);
  assert.equal(texts().includes('1 zona'), true);
  assert.equal(texts().includes('No se elimina dentro de una zona. Nadie ve dónde estás.'), true);
  const map = tree.root.findByType('MapZones');
  assert.equal(map.props.interactive, false, 'el mapa de la pestaña no se puede editar');
  assert.equal(map.props.editable, undefined, 'no está en modo dibujo');
  assert.deepEqual(map.props.zones, [biblioteca]);
});

test('con varias zonas dice cuántas hay y «VER MAPA» abre el mapa entero con las mismas zonas', async () => {
  const { navigation } = await mountTab([aulario, biblioteca, comedor]);
  assert.equal(texts().includes('3 zonas'), true);
  const open = tree.root.findAll((node) => node.props.accessibilityLabel === 'Ver mapa de zonas seguras' && typeof node.props.onPress === 'function')[0];
  await act(async () => open.props.onPress());
  assert.deepEqual(navigation.destinations, [['SafeZonesMap', { zones: [aulario, biblioteca, comedor] }]]);
});

test('si no se pueden leer las zonas, la pestaña Partida sigue con los jugadores y sin el bloque', async () => {
  await mountTab(new Error('sin red'));
  assert.equal(texts().includes('ZONAS SEGURAS'), false);
  assert.equal(texts().includes('JUGADORES'), true);
});

test('el mapa entero de zonas es de solo lectura, sin dibujo, y vuelve a Partida', async () => {
  const { SafeZonesMapScreen } = load('screens/SafeZonesMapScreen.tsx', common);
  let back = 0;
  await act(async () => { tree = create(React.createElement(SafeZonesMapScreen, { navigation: { goBack() { back += 1; } }, route: { params: { zones: [biblioteca] } } })); });
  const map = tree.root.findByType('MapZones');
  assert.deepEqual(map.props.zones, [biblioteca]);
  assert.equal(map.props.editable, undefined);
  assert.equal(map.props.onAddPoint, undefined);
  const close = tree.root.findAll((node) => node.props.accessibilityLabel === 'Volver a partida' && typeof node.props.onPress === 'function')[0];
  await act(async () => close.props.onPress());
  assert.equal(back, 1);
});

test('ni la pestaña Partida ni el mapa entero piden la ubicación de nadie', () => {
  for (const file of ['screens/GameTabScreen.tsx', 'screens/SafeZonesMapScreen.tsx']) {
    const source = fs.readFileSync(path.join(__dirname, '../src', file), 'utf8');
    assert.doesNotMatch(source, /expo-location|navigator\.geolocation|getCurrentPosition|watchPosition/, file);
  }
});
