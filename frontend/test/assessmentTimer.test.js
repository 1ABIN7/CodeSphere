import test from 'node:test';
import assert from 'node:assert/strict';
import { getSectionSecondsRemaining } from '../src/utils/assessmentTimer.js';

test('resuming a section keeps the server-established deadline', () => {
  const startedAt = '2026-07-28T20:00:00.000Z';
  const afterRefresh = Date.parse('2026-07-28T20:07:15.000Z');

  assert.equal(
    getSectionSecondsRemaining({ durationMinutes: 10 }, { currentSectionStartedAt: startedAt }, afterRefresh),
    165,
  );
});

test('a section countdown never becomes negative', () => {
  assert.equal(
    getSectionSecondsRemaining(
      { durationMinutes: 1 },
      { currentSectionStartedAt: '2026-07-28T20:00:00.000Z' },
      Date.parse('2026-07-28T20:02:00.000Z'),
    ),
    0,
  );
});

test('untimed sections do not render a section countdown', () => {
  assert.equal(getSectionSecondsRemaining({ durationMinutes: null }, {}), null);
});
