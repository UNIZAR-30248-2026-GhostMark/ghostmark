/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');

const storage = new Map();
const secureStore = {
  getItemAsync: async (key) => storage.get(key) ?? null,
  setItemAsync: async (key, value) => { storage.set(key, value); },
  deleteItemAsync: async (key) => { storage.delete(key); },
};
function loadModule(name, imports) {
  const source = fs.readFileSync(path.join(__dirname, '../src/api', name + '.ts'), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)((id) => imports[id], module, module.exports);
  return module.exports;
}
const client = loadModule('client', {});
const auth = loadModule('auth', { './client': client, 'expo-secure-store': secureStore });
const account = { id: '1', alias: 'test', email: 'test@example.test', ubicacionAceptada: true, avisoDisponibilidadAceptado: true };
const session = (tokenAcceso, tokenRefresco, expires) => ({ tokenAcceso, tokenRefresco, accesoCaducaEn: expires, cuenta: account });
const future = new Date(Date.now() + 600000).toISOString();
const past = new Date(Date.now() - 600000).toISOString();
let refreshes = 0;
let closedToken;
global.fetch = async (url, options) => {
  const endpoint = new URL(url).pathname;
  if (endpoint === '/api/v1/sesiones/refresco') {
    refreshes++;
    await new Promise((resolve) => setTimeout(resolve, 10));
    return Response.json(session('new-access', 'new-refresh', future));
  }
  if (endpoint === '/api/v1/sesiones/cierre') {
    closedToken = JSON.parse(options.body).tokenRefresco;
    return new Response(null, { status: 204 });
  }
  return options.headers.get('Authorization') === 'Bearer new-access'
    ? Response.json({ ok: true }) : new Response(null, { status: 403 });
};

async function main() {
  await auth.saveSession(session('expired', 'old-refresh', past));
  const results = await Promise.all([client.request('/private'), client.request('/private')]);
  assert.deepEqual(results, [{ ok: true }, { ok: true }]);
  assert.equal(refreshes, 1, 'concurrent requests share one refresh');
  assert.equal(storage.get('ghostmark_refresh_token'), 'new-refresh');
  assert.equal(auth.needsAccountConsents(), false);
  await auth.signOut();
  assert.equal(closedToken, 'new-refresh');
  assert.equal(storage.size, 0);
  await auth.saveSession(session('valid', 'old-refresh', future));
  await assert.rejects(client.request('/private'), (error) => error.status === 403);
  assert.equal(refreshes, 1, 'a genuine 403 does not trigger a refresh');
  await auth.clearSession();
  console.log('Session tests passed');
}
main().catch((error) => { console.error(error); process.exitCode = 1; });
