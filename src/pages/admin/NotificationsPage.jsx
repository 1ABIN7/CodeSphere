import { useState } from 'react';
import { motion } from 'framer-motion';
import { Bell, CheckCheck, Inbox } from 'lucide-react';

// TODO: backend endpoint pending — see NotificationController
const MOCK_NOTIFICATIONS = [
  {
    id: 1,
    title: 'New assessment created',
    message: 'A new assessment "React Fundamentals" has been created and is ready for review.',
    timestamp: '2026-07-19T10:30:00',
    read: false,
  },
  {
    id: 2,
    title: 'Candidate submitted',
    message: 'John Doe has submitted their attempt for "JavaScript Advanced".',
    timestamp: '2026-07-19T09:15:00',
    read: false,
  },
  {
    id: 3,
    title: 'Evaluation completed',
    message: 'Pending evaluation for "Data Structures" has been auto-graded.',
    timestamp: '2026-07-18T16:45:00',
    read: true,
  },
  {
    id: 4,
    title: 'System maintenance',
    message: 'Scheduled maintenance window on July 20th from 2:00 AM to 4:00 AM UTC.',
    timestamp: '2026-07-18T12:00:00',
    read: true,
  },
];

function timeAgo(timestamp) {
  const diff = Date.now() - new Date(timestamp).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState(MOCK_NOTIFICATIONS);

  const markAsRead = (id) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  const markAllAsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1 className="page-title">Notifications</h1>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          {unreadCount > 0 && (
            <span className="badge badge-medium">{unreadCount} unread</span>
          )}
          {unreadCount > 0 && (
            <button className="btn btn-sm btn-secondary" onClick={markAllAsRead}>
              <CheckCheck size={14} /> Mark all as read
            </button>
          )}
        </div>
      </div>

      {notifications.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <Inbox size={48} style={{ color: 'var(--text-secondary)', marginBottom: 16 }} />
            <div className="empty-title">No notifications</div>
            <div className="empty-subtitle">You're all caught up. New notifications will appear here.</div>
          </div>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          {notifications.map((n, i) => (
            <motion.div
              key={n.id}
              className="card"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.05 }}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
                padding: '16px 20px',
                borderLeft: n.read ? undefined : '3px solid var(--accent)',
                background: n.read ? undefined : 'rgba(124, 58, 237, 0.04)',
              }}
            >
              <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start', flex: 1 }}>
                <Bell
                  size={18}
                  style={{
                    color: n.read ? 'var(--text-secondary)' : 'var(--accent-light)',
                    marginTop: 2,
                    flexShrink: 0,
                  }}
                />
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                    <span style={{ fontWeight: n.read ? 500 : 700, fontSize: 14 }}>
                      {n.title}
                    </span>
                    <span style={{ fontSize: 12, color: 'var(--text-secondary)', flexShrink: 0, marginLeft: 12 }}>
                      {timeAgo(n.timestamp)}
                    </span>
                  </div>
                  <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
                    {n.message}
                  </div>
                </div>
              </div>
              {!n.read && (
                <button
                  className="btn btn-sm btn-ghost"
                  onClick={() => markAsRead(n.id)}
                  style={{ marginLeft: 12, flexShrink: 0 }}
                >
                  Mark read
                </button>
              )}
            </motion.div>
          ))}
        </div>
      )}
    </div>
  );
}
