# Minecraft TNT 实体研究报告

**这份文档为AI分析并攥写，可能描述有误，参考我的世界1.21.1的反混淆代码。**

## 1. 概述

TNT (Trinitrotoluene) 是Minecraft中最经典的爆炸性方块之一。本报告详细分析TNT实体的内部机制、行为逻辑和代码实现，基于Minecraft 1.21.1版本的反编译代码和Yarn映射。

---

## 2. 类继承结构

```
net.minecraft.entity.Entity (bsr)
    └── net.minecraft.entity.TntEntity (cji)
            └── 实现: bun (ProjectileEntity接口相关)
```

TNT实体是一个简单的实体，直接继承自`Entity`基类，不继承自`LivingEntity`或`MobEntity`。

---

## 3. 核心字段分析

### 3.1 静态常量

| 映射名 | 混淆名 | 类型 | 描述 |
|--------|--------|------|------|
| `FUSE_NBT_KEY` | `b` | `String` | NBT中引信时间的键名 |
| `FUSE` | `c` | `TrackedData<Integer>` | 同步的引信时间数据 |
| `BLOCK_STATE` | `d` | `TrackedData<BlockState>` | 同步的方块状态数据 |
| `DEFAULT_FUSE` | `e` | `int` | 默认引信时间 (80 ticks = 4秒) |
| `BLOCK_STATE_NBT_KEY` | `f` | `String` | NBT中方块状态的键名 |
| `TELEPORTED_EXPLOSION_BEHAVIOR` | `g` | `ExplosionBehavior` | 传送后的爆炸行为 |

### 3.2 实例字段

| 映射名 | 混淆名 | 类型 | 描述 |
|--------|--------|------|------|
| `causingEntity` | `h` | `LivingEntity` | 引起爆炸的实体(如点燃TNT的玩家) |
| `teleported` | `i` | `boolean` | 是否通过末地传送门传送 |

---

## 4. 核心方法分析

### 4.1 构造方法

```java
// 从方块创建
public TntEntity(World world, double x, double y, double z, LivingEntity igniter)

// 从实体类型创建
public TntEntity(EntityType<? extends TntEntity> type, World world)
```

### 4.2 tick() 方法 (核心逻辑)

TNT实体的tick方法是其核心逻辑所在。根据映射：

```java
public void tick() {
    // 1. 基础物理更新
    // 2. 引信倒计时
    // 3. 爆炸检测
}
```

**关键流程：**

1. **物理计算**: TNT受重力影响，会在空气中下落
2. **引信递减**: 每tick减少引信时间
3. **爆炸触发**: 当引信时间 <= 0 时调用 `explode()`

### 4.3 explode() 方法

```java
private void explode() {
    // 创建爆炸
    // 爆炸威力固定为4.0
    float power = 4.0f;
    world.createExplosion(this, x, y, z, power, Explosion.DestructionType.DESTROY);
}
```

### 4.4 引信管理方法

```java
public void setFuse(int fuse)    // 设置引信时间
public int getFuse()              // 获取当前引信时间
```

### 4.5 方块状态管理

```java
public void setBlockState(BlockState state)  // 设置TNT的方块状态
public BlockState getBlockState()             // 获取方块状态
```

---

## 5. 轨道运算详解

### 5.1 物理系统架构

TNT实体的运动遵循Minecraft的标准物理系统：

```
Entity.tick()
├── baseTick() - 基础更新
├── tickPortalCooldown() - 传送门冷却
└── move(MovementType, Vec3d) - 移动处理
    ├── adjustMovementForCollisions() - 碰撞调整
    ├── moveRelative() - 相对移动
    └── applyGravity() - 重力应用
```

### 5.2 重力计算

```java
// Entity类中的重力处理
protected void applyGravity() {
    if (!this.hasNoGravity()) {
        this.setVelocity(this.getVelocity().add(0.0, -0.04, 0.0));
    }
}
```

**TNT重力参数：**
- 重力加速度: 0.04 blocks/tick²
- 空气阻力: 0.98 (每tick速度衰减)
- 最大下落速度: 约3.0 blocks/tick

### 5.3 碰撞检测算法

```java
// 碰撞检测流程
public Vec3d adjustMovementForCollisions(Vec3d movement) {
    // 1. 获取实体包围盒
    Box box = this.getBoundingBox();
    
    // 2. 计算碰撞形状
    List<VoxelShape> collisions = this.getWorld()
        .getEntityCollisions(this, box.stretch(movement));
    
    // 3. 沿各轴调整移动向量
    Vec3d adjusted = movement;
    if (collisions.size() != 0) {
        adjusted = movement.lengthSquared() == 0.0 
            ? movement 
            : Entity.adjustMovementForCollisions(this, movement, box, this.getWorld(), collisions);
    }
    
    return adjusted;
}
```

