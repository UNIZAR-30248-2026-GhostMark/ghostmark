/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const ts = require('typescript');

function loadModal() {
  const source = fs.readFileSync(path.join(__dirname, '../src/components/DayScheduleModal.tsx'), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true } }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)((id) => {
    if (id === 'react') return { useMemo: () => undefined, useState: () => undefined };
    if (id === 'react-native') return { Modal: 'Modal', Pressable: 'Pressable', StyleSheet: { create: (styles) => styles }, Text: 'Text', View: 'View' };
    if (id === './WeeklyScheduleGrid') return { slotKey: () => '' };
    if (id === '../theme/colors') return { colors: { danger: 'danger', textMuted: 'muted' } };
    if (id === '../theme/bangers') return { bangers: () => ({}) };
    return require(id);
  }, module, module.exports);
  return module.exports;
}

const { mergeRange, rangeChangeMessage, removedRangeMessage } = loadModal();
const range = (start, end) => ({ startMinutes: start * 60, endMinutes: end * 60 });

test('añadir una franja separada informa de que se ha añadido', () => {
  const added = range(15, 16);
  const result = mergeRange([range(9, 12)], added);
  assert.equal(rangeChangeMessage(result.change, result.resultingRange).text, 'Franja añadida: 15:00-16:00.');
  assert.deepEqual(result.ranges, [range(9, 12), added]);
});

test('unir una franja informa de la franja con la que se une', () => {
  const added = range(12, 13);
  const result = mergeRange([range(9, 12)], added);
  assert.equal(rangeChangeMessage(result.change, result.resultingRange).text, 'Se ha unido con 9:00-12:00: ahora 9:00-13:00.');
  assert.deepEqual(result.ranges, [range(9, 13)]);
});

test('una franja que ocupa otra informa de la sustitución', () => {
  const added = range(8, 13);
  const result = mergeRange([range(9, 12)], added);
  assert.equal(rangeChangeMessage(result.change, result.resultingRange).text, 'Ocupa 9:00-12:00: ahora 8:00-13:00.');
  assert.deepEqual(result.ranges, [added]);
});

test('una franja ya cubierta no cambia el horario y muestra el error', () => {
  const added = range(10, 11);
  const original = [range(9, 12)];
  const result = mergeRange(original, added);
  const feedback = rangeChangeMessage(result.change, result.resultingRange);
  assert.equal(feedback.text, 'Esa franja ya está dentro de 9:00-12:00.');
  assert.equal(feedback.color, 'danger');
  assert.equal(result.ranges, original);
});

test('unir varias franjas nombra todas las franjas afectadas', () => {
  const added = range(9.5, 11.5);
  const result = mergeRange([range(9, 10), range(11, 12)], added);
  assert.equal(rangeChangeMessage(result.change, result.resultingRange).text, 'Se ha unido con 9:00-10:00 y 11:00-12:00: ahora 9:00-12:00.');
  assert.deepEqual(result.ranges, [range(9, 12)]);
});

test('quitar una franja informa de que se ha eliminado', () => {
  assert.equal(removedRangeMessage(range(9, 12)), 'Franja 9:00-12:00 eliminada.');
});
