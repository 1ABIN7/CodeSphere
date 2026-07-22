import { useEffect, useRef } from 'react';

// TODO: backend endpoint pending — see ProctoringController
export default function BehaviorTracker({ sessionId, onAnomalyDetected }) {
  const eventsRef = useRef([]);

  useEffect(() => {
    const push = (eventType, severity) => {
      eventsRef.current.push({
        eventType,
        severity,
        timestamp: new Date().toISOString(),
        sessionId,
      });
      if (onAnomalyDetected) {
        onAnomalyDetected(eventType, severity);
      }
    };

    const onVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        push('TAB_SWITCH', 'HIGH');
      }
    };

    const onBlur = () => {
      push('WINDOW_BLUR', 'MEDIUM');
    };

    const onClipboard = (e) => {
      const eventType = e.type === 'paste' ? 'CLIPBOARD_PASTE' : 'CLIPBOARD_COPY';
      push(eventType, 'HIGH');
    };

    document.addEventListener('visibilitychange', onVisibilityChange);
    window.addEventListener('blur', onBlur);
    document.addEventListener('copy', onClipboard);
    document.addEventListener('cut', onClipboard);
    document.addEventListener('paste', onClipboard);

    const intervalId = setInterval(async () => {
      if (eventsRef.current.length === 0) return;
      const batch = eventsRef.current.splice(0);
      try {
        // TODO: backend endpoint pending — see ProctoringController
        await fetch('/api/v1/proctoring/events', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ sessionId, events: batch }),
        });
      } catch {
        // re-queue on failure
        eventsRef.current.unshift(...batch);
      }
    }, 30000);

    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange);
      window.removeEventListener('blur', onBlur);
      document.removeEventListener('copy', onClipboard);
      document.removeEventListener('cut', onClipboard);
      document.removeEventListener('paste', onClipboard);
      clearInterval(intervalId);

      // flush remaining events
      if (eventsRef.current.length > 0) {
        const remaining = eventsRef.current.splice(0);
        fetch('/api/v1/proctoring/events', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ sessionId, events: remaining }),
        }).catch(() => {});
      }
    };
  }, [sessionId, onAnomalyDetected]);

  return null;
}
