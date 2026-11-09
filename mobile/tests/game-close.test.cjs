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

class ApiError extends Error { constructor(message, status = 400, code) { super(message); this.status = status; this.code = code; this.fieldErrors = []; } }
class AnimatedValue { constructor(value) { this.value = value; } interpolate() { return this; } }
let alerts = [];
const native = {
  ActivityIndicator: 'ActivityIndicator', Image: 'Image', Pressable: 'Pressable', ScrollView: 'ScrollView', Text: 'Text', View: 'View',
  Alert: { alert: (...args) => alerts.push(args) },
  Animated: { Value: AnimatedValue, View: 'AnimatedView', timing: () => ({ start() {} }) },
  StyleSheet: { create: (styles) => styles, absoluteFillObject: {} },
};
const gameDate = load('utils/gameDate.ts');
const gameResult = load('utils/gameResult.ts');
const common = {
  'react-native': native,
  '@expo/vector-icons': { Ionicons: 'Ionicons' },
  '@react-navigation/native': { useFocusEffect: (effect) => React.useEffect(effect, [effect]) },
  '../api/client': { ApiError },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../utils/caughtSeen': { hasSeenCaught: async () => true },
  '../components/Screen': { Screen: 'Screen' },
  '../theme/bangers': { bangers: () => ({}) },
  '../theme/colors': { colors: {} },
  '../utils/gameDate': gameDate,
  '../utils/gameResult': gameResult,
  '../utils/serverWakeup': { serverWakeupMessage: '', useServerWakeupNotice: () => false },
};

const result = (tipo, aliasGanadores, eliminaciones, motivo = 'FECHA_FIN', cerradaEn = '2026-11-10T21:00:00+01:00') => ({ tipo, motivo, aliasGanadores, eliminaciones, cerradaEn });
const inPlay = { id: 'p1', nombre: 'Killer EINA', estado: 'EN_JUEGO', inicioEn: '2026-11-05T09:00:00+01:00', finEn: '2026-11-10T21:00:00+01:00', numInscritos: 7, soyMaster: true, soyJugador: false, motivoCancelacion: null, resultado: null };
const closedGame = (resultado) => ({ ...inPlay, estado: 'CERRADA', soyMaster: false, soyJugador: true, resultado });

let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; alerts = []; });
const shown = () => JSON.stringify(tree.toJSON());
const texts = () => tree.root.findAllByType('Text').map((node) => [node.props.children].flat().join('')).join(' | ');
const byLabel = (label) => tree.root.findAll((node) => node.props.accessibilityLabel === label && typeof node.props.onPress === 'function')[0];
const mount = async (component, props) => act(async () => { tree = create(React.createElement(component, props)); });

test('los textos del resultado son los de R11-6, con «cuenta eliminada» y «e» delante de «i»', () => {
  const { resultParts, resultSentence } = gameResult;
  assert.deepEqual(resultParts(result('GANADOR', ['Marta'], 4)), { title: 'Gana Marta', detail: 'con 4 eliminaciones' });
  assert.equal(resultSentence(result('GANADOR', ['Marta'], 1)), 'Gana Marta con 1 eliminación');
  assert.deepEqual(resultParts(result('SUPERVIVIENTE', ['Álex'], 2, 'ULTIMO_SUPERVIVIENTE')), { title: 'Álex', detail: 'último superviviente' });
  assert.equal(resultSentence(result('SUPERVIVIENTE', ['Álex'], 2, 'ULTIMO_SUPERVIVIENTE')), 'Álex, último superviviente');
  assert.equal(resultSentence(result('EMPATE', ['Marta', null], 3)), 'Empate entre Marta y cuenta eliminada');
  assert.equal(resultSentence(result('EMPATE', ['Marta', 'Inés'], 3)), 'Empate entre Marta e Inés');
  assert.deepEqual(resultParts(result('EMPATE', ['Marta', 'Sara', 'Hielo'], 3)), { title: 'Empate', detail: 'entre Marta, Sara y Hielo' });
});

