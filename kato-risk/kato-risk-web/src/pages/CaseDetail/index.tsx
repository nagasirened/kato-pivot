import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Card, Descriptions, Tag, Spin } from 'antd';
import { getCase } from '../../api/risk';

const actionColor = (a: string) => {
  if (a === 'PASS') return 'success';
  if (a === 'BLOCK') return 'error';
  return 'warning';
};

export default function CaseDetail() {
  const { id } = useParams();
  const { data: caseItem, isLoading } = useQuery({
    queryKey: ['case', id],
    queryFn: () => getCase(Number(id)),
  });

  if (isLoading) return <Spin />;

  return (
    <Card title={`事件详情 #${id}`}>
      <Descriptions column={2} bordered>
        <Descriptions.Item label="用户ID">{caseItem?.userId}</Descriptions.Item>
        <Descriptions.Item label="场景">
          <Tag color="blue">{caseItem?.scene}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="决策">
          <Tag color={actionColor(caseItem?.action || '')}>{caseItem?.action}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="风险分">{caseItem?.riskScore}</Descriptions.Item>
        <Descriptions.Item label="请求ID">{caseItem?.requestId}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag color={caseItem?.status === 'HANDLED' ? 'success' : 'default'}>
            {caseItem?.status}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="处理人">{caseItem?.handler || '-'}</Descriptions.Item>
        <Descriptions.Item label="处理时间">{caseItem?.handleTime || '-'}</Descriptions.Item>
        <Descriptions.Item label="命中规则" span={2}>
          {caseItem?.reasonCodes || '-'}
        </Descriptions.Item>
        <Descriptions.Item label="创建时间">{caseItem?.createdAt}</Descriptions.Item>
      </Descriptions>
    </Card>
  );
}