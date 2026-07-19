import { useState } from 'react';
import { Upload, FileText, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

export default function QuestionImportPage() {
  const [file, setFile] = useState(null);

  const handleImport = () => {
    if (!file) { toast.error('Select a CSV or JSON file first'); return; }
    // TODO: backend endpoint pending — see AssessmentQuestionController (bulk import)
    toast.error('Import endpoint not yet available');
  };

  return (
    <div className="fade-in">
      <h1 style={{ fontSize: '24px', fontWeight: 800, marginBottom: 8 }}>Import Questions</h1>
      <p className="text-secondary" style={{ marginBottom: 24 }}>Bulk-import questions from a CSV or JSON file</p>

      <div className="card" style={{ padding: 32, textAlign: 'center' }}>
        <Upload size={48} style={{ color: 'var(--text-muted)', marginBottom: 16 }} />
        <p style={{ marginBottom: 16 }}>Drag a file here or click to browse</p>
        <input
          type="file"
          accept=".csv,.json"
          onChange={(e) => setFile(e.target.files?.[0] || null)}
          style={{ marginBottom: 16 }}
        />
        {file && (
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 16 }}>
            <FileText size={14} style={{ verticalAlign: -2 }} /> {file.name}
          </p>
        )}
        <button className="btn btn-primary" onClick={handleImport} disabled={!file}>
          <Upload size={16} /> Import
        </button>
      </div>

      <div style={{ marginTop: 24, padding: 16, background: 'var(--bg-elevated)', borderRadius: 'var(--radius-sm)', display: 'flex', gap: 12 }}>
        <AlertCircle size={18} style={{ color: 'var(--accent)', flexShrink: 0, marginTop: 2 }} />
        <div style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
          <strong>Supported formats:</strong> CSV with headers (title, type, difficulty, content, options) or JSON array of question objects. Max 500 questions per import.
        </div>
      </div>
    </div>
  );
}
