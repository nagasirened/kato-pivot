import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Card, Form, Input, Select, Button, Alert, Space, Tag } from 'antd';
import { testGroovyScript } from '../../api/risk';
import type { GroovyTestRequest, GroovyTestResult } from '../../api/risk';

const SCENE_OPTIONS = ['REGISTER', 'LOGIN', 'MARKETING', 'ORDER', 'PAYMENT', 'REFUND'];

export default function ScriptTest() {
  const [result, setResult] = useState<GroovyTestResult | null>(null);
  const [form] = Form.useForm();

  const testMut = useMutation({
    mutationFn: (data: GroovyTestRequest) => testGroovyScript(data),
    onSuccess: (res) => setResult(res),
    onError: (err: Error) => setResult({
      testCaseName: 'error',
      passed: false,
      output: '',
      executionTimeMs: 0,
      errorMessage: err.message,
    }),
  });

  return (
    <div>
      <Card title="Groovy 脚本测试" style={{ marginBottom: 24 }}>
        <Form form={form} layout="vertical" onFinish={(values) => {
          const req: GroovyTestRequest = {
            scene: values.scene,
            scriptContent: values.scriptContent,
            inputContext: { userId: values.userId || '', deviceId: values.deviceId || '', ip: values.ip || '', orderAmount: values.orderAmount || '' },
          };
          testMut.mutate(req);
        }}>
          <Form.Item name="scene" label="场景" rules={[{ required: true }]}>
            <Select options={SCENE_OPTIONS.map((s) => ({ label: s, value: s }))} />
          </Form.Item>
          <Form.Item name="userId" label="userId"><Input placeholder="用户ID" /></Form.Item>
          <Form.Item name="deviceId" label="deviceId"><Input placeholder="设备ID" /></Form.Item>
          <Form.Item name="ip" label="ip"><Input placeholder="IP地址" /></Form.Item>
          <Form.Item name="orderAmount" label="orderAmount"><Input placeholder="订单金额" /></Form.Item>
          <Form.Item name="scriptContent" label="脚本内容" rules={[{ required: true }]}>
            <Input.TextArea rows={10} placeholder={`// ctx 变量: userId, deviceId, ip, scene, requestId, orderAmount\ndef action = "PASS"\ndef score = 0.0\nif (ctx.userId && ctx.userId.startsWith("test")) {\n  action = "BLOCK"\n  score = 0.8\n}\nreturn [action: action, score: score]`} />
          </Form.Item>
          <Form.Item><Button type="primary" htmlType="submit" loading={testMut.isPending}>执行测试</Button></Form.Item>
        </Form>
      </Card>

      {result && (
        <Card title="测试结果">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Tag color={result.passed ? 'success' : 'error'}>{result.passed ? '通过' : '失败'}</Tag>
            <div>执行时间: {result.executionTimeMs}ms</div>
            {result.output && <Alert message={result.output} type="info" />}
            {result.errorMessage && <Alert message={result.errorMessage} type="error" />}
          </Space>
        </Card>
      )}
    </div>
  );
}