import { useEffect, useRef, useState } from 'react';
import { Video, VideoOff } from 'lucide-react';

export default function WebcamMonitor({ onPermissionDenied }) {
  const videoRef = useRef(null);
  const streamRef = useRef(null);
  const [enabled, setEnabled] = useState(false);
  const [denied, setDenied] = useState(false);

  const startCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { width: 120, height: 90, facingMode: 'user' },
        audio: false,
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
      setEnabled(true);
      setDenied(false);
    } catch {
      setDenied(true);
      setEnabled(false);
      if (onPermissionDenied) onPermissionDenied();
    }
  };

  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setEnabled(false);
  };

  const toggle = () => {
    if (enabled) {
      stopCamera();
    } else {
      startCamera();
    }
  };

  useEffect(() => {
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => t.stop());
      }
    };
  }, []);

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 16,
        right: 16,
        width: 120,
        height: 90,
        borderRadius: 'var(--radius-sm)',
        overflow: 'hidden',
        border: '1px solid var(--border)',
        background: 'var(--bg-secondary)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 50,
      }}
    >
      <video
        ref={videoRef}
        autoPlay
        muted
        playsInline
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          display: enabled ? 'block' : 'none',
          transform: 'scaleX(-1)',
        }}
      />

      {!enabled && (
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 4,
            color: 'var(--text-secondary)',
            fontSize: 11,
          }}
        >
          <VideoOff size={20} />
          <span>{denied ? 'Camera off' : 'Camera off'}</span>
        </div>
      )}

      <button
        onClick={toggle}
        title={enabled ? 'Disable camera' : 'Enable camera'}
        style={{
          position: 'absolute',
          bottom: 4,
          right: 4,
          width: 24,
          height: 24,
          borderRadius: 4,
          border: 'none',
          background: 'rgba(0,0,0,0.6)',
          color: '#fff',
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 0,
        }}
      >
        {enabled ? <Video size={12} /> : <VideoOff size={12} />}
      </button>
    </div>
  );
}
