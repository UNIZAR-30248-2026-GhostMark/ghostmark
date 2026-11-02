/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
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
  Image: 'Image', Pressable: 'Pressable', StyleSheet: { create: (styles) => styles }, Text: 'Text', View: 'View',
};
const lifeCode = load('utils/lifeCode.ts');
const common = {
  'react-native': native,
  '../components/Screen': { Screen: 'Screen' },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../theme/bangers': { bangers: () => ({}) },
  '../theme/colors': { colors: { background: '', border: '', danger: '', ink: '', lime: '', surfaceRaised: '', text: '', textMuted: '', white: '' } },
  '../api/client': { ApiError: class ApiError extends Error {} },
  '../utils/lifeCode': lifeCode,
  'react-native-svg': {
    __esModule: true,
    default: (props) => React.createElement('Svg', props),
    Circle: (props) => React.createElement('Circle', props),
  },
};

test('Mi código muestra los seis dígitos y la cuenta atrás recibidos', async () => {
  const { LifeCodeScreen } = load('screens/LifeCodeScreen.tsx', {
    ...common,
    '../api/game': { getMyLifeCode: async () => ({ codigo: '482913', caducaEn: new Date(Date.now() + 60_000).toISOString() }) },
  });
  let tree;
  try {
    await act(async () => { tree = create(React.createElement(LifeCodeScreen, { navigation: { goBack() {} }, route: { params: { gameId: 'p1' } } })); });
    const shown = JSON.stringify(tree.toJSON());
    assert.match(shown, /482/);
    assert.match(shown, /913/);
    assert.match(shown, /Tu código/);
  } finally {
    await act(async () => tree.unmount());
  }
});

test('Eliminar envía seis dígitos y abre la pantalla de acierto', async () => {
  const calls = [];
  const destinations = [];
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    ...common,
    '../api/game': {
      eliminateTarget: async (...args) => {
        calls.push(args);
        return { aliasVictima: 'Pablo', carta: { aliasObjetivo: 'Marta', mision: 'Saluda', estado: 'VIVO', eliminaciones: 1 } };
      },
    },
  });
  let tree;
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: { goBack() {}, replace: (...args) => destinations.push(args) }, route: { params: { gameId: 'p1' } } })); });
  for (const digit of '482913') {
    const key = tree.root.findAllByType('Pressable').find((node) => node.props.accessibilityLabel === `Introducir ${digit}`);
    await act(async () => key.props.onPress());
  }
  assert.deepEqual(calls, [['p1', '482913']]);
  assert.deepEqual(destinations, [['EliminationSuccess', { gameId: 'p1', result: { aliasVictima: 'Pablo', carta: { aliasObjetivo: 'Marta', mision: 'Saluda', estado: 'VIVO', eliminaciones: 1 } } }]]);
  tree.unmount();
});
