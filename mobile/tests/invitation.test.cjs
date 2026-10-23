/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test, afterEach } = require('node:test');
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
const invitation = load('utils/invitation.ts');
const client = load('api/client.ts');
const api = load('api/game.ts', { './client': client });
const native = {
  View: 'View', Text: 'Text', Pressable: 'Pressable', Image: 'Image', ScrollView: 'ScrollView', ActivityIndicator: 'ActivityIndicator',
  StyleSheet: { create: (styles) => styles }, useWindowDimensions: () => ({ width: 390, height: 844 }),
};
const common = {
  'react-native': native,
  '../components/Screen': { Screen: 'Screen' },
  '../components/PrimaryButton': { PrimaryButton: 'PrimaryButton' },
  '../components/GameWizardHeader': { GameWizardHeader: 'GameWizardHeader' },
  '../theme/colors': { colors: {} },
  '../theme/typography': { typography: {} },
  '../utils/invitation': invitation,
  '../api/client': client,
};
const code = 'ABCDEFGH';
const link = `ghostmark://unirse/${code}`;
const game = { id: 'private-id', codigoInvitacion: code, enlace: link, nombre: 'Partida EINA', estado: 'EN_INSCRIPCION', inicioEn: '2026-11-01T10:00:00Z', finEn: '2026-11-02T10:00:00Z', numInscritos: 0 };
let tree;
afterEach(async () => { if (tree) await act(async () => tree.unmount()); tree = undefined; });
async function mount(component, props) { await act(async () => { tree = create(React.createElement(component, props)); }); }

for (const url of [link, link + '/', 'exp://192.168.1.20:8081/--/unirse/ABCDEFGH', 'exps://example.test/--/unirse/ABCDEFGH']) {
  test(`acepta la invitación ${url}`, () => assert.equal(invitation.parseInvitationLink(url), code));
}
test('rechaza esquemas, rutas, longitudes y caracteres ajenos al contrato', () => {
  for (const url of ['https://unirse/ABCDEFGH', 'ghostmark://otra/ABCDEFGH', 'ghostmark://unirse/ABCDEFG', 'ghostmark://unirse/ABCDEFG0', 'ghostmark://unirse/ABCDEFGI', 'ghostmark://unirse/ABCDEFGL', 'ghostmark://unirse/abcdefgh', 'ghostmark://unirse/ABCDEFGH/extra', link + '?token=secret']) {
    assert.equal(invitation.parseInvitationLink(url), undefined, url);
  }
  assert.throws(() => invitation.invitationLink('bad'));
});
test('conserva la invitación hasta terminar login y permisos y la consume una sola vez', () => {
  const inbox = invitation.createInvitationInbox();
  inbox.receive(link);
  assert.equal(inbox.take(false), undefined);
  inbox.receive('https://example.test');
  assert.equal(inbox.take(false), undefined);
  assert.equal(inbox.take(true), code);
  assert.equal(inbox.take(true), undefined);
  inbox.receive(link);
  inbox.receive('ghostmark://unirse/23456789');
  assert.equal(inbox.take(true), '23456789');
});
test('consulta el contrato de la #48 con autenticación y propaga el 404', async () => {
  client.setAccessToken('test-access');
  global.fetch = async (url, options) => {
    assert.equal(new URL(url).pathname, '/api/v1/partidas/codigo/ABCDEFGH');
    assert.equal(options.headers.get('Authorization'), 'Bearer test-access');
    return Response.json({ ...game, aliasMaster: 'Master' });
  };
  assert.equal((await api.getGameByCode(code)).nombre, game.nombre);
  global.fetch = async () => Response.json({ detail: 'No encontrada' }, { status: 404 });
  await assert.rejects(api.getGameByCode(code), (error) => error instanceof client.ApiError && error.status === 404);
  client.setAccessToken(undefined);
});
test('el QR, copiar y compartir contienen el mismo enlace; ir a la partida lleva su código', async () => {
  let copied, shared, destination;
  const { ShareGameScreen } = load('screens/ShareGameScreen.tsx', { ...common,
    '@expo/vector-icons': { Ionicons: 'Ionicons' },
    'expo-clipboard': { setStringAsync: async (value) => { copied = value; return true; } },
    'react-native-qrcode-svg': { __esModule: true, default: 'QRCode' },
    'react-native': { ...native, Share: { share: async (value) => { shared = value; } } },
  });
  await mount(ShareGameScreen, { navigation: { replace: (...args) => { destination = args; } }, route: { params: { game } } });
  assert.equal(tree.root.findByType('QRCode').props.value, link);
  assert.equal(tree.root.findByType('GameWizardHeader').props.step, 5);
  const buttons = tree.root.findAllByType('Pressable');
  await act(async () => buttons[1].props.onPress());
  assert.equal(copied, link);
  await act(async () => buttons[0].props.onPress());
  assert.equal(shared.message, `Únete a «${game.nombre}» en GhostMark: ${link}`);
  act(() => tree.root.findByType('PrimaryButton').props.onPress());
  assert.deepEqual(destination, ['GameInvitation', { code }]);
});
test('crear una partida abre compartir y retira los formularios del historial', async () => {
  let calls = 0, reset;
  const { CreateGameRulesScreen } = load('screens/CreateGameRulesScreen.tsx', { ...common,
    '../api/game': { createGame: async () => { calls++; return game; } },
  });
  await mount(CreateGameRulesScreen, { navigation: { reset: (value) => { reset = value; } }, route: { params: { game: {} } } });
  await act(async () => tree.root.findByType('PrimaryButton').props.onPress());
  assert.equal(calls, 1);
  assert.deepEqual(reset, {
    index: 1,
    routes: [
      {
        name: 'Home',
        params: {
          createdGame: {
            finEn: game.finEn,
            id: game.id,
            inicioEn: game.inicioEn,
            nombre: game.nombre,
            estado: game.estado,
            numInscritos: game.numInscritos,
            soyMaster: true,
          },
        },
      },
      { name: 'ShareGame', params: { game } },
    ],
  });
});
test('la vista muestra el nombre real de la partida y permite reintentar tras un 404', async () => {
  let calls = 0;
  const { GameInvitationScreen } = load('screens/GameInvitationScreen.tsx', { ...common,
    '../api/game': { getGameByCode: async (value) => {
      assert.equal(value, code);
      if (++calls === 1) throw new client.ApiError('No existe', 404);
      return { ...game, aliasMaster: 'Master' };
    } },
  });
  await mount(GameInvitationScreen, { navigation: {}, route: { params: { code } } });
  assert.match(JSON.stringify(tree.toJSON()), /No existe una partida/);
  await act(async () => tree.root.findAllByType('PrimaryButton')[0].props.onPress());
  assert.equal(calls, 2);
  assert.match(JSON.stringify(tree.toJSON()), /Partida EINA/);
});
test('al abrir dos invitaciones no se muestra la respuesta tardía de la primera', async () => {
  const pending = new Map();
  const { GameInvitationScreen } = load('screens/GameInvitationScreen.tsx', { ...common,
    '../api/game': { getGameByCode: (value) => new Promise((resolve) => pending.set(value, resolve)) },
  });
  await mount(GameInvitationScreen, { navigation: {}, route: { params: { code } } });
  await act(async () => tree.update(React.createElement(GameInvitationScreen, { navigation: {}, route: { params: { code: '23456789' } } })));
  await act(async () => pending.get('23456789')({ ...game, nombre: 'Partida nueva' }));
  await act(async () => pending.get(code)({ ...game, nombre: 'Partida antigua' }));
  assert.match(JSON.stringify(tree.toJSON()), /Partida nueva/);
  assert.doesNotMatch(JSON.stringify(tree.toJSON()), /Partida antigua/);
});