### 5.4 移动处理

```java
public void move(MovementType type, Vec3d movement) {
    // 1. 准备移动
    Vec3d adjustedMovement = this.adjustMovementForCollisions(movement);
    
    // 2. 更新位置
    this.setPos(this.getX() + adjustedMovement.x,
                this.getY() + adjustedMovement.y,
                this.getZ() + adjustedMovement.z);
    
    // 3. 更新包围盒
    this.setBoundingBox(this.getBoundingBox().offset(adjustedMovement));
    
    // 4. 检测着地
    this.onGround = movement.y != adjustedMovement.y && movement.y < 0.0;
    
    // 5. 应用摩擦力
    if (this.onGround) {
        this.setVelocity(this.getVelocity().multiply(0.7, 1.0, 0.7));
    }
}
```

### 5.5 TNT特有运动逻辑

```java
// TntEntity.tick() 中的运动处理
public void tick() {
    // 1. 处理之前的位置
    this.prevX = this.getX();
    this.prevY = this.getY();
    this.prevZ = this.getZ();
    
    // 2. 应用重力
    if (!this.hasNoGravity()) {
        this.setVelocity(this.getVelocity().add(0.0, -0.04, 0.0));
    }
    
    // 3. 移动实体
    this.move(MovementType.SELF, this.getVelocity());
    
    // 4. 应用空气阻力
    this.setVelocity(this.getVelocity().multiply(0.98));
    
    // 5. 水中行为
    if (this.isTouchingWater()) {
        this.setVelocity(this.getVelocity().multiply(0.85));
        // 水中不产生烟雾粒子
    }
    
    // 6. 引信处理
    this.setFuse(this.getFuse() - 1);
    if (this.getFuse() <= 0) {
        this.explode();
        this.discard();
    }
}
```

### 5.6 速度计算公式

```
每tick速度变化:
v_y(t+1) = (v_y(t) - 0.04) * 0.98  (空气中)
v_y(t+1) = (v_y(t) - 0.02) * 0.85  (水中)

水平速度:
v_x(t+1) = v_x(t) * 0.98  (空气中)
v_x(t+1) = v_x(t) * 0.85  (水中)
```

---

## 6. 爆炸运算详解

### 6.1 爆炸创建流程

```java
// TntEntity.explode()
private void explode() {
    float power = 4.0f;
    this.getWorld().createExplosion(
        this,                          // 爆炸源实体
        this.getX(), this.getY(), this.getZ(),  // 位置
        power,                         // 威力
        World.ExplosionSourceType.TNT  // 爆炸类型
    );
}

// World.createExplosion() 完整签名
public Explosion createExplosion(
    @Nullable Entity entity,           // 爆炸源实体
    @Nullable DamageSource damageSource, // 伤害来源
    @Nullable ExplosionBehavior behavior, // 爆炸行为
    double x, double y, double z,      // 爆炸中心
    float power,                       // 爆炸威力
    boolean createFire,                // 是否产生火焰
    World.ExplosionSourceType sourceType, // 爆炸源类型
    ParticleEffect particle,           // 粒子效果
    ParticleEffect emitterParticle,    // 发射器粒子
    SoundEvent soundEvent              // 音效
)
```

### 6.2 爆炸半径计算

```
爆炸半径 = 威力 * 2

TNT威力4.0 → 半径8格
苦力怕威力3.0 → 半径6格
闪电苦力怕威力6.0 → 半径12格
```

### 6.3 方块破坏算法

