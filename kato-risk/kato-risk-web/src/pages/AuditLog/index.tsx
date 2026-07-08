import { useQuery } from '@tanstack/react-query';
import { Table, Card, Tag, Space } from 'antd';
import { useState } from 'react';
import { getAuditLogs } from '../../api/risk';

const operationColor = (op: string) => {
  if (op === 'CREATE') return 'success';
  if (op === 'DELETE') return 'error';
  if (op === 'UPDATE') return 'blue';
  return 'default';
};

export default function AuditLog() {
  const [ruleId, setRuleId] = useState<number | undefined>();

  const { data, isLoading } = useQuery({
    queryKey: ['audit-logs', ruleId],
    queryFn: () => getAuditLogs({ ruleId, size: 100 }),
  });

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '规则ID', dataIndex: 'ruleId', width: 80 },
    {
      title: '操作',
      dataIndex: 'operation',
      render: (op: string) => <Tag color={operationColor(op)}>{op}</Tag>,
    },
    { title: '操作人', dataIndex: 'operator' },
    { title: '操作时间', dataIndex: 'operateAt' },
    {
      title: '变更内容',
      render: (_: unknown, record: { oldContent?: string; newContent?: string }) => (
        <span>
          {record.oldContent && <span style={{ color: '#cf1322' }}>删: {record.oldContent.substring(0, 30)}...</span>}
          {record.newContent && <span style={{ color: '#3f8600' }}> 增: {record.newContent.substring(0, 30)}...</span>}
        </span>
      ),
    },
  ];

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        <input
          type="number"
          placeholder="规则ID"
          onChange={(e) => setRuleId(e.target.value ? Number(e.target.value) : undefined)}
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