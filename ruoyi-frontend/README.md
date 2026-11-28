# 犀云ERP（保税维修管理系统）前端

基于 Vue3 + Element Plus + Vite 的保税维修业务管理系统前端，覆盖物料备案、入库、发料、收料、出库、库存台账、海关联网申报与回执跟踪等业务模块。

## 技术栈

- Vue 3 + Vite
- Element Plus
- Pinia 状态管理
- Vue Router 4

## 开发与构建

```bash
# 克隆项目
git clone https://github.com/leebeep/BondedRepair.git

# 进入前端目录
cd BondedRepair/ruoyi-frontend

# 安装依赖
npm install

# 启动开发环境（后端接口代理见 vite.config.js）
npm run dev

# 生产环境打包
npm run build:prod
```

## 环境配置

| 文件 | 说明 |
| --- | --- |
| `.env.development` | 开发环境（接口前缀 `/dev-api`） |
| `.env.staging` | 预发布环境（接口前缀 `/stage-api`） |
| `.env.production` | 生产环境（接口前缀 `/prod-api`） |

配套后端位于仓库的 `ruoyi-backend` 目录。
