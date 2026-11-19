/* global __dirname */
// 10b.2: la zona se comprueba al meter el código. Cálculo puro, medición y pantalla «Eliminar».
// Los assert comparan solo booleanos, números y textos: nunca nodos del árbol.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { afterEach, mock, test } = require('node:test');
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

const zoneCheck = load('utils/zoneCheck.ts');
const { zoneResult } = zoneCheck;

// Puntos a tantos metros al este (x) y al norte (y) de la EINA.
const EINA = { lat: 41.6835, lon: -0.8885 };
const M_LAT = 1 / 111_195;
const M_LON = 1 / (111_195 * Math.cos(EINA.lat * Math.PI / 180));
const at = (x, y) => ({ lat: EINA.lat + y * M_LAT, lon: EINA.lon + x * M_LON });
// Cuadrado de 100 m de lado centrado en la EINA.
const square = (name, cx = 0, cy = 0, half = 50) => ({ nombre: name, puntos: [at(cx - half, cy - half), at(cx + half, cy - half), at(cx + half, cy + half), at(cx - half, cy + half)] });
const biblioteca = square('Biblioteca');

// ---------- utils/zoneCheck ----------

test('dentro, fuera y no concluyente según el círculo de la precisión', () => {
  assert.equal(zoneResult(at(0, 0), 10, [biblioteca]), 'DENTRO');
  assert.equal(zoneResult(at(0, 39), 10, [biblioteca]), 'DENTRO', 'a 11 m del borde, el círculo de 10 m cae dentro');
  assert.equal(zoneResult(at(0, 45), 10, [biblioteca]), 'NO_CONCLUYENTE', 'a 5 m del borde, por dentro, cruza');
  assert.equal(zoneResult(at(0, 55), 10, [biblioteca]), 'NO_CONCLUYENTE', 'a 5 m del borde, por fuera, cruza');
  assert.equal(zoneResult(at(0, 61), 10, [biblioteca]), 'FUERA');
  assert.equal(zoneResult(at(300, 300), 10, [biblioteca]), 'FUERA');
});

test('la precisión peor de 50 m o sin posición no concluye (R12-18)', () => {
  assert.equal(zoneResult(at(0, 0), 50, [square('Grande', 0, 0, 200)]), 'DENTRO');
  assert.equal(zoneResult(at(0, 0), 50.5, [square('Grande', 0, 0, 200)]), 'NO_CONCLUYENTE');
  assert.equal(zoneResult(null, null, [biblioteca]), 'NO_CONCLUYENTE');
  assert.equal(zoneResult(at(0, 0), null, [biblioteca]), 'NO_CONCLUYENTE');
});

test('sin zonas es FUERA aunque no haya posición (R12-19)', () => {
  assert.equal(zoneResult(null, null, []), 'FUERA');
});

test('dentro de una zona cuenta aunque toque el borde de otra que se solapa', () => {
  const aulario = square('Aulario', 60, 0);
  assert.equal(zoneResult(at(0, 0), 10, [aulario, biblioteca]), 'DENTRO');
  assert.equal(zoneResult(at(5, 0), 10, [biblioteca, aulario]), 'DENTRO');
  assert.equal(zoneResult(at(150, 0), 10, [biblioteca, aulario]), 'FUERA');
});

test('vale con polígonos no convexos (en forma de L)', () => {
  const ele = { nombre: 'L', puntos: [at(0, 0), at(100, 0), at(100, 40), at(40, 40), at(40, 100), at(0, 100)] };
  assert.equal(zoneResult(at(20, 80), 5, [ele]), 'DENTRO');
  assert.equal(zoneResult(at(80, 80), 5, [ele]), 'FUERA', 'el hueco de la L está fuera');
});

// ---------- utils/zoneLocation ----------

function location(position) {
  const asked = [];
  const module = load('utils/zoneLocation.ts', {
    'expo-location': { Accuracy: { High: 4 }, getCurrentPositionAsync: (options) => { asked.push(options); return typeof position === 'function' ? position() : Promise.resolve(position); } },
    './zoneCheck': zoneCheck,
  });
  return { ...module, asked };
}

afterEach(() => mock.timers.reset());

test('sin zonas no se pide la posición; sin poder cargarlas, no concluyente (R12-19, R12-37)', async () => {
  const { measureZone, asked } = location({ coords: { latitude: EINA.lat, longitude: EINA.lon, accuracy: 5 } });
  assert.equal(await measureZone([]), 'FUERA');
  assert.equal(await measureZone(null), 'NO_CONCLUYENTE');
  assert.equal(asked.length, 0);
});

test('con zonas pide la posición una vez y solo devuelve el resultado', async () => {
  const { measureZone, asked } = location({ coords: { latitude: EINA.lat, longitude: EINA.lon, accuracy: 8 } });
  const result = await measureZone([biblioteca]);
  assert.equal(result, 'DENTRO');
  assert.equal(typeof result, 'string');
  assert.deepEqual(asked, [{ accuracy: 4 }]);
});

test('sin posición en 10 s o si falla, no concluyente (R12-18)', async () => {
  mock.timers.enable({ apis: ['setTimeout'] });
  const { measureZone, LOCATION_TIMEOUT_MS } = location(() => new Promise(() => {}));
  assert.equal(LOCATION_TIMEOUT_MS, 10_000);
  const pending = measureZone([biblioteca]);
  mock.timers.tick(10_000);
  // Si no responde justo a los 10 s, gana «SIN_RESPUESTA» en lugar de quedarse esperando.
  assert.equal(await Promise.race([pending, new Promise((resolve) => setImmediate(() => resolve('SIN_RESPUESTA')))]), 'NO_CONCLUYENTE');
  const failing = location(() => Promise.reject(new Error('sin GPS')));
  assert.equal(await failing.measureZone([biblioteca]), 'NO_CONCLUYENTE');
});

