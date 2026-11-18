/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { mock, test } = require('node:test');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');

global.IS_REACT_ACT_ENVIRONMENT = true;

function load(file, imports = {}) {
  const source = fs.readFileSync(path.join(__dirname, '../src', file), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true } }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)((id) => {
    if (id in imports) return imports[id];
    if (id.endsWith('.png')) return 1;
    return require(id);
  }, module, module.exports);
  return module.exports;
}

const native = {
  Image: 'Image', Pressable: 'Pressable', StyleSheet: { create: (styles) => styles }, Text: 'Text', View: 'View',
};
const lifeCode = load('utils/lifeCode.ts');
const common = {
  'react-native': native,
  '../components/Screen': { Screen: 'Screen' },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../theme/bangers': { bangers: () => ({}) },
  '../theme/colors': { colors: { background: '', border: '', coral: '', cream: '', danger: '', ink: '', lime: '', surfaceRaised: '', text: '', textMuted: '', white: '' } },
  'react-native-safe-area-context': { SafeAreaView: 'SafeAreaView' },
  '@expo/vector-icons': { Ionicons: 'Ionicons' },
  '../api/client': { ApiError: class ApiError extends Error {} },
  '../utils/lifeCode': lifeCode,
  'react-native-svg': {
    __esModule: true,
    default: (props) => React.createElement('Svg', props),
    Circle: (props) => React.createElement('Circle', props),
    Defs: (props) => React.createElement('Defs', props),
    G: (props) => React.createElement('G', props),
    Path: (props) => React.createElement('Path', props),
    Pattern: (props) => React.createElement('Pattern', props),
    Rect: (props) => React.createElement('Rect', props),
  },
  '../api/game': { getMyGames: async () => [] },
};

test('Mi código muestra los seis dígitos y la cuenta atrás recibidos', async () => {
  const { LifeCodeScreen } = load('screens/LifeCodeScreen.tsx', {
    ...common,
    '../api/game': { getMyLifeCode: async () => ({ codigo: '482913', caducaEn: new Date(Date.now() + 60_000).toISOString() }), getMyMissionCard: async () => ({ estado: 'VIVO' }) },
  });
  let tree;
  try {
    await act(async () => { tree = create(React.createElement(LifeCodeScreen, { navigation: { goBack() {} }, route: { params: { gameId: 'p1' } } })); });
    // Solo el texto visible: la etiqueta de accesibilidad también lleva el código.
    const texts = [];
    const visit = (node) => {
      if (!node || typeof node === 'string') return;
      if (Array.isArray(node)) { node.forEach(visit); return; }
      if (node.type === 'Text') texts.push((node.children ?? []).filter((child) => typeof child === 'string').join(''));
      visit(node.children);
    };
    visit(tree.toJSON());
    assert.ok(texts.includes('482 913'), 'Se ve el código en dos grupos de tres');
    assert.ok(texts.includes('Tu código'));
    assert.ok(texts.includes('60 s'), 'con el contrato nuevo el círculo empieza con un minuto completo');
  } finally {
    await act(async () => tree.unmount());
  }
});

test('Mi código pasa directamente a «Te han cazado» si el servidor confirma la eliminación', async () => {
  const ApiErrorWithCode = class ApiError extends Error { constructor() { super('El jugador está eliminado'); this.code = 'JUGADOR_ELIMINADO'; } };
  const destinations = [];
  const { LifeCodeScreen } = load('screens/LifeCodeScreen.tsx', {
    ...common,
    '../api/client': { ApiError: ApiErrorWithCode },
    '../api/game': { getMyLifeCode: async () => { throw new ApiErrorWithCode(); } },
  });
  let tree;
  try {
    await act(async () => { tree = create(React.createElement(LifeCodeScreen, { navigation: { goBack() {}, replace: (...args) => destinations.push(args) }, route: { params: { gameId: 'p1' } } })); });
    assert.deepEqual(destinations, [['Caught', { gameId: 'p1' }]]);
    assert.doesNotMatch(JSON.stringify(tree.toJSON()), /REINTENTAR/);
  } finally {
    await act(async () => tree.unmount());
  }
});

