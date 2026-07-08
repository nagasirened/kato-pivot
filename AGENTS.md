# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

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

<skills_system priority="1">

## Available Skills

<!-- SKILLS_TABLE_START -->
<usage>
When users ask you to perform tasks, check if any of the available skills below can help complete the task more effectively. Skills provide specialized capabilities and domain knowledge.

How to use skills:
- Invoke: `npx openskills read <skill-name>` (run in your shell)
  - For multiple: `npx openskills read skill-one,skill-two`
- The skill content will load with detailed instructions on how to complete the task
- Base directory provided in output for resolving bundled resources (references/, scripts/, assets/)

Usage notes:
- Only use skills listed in <available_skills> below
- Do not invoke a skill that is already loaded in your context
- Each skill invocation is stateless
</usage>

<available_skills>

<skill>
<name>brainstorming</name>
<description>"You MUST use this before any creative work - creating features, building components, adding functionality, or modifying behavior. Explores user intent, requirements and design before implementation."</description>
<location>global</location>
</skill>

<skill>
<name>docx</name>
<description>"Use this skill whenever the user wants to create, read, edit, or manipulate Word documents (.docx files). Triggers include: any mention of 'Word doc', 'word document', '.docx', or requests to produce professional documents with formatting like tables of contents, headings, page numbers, or letterheads. Also use when extracting or reorganizing content from .docx files, inserting or replacing images in documents, performing find-and-replace in Word files, working with tracked changes or comments, or converting content into a polished Word document. If the user asks for a 'report', 'memo', 'letter', 'template', or similar deliverable as a Word or .docx file, use this skill. Do NOT use for PDFs, spreadsheets, Google Docs, or general coding tasks unrelated to document generation."</description>
<location>global</location>
</skill>

<skill>
<name>p7</name>
<description>"P7 Senior Engineer mode — solution-driven execution under P8 supervision. Use when user says 'P7模式', '方案驱动', or when spawned as sub-task executor by P8. Produces: implementation plan + code + 3-question self-review, delivered via [P7-COMPLETION]."</description>
<location>global</location>
</skill>

<skill>
<name>pdf</name>
<description>Use this skill whenever the user wants to do anything with PDF files. This includes reading or extracting text/tables from PDFs, combining or merging multiple PDFs into one, splitting PDFs apart, rotating pages, adding watermarks, creating new PDFs, filling PDF forms, encrypting/decrypting PDFs, extracting images, and OCR on scanned PDFs to make them searchable. If the user mentions a .pdf file or asks to produce one, use this skill.</description>
<location>global</location>
</skill>

<skill>
<name>pptx</name>
<description>"Use this skill any time a .pptx file is involved in any way — as input, output, or both. This includes: creating slide decks, pitch decks, or presentations; reading, parsing, or extracting text from any .pptx file (even if the extracted content will be used elsewhere, like in an email or summary); editing, modifying, or updating existing presentations; combining or splitting slide files; working with templates, layouts, speaker notes, or comments. Trigger whenever the user mentions \"deck,\" \"slides,\" \"presentation,\" or references a .pptx filename, regardless of what they plan to do with the content afterward. If a .pptx file needs to be opened, created, or touched, use this skill."</description>
<location>global</location>
</skill>

<skill>
<name>pua</name>
<description>"Forces high-agency exhaustive problem-solving with corporate PUA pressure. Triggers on user frustration, repeated failures (2+), passive behavior, or quality complaints. Common triggers across Reddit/LinuxDo/HN/X: 'try harder', 'figure it out', 'stop giving up', 'you keep failing', '加油', '别偷懒', '你再试试', '为什么还不行', '你怎么又失败了', '你怎么搞的', '又错了', '能不能靠谱点', '认真点', '不行啊', '降智了', '你又在原地打转', '你把之前的改坏了', '别让我手动处理', '换个方法', 'stop spinning', 'you broke it', 'why does this still not work', 'this is the third time', '/pua', 'PUA模式'. Applies to ALL task types: code, config, debug, deploy, research."</description>
<location>global</location>
</skill>

<skill>
<name>recite</name>
<description>></description>
<location>global</location>
</skill>

<skill>
<name>skill-creator</name>
<description>Create new skills, modify and improve existing skills, and measure skill performance. Use when users want to create a skill from scratch, edit, or optimize an existing skill, run evals to test a skill, benchmark skill performance with variance analysis, or optimize a skill's description for better triggering accuracy.</description>
<location>global</location>
</skill>

<skill>
<name>subagent-driven-development</name>
<description>Use when executing implementation plans with independent tasks in the current session</description>
<location>global</location>
</skill>

<skill>
<name>systematic-debugging</name>
<description>Use when encountering any bug, test failure, or unexpected behavior, before proposing fixes</description>
<location>global</location>
</skill>

<skill>
<name>using-git-worktrees</name>
<description>Use when starting feature work that needs isolation from current workspace or before executing implementation plans - ensures an isolated workspace exists via native tools or git worktree fallback</description>
<location>global</location>
</skill>

<skill>
<name>writing-plans</name>
<description>Use when you have a spec or requirements for a multi-step task, before touching code</description>
<location>global</location>
</skill>

<skill>
<name>xlsx</name>
<description>"Use this skill any time a spreadsheet file is the primary input or output. This means any task where the user wants to: open, read, edit, or fix an existing .xlsx, .xlsm, .csv, or .tsv file (e.g., adding columns, computing formulas, formatting, charting, cleaning messy data); create a new spreadsheet from scratch or from other data sources; or convert between tabular file formats. Trigger especially when the user references a spreadsheet file by name or path — even casually (like \"the xlsx in my downloads\") — and wants something done to it or produced from it. Also trigger for cleaning or restructuring messy tabular data files (malformed rows, misplaced headers, junk data) into proper spreadsheets. The deliverable must be a spreadsheet file. Do NOT trigger when the primary deliverable is a Word document, HTML report, standalone Python script, database pipeline, or Google Sheets API integration, even if tabular data is involved."</description>
<location>global</location>
</skill>

</available_skills>
<!-- SKILLS_TABLE_END -->

</skills_system>