// ---------- EliminateScreen ----------

class ApiError extends Error { constructor(message, status = 400, code, details = {}) { super(message); this.status = status; this.code = code; this.details = details; } }
const native = {
  AppState: { addEventListener: () => ({ remove() {} }) }, Image: 'Image', Pressable: 'Pressable', Text: 'Text', View: 'View',
  StyleSheet: { create: (styles) => styles },
};
let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; });
const textOf = (node) => [node.props.children].flat().join('');
const hasText = (text) => tree.root.findAllByType('Text').some((node) => textOf(node) === text);
const buttonWith = (text) => tree.root.findAllByType('Pressable').find((node) => node.findAllByType('Text').some((child) => textOf(child) === text));
const keys = () => tree.root.findAll((node) => /^Introducir \d$/.test(node.props.accessibilityLabel ?? '') && typeof node.props.onPress === 'function');
const flush = () => act(async () => { await new Promise((resolve) => setImmediate(resolve)); });

function eliminate({ permission = { granted: true, canAskAgain: true }, request = permission, zones = async () => ({ zonas: [biblioteca] }), measured = 'DENTRO' } = {}) {
  const calls = { eliminate: [], measure: [], settings: 0, requested: 0 };
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    'react-native': { ...native, Linking: { openSettings: async () => { calls.settings += 1; } } },
    '@expo/vector-icons': { Ionicons: 'Ionicons' },
    'expo-location': {
      getForegroundPermissionsAsync: async () => permission,
      requestForegroundPermissionsAsync: async () => { calls.requested += 1; return request; },
    },
    '../api/client': { ApiError },
    '../api/game': {
      getMyMissionCard: async () => ({ estado: 'VIVO', aliasObjetivo: 'Pablo', intentosRestantes: 3, bloqueadoHasta: null }),
      getSafeZones: zones,
      eliminateTarget: async (...args) => { calls.eliminate.push(args); return { aliasVictima: 'Pablo', carta: { estado: 'VIVO', aliasObjetivo: 'Marta' } }; },
    },
    '../theme/bangers': { bangers: () => ({}) },
    '../theme/colors': { colors: { background: '', border: '', danger: '', ink: '', lime: '', surfaceRaised: '', text: '', textMuted: '', white: '' } },
    '../utils/zoneLocation': { measureZone: async (value) => { calls.measure.push(value); return measured; } },
  });
  return { EliminateScreen, calls };
}
const navigation = () => ({ replaced: [], goBack() {}, navigate() {}, replace(...args) { this.replaced.push(args); } });
async function type(code) {
  for (const digit of code) {
    const key = keys().find((node) => node.props.accessibilityLabel === `Introducir ${digit}`);
    await act(async () => key.props.onPress());
  }
  await flush();
}

test('sin permiso de ubicación no se puede meter el código hasta darlo (criterio 2)', async () => {
  const { EliminateScreen, calls } = eliminate({ permission: { granted: false, canAskAgain: true }, request: { granted: true, canAskAgain: true } });
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: navigation(), route: { params: { gameId: 'p1' } } })); });
  assert.equal(hasText('Para eliminar hace falta la ubicación'), true);
  assert.equal(hasText('Solo miro si estás en una zona segura al meter el código. Nunca guardo dónde estás.'), true);
  assert.equal(keys().length, 0);
  await act(async () => buttonWith('DAR PERMISO').props.onPress());
  assert.equal(calls.requested, 1);
  assert.equal(hasText('Para eliminar hace falta la ubicación'), false);
  assert.equal(keys().length, 10);
});

test('si se negó para siempre, ofrece abrir los ajustes y sigue sin teclado', async () => {
  const { EliminateScreen, calls } = eliminate({ permission: { granted: false, canAskAgain: false } });
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: navigation(), route: { params: { gameId: 'p1' } } })); });
  await act(async () => buttonWith('ABRIR AJUSTES').props.onPress());
  assert.equal(calls.settings, 1);
  assert.equal(calls.requested, 0);
  assert.equal(keys().length, 0);
});

test('al meter el código se mide con las zonas de la partida y se manda solo el resultado', async () => {
  const { EliminateScreen, calls } = eliminate({ measured: 'DENTRO' });
  const nav = navigation();
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: nav, route: { params: { gameId: 'p1' } } })); });
  await type('482913');
  assert.equal(calls.measure.length, 1);
  assert.equal(calls.measure[0][0].nombre, 'Biblioteca');
  assert.deepEqual(calls.eliminate, [['p1', '482913', 'DENTRO']]);
  assert.equal(nav.replaced[0][0], 'EliminationSuccess');
});

test('si no se pudieron cargar las zonas se mide con null y se manda no concluyente (R12-37)', async () => {
  const { EliminateScreen, calls } = eliminate({ zones: async () => { throw new ApiError('Servidor dormido', 503); }, measured: 'NO_CONCLUYENTE' });
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: navigation(), route: { params: { gameId: 'p1' } } })); });
  await type('482913');
  assert.equal(calls.measure.length === 1 && calls.measure[0] === null, true);
  assert.deepEqual(calls.eliminate, [['p1', '482913', 'NO_CONCLUYENTE']]);
});
