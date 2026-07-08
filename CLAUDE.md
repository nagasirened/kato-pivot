# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Full build (skip tests)
mvn clean install -DskipTests

# Build specific module (with dependencies)
mvn clean install -DskipTests -pl kato-sensitive -am

# Run tests
mvn test

# Run single test class
mvn test -Dtest=ClassName -pl module-name

# Run Spring Boot application
mvn spring-boot:run -pl kato-recommend              # recommend service
mvn spring-boot:run -pl kato-sensitive/kato-sensitive-server   # sensitive server (port 8080)
mvn spring-boot:run -pl kato-sensitive/kato-sensitive-client   # sensitive client (port 8081)
```

## Project Architecture

### Module Hierarchy

```
kato-pivot (parent pom, Spring Boot 2.5.5, Java 8)
├── kato-core/                    # Shared library modules (Java 8)
│   ├── kato-common/             # Common utilities, exceptions, response wrappers
│   ├── kato-web/                # Web layer: base controllers, interceptors, CORS
│   ├── kato-redis/             # Redis client wrapper
│   ├── kato-redisson/           # Distributed lock (Redisson)
│   ├── kato-database/           # MyBatis-Plus base mapper, generic CRUD
│   ├── kato-auth/              # Authentication utilities
│   ├── kato-s3/                 # AWS S3 / Aliyun OSS file storage
│   ├── kato-dynamodb/          # DynamoDB client
│   ├── kato-oss/                # Object storage abstraction
│   ├── kato-zookeeper/         # Zookeeper curator wrapper
│   ├── kato-resilience4j/      # Circuit breaker, rate limiter
│   └── code-generator/          # MyBatis-Plus / MapStruct generator
├── kato-rpc/                     # Custom RPC framework (Java 8)
│   ├── rpc-core/               # Core: serialization (Hessian/Protobuf/XStream), Netty transport, Curator service registry
│   ├── kato-rpc-server-starter/ # Server bootstrap
│   └── kato-rpc-client-starter/ # Client auto-configuration
├── kato-recommend/              # Recommendation engine service (Spring Boot)
├── kato-sensitive/              # Sensitive word filtering (client-server split, see below)
├── kato-uaa/                     # User authentication & authorization (Spring Security + JWT)
├── kato-ai/                      # AI integration (Java 21 only)
│   ├── kato-langchain/         # LangChain4j integration
│   └── kato-spring-ai/         # Spring AI integration
└── tool-test/                   # Testing utilities
```

### kato-sensitive Architecture

This module has a client-server split:
- **kato-sensitive-server** (port 8080): Management API — CRUD for sensitive words, Excel import/export, audit workflow, Kafka producer for hot-reload events
- **kato-sensitive-client** (port 8081): Detection API — DFA/Trie/Regex matching engine, fuzzy match (homophone/visual-variant/simplified-traditional), Caffeine cache, Kafka consumer for real-time word updates, Resilience4j circuit breaker

Key packages in sensitive-server: `controller/`, `service/`, `mapper/`, `entity/`, `dto/`, `listener/`, `config/`
Key packages in sensitive-client: `service/`, `dfa/`, `trie/`, `algorithm/`, `util/`, `kafka/`, `config/`

### kato-rpc Architecture

Custom RPC over Netty with Curator for service discovery. Serialization is pluggable (Hessian default). Service registry path: `/kato/rpc/services/{serviceName}`.

## Key Technologies

| Component | Library/Version |
|-----------|----------------|
| Framework | Spring Boot 2.5.5, Spring Cloud 2020.0.4, Spring Cloud Alibaba 2021.1 |
| Persistence | MyBatis-Plus 3.5.7 |
| Distributed Lock | Redisson 3.17.0 |
| RPC | Netty 4.1.75.Final + Curator 5.2.1 |
| Serialization | Hessian 4.0.65, Protobuf 3.19.4, XStream 1.4.19 |
| Circuit Breaker | Resilience4j 2.3.0 |
| Excel | EasyExcel (Alibaba) |
| File Storage | AWS S3 SDK 1.12.63 / Aliyun OSS SDK 3.14.0 |
| AI | LangChain4j 1.12.2, Spring AI |
| Build | Maven, lombok, mapstruct 1.4.2 |

## Important Patterns

### Package naming
All production code lives under `com.kato.pro` (e.g., `com.kato.pro.sensitive`, `com.kato.pro.rec`, `com.kato.pro.uaa`). Common utilities may also use `org.kato` groupId (see `rpc-core`).

### Response wrapper
All REST endpoints return a unified result object. Check existing controllers (e.g., in kato-recommend or kato-sensitive) for the `Result`/`Response` pattern — do not return raw entities.

### Sensitive word data model
`word`, `level` (URGENT/MEDIUM/NORMAL), `status` (ENABLE/DISABLE), `category` (POLITICS/PORN/AD/VIOLENCE/FRAUD/OTHER), `word_type` (EXACT/REGEX), `remark`. DTOs and entity fields use snake_case.

### Database conventions
- MyBatis-Plus for data access (not JPA)
- No `select *`, all updates/deletes must have WHERE conditions
- Paginated queries via MyBatis-Plus `Page`
- Logical deletion field is common

### Hot-reload via Kafka
Sensitive words are synchronized between server and client via Kafka topic `sensitive-word-update`. Server is the producer (on word CRUD), client is the consumer (triggers hot-reload of DFA/Trie/Regex matchers).

### kato-ai is Java 21
All other modules target Java 8. When adding dependencies to kato-ai, ensure it does not pollute the parent dependencyManagement.