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

test('todos los estilos Bangers usan el helper compartido', () => {
  const sourceRoot = path.join(__dirname, '../src');
  const files = sourceFiles(sourceRoot).filter((file) => !file.endsWith(`${path.sep}theme${path.sep}bangers.ts`));
  for (const file of files) {
    assert.doesNotMatch(fs.readFileSync(file, 'utf8'), /fontFamily:\s*["']Bangers["']/,
      `${path.relative(sourceRoot, file)} debe usar bangers()`);
  }
});

test('el helper Bangers conserva el tamaño solicitado', () => {
  const source = fs.readFileSync(path.join(__dirname, '../src/theme/bangers.ts'), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
  const module = { exports: {} };
  new Function('module', 'exports', code)(module, module.exports);
  assert.deepEqual(module.exports.bangers(20), { fontFamily: 'Bangers', fontSize: 20 });
  assert.deepEqual(module.exports.bangers(40), { fontFamily: 'Bangers', fontSize: 40 });
});

test('los textos Bangers de botones y pestañas no quedan ajustados a su contenido', () => {
  const sourceRoot = path.join(__dirname, '../src');
  const buttonTexts = [
    ['components/PrimaryButton.tsx', 'label'],
    ['components/DayScheduleModal.tsx', 'addText'],
    ['components/DayScheduleModal.tsx', 'saveText'],
    ['screens/CreateGameDataScreen.tsx', 'doneText'],
    ['screens/GameStartedScreen.tsx', 'cardButtonText'],
    ['screens/HomeScreen.tsx', 'buttonBangersText'],
    ['screens/MissionCardScreen.tsx', 'tabOnText'],
    ['screens/MissionCardScreen.tsx', 'codeText'],
    ['screens/MissionCardScreen.tsx', 'removeText'],
    ['screens/ScanInvitationScreen.tsx', 'buttonText'],
    ['screens/ScanInvitationScreen.tsx', 'secondaryText'],
    ['screens/WaitingRoomScreen.tsx', 'reviewText'],
  ];
  for (const [file, style] of buttonTexts) {
    const source = fs.readFileSync(path.join(sourceRoot, file), 'utf8');
    const match = source.match(new RegExp(`${style}:\\s*\\{([^}]*)\\}`));
    assert.ok(match, `${file} debe declarar ${style}`);
    assert.match(match[1], /alignSelf:\s*"stretch"/,
      `${file}:${style} debe ocupar el ancho del contenedor`);
    assert.match(match[1], /textAlign:\s*"center"/,
      `${file}:${style} debe centrar dentro de una caja amplia`);
  }
});

test('Inicio y los controles en fila reservan ancho para Bangers', () => {
  const sourceRoot = path.join(__dirname, '../src');
  const home = fs.readFileSync(path.join(sourceRoot, 'screens/HomeScreen.tsx'), 'utf8');
  const cancelled = fs.readFileSync(path.join(sourceRoot, 'screens/GameCancelledScreen.tsx'), 'utf8');
  const createGame = fs.readFileSync(path.join(sourceRoot, 'screens/CreateGameDataScreen.tsx'), 'utf8');

  assert.match(home, /styles\.createText, styles\.buttonBangersText/);
  assert.match(home, /styles\.joinText, styles\.buttonBangersText/);
  assert.match(home, /brandWide:\s*\{\s*flex:\s*1\s*\}/);
  assert.match(home, /brandTextWide:\s*\{\s*flex:\s*1\s*\}/);
  assert.match(cancelled, /countValueWide:\s*\{[^}]*alignSelf:\s*"stretch"[^}]*textAlign:\s*"center"/);
  assert.match(createGame, /timeWide:\s*\{[^}]*flex:\s*1[^}]*textAlign:\s*"center"/);
});