```java
// Explosion.collectBlocksAndDamageEntities()
public void collectBlocksAndDamageEntities() {
    // 1. 初始化
    Set<BlockPos> affectedBlocks = Sets.newHashSet();
    
    // 2. 射线追踪 (从爆炸中心向各方向发射射线)
    for (int x = 0; x < 16; ++x) {
        for (int y = 0; y < 16; ++y) {
            for (int z = 0; z < 16; ++z) {
                if (x == 0 || x == 15 || y == 0 || y == 15 || z == 0 || z == 15) {
                    // 计算射线方向
                    double dx = (x / 15.0 - 0.5) * 2.0;
                    double dy = (y / 15.0 - 0.5) * 2.0;
                    double dz = (z / 15.0 - 0.5) * 2.0;
                    
                    // 归一化
                    double length = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    dx /= length; dy /= length; dz /= length;
                    
                    // 射线追踪
                    float currentPower = this.power * (0.7F + this.random.nextFloat() * 0.6F);
                    BlockPos pos = BlockPos.ofFloored(this.x, this.y, this.z);
                    
                    while (currentPower > 0.0F) {
                        // 检查方块抗性
                        BlockState state = this.world.getBlockState(pos);
                        FluidState fluid = this.world.getFluidState(pos);
                        
                        float resistance = this.behavior.getEffectiveExplosionResistance(
                            this, this.world, pos, state, fluid
                        );
                        
                        currentPower -= (resistance + 0.3F) * 0.3F;
                        
                        if (currentPower > 0.0F && this.behavior.canDestroyBlock(
                            this, this.world, pos, state, currentPower
                        )) {
                            affectedBlocks.add(pos);
                        }
                        
                        // 移动到下一个位置
                        pos = pos.add(dx, dy, dz);
                        currentPower -= 0.225F;
                    }
                }
            }
        }
    }
    
    this.affectedBlocks.addAll(affectedBlocks);
}
```

### 6.4 实体伤害计算

```java
// 实体伤害计算
private void damageEntities() {
    // 1. 获取范围内的实体
    List<Entity> entities = this.world.getOtherEntities(
        this.entity,
        new Box(this.x - power*2, this.y - power*2, this.z - power*2,
                this.x + power*2, this.y + power*2, this.z + power*2)
    );
    
    // 2. 计算每个实体受到的伤害
    for (Entity entity : entities) {
        // 计算距离
        double distance = Math.sqrt(
            entity.squaredDistanceTo(this.x, this.y, this.z)
        ) / (this.power * 2.0);
        
        if (distance <= 1.0) {
            // 计算暴露程度 (射线追踪)
            double exposure = getExposure(this.x, this.y, this.z, entity);
            
            // 计算伤害
            double impact = (1.0 - distance) * exposure;
            float damage = (float)((impact * impact + impact) / 2.0 * 7.0 * this.power + 1.0);
            
            // 应用伤害
            entity.damage(this.damageSource, damage);
            
            // 计算击退
            double knockback = impact * this.behavior.getKnockbackModifier(entity);
            Vec3d velocity = new Vec3d(
                entity.getX() - this.x,
                entity.getEyeY() - this.y,
                entity.getZ() - this.z
            ).normalize().multiply(knockback);
            
            entity.setVelocity(entity.getVelocity().add(velocity));
        }
    }
}
```

### 6.5 暴露程度计算 (getExposure)

```java
// 计算实体暴露在爆炸中的程度
public static float getExposure(Vec3d source, Entity entity) {
    Box box = entity.getBoundingBox();
    double stepSize = 1.0 / (box.getLengthX() * 2.0 + 1.0);
    
    int hits = 0;
    int total = 0;
    
    for (double x = 0; x <= 1.0; x += stepSize) {
        for (double y = 0; y <= 1.0; y += stepSize) {
            for (double z = 0; z <= 1.0; z += stepSize) {
                Vec3d target = new Vec3d(
                    MathHelper.lerp(x, box.minX, box.maxX),
                    MathHelper.lerp(y, box.minY, box.maxY),
                    MathHelper.lerp(z, box.minZ, box.maxZ)
                );
                
                // 射线追踪检查是否被方块阻挡
                if (!entity.getWorld().raycastBlock(source, target, pos -> false).isPresent()) {
                    hits++;
                }
                total++;
            }
        }
    }
    
    return (float)hits / (float)total;
}
```

### 6.6 爆炸应用效果

```java
// Explosion.affectWorld()
public void affectWorld(boolean particles) {
    // 1. 生成粒子效果
    if (particles) {
        this.world.addParticle(this.particle, this.x, this.y, this.z, 1.0, 0.0, 0.0);
        this.world.addParticle(this.emitterParticle, this.x, this.y, this.z, 1.0, 0.0, 0.0);
    }
    
    // 2. 播放音效
    this.world.playSound(null, this.x, this.y, this.z, this.soundEvent, SoundCategory.BLOCKS, 4.0F, 1.0F);
    
    // 3. 破坏方块
    if (this.destructionType.shouldDestroy()) {
        for (BlockPos pos : this.affectedBlocks) {
            BlockState state = this.world.getBlockState(pos);
            
            // 方块掉落
            if (!state.isAir()) {
                if (this.world instanceof ServerWorld serverWorld) {
                    BlockEntity blockEntity = state.hasBlockEntity() 
                        ? this.world.getBlockEntity(pos) : null;
                    
                    // 掉落物处理
                    Block.dropStacks(state, serverWorld, pos, blockEntity, this.entity, ItemStack.EMPTY);
                }
                
                // 移除方块
                this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                
                // 触发连锁反应 (如其他TNT)
                state.getBlock().onDestroyedByExplosion(this.world, pos, this);
            }
        }
    }
    
    // 4. 产生火焰 (如需要)
    if (this.createFire) {
        for (BlockPos pos : this.affectedBlocks) {
            if (this.random.nextInt(3) == 0 && 
                this.world.getBlockState(pos).isAir() &&
                this.world.getBlockState(pos.down()).isOpaque()) {
                this.world.setBlockState(pos, Blocks.FIRE.getDefaultState());
            }
        }
    }
}
```

