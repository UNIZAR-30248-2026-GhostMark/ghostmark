/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test, afterEach, mock } = require('node:test');
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
    if (id.endsWith('/theme/bangers')) return { bangers: (fontSize) => ({ fontFamily: 'Bangers', fontSize, paddingRight: Math.ceil(fontSize * 0.18) }) };
    if (id.endsWith('.png')) return 1;
    return require(id);
  }, module, module.exports);
  return module.exports;
}
const client = load('api/client.ts');
const api = load('api/inscription.ts', { './client': client });
const gameApi = load('api/game.ts', { './client': client });
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
  '@react-navigation/native': { useIsFocused: () => focus.value },
};
const focus = { value: true };
const code = 'ABCDEFGH';
const game = { nombre: 'Partida EINA', estado: 'EN_INSCRIPCION', inicioEn: '2026-11-01T10:00:00Z', finEn: '2026-11-02T10:00:00Z', aliasMaster: 'ana', numInscritos: 1 };
let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; focus.value = true; mock.timers.reset(); });
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

test('el arranque usa el endpoint de la 4.2 con la partida autenticada', async () => {
  let call;
  global.fetch = async (url, options) => {
    call = [options.method, new URL(url).pathname];
    return Response.json({ id: 'p1', estado: 'EN_JUEGO' });
  };
  await gameApi.startGame('p1');
  assert.deepEqual(call, ['POST', '/api/v1/partidas/p1/arranque']);
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

test('las normas usan el singular con un único inscrito', async () => {
  const { GameNormsScreen } = load('screens/GameNormsScreen.tsx', { ...common,
    '../api/inscription': { joinGame: async () => ({ id: 'i1', partidaId: 'p1' }) },
  });
  await mount(GameNormsScreen, { navigation: { replace: () => {} }, route: { params: { code, game } } });
  assert.match(shown(), /1 inscrito/);
  assert.doesNotMatch(shown(), /1 inscritos/);
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

function waitingRoom(players, isMaster = false) {
  return load('screens/WaitingRoomScreen.tsx', { ...common,
    '../api/inscription': { getPlayers: async (value) => { assert.equal(value, 'p1'); return players; } },
    '../api/game': {
      getMyGames: async () => [{ id: 'p1', ...game, soyMaster: isMaster }],
      startGame: async () => ({ id: 'p1', ...game, estado: 'EN_JUEGO', soyMaster: isMaster }),
    },
  }).WaitingRoomScreen;
}

test('la sala muestra los inscritos reales y «Cuenta eliminada» si borró la cuenta', async () => {
  let destination;
  const players = [{ id: 'i1', alias: 'ana', esMaster: true }, { id: 'i2', alias: 'bea', esMaster: false }, { id: 'i3', alias: null, esMaster: false }];
  await mount(waitingRoom(players), { navigation: { navigate: (...args) => { destination = args; } }, route: { params: { gameId: 'p1', game } } });
  const text = shown();
  for (const expected of ['ana', 'bea', 'Cuenta eliminada', '3 inscritos']) assert.match(text, new RegExp(expected));
  assert.doesNotMatch(text, /Pablo|Lucía|fantasmilla/);
  assert.equal(button('ARRANCAR YA'), undefined, 'solo el máster ve «Arrancar ya»');
  await act(async () => button('REVISAR MI HORARIO').props.onPress());
  assert.deepEqual(destination, ['Schedule', { gameId: 'p1' }]);
});

test('la sala usa el singular con un único inscrito', async () => {
  await mount(waitingRoom([{ id: 'i1', alias: 'ana', esMaster: true }]), { navigation: {}, route: { params: { gameId: 'p1', game } } });
  assert.match(shown(), /1 inscrito/);
  assert.doesNotMatch(shown(), /1 inscritos/);
});

test('el máster que juega ve «Arrancar ya» en su sala', async () => {
  await mount(waitingRoom([{ id: 'i1', alias: 'ana', esMaster: true }], true), { navigation: {}, route: { params: { gameId: 'p1', game } } });
  assert.match(shown(), /Eres el máster/);
  assert.notEqual(button('ARRANCAR YA'), undefined);
  assert.notEqual(button('REVISAR MI HORARIO'), undefined);
});

test('el máster que no juega no ve «Revisar mi horario»', async () => {
  await mount(waitingRoom([{ id: 'i2', alias: 'bea', esMaster: false }], true), { navigation: {}, route: { params: { gameId: 'p1', game } } });
  assert.match(shown(), /Eres el máster/);
  assert.equal(button('REVISAR MI HORARIO'), undefined);
});

test('volver desde la sala abre Partidas con la partida ya disponible', async () => {
  let reset;
  await mount(waitingRoom([{ id: 'i1', alias: 'ana', esMaster: true }], true), {
    navigation: { reset: (value) => { reset = value; } }, route: { params: { gameId: 'p1', game } },
  });
  await act(async () => tree.root.findByProps({ accessibilityLabel: 'Volver a mis partidas' }).props.onPress());
  assert.equal(reset.routes[0].name, 'Home');
  assert.equal(reset.routes[0].params.createdGame.id, 'p1');
  assert.equal(reset.routes[0].params.createdGame.soyMaster, true);
});

test('arrancar ya abre la partida iniciada y avisa si faltan jugadores', async () => {
  const players = [{ id: 'i1', alias: 'ana', esMaster: true }, { id: 'i2', alias: 'bea', esMaster: false }, { id: 'i3', alias: 'cora', esMaster: false }];
  let destination;
  const WaitingRoomScreen = load('screens/WaitingRoomScreen.tsx', { ...common,
    '../api/inscription': { getPlayers: async () => players },
    '../api/game': {
      getMyGames: async () => [{ id: 'p1', ...game, soyMaster: true }],
      startGame: async () => ({ id: 'p1', ...game, estado: 'EN_JUEGO', soyMaster: true }),
    },
  }).WaitingRoomScreen;
  await mount(WaitingRoomScreen, { navigation: { reset: (state) => { destination = state; } }, route: { params: { gameId: 'p1', game } } });
  await act(async () => button('ARRANCAR YA').props.onPress());
  assert.deepEqual(destination.routes.map((route) => route.name), ['Home', 'GameStarted']);
  assert.equal(destination.index, 1);
  assert.equal(destination.routes[1].params.game.estado, 'EN_JUEGO');
});

test('la cuenta atrás de la sala va en segundos, centrada, y corre cada segundo', async () => {
  const start = Date.parse(game.inicioEn);
  mock.timers.enable({ apis: ['setInterval', 'Date'], now: start - ((((1 * 24 + 23) * 60 + 58) * 60 + 12) * 1000) });
  await mount(waitingRoom([{ id: 'i1', alias: 'ana', esMaster: false }]), { navigation: {}, route: { params: { gameId: 'p1', game } } });
  const countdown = () => tree.root.findAll((node) => node.type === 'Text' && /\bS$/.test(String(node.props.children)))[0];
  assert.equal(countdown().props.children, '1 D 23 H 58 MIN 12 S');
  assert.equal(countdown().props.numberOfLines, 1);
  assert.ok(countdown().props.style.some((style) => style.textAlign === 'center'));
  await act(async () => mock.timers.tick(1000));
  assert.equal(countdown().props.children, '1 D 23 H 58 MIN 11 S');
  await act(async () => mock.timers.setTime(start + 5000));
  await act(async () => mock.timers.tick(1000));
  assert.equal(countdown().props.children, '0 D 00 H 00 MIN 00 S');
});

test('con menos de 3 inscritos «Arrancar ya» está desactivado', async () => {
  await mount(waitingRoom([{ id: 'i1', alias: 'ana', esMaster: true }, { id: 'i2', alias: 'bea', esMaster: false }], true), { navigation: {}, route: { params: { gameId: 'p1', game } } });
  assert.equal(button('ARRANCAR YA').props.disabled, true);
});

test('la sala pasa a «Partida cancelada» si cancelan la partida con el inscrito dentro', async () => {
  mock.timers.enable({ apis: ['setInterval'] });
  let estado = 'EN_INSCRIPCION';
  let destination;
  const WaitingRoomScreen = load('screens/WaitingRoomScreen.tsx', { ...common,
    '../api/inscription': { getPlayers: async () => [{ id: 'i1', alias: 'ana', esMaster: false }] },
    '../api/game': { getMyGames: async () => [{ id: 'p1', ...game, estado, soyMaster: false, motivoCancelacion: estado === 'CANCELADA' ? 'POCOS_JUGADORES' : null }], startGame: async () => undefined },
  }).WaitingRoomScreen;
  await mount(WaitingRoomScreen, { navigation: { replace: (...args) => { destination = args; } }, route: { params: { gameId: 'p1', game } } });
  assert.equal(destination, undefined);
  estado = 'CANCELADA';
  await act(async () => mock.timers.tick(10_000));
  assert.equal(destination[0], 'GameCancelled');
  assert.equal(destination[1].game.motivoCancelacion, 'POCOS_JUGADORES');
});

test('la sala se recarga cada 10 s y abre la partida cuando arranca', async () => {
  mock.timers.enable({ apis: ['setInterval'] });
  let estado = 'EN_INSCRIPCION';
  let loads = 0;
  let destination;
  const WaitingRoomScreen = load('screens/WaitingRoomScreen.tsx', { ...common,
    '../api/inscription': { getPlayers: async () => { loads += 1; return [{ id: 'i1', alias: 'ana', esMaster: false }]; } },
    '../api/game': { getMyGames: async () => [{ id: 'p1', ...game, estado, soyMaster: false, motivoCancelacion: null }], startGame: async () => undefined },
  }).WaitingRoomScreen;
  await mount(WaitingRoomScreen, { navigation: { reset: (state) => { destination = state; } }, route: { params: { gameId: 'p1', game } } });
  assert.equal(loads, 1);
  await act(async () => mock.timers.tick(9_999));
  assert.equal(loads, 1);
  estado = 'EN_JUEGO';
  await act(async () => mock.timers.tick(1));
  assert.equal(loads, 2);
  assert.deepEqual(destination.routes.map((route) => route.name), ['Home', 'GameStarted']);
});

test('sin el foco la sala no se recarga', async () => {
  mock.timers.enable({ apis: ['setInterval'] });
  focus.value = false;
  let loads = 0;
  const WaitingRoomScreen = load('screens/WaitingRoomScreen.tsx', { ...common,
    '../api/inscription': { getPlayers: async () => { loads += 1; return []; } },
    '../api/game': { getMyGames: async () => [{ id: 'p1', ...game, soyMaster: false, motivoCancelacion: null }], startGame: async () => undefined },
  }).WaitingRoomScreen;
  await mount(WaitingRoomScreen, { navigation: {}, route: { params: { gameId: 'p1', game } } });
  await act(async () => mock.timers.tick(30_000));
  assert.equal(loads, 0);
});

function cancelled() {
  return load('screens/GameCancelledScreen.tsx', common).GameCancelledScreen;
}

test('cancelada por falta de jugadores muestra los inscritos y los que hacen falta (f4-cancelada)', async () => {
  await mount(cancelled(), { navigation: {}, route: { params: { game: { nombre: 'Partida EINA', numInscritos: 2, motivoCancelacion: 'POCOS_JUGADORES' } } } });
  const text = shown();
  for (const expected of ['NO HAY JUGADORES', 'SUFICIENTES', 'Partida EINA', '"2"', 'inscritos', '"3"', 'hacen falta']) assert.ok(text.includes(expected), expected);
  assert.doesNotMatch(text, /borrado su cuenta/);
});

test('cancelada porque el máster borró su cuenta lo dice', async () => {
  await mount(cancelled(), { navigation: {}, route: { params: { game: { nombre: 'Partida EINA', numInscritos: 4, motivoCancelacion: 'MASTER_SIN_CUENTA' } } } });
  assert.match(shown(), /El máster ha borrado su cuenta\./);
  assert.doesNotMatch(shown(), /hacen falta/);
});

test('la partida iniciada muestra la fecha como el prototipo', async () => {
  const { GameStartedScreen } = load('screens/GameStartedScreen.tsx', common);
  await mount(GameStartedScreen, { navigation: {}, route: { params: { game: { nombre: 'Partida EINA', numInscritos: 7, inicioEn: new Date(2026, 9, 5, 9, 0).toISOString(), soyJugador: true } } } });
  assert.match(shown(), /Lun 5 oct · 9:00/);
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

test('un horario de partida no muestra el progreso del alta y explica si ya no es editable', async () => {
  const { ScheduleScreen } = load('screens/ScheduleScreen.tsx', { ...common,
    '../api/schedule': { getSchedule: async () => ({ franjas: [] }), updateSchedule: async () => undefined },
    '../api/inscription': {
      getGameSchedule: async () => ({ franjas: [] }),
      updateGameSchedule: async () => { throw new client.ApiError('La partida ya ha empezado', 409, [], 'PARTIDA_NO_EDITABLE'); },
    },
    '../components/DayScheduleModal': { DayScheduleModal: 'DayScheduleModal' },
    '../components/WeeklyScheduleGrid': { WeeklyScheduleGrid: 'WeeklyScheduleGrid', scheduleToSelectedSlots: () => new Set(), selectedSlotsToSchedule: () => ({ franjas: [] }) },
  });
  await mount(ScheduleScreen, { navigation: { canGoBack: () => true, goBack: () => {} }, route: { params: { gameId: 'p1' } } });
  assert.equal(tree.root.findAll((node) => node.props.style?.flexDirection === 'row' && node.props.style?.gap === 7).length, 0);
  await act(async () => tree.root.findByType('PrimaryButton').props.onPress());
  assert.match(shown(), /La partida ya ha empezado: tu horario queda fijo para toda la partida/);
});
