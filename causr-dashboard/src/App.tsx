import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { AnomaliesPage } from './pages/AnomaliesPage';
import { DashboardPage } from './pages/DashboardPage';
import { LogsPage } from './pages/LogsPage';
import { TraceLookupPage } from './pages/TraceLookupPage';
import { TracePage } from './pages/TracePage';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="logs" element={<LogsPage />} />
          <Route path="anomalies" element={<AnomaliesPage />} />
          <Route path="traces" element={<TraceLookupPage />} />
          <Route path="traces/:traceId" element={<TracePage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
