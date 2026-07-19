import { motion, AnimatePresence } from 'framer-motion';
import { X } from 'lucide-react';
import { useEffect } from 'react';

export default function Modal({ isOpen, onClose, title, children, footer }) {
  // Prevent body scrolling when modal is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = 'unset';
    }
    return () => { document.body.style.overflow = 'unset'; };
  }, [isOpen]);

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            className="modal-backdrop"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            style={{
              position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
              background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(4px)', zIndex: 999
            }}
          />
          <div className="modal-container" style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            zIndex: 1000, pointerEvents: 'none', padding: '20px'
          }}>
            <motion.div
              className="modal-content card"
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              transition={{ type: 'spring', damping: 25, stiffness: 300 }}
              style={{
                pointerEvents: 'auto', width: '100%', maxWidth: '500px',
                background: 'var(--bg-card)', border: '1px solid var(--border-bright)',
                boxShadow: 'var(--shadow)', display: 'flex', flexDirection: 'column',
                maxHeight: '90vh'
              }}
            >
              <div className="modal-header" style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '20px', borderBottom: '1px solid var(--border)'
              }}>
                <h3 style={{ fontSize: '18px', fontWeight: 700, margin: 0 }}>{title}</h3>
                <button onClick={onClose} className="btn-ghost btn-icon" style={{ cursor: 'pointer', border: 'none', background: 'transparent' }}>
                  <X size={20} />
                </button>
              </div>
              
              <div className="modal-body" style={{ padding: '20px', overflowY: 'auto' }}>
                {children}
              </div>
              
              {footer && (
                <div className="modal-footer" style={{
                  padding: '16px 20px', borderTop: '1px solid var(--border)',
                  display: 'flex', justifyContent: 'flex-end', gap: '12px', background: 'var(--bg-secondary)'
                }}>
                  {footer}
                </div>
              )}
            </motion.div>
          </div>
        </>
      )}
    </AnimatePresence>
  );
}
