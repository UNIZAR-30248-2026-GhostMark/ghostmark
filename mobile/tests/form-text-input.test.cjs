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
  const code = ts.transpileModule(source, {
    compilerOptions: { esModuleInterop: true, jsx: ts.JsxEmit.ReactJSX, module: ts.ModuleKind.CommonJS },
  }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)((id) => imports[id] ?? require(id), module, module.exports);
  return module.exports;
}

const native = {
  Pressable: 'Pressable',
  StyleSheet: { create: (styles) => styles },
  Text: 'Text',
  TextInput: 'TextInput',
  View: 'View',
};

test('los campos de contraseña no autocorrigen ni ponen mayúscula inicial', async () => {
  const { FormTextInput } = load('components/FormTextInput.tsx', {
    'react-native': native,
    '@expo/vector-icons': { Ionicons: 'Ionicons' },
    '../theme/colors': { colors: {} },
  });
  let tree;
  await act(async () => { tree = create(React.createElement(FormTextInput, { label: 'Contraseña', secureTextEntry: true })); });
  const input = tree.root.findByType('TextInput');
  assert.equal(input.props.autoCapitalize, 'none');
  assert.equal(input.props.autoCorrect, false);
});
