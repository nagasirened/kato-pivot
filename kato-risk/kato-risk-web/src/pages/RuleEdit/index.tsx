import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Form, Input, Select, Button, Card, message, Space } from 'antd';
import { getRule, createRule, updateRule } from '../../api/risk';
import type { RuleSaveRequest } from '../../api/risk';

const SCENE_OPTIONS = ['REGISTER', 'LOGIN', 'MARKETING', 'ORDER', 'PAYMENT', 'REFUND'];

export default function RuleEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form] = Form.useForm();
  const isEdit = Boolean(id);

  const { data: rule } = useQuery({
    queryKey: ['rule', id],
    queryFn: () => getRule(Number(id)),
    enabled: isEdit,
  });

  const saveMut = useMutation({
    mutationFn: (data: RuleSaveRequest) => isEdit ? updateRule(Number(id), data) : createRule(data),
    onSuccess: () => {
      message.success('保存成功');
      queryClient.invalidateQueries({ queryKey: ['rules'] });
      navigate('/rules');
    },
    onError: () => message.error('保存失败'),
  });

  return (
    <Card title={isEdit ? '编辑规则' : '新建规则'}>
      <Form form={form} layout="vertical" initialValues={rule || { enabled: true, priority: 99 }} onFinish={(values) => saveMut.mutate({ ...values, createdBy: 'admin' })}>
        <Form.Item name="name" label="规则名称" rules={[{ required: true }]}>
          <Input placeholder="请输入规则名称" />
        </Form.Item>
        <Form.Item name="scene" label="场景" rules={[{ required: true }]}>
          <Select options={SCENE_OPTIONS.map((s) => ({ label: s, value: s }))} />
        </Form.Item>
        <Form.Item name="ruleType" label="规则类型" rules={[{ required: true }]}>
          <Select options={[{ label: '配置规则 (SpEL)', value: 'CONFIG' }, { label: 'Groovy 脚本', value: 'GROOVY' }]} />
        </Form.Item>
        <Form.Item name="priority" label="优先级">
          <Input type="number" placeholder="数字越小越优先" />
        </Form.Item>
        <Form.Item name="content" label="规则内容" rules={[{ required: true }]}>
          <Input.TextArea rows={8} placeholder="SpEL 表达式或 Groovy 脚本" />
        </Form.Item>
        <Form.Item name="abGroup" label="A/B 分组">
          <Input placeholder="如 10%" />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" loading={saveMut.isPending}>保存</Button>
            <Button onClick={() => navigate('/rules')}>取消</Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );
}