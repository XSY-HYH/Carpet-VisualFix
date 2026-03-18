# Carpet-VisualFix

[中文说明](docs/README_zh.md)

A Fabric mod for Minecraft 1.21.1 that fixes entity animations in lazy-loaded chunks.

Requires [Carpet Mod](https://modrinth.com/mod/carpet).

## What it does

Entities in lazy-loaded chunks (outside simulation distance but within render distance) continue to animate on the client even though the server isn't updating them. This mod freezes those entities in place, making their visual state match what's actually happening on the server.

## Carpet Rule

`entityRenderingFix` - Fixes entity animations in lazy-loaded chunks (default: true)

## Requirements

- Minecraft 1.21.1
- Fabric API
- Carpet Mod

## Links

- [Modrinth](https://modrinth.com/project/carpet-visualfix/)
- [GitHub](https://github.com/XSY-HYH/Carpet-VisualFix)
- [Issues](https://github.com/XSY-HYH/Carpet-VisualFix/issues)
- [Discord](https://discord.gg/crUVCgrePz)
- [Technical Details](docs/TECHNICAL.md)

## License

MIT

## Author

XSY_xiaoqi

## Research

- [Minecraft TNT Entity Research Report (English)](docs/Minecraft_TNT_Research_Report_en.md)
- [Minecraft TNT 实体研究报告 (中文)](docs/Minecraft_TNT_Research_Report_zh.md)
