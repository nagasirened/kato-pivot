import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Table, Button, Space, Tag, Select, Popconfirm, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { getRules, deleteRule, toggleRule } from '../../api/risk';
import type { RiskRule } from '../../api/risk';

const SCENE_OPTIONS = ['REGISTER', 'LOGIN', 'MARKETING', 'ORDER', 'PAYMENT', 'REFUND'];
const TYPE_OPTIONS = ['CONFIG', 'GROOVY'];

export default function RuleList() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [scene, setScene] = useState('');
  const [ruleType, setRuleType] = useState('');
  const [enabled, setEnabled] = useState<boolean | undefined>(undefined);

  const { data, isLoading } = useQuery({
    queryKey: ['rules', { scene, ruleType, enabled }],
    queryFn: () => getRules({ scene, ruleType, enabled, size: 100 }),
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => deleteRule(id),
    onSuccess: () => {
      message.success('删除成功');
      queryClient.invalidateQueries({ queryKey: ['rules'] });
    },
  });

  const toggleMut = useMutation({
    mutationFn: (params: { id: number; enabled?: boolean }) => toggleRule(params.id, params.enabled),
    onSuccess: () => {
      message.success('操作成功');
      queryClient.invalidateQueries({ queryKey: ['rules'] });
    },
  });

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '规则名称', dataIndex: 'name' },
    { title: '场景', dataIndex: 'scene', render: (scene: string) => <Tag color="blue">{scene}</Tag> },
    { title: '类型', dataIndex: 'ruleType', render: (type: string) => type === 'CONFIG' ? <Tag color="green">配置</Tag> : <Tag color="purple">Groovy</Tag> },
    { title: '优先级', dataIndex: 'priority', width: 80 },
    { title: '状态', dataIndex: 'enabled', render: (enabled: boolean) => enabled ? <Tag color="success">启用</Tag> : <Tag color="default">禁用</Tag> },
    { title: '版本', dataIndex: 'version', width: 60 },
    {
      title: '操作',
      render: (_: unknown, record: RiskRule) => (
        <Space>
          <Button size="small" onClick={() => navigate(`/rules/${record.id}/edit`)}>编辑</Button>
          <Button size="small" type="link" onClick={() => toggleMut.mutate({ id: record.id ?? 0 })}>
            {record.enabled ? '禁用' : '启用'}
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => deleteMut.mutate(record.id ?? 0)}>
            <Button size="small" danger type="link">删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <Select placeholder="场景" allowClear style={{ width: 120 }} onChange={(v) => setScene(v)}
          options={SCENE_OPTIONS.map((s) => ({ label: s, value: s }))} />
        <Select placeholder="类型" allowClear style={{ width: 100 }} onChange={(v) => setRuleType(v)}
          options={TYPE_OPTIONS.map((t) => ({ label: t, value: t }))} />
        <Select placeholder="状态" allowClear style={{ width: 100 }} onChange={(v) => setEnabled(v)}
          options={[{ label: '启用', value: true }, { label: '禁用', value: false }]} />
        <Button type="primary" onClick={() => navigate('/rules/new')}>新建规则</Button>
        <Button onClick={() => navigate('/rules/test')}>脚本测试</Button>
      </Space>

      <Table columns={columns} dataSource={data?.records || []} rowKey="id" loading={isLoading}
        pagination={{ total: data?.total, pageSize: 20 }} />
    </div>
  );
}