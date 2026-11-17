/* global __dirname */
// 11a.4: abrir, responder y arbitrar una disputa, bloqueo de «Eliminar», contador y último vivo.
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
  ActivityIndicator: 'ActivityIndicator', Image: 'Image', KeyboardAvoidingView: 'KeyboardAvoidingView', Platform: { OS: 'android' },
  Pressable: 'Pressable', ScrollView: 'ScrollView', Text: 'Text', TextInput: 'TextInput', View: 'View',
  Animated: { Value: AnimatedValue, View: 'AnimatedView', timing: (value, options) => ({ start: () => { value.value = options.toValue; } }) },
  StyleSheet: { create: (styles) => styles, absoluteFill: {}, absoluteFillObject: {} },
};
const svg = (name) => function SvgMock(props) { return React.createElement(name, props); };
const dispute = load('utils/dispute.ts');
const common = {
  'react-native': native,
  'react-native-safe-area-context': { SafeAreaView: 'SafeAreaView' },
  'react-native-svg': { __esModule: true, default: svg('Svg'), G: svg('G'), Line: svg('Line') },
  '@expo/vector-icons': { Ionicons: 'Ionicons' },
  '@react-navigation/native': { useFocusEffect: (effect) => React.useEffect(effect, [effect]) },
  '../api/client': { ApiError },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../components/Screen': { Screen: 'Screen' },
  '../theme/bangers': { bangers: (fontSize) => ({ fontFamily: 'Bangers', fontSize }) },
  '../theme/colors': { colors: { background: '#000', border: '#333', coral: '#f55', cream: '#fff', danger: '#f55', ink: '#111', lime: '#bf3', surfaceRaised: '#222', text: '#eee', textMuted: '#aaa', white: '#fff' } },
  '../utils/dispute': dispute,
  '../utils/caughtSeen': { hasSeenCaught: async () => true, markCaughtSeen: async () => {} },
};

let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; mock.timers.reset(); });
const mount = async (component, props) => act(async () => { tree = create(React.createElement(component, props)); });
const textOf = (node) => [node.props.children].flat().join('');
const texts = () => tree.root.findAllByType('Text').map(textOf).join(' | ');
const hasText = (text) => tree.root.findAllByType('Text').some((node) => textOf(node) === text);
const buttonWith = (text) => tree.root.findAllByType('Pressable').find((node) => node.findAllByType('Text').some((child) => textOf(child) === text));
const flush = () => act(async () => { await new Promise((resolve) => setImmediate(resolve)); });

function navigationMock() {
  return {
    destinations: [],
    navigate(...args) { this.destinations.push(args); },
    replace(...args) { this.destinations.push(['replace', ...args]); },
    reset(...args) { this.destinations.push(['reset', ...args]); },
    setParams() {}, goBack() { this.destinations.push(['back']); }, popTo() {},
    addListener: () => () => {},
  };
}

const now = new Date(2026, 10, 17, 18, 30);
const at = (...args) => new Date(2026, ...args).toISOString();
function base(fields = {}) {
  return {
    id: 'd1', tipo: 'ELIMINACION', estado: 'ABIERTA', aliasCazador: 'Pablo', aliasVictima: 'Marta', mision: 'Pide un boli',
    textoApertura: 'No me pidió un boli.', textoRespuesta: null, abiertaEn: at(10, 17, 18, 2), plazoHasta: at(10, 18, 0, 2),
    soyArbitro: false, puedoResponder: false, resultadoZona: null, razon: null, resueltaPor: null, cerradaEn: null, ...fields,
  };
}

// ---------- api/game ----------

test('el cliente sigue el contrato de abrir, leer, responder y decidir disputas', async () => {
  const calls = [];
  const api = load('api/game.ts', { './client': { request: async (...args) => { calls.push(args); return { id: 'd1' }; } } });
  await api.getDisputes('p 1'); await api.getDispute('p 1', 'd/1'); await api.openDispute('p 1', 'ELIMINACION', 'No fue limpio');
  await api.answerDispute('p 1', 'd/1', 'Sí lo fue'); await api.decideDispute('p 1', 'd/1', 'VICTIMA');
  assert.deepEqual(calls, [
    ['/api/v1/partidas/p%201/disputas'], ['/api/v1/partidas/p%201/disputas/d%2F1'],
    ['/api/v1/partidas/p%201/disputas', { body: { tipo: 'ELIMINACION', texto: 'No fue limpio' }, method: 'POST' }],
    ['/api/v1/partidas/p%201/disputas/d%2F1/respuesta', { body: { texto: 'Sí lo fue' }, method: 'POST' }],
    ['/api/v1/partidas/p%201/disputas/d%2F1/decision', { body: { razon: 'VICTIMA' }, method: 'POST' }],
  ]);
});