### 6.7 爆炸行为接口

```java
// ExplosionBehavior 类
public class ExplosionBehavior {
    // 获取击退修正 (默认1.0)
    public float getKnockbackModifier(Entity entity) {
        return 1.0F;
    }
    
    // 是否应该伤害实体
    public boolean shouldDamage(Explosion explosion, Entity entity) {
        return true;
    }
    
    // 是否可以破坏方块
    public boolean canDestroyBlock(Explosion explosion, BlockView world, 
                                   BlockPos pos, BlockState state, float power) {
        return state.getBlock().shouldDropItemsOnExplosion(explosion);
    }
    
    // 获取有效爆炸抗性
    public Optional<Float> getBlastResistance(Explosion explosion, BlockView world,
                                               BlockPos pos, BlockState blockState, 
                                               FluidState fluidState) {
        return Optional.of(Math.max(blockState.getBlock().getBlastResistance(), 
                                    fluidState.getBlastResistance()));
    }
    
    // 计算伤害
    public float calculateDamage(Explosion explosion, Entity entity) {
        float maxDamage = explosion.power * 2.0F;
        // ... 伤害计算逻辑
        return maxDamage;
    }
}
```

---

## 7. 纹理与渲染计算

### 7.1 TntEntityRenderer 结构

```java
// TntEntityRenderer 类
public class TntEntityRenderer extends EntityRenderer<TntEntity> {
    private final BlockRenderManager blockRenderManager;
    
    public TntEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.blockRenderManager = ctx.getBlockRenderManager();
        this.shadowRadius = 0.5F;  // 阴影半径
    }
    
    @Override
    public Identifier getTexture(TntEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
    
    @Override
    public void render(TntEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, 
                       int light) {
        // 渲染逻辑
    }
}
```

### 7.2 渲染流程

```java
public void render(TntEntity entity, float yaw, float tickDelta,
                   MatrixStack matrices, VertexConsumerProvider vertexConsumers, 
                   int light) {
    // 1. 获取方块状态
    BlockState blockState = entity.getBlockState();
    
    // 2. 设置变换矩阵
    matrices.push();
    matrices.translate(-0.5, 0.0, -0.5);  // 居中偏移
    
    // 3. 计算闪烁效果
    int fuse = entity.getFuse();
    boolean shouldFlash = fuse / 5 % 2 == 0 && fuse <= 30;
    
    // 4. 渲染方块
    if (shouldFlash) {
        // 闪烁时渲染白色叠加层
        renderFlashingBlock(blockRenderManager, blockState, matrices, 
                           vertexConsumers, light, true);
    } else {
        // 正常渲染
        blockRenderManager.renderBlockAsEntity(blockState, matrices, 
                                               vertexConsumers, light, OverlayTexture.DEFAULT_UV);
    }
    
    matrices.pop();
    super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
}
```

### 7.3 闪烁效果实现

```java
// TntMinecartEntityRenderer.renderFlashingBlock()
public static void renderFlashingBlock(BlockRenderManager blockRenderManager,
                                       BlockState state, MatrixStack matrices,
                                       VertexConsumerProvider vertexConsumers,
                                       int light, boolean drawFlash) {
    // 1. 渲染正常方块
    blockRenderManager.renderBlockAsEntity(state, matrices, vertexConsumers, 
                                           light, OverlayTexture.DEFAULT_UV);
    
    // 2. 渲染闪烁叠加层
    if (drawFlash) {
        VertexConsumer flashConsumer = vertexConsumers.getBuffer(
            RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
        );
        
        // 使用白色半透明叠加
        int overlay = OverlayTexture.packUv(0.0F, 10.0F);  // 白色叠加
        
        blockRenderManager.renderBlockAsEntity(state, matrices, 
                                               vertexConsumers, 0xF000F0, overlay);
    }
}
```

