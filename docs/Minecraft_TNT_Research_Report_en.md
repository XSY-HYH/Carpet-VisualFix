# Minecraft TNT Entity Research Report

**This document was analyzed and written by AI, so the descriptions might be inaccurate. It references the deobfuscated code for Minecraft 1.21.1.**

## 1. Overview

TNT (Trinitrotoluene) is one of the most classic explosive blocks in Minecraft. This report provides a detailed analysis of the internal mechanisms, behavioral logic, and code implementation of TNT entities, based on decompiled code and Yarn mappings from Minecraft version 1.21.1.

---

## 2. Class Inheritance Structure

```
net.minecraft.entity.Entity (bsr)
    └── net.minecraft.entity.TntEntity (cji)
            └── Implements: bun (ProjectileEntity interface related)
```

The TNT entity is a simple entity that directly inherits from the `Entity` base class, not from `LivingEntity` or `MobEntity`.

---

## 3. Core Field Analysis

### 3.1 Static Constants

| Mapped Name | Obfuscated Name | Type | Description |
|-------------|-----------------|------|-------------|
| `FUSE_NBT_KEY` | `b` | `String` | Key name for fuse time in NBT |
| `FUSE` | `c` | `TrackedData<Integer>` | Synchronized fuse time data |
| `BLOCK_STATE` | `d` | `TrackedData<BlockState>` | Synchronized block state data |
| `DEFAULT_FUSE` | `e` | `int` | Default fuse time (80 ticks = 4 seconds) |
| `BLOCK_STATE_NBT_KEY` | `f` | `String` | Key name for block state in NBT |
| `TELEPORTED_EXPLOSION_BEHAVIOR` | `g` | `ExplosionBehavior` | Explosion behavior after teleportation |

### 3.2 Instance Fields

| Mapped Name | Obfuscated Name | Type | Description |
|-------------|-----------------|------|-------------|
| `causingEntity` | `h` | `LivingEntity` | Entity that caused the explosion (e.g., player who lit the TNT) |
| `teleported` | `i` | `boolean` | Whether teleported through End portal |

---

## 4. Core Method Analysis

### 4.1 Constructors

```java
// Create from block
public TntEntity(World world, double x, double y, double z, LivingEntity igniter)

// Create from entity type
public TntEntity(EntityType<? extends TntEntity> type, World world)
```

### 4.2 tick() Method (Core Logic)

The tick method of the TNT entity is where its core logic resides. According to mappings:

```java
public void tick() {
    // 1. Basic physics update
    // 2. Fuse countdown
    // 3. Explosion detection
}
```

**Key Processes:**

1. **Physics Calculation**: TNT is affected by gravity and falls through the air
2. **Fuse Decrement**: Fuse time decreases each tick
3. **Explosion Trigger**: Calls `explode()` when fuse time <= 0

### 4.3 explode() Method

```java
private void explode() {
    // Create explosion
    // Fixed explosion power of 4.0
    float power = 4.0f;
    world.createExplosion(this, x, y, z, power, Explosion.DestructionType.DESTROY);
}
```

### 4.4 Fuse Management Methods

```java
public void setFuse(int fuse)    // Set fuse time
public int getFuse()              // Get current fuse time
```

### 4.5 Block State Management

```java
public void setBlockState(BlockState state)  // Set TNT's block state
public BlockState getBlockState()             // Get block state
```

---

## 5. Trajectory Calculation Details

### 5.1 Physics System Architecture

TNT entity movement follows Minecraft's standard physics system:

```
Entity.tick()
├── baseTick() - Basic update
├── tickPortalCooldown() - Portal cooldown
└── move(MovementType, Vec3d) - Movement processing
    ├── adjustMovementForCollisions() - Collision adjustment
    ├── moveRelative() - Relative movement
    └── applyGravity() - Gravity application
```

### 5.2 Gravity Calculation

