/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { afterEach, mock, test } = require('node:test');
const React = require('react');
const { act, create } = require('react-test-renderer');
const ts = require('typescript');

global.IS_REACT_ACT_ENVIRONMENT = true;

// Se usa el mismo montaje ligero que en los tests existentes: componentes reales con primitivas nativas simuladas.
function load(file, imports = {}) {
  const source = fs.readFileSync(path.join(__dirname, '../src', file), 'utf8');
  const code = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      jsx: ts.JsxEmit.ReactJSX,
      esModuleInterop: true,
    },
  }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)((id) => {
    if (id in imports) return imports[id];
    if (id.endsWith('.png')) return 1;
    return require(id);
  }, module, module.exports);
  return module.exports;
}

// El cliente HTTP y getMyGames son reales; solo se sustituye el transporte de red.
const client = load('api/client.ts');
const gameApi = load('api/game.ts', { './client': client });
const inscriptionApi = load('api/inscription.ts', { './client': client });
const gameDate = load('utils/gameDate.ts');
const serverWakeup = load('utils/serverWakeup.ts');
const common = {
  'react-native': {
    View: 'View', Text: 'Text', Pressable: 'Pressable', Image: 'Image', ScrollView: 'ScrollView',
    ActivityIndicator: 'ActivityIndicator',
    StyleSheet: { create: (styles) => styles },
    useWindowDimensions: () => ({ width: 390, height: 844 }),
  },
  '@expo/vector-icons': { Ionicons: 'Ionicons' },
  '@react-navigation/native': { useIsFocused: () => true },
  '../components/Screen': { Screen: 'Screen' },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../theme/colors': { colors: {} },
  '../theme/typography': { typography: {} },
  '../api/game': gameApi,
  '../api/inscription': inscriptionApi,
  '../api/client': client,
  '../utils/gameDate': gameDate,
  '../utils/serverWakeup': serverWakeup,
};
const game = {
  id: 'pbi4-partida', nombre: 'Partida PBI 4', estado: 'EN_INSCRIPCION',
  inicioEn: '2026-11-01T10:00:00Z', finEn: '2026-11-02T10:00:00Z',
  aliasMaster: 'ana', numInscritos: 2, soyMaster: false, motivoCancelacion: null,
};
let tree;

afterEach(async () => {
  // Desmontar libera los intervalos de la sala y evita contaminar el siguiente caso.
  if (tree) await act(async () => tree.unmount());
  tree = undefined;
  client.setAccessToken(undefined);
  mock.restoreAll();
});

async function mount(component, props) {
  await act(async () => { tree = create(React.createElement(component, props)); });
}

for (const position of ['destacada', 'otra']) {
  test(`criterio 3: al abrir Inicio el inscrito ve la partida cancelada (${position})`, async () => {
    const cancelled = { ...game, estado: 'CANCELADA', motivoCancelacion: 'POCOS_JUGADORES' };
    const calls = [];
    client.setAccessToken('token-del-inscrito');
    mock.method(global, 'fetch', async (url, options) => {
      calls.push([options.method ?? 'GET', new URL(url).pathname]);
      assert.equal(options.headers.get('Authorization'), 'Bearer token-del-inscrito');
      return Response.json(position === 'destacada' ? [cancelled]
        : [{ ...game, id: 'otra-partida', nombre: 'Otra partida' }, cancelled]);
    });
    let destination;
    const { HomeScreen } = load('screens/HomeScreen.tsx', common);
    // Se parte de una copia antigua: Inicio tiene que mostrar el estado recibido del servidor.
    await mount(HomeScreen, {
      navigation: { navigate: (...args) => { destination = args; } },
      route: { params: { createdGame: game } },
    });
    assert.deepEqual(calls, [['GET', '/api/v1/cuentas/yo/partidas']]);
    const card = tree.root.findAllByType('Pressable').find((node) =>
      node.findAllByType('Text').some((text) =>
        text.props.children === (position === 'destacada' ? game.nombre.toUpperCase() : game.nombre)));
    assert.ok(card, 'La partida del inscrito sigue visible');
    assert.ok(card.findAllByType('Text').some((text) => text.props.children === 'Cancelada'));
    await act(async () => card.props.onPress());
    assert.deepEqual(destination, ['GameCancelled', { game: cancelled }]);

    // También se comprueba que el motivo llega a la pantalla, sin confundirlo con el borrado del máster.
    await act(async () => tree.unmount());
    tree = undefined;
    const { GameCancelledScreen } = load('screens/GameCancelledScreen.tsx', common);
    await mount(GameCancelledScreen, { navigation: {}, route: { params: destination[1] } });
    const shown = JSON.stringify(tree.toJSON());
    assert.match(shown, /NO HAY JUGADORES/);
    assert.match(shown, /SUFICIENTES/);
    assert.doesNotMatch(shown, /borrado su cuenta/);
  });
}

test('criterio 1: el inscrito ve el arranque al consultar la sala', async () => {
  const started = { ...game, estado: 'EN_JUEGO', numInscritos: 3 };
  const calls = [];
  mock.method(global, 'fetch', async (url) => {
    const route = new URL(url).pathname;
    calls.push(route);
    if (route === '/api/v1/cuentas/yo/partidas') return Response.json([started]);
    assert.equal(route, `/api/v1/partidas/${game.id}/inscritos`);
    return Response.json([
      { id: 'i1', alias: 'ana', esMaster: true },
      { id: 'i2', alias: 'bea', esMaster: false },
      { id: 'i3', alias: 'cora', esMaster: false },
    ]);
  });
  let destination;
  const { WaitingRoomScreen } = load('screens/WaitingRoomScreen.tsx', common);
  await mount(WaitingRoomScreen, {
    navigation: { replace: (...args) => { destination = args; } },
    route: { params: { gameId: game.id, game } },
  });
  assert.deepEqual(new Set(calls), new Set([
    '/api/v1/cuentas/yo/partidas', `/api/v1/partidas/${game.id}/inscritos`,
  ]));
  assert.deepEqual(destination, ['GameStarted', { game: started }]);
});
