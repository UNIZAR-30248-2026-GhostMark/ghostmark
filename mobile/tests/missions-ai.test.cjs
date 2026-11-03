/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { afterEach, test } = require('node:test');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');

global.IS_REACT_ACT_ENVIRONMENT = true;
global.requestAnimationFrame = (callback) => callback();

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

// Las animaciones terminan en el acto y el gesto se puede lanzar a mano.
class AnimatedValue { constructor(value) { this.value = value; } setValue(value) { this.value = value; } interpolate() { return this; } }
const finished = { start: (callback) => callback?.({ finished: true }) };
let pan;
const common = {
  'react-native': {
    Animated: { Value: AnimatedValue, View: 'AnimatedView', delay: () => finished, parallel: () => finished, sequence: () => finished, spring: () => finished, timing: () => finished },
    Image: 'Image', PanResponder: { create: (config) => { pan = config; return { panHandlers: {} }; } },
    Pressable: 'Pressable', ScrollView: 'ScrollView', StyleSheet: { create: (styles) => styles }, Text: 'Text', TextInput: 'TextInput', View: 'View',
  },
  '../api/client': { ApiError: class ApiError extends Error { constructor(message) { super(message); this.fieldErrors = []; } } },
  '../components/GameWizardHeader': { GameWizardHeader: 'GameWizardHeader' },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../components/Screen': { Screen: 'Screen' },
  '../theme/bangers': { bangers: () => ({}) },
  '../theme/colors': { colors: { border: '', danger: '', ink: '', lime: '', surfaceRaised: '', text: '', textMuted: '', white: '' } },
};

const configuration = { nombre: 'Partida IA', inicioEn: '2026-11-10T10:00:00Z', finEn: '2026-11-17T10:00:00Z', entradasTardias: false, masterJuega: true, plazoDisputaHoras: 12 };
const game = { ...configuration, id: 'p1', tipoMisiones: 'IA', estado: 'EN_INSCRIPCION', codigoInvitacion: 'ABC123', enlace: 'https://x', numInscritos: 1, creadaEn: '2026-11-03T10:00:00Z' };
const mission = (id, texto) => ({ id, texto, estado: 'PROPUESTA', tanda: 1 });
const batch = (misiones, tandasRestantes = 4) => ({ tanda: 1, misiones, descartadas: 0, tandasRestantes });

let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; });
const shown = () => JSON.stringify(tree.toJSON());
const pressable = (label) => tree.root.findAll((node) => node.type === 'Pressable' && (node.props.accessibilityLabel === label || JSON.stringify(node.findAllByType('Text').map((text) => text.props.children)).includes(label)))[0];
const press = async (node) => act(async () => node.props.onPress());

function missionsStep(api) {
  const navigation = { goBacks: 0, resets: [], goBack() { this.goBacks += 1; }, reset(value) { this.resets.push(value); } };
  const { CreateGameMissionsScreen } = load('screens/CreateGameMissionsScreen.tsx', { ...common, '../api/game': api });
  return { CreateGameMissionsScreen, navigation };
}

test('Misiones con IA crea la partida una vez y pide la tanda con el chip elegido', async () => {
  const created = [];
  const asked = [];
  const { CreateGameMissionsScreen, navigation } = missionsStep({
    createGame: async (value) => { created.push(value); return game; },
    requestAiMissionBatch: async (...args) => { asked.push(args); return batch([mission('m1', 'Que te diga su asignatura favorita')]); },
  });
  await act(async () => { tree = create(React.createElement(CreateGameMissionsScreen, { navigation, route: { params: { configuration } } })); });
  assert.equal(tree.root.findByType('GameWizardHeader').props.step, 3);
  for (const chip of ['Campus', 'Fiesta de bienvenida', 'Semana de exámenes', 'Otro…']) assert.match(shown(), new RegExp(chip));
  await press(pressable('Semana de exámenes'));
  await press(tree.root.findByType('PrimaryButton'));
  assert.deepEqual(created, [{ ...configuration, tipoMisiones: 'IA' }]);
  assert.deepEqual(asked, [['p1', 'Semana de exámenes']]);
  assert.equal(navigation.resets[0].routes[1].name, 'ReviewAiMissions');
  assert.equal(navigation.resets[0].routes[1].params.contexto, 'Semana de exámenes');
});

test('«Otro…» pide un contexto de al menos 3 caracteres antes de crear nada', async () => {
  let created = 0;
  const { CreateGameMissionsScreen, navigation } = missionsStep({ createGame: async () => { created += 1; return game; }, requestAiMissionBatch: async () => batch([]) });
  await act(async () => { tree = create(React.createElement(CreateGameMissionsScreen, { navigation, route: { params: { configuration } } })); });
  await press(pressable('Otro…'));
  await act(async () => tree.root.findByType('TextInput').props.onChangeText('ab'));
  await press(tree.root.findByType('PrimaryButton'));
  assert.match(shown(), /al menos 3 caracteres/);
  assert.equal(created, 0);
});

