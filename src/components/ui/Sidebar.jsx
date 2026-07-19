import { Link, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';

export default function Sidebar({ items, userRole }) {
  const location = useLocation();

  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <div className="brand-icon">⚡</div>
        <span className="sidebar-title">CodeSphere</span>
      </div>
      <div className="sidebar-role-badge">
        {userRole?.replace('ROLE_', '')}
      </div>
      <nav className="sidebar-nav">
        {items.map((item) => {
          const isActive = location.pathname === item.path || location.pathname.startsWith(item.path + '/');
          return (
            <Link key={item.path} to={item.path} className={`sidebar-link ${isActive ? 'active' : ''}`}>
              <span className="sidebar-icon">{item.icon}</span>
              {item.label}
              {isActive && (
                <motion.div
                  className="sidebar-active-indicator"
                  layoutId="sidebarActiveIndicator"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ duration: 0.2 }}
                />
              )}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
