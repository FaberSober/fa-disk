# ADR-001：文件仓库模块 MVP 审查优化

- 状态：🔍验证中
- 日期：2026-09-21
- 范围：`fa-disk` 后端与 `fa-disk-pages` 管理端页面
- 目标：以较少的新增复杂度，先保证桶、目录、文件、上传、回收站和基础权限形成可用闭环。

## 功能清单（按开发顺序）

| 编号 | 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| --- | --- | --- | --- | --- | --- |
| D01 | 领域契约 | 统一 API、权限和状态模型 | 固定桶/用户边界、文件状态、路径与响应约定 | 执行开发（MVP） | ❌未完成 |
| D02 | 桶管理 | 桶生命周期与成员授权 | 新增、编辑、删除、授权均校验当前用户范围 | 执行开发（MVP） | ❌未完成 |
| D03 | 文件生命周期 | 目录/文件删除与恢复 | 活跃列表、回收站、批量删除和恢复保持一致 | 执行开发（MVP） | ❌未完成 |
| D04 | 目录操作 | 移动、复制和树约束 | 禁止跨桶、移动到自身/子孙节点，维护父子关系 | 执行开发（MVP） | ❌未完成 |
| D05 | 文件查询 | 目录、树、分页和统计 | 当前目录查询、子节点查询、名称搜索和数量统计统一 | 执行开发（MVP） | ❌未完成 |
| D06 | 上传存储 | 上传、失败处理和清理 | 统一上传响应，处理失败、重复和孤立记录 | 执行开发（MVP） | ❌未完成 |
| D07 | 辅助能力 | 标签、历史、最近和回收站联动 | 操作结果可追踪，列表来源和刷新规则一致 | 执行开发（MVP） | ❌未完成 |
| D08 | 管理端页面 | 状态、表格和弹窗交互 | 首次加载、刷新、分页、删除、上传和右键操作可恢复 | 执行开发（MVP） | ❌未完成 |
| D09 | 全局上传 | 上传任务、进度和后台执行 | 切换菜单 Tab 后继续上传，右下角自定义浮动入口查看任务 | 执行开发（MVP） | ❌未完成 |
| D10 | 数据与任务 | DDL、索引和定时同步 | MySQL/PostgreSQL 定义对齐，任务字段和统计口径一致 | 执行开发（MVP） | ❌未完成 |
| D11 | 存储安全 | 文件访问与物理清理策略 | 私有文件访问、删除保留和清理时机明确 | 待确认后执行 | 👀待确认 |
| D12 | 扩展能力 | 预览、分享、配额、分片续传 | 不阻塞 MVP，按实际需求拆分后续版本 | 未来版本 | 🕒待处理 |

## 决策与开发说明

### D01：统一领域契约、权限和状态模型

- 先核对并固定前端 `/api/disk/store` 与后端 Controller 的实际路由、请求参数和响应结构。
- 所有桶、目录、文件查询与写操作都必须从当前用户可访问的桶范围开始过滤，不能只依赖前端传入的 ID。
- 明确“正常、回收站、已恢复”的状态转换；沿用现有逻辑删除能力，避免另造一套并行状态。
- 参考：`frontend/apps/admin/features/fa-disk-pages/configs/index.ts:8`、`fa-disk/src/main/java/com/faber/api/disk/store/rest/StoreFileController.java:31`、`fa-core/src/main/java/com/faber/core/bean/BaseDelEntity.java:21`。

### D02：桶生命周期与成员授权

- 桶新增、编辑、删除、成员授权统一由后端校验所有权或管理员权限。
- 删除桶前明确是否允许存在文件；若不允许，应返回可理解的业务错误，不做隐式级联物理删除。
- 前端保留现有桶列表、编辑弹窗和成员列表结构，只补齐保存、删除后的刷新和错误反馈。
- 参考：`fa-disk/src/main/java/com/faber/api/disk/store/biz/StoreBucketBiz.java:61`、`fa-disk/src/main/java/com/faber/api/disk/store/rest/StoreBucketUserController.java:23`、`frontend/apps/admin/features/fa-disk-pages/pages/admin/disk/buckets/index.tsx:22`。

### D03：目录/文件删除与恢复

