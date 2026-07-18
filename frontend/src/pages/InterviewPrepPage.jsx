import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { interviewAPI } from '../api';
import { toast } from 'react-hot-toast';

export default function InterviewPrepPage() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchCategories();
  }, []);

  const fetchCategories = async () => {
    try {
      const { data } = await interviewAPI.getCategories();
      setCategories(data);
    } catch (err) {
      toast.error('Failed to load categories');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const startSession = async (categoryId) => {
    try {
      toast.loading('Preparing your session...', { id: 'prep' });
      const { data } = await interviewAPI.startSession({
        sessionType: 'PRACTICE',
        categoryIds: [categoryId],
        totalQuestions: 10
      });
      toast.dismiss('prep');
      navigate(`/interview/session/${data.sessionId}`);
    } catch (err) {
      toast.dismiss('prep');
      toast.error('Failed to start session');
      console.error(err);
    }
  };

  const getCategoryIcon = (name) => {
    switch (name) {
      case 'TECHNICAL': return '💻';
      case 'APTITUDE': return '🧮';
      case 'LOGICAL': return '🧩';
      case 'GRAMMAR': return '📝';
      case 'BEHAVIORAL': return '🤝';
      default: return '📚';
    }
  };

  return (
    <div className="container fade-in">
      <div className="hero" style={{ minHeight: 'auto', padding: '60px 0 40px' }}>
        <h1 className="hero-title">
          <span className="hero-gradient">Interview Prep</span>
        </h1>
        <p className="hero-subtitle">
          Master every aspect of the technical interview process. Practice curated questions across core categories to build confidence and hone your skills.
        </p>
        <div className="hero-actions">
          <button 
            onClick={() => navigate('/interview/performance')}
            className="btn btn-secondary btn-lg"
          >
            View My Performance
          </button>
        </div>
      </div>

      {loading ? (
        <div className="loading-center">
          <div className="spinner" />
        </div>
      ) : (
        <div className="category-grid">
          {categories.map((cat, i) => (
            <div 
              key={cat.id}
              className="category-card slide-up"
              style={{ animationDelay: `${i * 0.1}s`, animationFillMode: 'both' }}
            >
              <div className="category-accent" />
              <div className="category-icon">
                {getCategoryIcon(cat.name)}
              </div>
              <h3 className="category-title">{cat.displayName}</h3>
              <p className="category-desc">{cat.description}</p>
              
              <button 
                onClick={() => startSession(cat.id)}
                className="category-action"
              >
                Practice Now →
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
