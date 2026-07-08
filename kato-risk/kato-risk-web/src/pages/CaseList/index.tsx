import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Table, Select, Tag, Button, Space, Card } from 'antd';
import { useNavigate } from 'react-router-dom';
import { getCases } from '../../api/risk';
import type { RiskCase } from '../../api/risk';

const SCENE_OPTIONS = ['REGISTER', 'LOGIN', 'MARKETING', 'ORDER', 'PAYMENT', 'REFUND'];
const ACTION_OPTIONS = ['PASS', 'REVIEW', 'BLOCK'];

export default function CaseList() {
  const navigate = useNavigate();
  const [scene, setScene] = useState('');
  const [action, setAction] = useState('');
  const [userId, setUserId] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['cases', { scene, action, userId }],
    queryFn: () => getCases({ scene, action, userId, size: 100 }),
  });

  const actionColor = (a: string) => {
    if (a === 'PASS') return 'success';
    if (a === 'BLOCK') return 'error';
    return 'warning';
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '用户ID', dataIndex: 'userId' },
    { title: '场景', dataIndex: 'scene', render: (s: string) => <Tag color="blue">{s}</Tag> },
    { title: '决策', dataIndex: 'action', render: (a: string) => <Tag color={actionColor(a)}>{a}</Tag> },
    { title: '风险分', dataIndex: 'riskScore' },
    { title: '请求ID', dataIndex: 'requestId' },
    { title: '创建时间', dataIndex: 'createdAt' },
    {
      title: '操作',
      render: (_: unknown, record: RiskCase) => (
        <Button size="small" onClick={() => navigate(`/cases/${record.id}`)}>
          详情
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Select
          placeholder="场景"
          allowClear
          style={{ width: 120 }}
          onChange={(v) => setScene(v)}
          options={SCENE_OPTIONS.map((s) => ({ label: s, value: s }))}
        />
        <Select
          placeholder="决策"
          allowClear
          style={{ width: 100 }}
          onChange={(v) => setAction(v)}
          options={ACTION_OPTIONS.map((a) => ({ label: a, value: a }))}
        />
        <input
          placeholder="用户ID"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          style={{ padding: '4px 8px', border: '1px solid #d9d9d9', borderRadius: 4 }}
        />
      </Space>

      <Card>
        <Table
          columns={columns}
          dataSource={data?.records || []}
          rowKey="id"
          loading={isLoading}
          pagination={{ total: data?.total, pageSize: 20 }}
        />
      </Card>
    </div>
  );
}