- 正常列表不返回已删除数据，回收站只返回已删除数据；批量删除和单条删除使用同一套状态规则。
- 恢复时校验目标桶、父目录和父目录状态；父目录不存在或已删除时返回明确错误。
- 恢复到原目录与恢复到指定目录的结果要统一更新树、列表和统计数据。
- 参考：`fa-disk/src/main/java/com/faber/api/disk/store/biz/StoreFileBiz.java:177`、`fa-disk/src/main/java/com/faber/api/disk/store/biz/StoreFileBiz.java:399`、`frontend/apps/admin/features/fa-disk-pages/pages/admin/disk/store/recycle/index.tsx:35`。

### D04：移动、复制与目录树约束

- 移动和复制前校验源文件、目标目录属于同一可访问桶；目录不能移动到自身或其子孙节点。
- 对重名、目标目录不存在和目标为文件的情况采用统一错误码/提示，不让数据库产生半完成关系。
- 需要事务包住关系写入和必要的路径/统计更新；失败时不得只更新部分节点。
- 参考：`fa-disk/src/main/java/com/faber/api/disk/store/biz/StoreFileBiz.java:275`、`fa-disk/src/main/java/com/faber/api/disk/store/biz/StoreFileBiz.java:285`、`fa-disk/src/main/resources/mapper/disk/store/StoreFileMapper.xml:74`。

### D05：文件查询、分页和统计

- 统一当前目录、目录树、名称搜索、子节点和回收站的查询口径，明确是否包含目录、软删除记录和跨目录结果。
- 大列表使用分页；目录首屏只查询必要字段，避免每次加载整棵树或重复计算完整路径。
- 为常用过滤条件补充必要索引，并在 MySQL/PostgreSQL 中验证同一语义。
- 参考：`fa-disk/src/main/java/com/faber/api/disk/store/biz/StoreFileBiz.java:367`、`fa-disk/src/main/resources/mapper/disk/store/StoreFileMapper.xml:30`、`frontend/apps/admin/features/fa-disk-pages/pages/admin/disk/store/index.tsx:16`。

### D06：上传、失败处理和存储一致性

- 统一上传接口返回的文件 ID、名称、路径和状态；前端成功后只按服务端返回值刷新，不自行拼接关键字段。
- 上传失败、重复文件和数据库写入失败时，明确临时文件、数据库记录和用户提示的处理顺序，避免孤立记录或孤立物理文件。
- 上传弹窗应展示进行中、成功、失败三种状态，并在完成后只触发一次列表刷新。
- 参考：`fa-base/src/main/java/com/faber/api/base/admin/rest/FileSaveController.java:109`、`fa-base/src/main/java/com/faber/api/base/admin/biz/FileSaveBiz.java:114`、`frontend/apps/admin/features/fa-disk-pages/pages/admin/disk/store/alls/cube/StoreUploadFile.tsx:45`。

### D07：标签、历史、最近和回收站联动

- 标签新增/删除、保存历史、最近访问和回收站列表必须复用同一文件可见性和权限判断。
- 操作成功后刷新受影响的列表或局部行，不能依赖页面重新挂载才能看到结果。
- 历史记录只记录确有业务意义的操作；不为简单刷新或重复查询增加记录。
- 参考：`fa-disk/src/main/java/com/faber/api/disk/store/biz/StoreFileBiz.java:322`、`frontend/apps/admin/features/fa-disk-pages/pages/admin/disk/store/alls/modal/StoreFileTagsModal.tsx:49`、`frontend/apps/admin/features/fa-disk-pages/pages/admin/disk/store/alls/cube/FileSaveHisTable.tsx:22`。

### D08：管理端状态、表格和弹窗交互

- 页面首次进入时明确当前桶、当前目录和查询参数的初始化顺序，避免请求使用空桶或旧目录。
- 表格统一传递 `dataSource`、`loading`、分页参数和变更回调；删除、上传、移动、复制、标签等操作完成后刷新正确的数据源。
- 右键菜单、弹窗关闭、上传进度和错误状态必须可恢复；服务调用类型与实际后端返回类型保持一致。
- 页面级交互优先复用现有 `useTableQueryParams`、`useDelete`、`useApiLoading` 等能力；上传任务按 D09 使用专用全局 Store，不继续扩展 `DiskContext`。
- 参考：`frontend/apps/admin/features/fa-disk-pages/layout/disk/DiskLayout.tsx:44`、`frontend/apps/admin/features/fa-disk-pages/pages/admin/disk/store/alls/cube/FileTable.tsx:58`、`frontend/apps/admin/features/fa-disk-pages/pages/admin/disk/store/alls/cube/FileGrid.tsx:82`。

