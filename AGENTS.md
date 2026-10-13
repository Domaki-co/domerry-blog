# AGENTS.md — Domerry 博客 monorepo

本仓库是一个 monorepo，包含三个互相独立、各自有独立构建链的子项目。**改哪个子项目，就先读它自己的 `AGENTS.md`**（那份文件里的信息更具体、优先级更高）。

| 目录 | 内容 | 技术栈 | 详情 |
| --- | --- | --- | --- |
| `blog/` | 博客后端 | Spring Boot 2.7 + MyBatis-Plus + Sa-Token，Maven 多模块，Java 8 | [blog/AGENTS.md](blog/AGENTS.md) |
| `blog-admin/` | 管理后台前端 | Vue 3 + TS + Vite 4 + Element Plus + Pinia | [blog-admin/AGENTS.md](blog-admin/AGENTS.md) |
| `blog-web/` | 门户前端 | Vue 3.5 + JS + Vite 5 + Element Plus + Vuex | [blog-web/AGENTS.md](blog-web/AGENTS.md) |

- 后端端口 **8900**，无 context-path；门户接口 `/api/**`，管理端 `/sys/**`。
- 前端开发时通过 Vite 代理连到 `http://localhost:8900`，需要后端先起。

## 验证与测试约束（重要）

**背景**：本项目三个子项目都没有单元测试 / lint 脚本——后端 `surefire` 配了 `skipTests=true` 且无 `src/test`，两个前端也没有 lint/test/typecheck 脚本。也就是说，唯一的"验证手段"是构建，而构建很慢。历史上大量时间被浪费在重复、过度的验证上，因此约定如下。

### 分层验证：从便宜到贵，够用即停

- **L0 自查**：改完先看 `git diff`，人工确认改动范围与语法/类型无误。这是第一道，也是默认只做的一道。
- **L1 针对性检查**：只针对**本次改动的那个子项目**做最便宜的检查。
  - 前端：优先只对改动文件做语法自查；`blog-admin` 装了 `vue-tsc`，必要时可跑 `npx vue-tsc --noEmit -p tsconfig.json`（比 `vite build` 快）。
  - 后端：`mvn -q -pl <改动模块> -am compile` 比全量 `package` 快很多。
- **L2 构建**：`npm run build` / `mvn clean package` 是**昂贵操作**，只在下面「何时必须跑」里跑，且一次任务内最多一次。

### 硬规则

1. **不要重复跑同一个验证命令**。命令失败 → 先改代码 → 再跑；不要原地反复重试。
2. **不要为了"保险"把三个子项目都构建一遍**。只验证被改动的子项目。
3. **不要主动启动 dev server / preview / 后端服务**，除非用户明确要求做端到端联调。这类验证默认交给用户手动完成。
4. **不要用 `mvn clean package` 全量打包来"验证"一个小的 Java 改动**。用 `-pl <module> -am` 增量编译，或直接交给用户。
5. **不要反复 `npm install` / `mvn dependency:resolve`**，除非依赖清单（`package.json` / `pom.xml`）真的变了。
6. **改动纯样式/文案/文档时，可以跳过构建**，只做 L0 自查并向用户说明。
7. **收尾时如实汇报**：跑了哪些命令、结果如何、以及**因为本约束而没跑的验证**，让用户决定是否补跑。

### 何时必须跑完整构建（L2）

- 用户明确要求验证 / 提交 / 发布；
- 改动会影响构建产物（新增依赖、改 `vite.config.*` / `pom.xml` / 环境变量等）；
- 改动跨子项目契约（后端接口 + 前端调用）。

### 报告格式建议

> 已验证：`blog-web` 改动文件自查通过（未跑 build）。
> 未验证：`npm run build`（按约束留给发布前执行）。

让"省略了什么"始终可见，用户就不会以为验证已经全跑过。