test('Mi código detecta una eliminación mientras está abierto, sin pedir códigos nuevos', async () => {
  mock.timers.enable({ apis: ['setInterval', 'Date'], now: Date.parse('2026-11-05T17:30:00Z') });
  const destinations = [];
  let codeCalls = 0;
  let cardCalls = 0;
  const { LifeCodeScreen } = load('screens/LifeCodeScreen.tsx', {
    ...common,
    '../api/game': {
      getMyLifeCode: async () => { codeCalls += 1; return { codigo: '482913', caducaEn: new Date(Date.now() + 60_000).toISOString() }; },
      getMyMissionCard: async () => { cardCalls += 1; return { estado: cardCalls === 1 ? 'VIVO' : 'ELIMINADO' }; },
    },
  });
  let tree;
  try {
    await act(async () => { tree = create(React.createElement(LifeCodeScreen, { navigation: { goBack() {}, replace: (...args) => destinations.push(args) }, route: { params: { gameId: 'p1' } } })); });
    await act(async () => { mock.timers.tick(5_000); });
    assert.equal(destinations.length, 0);
    await act(async () => { mock.timers.tick(5_000); });
    assert.deepEqual(destinations, [['Caught', { gameId: 'p1' }]]);
    // Con el #261 el código se emite al pedirlo: la comprobación no puede pedir códigos.
    assert.equal(codeCalls, 1);
  } finally {
    await act(async () => tree.unmount());
    mock.timers.reset();
  }
});

test('Eliminar envía seis dígitos y abre la pantalla de acierto', async () => {
  const calls = [];
  const destinations = [];
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    ...common,
    '../api/game': {
      getMyMissionCard: async () => ({ aliasObjetivo: 'Pablo', intentosRestantes: 3, bloqueadoHasta: null }),
      eliminateTarget: async (...args) => {
        calls.push(args);
        return { aliasVictima: 'Pablo', carta: { aliasObjetivo: 'Marta', mision: 'Saluda', estado: 'VIVO', eliminaciones: 1 } };
      },
    },
  });
  let tree;
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: { goBack() {}, replace: (...args) => destinations.push(args) }, route: { params: { gameId: 'p1' } } })); });
  for (const digit of '482913') {
    const key = tree.root.findAllByType('Pressable').find((node) => node.props.accessibilityLabel === `Introducir ${digit}`);
    await act(async () => key.props.onPress());
  }
  assert.deepEqual(calls, [['p1', '482913']]);
  assert.deepEqual(destinations, [['EliminationSuccess', { gameId: 'p1', result: { aliasVictima: 'Pablo', carta: { aliasObjetivo: 'Marta', mision: 'Saluda', estado: 'VIVO', eliminaciones: 1 } } }]]);
  tree.unmount();
});

test('al acabar la cuenta atrás Mi código pide el código nuevo sin repetir la petición cada segundo', async () => {
  mock.timers.enable({ apis: ['setInterval', 'Date'], now: Date.parse('2026-11-05T17:30:30Z') });
  // Con el reloj del móvil adelantado, el servidor aún devuelve el mismo código al llegar a 0.
  const responses = [
    { codigo: '482913', caducaEn: '2026-11-05T17:31:00Z' },
    { codigo: '482913', caducaEn: '2026-11-05T17:31:00Z' },
    { codigo: '105377', caducaEn: '2026-11-05T17:32:00Z' },
  ];
  let calls = 0;
  const { LifeCodeScreen } = load('screens/LifeCodeScreen.tsx', {
    ...common,
    // La comprobación de la eliminación va a la carta, así que no cuenta como petición de código.
    '../api/game': { getMyLifeCode: async () => responses[Math.min(calls++, responses.length - 1)], getMyMissionCard: async () => ({ estado: 'VIVO' }) },
  });
  let tree;
  try {
    await act(async () => { tree = create(React.createElement(LifeCodeScreen, { navigation: { goBack() {} }, route: { params: { gameId: 'p1' } } })); });
    assert.match(JSON.stringify(tree.toJSON()), /"30 s"/);
    await act(async () => { mock.timers.tick(29_000); });
    assert.equal(calls, 1);
    assert.match(JSON.stringify(tree.toJSON()), /"1 s"/);
    await act(async () => { mock.timers.tick(1_000); });
    assert.equal(calls, 2);
    // Sigue el mismo código: espera 2 s antes de volver a pedirlo.
    await act(async () => { mock.timers.tick(1_000); });
    assert.equal(calls, 2);
    await act(async () => { mock.timers.tick(1_000); });
    assert.equal(calls, 3);
    const shown = JSON.stringify(tree.toJSON());
    assert.match(shown, /105/);
    assert.match(shown, /377/);
    assert.doesNotMatch(shown, /482/);
  } finally {
    await act(async () => tree.unmount());
    mock.timers.reset();
  }
});