### 7.4 光照计算

```java
// TNT实体的光照计算
public int getBlockLight(TntEntity entity, BlockPos pos) {
    // TNT实体不发光，使用环境光照
    return entity.getWorld().getLightLevel(LightType.BLOCK, pos);
}

// 实际渲染时的光照
int light = LightmapTextureManager.pack(
    getBlockLight(entity, pos),    // 方块光照 (0-15)
    getSkyLight(entity, pos)       // 天空光照 (0-15)
);
```

### 7.5 粒子效果

```java
// TNT产生的粒子效果
public void tick() {
    // ... 其他逻辑
    
    // 产生烟雾粒子
    if (this.isTouchingWater()) {
        // 水中不产生粒子
    } else {
        // 空气中产生烟雾
        this.getWorld().addParticle(
            ParticleTypes.SMOKE,
            this.getX() + random.nextGaussian() * 0.1,
            this.getY() + random.nextGaussian() * 0.1,
            this.getZ() + random.nextGaussian() * 0.1,
            0.0, 0.0, 0.0
        );
    }
}
```

### 7.6 爆炸粒子

```java
// 爆炸时的粒子效果
// ParticleTypes 常量
ParticleTypes.EXPLOSION           // 普通爆炸粒子
ParticleTypes.EXPLOSION_EMITTER   // 大型爆炸粒子

// 爆炸音效
SoundEvents.ENTITY_GENERIC_EXPLODE  // 通用爆炸音效
SoundEvents.ENTITY_TNT_PRIMED       // TNT点燃音效
```

---

## 8. TNT方块机制

### 8.1 TntBlock 类结构

```java
public class TntBlock extends Block {
    public static final BooleanProperty UNSTABLE = Properties.UNSTABLE;
    
    // 点燃TNT
    public void primeTnt(World world, BlockPos pos) {
        primeTnt(world, pos, null);
    }
    
    public void primeTnt(World world, BlockPos pos, @Nullable LivingEntity igniter) {
        if (!world.isClient) {
            TntEntity tnt = new TntEntity(world, 
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, igniter);
            world.spawnEntity(tnt);
            world.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(),
                SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }
}
```

### 8.2 点燃触发条件

```java
// 1. 红石信号触发
public void neighborUpdate(BlockState state, World world, BlockPos pos, 
                          Block sourceBlock, BlockPos sourcePos, boolean notify) {
    if (world.isReceivingRedstonePower(pos)) {
        primeTnt(world, pos);
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }
}

// 2. 火焰/熔岩接触
public void onBlockAdded(BlockState state, World world, BlockPos pos, 
                        BlockState oldState, boolean notify) {
    if (!oldState.isOf(state.getBlock())) {
        if (world.getBlockState(pos.up()).isOf(Blocks.FIRE) ||
            world.getBlockState(pos.up()).isOf(Blocks.LAVA)) {
            primeTnt(world, pos);
        }
    }
}

// 3. 玩家使用打火石
public ActionResult onUse(BlockState state, World world, BlockPos pos, 
                         PlayerEntity player, Hand hand, BlockHitResult hit) {
    ItemStack stack = player.getStackInHand(hand);
    if (stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE)) {
        primeTnt(world, pos, player);
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
        stack.damage(1, player, p -> p.sendToolBreakStatus(hand));
        return ActionResult.SUCCESS;
    }
    return super.onUse(state, world, pos, player, hand, hit);
}

// 4. 爆炸连锁反应
public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
    primeTnt(world, pos);
}
```

### 8.3 UNSTABLE 属性

```java
// UNSTABLE 属性控制TNT是否在破坏时点燃
public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
    if (!world.isClient && !player.isCreative() && state.get(UNSTABLE)) {
        primeTnt(world, pos);
    }
    return super.onBreak(world, pos, state, player);
}

// 从结构自然生成的TNT带有UNSTABLE=true
// 玩家放置的TNT默认UNSTABLE=false
```

---

## 9. 连锁爆炸机制

### 9.1 爆炸触发TNT

```java
// Explosion.affectWorld() 中调用
for (BlockPos pos : this.affectedBlocks) {
    BlockState state = this.world.getBlockState(pos);
    if (!state.isAir()) {
        // ... 方块破坏逻辑
        
        // 触发方块的爆炸回调
        state.getBlock().onDestroyedByExplosion(this.world, pos, this);
    }
}

// TntBlock.onDestroyedByExplosion()
public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
    if (!world.isClient) {
        TntEntity tnt = new TntEntity(world, 
            pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 
            explosion.getCausingEntity());
        // 随机引信时间 (更短的引信)
        tnt.setFuse((byte)(world.random.nextInt(10) + 10));
        world.spawnEntity(tnt);
    }
}
```