test('la pantalla de fin dice quién gana, la duración y vuelve a Inicio', async () => {
  const { GameEndedScreen } = load('screens/GameEndedScreen.tsx', common);
  const resets = [];
  await mount(GameEndedScreen, { navigation: { reset: (value) => resets.push(value) }, route: { params: { game: closedGame(result('GANADOR', ['Marta'], 4)) } } });
  assert.match(texts(), /GANA MARTA \| con 4 eliminaciones/);
  assert.match(texts(), /Killer EINA · 7 jugadores · 6 días/);
  await act(async () => tree.root.findByType('Pressable').props.onPress());
  assert.deepEqual(resets, [{ index: 0, routes: [{ name: 'Home' }] }]);
});

test('la pantalla de fin usa la hora real del cierre y no el fin previsto', async () => {
  const { GameEndedScreen } = load('screens/GameEndedScreen.tsx', common);
  const closedAt = '2026-11-07T18:00:00+01:00';
  const game = {
    ...closedGame(result('GANADOR', ['Marta'], 1, 'MASTER', closedAt)),
    inicioEn: '2026-11-07T10:19:00+01:00',
  };
  await mount(GameEndedScreen, { navigation: { reset() {} }, route: { params: { game } } });
  assert.match(texts(), new RegExp(gameDate.formatGameDate(closedAt)));
  assert.match(texts(), /Killer EINA · 7 jugadores · 1 día/);
});

test('la pantalla de fin dice el empate y el último superviviente', async () => {
  const { GameEndedScreen } = load('screens/GameEndedScreen.tsx', common);
  for (const [resultado, expected] of [[result('EMPATE', ['Marta', null], 3), /EMPATE \| entre Marta y cuenta eliminada/], [result('SUPERVIVIENTE', ['Álex'], 2, 'ULTIMO_SUPERVIVIENTE'), /ÁLEX \| último superviviente/]]) {
    await mount(GameEndedScreen, { navigation: { reset() {} }, route: { params: { game: closedGame(resultado) } } });
    assert.match(texts(), expected);
    await act(async () => tree.unmount()); tree = undefined;
  }
});

function gameTab({ games = [inPlay], players, close = async () => closedGame(result('GANADOR', ['Marta'], 4, 'MASTER')) } = {}) {
  const calls = [];
  const navigation = { destinations: [], replaced: [], navigate(...args) { this.destinations.push(args); }, replace(...args) { this.replaced.push(args); } };
  const { GameTabScreen } = load('screens/GameTabScreen.tsx', { ...common,
    '../api/auth': { getCurrentAccount: () => ({ alias: 'fantasmilla' }) },
    '../api/game': { getMyGames: async () => games, closeGame: (...args) => { calls.push(args); return close(...args); } },
    '../api/inscription': { getPlayers: async () => players ?? [{ id: 'i1', alias: 'fantasmilla', esMaster: true }, { id: 'i2', alias: 'Marta', esMaster: false }] },
  });
  return { GameTabScreen, navigation, calls };
}

test('el máster cierra desde la pestaña Partida tras confirmar y se abre el fin (R11-24)', async () => {
  const { GameTabScreen, navigation, calls } = gameTab();
  await mount(GameTabScreen, { navigation, route: { params: { gameId: 'p1' } } });
  assert.match(shown(), /TAREAS DE MÁSTER/);
  await act(async () => byLabel('Cerrar partida').props.onPress());
  assert.equal(calls.length, 0, 'no cierra sin confirmar');
  const [title, message, buttons] = alerts[0];
  assert.equal(title, '¿Cerrar la partida?');
  assert.match(message, /no se puede deshacer/);
  await act(async () => buttons.find((button) => button.style === 'cancel').onPress?.());
  assert.equal(calls.length, 0, 'cancelar no cierra');
  await act(async () => buttons.find((button) => button.style === 'destructive').onPress());
  assert.deepEqual(calls, [['p1']]);
  assert.deepEqual(navigation.replaced, [['GameEnded', { game: closedGame(result('GANADOR', ['Marta'], 4, 'MASTER')) }]]);
});

