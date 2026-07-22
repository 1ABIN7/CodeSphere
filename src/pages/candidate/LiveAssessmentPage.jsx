import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { AlertTriangle, Clock, Terminal, Send } from 'lucide-react';
import toast from 'react-hot-toast';

export default function LiveAssessmentPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [timeLeft, setTimeLeft] = useState(3600); // 60 mins in seconds
  const [warnings, setWarnings] = useState(0);

  // Timer logic
  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          handleSubmit();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Format time (MM:SS)
  const formatTime = (seconds) => {
    const m = Math.floor(seconds / 60).toString().padStart(2, '0');
    const s = (seconds % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  // Visibility / Anti-Cheat Detection
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden) {
        setWarnings(prev => {
          const newWarnings = prev + 1;
          toast.error(`Warning ${newWarnings}/3: Tab switch detected!`, { duration: 5000 });
          if (newWarnings >= 3) {
            toast.error("Assessment terminated due to multiple violations.");
            navigate('/dashboard'); // Kick out
          }
          return newWarnings;
        });
      }
    };
    
    document.addEventListener("visibilitychange", handleVisibilityChange);
    return () => document.removeEventListener("visibilitychange", handleVisibilityChange);
  }, [navigate]);

  const handleSubmit = () => {
    toast.success("Assessment submitted successfully!");
    navigate('/dashboard');
  };

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Top Proctoring Bar */}
      <div style={{ 
        height: '56px', background: 'var(--bg-secondary)', borderBottom: '1px solid var(--border)',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 24px'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--red)', fontWeight: 600, fontSize: '13px' }}>
            <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--red)', boxShadow: '0 0 8px var(--red)' }} />
            LIVE PROCTORING ACTIVE
          </div>
          {warnings > 0 && (
            <span style={{ fontSize: '12px', color: 'var(--yellow)', display: 'flex', alignItems: 'center', gap: '4px' }}>
              <AlertTriangle size={14} /> {warnings}/3 Warnings
            </span>
          )}
        </div>
        
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontFamily: 'JetBrains Mono', fontSize: '18px', fontWeight: 700, color: timeLeft < 300 ? 'var(--red)' : 'var(--text-primary)' }}>
            <Clock size={18} /> {formatTime(timeLeft)}
          </div>
          <button className="btn btn-primary btn-sm" onClick={handleSubmit}>
            <Send size={14} /> Submit Final
          </button>
        </div>
      </div>

      {/* Main Split Interface */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', flex: 1, overflow: 'hidden' }}>
        {/* Left Side: Question Content */}
        <div style={{ borderRight: '1px solid var(--border)', padding: '32px', overflowY: 'auto' }}>
          <div className="badge badge-easy" style={{ marginBottom: '16px' }}>Question 1 of 5</div>
          <h2 style={{ fontSize: '24px', fontWeight: 700, marginBottom: '16px' }}>Two Sum</h2>
          <div className="prose">
            <p>Given an array of integers <code>nums</code> and an integer <code>target</code>, return indices of the two numbers such that they add up to <code>target</code>.</p>
            <p>You may assume that each input would have exactly one solution, and you may not use the same element twice. You can return the answer in any order.</p>
            
            <h3>Example 1:</h3>
            <pre>
Input: nums = [2,7,11,15], target = 9
Output: [0,1]
Explanation: Because nums[0] + nums[1] == 9, we return [0, 1].
            </pre>
          </div>
        </div>

        {/* Right Side: IDE / Answer Area */}
        <div style={{ display: 'flex', flexDirection: 'column', background: '#1e1e1e' }}>
          <div style={{ padding: '8px 16px', background: '#252526', display: 'flex', gap: '12px', borderBottom: '1px solid #333' }}>
            <select style={{ background: '#333', color: '#fff', border: 'none', padding: '4px 8px', borderRadius: '4px' }}>
              <option>Java</option>
              <option>Python</option>
              <option>C++</option>
            </select>
          </div>
          
          <div style={{ flex: 1, padding: '16px', color: '#d4d4d4', fontFamily: 'JetBrains Mono, monospace', fontSize: '14px', whiteSpace: 'pre-wrap' }}>
{`class Solution {
    public int[] twoSum(int[] nums, int target) {
        // Write your code here
        
    }
}`}
          </div>
          
          <div style={{ height: '200px', background: '#1e1e1e', borderTop: '1px solid #333', padding: '12px' }}>
            <div style={{ fontSize: '12px', color: '#888', textTransform: 'uppercase', display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
              <Terminal size={12} /> Test Case Output
            </div>
            <div style={{ color: '#aaa', fontSize: '13px', fontStyle: 'italic' }}>Run code to see output here...</div>
          </div>
        </div>
      </div>
    </div>
  );
}
