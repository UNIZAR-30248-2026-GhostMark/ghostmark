/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test, afterEach } = require('node:test');
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
const client = load('api/client.ts');
const api = load('api/inscription.ts', { './client': client });
const native = {
  View: 'View', Text: 'Text', Pressable: 'Pressable', Image: 'Image', ScrollView: 'ScrollView', ActivityIndicator: 'ActivityIndicator',
  StyleSheet: { create: (styles) => styles }, useWindowDimensions: () => ({ width: 390, height: 844 }),
};
const common = {
  'react-native': native,
  '@expo/vector-icons': { Ionicons: 'Ionicons' },
  '../components/Screen': { Screen: 'Screen' },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../theme/colors': { colors: {} },
  '../theme/typography': { typography: {} },
  '../api/client': client,
};
const code = 'ABCDEFGH';
const game = { nombre: 'Partida EINA', estado: 'EN_INSCRIPCION', inicioEn: '2026-11-01T10:00:00Z', finEn: '2026-11-02T10:00:00Z', aliasMaster: 'ana', numInscritos: 1 };
let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; });
async function mount(component, props) { await act(async () => { tree = create(React.createElement(component, props)); }); }
const shown = () => JSON.stringify(tree.toJSON());
const button = (label) => tree.root.findAll((node) => (node.type === 'PrimaryButton' && node.props.children === label)
  || (node.type === 'Pressable' && node.findAllByType('Text').some((text) => text.props.children === label)))[0];

test('el cliente de inscripción sigue el contrato de la 3.1 y la 3.3', async () => {
  const calls = [];
  global.fetch = async (url, options) => {
    calls.push([options.method ?? 'GET', new URL(url).pathname, options.body && JSON.parse(options.body)]);
    return Response.json({});
  };
  await api.joinGame(code);
  await api.getPlayers('p1');
  await api.getGameSchedule('p1');
  await api.updateGameSchedule('p1', { franjas: [] });
  assert.deepEqual(calls, [
    ['POST', '/api/v1/inscripciones', { codigo: code, aceptaNormas: true }],
    ['GET', '/api/v1/partidas/p1/inscritos', undefined],
    ['GET', '/api/v1/partidas/p1/yo/horario', undefined],
    ['PUT', '/api/v1/partidas/p1/yo/horario', { franjas: [] }],
  ]);
});

test('el error conserva el código del servidor para distinguir los 409', async () => {
  global.fetch = async () => Response.json({ detail: 'Ya está inscrito', codigo: 'YA_INSCRITO' }, { status: 409 });
  await assert.rejects(api.joinGame(code), (error) => error instanceof client.ApiError
    && error.status === 409 && error.code === 'YA_INSCRITO');
});

test('aceptar las normas inscribe y abre la sala de esa partida', async () => {
  let joined; let destination;
  const { GameNormsScreen } = load('screens/GameNormsScreen.tsx', { ...common,
    '../api/inscription': { joinGame: async (value) => { joined = value; return { id: 'i1', partidaId: 'p1' }; } },
  });
  await mount(GameNormsScreen, { navigation: { replace: (...args) => { destination = args; } }, route: { params: { code, game } } });
  await act(async () => button('ACEPTO Y ME UNO').props.onPress());
  assert.equal(joined, code);
  assert.deepEqual(destination, ['WaitingRoom', { gameId: 'p1', game }]);
});

test('si ya estaba inscrito lo dice y lleva a mis partidas', async () => {
  let reset; let replaced = false;
  const { GameNormsScreen } = load('screens/GameNormsScreen.tsx', { ...common,
    '../api/inscription': { joinGame: async () => { throw new client.ApiError('Ya está inscrito', 409, [], 'YA_INSCRITO'); } },
  });
  await mount(GameNormsScreen, { navigation: { replace: () => { replaced = true; }, reset: (value) => { reset = value; } }, route: { params: { code, game } } });
  await act(async () => button('ACEPTO Y ME UNO').props.onPress());
  assert.match(shown(), /Ya estás inscrito en esta partida/);
  assert.equal(replaced, false);
  await act(async () => button('IR A MIS PARTIDAS').props.onPress());
  assert.deepEqual(reset, { index: 0, routes: [{ name: 'Home' }] });
});

