/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const ts = require('typescript');

const source = fs.readFileSync(path.join(__dirname, '../src/utils/gameDate.ts'), 'utf8');
const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
const loadedModule = { exports: {} };
new Function('require', 'module', 'exports', code)(require, loadedModule, loadedModule.exports);

test('las fechas de partida incluyen día y hora sin segundos', () => {
  const label = loadedModule.exports.formatGameDate('2026-10-26T09:00:00.000Z');
  assert.match(label, /·/);
  assert.doesNotMatch(label, /:\d{2}:\d{2}/);
});
