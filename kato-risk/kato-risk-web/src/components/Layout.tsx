import { Layout as AntLayout, Menu } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  DashboardOutlined,
  ScheduleOutlined,
  ExperimentOutlined,
  FileTextOutlined,
  AuditOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import styles from './Layout.module.css';

const { Sider, Content } = AntLayout;

const menuItems = [
  { key: '/', icon: <DashboardOutlined />, label: '仪表盘' },
  { key: '/rules', icon: <ScheduleOutlined />, label: '规则管理' },
  { key: '/rules/test', icon: <ExperimentOutlined />, label: '脚本测试' },
  { key: '/cases', icon: <FileTextOutlined />, label: '风险事件' },
  { key: '/audit-logs', icon: <AuditOutlined />, label: '审核日志' },
  { key: '/scene-config', icon: <SettingOutlined />, label: '场景配置' },
];

export default function Layout({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const location = useLocation();

  const selectedKey = menuItems.find((item) =>
    location.pathname.startsWith(item.key)
  )?.key || '/';

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider theme="dark" width={200}>
        <div className={styles.logo}>风控系统</div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <AntLayout>
        <Content style={{ padding: '24px', background: '#f0f2f5' }}>
          {children}
        </Content>
      </AntLayout>
    </AntLayout>
  );
}