### 9.2 连锁爆炸延迟

```
正常TNT引信: 80 ticks (4秒)
爆炸触发的TNT引信: 10-20 ticks (0.5-1秒)

这确保了连锁爆炸的视觉效果
```

---

## 10. 网络同步机制

### 10.1 服务端 EntityTrackerEntry

```java
public class EntityTrackerEntry {
    // 每tick调用
    public void tick() {
        // 1. 更新实体位置追踪
        // 2. 同步实体数据
        // 3. 发送更新包给客户端
    }
    
    // 同步实体数据
    public void syncEntityData();
    
    // 发送同步包
    public void sendSyncPacket(Packet<?> packet);
}
```

### 10.2 客户端 ClientWorld

```java
public class ClientWorld {
    // 客户端tick实体
    public void tickEntity(Entity entity) {
        // 1. 更新实体位置
        // 2. 执行实体tick逻辑
        // 3. 渲染更新
    }
}
```

### 10.3 关键网络包

| 包名 | 用途 |
|------|------|
| `EntitySpawnS2CPacket` | 实体生成通知 |
| `EntityS2CPacket.MoveRelative` | 实体相对移动 |
| `EntityS2CPacket.RotateAndMoveRelative` | 实体旋转和移动 |
| `EntityS2CPacket.Rotate` | 实体旋转 |
| `ExplosionS2CPacket` | 爆炸效果通知 |

---

## 11. 弱加载区块中的TNT行为

### 11.1 问题分析

在弱加载区块中，TNT的行为取决于：

1. **服务端tick**: 
   - 弱加载区块中，实体tick仍然执行
   - 引信继续倒计时
   - 物理计算继续

2. **客户端同步**:
   - `EntityTrackerEntry.tick()` 可能不执行
   - 客户端不收到位置更新包
   - 动画"冻结"

### 11.2 代码路径分析

```
服务端:
ServerWorld.tick()
└── tickEntity(entity)
    └── EntityTrackerEntry.tick()
        └── 检查区块加载状态
            └── 如果弱加载: 可能跳过同步包发送

客户端:
ClientWorld.tickEntity(entity)
└── TntEntity.tick()
    └── 引信递减 (服务端控制)
    └── 物理更新 (本地计算)
```

### 11.3 Carpet-VisualFix 的解决方案

通过在服务端注入`EntityTrackerEntry.tick()`：

```java
@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
private void onTick(CallbackInfo ci) {
    if (isInLazyChunk()) {
        ci.cancel();  // 取消tick，停止发送更新包
    }
}
```

客户端通过取消`ClientWorld.tickEntity()`：

```java
@Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
private void onTickEntity(Entity entity, CallbackInfo ci) {
    if (isInLazyChunk(entity)) {
        ci.cancel();  // 取消客户端tick
    }
}
```

---

## 12. TNT矿车对比

### 12.1 TntMinecartEntity 结构

```java
public class TntMinecartEntity extends AbstractMinecartEntity {
    private int fuseTicks;  // 引信tick数
    
    public void prime();           // 点燃
    public int getFuseTicks();     // 获取引信时间
    public boolean isPrimed();     // 是否已点燃
    public void explode(double power);  // 爆炸
}
```

### 12.2 主要区别

| 特性 | TntEntity | TntMinecartEntity |
|------|-----------|-------------------|
| 移动方式 | 重力+碰撞 | 铁轨系统 |
| 爆炸触发 | 引信倒计时 | 碰撞/手动触发 |
| 爆炸威力 | 固定4.0 | 可变 |
| 点燃方式 | 红石/火焰/打火石 | 撞击/激活 |

---

## 13. 相关实体类型

### 13.1 其他爆炸性实体

| 实体 | 类名 | 爆炸威力 |
|------|------|----------|
| 末地水晶 | `EndCrystalEntity` | 与末影龙相关 |
| 苦力怕 | `CreeperEntity` | 3.0/6.0(闪电) |
| 恶魂火球 | `FireballEntity` | 1.0 |
| 床/重生锚 | N/A | 5.0 |

### 13.2 爆炸抗性机制

```java
// 方块爆炸抗性
public float getBlastResistance();

// 实体爆炸抗性检查
public boolean isImmuneToExplosion();

// 能否被爆炸破坏
public boolean canExplosionDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power);
```