// ---------- utils/dispute ----------

test('la cabecera dice qué pasó según el tipo y con la fecha real (R12-34)', () => {
  assert.equal(dispute.disputeHeader(base(), now), 'Pablo eliminó a Marta · hoy, 18:02');
  assert.equal(dispute.disputeHeader(base({ tipo: 'NEGATIVA' }), now), 'Pablo dice que Marta no le enseña el código · hoy, 18:02');
  assert.equal(dispute.disputeHeader(base({ abiertaEn: at(10, 16, 9, 5) }), now), 'Pablo eliminó a Marta · ayer, 9:05');
  assert.equal(dispute.disputeHeader(base({ abiertaEn: at(10, 12, 18, 2), aliasCazador: null }), now), 'Cuenta eliminada eliminó a Marta · 12 nov, 18:02');
});

test('la víctima habla a la izquierda y el cazador a la derecha en los dos tipos (UI19)', () => {
  assert.deepEqual(dispute.disputeBubbles(base({ textoRespuesta: 'Sí me lo pidió.' })), [
    { align: 'left', author: 'Marta · víctima', text: 'No me pidió un boli.' },
    { align: 'right', author: 'Pablo · cazador', text: 'Sí me lo pidió.' },
  ]);
  assert.deepEqual(dispute.disputeBubbles(base({ tipo: 'NEGATIVA', textoApertura: 'No me lo enseña.', textoRespuesta: 'Nunca me lo pidió.' })), [
    { align: 'right', author: 'Pablo · cazador', text: 'No me lo enseña.' },
    { align: 'left', author: 'Marta · víctima', text: 'Nunca me lo pidió.' },
  ]);
});

test('el resultado distingue quién gana, la zona y la disputa sin objeto (R12-35)', () => {
  assert.equal(dispute.disputeOutcome(base()), null);
  assert.deepEqual(dispute.disputeOutcome(base({ estado: 'RESUELTA', razon: 'CAZADOR', resueltaPor: 'MASTER' })), { title: 'GANA EL CAZADOR', detail: null });
  assert.deepEqual(dispute.disputeOutcome(base({ estado: 'RESUELTA', razon: 'VICTIMA', resueltaPor: 'ZONA' })), { title: 'GANA LA VÍCTIMA', detail: 'Estaba en una zona segura' });
  assert.deepEqual(dispute.disputeOutcome(base({ tipo: 'NEGATIVA', estado: 'SIN_OBJETO' })), { title: 'DISPUTA CERRADA: YA NO ES SU OBJETIVO', detail: null });
  assert.equal(dispute.disputeListMeta(base({ estado: 'SIN_OBJETO' })), 'CERRADA · VER DETALLE');
});

test('mientras está abierta se espera la respuesta y luego al árbitro, sea quien sea', () => {
  assert.equal(dispute.disputeWaiting(base()), 'Esperando la respuesta de la otra persona.');
  assert.equal(dispute.disputeWaiting(base({ textoRespuesta: 'Sí.' })), 'Esperando la decisión del árbitro.');
  assert.equal(dispute.disputeWaiting(base({ puedoResponder: true })), null);
  assert.equal(dispute.disputeWaiting(base({ soyArbitro: true })), null);
  assert.equal(dispute.disputeWaiting(base({ estado: 'RESUELTA', razon: 'CAZADOR' })), null);
});

// ---------- DisputeScreen ----------

function disputeScreen(value) {
  const calls = [];
  const { DisputeScreen } = load('screens/DisputeScreen.tsx', { ...common,
    '../api/game': {
      getDispute: async () => value,
      answerDispute: async (...args) => { calls.push(['respuesta', ...args]); return { ...value, textoRespuesta: args[2], puedoResponder: false }; },
      decideDispute: async (...args) => { calls.push(['decision', ...args]); return { ...value, estado: 'RESUELTA', razon: args[2], resueltaPor: 'MASTER' }; },
    },
  });
  return { DisputeScreen, calls };
}

