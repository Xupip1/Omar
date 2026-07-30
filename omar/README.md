<div align="center">

# ⛏️ Omar Anti-Xray Plugin

**一个针对 Minecraft Paper 1.21.8 服务器的反矿透检测插件**

[![Paper](https://img.shields.io/badge/Paper-1.21.8-2d2d2d?logo=minecraft&labelColor=FCFC03)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.0.0-blue)](https://github.com/YOUR_USERNAME/Omar)

</div>

---

## 📋 简介

Omar 是一款专为 Paper 1.21.8 设计的反矿透（Anti-Xray）检测插件。它通过监控玩家挖掘矿石的行为模式，自动识别疑似使用矿透作弊（X-Ray）的玩家，并以**可点击的聊天警报**、**带音效的通知**和**分页箱子面板**的形式呈现给管理员。

核心特性：

- **滑动窗口检测** — 基于时间窗口和挖掘数量智能判断
- **独立矿石阈值** — 钻石矿石、深层钻石矿石、远古残骸可分别设置
- **重点汇报升级** — 被多次汇报的玩家自动升格为重点关注目标
- **全 GUI 管理** — 所有功能均可通过 `/omar` 箱子面板点击操作
- **可点击传送** — 聊天警报和面板中点击即可传送至矿石坐标
- **全配置化消息** — 支持 `&#RRGGBB` 十六进制颜色代码
- **LuckPerms 兼容** — 完整权限节点体系

---

## 📦 安装

1. 下载最新版本的 `Omar-1.0.0.jar`
2. 放入服务器 `plugins/` 目录
3. 重启服务器
4. 编辑 `plugins/Omar/config.yml` 按需配置
5. 输入 `/omar reload` 重载配置

### 依赖

- **服务器**: [Paper](https://papermc.io) 1.21.8（或兼容的 Paper 分支）
- **Java**: 21 或更高版本
- **可选**: [LuckPerms](https://luckperms.net)（用于细粒度权限管理）

---

## ⚡ 快速上手

| 步骤 | 操作 | 说明 |
|:----:|------|------|
| ① | `/omar add <玩家>` | 将该玩家添加为汇报管理员 |
| ② | `/omar panel` | 打开管理面板浏览所有功能 |
| ③ | `/omar help` | 查看完整命令帮助 |

---

## 🎮 命令

| 命令 | 描述 | 权限 |
|:-----|:-----|:----:|
| `/omar` | 打开管理面板 | `omar.command.panel` |
| `/omar panel` | 打开管理面板 | `omar.command.panel` |
| `/omar add <玩家>` | 添加汇报管理员 | `omar.command.add` |
| `/omar remove <玩家>` | 移除汇报管理员 | `omar.command.remove` |
| `/omar log` | 打开汇报记录箱子面板 | `omar.command.log` |
| `/omar imlog` | 打开重点汇报记录面板 | `omar.command.imlog` |
| `/omar check <玩家>` | 查询玩家的汇报记录与状态 | `omar.command.check` |
| `/omar bypass add <玩家>` | 添加绕过白名单 | `omar.command.bypass` |
| `/omar bypass remove <玩家>` | 移除绕过白名单 | `omar.command.bypass` |
| `/omar bypass list` | 查看绕过白名单（或 GUI 管理） | `omar.command.bypass` |
| `/omar stats` | 查看插件统计信息 | `omar.command.stats` |
| `/omar delete` | 删除所有汇报记录 | `omar.command.delete` |
| `/omar reload` | 重载配置文件 | `omar.command.reload` |
| `/omar help` | 显示帮助信息 | `omar.command.help` |

> 💡 拥有 `omar.admin` 权限的玩家自动拥有上述所有命令权限。

---

## 🔐 权限

| 权限节点 | 默认 | 描述 |
|:---------|:----:|:-----|
| `omar.admin` | op | 拥有所有 Omar 插件权限 |
| `omar.report.receive` | op | 接收矿透警报和查看汇报记录 |
| `omar.command.*` | op | 所有命令权限（子节点） |
| `omar.command.add` | op | 添加汇报管理员 |
| `omar.command.remove` | op | 移除汇报管理员 |
| `omar.command.log` | op | 打开汇报记录面板 |
| `omar.command.imlog` | op | 打开重点汇报记录面板 |
| `omar.command.check` | op | 查询玩家的汇报记录 |
| `omar.command.bypass` | op | 管理绕过白名单 |
| `omar.command.stats` | op | 查看插件统计信息 |
| `omar.command.panel` | op | 打开管理面板 |
| `omar.command.delete` | op | 删除所有汇报记录 |
| `omar.command.reload` | op | 重载配置文件 |
| `omar.command.help` | op | 查看帮助信息 |
| `omar.command.tp` | op | 传送至矿道位置（内部使用） |

> 所有权限默认仅 OP 可用。如需赋予非 OP 管理员，可使用 LuckPerms 等权限插件授予 `omar.admin`。

---

## ⚙️ 配置说明

配置文件位于 `plugins/Omar/config.yml`，支持完整注释和 `&#RRGGBB` 十六进制颜色。

### 检测参数

| 配置项 | 默认值 | 说明 |
|:-------|:------:|:-----|
| `time-window` | `120` | 检测时间窗口（秒） |
| `ore-threshold` | `5` | 全局矿石阈值 |
| `ore-thresholds.diamond_ore` | `0` | 钻石矿石独立阈值（0 使用全局） |
| `ore-thresholds.deepslate_diamond_ore` | `0` | 深层钻石矿石独立阈值（0 使用全局） |
| `ore-thresholds.ancient_debris` | `0` | 远古残骸独立阈值（0 使用全局） |

### 重点汇报

| 配置项 | 默认值 | 说明 |
|:-------|:------:|:-----|
| `priority-time-window` | `300` | 重点汇报时间窗口（秒），也作为两次重点警告之间的冷却时间 |
| `priority-report-threshold` | `3` | 在窗口内被汇报多少次即升格为重点 |
| `warn-suspected-player` | `true` | 是否向被关注的玩家发送警告消息 |

### 自动处理（默认关闭）

| 配置项 | 默认值 | 说明 |
|:-------|:------:|:-----|
| `auto-action.enabled` | `false` | 是否启用自动处理措施 |
| `auto-action.action` | `none` | 动作类型：`none` / `kick` / `ban` / `command` |
| `auto-action.command` | `tempban %player% 1d 矿透作弊` | 自定义命令（`%player%` 替换为玩家名） |
| `auto-action.message` | `您因矿透嫌疑已被移出服务器` | 踢出/封禁提示消息 |

### 声音通知

| 配置项 | 默认值 | 说明 |
|:-------|:------:|:-----|
| `sound-notification.enabled` | `true` | 是否启用警报音效 |
| `sound-notification.sound` | `BLOCK_NOTE_BLOCK_PLING` | 音效名称 |
| `sound-notification.volume` | `1.0` | 音量 |
| `sound-notification.pitch` | `1.0` | 音调 |

### 配色参考

配置文件中所有消息文本均支持以下自定义颜色：

```
绿色  &#a6e3a1    红色  &#f38ba8
黄色  &#f9e2af    灰色  &#9399b2
金色  &#fab387
```

---

## 🖥️ 管理面板

输入 `/omar` 即可打开管理面板（GUI 仓库界面）：

```
┌─────────────────────────────────────────────────┐
│   (填充) (填充) (填充) (填充) (填充) (填充) (填充) (填充) (填充) │
│   (填充) (填充) (填充) (填充) (填充) (填充) (填充) (填充) (填充) │
│   (填充) 💎  💀  🛡️  🧭  📄  ⏰  📖  (填充) │  ← 功能按钮
│   (填充) (填充) (填充) (填充) (填充) (填充) (填充) (填充) (填充) │
│   (填充) (填充) (填充) (填充) (填充) (填充) (填充) (填充) (填充) │
│   (填充) (填充) (填充) (填充) (填充) (填充) (填充) (填充) (填充) │
└─────────────────────────────────────────────────┘
     💎 汇报记录    💀 重点汇报    🛡️ 白名单管理
     🧭 查询玩家    📄 统计信息    ⏰ 重载配置
     📖 帮助
```

---

## 📊 数据存储

插件数据存储在 `plugins/Omar/` 目录下：

| 文件 | 说明 |
|:----|:-----|
| `config.yml` | 插件配置文件 |
| `reports.json` | 普通汇报记录 |
| `priority_reports.json` | 重点汇报记录 |
| `stats.json` | 统计信息 |

---

## 🧱 构建

```bash
git clone https://github.com/YOUR_USERNAME/Omar.git
cd Omar
mvn clean package -DskipTests
```

构建产物位于 `target/Omar-1.0.0.jar`。

---

## 📄 开源许可

本项目基于 [MIT License](LICENSE) 开源。

---

<div align="center">

**Made with ❤️ for Minecraft Paper Servers**

</div>
