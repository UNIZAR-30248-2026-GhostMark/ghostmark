/* global __dirname */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { test } = require('node:test');

const root = path.join(__dirname, '../src');
const files = [
  'components/PrimaryButton.tsx',
  'components/DayScheduleModal.tsx',
  'components/WeeklyScheduleGrid.tsx',
  'screens/HomeScreen.tsx',
  'screens/ShareGameScreen.tsx',
  'screens/WaitingRoomScreen.tsx',
  'screens/MissionCardScreen.tsx',
  'screens/GameStartedScreen.tsx',
  'screens/CreateGameRulesScreen.tsx',
  'screens/ProfileScreen.tsx',
  'screens/GameNormsScreen.tsx',
];

test('los relieves usan boxShadow compatible con Android', () => {
  for (const file of files) {
    const source = fs.readFileSync(path.join(root, file), 'utf8');
    assert.match(source, /boxShadow:/, `${file} debe definir un relieve`);
    assert.doesNotMatch(source, /shadowColor|shadowOffset|shadowOpacity|shadowRadius/,
      `${file} no debe usar una sombra exclusiva de web o iOS`);
  }
});
