import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import ExamContainer from './components/exam/ExamContainer';
import PostSubmitScreen from './components/exam/PostSubmitScreen';

const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Navigate to="/exam/placeholder" replace />} />
        <Route path="/exam/:examId" element={<ExamContainer />} />
        <Route path="/exam/:examId/post-submit" element={<PostSubmitScreen />} />
      </Routes>
    </Router>
  );
};

export default App;