async function successScreen(result) {
  const destinations = [];
  const { EliminationSuccessScreen } = load('screens/EliminationSuccessScreen.tsx', common);
  let tree;
  await act(async () => {
    tree = create(React.createElement(EliminationSuccessScreen, {
      navigation: { popTo: (...args) => destinations.push(args), replace: (...args) => destinations.push(args) },
      route: { params: { gameId: 'p1', result } },
    }));
  });
  return { destinations, tree };
}

test('el acierto dice a quién has eliminado y vuelve a la misma carta, ya recargada', async () => {
  const { destinations, tree } = await successScreen({ aliasVictima: 'Pablo', carta: { aliasObjetivo: 'Marta', mision: 'Saluda', estado: 'VIVO', eliminaciones: 1 } });
  const shown = JSON.stringify(tree.toJSON());
  assert.match(shown, /¡ELIMINADO!/);
  assert.match(shown, /Pablo/);
  assert.match(shown, /queda fuera de la partida/);
  assert.match(shown, /Heredas su objetivo y su misión/);
  const button = tree.root.findAllByType('Pressable')[0];
  assert.match(JSON.stringify(button.findAllByType('Text').map((text) => text.props.children)), /VER MI NUEVA CARTA/);
  button.props.onPress();
  // popTo vuelve a la carta que ya estaba abierta: atrás no queda una carta antigua.
  // Con herencia, la carta nueva gira sola una vez al volver (UI11).
  assert.deepEqual(destinations, [['MissionCard', { gameId: 'p1', revealOnce: true }]]);
  await act(async () => tree.unmount());
});

test('el acierto del último vivo no promete un objetivo heredado', async () => {
  const { destinations, tree } = await successScreen({ aliasVictima: null, carta: { aliasObjetivo: null, mision: null, estado: 'VIVO', eliminaciones: 2 } });
  const shown = JSON.stringify(tree.toJSON());
  assert.match(shown, /Cuenta eliminada/);
  assert.match(shown, /Ya no tienes objetivo/);
  assert.doesNotMatch(shown, /Heredas/);
  assert.match(shown, /VER MI CARTA/);
  tree.root.findAllByType('Pressable')[0].props.onPress();
  // Sin objetivo heredado no hay carta nueva que girar.
  assert.deepEqual(destinations, [['MissionCard', { gameId: 'p1', revealOnce: false }]]);
  await act(async () => tree.unmount());
});

test('el acierto del último rival lleva al fin, aunque la carta de la respuesta llegue antigua', async () => {
  const closed = { id: 'p1', estado: 'CERRADA', resultado: { tipo: 'SUPERVIVIENTE', aliasGanadores: ['Ana'], eliminaciones: 1, cerradaEn: '2026-11-15T12:00:00Z' } };
  const destinations = [];
  const { EliminationSuccessScreen } = load('screens/EliminationSuccessScreen.tsx', { ...common, '../api/game': { getMyGames: async () => [closed] } });
  let tree;
  await act(async () => {
    tree = create(React.createElement(EliminationSuccessScreen, {
      navigation: { popTo: (...args) => destinations.push(args), replace: (...args) => destinations.push(args) },
      route: { params: { gameId: 'p1', result: { aliasVictima: 'Pablo', carta: { aliasObjetivo: 'Marta', mision: 'Antigua', estado: 'VIVO', eliminaciones: 1 } } } },
    }));
  });
  assert.match(JSON.stringify(tree.toJSON()), /Ya no tienes objetivo/);
  await act(async () => tree.root.findAllByType('Pressable')[0].props.onPress());
  assert.deepEqual(destinations, [['GameEnded', { game: closed }]]);
  await act(async () => tree.unmount());
});

test('Eliminar manda el código una sola vez aunque se pulse otra tecla mientras se comprueba', async () => {
  let calls = 0;
  let resolve;
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    ...common,
    '../api/game': { getMyMissionCard: async () => ({ intentosRestantes: 3, bloqueadoHasta: null }), eliminateTarget: () => { calls += 1; return new Promise((done) => { resolve = done; }); } },
  });
  let tree;
  // StrictMode repite las funciones de cambio de estado, como puede hacer React en la app.
  await act(async () => { tree = create(React.createElement(React.StrictMode, null, React.createElement(EliminateScreen, { navigation: { goBack() {}, replace() {} }, route: { params: { gameId: 'p1' } } }))); });
  const press = async (digit) => {
    const key = tree.root.findAllByType('Pressable').find((node) => node.props.accessibilityLabel === `Introducir ${digit}`);
    await act(async () => key.props.onPress());
  };
  for (const digit of '4829131') await press(digit);
  assert.equal(calls, 1);
  await act(async () => resolve({ aliasVictima: 'Pablo', carta: { aliasObjetivo: 'Marta', mision: 'Saluda' } }));
  tree.unmount();
});

