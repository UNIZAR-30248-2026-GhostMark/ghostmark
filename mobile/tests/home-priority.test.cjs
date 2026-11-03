/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const ts = require('typescript');

function loadHome() {
  const source = fs.readFileSync(path.join(__dirname, '../src/screens/HomeScreen.tsx'), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true } }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)((id) => {
    if (id === '@expo/vector-icons') return { Ionicons: 'Ionicons' };
    if (id === 'react-native') return { ActivityIndicator: 'ActivityIndicator', Image: 'Image', Pressable: 'Pressable', ScrollView: 'ScrollView', StyleSheet: { create: (styles) => styles }, Text: 'Text', View: 'View' };
    if (id === '../api/game') return { getMyGames: async () => [] };
    if (id === '../components/PrimaryButton') return { PrimaryButton: 'PrimaryButton' };
    if (id === '../components/Screen') return { Screen: 'Screen' };
    if (id === '../theme/colors') return { colors: {} };
    if (id === '../theme/bangers') return { bangers: () => ({}) };
    if (id === '../utils/gameDate') return { formatGameDate: () => '' };
    if (id === '../utils/serverWakeup') return { serverWakeupMessage: '', useServerWakeupNotice: () => false };
    return require(id);
  }, module, module.exports);
  return module.exports;
}

const { selectHomeGames } = loadHome();
const game = (id, estado) => ({ id, estado, nombre: id, inicioEn: '2026-11-01T10:00:00Z', finEn: '2026-11-02T10:00:00Z', numInscritos: 1 });

test('Inicio destaca una partida en juego aunque una cancelada llegue antes', () => {
  const cancelled = game('cancelada', 'CANCELADA');
  const active = game('en-juego', 'EN_JUEGO');
  const waiting = game('inscripcion', 'EN_INSCRIPCION');

  const selected = selectHomeGames([cancelled, waiting, active]);

  assert.equal(selected.featuredGame, active);
  assert.deepEqual(selected.otherGames, [cancelled, waiting]);
});

test('Inicio no muestra una cancelada como tarjeta grande si existe otra partida abierta', () => {
  const cancelled = game('cancelada', 'CANCELADA');
  const waiting = game('inscripcion', 'EN_INSCRIPCION');

  const selected = selectHomeGames([cancelled, waiting]);

  assert.equal(selected.featuredGame, waiting);
  assert.deepEqual(selected.otherGames, [cancelled]);
});
