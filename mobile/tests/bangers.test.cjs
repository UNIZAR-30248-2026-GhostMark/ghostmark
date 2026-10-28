/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const ts = require('typescript');

function sourceFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const file = path.join(directory, entry.name);
    if (entry.isDirectory()) return sourceFiles(file);
    return /\.tsx?$/.test(entry.name) ? [file] : [];
  });
}

test('todos los estilos Bangers usan el margen proporcional compartido', () => {
  const sourceRoot = path.join(__dirname, '../src');
  const files = sourceFiles(sourceRoot).filter((file) => !file.endsWith(`${path.sep}theme${path.sep}bangers.ts`));
  for (const file of files) {
    assert.doesNotMatch(fs.readFileSync(file, 'utf8'), /fontFamily:\s*["']Bangers["']/,
      `${path.relative(sourceRoot, file)} debe usar bangers()`);
  }
});

test('el margen de Bangers aumenta con el tamaño de letra', () => {
  const source = fs.readFileSync(path.join(__dirname, '../src/theme/bangers.ts'), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
  const module = { exports: {} };
  new Function('module', 'exports', code)(module, module.exports);
  assert.deepEqual(module.exports.bangers(20), { fontFamily: 'Bangers', fontSize: 20, paddingRight: 4 });
  assert.deepEqual(module.exports.bangers(40), { fontFamily: 'Bangers', fontSize: 40, paddingRight: 8 });
});
