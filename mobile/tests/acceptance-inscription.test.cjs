/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test, afterEach } = require('node:test');
const ts = require('typescript');
const React = require('react');
const { act, create } = require('react-test-renderer');

global.IS_REACT_ACT_ENVIRONMENT = true;

const originalFetch = global.fetch;
const code = 'ABCDEFGH';
const publicGame = {
  nombre: 'Aceptacion PBI 3', estado: 'EN_INSCRIPCION', aliasMaster: 'master-real',
  inicioEn: '2099-11-01T10:00:00Z', finEn: '2099-11-02T10:00:00Z', numInscritos: 1,
};
let tree;

afterEach(async () => {
  try {
    if (tree) await act(async () => tree.unmount());
  } finally {
    tree = undefined;
    global.fetch = originalFetch;
  }
});

// Se ejecutan las pantallas y el cliente HTTP reales. Solo se sustituyen el
// transporte HTTP, el navegador y los componentes nativos que Node no ofrece.
function app() {
  const calls = [];
  const destinations = [];
  const cache = new Map();
  const native = {
    View: 'View', Text: 'Text', Pressable: 'Pressable', Image: 'Image',
    ScrollView: 'ScrollView', ActivityIndicator: 'ActivityIndicator',
    StyleSheet: { create: (styles) => styles, absoluteFill: {}, absoluteFillObject: {} },
    useWindowDimensions: () => ({ width: 390, height: 844 }),
    Linking: { openSettings: async () => {} },
  };
  const substitutions = {
    'react-native': native,
    '@expo/vector-icons': { Ionicons: 'Ionicons' },
    '@react-navigation/native': { useIsFocused: () => true },
    'expo-camera': {
      CameraView: 'CameraView',
      useCameraPermissions: () => [{ granted: true, canAskAgain: true }, async () => {}],
    },
  };
  const sourceRoot = path.resolve(__dirname, '../src');

  function load(file) {
    const absolute = path.resolve(sourceRoot, file);
    if (cache.has(absolute)) return cache.get(absolute).exports;
    const source = fs.readFileSync(absolute, 'utf8');
    const compiled = ts.transpileModule(source, { compilerOptions: {
      module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true,
    } }).outputText;
    const module = { exports: {} };
    cache.set(absolute, module);
    new Function('require', 'module', 'exports', compiled)((id) => {
      if (id in substitutions) return substitutions[id];
      if (id.endsWith('.png')) return 1;
      if (id.endsWith('/components/Screen')) return { Screen: 'Screen' };
      if (id.endsWith('/components/PrimaryButton')) return { PrimaryButton: 'PrimaryButton' };
      if (id.startsWith('.')) {
        const resolved = path.resolve(path.dirname(absolute), id);
        const target = [resolved, `${resolved}.ts`, `${resolved}.tsx`].find((name) => fs.existsSync(name));
        assert.ok(target, `No se ha encontrado el módulo real ${id}`);
        return load(path.relative(sourceRoot, target));
      }
      return require(id);
    }, module, module.exports);
    return module.exports;
  }

  global.fetch = async (url, options = {}) => {
    const pathname = new URL(url).pathname;
    const method = options.method ?? 'GET';
    const body = options.body === undefined ? undefined : JSON.parse(options.body);
    calls.push({ pathname, method, body });
    assert.equal(new Headers(options.headers).get('Authorization'), 'Bearer test-pbi3');
    if (method === 'GET' && pathname === `/api/v1/partidas/codigo/${code}`) {
      return Response.json(publicGame);
    }
    assert.fail(`Petición fuera del contrato del escenario: ${method} ${pathname}`);
  };

  load('api/client.ts').setAccessToken('test-pbi3');
  const navigation = {
    replace: (name, params) => destinations.push({ name, params }),
    navigate: (name, params) => destinations.push({ name, params }),
    goBack: () => destinations.push({ name: 'Back' }),
    reset: (state) => destinations.push({ name: 'Reset', state }),
  };

  async function show(name, params) {
    const Component = load(`screens/${name}Screen.tsx`)[`${name}Screen`];
    if (tree) await act(async () => tree.unmount());
    await act(async () => { tree = create(React.createElement(Component, { navigation, route: { params } })); });
  }

  return {
    calls, destinations, show, load,
    joins: () => calls.filter((call) => call.method === 'POST' && call.pathname === '/api/v1/inscripciones'),

  };
}

function text() {
  // Extrae texto visible, sin depender de estilos o de cómo se anidan los View.
  return tree.root.findAllByType('Text').map((node) => node.props.children).flat(Infinity).join(' ');
}

function primary(label) {
  return tree.root.findAllByType('PrimaryButton').find((button) => button.props.children === label);
}

async function press(button) {
  assert.ok(button, 'El botón debe estar presente');
  await act(async () => button.props.onPress());
}

async function openNorms(application, source) {
  if (source === 'QR') {
    await application.show('ScanInvitation');
    await act(async () => tree.root.findByType('CameraView').props.onBarcodeScanned({
      data: `ghostmark://unirse/${code}`,
    }));
    assert.deepEqual(application.destinations.pop(), { name: 'GameInvitation', params: { code } });
  } else {
    assert.equal(application.load('utils/invitation.ts').parseInvitationLink(`ghostmark://unirse/${code}`), code);
  }
  await application.show('GameInvitation', { code });
  await press(primary('VER NORMAS'));
  const destination = application.destinations.pop();
  assert.equal(destination?.name, 'GameNorms');
  await application.show(destination.name, destination.params);
}

test('CA2 - muestra las cuatro normas antes de aceptar', async () => {
  const application = app();
  await openNorms(application, 'enlace');
  for (const rule of ['Sin contacto físico', 'Sin objetos que simulen armas', 'Zonas y horarios seguros', 'Puedes abandonar cuando quieras']) {
    assert.ok(text().includes(rule), rule);
  }
  assert.equal(application.joins().length, 0);
});

test('CA2 - volver sin aceptar no inscribe ni anuncia que el jugador se ha unido', async () => {
  const application = app();
  await openNorms(application, 'enlace');
  await press(tree.root.findByProps({ accessibilityLabel: 'Volver a la invitación' }));
  assert.equal(application.joins().length, 0);
  assert.deepEqual(application.destinations, [{ name: 'Back' }]);
});

// CA1 se comprueba por capas: aquí QR/enlace -> invitación -> normas;
// AceptacionInscripcionTest comprueba alta real -> lista del máster por HTTP.
// Estos casos no simulan una inscripción ni acreditan el flujo completo de #57.
for (const source of ['QR', 'enlace']) {
  test(`CA1 - entrar por ${source} consulta la partida y permite revisar sus normas`, async () => {
    const application = app();
    await openNorms(application, source);
    assert.deepEqual(application.calls, [{
      pathname: `/api/v1/partidas/codigo/${code}`, method: 'GET', body: undefined,
    }]);
    assert.ok(text().includes(publicGame.nombre.toUpperCase()));
    assert.ok(primary('ACEPTO Y ME UNO'), 'La aceptación debe ser una acción explícita');
    assert.equal(application.joins().length, 0, 'Consultar la invitación no inscribe');
    assert.deepEqual(application.destinations, [], 'No se abre la sala antes de aceptar');
  });
}
