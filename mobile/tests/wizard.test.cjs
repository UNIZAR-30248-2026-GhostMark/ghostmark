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
  new Function('require', 'module', 'exports', code)((id) => imports[id] ?? (id.endsWith('/theme/bangers') ? { bangers: (fontSize) => ({ fontFamily: 'Bangers', fontSize, paddingRight: Math.ceil(fontSize * 0.18) }) } : require(id)), module, module.exports);
  return module.exports;
}

test('el asistente cuenta los cinco pasos, con Zonas seguras como paso 4', async () => {
  const { GameWizardHeader } = load('components/GameWizardHeader.tsx', {
    'react-native': { View: 'View', Text: 'Text', Pressable: 'Pressable', StyleSheet: { create: (styles) => styles } },
    '@expo/vector-icons': { Ionicons: 'Ionicons' },
    '../theme/colors': { colors: {} },
  });
  for (const [step, label] of [[1, 'Datos'], [2, 'Reglas'], [3, 'Misiones'], [4, 'Zonas seguras'], [5, 'Compartir']]) {
    let tree;
    await act(async () => { tree = create(React.createElement(GameWizardHeader, { step, label, onBack() {} })); });
    const content = JSON.stringify(tree.toJSON());
    assert.match(content, new RegExp(`"Paso ","${step}"`));
    assert.match(content, /de 5/);
    assert.equal(tree.root.findAllByType('View').filter((node) => (Array.isArray(node.props.style) ? node.props.style : [node.props.style]).some((style) => style?.height === 11)).length, 5);
    tree.unmount();
  }
});
