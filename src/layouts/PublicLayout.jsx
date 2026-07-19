import { Outlet } from 'react-router-dom';
import Navbar from '../components/Navbar';

export default function PublicLayout() {
  return (
    <div className="app">
      <Navbar />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