test('si el servidor rechaza la inscripción no entra en la sala', async () => {
  let replaced = false;
  const { GameNormsScreen } = load('screens/GameNormsScreen.tsx', { ...common,
    '../api/inscription': { joinGame: async () => { throw new client.ApiError('La partida ya no admite inscripciones', 409, [], 'PARTIDA_NO_ABIERTA'); } },
  });
  await mount(GameNormsScreen, { navigation: { replace: () => { replaced = true; } }, route: { params: { code, game } } });
  await act(async () => button('ACEPTO Y ME UNO').props.onPress());
  assert.match(shown(), /La partida ya no admite inscripciones/);
  assert.equal(replaced, false);
});

function waitingRoom(players, alias) {
  return load('screens/WaitingRoomScreen.tsx', { ...common,
    '../api/inscription': { getPlayers: async (value) => { assert.equal(value, 'p1'); return players; } },
    '../api/auth': { getCurrentAccount: () => ({ alias }) },
  }).WaitingRoomScreen;
}

test('la sala muestra los inscritos reales y «Cuenta eliminada» si borró la cuenta', async () => {
  let destination;
  const players = [{ id: 'i1', alias: 'ana', esMaster: true }, { id: 'i2', alias: 'bea', esMaster: false }, { id: 'i3', alias: null, esMaster: false }];
  await mount(waitingRoom(players, 'bea'), { navigation: { navigate: (...args) => { destination = args; } }, route: { params: { gameId: 'p1', game } } });
  const text = shown();
  for (const expected of ['ana', 'bea', 'Cuenta eliminada', '3 inscritos']) assert.match(text, new RegExp(expected));
  assert.doesNotMatch(text, /Pablo|Lucía|fantasmilla/);
  assert.equal(button('ARRANCAR YA'), undefined, 'solo el máster ve «Arrancar ya»');
  await act(async () => button('REVISAR MI HORARIO').props.onPress());
  assert.deepEqual(destination, ['Schedule', { gameId: 'p1' }]);
});

test('el máster que juega ve «Arrancar ya» en su sala', async () => {
  await mount(waitingRoom([{ id: 'i1', alias: 'ana', esMaster: true }], 'ana'), { navigation: {}, route: { params: { gameId: 'p1', game } } });
  assert.match(shown(), /Eres el máster/);
  assert.notEqual(button('ARRANCAR YA'), undefined);
});

test('revisar el horario de una partida no toca el de la cuenta', async () => {
  const calls = [];
  const { ScheduleScreen } = load('screens/ScheduleScreen.tsx', { ...common,
    '../api/schedule': { getSchedule: async () => { calls.push('cuenta'); return { franjas: [] }; }, updateSchedule: async () => { calls.push('guardar cuenta'); } },
    '../api/inscription': {
      getGameSchedule: async (id) => { calls.push(`partida ${id}`); return { franjas: [] }; },
      updateGameSchedule: async (id) => { calls.push(`guardar partida ${id}`); return { franjas: [] }; },
    },
    '../components/DayScheduleModal': { DayScheduleModal: 'DayScheduleModal' },
    '../components/WeeklyScheduleGrid': { WeeklyScheduleGrid: 'WeeklyScheduleGrid', scheduleToSelectedSlots: () => new Set(), selectedSlotsToSchedule: () => ({ franjas: [] }) },
  });
  await mount(ScheduleScreen, { navigation: { canGoBack: () => true, goBack: () => {} }, route: { params: { gameId: 'p1' } } });
  await act(async () => tree.root.findByType('PrimaryButton').props.onPress());
  assert.deepEqual(calls, ['partida p1', 'guardar partida p1']);
});
