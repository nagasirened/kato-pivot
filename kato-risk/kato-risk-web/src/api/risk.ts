import http from './http';

// ============ 类型定义 ============
export interface RiskRule {
  id?: number;
  name: string;
  scene: string;
  ruleType: string;
  content: string;
  priority: number;
  enabled: boolean;
  version?: number;
  abGroup?: string;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RuleSaveRequest {
  name: string;
  scene: string;
  ruleType: string;
  content: string;
  priority?: number;
  enabled?: boolean;
  abGroup?: string;
  createdBy?: string;
}

export interface GroovyTestRequest {
  scene: string;
  inputContext: Record<string, string>;
  scriptContent: string;
  testCaseName?: string;
}

export interface GroovyTestResult {
  testCaseName: string;
  passed: boolean;
  output: string;
  executionTimeMs: number;
  errorMessage?: string;
}

export interface RiskCase {
  id: number;
  userId: string;
  scene: string;
  requestId: string;
  action: string;
  riskScore: number;
  reasonCodes: string;
  requestContext: string;
  status: string;
  handler?: string;
  handleNote?: string;
  handleTime?: string;
  createdAt: string;
}

export interface RiskRuleAudit {
  id: number;
  ruleId: number;
  operation: string;
  operator: string;
  oldContent?: string;
  newContent?: string;
  operateAt: string;
}

export interface SceneConfig {
  scene: string;
  enabled: boolean;
  defaultAction: string;
}

export interface DashboardStats {
  todayTotal: number;
  todayPassed: number;
  todayBlocked: number;
  todayPending: number;
  passRate: number;
  blockRate: number;
  sceneDistribution: { scene: string; count: number }[];
  trendData: { date: string; passed: number; blocked: number }[];
  topRules: { ruleId: number; ruleName: string; triggerCount: number }[];
}

export interface PageResponse<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// ============ 规则管理 API ============
export const getRules = (params: { page?: number; size?: number; scene?: string; ruleType?: string; enabled?: boolean }) =>
  http.get<PageResponse<RiskRule>>('/risk/rules', { params }).then(res => res as unknown as PageResponse<RiskRule>);

export const getRule = (id: number) =>
  http.get<RiskRule>(`/risk/rules/${id}`).then(res => res as unknown as RiskRule);

export const createRule = (data: RuleSaveRequest) =>
  http.post<RiskRule>('/risk/rules', data).then(res => res as unknown as RiskRule);

export const updateRule = (id: number, data: RuleSaveRequest) =>
  http.put<RiskRule>(`/risk/rules/${id}`, data).then(res => res as unknown as RiskRule);

export const deleteRule = (id: number) => http.delete(`/risk/rules/${id}`);

export const toggleRule = (id: number, enabled?: boolean) =>
  http.put<RiskRule>(`/risk/rules/${id}/toggle`, null, { params: { enabled } }).then(res => res as unknown as RiskRule);

export const testGroovyScript = (data: GroovyTestRequest) =>
  http.post<GroovyTestResult>('/risk/rules/test', data).then(res => res as unknown as GroovyTestResult);

// ============ 事件管理 API ============
export const getCases = (params: { page?: number; size?: number; scene?: string; action?: string; userId?: string; startDate?: string; endDate?: string }) =>
  http.get<PageResponse<RiskCase>>('/risk/cases', { params }).then(res => res as unknown as PageResponse<RiskCase>);

export const getCase = (id: number) =>
  http.get<RiskCase>(`/risk/cases/${id}`).then(res => res as unknown as RiskCase);

export const handleCase = (id: number, handler: string, status?: string, handleNote?: string) =>
  http.put<RiskCase>(`/risk/cases/${id}/handle`, null, { params: { handler, status, handleNote } }).then(res => res as unknown as RiskCase);

// ============ 审核日志 API ============
export const getAuditLogs = (params: { page?: number; size?: number; ruleId?: number }) =>
  http.get<PageResponse<RiskRuleAudit>>('/risk/audit-logs', { params }).then(res => res as unknown as PageResponse<RiskRuleAudit>);

// ============ 仪表盘 API ============
export const getDashboardStats = (params?: { startDate?: string; endDate?: string }) =>
  http.get<DashboardStats>('/risk/stats/dashboard', { params }).then(res => res as unknown as DashboardStats);

// ============ 场景配置 API ============
export const getScenes = () =>
  http.get<Record<string, SceneConfig>>('/risk/scenes').then(res => res as unknown as Record<string, SceneConfig>);

export const toggleScene = (scene: string, enabled?: boolean, defaultAction?: string) =>
  http.put<SceneConfig>(`/risk/scenes/${scene}/toggle`, null, { params: { enabled, defaultAction } }).then(res => res as unknown as SceneConfig);