function rejectedError(code, message, details) {
  const error = new Error(message);
  Object.setPrototypeOf(error, ApiError.prototype);
  error.code = code;
  error.details = details;
  return error;
}

const ApiError = class ApiError extends Error {};

test('Eliminar muestra el rechazo y los intentos que devuelve el servidor', async () => {
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    ...common,
    '../api/client': { ApiError },
    '../api/game': { getMyMissionCard: async () => ({ intentosRestantes: 3, bloqueadoHasta: null }), eliminateTarget: async () => { throw rejectedError('CODIGO_INCORRECTO', 'El código no es el de tu objetivo', { intentosRestantes: 2 }); } },
  });
  let tree;
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: { goBack() {}, replace() {} }, route: { params: { gameId: 'p1' } } })); });
  for (const digit of '111111') {
    const key = tree.root.findAllByType('Pressable').find((node) => node.props.accessibilityLabel === `Introducir ${digit}`);
    await act(async () => key.props.onPress());
  }
  const shown = JSON.stringify(tree.toJSON());
  assert.match(shown, /El código no es el de tu objetivo/);
  assert.match(shown, /Te quedan 2 intentos/);
  await act(async () => tree.unmount());
});

test('Eliminar explica el horario lectivo sin descontar intentos', async () => {
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    ...common,
    '../api/client': { ApiError },
    '../api/game': { getMyMissionCard: async () => ({ intentosRestantes: 2, bloqueadoHasta: null }), eliminateTarget: async () => { throw rejectedError('HORARIO_LECTIVO', 'No disponible', { motivo: 'OBJETIVO', disponibleEn: new Date(Date.now() + 80_000).toISOString() }); } },
  });
  let tree;
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: { goBack() {}, replace() {} }, route: { params: { gameId: 'p1', targetAlias: 'Pablo', attemptsLeft: 2 } } })); });
  for (const digit of '111111') {
    const key = tree.root.findAllByType('Pressable').find((node) => node.props.accessibilityLabel === `Introducir ${digit}`);
    await act(async () => key.props.onPress());
  }
  const shown = JSON.stringify(tree.toJSON());
  assert.match(shown, /Pablo está en clase/);
  assert.match(shown, /Te quedan 2 intentos/);
  await act(async () => tree.unmount());
});

test('Eliminar bloquea el teclado hasta la fecha que devuelve el servidor', async () => {
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    ...common,
    '../api/client': { ApiError },
    '../api/game': { getMyMissionCard: async () => ({ intentosRestantes: 3, bloqueadoHasta: null }), eliminateTarget: async () => { throw rejectedError('INTENTOS_BLOQUEADOS', 'Los intentos están bloqueados hasta que rote el código', { bloqueadoHasta: new Date(Date.now() + 60_000).toISOString() }); } },
  });
  let tree;
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: { goBack() {}, replace() {} }, route: { params: { gameId: 'p1' } } })); });
  for (const digit of '111111') {
    const key = tree.root.findAllByType('Pressable').find((node) => node.props.accessibilityLabel === `Introducir ${digit}`);
    await act(async () => key.props.onPress());
  }
  const shown = JSON.stringify(tree.toJSON());
  assert.match(shown, /INTENTOS BLOQUEADOS/);
  assert.match(shown, /Podrás volver a intentarlo en/);
  const digit = tree.root.findAllByType('Pressable').find((node) => node.props.accessibilityLabel === 'Introducir 1');
  assert.equal(digit.props.disabled, true);
  await act(async () => tree.unmount());
});

test('Eliminar usa los intentos frescos aunque la carta anterior dijera otra cosa', async () => {
  let sent = false;
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    ...common,
    '../api/client': { ApiError },
    '../api/game': { getMyMissionCard: async () => ({ intentosRestantes: 1, bloqueadoHasta: null }), eliminateTarget: async () => { sent = true; } },
  });
  let tree;
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: { goBack() {}, replace() {} }, route: { params: { gameId: 'p1', attemptsLeft: 3, blockedUntil: null } } })); });
  const shown = JSON.stringify(tree.toJSON());
  assert.match(shown, /Te queda 1 intento/);
  assert.equal(/Te quedan 3 intentos/.test(shown), false);
  assert.equal(sent, false);
  await act(async () => tree.unmount());
});

