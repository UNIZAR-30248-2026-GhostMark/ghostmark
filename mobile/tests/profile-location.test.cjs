/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { afterEach, test } = require('node:test');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');
global.IS_REACT_ACT_ENVIRONMENT = true;

function load(file, imports) {
  const source = fs.readFileSync(path.join(__dirname, '../src', file), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { esModuleInterop: true, jsx: ts.JsxEmit.ReactJSX, module: ts.ModuleKind.CommonJS } }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)((id) => (id in imports ? imports[id] : id.endsWith('.png') ? 1 : require(id)), module, module.exports);
  return module.exports;
}

let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; });

function setupProfile({ granted, canAskAgain = false, alias = 'fantasmilla' }) {
  let appStateListener;
  let settingsOpened = 0;
  const { ProfileScreen } = load('screens/ProfileScreen.tsx', {
    '@expo/vector-icons': { Ionicons: 'Ionicons' },
    '@react-navigation/native': { useFocusEffect: (effect) => React.useEffect(effect, [effect]) },
    'expo-location': {
      getForegroundPermissionsAsync: async () => ({ granted: granted.value, canAskAgain }),
      requestForegroundPermissionsAsync: async () => ({ granted: granted.value, canAskAgain }),
    },
    'react-native': {
      Alert: { alert() {} },
      AppState: { addEventListener: (_event, listener) => { appStateListener = listener; return { remove: () => { appStateListener = undefined; } }; } },
      Image: 'Image',
      Linking: { openSettings: async () => { settingsOpened += 1; } },
      Pressable: 'Pressable',
      StyleSheet: { create: (styles) => styles },
      Text: 'Text',
      View: 'View',
    },
    '../api/account': { deleteAccount: async () => undefined },
    '../api/auth': { clearSession: async () => undefined, getCurrentAccount: () => ({ alias }), signOut: async () => undefined },
    '../components/Screen': { Screen: 'Screen' },
    '../theme/colors': { colors: {} },
  });
  return {
    ProfileScreen,
    active: async () => act(async () => appStateListener('active')),
    settingsOpened: () => settingsOpened,
  };
}

test('Perfil muestra el estado real del permiso de ubicación', async () => {
  const granted = { value: true };
  const app = setupProfile({ granted });
  await act(async () => { tree = create(React.createElement(app.ProfileScreen, { navigation: {} })); });
  assert.match(JSON.stringify(tree.toJSON()), /Ubicación activa/);
});

test('Perfil abre Ajustes si Android ya no permite pedir ubicación y se actualiza al volver', async () => {
  const granted = { value: false };
  const app = setupProfile({ granted, canAskAgain: false });
  await act(async () => { tree = create(React.createElement(app.ProfileScreen, { navigation: {} })); });
  assert.match(JSON.stringify(tree.toJSON()), /Ubicación desactivada/);
  const permissionRow = tree.root.findAll((node) => node.type === 'Pressable' && node.props.accessibilityLabel === 'Actualizar permiso de ubicación')[0];
  await act(async () => permissionRow.props.onPress());
  assert.equal(app.settingsOpened(), 1);
  granted.value = true;
  await app.active();
  assert.match(JSON.stringify(tree.toJSON()), /Ubicación activa/);
});

test('Perfil muestra un alias válido largo completo en una sola línea', async () => {
  const app = setupProfile({ granted: { value: true }, alias: 'pbi1_regresion' });
  await act(async () => { tree = create(React.createElement(app.ProfileScreen, { navigation: {} })); });
  const alias = tree.root.findAllByType('Text').find((text) => text.props.children === 'pbi1_regresion');
  assert.equal(alias.props.numberOfLines, 1);
  assert.equal(alias.props.adjustsFontSizeToFit, true);
  assert.equal(alias.props.minimumFontScale, 0.55);
  assert.equal(alias.props.style[1].fontSize, 16);
});
