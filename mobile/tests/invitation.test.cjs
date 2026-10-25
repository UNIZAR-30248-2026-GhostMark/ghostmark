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
const accountValidation = load('utils/accountValidation.ts');
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
test('normaliza minúsculas en enlaces válidos', () => {
  assert.equal(invitation.parseInvitationLink('ghostmark://unirse/abcdefgh'), code);
});
test('rechaza esquemas, rutas, longitudes y caracteres ajenos al contrato', () => {
  for (const url of ['https://unirse/ABCDEFGH', 'ghostmark://otra/ABCDEFGH', 'ghostmark://unirse/ABCDEFG', 'ghostmark://unirse/ABCDEFG0', 'ghostmark://unirse/ABCDEFGI', 'ghostmark://unirse/ABCDEFGL', 'ghostmark://unirse/ABCDEFGH/extra', link + '?token=secret']) {
    assert.equal(invitation.parseInvitationLink(url), undefined, url);
  }
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
test('el QR y copiar usan el enlace del servidor; ir a la partida lleva su código', async () => {
  let copied, destination;
  const serverLink = 'ghostmark://unirse/23456789';
  const { ShareGameScreen } = load('screens/ShareGameScreen.tsx', { ...common,
    '@expo/vector-icons': { Ionicons: 'Ionicons' },
    'expo-clipboard': { setStringAsync: async (value) => { copied = value; return true; } },
    'react-native-qrcode-svg': { __esModule: true, default: 'QRCode' },
  });
  await mount(ShareGameScreen, { navigation: { replace: (...args) => { destination = args; } }, route: { params: { game: { ...game, enlace: serverLink } } } });
  assert.equal(tree.root.findByType('QRCode').props.value, serverLink);
  assert.equal(tree.root.findByType('GameWizardHeader').props.step, 3);
  const buttons = tree.root.findAllByType('Pressable');
  assert.equal(buttons.length, 1, 'sin «Compartir invitación» (R9-5)');
  await act(async () => buttons[0].props.onPress());
  assert.equal(copied, serverLink);
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
test('el nombre de la partida empieza vacío, con «Killer Bienvenida EINA» como ejemplo', async () => {
  let destination;
  const { CreateGameDataScreen } = load('screens/CreateGameDataScreen.tsx', { ...common,
    'react-native': { ...native, KeyboardAvoidingView: 'KeyboardAvoidingView', Modal: 'Modal', Platform: { OS: 'android' }, TextInput: 'TextInput' },
  });
  await mount(CreateGameDataScreen, { navigation: { navigate: (...args) => { destination = args; } } });
  const input = tree.root.findByProps({ accessibilityLabel: 'Nombre de la partida' });
  assert.equal(input.props.value, '');
  assert.equal(input.props.placeholder, 'Killer Bienvenida EINA');
  await act(async () => tree.root.findByType('PrimaryButton').props.onPress());
  assert.match(JSON.stringify(tree.toJSON()), /El nombre de la partida debe tener al menos 3 caracteres\./);
  assert.equal(destination, undefined);
  await act(async () => tree.root.findByProps({ accessibilityLabel: 'Nombre de la partida' }).props.onChangeText('Killer EINA'));
  await act(async () => tree.root.findByType('PrimaryButton').props.onPress());
  assert.equal(destination[0], 'CreateGameRules');
  assert.equal(destination[1].game.nombre, 'Killer EINA');
});

test('el alta muestra alias, email y contraseña desde el principio, sin «Usar email»', async () => {
  let registered;
  const { WelcomeScreen } = load('screens/WelcomeScreen.tsx', { ...common,
    'react-native': { ...native, Keyboard: { addListener: () => ({ remove() {} }) }, KeyboardAvoidingView: 'KeyboardAvoidingView', Platform: { OS: 'android' } },
    '../api/auth': { registerAccount: async (value) => { registered = value; return {}; }, saveSession: async () => undefined },
    '../components/FormTextInput': { FormTextInput: 'FormTextInput' },
    '../utils/accountValidation': accountValidation,
  });
  await mount(WelcomeScreen, { navigation: { replace() {}, navigate() {} } });
  const fields = tree.root.findAllByType('FormTextInput').map((field) => field.props.label);
  assert.deepEqual(fields, ['Tu alias', 'Email', 'Contraseña']);
  assert.doesNotMatch(JSON.stringify(tree.toJSON()), /USAR EMAIL/);
  const continueButton = tree.root.findByType('PrimaryButton');
  assert.equal(continueButton.props.disabled, false);
  await act(async () => continueButton.props.onPress());
  assert.match(JSON.stringify(tree.toJSON()), /De 3 a 20: letras sin tildes, números y guion bajo/);
  assert.equal(registered, undefined, 'la validación local no llama al servidor');
  const [alias, email, password] = tree.root.findAllByType('FormTextInput');
  await act(async () => { alias.props.onChangeText('fantasmilla'); email.props.onChangeText('ana@example.com'); password.props.onChangeText('Prueba-1234'); });
  await act(async () => tree.root.findByType('PrimaryButton').props.onPress());
  assert.deepEqual(registered, { alias: 'fantasmilla', email: 'ana@example.com', contrasena: 'Prueba-1234' });
});

test('el alta muestra un 409 sin campo debajo del formulario', async () => {
  const { WelcomeScreen } = load('screens/WelcomeScreen.tsx', { ...common,
    'react-native': { ...native, Keyboard: { addListener: () => ({ remove() {} }) }, KeyboardAvoidingView: 'KeyboardAvoidingView', Platform: { OS: 'android' } },
    '../api/auth': {
      registerAccount: async () => { throw new client.ApiError('Ese alias ya está en uso', 409, [], 'ALIAS_OCUPADO'); },
      saveSession: async () => undefined,
    },
    '../components/FormTextInput': { FormTextInput: 'FormTextInput' },
    '../utils/accountValidation': accountValidation,
  });
  await mount(WelcomeScreen, { navigation: { replace() {}, navigate() {} } });
  const [alias, email, password] = tree.root.findAllByType('FormTextInput');
  await act(async () => { alias.props.onChangeText('fantasmilla'); email.props.onChangeText('ana@example.com'); password.props.onChangeText('Prueba-1234'); });
  await act(async () => tree.root.findByType('PrimaryButton').props.onPress());
  assert.equal(tree.root.findAllByType('FormTextInput')[0].props.error, undefined);
  assert.match(JSON.stringify(tree.toJSON()), /Ese alias ya está en uso/);
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
  for (const screen of ['ApiStatus', 'AvailabilityConsent', 'CreateGameData', 'CreateGameRules', 'GameInvitation', 'GameNorms', 'WaitingRoom', 'ShareGame', 'ScanInvitation', 'Home', 'LocationConsent', 'Profile', 'SignIn', 'Schedule', 'Welcome']) {
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
    currentRoute() { return route.name; },
  };
}

test('sin sesión la app abre en «Iniciar sesión»', async () => {
  const app = await mountNavigator({ signedIn: false });
  assert.equal(app.currentRoute(), 'SignIn');
});

test('desde «Iniciar sesión», «Crear cuenta» abre el alta', async () => {
  let destination;
  const { SignInScreen } = load('screens/SignInScreen.tsx', { ...common,
    'react-native': { ...native, Keyboard: { addListener: () => ({ remove() {} }) }, KeyboardAvoidingView: 'KeyboardAvoidingView', Platform: { OS: 'android' } },
    '../api/auth': { login: async () => undefined, nextAccountScreen: () => 'Home', saveSession: async () => undefined },
    '../components/FormTextInput': { FormTextInput: 'FormTextInput' },
    '../utils/accountValidation': accountValidation,
  });
  await mount(SignInScreen, { navigation: { navigate: (name) => { destination = name; }, goBack: () => { destination = 'atrás'; } } });
  const link = tree.root.findAll((node) => node.type === 'Pressable' && node.props.accessibilityRole === 'link')[0];
  await act(async () => link.props.onPress());
  assert.equal(destination, 'Welcome');
});

test('desde el alta, «¿Ya tienes cuenta? Inicia sesión» vuelve a «Iniciar sesión» sin apilar otra', async () => {
  const calls = [];
  const { WelcomeScreen } = load('screens/WelcomeScreen.tsx', { ...common,
    'react-native': { ...native, Keyboard: { addListener: () => ({ remove() {} }) }, KeyboardAvoidingView: 'KeyboardAvoidingView', Platform: { OS: 'android' } },
    '../api/auth': { registerAccount: async () => ({}), saveSession: async () => undefined },
    '../components/FormTextInput': { FormTextInput: 'FormTextInput' },
    '../utils/accountValidation': accountValidation,
  });
  await mount(WelcomeScreen, { navigation: { navigate: (name) => { calls.push(['navigate', name]); }, popTo: (name) => { calls.push(['popTo', name]); }, replace() {} } });
  const link = tree.root.findAll((node) => node.type === 'Pressable' && node.props.accessibilityRole === 'link')[0];
  await act(async () => link.props.onPress());
  assert.deepEqual(calls, [['popTo', 'SignIn']]);
});

test('cerrar sesión y borrar la cuenta llevan a «Iniciar sesión»', async () => {
  const resets = [];
  const { ProfileScreen } = load('screens/ProfileScreen.tsx', { ...common,
    'react-native': { ...native, Alert: { alert: (_title, _message, buttons) => buttons[1].onPress() }, AppState: { addEventListener: () => ({ remove() {} }) }, Linking: { openSettings: async () => undefined } },
    '@expo/vector-icons': { Ionicons: 'Ionicons' },
    '@react-navigation/native': { useFocusEffect: (effect) => React.useEffect(effect, [effect]) },
    'expo-location': { getForegroundPermissionsAsync: async () => ({ granted: true, canAskAgain: true }) },
    '../api/account': { deleteAccount: async () => undefined },
    '../api/auth': { clearSession: async () => undefined, getCurrentAccount: () => ({ alias: 'ana' }), signOut: async () => undefined },
  });
  await mount(ProfileScreen, { navigation: { reset: (value) => { resets.push(value.routes[0].name); }, navigate() {} } });
  const pressable = (label) => tree.root.findAll((node) => node.type === 'Pressable' && node.findAllByType('Text').some((text) => text.props.children === label))[0];
  await act(async () => pressable('Cerrar sesión').props.onPress());
  await act(async () => pressable('BORRAR MI CUENTA').props.onPress());
  assert.deepEqual(resets, ['SignIn', 'SignIn']);
});

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

function scanner(permission) {
  let requested = 0, settingsOpened = 0, focused = true;
  const replacements = [];
  const { ScanInvitationScreen } = load('screens/ScanInvitationScreen.tsx', { ...common,
    '@expo/vector-icons': { Ionicons: 'Ionicons' },
    '@react-navigation/native': { useIsFocused: () => focused },
    'expo-camera': {
      CameraView: 'CameraView',
      useCameraPermissions: () => [permission, async () => { requested++; }],
    },
    'react-native': { ...native,
      Linking: { openSettings: async () => { settingsOpened++; } },
      StyleSheet: { ...native.StyleSheet, absoluteFill: {}, absoluteFillObject: {} },
    },
  });
  return {
    ScanInvitationScreen,
    props: { navigation: { replace: (...args) => replacements.push(args), goBack() {} } },
    replacements,
    requested: () => requested,
    settingsOpened: () => settingsOpened,
    setPermission: (value) => { permission = value; },
    setFocused: (value) => { focused = value; },
  };
}

test('escáner rechaza QR ajeno y abre la partida una sola vez con un enlace válido', async () => {
  const app = scanner({ granted: true, canAskAgain: true });
  await mount(app.ScanInvitationScreen, app.props);
  const camera = tree.root.findByType('CameraView');
  await act(async () => camera.props.onBarcodeScanned({ data: 'https://example.test/' }));
  assert.match(JSON.stringify(tree.toJSON()), /no contiene una invitación válida/);
  assert.deepEqual(app.replacements, []);
  await act(async () => camera.props.onBarcodeScanned({ data: 'ghostmark://unirse/abcdefgh' }));
  await act(async () => camera.props.onBarcodeScanned({ data: link }));
  assert.deepEqual(app.replacements, [['GameInvitation', { code }]]);
});

test('escáner pide permiso al entrar y ofrece ajustes si se deniega definitivamente', async () => {
  const app = scanner({ granted: false, canAskAgain: true });
  await mount(app.ScanInvitationScreen, app.props);
  assert.equal(tree.root.findAllByType('CameraView').length, 0);
  await act(async () => tree.root.findAllByType('Pressable').find((button) => button.findAllByType('Text').some((label) => label.props.children === 'PERMITIR CÁMARA')).props.onPress());
  assert.equal(app.requested(), 1);
  app.setPermission({ granted: false, canAskAgain: false });
  await act(async () => tree.update(React.createElement(app.ScanInvitationScreen, app.props)));
  await act(async () => tree.root.findAllByType('Pressable').find((button) => button.findAllByType('Text').some((label) => label.props.children === 'ABRIR AJUSTES')).props.onPress());
  assert.equal(app.settingsOpened(), 1);
});

test('escáner detiene la cámara cuando pierde el foco y muestra un fallo de cámara', async () => {
  const app = scanner({ granted: true, canAskAgain: true });
  await mount(app.ScanInvitationScreen, app.props);
  app.setFocused(false);
  await act(async () => tree.update(React.createElement(app.ScanInvitationScreen, app.props)));
  assert.equal(tree.root.findAllByType('CameraView').length, 0);
  app.setFocused(true);
  await act(async () => tree.update(React.createElement(app.ScanInvitationScreen, app.props)));
  await act(async () => tree.root.findByType('CameraView').props.onMountError());
  assert.match(JSON.stringify(tree.toJSON()), /No se ha podido abrir la cámara/);
});

test('Unirse desde Inicio abre el escáner', async () => {
  const destinations = [];
  const { HomeScreen } = load('screens/HomeScreen.tsx', { ...common,
    '@expo/vector-icons': { Ionicons: 'Ionicons' },
    '../api/game': { getMyGames: async () => [] },
  });
  await mount(HomeScreen, { navigation: { navigate: (name) => destinations.push(name) }, route: { params: undefined } });
  const join = tree.root.findAllByType('Pressable').find((button) => button.findAllByType('Text').some((label) => label.props.children === 'UNIRSE'));
  act(() => join.props.onPress());
  assert.deepEqual(destinations, ['ScanInvitation']);
});