```java
// Gravity handling in Entity class
protected void applyGravity() {
    if (!this.hasNoGravity()) {
        this.setVelocity(this.getVelocity().add(0.0, -0.04, 0.0));
    }
}
```

**TNT Gravity Parameters:**
- Gravity acceleration: 0.04 blocks/tick²
- Air resistance: 0.98 (velocity decay per tick)
- Maximum fall speed: ~3.0 blocks/tick

### 5.3 Collision Detection Algorithm

```java
// Collision detection flow
public Vec3d adjustMovementForCollisions(Vec3d movement) {
    // 1. Get entity bounding box
    Box box = this.getBoundingBox();
    
    // 2. Calculate collision shapes
    List<VoxelShape> collisions = this.getWorld()
        .getEntityCollisions(this, box.stretch(movement));
    
    // 3. Adjust movement vector along each axis
    Vec3d adjusted = movement;
    if (collisions.size() != 0) {
        adjusted = movement.lengthSquared() == 0.0 
            ? movement 
            : Entity.adjustMovementForCollisions(this, movement, box, this.getWorld(), collisions);
    }
    
    return adjusted;
}
```

### 5.4 Movement Processing

```java
public void move(MovementType type, Vec3d movement) {
    // 1. Prepare movement
    Vec3d adjustedMovement = this.adjustMovementForCollisions(movement);
    
    // 2. Update position
    this.setPos(this.getX() + adjustedMovement.x,
                this.getY() + adjustedMovement.y,
                this.getZ() + adjustedMovement.z);
    
    // 3. Update bounding box
    this.setBoundingBox(this.getBoundingBox().offset(adjustedMovement));
    
    // 4. Detect landing
    this.onGround = movement.y != adjustedMovement.y && movement.y < 0.0;
    
    // 5. Apply friction
    if (this.onGround) {
        this.setVelocity(this.getVelocity().multiply(0.7, 1.0, 0.7));
    }
}
```

### 5.5 TNT-Specific Movement Logic

```java
// Movement handling in TntEntity.tick()
public void tick() {
    // 1. Handle previous position
    this.prevX = this.getX();
    this.prevY = this.getY();
    this.prevZ = this.getZ();
    
    // 2. Apply gravity
    if (!this.hasNoGravity()) {
        this.setVelocity(this.getVelocity().add(0.0, -0.04, 0.0));
    }
    
    // 3. Move entity
    this.move(MovementType.SELF, this.getVelocity());
    
    // 4. Apply air resistance
    this.setVelocity(this.getVelocity().multiply(0.98));
    
    // 5. Water behavior
    if (this.isTouchingWater()) {
        this.setVelocity(this.getVelocity().multiply(0.85));
        // No smoke particles in water
    }
    
    // 6. Fuse handling
    this.setFuse(this.getFuse() - 1);
    if (this.getFuse() <= 0) {
        this.explode();
        this.discard();
    }
}
```

### 5.6 Velocity Calculation Formula

```
Velocity change per tick:
v_y(t+1) = (v_y(t) - 0.04) * 0.98  (in air)
v_y(t+1) = (v_y(t) - 0.02) * 0.85  (in water)

Horizontal velocity:
v_x(t+1) = v_x(t) * 0.98  (in air)
v_x(t+1) = v_x(t) * 0.85  (in water)
```

---

## 6. Explosion Calculation Details

### 6.1 Explosion Creation Flow

```java
// TntEntity.explode()
private void explode() {
    float power = 4.0f;
    this.getWorld().createExplosion(
        this,                          // Source entity
        this.getX(), this.getY(), this.getZ(),  // Position
        power,                         // Power
        World.ExplosionSourceType.TNT  // Explosion type
    );
}

// World.createExplosion() full signature
public Explosion createExplosion(
    @Nullable Entity entity,           // Source entity
    @Nullable DamageSource damageSource, // Damage source
    @Nullable ExplosionBehavior behavior, // Explosion behavior
    double x, double y, double z,      // Explosion center
    float power,                       // Explosion power
    boolean createFire,                // Whether to create fire
    World.ExplosionSourceType sourceType, // Explosion source type
    ParticleEffect particle,           // Particle effect
    ParticleEffect emitterParticle,    // Emitter particle
    SoundEvent soundEvent              // Sound event
)
```

