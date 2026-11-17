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
  StyleSheet: { create: (styles) => styles }, useWindowDimensions: () => ({ height: 780, width: 360 }),
};
const common = {
  'react-native': native,
  '@expo/vector-icons': { Ionicons: 'Ionicons' },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../components/GameWizardHeader': { GameWizardHeader: 'GameWizardHeader' },
  '../components/Screen': { Screen: 'Screen' },
  '../theme/bangers': { bangers: () => ({}) },
  '../theme/colors': { colors: {} },
};

let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; });
const texts = () => tree.root.findAllByType('Text').map((node) => [node.props.children].flat().join('')).join(' | ');
const mount = async (component, props) => act(async () => { tree = create(React.createElement(component, props)); });
const cancelled = (numInscritos) => ({ id: 'p1', nombre: 'Killer EINA', numInscritos, motivoCancelacion: 'POCOS_JUGADORES' });

test('#255: con una persona inscrita la partida cancelada dice «inscrito», con más, «inscritos»', async () => {
  const { GameCancelledScreen } = load('screens/GameCancelledScreen.tsx', common);
  await mount(GameCancelledScreen, { navigation: { reset() {} }, route: { params: { game: cancelled(1) } } });
  assert.match(texts(), /\b1 \| inscrito \|/);
  await act(async () => tree.unmount());
  await mount(GameCancelledScreen, { navigation: { reset() {} }, route: { params: { game: cancelled(2) } } });
  assert.match(texts(), /\b2 \| inscritos \|/);
});

test('#256: Inicio vuelve a pedir las partidas cada vez que se vuelve a la pantalla (R11-39)', async () => {
  const focus = { value: true };
  let calls = 0;
  const games = [];
  const { HomeScreen } = load('screens/HomeScreen.tsx', { ...common,
    '@react-navigation/native': { useIsFocused: () => focus.value },
    '../api/game': { getMyGames: async () => { calls += 1; return [...games]; } },
    '../utils/gameDate': { formatGameDate: () => '' },
    '../utils/gameResult': { resultSentence: () => '' },
    '../utils/serverWakeup': { serverWakeupMessage: '', useServerWakeupNotice: () => false },
  });
  const props = { navigation: { navigate() {} }, route: { params: undefined } };
  await mount(HomeScreen, props);
  assert.equal(calls, 1);
  focus.value = false;
  await act(async () => tree.update(React.createElement(HomeScreen, props)));
  games.push({ id: 'qr', nombre: 'Partida del QR', estado: 'EN_INSCRIPCION', inicioEn: '2026-11-20T10:00:00+01:00', finEn: '2026-11-27T21:00:00+01:00', numInscritos: 4, soyMaster: false, soyJugador: true, motivoCancelacion: null, resultado: null });
  focus.value = true;
  await act(async () => tree.update(React.createElement(HomeScreen, props)));
  assert.equal(calls, 2, 'al volver a Inicio se piden otra vez');
  assert.match(texts(), /PARTIDA DEL QR/);
});

test('#257: el código de la partida va dentro de la tarjeta del QR, no debajo del todo (R11-35)', async () => {
  const { ShareGameScreen } = load('screens/ShareGameScreen.tsx', { ...common,
    'expo-clipboard': { setStringAsync: async () => true },
    'react-native-qrcode-svg': { __esModule: true, default: 'QRCode' },
  });
  await mount(ShareGameScreen, { navigation: { popToTop() {}, replace() {} }, route: { params: { game: { id: 'p1', nombre: 'Killer EINA', codigoInvitacion: '4RN87Y6U' } } } });
  const qr = tree.root.findByType('QRCode');
  const card = qr.parent.parent;
  const codeInCard = card.findAllByType('Text').some((node) => node.props.children === '4RN87Y6U');
  assert.equal(codeInCard, true, 'el código está en la misma tarjeta que el QR');
  assert.ok(qr.props.size <= 210, 'el QR deja sitio al código en la primera pantalla');
});

test('#258: en la pestaña Partida, el máster que no juega solo tiene Feed y Partida (R11-36)', async () => {
  const game = { id: 'p1', nombre: 'Killer EINA', estado: 'EN_JUEGO', inicioEn: '2026-11-16T09:00:00+01:00', finEn: '2026-11-23T21:00:00+01:00', numInscritos: 3, soyMaster: true, soyJugador: false, motivoCancelacion: null, resultado: null };
  const { GameTabScreen } = load('screens/GameTabScreen.tsx', { ...common,
    '@react-navigation/native': { useIsFocused: () => true },
    '../api/client': { ApiError: class ApiError extends Error {} },
    '../api/auth': { getCurrentAccount: () => ({ alias: 'imad' }) },
    '../api/game': { getMyGames: async () => [game], closeGame: async () => game },
    '../api/inscription': { getPlayers: async () => [{ id: 'i1', alias: 'ana', esMaster: false }] },
    '../components/ConfirmDialog': { ConfirmDialog: 'ConfirmDialog' },
  });
  await mount(GameTabScreen, { navigation: { navigate() {}, replace() {} }, route: { params: { gameId: 'p1' } } });
  assert.equal(texts().includes('Misión'), false);
  assert.match(texts(), /Feed \| PARTIDA/);
});

test('#265: el cuadro de confirmación tiene título, mensaje y los dos botones abajo, y cada uno hace lo suyo (R11-40)', async () => {
  const { ConfirmDialog } = load('components/ConfirmDialog.tsx', common);
  const pressed = [];
  await mount(ConfirmDialog, { visible: true, title: '¿Cerrar la partida?', message: 'Termina para todos.', confirmLabel: 'Cerrar', destructive: true, onCancel: () => pressed.push('cancelar'), onConfirm: () => pressed.push('cerrar') });
  assert.equal(tree.root.findByType('Modal').props.visible, true);
  assert.match(texts(), /¿CERRAR LA PARTIDA\? \| Termina para todos\. \| CANCELAR \| CERRAR/);
  const buttons = tree.root.findAll((node) => node.type === 'Pressable');
  await act(async () => buttons[0].props.onPress());
  await act(async () => buttons[1].props.onPress());
  assert.deepEqual(pressed, ['cancelar', 'cerrar']);
});
