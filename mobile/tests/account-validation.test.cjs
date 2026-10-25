/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');
const ts = require('typescript');

function load(file) {
  const source = fs.readFileSync(path.join(__dirname, '../src', file), 'utf8');
  const code = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS } }).outputText;
  const module = { exports: {} };
  new Function('require', 'module', 'exports', code)(require, module, module.exports);
  return module.exports;
}

const validation = load('utils/accountValidation.ts');
const client = load('api/client.ts');

test('el alta valida alias, email y contraseña sin pedir nada al servidor', () => {
  assert.deepEqual(
    validation.validateRegistration(' alias con espacios ', 'correo-invalido', 'corta'),
    {
      alias: 'De 3 a 20: letras sin tildes, números y guion bajo',
      email: 'Debe ser un email válido',
      contrasena: 'Debe tener entre 8 y 72 caracteres',
    },
  );
  assert.deepEqual(validation.validateRegistration('fantasmilla_1', 'ana@example.test', 'Prueba-1234'), {});
});

test('el inicio valida email y contraseña vacía antes de enviarlos', () => {
  assert.deepEqual(validation.validateSignIn('no-es-email', ''), {
    email: 'Debe ser un email válido',
    contrasena: 'Introduce tu contraseña',
  });
  assert.deepEqual(validation.validateSignIn('ana@example.test', 'cualquier-clave'), {});
});

test('el cliente conserva el campo de cada error devuelto por el servidor', async () => {
  global.fetch = async () => Response.json({
    detail: 'Algún campo no cumple sus reglas',
    errores: [
      { campo: 'email', mensaje: 'Debe ser un email válido' },
      { campo: 'contrasena', mensaje: 'Debe tener entre 8 y 72 caracteres' },
    ],
  }, { status: 400 });
  await assert.rejects(client.request('/api/v1/sesiones'), (error) => {
    assert.equal(error.fieldErrorsByName.email, 'Debe ser un email válido');
    assert.equal(error.fieldErrorsByName.contrasena, 'Debe tener entre 8 y 72 caracteres');
    return true;
  });
});
