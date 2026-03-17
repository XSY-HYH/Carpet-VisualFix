# Carpet-VisualFix

一个 Minecraft 1.21.1 Fabric 模组，修复弱加载区块内实体的动画问题。

需要 [Carpet Mod](https://modrinth.com/mod/carpet)。

## 功能

弱加载区块（超出模拟距离但在渲染距离内）里的实体，服务端不再更新它们，但客户端的动画还在播放。这个模组让这些实体静止，让视觉效果和服务端实际状态一致。

## Carpet 规则

`entityRenderingFix` - 修复了弱加载区域内实体的动画（默认开启）

## 环境要求

- Minecraft 1.21.1
- Fabric API
- Carpet Mod

## 链接

- [Modrinth](https://modrinth.com/project/carpet-visualfix/)
- [GitHub](https://github.com/XSY-HYH/Carpet-VisualFix)
- [问题反馈](https://github.com/XSY-HYH/Carpet-VisualFix/issues)
- [Discord](https://discord.gg/crUVCgrePz)
- [技术详解](TECHNICAL.md)

## 许可证

MIT

## 作者

XSY_xiaoqi