### D09：全局上传任务中心

- 将上传任务从 `DiskContext` 和 `DiskLayout` 中抽离，新增独立的全局上传 Store；不要把整个网盘上下文提升到管理端根布局。
- 复用项目已有 Zustand Store 模式，不新增状态管理库；建议新增 `types/UploadTask.ts`、`stores/useUploadTaskStore.ts`、`services/uploadTaskRunner.ts` 和 `components/upload/GlobalUploadCenter.tsx`，并通过 feature 聚合出口导出。
- Store 只保存可展示的任务元数据：`taskId`、文件名、大小、进度、状态、`bucketId`、`parentId`、目标路径和错误信息；运行时的 `File`、请求控制器放在任务执行器中。
- 任务状态至少包含 `queued`、`uploading`、`saving`、`success`、`error`、`cancelled`；只有物理文件上传和网盘关系保存都成功后才标记完成。
- 入队时固定目标桶和父目录，上传完成后通过 `@@disk/UPLOAD_FINISHED` 通知对应目录刷新，不保存页面组件的 `onSuccess` 回调。
- 使用现有 `AdminLayout` → `MenuContainer` → `MenuLayout` 的注入链挂载 `GlobalUploadCenter`；展示层使用自定义右下角浮动按钮和任务面板，不强制使用 Ant Design `FloatButton`。
- 浮动入口使用 `position: fixed`，不依赖网盘页面容器；初版保持单任务串行，支持进行中、成功、失败和重试，取消能力按请求层支持情况实现。
- 移除上传链路中未使用的 `FileReader.readAsDataURL`；如需取消上传，为 `fileSaveApi.uploadFile` 增加可选请求配置，底层 `postFile` 已支持传递配置。
- 参考：`frontend/apps/admin/src/pages/admin.tsx:17`、`frontend/apps/admin/features/fa-admin-pages/layout/menu/MenuContainer.tsx:25`、`frontend/apps/admin/features/fa-admin-pages/layout/menu/MenuLayout.tsx:333`、`frontend/apps/admin/features/fa-disk-pages/layout/disk/DiskLayout.tsx:13`、`frontend/apps/admin/features/fa-disk-pages/pages/admin/disk/store/alls/cube/StoreUploadFile.tsx:24`、`frontend/apps/admin/features/fa-admin-pages/services/base/admin/fileSave.ts:13`、`frontend/fa-ui/packages/ui/src/services/core/BaseZeroApi.ts:29`。

### D10：DDL、索引和定时同步

- 同步维护 MySQL 与 PostgreSQL DDL；字段类型、默认值、索引和菜单/任务初始化保持同一业务语义。
- 定时同步任务只处理明确的统计字段，避免重复扫描或覆盖用户可编辑数据。
- 对新增索引、字段和任务先给出迁移兼容性说明；不通过触发器或数据库方言特性隐藏业务逻辑。
- 参考：`fa-disk/src/main/resources/sql/fa-disk/mysql/1.0.0_disk_ddl.sql:13`、`fa-disk/src/main/resources/sql/fa-disk/postgre/1.0.0_disk_ddl.sql:13`、`fa-disk/src/main/java/com/faber/api/disk/store/jobs/JobSyncBucketInfo.java:22`。

### D11：文件访问与物理清理策略（待确认）

- 先确定文件是否私有、下载/预览是否需要签名 URL，以及管理员是否可以跨桶访问。
- 再确定逻辑删除后物理文件的保留期限、重复文件引用和永久删除入口；未确认前不实现自动物理清理。
- 该项确认后再补充接口、权限测试和定时清理任务，避免先做出无法回滚的删除策略。

### D12：后续版本能力

