/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');
global.IS_REACT_ACT_ENVIRONMENT = true;

function load() {
  const source = fs.readFileSync(path.join(__dirname, '../src/utils/serverWakeup.ts'), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)(require, module, module.exports);
  return module.exports;
}

const wakeup = load();

test('el aviso aparece tras cinco segundos y desaparece al acabar la petición', async () => {
  const originalSetTimeout = global.setTimeout;
  const originalClearTimeout = global.clearTimeout;
  let callback;
  global.setTimeout = (fn, delay) => { assert.equal(delay, 5_000); callback = fn; return 1; };
  global.clearTimeout = () => undefined;
  try {
    function Probe({ pending, requestId }) {
      return React.createElement('Text', null, wakeup.useServerWakeupNotice(pending, requestId) ? wakeup.serverWakeupMessage : 'sin aviso');
    }
    let tree;
    await act(async () => { tree = create(React.createElement(Probe, { pending: true, requestId: 1 })); });
    assert.match(JSON.stringify(tree.toJSON()), /sin aviso/);
    await act(async () => callback());
    assert.match(JSON.stringify(tree.toJSON()), /Despertando el servidor/);
    await act(async () => tree.update(React.createElement(Probe, { pending: false, requestId: 1 })));
    assert.match(JSON.stringify(tree.toJSON()), /sin aviso/);
    await act(async () => tree.unmount());
  } finally {
    global.setTimeout = originalSetTimeout;
    global.clearTimeout = originalClearTimeout;
  }
});
