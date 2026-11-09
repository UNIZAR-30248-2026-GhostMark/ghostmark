/* global __dirname */
// 7a.5: «Te han cazado», carta del espectador y del último vivo, giro tras el acierto y feed.
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

class ApiError extends Error { constructor(message, status = 400, code) { super(message); this.status = status; this.code = code; } }
class AnimatedValue { constructor(value) { this.value = value; } interpolate() { return this; } }
const native = {
  ActivityIndicator: 'ActivityIndicator', Image: 'Image', Pressable: 'Pressable', ScrollView: 'ScrollView', Text: 'Text', View: 'View',
  Animated: { Value: AnimatedValue, View: 'AnimatedView', timing: (value, options) => ({ start: () => { value.value = options.toValue; } }) },
  StyleSheet: { create: (styles) => styles, absoluteFill: {}, absoluteFillObject: {} },
};
const svg = (name) => function SvgMock(props) { return React.createElement(name, props); };
const common = {
  'react-native': native,
  'react-native-safe-area-context': { SafeAreaView: 'SafeAreaView' },
  'react-native-svg': { __esModule: true, default: svg('Svg'), G: svg('G'), Line: svg('Line') },
  '@react-navigation/native': { useFocusEffect: (effect) => React.useEffect(effect, [effect]) },
  '../api/client': { ApiError },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../components/Screen': { Screen: 'Screen' },
  '../theme/bangers': { bangers: (fontSize) => ({ fontFamily: 'Bangers', fontSize }) },
  '../theme/colors': { colors: { background: '#000', border: '#333', coral: '#f55', cream: '#fff', danger: '#f55', ink: '#111', lime: '#bf3', surfaceRaised: '#222', text: '#eee', textMuted: '#aaa', white: '#fff' } },
};

let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; });
const mount = async (component, props) => act(async () => { tree = create(React.createElement(component, props)); });
const texts = () => tree.root.findAllByType('Text').map((node) => [node.props.children].flat().join('')).join(' | ');
const hasText = (text) => tree.root.findAllByType('Text').some((node) => [node.props.children].flat().join('') === text);
const byLabel = (label) => tree.root.findAll((node) => node.props.accessibilityLabel === label && typeof node.props.onPress === 'function')[0];
const buttonWith = (text) => tree.root.findAllByType('Pressable').find((node) => node.findAllByType('Text').some((child) => child.props.children === text));
const flush = () => act(async () => { await new Promise((resolve) => setImmediate(resolve)); });

function navigationMock() {
  return {
    destinations: [], params: [], back: 0,
    navigate(...args) { this.destinations.push(args); },
    replace(...args) { this.destinations.push(['replace', ...args]); },
    setParams(params) { this.params.push(params); },
    goBack() { this.back += 1; },
  };
}

// ---------- utils/feedTime ----------

const { feedText, feedTime } = load('utils/feedTime.ts');
const now = new Date(2026, 10, 4, 12, 0);

test('la hora del feed sigue f-feed: ahora, minutos, horas, ayer y fecha', () => {
  const at = (...args) => new Date(2026, ...args).toISOString();
  assert.equal(feedTime(at(10, 4, 11, 59, 30), now), 'ahora');
  assert.equal(feedTime(at(10, 4, 11, 55), now), 'hace 5 min');
  assert.equal(feedTime(at(10, 4, 11, 1), now), 'hace 59 min');
  assert.equal(feedTime(at(10, 4, 11, 0), now), 'hace 1 h');
  assert.equal(feedTime(at(10, 4, 0, 30), now), 'hace 11 h');
  assert.equal(feedTime(at(10, 3, 18, 40), now), 'ayer, 18:40');
  assert.equal(feedTime(at(10, 3, 9, 5), now), 'ayer, 9:05');
  assert.equal(feedTime(at(10, 1, 18, 40), now), '1 nov, 18:40');
  assert.equal(feedTime(at(8, 30, 23, 59), now), '30 sept, 23:59');
});

test('el texto del feed nombra solo al eliminado y cubre la cuenta borrada (R11-28)', () => {
  assert.equal(feedText('Pablo'), 'Pablo fue eliminado');
  assert.equal(feedText(null), 'Una cuenta eliminada fue eliminada');
});

// ---------- utils/caughtSeen ----------