test('si falla la tanda, reintentar o pasar a Biblioteca no crea otra partida', async () => {
  const { ApiError } = common['../api/client'];
  let created = 0;
  let attempts = 0;
  const { CreateGameMissionsScreen, navigation } = missionsStep({
    createGame: async () => { created += 1; return game; },
    requestAiMissionBatch: async () => { attempts += 1; throw new ApiError('La IA no responde'); },
  });
  await act(async () => { tree = create(React.createElement(CreateGameMissionsScreen, { navigation, route: { params: { configuration } } })); });
  await press(tree.root.findByType('PrimaryButton'));
  assert.match(shown(), /La IA no responde/);
  await press(tree.root.findByType('PrimaryButton'));
  assert.equal(attempts, 2);
  await press(pressable('BIBLIOTECA'));
  await press(tree.root.findByType('PrimaryButton'));
  assert.equal(created, 1);
  assert.equal(navigation.resets.at(-1).routes[1].name, 'ShareGame');
  assert.equal(navigation.resets.at(-1).routes[1].params.game.id, 'p1');
  // Volver atrás con la partida creada lleva a Inicio, no a Reglas.
  tree.root.findByType('GameWizardHeader').props.onBack();
  assert.equal(navigation.goBacks, 0);
  assert.equal(navigation.resets.at(-1).routes[0].name, 'Home');
});

test('Biblioteca crea la partida con las misiones de la biblioteca y abre Compartir', async () => {
  const created = [];
  const { CreateGameMissionsScreen, navigation } = missionsStep({ createGame: async (value) => { created.push(value); return { ...game, tipoMisiones: 'BIBLIOTECA' }; }, requestAiMissionBatch: async () => assert.fail('no pide tanda') });
  await act(async () => { tree = create(React.createElement(CreateGameMissionsScreen, { navigation, route: { params: { configuration } } })); });
  await press(pressable('BIBLIOTECA'));
  await press(tree.root.findByType('PrimaryButton'));
  assert.deepEqual(created, [{ ...configuration, tipoMisiones: 'BIBLIOTECA' }]);
  assert.equal(navigation.resets[0].routes[1].name, 'ShareGame');
});

function reviewScreen(initial, api) {
  const navigation = { destinations: [], goBack() {}, navigate(...args) { this.destinations.push(args); } };
  const { ReviewAiMissionsScreen } = load('screens/ReviewAiMissionsScreen.tsx', { ...common, '../api/game': api });
  return { navigation, element: React.createElement(ReviewAiMissionsScreen, { navigation, route: { params: { batch: initial, contexto: 'Campus', game } } }) };
}

test('la pila aprueba con ✓ o deslizando a la derecha y rechaza con ✗ o deslizando a la izquierda', async () => {
  const decisions = [];
  const { element } = reviewScreen(batch([mission('m1', 'Primera misión'), mission('m2', 'Segunda misión'), mission('m3', 'Tercera misión'), mission('m4', 'Cuarta misión')]), {
    reviewAiMission: async (...args) => { decisions.push(args); return {}; },
    requestAiMissionBatch: async () => assert.fail('no pide tanda'),
  });
  await act(async () => { tree = create(element); });
  assert.match(shown(), /"1"," de ","4"/);
  assert.match(shown(), /Primera misión/);
  await press(pressable('Aprobar misión'));
  await press(pressable('Rechazar misión'));
  await act(async () => pan.onPanResponderRelease({}, { dx: 120 }));
  await act(async () => pan.onPanResponderRelease({}, { dx: 40 }));
  await act(async () => pan.onPanResponderRelease({}, { dx: -120 }));
  assert.deepEqual(decisions, [['p1', 'm1', 'APROBADA'], ['p1', 'm2', 'RECHAZADA'], ['p1', 'm3', 'APROBADA'], ['p1', 'm4', 'RECHAZADA']]);
  assert.match(shown(), /TANDA REVISADA/);
  assert.match(shown(), /"Has aprobado ","2"," misión","es"/);
});

test('al acabar la tanda se puede pedir otra mientras queden y continuar a Compartir', async () => {
  const asked = [];
  const { element, navigation } = reviewScreen(batch([mission('m1', 'Primera misión')], 1), {
    reviewAiMission: async () => ({}),
    requestAiMissionBatch: async (...args) => { asked.push(args); return { ...batch([mission('m2', 'Misión de la tanda 2')], 0), tanda: 2 }; },
  });
  await act(async () => { tree = create(element); });
  await press(pressable('Rechazar misión'));
  await press(pressable('PEDIR OTRA TANDA (1)'));
  assert.deepEqual(asked, [['p1', 'Campus']]);
  assert.match(shown(), /Misión de la tanda 2/);
  await press(pressable('Aprobar misión'));
  assert.ok(pressable('PEDIR OTRA TANDA') === undefined, 'no debería aparecer PEDIR OTRA TANDA');
  await press(pressable('CONTINUAR'));
  assert.deepEqual(navigation.destinations, [['ShareGame', { game }]]);
});

test('el cliente usa las rutas de misiones de la IA del contrato', async () => {
  const calls = [];
  const { requestAiMissionBatch, reviewAiMission } = load('api/game.ts', { './client': { request: async (...args) => { calls.push(args); return {}; } } });
  await requestAiMissionBatch('p 1', 'Campus');
  await reviewAiMission('p 1', 'm 1', 'APROBADA');
  assert.deepEqual(calls, [
    ['/api/v1/partidas/p%201/misiones-ia/tandas', { body: { contexto: 'Campus' }, method: 'POST' }],
    ['/api/v1/partidas/p%201/misiones-ia/m%201', { body: { estado: 'APROBADA' }, method: 'PATCH' }],
  ]);
});
