# Technical Details

[中文版本](#中文版本)

## How it works

The mod uses a server-client architecture to freeze entities in lazy-loaded chunks.

### Server-side

`EntityTrackerEntryMixin` intercepts the entity tracking system. When an entity is in a lazy-loaded chunk (detected via `ChunkLevelType.INACCESSIBLE`), the mixin stops sending entity update packets to clients. This prevents the client from receiving new position/metadata updates.

### Client-side

`ClientWorldMixin` cancels entity ticks for entities in lazy-loaded chunks. Player entities are excluded to avoid affecting player movement. The chunk status is determined using `LazyChunkHelper`, which checks if the chunk's level type is `INACCESSIBLE`.

### Why both sides?

- Server-side packet suppression ensures no stale data reaches the client
- Client-side tick cancellation prevents the client from predicting entity behavior
- Together, they create a consistent frozen state

---

## 中文版本

## 工作原理

本模组采用服务端-客户端架构来冻结弱加载区块内的实体。

### 服务端

`EntityTrackerEntryMixin` 拦截实体追踪系统。当实体处于弱加载区块（通过 `ChunkLevelType.INACCESSIBLE` 检测）时，mixin 停止向客户端发送实体更新数据包。这防止客户端收到新的位置/元数据更新。

### 客户端

`ClientWorldMixin` 取消弱加载区块内实体的 tick。玩家实体被排除，避免影响玩家移动。区块状态通过 `LazyChunkHelper` 确定，它检查区块的级别类型是否为 `INACCESSIBLE`。

### 为什么需要两端？

- 服务端数据包抑制确保没有过期数据发送到客户端
- 客户端 tick 取消防止客户端预测实体行为
- 两者配合，创造一致的冻结状态