test('«Te han cazado» se recuerda por partida y un fallo del almacén no rompe nada', async () => {
  const store = new Map();
  let failing = false;
  const secureStore = {
    getItemAsync: async (key) => { if (failing) throw new Error('sin almacén'); return store.get(key) ?? null; },
    setItemAsync: async (key, value) => { if (failing) throw new Error('sin almacén'); store.set(key, value); },
  };
  const { hasSeenCaught, markCaughtSeen } = load('utils/caughtSeen.ts', { 'expo-secure-store': secureStore });
  assert.equal(await hasSeenCaught('p1'), false);
  await markCaughtSeen('p1');
  assert.equal(await hasSeenCaught('p1'), true);
  assert.equal(await hasSeenCaught('p2'), false, 'cada partida tiene su marca');
  await markCaughtSeen('a/b c');
  assert.deepEqual([...store.keys()], ['cazado-visto-p1', 'cazado-visto-a_b_c'], 'la clave solo lleva caracteres válidos de SecureStore');
  failing = true;
  assert.equal(await hasSeenCaught('p1'), false);
  await markCaughtSeen('p1');
});

// ---------- CaughtScreen ----------

test('«Te han cazado» queda vista al abrirse, no ofrece disputa y CERRAR vuelve a la carta', async () => {
  const marked = [];
  const { CaughtScreen } = load('screens/CaughtScreen.tsx', { ...common, '../utils/caughtSeen': { markCaughtSeen: async (gameId) => { marked.push(gameId); } } });
  const navigation = navigationMock();
  await mount(CaughtScreen, { navigation, route: { params: { gameId: 'p1' } } });
  assert.deepEqual(marked, ['p1']);
  assert.equal(hasText('TE HAN\nCAZADO'), true);
  assert.doesNotMatch(texts(), /disputa/i);
  await act(async () => buttonWith('CERRAR').props.onPress());
  await flush();
  assert.deepEqual(marked, ['p1', 'p1']);
  assert.equal(navigation.back, 1);
});