async function mountNavigator({ initialURL = null, signedIn = true, consentRoute = 'Home' } = {}) {
  let listener, ready = false, route = { name: 'Home' }, container;
  const destinations = [];
  let account = signedIn ? { id: '1' } : undefined;
  let nextRoute = consentRoute;
  const navigationRef = {
    isReady: () => ready,
    getCurrentRoute: () => route,
    navigate: (name, params) => { destinations.push([name, params]); route = { name }; },
    reset: () => { route = { name: 'Welcome' }; },
  };
  const imports = { ...common,
    'react-native': { ...native, Alert: { alert() {} }, Button: 'Button', Linking: {
      getInitialURL: async () => initialURL,
      addEventListener: (_event, callback) => { listener = callback; return { remove: () => { listener = undefined; } }; },
    } },
    '@react-navigation/native': {
      createNavigationContainerRef: () => navigationRef,
      DarkTheme: { colors: {} },
      NavigationContainer: (props) => { container = props; return props.children; },
    },
    '@react-navigation/native-stack': { createNativeStackNavigator: () => ({
      Navigator: (props) => { route = { name: props.initialRouteName }; return null; }, Screen: () => null,
    }) },
    '../api/auth': {
      restoreSession: async () => signedIn,
      getCurrentAccount: () => account,
      nextAccountScreen: () => nextRoute,
      setSessionExpiredHandler() {},
    },
  };
  for (const screen of ['ApiStatus', 'AvailabilityConsent', 'CreateGameData', 'CreateGameRules', 'GameInvitation', 'ShareGame', 'Home', 'LocationConsent', 'Profile', 'SignIn', 'Schedule', 'Welcome']) {
    imports[`../screens/${screen}Screen`] = { [`${screen}Screen`]: () => null };
  }
  const { RootNavigator } = load('navigation/RootNavigator.tsx', imports);
  await mount(RootNavigator, {});
  act(() => { ready = true; container.onReady(); });
  return {
    destinations,
    receive(url) { act(() => listener({ url })); },
    arrive(name) { act(() => { route = { name }; container.onStateChange(); }); },
    login() { account = { id: '1' }; },
    consent(name) { nextRoute = name; },
  };
}

test('un enlace con la app cerrada abre la partida tras restaurar sesión y preparar el navegador', async () => {
  const app = await mountNavigator({ initialURL: link });
  assert.deepEqual(app.destinations, [['GameInvitation', { code }]]);
});
test('con la app abierta se pueden abrir dos invitaciones consecutivas', async () => {
  const app = await mountNavigator();
  app.receive(link);
  app.receive('ghostmark://unirse/23456789');
  assert.deepEqual(app.destinations, [['GameInvitation', { code }], ['GameInvitation', { code: '23456789' }]]);
});
test('el enlace espera al login y a los permisos sin perderse ni abrirse dos veces', async () => {
  const app = await mountNavigator({ initialURL: link, signedIn: false, consentRoute: 'LocationConsent' });
  assert.deepEqual(app.destinations, []);
  app.login();
  app.arrive('LocationConsent');
  app.consent('AvailabilityConsent');
  app.arrive('AvailabilityConsent');
  assert.deepEqual(app.destinations, []);
  app.consent('Home');
  app.arrive('Home');
  assert.deepEqual(app.destinations, [['GameInvitation', { code }]]);
  app.arrive('GameInvitation');
  assert.equal(app.destinations.length, 1);
});
