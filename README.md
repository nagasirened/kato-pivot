# Kato Pivot

Kato Pivot 是一个基于 Spring Boot 和 Spring Cloud 的微服务项目集合，包含了多个功能模块。

## 项目模块

- **kato-core**: 核心功能模块
- **kato-rpc**: RPC 服务模块
- **kato-recommend**: 推荐系统模块
- **kato-sensitive**: 敏感词过滤模块
- **kato-uaa**: 用户认证和授权模块
- **tool-test**: 工具测试模块

## 技术栈

- Spring Boot 2.5.5
- Spring Cloud 2020.0.4
- Spring Cloud Alibaba 2021.1
- MyBatis-Plus 3.5.7
- Redisson 3.17.0
- Netty 4.1.75.Final
- Protobuf 3.19.4
- Kubernetes Client 5.3.0

## 主要特性

1. **分布式锁**
   - 基于 Redisson 的分布式锁实现
   - 配置项：`kato.redisson.enable=true`（默认开启）
   - 锁类型：`kato.lock.type=REDISSON`（默认开启）

2. **RPC 服务**
   - 支持多种序列化方式（Hessian、Protobuf、XStream）
   - 基于 Netty 的高性能网络通信
   - 服务注册与发现

3. **推荐系统**
   - 个性化推荐算法
   - 实时数据处理

4. **敏感词过滤**
   - 高效的敏感词检测
   - 支持自定义词库

5. **用户认证**
   - 基于 JWT 的认证机制
   - OAuth2 支持
   - 多因素认证

## 开发环境要求

- JDK 8+
- Maven 3.6+
- Redis
- MySQL 5.7+

## 快速开始

1. 克隆项目
```bash
git clone https://github.com/yourusername/kato-pivot.git
```

2. 编译项目
```bash
mvn clean install
```

3. 运行服务
```bash
mvn spring-boot:run
```

## 配置说明

主要配置项：
- `kato.redisson.enable`: Redisson 客户端开关
- `kato.lock.type`: 分布式锁类型

## 贡献指南

欢迎提交 Issue 和 Pull Request。

## 许可证

[MIT License](LICENSE)