---

## 14. NBT数据结构

### 14.1 TNT实体NBT

```json
{
    "Fuse": 80,           // 引信时间 (ticks)
    "BlockState": {       // 方块状态
        "Name": "minecraft:tnt"
    }
}
```

### 14.2 关键NBT操作

```java
// 写入NBT
protected void writeCustomDataToNbt(NbtCompound nbt) {
    nbt.putShort("Fuse", (short)this.getFuse());
    nbt.put("BlockState", NbtHelper.fromBlockState(this.getBlockState()));
}

// 读取NBT
protected void readCustomDataFromNbt(NbtCompound nbt) {
    this.setFuse(nbt.getShort("Fuse"));
    this.setBlockState(NbtHelper.toBlockState(nbt.getCompound("BlockState")));
}
```

---

## 15. 总结

### 15.1 TNT实体特点

1. **简单结构**: 直接继承Entity，无复杂AI
2. **服务端主导**: 引信和爆炸由服务端控制
3. **网络同步**: 通过TrackedData和EntityTrackerEntry同步
4. **物理模拟**: 受重力影响，有碰撞检测

### 15.2 弱加载行为

1. 服务端继续tick，引信倒计时
2. 客户端可能不收到更新，导致动画"冻结"
3. 爆炸仍会正常发生

### 15.3 优化建议

对于Carpet-VisualFix模组：
- 服务端取消EntityTrackerEntry.tick()可停止同步
- 客户端取消tickEntity()可停止本地动画
- 两者配合实现完整的"冻结"效果

---

## 16. 特殊行为

### 16.1 末地传送门处理

```java
// TntEntity 中的末地传送门处理
private static final ExplosionBehavior TELEPORTED_EXPLOSION_BEHAVIOR = new ExplosionBehavior() {
    @Override
    public boolean canDestroyBlock(Explosion explosion, BlockView world, 
                                   BlockPos pos, BlockState state, float power) {
        // 传送后的TNT不破坏传送门方块
        return !state.isOf(Blocks.END_PORTAL) && 
               !state.isOf(Blocks.END_GATEWAY) &&
               super.canDestroyBlock(explosion, world, pos, state, power);
    }
};

// 传送检测
public void setTeleported(boolean teleported) {
    this.teleported = teleported;
    if (teleported) {
        // 使用特殊的爆炸行为
        this.world.createExplosion(this, this.getX(), this.getY(), this.getZ(), 
                                   4.0f, TELEPORTED_EXPLOSION_BEHAVIOR);
    }
}
```

### 16.2 游戏规则影响

```java
// 与TNT相关的游戏规则
GameRules.TNT_EXPLOSION_DROP_DECAY  // TNT爆炸掉落物衰减
GameRules.MOB_EXPLOSION_DROP_DECAY  // 生物爆炸掉落物衰减
GameRules.BLOCK_EXPLOSION_DROP_DECAY // 方块爆炸掉落物衰减

// 掉落衰减逻辑
if (world.getGameRules().getBoolean(GameRules.TNT_EXPLOSION_DROP_DECAY)) {
    // 掉落物有概率消失
    float dropChance = 1.0f / (float)(affectedBlocks.size());
    // ... 应用衰减
}
```

### 16.3 水中爆炸行为

```java
// 水中爆炸的特殊处理
public void explode() {
    // 检查是否在水中
    boolean inWater = this.isTouchingWater();
    
    // 水中爆炸不产生火焰
    this.getWorld().createExplosion(
        this, 
        this.getX(), this.getY(), this.getZ(), 
        4.0f,
        false,  // 不产生火焰
        World.ExplosionSourceType.TNT
    );
    
    // 水中爆炸威力不受影响，但视觉效果不同
}
```

### 16.4 击退抗性计算

```java
// 实体击退抗性
public static final RegistryEntry<Enchantment> GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE;

// 击退计算
public void takeKnockback(double strength, double x, double z) {
    // 考虑击退抗性
    double resistance = this.getAttributeValue(EntityAttributes.GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE);
    strength *= (1.0 - resistance);
    
    // 应用击退
    this.setVelocity(this.getVelocity().add(
        x * strength,
        0.1,  // 垂直击退
        z * strength
    ));
}
```

---

## 17. 性能优化

### 17.1 爆炸缓存