test('mientras se cierra, «Cerrar partida» no se puede volver a pulsar', async () => {
  let finish;
  const { GameTabScreen, navigation, calls } = gameTab({ close: () => new Promise((resolve) => { finish = resolve; }) });
  await mount(GameTabScreen, { navigation, route: { params: { gameId: 'p1' } } });
  await act(async () => byLabel('Cerrar partida').props.onPress());
  await act(async () => alerts[0][2].find((button) => button.style === 'destructive').onPress());
  const button = tree.root.findAll((node) => node.props.accessibilityLabel === 'Cerrar partida' && node.type === 'Pressable')[0];
  assert.equal(button.props.disabled, true);
  assert.match(shown(), /CERRANDO…/);
  await act(async () => finish(closedGame(result('GANADOR', ['Marta'], 4, 'MASTER'))));
  assert.equal(calls.length, 1);
});

test('un jugador que no es máster no ve las tareas ni «Cerrar partida» y vuelve a su misión', async () => {
  const { GameTabScreen, navigation } = gameTab({ games: [{ ...inPlay, soyMaster: false, soyJugador: true }] });
  await mount(GameTabScreen, { navigation, route: { params: { gameId: 'p1' } } });
  assert.doesNotMatch(shown(), /TAREAS DE MÁSTER/);
  // Se compara un booleano: con un nodo, node:assert recorre todo el árbol al fallar y agota la memoria.
  assert.equal(byLabel('Cerrar partida') === undefined, true, 'no debe verse «Cerrar partida»');
  await act(async () => byLabel('Ir a mi misión').props.onPress());
  assert.deepEqual(navigation.destinations, [['MissionCard', { gameId: 'p1' }]]);
});

test('si la partida ya está cerrada, la pestaña Partida abre el fin', async () => {
  const closed = closedGame(result('GANADOR', ['Marta'], 4));
  const { GameTabScreen, navigation } = gameTab({ games: [closed] });
  await mount(GameTabScreen, { navigation, route: { params: { gameId: 'p1' } } });
  assert.deepEqual(navigation.replaced, [['GameEnded', { game: closed }]]);
});

test('los estados y el contador de vivos solo salen cuando el servidor los manda', async () => {
  const sinEstado = gameTab();
  await mount(sinEstado.GameTabScreen, { navigation: sinEstado.navigation, route: { params: { gameId: 'p1' } } });
  assert.doesNotMatch(texts(), /vivos de|VIVO|ELIMINADO/);
  await act(async () => tree.unmount()); tree = undefined;
  const conEstado = gameTab({ players: [{ id: 'i1', alias: 'fantasmilla', esMaster: true, estado: 'VIVO' }, { id: 'i2', alias: 'Marta', esMaster: false, estado: 'VIVO' }, { id: 'i3', alias: null, esMaster: false, estado: 'ELIMINADO' }] });
  await mount(conEstado.GameTabScreen, { navigation: conEstado.navigation, route: { params: { gameId: 'p1' } } });
  assert.match(texts(), /2 vivos de 3/);
  assert.match(texts(), /Cuenta eliminada \| ELIMINADO/);
});

test('el cliente cierra con POST a la ruta del cierre del contrato', async () => {
  const calls = [];
  const { closeGame } = load('api/game.ts', { './client': { request: async (...args) => { calls.push(args); return {}; } } });
  await closeGame('p 1');
  assert.deepEqual(calls, [['/api/v1/partidas/p%201/cierre', { method: 'POST' }]]);
});

