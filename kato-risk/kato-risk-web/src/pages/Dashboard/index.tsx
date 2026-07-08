import { useQuery } from '@tanstack/react-query';
import { Card, Row, Col, Statistic, Table } from 'antd';
import { getDashboardStats } from '../../api/risk';
import * as echarts from 'echarts';
import { useEffect, useRef } from 'react';

interface DashboardData {
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

export default function Dashboard() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: () => getDashboardStats() as Promise<DashboardData>,
  });

  const trendChartRef = useRef<HTMLDivElement>(null);
  const sceneChartRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!data || !trendChartRef.current || !sceneChartRef.current) return;

    const trendChart = echarts.init(trendChartRef.current);
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['通过', '拦截'] },
      xAxis: { type: 'category', data: data.trendData?.map((d) => d.date) || [] },
      yAxis: { type: 'value' },
      series: [
        { name: '通过', type: 'line', data: data.trendData?.map((d) => d.passed) || [] },
        { name: '拦截', type: 'line', data: data.trendData?.map((d) => d.blocked) || [] },
      ],
    });

    const sceneChart = echarts.init(sceneChartRef.current);
    sceneChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: data.sceneDistribution?.map((s) => ({ name: s.scene, value: s.count })) || [],
      }],
    });

    return () => {
      trendChart.dispose();
      sceneChart.dispose();
    };
  }, [data]);

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card loading={isLoading}>
            <Statistic title="今日检查" value={data?.todayTotal || 0} />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={isLoading}>
            <Statistic title="今日通过" value={data?.todayPassed || 0} valueStyle={{ color: '#3f8600' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={isLoading}>
            <Statistic title="今日拦截" value={data?.todayBlocked || 0} valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card loading={isLoading}>
            <Statistic title="待审" value={data?.todayPending || 0} valueStyle={{ color: '#faad14' }} />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={16}>
          <Card title="拦截趋势（近7天）">
            <div ref={trendChartRef} style={{ height: 300 }} />
          </Card>
        </Col>
        <Col span={8}>
          <Card title="场景分布">
            <div ref={sceneChartRef} style={{ height: 300 }} />
          </Card>
        </Col>
      </Row>

      <Card title="TOP 触发规则">
        <Table
          columns={[{ title: '规则名称', dataIndex: 'ruleName' }, { title: '触发次数', dataIndex: 'triggerCount' }]}
          dataSource={data?.topRules || []}
          rowKey="ruleId"
          pagination={false}
          size="small"
        />
      </Card>
    </div>
  );
}