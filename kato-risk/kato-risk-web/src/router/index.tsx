import { createBrowserRouter, Outlet } from 'react-router-dom';
import Dashboard from '../pages/Dashboard';
import RuleList from '../pages/RuleList';
import RuleEdit from '../pages/RuleEdit';
import ScriptTest from '../pages/ScriptTest';
import CaseList from '../pages/CaseList';
import CaseDetail from '../pages/CaseDetail';
import AuditLog from '../pages/AuditLog';
import SceneConfig from '../pages/SceneConfig';
import Layout from '../components/Layout';

const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout><Outlet /></Layout>,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'rules', element: <RuleList /> },
      { path: 'rules/new', element: <RuleEdit /> },
      { path: 'rules/:id/edit', element: <RuleEdit /> },
      { path: 'rules/test', element: <ScriptTest /> },
      { path: 'cases', element: <CaseList /> },
      { path: 'cases/:id', element: <CaseDetail /> },
      { path: 'audit-logs', element: <AuditLog /> },
      { path: 'scene-config', element: <SceneConfig /> },
    ],
  },
]);

export default router;