test('Eliminar se abre bloqueado si la carta trae bloqueadoHasta y no deja escribir', async () => {
  let sent = false;
  const blockedUntil = new Date(Date.now() + 45_000).toISOString();
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    ...common,
    '../api/client': { ApiError },
    '../api/game': { getMyMissionCard: async () => ({ intentosRestantes: 3, bloqueadoHasta: blockedUntil }), eliminateTarget: async () => { sent = true; } },
  });
  let tree;
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: { goBack() {}, replace() {} }, route: { params: { gameId: 'p1', attemptsLeft: 3, blockedUntil } } })); });
  const shown = JSON.stringify(tree.toJSON());
  assert.match(shown, /INTENTOS BLOQUEADOS/);
  assert.match(shown, /Podrás volver a intentarlo en/);
  const digit = tree.root.findAllByType('Pressable').find((node) => node.props.accessibilityLabel === 'Introducir 1');
  assert.equal(digit.props.disabled, true);
  await act(async () => digit.props.onPress());
  assert.equal(sent, false);
  await act(async () => tree.unmount());
});

test('Eliminar no deja escribir mientras espera el estado fresco de la carta', async () => {
  let resolveCard;
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    ...common,
    '../api/client': { ApiError },
    '../api/game': { getMyMissionCard: () => new Promise((resolve) => { resolveCard = resolve; }), eliminateTarget: async () => { throw new Error('no debe enviar'); } },
  });
  let tree;
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: { goBack() {}, replace() {} }, route: { params: { gameId: 'p1', attemptsLeft: 3 } } })); });
  const digit = tree.root.findAllByType('Pressable').find((node) => node.props.accessibilityLabel === 'Introducir 1');
  assert.equal(digit.props.disabled, true);
  assert.match(JSON.stringify(tree.toJSON()), /COMPROBANDO INTENTOS/);
  await act(async () => resolveCard({ intentosRestantes: 2, bloqueadoHasta: null }));
  assert.match(JSON.stringify(tree.toJSON()), /Te quedan 2 intentos/);
  await act(async () => tree.unmount());
});

test('Eliminar ofrece REINTENTAR si no puede consultar la carta y vuelve a pedirla', async () => {
  let cardCalls = 0;
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    ...common,
    '../api/client': { ApiError },
    '../api/game': {
      getMyMissionCard: async () => { cardCalls += 1; if (cardCalls === 1) throw new Error('sin conexión'); return { intentosRestantes: 2, bloqueadoHasta: null }; },
      eliminateTarget: async () => { throw new Error('no debe enviar'); },
    },
  });
  let tree;
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: { goBack() {}, replace() {} }, route: { params: { gameId: 'p1', attemptsLeft: 3 } } })); });
  const digitDisabled = () => tree.root.findAllByType('Pressable').find((node) => node.props.accessibilityLabel === 'Introducir 1').props.disabled === true;
  const retry = () => tree.root.findAllByType('Pressable').find((node) => node.findAllByType('Text').some((text) => text.props.children === 'REINTENTAR'));
  assert.equal(digitDisabled(), true);
  assert.equal(retry() !== undefined, true);
  await act(async () => retry().props.onPress());
  assert.equal(cardCalls, 2);
  assert.match(JSON.stringify(tree.toJSON()), /Te quedan 2 intentos/);
  assert.equal(digitDisabled(), false);
  assert.equal(retry() === undefined, true);
  await act(async () => tree.unmount());
});

test('Eliminar no enseña cero intentos si eliminan al jugador con esa pantalla abierta', async () => {
  const destinations = [];
  const { EliminateScreen } = load('screens/EliminateScreen.tsx', {
    ...common,
    '../api/client': { ApiError },
    '../api/game': { getMyMissionCard: async () => ({ estado: 'ELIMINADO', intentosRestantes: null, bloqueadoHasta: null }), eliminateTarget: async () => { throw new Error('no debe enviar'); } },
  });
  let tree;
  await act(async () => { tree = create(React.createElement(EliminateScreen, { navigation: { goBack() {}, replace: (...args) => destinations.push(args) }, route: { params: { gameId: 'p1' } } })); });
  assert.deepEqual(destinations, [['Caught', { gameId: 'p1' }]]);
  assert.doesNotMatch(JSON.stringify(tree.toJSON()), /Te quedan 0 intentos/);
  await act(async () => tree.unmount());
});
