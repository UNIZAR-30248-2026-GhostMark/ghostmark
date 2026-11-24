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

test('el helper Bangers reserva altura vertical en Android', () => {
  const source = fs.readFileSync(path.join(__dirname, '../src/theme/bangers.ts'), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
  const module = { exports: {} };
  new Function('module', 'exports', code)(module, module.exports);
  assert.deepEqual(module.exports.bangers(20), { fontFamily: 'Bangers', fontSize: 20, includeFontPadding: true, lineHeight: 25 });
  assert.deepEqual(module.exports.bangers(40), { fontFamily: 'Bangers', fontSize: 40, includeFontPadding: true, lineHeight: 49 });
});

test('los textos Bangers de botones y pestañas no quedan ajustados a su contenido', () => {
  const sourceRoot = path.join(__dirname, '../src');
  const buttonTexts = [
    ['components/PrimaryButton.tsx', 'label'],
    ['components/DayScheduleModal.tsx', 'addText'],
    ['components/DayScheduleModal.tsx', 'saveText'],
    ['screens/CreateGameDataScreen.tsx', 'doneText'],
    ['screens/GameStartedScreen.tsx', 'cardButtonText'],
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

  assert.match(home, /styles\.createText, bangersLayout\.buttonText/);
  assert.match(home, /styles\.joinText, bangersLayout\.buttonText/);
  assert.match(home, /brand:\s*\{\s*flex:\s*1\s*\}/);
  assert.match(home, /brandText:\s*\{\s*flex:\s*1\s*\}/);
  assert.match(home, /buttonText:\s*\{[^}]*alignSelf:\s*"stretch"[^}]*textAlign:\s*"center"/);
  assert.match(cancelled, /countValueWide:\s*\{[^}]*alignSelf:\s*"stretch"[^}]*textAlign:\s*"center"/);
  assert.match(createGame, /timeWide:\s*\{[^}]*flex:\s*1[^}]*textAlign:\s*"center"/);
});

test('los títulos Bangers de varias líneas tienen altura suficiente en Android', () => {
  const sourceRoot = path.join(__dirname, '../src');
  const started = fs.readFileSync(path.join(sourceRoot, 'screens/GameStartedScreen.tsx'), 'utf8');
  const cancelled = fs.readFileSync(path.join(sourceRoot, 'screens/GameCancelledScreen.tsx'), 'utf8');

  const startedTitle = started.match(/title:\s*\{([^}]*)\}/);
  assert.ok(startedTitle, 'GameStartedScreen debe declarar el estilo title');
  assert.match(startedTitle[1], /alignSelf:\s*"stretch"/);
  assert.match(startedTitle[1], /\.\.\.bangers\(42\)/);
  assert.match(startedTitle[1], /lineHeight:\s*(4[3-9]|[5-9]\d)/);

  const cancelledTitle = cancelled.match(/title:\s*\{([^}]*)\}/);
  assert.ok(cancelledTitle, 'GameCancelledScreen debe declarar el estilo title');
  assert.match(cancelledTitle[1], /\.\.\.bangers\(38\)/);
  assert.match(cancelledTitle[1], /lineHeight:\s*(3[8-9]|[4-9]\d)/);

  // #259: con la altura de línea igual a la letra, Android corta la segunda línea («TERMINADO»).
  const ended = fs.readFileSync(path.join(sourceRoot, 'screens/GameEndedScreen.tsx'), 'utf8');
  const endedTitle = ended.match(/title:\s*\{([^}]*)\}/);
  assert.ok(endedTitle, 'GameEndedScreen debe declarar el estilo title');
  assert.match(endedTitle[1], /\.\.\.bangers\(42\)/);
  assert.match(endedTitle[1], /lineHeight:\s*(5[0-9]|[6-9]\d)/);
  assert.match(endedTitle[1], /minHeight:\s*112/);

  const mission = fs.readFileSync(path.join(sourceRoot, 'screens/MissionCardScreen.tsx'), 'utf8');
  const dormantCountdown = mission.match(/dormantCountdown:\s*\{([^}]*)\}/);
  assert.ok(dormantCountdown, 'la carta dormida debe reservar dos líneas completas');
  assert.match(mission, /dormantCard:[^\n]*minHeight:\s*348/);
  assert.match(dormantCountdown[1], /lineHeight:\s*(3[5-9]|[4-9]\d)/);
  assert.match(dormantCountdown[1], /minHeight:\s*76/);
});

test('los números de las casillas de Eliminar caben enteros y centrados (#263)', () => {
  const eliminate = fs.readFileSync(path.join(__dirname, '../src/screens/EliminateScreen.tsx'), 'utf8');
  const slotText = eliminate.match(/slotText:\s*\{([^}]*)\}/);
  assert.ok(slotText, 'EliminateScreen debe declarar el estilo slotText');
  assert.match(slotText[1], /alignSelf:\s*"stretch"/);
  assert.match(slotText[1], /textAlign:\s*"center"/);
  assert.match(slotText[1], /\.\.\.bangers\(34\)/);
  assert.match(slotText[1], /lineHeight:\s*(3[5-9]|[4-9]\d)/);
});

test('Sala de espera reserva ancho para el título entre los controles', () => {
  const waitingRoom = fs.readFileSync(path.join(__dirname, '../src/screens/WaitingRoomScreen.tsx'), 'utf8');
  assert.match(waitingRoom, /title:\s*\{[^}]*flex:\s*1/);
});
