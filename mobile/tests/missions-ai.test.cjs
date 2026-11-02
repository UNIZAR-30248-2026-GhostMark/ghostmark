/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');

const source = (name) => fs.readFileSync(path.join(__dirname, '../src', name), 'utf8');

test('el paso Misiones ofrece Biblioteca, IA y los cuatro contextos', () => {
  const screen = source('screens/CreateGameMissionsScreen.tsx');
  for (const text of ['BIBLIOTECA', 'GENERAR CON IA', 'Campus', 'Fiesta de bienvenida', 'Semana de exámenes', 'Otro…']) assert.match(screen, new RegExp(text));
  assert.match(screen, /step=\{3\}/);
  assert.match(screen, /requestAiMissionBatch/);
});

test('la pila permite aprobar, rechazar y pedir hasta cinco tandas mediante la API', () => {
  const screen = source('screens/ReviewAiMissionsScreen.tsx');
  assert.match(screen, /"APROBADA"/);
  assert.match(screen, /"RECHAZADA"/);
  assert.match(screen, /gesture\.dx > 80/);
  assert.match(screen, /gesture\.dx < -80/);
  assert.match(screen, /tandasRestantes/);
});

test('el cliente usa solo las rutas de misiones IA definidas por el contrato', () => {
  const client = source('api/game.ts');
  assert.match(client, /\/misiones-ia\/tandas/);
  assert.match(client, /\/misiones-ia\/\$\{encodeURIComponent\(missionId\)\}/);
  assert.match(client, /method: "PATCH"/);
});