### 6.2 Explosion Radius Calculation

```
Explosion radius = Power * 2

TNT power 4.0 → Radius 8 blocks
Creeper power 3.0 → Radius 6 blocks
Charged Creeper power 6.0 → Radius 12 blocks
```

### 6.3 Block Destruction Algorithm

```java
// Explosion.collectBlocksAndDamageEntities()
public void collectBlocksAndDamageEntities() {
    // 1. Initialize
    Set<BlockPos> affectedBlocks = Sets.newHashSet();
    
    // 2. Ray tracing (emit rays from explosion center in all directions)
    for (int x = 0; x < 16; ++x) {
        for (int y = 0; y < 16; ++y) {
            for (int z = 0; z < 16; ++z) {
                if (x == 0 || x == 15 || y == 0 || y == 15 || z == 0 || z == 15) {
                    // Calculate ray direction
                    double dx = (x / 15.0 - 0.5) * 2.0;
                    double dy = (y / 15.0 - 0.5) * 2.0;
                    double dz = (z / 15.0 - 0.5) * 2.0;
                    
                    // Normalize
                    double length = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    dx /= length; dy /= length; dz /= length;
                    
                    // Ray tracing
                    float currentPower = this.power * (0.7F + this.random.nextFloat() * 0.6F);
                    BlockPos pos = BlockPos.ofFloored(this.x, this.y, this.z);
                    
                    while (currentPower > 0.0F) {
                        // Check block resistance
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
                        
                        // Move to next position
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

### 6.4 Entity Damage Calculation

```java
// Entity damage calculation
private void damageEntities() {
    // 1. Get entities in range
    List<Entity> entities = this.world.getOtherEntities(
        this.entity,
        new Box(this.x - power*2, this.y - power*2, this.z - power*2,
                this.x + power*2, this.y + power*2, this.z + power*2)
    );
    
    // 2. Calculate damage for each entity
    for (Entity entity : entities) {
        // Calculate distance
        double distance = Math.sqrt(
            entity.squaredDistanceTo(this.x, this.y, this.z)
        ) / (this.power * 2.0);
        
        if (distance <= 1.0) {
            // Calculate exposure (ray tracing)
            double exposure = getExposure(this.x, this.y, this.z, entity);
            
            // Calculate damage
            double impact = (1.0 - distance) * exposure;
            float damage = (float)((impact * impact + impact) / 2.0 * 7.0 * this.power + 1.0);
            
            // Apply damage
            entity.damage(this.damageSource, damage);
            
            // Calculate knockback
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

### 6.5 Exposure Calculation (getExposure)

```java
// Calculate how exposed an entity is to the explosion
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
                
                // Ray trace to check if blocked by blocks
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

### 6.6 Explosion Application Effects

```java
// Explosion.affectWorld()
public void affectWorld(boolean particles) {
    // 1. Spawn particle effects
    if (particles) {
        this.world.addParticle(this.particle, this.x, this.y, this.z, 1.0, 0.0, 0.0);
        this.world.addParticle(this.emitterParticle, this.x, this.y, this.z, 1.0, 0.0, 0.0);
    }
    
    // 2. Play sound
    this.world.playSound(null, this.x, this.y, this.z, this.soundEvent, SoundCategory.BLOCKS, 4.0F, 1.0F);
    
    // 3. Destroy blocks
    if (this.destructionType.shouldDestroy()) {
        for (BlockPos pos : this.affectedBlocks) {
            BlockState state = this.world.getBlockState(pos);
            
            // Block drops
            if (!state.isAir()) {
                if (this.world instanceof ServerWorld serverWorld) {
                    BlockEntity blockEntity = state.hasBlockEntity() 
                        ? this.world.getBlockEntity(pos) : null;
                    
                    // Drop handling
                    Block.dropStacks(state, serverWorld, pos, blockEntity, this.entity, ItemStack.EMPTY);
                }
                
                // Remove block
                this.world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                
                // Trigger chain reaction (e.g., other TNT)
                state.getBlock().onDestroyedByExplosion(this.world, pos, this);
            }
        }
    }
    
    // 4. Create fire (if needed)
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

### 6.7 Explosion Behavior Interface

```java
// ExplosionBehavior class
public class ExplosionBehavior {
    // Get knockback modifier (default 1.0)
    public float getKnockbackModifier(Entity entity) {
        return 1.0F;
    }
    
    // Whether should damage entity
    public boolean shouldDamage(Explosion explosion, Entity entity) {
        return true;
    }
    
    // Whether can destroy block
    public boolean canDestroyBlock(Explosion explosion, BlockView world, 
                                   BlockPos pos, BlockState state, float power) {
        return state.getBlock().shouldDropItemsOnExplosion(explosion);
    }
    
    // Get effective blast resistance
    public Optional<Float> getBlastResistance(Explosion explosion, BlockView world,
                                               BlockPos pos, BlockState blockState, 
                                               FluidState fluidState) {
        return Optional.of(Math.max(blockState.getBlock().getBlastResistance(), 
                                    fluidState.getBlastResistance()));
    }
    
    // Calculate damage
    public float calculateDamage(Explosion explosion, Entity entity) {
        float maxDamage = explosion.power * 2.0F;
        // ... damage calculation logic
        return maxDamage;
    }
}
```

---

## 7. Texture and Rendering Calculation

### 7.1 TntEntityRenderer Structure

```java
// TntEntityRenderer class
public class TntEntityRenderer extends EntityRenderer<TntEntity> {
    private final BlockRenderManager blockRenderManager;
    
    public TntEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.blockRenderManager = ctx.getBlockRenderManager();
        this.shadowRadius = 0.5F;  // Shadow radius
    }
    
    @Override
    public Identifier getTexture(TntEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
    
    @Override
    public void render(TntEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, 
                       int light) {
        // Rendering logic
    }
}
```

### 7.2 Rendering Flow

```java
public void render(TntEntity entity, float yaw, float tickDelta,
                   MatrixStack matrices, VertexConsumerProvider vertexConsumers, 
                   int light) {
    // 1. Get block state
    BlockState blockState = entity.getBlockState();
    
    // 2. Set transformation matrix
    matrices.push();
    matrices.translate(-0.5, 0.0, -0.5);  // Center offset
    
    // 3. Calculate flash effect
    int fuse = entity.getFuse();
    boolean shouldFlash = fuse / 5 % 2 == 0 && fuse <= 30;
    
    // 4. Render block
    if (shouldFlash) {
        // Render white overlay when flashing
        renderFlashingBlock(blockRenderManager, blockState, matrices, 
                           vertexConsumers, light, true);
    } else {
        // Normal rendering
        blockRenderManager.renderBlockAsEntity(blockState, matrices, 
                                               vertexConsumers, light, OverlayTexture.DEFAULT_UV);
    }
    
    matrices.pop();
    super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
}
```

### 7.3 Flash Effect Implementation

```java
// TntMinecartEntityRenderer.renderFlashingBlock()
public static void renderFlashingBlock(BlockRenderManager blockRenderManager,
                                       BlockState state, MatrixStack matrices,
                                       VertexConsumerProvider vertexConsumers,
                                       int light, boolean drawFlash) {
    // 1. Render normal block
    blockRenderManager.renderBlockAsEntity(state, matrices, vertexConsumers, 
                                           light, OverlayTexture.DEFAULT_UV);
    
    // 2. Render flash overlay
    if (drawFlash) {
        VertexConsumer flashConsumer = vertexConsumers.getBuffer(
            RenderLayer.getEntityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
        );
        
        // Use white semi-transparent overlay
        int overlay = OverlayTexture.packUv(0.0F, 10.0F);  // White overlay
        
        blockRenderManager.renderBlockAsEntity(state, matrices, 
                                               vertexConsumers, 0xF000F0, overlay);
    }
}
```

### 7.4 Lighting Calculation

```java
// TNT entity lighting calculation
public int getBlockLight(TntEntity entity, BlockPos pos) {
    // TNT entity doesn't emit light, uses ambient lighting
    return entity.getWorld().getLightLevel(LightType.BLOCK, pos);
}

// Actual rendering lighting
int light = LightmapTextureManager.pack(
    getBlockLight(entity, pos),    // Block light (0-15)
    getSkyLight(entity, pos)       // Sky light (0-15)
);
```

### 7.5 Particle Effects

```java
// Particle effects produced by TNT
public void tick() {
    // ... other logic
    
    // Produce smoke particles
    if (this.isTouchingWater()) {
        // No particles in water
    } else {
        // Produce smoke in air
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

### 7.6 Explosion Particles

```java
// Particle effects during explosion
// ParticleTypes constants
ParticleTypes.EXPLOSION           // Normal explosion particle
ParticleTypes.EXPLOSION_EMITTER   // Large explosion particle

// Explosion sound effects
SoundEvents.ENTITY_GENERIC_EXPLODE  // Generic explosion sound
SoundEvents.ENTITY_TNT_PRIMED       // TNT primed sound
```

---

## 8. TNT Block Mechanism

### 8.1 TntBlock Class Structure

```java
public class TntBlock extends Block {
    public static final BooleanProperty UNSTABLE = Properties.UNSTABLE;
    
    // Prime TNT
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

### 8.2 Ignition Trigger Conditions

```java
// 1. Redstone signal trigger
public void neighborUpdate(BlockState state, World world, BlockPos pos, 
                          Block sourceBlock, BlockPos sourcePos, boolean notify) {
    if (world.isReceivingRedstonePower(pos)) {
        primeTnt(world, pos);
        world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }
}

// 2. Fire/Lava contact
public void onBlockAdded(BlockState state, World world, BlockPos pos, 
                        BlockState oldState, boolean notify) {
    if (!oldState.isOf(state.getBlock())) {
        if (world.getBlockState(pos.up()).isOf(Blocks.FIRE) ||
            world.getBlockState(pos.up()).isOf(Blocks.LAVA)) {
            primeTnt(world, pos);
        }
    }
}

// 3. Player using flint and steel
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

// 4. Explosion chain reaction
public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
    primeTnt(world, pos);
}
```

### 8.3 UNSTABLE Property

```java
// UNSTABLE property controls whether TNT ignites when broken
public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
    if (!world.isClient && !player.isCreative() && state.get(UNSTABLE)) {
        primeTnt(world, pos);
    }
    return super.onBreak(world, pos, state, player);
}

// TNT generated naturally in structures has UNSTABLE=true
// Player-placed TNT defaults to UNSTABLE=false
```

---

## 9. Chain Explosion Mechanism

### 9.1 Explosion Triggering TNT

```java
// Called in Explosion.affectWorld()
for (BlockPos pos : this.affectedBlocks) {
    BlockState state = this.world.getBlockState(pos);
    if (!state.isAir()) {
        // ... block destruction logic
        
        // Trigger block's explosion callback
        state.getBlock().onDestroyedByExplosion(this.world, pos, this);
    }
}

// TntBlock.onDestroyedByExplosion()
public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
    if (!world.isClient) {
        TntEntity tnt = new TntEntity(world, 
            pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 
            explosion.getCausingEntity());
        // Random fuse time (shorter fuse)
        tnt.setFuse((byte)(world.random.nextInt(10) + 10));
        world.spawnEntity(tnt);
    }
}
```

### 9.2 Chain Explosion Delay

```
Normal TNT fuse: 80 ticks (4 seconds)
Explosion-triggered TNT fuse: 10-20 ticks (0.5-1 second)

This ensures the visual effect of chain explosions
```

---

## 10. Network Synchronization Mechanism

### 10.1 Server-Side EntityTrackerEntry

```java
public class EntityTrackerEntry {
    // Called every tick
    public void tick() {
        // 1. Update entity position tracking
        // 2. Synchronize entity data
        // 3. Send update packets to clients
    }
    
    // Synchronize entity data
    public void syncEntityData();
    
    // Send sync packet
    public void sendSyncPacket(Packet<?> packet);
}
```

### 10.2 Client-Side ClientWorld

```java
public class ClientWorld {
    // Client tick entity
    public void tickEntity(Entity entity) {
        // 1. Update entity position
        // 2. Execute entity tick logic
        // 3. Rendering update
    }
}
```

### 10.3 Key Network Packets

| Packet Name | Purpose |
|-------------|---------|
| `EntitySpawnS2CPacket` | Entity spawn notification |
| `EntityS2CPacket.MoveRelative` | Entity relative movement |
| `EntityS2CPacket.RotateAndMoveRelative` | Entity rotation and movement |
| `EntityS2CPacket.Rotate` | Entity rotation |
| `ExplosionS2CPacket` | Explosion effect notification |

---

## 11. TNT Behavior in Lazy-Loaded Chunks

### 11.1 Problem Analysis

In lazy-loaded chunks, TNT behavior depends on:

1. **Server-side tick**: 
   - Entity tick still executes in lazy-loaded chunks
   - Fuse continues countdown
   - Physics calculations continue

2. **Client-side synchronization**:
   - `EntityTrackerEntry.tick()` may not execute
   - Client doesn't receive position update packets
   - Animation "freezes"

### 11.2 Code Path Analysis

```
Server-side:
ServerWorld.tick()
└── tickEntity(entity)
    └── EntityTrackerEntry.tick()
        └── Check chunk loading status
            └── If lazy-loaded: may skip sync packet sending

Client-side:
ClientWorld.tickEntity(entity)
└── TntEntity.tick()
    └── Fuse decrement (server-controlled)
    └── Physics update (local calculation)
```

### 11.3 Carpet-VisualFix Solution

By injecting into `EntityTrackerEntry.tick()` on the server:

```java
@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
private void onTick(CallbackInfo ci) {
    if (isInLazyChunk()) {
        ci.cancel();  // Cancel tick, stop sending update packets
    }
}
```

Client-side by canceling `ClientWorld.tickEntity()`:

```java
@Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
private void onTickEntity(Entity entity, CallbackInfo ci) {
    if (isInLazyChunk(entity)) {
        ci.cancel();  // Cancel client tick
    }
}
```

---

## 12. TNT Minecart Comparison

### 12.1 TntMinecartEntity Structure

```java
public class TntMinecartEntity extends AbstractMinecartEntity {
    private int fuseTicks;  // Fuse tick count
    
    public void prime();           // Prime
    public int getFuseTicks();     // Get fuse time
    public boolean isPrimed();     // Whether primed
    public void explode(double power);  // Explode
}
```

### 12.2 Main Differences

| Feature | TntEntity | TntMinecartEntity |
|---------|-----------|-------------------|
| Movement method | Gravity + collision | Rail system |
| Explosion trigger | Fuse countdown | Collision/manual trigger |
| Explosion power | Fixed 4.0 | Variable |
| Ignition method | Redstone/fire/flint and steel | Impact/activation |

---

## 13. Related Entity Types

### 13.1 Other Explosive Entities

| Entity | Class Name | Explosion Power |
|--------|------------|-----------------|
| End Crystal | `EndCrystalEntity` | Related to Ender Dragon |
| Creeper | `CreeperEntity` | 3.0/6.0 (charged) |
| Ghast Fireball | `FireballEntity` | 1.0 |
| Bed/Respawn Anchor | N/A | 5.0 |

### 13.2 Explosion Resistance Mechanism

```java
// Block blast resistance
public float getBlastResistance();

// Entity explosion resistance check
public boolean isImmuneToExplosion();

// Whether can be destroyed by explosion
public boolean canExplosionDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power);
```

---

## 14. NBT Data Structure

### 14.1 TNT Entity NBT

```json
{
    "Fuse": 80,           // Fuse time (ticks)
    "BlockState": {       // Block state
        "Name": "minecraft:tnt"
    }
}
```

### 14.2 Key NBT Operations

```java
// Write to NBT
protected void writeCustomDataToNbt(NbtCompound nbt) {
    nbt.putShort("Fuse", (short)this.getFuse());
    nbt.put("BlockState", NbtHelper.fromBlockState(this.getBlockState()));
}

// Read from NBT
protected void readCustomDataFromNbt(NbtCompound nbt) {
    this.setFuse(nbt.getShort("Fuse"));
    this.setBlockState(NbtHelper.toBlockState(nbt.getCompound("BlockState")));
}
```

---

## 15. Summary

### 15.1 TNT Entity Characteristics

1. **Simple Structure**: Directly inherits Entity, no complex AI
2. **Server-Dominated**: Fuse and explosion controlled by server
3. **Network Synchronization**: Via TrackedData and EntityTrackerEntry
4. **Physics Simulation**: Affected by gravity, has collision detection

### 15.2 Lazy-Loading Behavior

1. Server continues ticking, fuse countdowns
2. Client may not receive updates, causing animation "freeze"
3. Explosion still occurs normally

### 15.3 Optimization Suggestions

For Carpet-VisualFix mod:
- Server-side canceling EntityTrackerEntry.tick() stops synchronization
- Client-side canceling tickEntity() stops local animation
- Both combined achieve complete "freeze" effect

---

## 16. Special Behaviors

### 16.1 End Portal Handling

```java
// End portal handling in TntEntity
private static final ExplosionBehavior TELEPORTED_EXPLOSION_BEHAVIOR = new ExplosionBehavior() {
    @Override
    public boolean canDestroyBlock(Explosion explosion, BlockView world, 
                                   BlockPos pos, BlockState state, float power) {
        // Teleported TNT doesn't destroy portal blocks
        return !state.isOf(Blocks.END_PORTAL) && 
               !state.isOf(Blocks.END_GATEWAY) &&
               super.canDestroyBlock(explosion, world, pos, state, power);
    }
};

// Teleport detection
public void setTeleported(boolean teleported) {
    this.teleported = teleported;
    if (teleported) {
        // Use special explosion behavior
        this.world.createExplosion(this, this.getX(), this.getY(), this.getZ(), 
                                   4.0f, TELEPORTED_EXPLOSION_BEHAVIOR);
    }
}
```

### 16.2 Game Rule Effects

```java
// Game rules related to TNT
GameRules.TNT_EXPLOSION_DROP_DECAY  // TNT explosion drop decay
GameRules.MOB_EXPLOSION_DROP_DECAY  // Mob explosion drop decay
GameRules.BLOCK_EXPLOSION_DROP_DECAY // Block explosion drop decay

// Drop decay logic
if (world.getGameRules().getBoolean(GameRules.TNT_EXPLOSION_DROP_DECAY)) {
    // Drops have probability to disappear
    float dropChance = 1.0f / (float)(affectedBlocks.size());
    // ... apply decay
}
```

### 16.3 Underwater Explosion Behavior

```java
// Special handling for underwater explosions
public void explode() {
    // Check if in water
    boolean inWater = this.isTouchingWater();
    
    // Underwater explosion doesn't create fire
    this.getWorld().createExplosion(
        this, 
        this.getX(), this.getY(), this.getZ(), 
        4.0f,
        false,  // No fire
        World.ExplosionSourceType.TNT
    );
    
    // Underwater explosion power unaffected, but visual effects differ
}
```

### 16.4 Knockback Resistance Calculation

```java
// Entity knockback resistance
public static final RegistryEntry<Enchantment> GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE;

// Knockback calculation
public void takeKnockback(double strength, double x, double z) {
    // Consider knockback resistance
    double resistance = this.getAttributeValue(EntityAttributes.GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE);
    strength *= (1.0 - resistance);
    
    // Apply knockback
    this.setVelocity(this.getVelocity().add(
        x * strength,
        0.1,  // Vertical knockback
        z * strength
    ));
}
```

---

## 17. Performance Optimization

### 17.1 Explosion Caching

```java
// Caching mechanism in Explosion class
private final ObjectArrayList<BlockPos> affectedBlocks;  // Fast list
private final Map<PlayerEntity, Vec3d> affectedPlayers;  // Player cache

// Avoid repeated calculations
private boolean blocksCollected = false;
private boolean entitiesDamaged = false;
```

### 17.2 Ray Tracing Optimization

```java
// Use incremental calculation to avoid repeated square root operations
// Pre-calculate direction vectors
double dx = (x / 15.0 - 0.5) * 2.0;
double dy = (y / 15.0 - 0.5) * 2.0;
double dz = (z / 15.0 - 0.5) * 2.0;

// Normalize once
double length = Math.sqrt(dx*dx + dy*dy + dz*dz);
dx /= length; dy /= length; dz /= length;

// Incremental movement
pos = pos.add(dx, dy, dz);
```

### 17.3 Block State Caching

```java
// BlockState cache
private final BlockState cachedBlockState;

// Avoid getting on every render
public BlockState getBlockState() {
    return this.cachedBlockState;
}
```

---

## 18. Debug Information

### 18.1 Key Logging Points

```java
// TNT primed
LOGGER.debug("TNT primed at {} by {}", pos, igniter);

// Explosion occurred
LOGGER.debug("TNT exploded at {} with power {}", pos, power);

// Block destruction count
LOGGER.debug("Explosion destroyed {} blocks", affectedBlocks.size());
```

### 18.2 Performance Monitoring

```java
// Explosion performance timing
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

## Appendix A: Obfuscation Mapping Reference

| Original Name | Obfuscated Name | Yarn Mapped Name |
|---------------|-----------------|------------------|
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

## Appendix B: Key Value Reference

### B.1 TNT Parameters

| Parameter | Value | Description |
|-----------|-------|-------------|
| Default fuse | 80 ticks | 4 seconds |
| Explosion power | 4.0 | Standard TNT power |
| Explosion radius | 8 blocks | Power × 2 |
| Gravity acceleration | 0.04 | blocks/tick² |
| Air resistance | 0.98 | Decay per tick |
| Water resistance | 0.85 | Decay per tick |
| Shadow radius | 0.5 | Rendering parameter |

### B.2 Explosion Damage Calculation

```
Maximum damage = Power × 7 + 1
TNT maximum damage = 4 × 7 + 1 = 29 (no armor)

Damage decay formula:
damage = (impact² + impact) / 2 × 7 × power + 1
Where impact = (1 - distance) × exposure
```

### B.3 Block Blast Resistance

| Block | Blast Resistance |
|-------|------------------|
| Bedrock | 3600000.0 |
| End Stone | 9.0 |
| Stone | 6.0 |
| Dirt | 0.5 |
| TNT | 0.0 |

---

## Appendix C: Related Sounds and Particles

### C.1 Sounds

| Sound Event | Purpose |
|-------------|---------|
| `ENTITY_TNT_PRIMED` | TNT primed |
| `ENTITY_GENERIC_EXPLODE` | Explosion |

### C.2 Particle Effects

| Particle Type | Purpose |
|---------------|---------|
| `SMOKE` | When TNT is burning |
| `EXPLOSION` | Small explosion |
| `EXPLOSION_EMITTER` | Large explosion |

---

*Report Generated: 2026-03-19*
*Minecraft Version: 1.21.1*
*Mapping Version: Yarn 1.21.1+build.3*