```java
// Explosion 类中的缓存机制
private final ObjectArrayList<BlockPos> affectedBlocks;  // 使用快速列表
private final Map<PlayerEntity, Vec3d> affectedPlayers;  // 玩家缓存

// 避免重复计算
private boolean blocksCollected = false;
private boolean entitiesDamaged = false;
```

### 17.2 射线追踪优化

```java
// 使用增量计算避免重复的平方根运算
// 预计算方向向量
double dx = (x / 15.0 - 0.5) * 2.0;
double dy = (y / 15.0 - 0.5) * 2.0;
double dz = (z / 15.0 - 0.5) * 2.0;

// 归一化一次
double length = Math.sqrt(dx*dx + dy*dy + dz*dz);
dx /= length; dy /= length; dz /= length;

// 增量移动
pos = pos.add(dx, dy, dz);
```

### 17.3 方块状态缓存

```java
// BlockState 缓存
private final BlockState cachedBlockState;

// 避免每次渲染都获取
public BlockState getBlockState() {
    return this.cachedBlockState;
}
```

---

## 18. 调试信息

### 18.1 关键日志点

```java
// TNT点燃
LOGGER.debug("TNT primed at {} by {}", pos, igniter);

// 爆炸发生
LOGGER.debug("TNT exploded at {} with power {}", pos, power);

// 方块破坏数量
LOGGER.debug("Explosion destroyed {} blocks", affectedBlocks.size());
```

### 18.2 性能监控

```java
// 爆炸性能计时
long startTime = System.nanoTime();
explosion.collectBlocksAndDamageEntities();
long collectTime = System.nanoTime() - startTime;

startTime = System.nanoTime();
explosion.affectWorld(true);
long affectTime = System.nanoTime() - startTime;

LOGGER.debug("Explosion timing - collect: {}ms, affect: {}ms", 
             collectTime / 1_000_000, affectTime / 1_000_000);
```

---

## 附录A: 混淆映射参考

| 原名 | 混淆名 | Yarn映射名 |
|------|--------|------------|
| TntEntity | cji | net/minecraft/entity/TntEntity |
| Entity | bsr | net/minecraft/entity/Entity |
| Explosion | dco | net/minecraft/world/explosion/Explosion |
| EntityTrackerEntry | aqt | net/minecraft/server/network/EntityTrackerEntry |
| ClientWorld | fzf | net/minecraft/client/world/ClientWorld |
| TntEntityRenderer | gmz | net/minecraft/client/render/entity/TntEntityRenderer |
| TntMinecartEntity | cpf | net/minecraft/entity/vehicle/TntMinecartEntity |
| TntBlock | doi | net/minecraft/block/TntBlock |
| ExplosionBehavior | dcp | net/minecraft/world/explosion/ExplosionBehavior |
| DestructionType | dco$a | net/minecraft/world/explosion/Explosion$DestructionType |

---

## 附录B: 关键数值参考

### B.1 TNT参数

| 参数 | 值 | 描述 |
|------|-----|------|
| 默认引信 | 80 ticks | 4秒 |
| 爆炸威力 | 4.0 | 标准TNT威力 |
| 爆炸半径 | 8格 | 威力 × 2 |
| 重力加速度 | 0.04 | blocks/tick² |
| 空气阻力 | 0.98 | 每tick衰减 |
| 水中阻力 | 0.85 | 每tick衰减 |
| 阴影半径 | 0.5 | 渲染参数 |

### B.2 爆炸伤害计算

```
最大伤害 = 威力 × 7 + 1
TNT最大伤害 = 4 × 7 + 1 = 29 (无护甲)

伤害衰减公式:
damage = (impact² + impact) / 2 × 7 × power + 1
其中 impact = (1 - distance) × exposure
```

### B.3 方块爆炸抗性

| 方块 | 爆炸抗性 |
|------|----------|
| 基岩 | 3600000.0 |
| 末地石 | 9.0 |
| 石头 | 6.0 |
| 泥土 | 0.5 |
| TNT | 0.0 |

---

## 附录C: 相关音效和粒子

### C.1 音效

| 音效事件 | 用途 |
|----------|------|
| `ENTITY_TNT_PRIMED` | TNT点燃 |
| `ENTITY_GENERIC_EXPLODE` | 爆炸 |

### C.2 粒子效果

| 粒子类型 | 用途 |
|----------|------|
| `SMOKE` | TNT燃烧时 |
| `EXPLOSION` | 小型爆炸 |
| `EXPLOSION_EMITTER` | 大型爆炸 |

---

*报告生成时间: 2026-03-19*
*Minecraft版本: 1.21.1*
*映射版本: Yarn 1.21.1+build.3*