test('el contorno del rótulo queda oculto al lector de pantalla', async () => {
  const { CaughtScreen } = load('screens/CaughtScreen.tsx', { ...common, '../utils/caughtSeen': { markCaughtSeen: async () => {} } });
  await mount(CaughtScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1' } } });
  const titles = tree.root.findAllByType('Text').filter((node) => node.props.children.join?.('') === 'TE HAN\nCAZADO');
  assert.equal(titles.length, 10);
  assert.equal(titles.filter((node) => node.props.accessibilityElementsHidden && node.props.importantForAccessibility === 'no-hide-descendants').length, 9);
});

// ---------- MissionCardScreen ----------

function missionCard(card, { seen = false } = {}) {
  const asked = [];
  const { MissionCardScreen } = load('screens/MissionCardScreen.tsx', { ...common,
    '../api/game': { getMyMissionCard: async () => (typeof card === 'function' ? card() : card), getMyGames: async () => [] },
    '../utils/caughtSeen': { hasSeenCaught: async (gameId) => { asked.push(gameId); return seen; } },
  });
  return { MissionCardScreen, asked };
}

const eliminated = { estado: 'ELIMINADO', aliasObjetivo: null, mision: null, eliminaciones: 1 };

test('el eliminado que no lo ha visto va a «Te han cazado» y debajo queda la carta de espectador', async () => {
  const { MissionCardScreen, asked } = missionCard(eliminated);
  const navigation = navigationMock();
  await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p1' } } });
  assert.deepEqual(asked, ['p1']);
  assert.deepEqual(navigation.destinations, [['Caught', { gameId: 'p1' }]]);
  assert.equal(hasText('ERES ESPECTADOR'), true);
});

test('el espectador no ve «Mi código», «Eliminar» ni el objetivo (H3 de la 7a.4)', async () => {
  const { MissionCardScreen } = missionCard(eliminated, { seen: true });
  const navigation = navigationMock();
  await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p1' } } });
  assert.deepEqual(navigation.destinations, [], 'ya lo vio: no vuelve a salir');
  assert.equal(hasText('ERES ESPECTADOR'), true);
  assert.equal(hasText('Sigue la partida en Feed y en Partida hasta el final.'), true);
  assert.equal(buttonWith('MI CÓDIGO'), undefined);
  assert.equal(buttonWith('ELIMINAR'), undefined);
  assert.equal(hasText('Tu objetivo'), false);
  assert.equal(byLabel('Carta de misión') === undefined, true, 'la carta del espectador no se gira');
});

test('el jugador vivo no consulta la marca de «Te han cazado»', async () => {
  const { MissionCardScreen, asked } = missionCard({ estado: 'VIVO', aliasObjetivo: 'Pablo', mision: 'Di compilador', eliminaciones: 0 });
  const navigation = navigationMock();
  await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p1' } } });
  assert.deepEqual(asked, []);
  assert.deepEqual(navigation.destinations, []);
  assert.equal(buttonWith('ELIMINAR') !== undefined, true);
  assert.equal(hasText('ERES ESPECTADOR'), false);
});

test('el último vivo no tiene a quién eliminar, pero conserva su código (R11-26)', async () => {
  const { MissionCardScreen } = missionCard({ estado: 'VIVO', aliasObjetivo: null, mision: null, eliminaciones: 3 });
  await mount(MissionCardScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1' } } });
  assert.equal(hasText('Ya no tienes objetivo: eres la última persona en juego'), true);
  assert.equal(buttonWith('ELIMINAR'), undefined);
  assert.equal(buttonWith('MI CÓDIGO') !== undefined, true);
  assert.doesNotMatch(texts(), /CUENTA ELIMINADA|SIN MISIÓN/);
});

test('un objetivo que borró su cuenta no se confunde con ser el último vivo', async () => {
  const { MissionCardScreen } = missionCard({ estado: 'VIVO', aliasObjetivo: null, mision: 'Di compilador', eliminaciones: 0 });
  await mount(MissionCardScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1' } } });
  assert.equal(hasText('CUENTA ELIMINADA'), true);
  assert.equal(buttonWith('ELIMINAR') !== undefined, true);
});

test('la pestaña Feed de la carta abre el feed de esa partida como jugador', async () => {
  const { MissionCardScreen } = missionCard({ estado: 'VIVO', aliasObjetivo: 'Pablo', mision: 'Di compilador', eliminaciones: 0 });
  const navigation = navigationMock();
  await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p7' } } });
  await act(async () => byLabel('Ir al feed').props.onPress());
  assert.deepEqual(navigation.destinations, [['Feed', { gameId: 'p7', isPlayer: true }]]);
});

test('el espectador también llega al feed desde su carta', async () => {
  const { MissionCardScreen } = missionCard(eliminated, { seen: true });
  const navigation = navigationMock();
  await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p7' } } });
  await act(async () => byLabel('Ir al feed').props.onPress());
  assert.deepEqual(navigation.destinations, [['Feed', { gameId: 'p7', isPlayer: true }]]);
});

const flipValue = () => tree.root.findAllByType('AnimatedView')[1].props.style[1].opacity.value;

test('tras el acierto, la carta recargada enseña lo heredado, gira sola una vez y vuelve boca abajo (UI11)', async () => {
  mock.timers.enable({ apis: ['setTimeout'] });
  try {
    let current = { estado: 'VIVO', aliasObjetivo: 'Marta', mision: 'Saluda', eliminaciones: 1 };
    const { MissionCardScreen } = missionCard(() => current);
    const navigation = navigationMock();
    await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p1', revealOnce: true } } });
    assert.deepEqual(navigation.params, [{ revealOnce: false }], 'el giro no se repite al volver a la carta');
    assert.equal(flipValue(), 1);
    const shown = JSON.stringify(tree.toJSON());
    assert.equal(shown.includes('MARTA') && shown.includes('SALUDA'), true, 'la carta enseña el objetivo y la misión heredados');
    await act(async () => mock.timers.tick(1599));
    assert.equal(flipValue(), 1);
    await act(async () => mock.timers.tick(1));
    assert.equal(flipValue(), 0);
    current = { ...current, aliasObjetivo: 'Inés' };
    await act(async () => tree.update(React.createElement(MissionCardScreen, { navigation, route: { params: { gameId: 'p1', revealOnce: false } } })));
    assert.equal(flipValue(), 0);
  } finally {
    mock.timers.reset();
  }
});

test('sin acierto la carta no gira sola', async () => {
  const { MissionCardScreen } = missionCard({ estado: 'VIVO', aliasObjetivo: 'Marta', mision: 'Saluda', eliminaciones: 0 });
  const navigation = navigationMock();
  await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p1' } } });
  assert.equal(flipValue(), 0);
  assert.deepEqual(navigation.params, []);
});

test('el último vivo que acaba de acertar no gira una carta vacía', async () => {
  const { MissionCardScreen } = missionCard({ estado: 'VIVO', aliasObjetivo: null, mision: null, eliminaciones: 2 });
  const navigation = navigationMock();
  await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p1', revealOnce: true } } });
  assert.equal(tree.root.findAllByType('AnimatedView').length, 0);
  assert.equal(hasText('Ya no tienes objetivo: eres la última persona en juego'), true);
});

// ---------- FeedScreen ----------

function feed(getFeed) {
  const calls = [];
  const { FeedScreen } = load('screens/FeedScreen.tsx', { ...common,
    '../api/game': { getFeed: async (gameId) => { calls.push(gameId); return getFeed(calls.length); } },
    '../utils/feedTime': { feedText, feedTime: (iso) => `hora ${iso}` },
  });
  return { FeedScreen, calls };
}

test('el feed enseña cada eliminación con su hora y nunca quién la hizo', async () => {
  const { FeedScreen, calls } = feed(() => [
    { tipo: 'ELIMINACION', alias: 'Pablo', ocurridoEn: '2026-11-04T11:55:00Z' },
    { tipo: 'ELIMINACION', alias: null, ocurridoEn: '2026-11-03T17:40:00Z' },
  ]);
  await mount(FeedScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1', isPlayer: true } } });
  assert.deepEqual(calls, ['p1']);
  assert.equal(hasText('Pablo fue eliminado'), true);
  assert.equal(hasText('hora 2026-11-04T11:55:00Z'), true);
  assert.equal(hasText('Una cuenta eliminada fue eliminada'), true);
  const shown = texts();
  assert.ok(shown.indexOf('Pablo fue eliminado') < shown.indexOf('Una cuenta eliminada'), 'mantiene el orden del servidor');
  assert.doesNotMatch(shown, /eliminó a Pablo|cazó|por /);
});

test('el pie del feed no promete el Wrapped (R11-27)', async () => {
  const { FeedScreen } = feed(() => []);
  await mount(FeedScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1', isPlayer: true } } });
  assert.equal(hasText('El feed nunca dice quién eliminó a quién.'), true);
  assert.doesNotMatch(texts(), /Wrapped/);
});

test('sin eliminaciones el feed lo dice en vez de quedarse en blanco', async () => {
  const { FeedScreen } = feed(() => []);
  await mount(FeedScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1', isPlayer: true } } });
  assert.equal(hasText('Aún no hay eliminaciones.'), true);
  assert.equal(hasText('CARGANDO FEED…'), false);
});

test('si el feed falla se puede reintentar y la segunda vez carga', async () => {
  const { FeedScreen, calls } = feed((call) => { if (call === 1) throw new ApiError('La partida no está en juego', 409, 'PARTIDA_NO_EN_JUEGO'); return [{ tipo: 'ELIMINACION', alias: 'Pablo', ocurridoEn: '2026-11-04T11:55:00Z' }]; });
  await mount(FeedScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1', isPlayer: true } } });
  assert.equal(hasText('La partida no está en juego'), true);
  await act(async () => tree.root.findByType('PrimaryButton').props.onPress());
  await flush();
  assert.equal(calls.length, 2);
  assert.equal(hasText('Pablo fue eliminado'), true);
  assert.equal(hasText('La partida no está en juego'), false);
});

test('un error sin código del servidor da un mensaje de conexión', async () => {
  const { FeedScreen } = feed(() => { throw new TypeError('Network request failed'); });
  await mount(FeedScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1', isPlayer: true } } });
  assert.match(texts(), /No hemos podido cargar el feed/);
});

test('las pestañas del feed llevan a la carta y a Partida de la misma partida', async () => {
  const { FeedScreen } = feed(() => []);
  const navigation = navigationMock();
  await mount(FeedScreen, { navigation, route: { params: { gameId: 'p7', isPlayer: true } } });
  await act(async () => byLabel('Ir a mi misión').props.onPress());
  await act(async () => byLabel('Ir a la pestaña Partida').props.onPress());
  assert.deepEqual(navigation.destinations, [['MissionCard', { gameId: 'p7' }], ['GameTab', { gameId: 'p7' }]]);
});

test('el máster que no juega no puede ir a una carta que no tiene', async () => {
  const { FeedScreen } = feed(() => []);
  await mount(FeedScreen, { navigation: navigationMock(), route: { params: { gameId: 'p7', isPlayer: false } } });
  assert.equal(byLabel('Ir a mi misión') === undefined, true);
  assert.equal(hasText('Misión'), true);
  assert.equal(byLabel('Ir a la pestaña Partida') !== undefined, true);
});
