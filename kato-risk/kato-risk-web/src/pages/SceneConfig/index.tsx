import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Card, Table, Switch, Select, message } from 'antd';
import { getScenes, toggleScene } from '../../api/risk';
import type { SceneConfig as SceneConfigType } from '../../api/risk';

export default function SceneConfig() {
  const queryClient = useQueryClient();

  const { data: scenes, isLoading } = useQuery({
    queryKey: ['scenes'],
    queryFn: () => getScenes() as Promise<Record<string, SceneConfigType>>,
  });

  const toggleMut = useMutation({
    mutationFn: (params: { scene: string; enabled?: boolean; defaultAction?: string }) => toggleScene(params.scene, params.enabled, params.defaultAction),
    onSuccess: () => {
      message.success('更新成功');
      queryClient.invalidateQueries({ queryKey: ['scenes'] });
    },
    onError: () => message.error('更新失败'),
  });

  const sceneData = Object.values(scenes || {});

  return (
    <Card title="场景配置">
      <Table
        columns={[
          { title: '场景', dataIndex: 'scene' },
          {
            title: '启用状态',
            dataIndex: 'enabled',
            render: (enabled: boolean, record: SceneConfigType) => (
              <Switch checked={enabled} onChange={(checked) => toggleMut.mutate({ scene: record.scene, enabled: checked })} />
            ),
          },
          {
            title: '默认动作',
            render: (_: unknown, record: SceneConfigType) => (
              <Select value={record.defaultAction} style={{ width: 100 }}
                onChange={(defaultAction) => toggleMut.mutate({ scene: record.scene, defaultAction })}
                options={[{ label: 'PASS', value: 'PASS' }, { label: 'BLOCK', value: 'BLOCK' }]} />
            ),
          },
        ]}
        dataSource={sceneData}
        rowKey="scene"
        loading={isLoading}
        pagination={false}
      />
    </Card>
  );
}