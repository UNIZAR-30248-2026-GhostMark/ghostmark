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

class AnimatedValue {
  constructor(value) { this.value = value; }
  interpolate() { return this; }
}

const native = {
  View: 'View', Text: 'Text', Pressable: 'Pressable', Image: 'Image',
  StyleSheet: { create: (styles) => styles, absoluteFillObject: {} },
  Animated: { Value: AnimatedValue, View: 'AnimatedView', timing: (value, options) => ({ start: () => { value.value = options.toValue; } }) },
};
const common = {
  'react-native': native,
  '../components/Screen': { Screen: 'Screen' },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../theme/colors': { colors: { background: '#000', ink: '#111', white: '#fff', surfaceRaised: '#222', border: '#333', text: '#eee', textMuted: '#aaa', lime: '#bfff38', danger: '#f55' } },
};
let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; });
async function mount(component, props) { await act(async () => { tree = create(React.createElement(component, props)); }); }

test('la carta consulta solo la carta propia de la partida y se revela al mantenerla pulsada', async () => {
  let requestedId;
  const { MissionCardScreen } = load('screens/MissionCardScreen.tsx', { ...common,
    '../api/client': { ApiError: class ApiError extends Error {} },
    '../api/game': { getMyMissionCard: async (gameId) => { requestedId = gameId; return { aliasObjetivo: 'Pablo', mision: 'Di la palabra compilador' }; } },
  });
  await mount(MissionCardScreen, { route: { params: { gameId: 'partida-propia' } } });
  assert.equal(requestedId, 'partida-propia');
  assert.match(JSON.stringify(tree.toJSON()), /PABLO/);
  const card = tree.root.findByProps({ accessibilityLabel: 'Carta de misión' });
  const flip = () => tree.root.findAllByType('AnimatedView')[1].props.style[1].opacity.value;
  assert.equal(flip(), 0);
  await act(async () => card.props.onPressIn());
  assert.equal(flip(), 1);
  await act(async () => card.props.onPressOut());
  assert.equal(flip(), 0);
  assert.match(JSON.stringify(tree.toJSON()), /COMPILADOR/);
});

test('el botón de la partida iniciada abre la carta de su propia partida', async () => {
  let destination;
  const { GameStartedScreen } = load('screens/GameStartedScreen.tsx', { ...common });
  await mount(GameStartedScreen, {
    navigation: { navigate: (...args) => { destination = args; } },
    route: { params: { game: { id: 'partida-propia', nombre: 'Killer EINA', inicioEn: '2026-10-28T10:00:00Z', numInscritos: 3 } } },
  });
  const button = tree.root.findAllByType('Pressable').find((node) => node.findAllByType('Text').some((text) => text.props.children === 'VER MI CARTA'));
  await act(async () => button.props.onPress());
  assert.deepEqual(destination, ['MissionCard', { gameId: 'partida-propia' }]);
});

test('la carta no falla si el objetivo borró su cuenta', async () => {
  const { MissionCardScreen } = load('screens/MissionCardScreen.tsx', { ...common,
    '../api/client': { ApiError: class ApiError extends Error {} },
    '../api/game': { getMyMissionCard: async () => ({ aliasObjetivo: null, mision: 'Di la palabra compilador' }) },
  });
  await mount(MissionCardScreen, { route: { params: { gameId: 'partida-propia' } } });
  assert.match(JSON.stringify(tree.toJSON()), /CUENTA ELIMINADA/);
});