test('Inicio enseña el resultado de la cerrada y la abre en el fin', async () => {
  const closed = { ...closedGame(result('GANADOR', ['Marta'], 4)), id: 'p2', nombre: 'Killer Teleco' };
  const destinations = [];
  const { HomeScreen } = load('screens/HomeScreen.tsx', { ...common, '../api/game': { getMyGames: async () => [{ ...inPlay, soyMaster: false, soyJugador: true }, closed] } });
  await mount(HomeScreen, { navigation: { navigate: (...args) => destinations.push(args) }, route: { params: undefined } });
  assert.match(texts(), /Cerrada · Gana Marta con 4 eliminaciones/);
  const card = tree.root.findAllByType('Pressable').find((node) => node.findAllByType('Text').some((text) => text.props.children === 'Killer Teleco'));
  await act(async () => card.props.onPress());
  assert.deepEqual(destinations, [['GameEnded', { game: closed }]]);
});

test('la tarjeta grande de una partida cerrada dice cuándo se cerró realmente', async () => {
  const closedAt = '2026-11-07T18:00:00+01:00';
  const closed = {
    ...closedGame(result('GANADOR', ['Marta'], 1, 'MASTER', closedAt)),
    id: 'p3',
    inicioEn: '2026-11-07T10:19:00+01:00',
  };
  const { HomeScreen } = load('screens/HomeScreen.tsx', { ...common, '../api/game': { getMyGames: async () => [closed] } });
  await mount(HomeScreen, { navigation: { navigate() {} }, route: { params: undefined } });
  assert.match(texts(), new RegExp(`Cerrada el ${gameDate.formatGameDate(closedAt)}`));
});

function missionCard(error, games) {
  const navigation = { replaced: [], navigate() {}, replace(...args) { this.replaced.push(args); } };
  const { MissionCardScreen } = load('screens/MissionCardScreen.tsx', { ...common, '../api/game': { getMyMissionCard: async () => { throw error; }, getMyGames: async () => games } });
  return { MissionCardScreen, navigation };
}

test('si la partida se cierra con la carta abierta, la carta lleva al fin', async () => {
  const closed = closedGame(result('GANADOR', ['Marta'], 4));
  const { MissionCardScreen, navigation } = missionCard(new ApiError('La partida no está en juego', 409, 'PARTIDA_NO_EN_JUEGO'), [closed]);
  await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p1' } } });
  assert.deepEqual(navigation.replaced, [['GameEnded', { game: closed }]]);
});

test('si la partida aún no ha arrancado, la carta enseña el error', async () => {
  const { MissionCardScreen, navigation } = missionCard(new ApiError('La partida no está en juego', 409, 'PARTIDA_NO_EN_JUEGO'), [{ ...inPlay, estado: 'EN_INSCRIPCION' }]);
  await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p1' } } });
  assert.deepEqual(navigation.replaced, []);
  assert.match(texts(), /La partida no está en juego/);
});

test('la pestaña Partida de la carta abre la de esa partida', async () => {
  const navigation = { destinations: [], navigate(...args) { this.destinations.push(args); }, replace() {} };
  const { MissionCardScreen } = load('screens/MissionCardScreen.tsx', { ...common, '../api/game': { getMyMissionCard: async () => ({ aliasObjetivo: 'Pablo', mision: 'Di compilador' }), getMyGames: async () => [] } });
  await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p7' } } });
  await act(async () => byLabel('Ir a la pestaña Partida').props.onPress());
  assert.deepEqual(navigation.destinations, [['GameTab', { gameId: 'p7' }]]);
});

test('la pestaña Feed de Partida abre el feed y dice si quien mira juega', async () => {
  for (const soyJugador of [false, true]) {
    const { GameTabScreen, navigation } = gameTab({ games: [{ ...inPlay, soyJugador }] });
    await mount(GameTabScreen, { navigation, route: { params: { gameId: 'p1' } } });
    await act(async () => byLabel('Ir al feed').props.onPress());
    assert.deepEqual(navigation.destinations, [['Feed', { gameId: 'p1', isPlayer: soyJugador }]]);
    await act(async () => tree.unmount());
    tree = undefined;
  }
});