- OnlyOffice/在线预览、分享链接、配额、分片上传、断点续传和收藏等能力不纳入本轮 MVP。
- 浏览器刷新或关闭后的继续上传，需要后端上传会话、分片、合并和校验能力，不与 D09 的 SPA 菜单 Tab 切换能力混为一谈。
- 需要这些能力时，应先补充独立需求和数据模型，再更新本 ADR 的功能清单与开发顺序。

## 优先级、收益、范围与风险

| 优先级 | 功能 | 预期收益 | 改动范围 | 主要风险 |
| --- | --- | --- | --- | --- |
| P0 | D01-D04 | 先消除越权、错删、跨桶和目录循环等正确性风险 | 后端权限、生命周期、树操作及少量接口契约 | 既有接口调用方依赖旧参数或旧状态语义 |
| P1 | D05-D10 | 提升列表可用性、上传稳定性和中等数据量下的性能 | 查询 SQL、前端 Hook/表格、上传链路、DDL 与任务 | 查询口径或索引调整影响旧数据和两种数据库 |
| P2 | D11 | 明确私有访问和物理清理边界，降低数据泄露/误删风险 | 下载/预览权限、清理任务和保留策略 | 物理删除不可逆，必须先完成产品确认 |
| 未来 | D12 | 扩展在线网盘能力但不拖慢 MVP 交付 | 独立需求、接口和数据模型 | 范围膨胀、权限和存储成本上升 |

## 已确认问题与待验证判断

### 已确认问题

- 页面入口、桶管理和文件管理分别通过配置、布局上下文、列表/网格组件和弹窗串联，刷新责任分散在多个组件中。
- 当前上传面板挂在 `DiskLayout`/`DiskContext` 下，离开网盘页面后没有真正的全局展示宿主。
- 当前上传任务由页面组件串行驱动，物理文件上传完成和网盘关系保存完成之间的状态边界不清晰。
- 后端文件的删除、恢复、移动、复制、标签和查询是多条独立调用链，必须统一权限、状态和事务边界。
- 文件查询包含路径、子节点和统计等 SQL，列表规模增大后需要分页、索引和查询口径治理。
- 项目同时维护 MySQL 与 PostgreSQL DDL，结构调整不能只修改一种方言。

### 待验证判断

- `/api/disk/store` 与 Controller 的最终路由是否完全一致，需要以实际 Spring 路由扫描结果或接口定义核对。
- D09 当前默认只覆盖同一 SPA 内的后台菜单 Tab 切换；浏览器刷新、关闭或网络中断后的续传归入 D12。
- 桶成员权限、管理员越权范围和文件下载/预览的隐私要求需要产品确认。
- 物理文件删除、重复文件引用和历史记录保留策略目前不应由实现者自行推断。

## 实施约束

- 按 D01→D10 顺序推进；每完成一个功能，先做受影响模块的定向验证，再更新本表进度。
- 尽量复用现有 BaseController、BaseDelEntity、Mapper/Biz 基类和前端表格 Hook；不为解决局部问题引入新的全局状态或重复 CRUD 框架。
- D09 可以新增上传专用的全局 Store，但不扩展 `DiskContext` 承担全局任务状态；展示组件使用项目现有主题变量和图标体系，可采用自定义按钮样式。
- 后端先保证权限和数据一致性，前端再修正加载、回调和刷新；不要用前端隐藏按钮替代后端鉴权。
- 本 ADR 不要求启动服务或执行全量测试；开发阶段按变更范围补充最小必要的编译、接口或定向测试。

## MVP 验收标准

- 无权限用户不能读取或修改其他桶、目录和文件；同桶内的授权行为符合成员规则。
- 删除、回收、恢复、移动和复制在正常列表、回收站、目录树及统计中结果一致，且不会产生跨桶或循环父子关系。
- 上传任务在切换后台菜单 Tab、关闭网盘 Tab 后仍可继续并可从右下角自定义浮动入口查看；上传、保存关系、成功和失败状态明确。
- 上传失败不会留下无法解释的孤立业务记录；完成后只刷新目标桶和目标目录的相关列表。
- MySQL 与 PostgreSQL 的核心表结构和索引语义一致；D01、D11 的待确认项在实现前完成决策记录。
