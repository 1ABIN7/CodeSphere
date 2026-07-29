/**
 * Derives a display-only countdown from the server's authoritative timestamp.
 * Keeping it pure also makes the refresh/resume behavior straightforward to
 * test without a browser.
 */
export function getSectionSecondsRemaining(section, session, now = Date.now()) {
  const duration = Number(section?.durationMinutes || 0);
  if (!duration) return null;
  const startedAt = session?.currentSectionStartedAt;
  if (!startedAt) return duration * 60;
  const elapsed = Math.max(0, Math.floor((now - new Date(startedAt).getTime()) / 1000));
  return Math.max(0, duration * 60 - elapsed);
}
