import React, { useState } from 'react';
import { LoginPage } from './components/LoginPage';
import { EvaluationPanel } from './components/EvaluationPanel';
import type { Role } from './types';

interface SessionInfo {
  formId: string;
  userName: string;
  role: Role;
}

const App: React.FC = () => {
  const [session, setSession] = useState<SessionInfo | null>(null);

  const handleLogin = (formId: string, userName: string, role: Role) => {
    setSession({ formId, userName, role });
  };

  if (!session) {
    return <LoginPage onLogin={handleLogin} />;
  }

  return (
    <EvaluationPanel
      formId={session.formId}
      userName={session.userName}
      role={session.role}
    />
  );
};

export default App;
