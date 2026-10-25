/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test, afterEach } = require('node:test');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');
global.IS_REACT_ACT_ENVIRONMENT = true;

function load(file, imports) {
  const source = fs.readFileSync(path.join(__dirname, '../src', file), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true } }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)((id) => (id in imports ? imports[id] : id.endsWith('.png') ? 1 : require(id)), module, module.exports);
  return module.exports;
}

let tree;
let settingsOpened = 0;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; settingsOpened = 0; });

function locationScreen(current, requested) {
  return load('screens/LocationConsentScreen.tsx', {
    'react-native': { View: 'View', Text: 'Text', Image: 'Image', StyleSheet: { create: (styles) => styles }, Linking: { openSettings: async () => { settingsOpened += 1; } } },
    'expo-location': { getForegroundPermissionsAsync: async () => current(), requestForegroundPermissionsAsync: async () => requested() },
    '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
    '../components/Screen': { Screen: 'Screen' },
    '../theme/colors': { colors: {} },
    '../theme/typography': { typography: {} },
  }).LocationConsentScreen;
}
const button = (label) => tree.root.findAll((node) => node.type === 'PrimaryButton' && node.props.children === label)[0];
const shown = () => JSON.stringify(tree.toJSON());

test('denegada para siempre, pide ir a ajustes y al volver con el permiso sigue', async () => {
  let granted = false;
  let destination;
  const LocationConsentScreen = locationScreen(() => ({ granted, canAskAgain: false }), () => { throw new Error('No debe pedir el permiso'); });
  await act(async () => { tree = create(React.createElement(LocationConsentScreen, { navigation: { replace: (name) => { destination = name; } } })); });
  assert.equal(button('ABRIR AJUSTES'), undefined);
  await act(async () => button('DAR PERMISO').props.onPress());
  assert.match(shown(), /Activa la ubicación en los ajustes del móvil para seguir\./);
  await act(async () => button('ABRIR AJUSTES').props.onPress());
  assert.equal(settingsOpened, 1);
  granted = true;
  await act(async () => button('DAR PERMISO').props.onPress());
  assert.equal(destination, 'AvailabilityConsent');
});

test('si se deniega en el diálogo y Android ya no deja preguntar, ofrece los ajustes', async () => {
  const LocationConsentScreen = locationScreen(() => ({ granted: false, canAskAgain: true }), () => ({ granted: false, canAskAgain: false }));
  await act(async () => { tree = create(React.createElement(LocationConsentScreen, { navigation: {} })); });
  await act(async () => button('DAR PERMISO').props.onPress());
  assert.match(shown(), /Activa la ubicación en los ajustes del móvil para seguir\./);
  assert.notEqual(button('ABRIR AJUSTES'), undefined);
});

test('la primera denegación explica para qué es el permiso y no ofrece los ajustes', async () => {
  const LocationConsentScreen = locationScreen(() => ({ granted: false, canAskAgain: true }), () => ({ granted: false, canAskAgain: true }));
  await act(async () => { tree = create(React.createElement(LocationConsentScreen, { navigation: {} })); });
  await act(async () => button('DAR PERMISO').props.onPress());
  assert.match(shown(), /Necesitamos el permiso de ubicación para comprobar las zonas seguras al eliminar\./);
  assert.equal(button('ABRIR AJUSTES'), undefined);
});
