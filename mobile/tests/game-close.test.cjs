/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');

const source = (name) => fs.readFileSync(path.join(__dirname, '../src', name), 'utf8');

test('el cierre usa el endpoint de la partida autenticada', () => {
  const client = source('api/game.ts');
  assert.match(client, /partidas\/\$\{encodeURIComponent\(gameId\)\}\/cierre/);
  assert.match(client, /method: "POST"/);
});

test('la pestaña Partida solo muestra el cierre al máster y abre el final', () => {
  const screen = source('screens/GameTabScreen.tsx');
  assert.match(screen, /game\?\.soyMaster/);
  assert.match(screen, /CERRAR PARTIDA/);
  assert.match(screen, /navigation\.replace\("GameEnded"/);
});

test('la pantalla final muestra duración, jugadores y vuelve a Inicio', () => {
  const screen = source('screens/GameEndedScreen.tsx');
  for (const text of ['durationText', 'jugadores', 'días', 'VOLVER A MIS PARTIDAS']) assert.match(screen, new RegExp(text));
  assert.match(screen, /name: "Home"/);
});

test('Inicio identifica la partida cerrada y abre su pantalla de final', () => {
  const screen = source('screens/HomeScreen.tsx');
  assert.match(screen, /game\.estado === "CERRADA"/);
  assert.match(screen, /Cerrada/);
});