test('el árbitro de una negativa elimina a la víctima o rechaza, sin resultado de zona', async () => {
  const { DisputeScreen, calls } = disputeScreen(base({ tipo: 'NEGATIVA', soyArbitro: true, textoApertura: 'No me lo enseña.' }));
  await mount(DisputeScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1', disputeId: 'd1' } } });
  assert.equal(buttonWith('RECHAZAR') !== undefined, true);
  assert.equal(buttonWith('CONFIRMAR ELIMINACIÓN'), undefined);
  assert.equal(hasText('Resultado de zona'), false);
  await act(async () => buttonWith('ELIMINAR A MARTA').props.onPress());
  await flush();
  assert.deepEqual(calls, [['decision', 'p1', 'd1', 'CAZADOR']]);
  assert.equal(hasText('GANA EL CAZADOR'), true);
  assert.equal(buttonWith('RECHAZAR'), undefined);
});

test('el árbitro de una eliminación ve la zona y puede anularla', async () => {
  const { DisputeScreen, calls } = disputeScreen(base({ soyArbitro: true, resultadoZona: 'FUERA' }));
  await mount(DisputeScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1', disputeId: 'd1' } } });
  assert.equal(hasText('Fuera'), true);
  assert.equal(buttonWith('CONFIRMAR ELIMINACIÓN') !== undefined, true);
  await act(async () => buttonWith('ANULAR').props.onPress());
  await flush();
  assert.deepEqual(calls, [['decision', 'p1', 'd1', 'VICTIMA']]);
  assert.equal(hasText('GANA LA VÍCTIMA'), true);
});

test('el cazador responde una vez a la disputa de su víctima y queda esperando al árbitro', async () => {
  const { DisputeScreen, calls } = disputeScreen(base({ puedoResponder: true }));
  await mount(DisputeScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1', disputeId: 'd1' } } });
  assert.equal(hasText('Pablo eliminó a Marta · ' + dispute.disputeDate(base().abiertaEn)), true);
  const input = tree.root.findAll((node) => node.props.accessibilityLabel === 'Tu respuesta')[0];
  await act(async () => input.props.onChangeText('  Sí me pidió un boli.  '));
  await act(async () => buttonWith('RESPONDER').props.onPress());
  await flush();
  assert.deepEqual(calls, [['respuesta', 'p1', 'd1', 'Sí me pidió un boli.']]);
  assert.equal(hasText('Sí me pidió un boli.'), true);
  assert.equal(hasText('Esperando la decisión del árbitro.'), true);
  assert.equal(buttonWith('RESPONDER'), undefined);
});

test('una disputa sin objeto no ofrece botones ni dice quién gana', async () => {
  const { DisputeScreen } = disputeScreen(base({ tipo: 'NEGATIVA', estado: 'SIN_OBJETO', soyArbitro: true }));
  await mount(DisputeScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1', disputeId: 'd1' } } });
  assert.equal(hasText('DISPUTA CERRADA: YA NO ES SU OBJETIVO'), true);
  assert.doesNotMatch(texts(), /GANA|RECHAZAR|ELIMINAR A|Quedan/);
});

// ---------- CaughtScreen y OpenDisputeScreen ----------

test('la víctima abre la disputa desde «Te han cazado» solo dentro de los 10 minutos', async () => {
  mock.timers.enable({ apis: ['setInterval', 'Date'], now: Date.parse('2026-11-17T17:00:00Z') });
  const { CaughtScreen } = load('screens/CaughtScreen.tsx', common);
  const navigation = navigationMock();
  await mount(CaughtScreen, { navigation, route: { params: { gameId: 'p1', disputarHasta: '2026-11-17T17:09:30Z' } } });
  assert.equal(hasText('9:30'), true);
  await act(async () => buttonWith('ABRIR DISPUTA').props.onPress());
  assert.deepEqual(navigation.destinations, [['OpenDispute', { gameId: 'p1', returnToCard: 'ESPECTADOR' }]]);
  await act(async () => mock.timers.tick(570_000));
  assert.equal(buttonWith('ABRIR DISPUTA'), undefined, 'vencido el plazo ya no se ofrece');
});

test('abrir una disputa manda el tipo y el texto y sustituye el formulario por la disputa', async () => {
  const calls = [];
  const { OpenDisputeScreen } = load('screens/OpenDisputeScreen.tsx', { ...common,
    '../api/game': { openDispute: async (...args) => { calls.push(args); return { id: 'd9' }; } },
  });
  const navigation = navigationMock();
  await mount(OpenDisputeScreen, { navigation, route: { params: { gameId: 'p1', tipo: 'NEGATIVA', returnToCard: 'VIVO' } } });
  assert.equal(hasText('NO ME ENSEÑA EL CÓDIGO'), true);
  await act(async () => tree.root.findByType('PrimaryButton').props.onPress());
  assert.equal(hasText('Cuenta qué ocurrió para abrir la disputa.'), true);
  assert.deepEqual(calls, []);
  await act(async () => tree.root.findByType('TextInput').props.onChangeText(' Se negó. '));
  await act(async () => tree.root.findByType('PrimaryButton').props.onPress());
  await flush();
  assert.deepEqual(calls, [['p1', 'NEGATIVA', 'Se negó.']]);
  assert.deepEqual(navigation.destinations, [['replace', 'Dispute', { gameId: 'p1', disputeId: 'd9', returnToCard: 'VIVO' }]]);
});

// ---------- EliminateScreen ----------

function eliminate(card) {
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', { ...common,
    '../api/game': { getMyMissionCard: async () => card, eliminateTarget: async () => { throw new Error('no debe llamarse'); } },
  });
  return EliminateScreen;
}

test('con una disputa abierta sobre una eliminación suya, «Eliminar» queda bloqueado', async () => {
  const EliminateScreen = eliminate({ estado: 'VIVO', aliasObjetivo: 'Marta', intentosRestantes: 3, bloqueadoHasta: null, bloqueadoPorDisputa: true });
  await mount(EliminateScreen, { navigation: navigationMock(), route: { params: { gameId: 'p1' } } });
  assert.equal(hasText('DISPUTA ABIERTA'), true);
  assert.equal(hasText('Tienes una disputa abierta'), true);
  assert.equal(buttonWith('NO ME ENSEÑA EL CÓDIGO'), undefined);
  const keys = tree.root.findAll((node) => /^Introducir \d$/.test(node.props.accessibilityLabel ?? '') && typeof node.props.onPress === 'function');
  assert.equal(keys.length, 10);
  assert.equal(keys.every((key) => key.props.disabled === true), true);
});

test('sin bloqueo, «No me enseña el código» abre la disputa por negativa (R12-31)', async () => {
  const EliminateScreen = eliminate({ estado: 'VIVO', aliasObjetivo: 'Marta', intentosRestantes: 3, bloqueadoHasta: null, bloqueadoPorDisputa: false });
  const navigation = navigationMock();
  await mount(EliminateScreen, { navigation, route: { params: { gameId: 'p1', blockedByDispute: true } } });
  assert.equal(hasText('DISPUTA ABIERTA'), false, 'manda la carta recién pedida, no el parámetro');
  await act(async () => buttonWith('NO ME ENSEÑA EL CÓDIGO').props.onPress());
  assert.deepEqual(navigation.destinations, [['OpenDispute', { gameId: 'p1', tipo: 'NEGATIVA', returnToCard: 'VIVO' }]]);
});

// ---------- MissionCardScreen: único vivo ----------

test('el único vivo espera el plazo de disputa y pasa al fin cuando la partida se cierra', async () => {
  mock.timers.enable({ apis: ['setInterval'] });
  let calls = 0;
  const closed = { id: 'p1', estado: 'CERRADA' };
  const { MissionCardScreen } = load('screens/MissionCardScreen.tsx', { ...common,
    '../api/game': {
      getDisputes: async () => [],
      getMyGames: async () => [closed],
      getMyMissionCard: async () => {
        calls += 1;
        if (calls >= 3) throw new ApiError('La partida no está en juego', 409, 'PARTIDA_NO_EN_JUEGO');
        return { estado: 'VIVO', aliasObjetivo: null, mision: null, eliminaciones: 2, bloqueadoPorDisputa: false, disputarHasta: null };
      },
    },
  });
  const navigation = navigationMock();
  await mount(MissionCardScreen, { navigation, route: { params: { gameId: 'p1' } } });
  assert.equal(hasText('ESPERANDO EL PLAZO DE DISPUTA'), true);
  assert.equal(buttonWith('ELIMINAR'), undefined);
  assert.equal(calls, 1);
  await act(async () => mock.timers.tick(59_000));
  assert.equal(calls, 1);
  await act(async () => mock.timers.tick(1_000));
  await flush();
  assert.equal(calls, 2);
  await act(async () => mock.timers.tick(60_000));
  await flush();
  assert.deepEqual(navigation.destinations.at(-1), ['replace', 'GameEnded', { game: closed }]);
});
