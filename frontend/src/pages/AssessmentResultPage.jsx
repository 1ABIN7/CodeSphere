import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { assessmentAPI } from '../api';

export default function AssessmentResultPage() {
  const { id } = useParams();
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const res = await assessmentAPI.getResult(id);
        if (active) setResult(res.data || null);
      } catch {
        if (active) setError('Result details are not available yet.');
      } finally {
        if (active) setLoading(false);
      }
    };

    load();
    return () => { active = false; };
  }, [id]);

  if (loading) return <div className="container fade-in"><div className="card empty-state"><div className="spinner" /></div></div>;
  if (error) return <div className="container fade-in"><div className="card empty-state"><div className="empty-title">Result pending</div><div className="empty-subtitle">{error}</div></div></div>;

  return (
    <div className="container fade-in" style={{ paddingBottom: 60 }}>
      <div className="card" style={{ padding: 24, marginBottom: 20 }}>
        <div className="badge badge-tag">Assessment result</div>
        <h1 style={{ fontSize: 28, fontWeight: 800, marginTop: 8 }}>{result?.title || 'Assessment'}</h1>
        <div style={{ marginTop: 12, fontSize: 16, color: 'var(--text-secondary)' }}>
          {result?.status === 'PENDING' ? 'Your evaluation is still pending.' : `Score: ${result?.score ?? 0}`}
        </div>
      </div>

      <div className="card" style={{ padding: 24 }}>
        <h2 style={{ fontSize: 18, fontWeight: 700, marginBottom: 14 }}>Section breakdown</h2>
        {(result?.sections || []).length > 0 ? result.sections.map((section) => (
          <div key={section.name} className="assessment-meta-grid" style={{ marginBottom: 12 }}>
            <div>{section.name}</div>
            <div>{section.score ?? 'Pending'}</div>
          </div>
        )) : <div className="empty-subtitle">No section details are available yet from the backend.</div>}
      </div>
    </div>
  );
}
