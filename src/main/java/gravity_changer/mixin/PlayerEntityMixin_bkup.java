package gravity_changer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

//TODO: Double check the return ordinal stuff, idk why it did that
@Debug(export = true)
@Mixin(value = PlayerEntity.class, priority = 1001)
public abstract class PlayerEntityMixin_bkup extends LivingEntity {
    @Shadow @Final private PlayerAbilities abilities;

    @Shadow protected abstract boolean clipAtLedge();
    @Shadow protected abstract boolean method_30263(float stepHeight); //isAboveGround

    @Shadow protected abstract boolean method_59818(double d, double e, float f);

    protected PlayerEntityMixin_bkup(EntityType<? extends LivingEntity> entityType, World world) { super(entityType, world); }

    @WrapOperation(
        method = "travel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private Vec3d wrapOperation_travel_getRotationVector_0(PlayerEntity playerEntity, Operation<Vec3d> original) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(playerEntity);
        if (gravityDirection == Direction.DOWN) {
            return original.call(playerEntity);
        }
        
        return RotationUtil.vecWorldToPlayer(original.call(playerEntity), gravityDirection);
    }
    
    
    @ModifyArgs(
        method = "travel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/BlockPos;ofFloored(DDD)Lnet/minecraft/util/math/BlockPos;"
        )
    )
    private void modify_move_multiply_0(Args args) {
        Vec3d rotate = new Vec3d(0.0D, 1.0D - 0.1D, 0.0D);
        rotate = RotationUtil.vecPlayerToWorld(rotate, GravityChangerAPI.getGravityDirection(this));
        args.set(0, (double) args.get(0) - rotate.x);
        args.set(1, (double) args.get(1) - rotate.y + (1.0D - 0.1D));
        args.set(2, (double) args.get(2) - rotate.z);
    }
    
    @Redirect(
        method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
        at = @At(
            value = "NEW",
            target = "(Lnet/minecraft/world/World;DDDLnet/minecraft/item/ItemStack;)Lnet/minecraft/entity/ItemEntity;",
            ordinal = 0
        )
    )
    private ItemEntity redirect_dropItem_new_0(
        World world, double x, double y, double z, ItemStack stack
    ) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return new ItemEntity(world, x, y, z, stack);
        }
        
        Vec3d vec3d = this.getEyePos()
            .subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.3D, 0.0D, gravityDirection));

        ItemEntity itemEntity = new ItemEntity(world, vec3d.x, vec3d.y, vec3d.z, stack);

//        // change the gravity of the thrown item
//        GravityChangerAPI.setBaseGravityDirection(
//            itemEntity, gravityDirection
//        );
        // the item entity calculates getPos both on client and server separately
        // if gravity is not down, the client and server will desync (the reason is not yet known)
        // don't let item change gravity for now
        
        return itemEntity;
    }
    
    @WrapOperation(
        method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/ItemEntity;setVelocity(DDD)V"
        )
    )
    private void wrapOperation_dropItem_setVelocity(ItemEntity itemEntity, double x, double y, double z, Operation<Void> original) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            original.call(itemEntity, x, y, z);
            return;
        }
        
        Vec3d world = RotationUtil.vecPlayerToWorld(x, y, z, gravityDirection);
        GravityChangerAPI.setWorldVelocity(itemEntity, world);
    }

    //TODO: Make sure this works, the ordinal on the return injections was the
    // opposite of what I expected, so make sure it still works as expected

    /*@ModifyVariable(
            method = "adjustMovementForSneaking",
            at = @At(value = "HEAD"),
            argsOnly = true
    )
    private Vec3d injected(Vec3d movement) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        return RotationUtil.vecWorldToPlayer(movement, gravityDirection);
    }

    @Inject(
            method = "adjustMovementForSneaking",
            at = @At(value = "RETURN", ordinal = 1),
            cancellable = true
    )
    private void modify_return_if(CallbackInfoReturnable<Vec3d> cir,
                                 @Local(argsOnly = true) Vec3d movement,
                                 @Local(ordinal = 0) double d,
                                 @Local(ordinal = 1) double e) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(d, movement.y, e, gravityDirection));
    }

    @Inject(
            method = "adjustMovementForSneaking",
            at = @At(value = "RETURN", ordinal = 0),
            cancellable = true
    )
    private void modify_return_else(CallbackInfoReturnable<Vec3d> cir, @Local(argsOnly = true) Vec3d movement) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(movement, gravityDirection));
    }*/



    @Inject(
            method = "adjustMovementForSneaking",
            at = @At("HEAD"),
            cancellable = true
    )
    private void inject_adjustMovementForSneaking(Vec3d movement, MovementType type, CallbackInfoReturnable<Vec3d> cir) {
        Entity this_ = (Entity) (Object) this;
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(this_);
        if (gravityDirection == Direction.DOWN) return;

        Vec3d playerMovement = RotationUtil.vecWorldToPlayer(movement, gravityDirection);

        float f = this.getStepHeight();
        if (!this.abilities.flying
                //&& !(movement.y > 0.0)
                && (type == MovementType.SELF || type == MovementType.PLAYER)
                && this.clipAtLedge()
                && this.isAboveGround(f)) {
            double d = playerMovement.x;
            double e = playerMovement.z;
            double h = Math.signum(d) * 0.05;
            double i = Math.signum(e) * 0.05;

            while (d != 0.0 && this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(RotationUtil.vecPlayerToWorld(d, -f, 0.0, gravityDirection)))) {//this.method_59818(d, 0.0, f)) {
                if (d < 0.05D && d >= -0.05D) {
                    d = 0.0D;
                } else if (d > 0.0D) {
                    d -= 0.05D;
                } else {
                    d += 0.05D;
                }
            }

            while (e != 0.0 && this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(RotationUtil.vecPlayerToWorld(0.0, -f, e, gravityDirection)))) {//this.method_59818(0.0, e, f)) {
                if (e < 0.05D && e >= -0.05D) {
                    e = 0.0D;
                } else if (e > 0.0D) {
                    e -= 0.05D;
                } else {
                    e += 0.05D;
                }
            }

            while (d != 0.0 && e != 0.0 && this.getWorld().isSpaceEmpty(this, this.getBoundingBox().offset(RotationUtil.vecPlayerToWorld(d, -f, e, gravityDirection)))) {//&& this.method_59818(d, e, f)) {
                if (d < 0.05D && d >= -0.05D) {
                    d = 0.0D;
                } else if (d > 0.0D) {
                    d -= 0.05D;
                } else {
                    d += 0.05D;
                }

                if (e < 0.05D && e >= -0.05D) {
                    e = 0.0D;
                } else if (e > 0.0D) {
                    e -= 0.05D;
                } else {
                    e += 0.05D;
                }
            }

            cir.setReturnValue(RotationUtil.vecPlayerToWorld(d, playerMovement.y, e, gravityDirection));
        } else {
            cir.setReturnValue(movement);
        }
    }

    private boolean isAboveGround(float f) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        Box box =  this.getBoundingBox();
        double x = 0.0;
        double y = this.fallDistance - f;
        double z = 0.0;

        if (gravityDirection == Direction.DOWN) {
            return this.isOnGround() || this.fallDistance < f && !this.getWorld().isSpaceEmpty(this, box.offset(x, y, z));
        }

        Vec3d world = RotationUtil.vecPlayerToWorld(x, y, z, gravityDirection);
        return this.isOnGround() || this.fallDistance < f && !this.getWorld().isSpaceEmpty(this, box.offset(world.x, world.y, world.z));
    }

    //Uses implementation from 1.21.5, to both simplify and future-proof this
    //TODO: This does NOT work
    @Redirect(
            method = "method_59818", //isSpaceAroundPlayerEmpty
            at = @At(
                    value = "NEW",
                    target = "(DDDDDD)Lnet/minecraft/util/math/Box;"
            )
    )
    private Box redirect_method_59818_new_box(
            double x1, double y1, double z1, double x2, double y2, double z2,
            @Local(ordinal = 0, argsOnly = true) double offsetX,
            @Local(ordinal = 1, argsOnly = true) double offsetZ,
            @Local(ordinal = 0, argsOnly = true) float d,
            @Local(ordinal=0) Box box
    ) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        return RotationUtil.boxPlayerToWorld(
                new Box(box.minX + 1.0E-7 + offsetX, box.minY - d - 1.0E-7, box.minZ + 1.0E-7 + offsetZ,
                        box.maxX - 1.0E-7 + offsetX, box.minY, box.maxZ - 1.0E-7 + offsetZ), gravityDirection);
    }
    
    @WrapOperation(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F",
            ordinal = 0
        )
    )
    private float wrapOperation_attack_getYaw_0(PlayerEntity attacker, Operation<Float> original, Entity target) {
        Direction targetGravityDirection = GravityChangerAPI.getGravityDirection(target);
        Direction attackerGravityDirection = GravityChangerAPI.getGravityDirection(attacker);
        if (targetGravityDirection == attackerGravityDirection) {
            return original.call(attacker);
        }
        
        return RotationUtil.rotWorldToPlayer(RotationUtil.rotPlayerToWorld(original.call(attacker), attacker.getPitch(), attackerGravityDirection), targetGravityDirection).x;
    }
    
    @WrapOperation(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F",
            ordinal = 1
        )
    )
    private float wrapOperation_attack_getYaw_1(PlayerEntity attacker, Operation<Float> original, Entity target) {
        Direction targetGravityDirection = GravityChangerAPI.getGravityDirection(target);
        Direction attackerGravityDirection = GravityChangerAPI.getGravityDirection(attacker);
        if (targetGravityDirection == attackerGravityDirection) {
            return original.call(attacker);
        }
        
        return RotationUtil.rotWorldToPlayer(RotationUtil.rotPlayerToWorld(original.call(attacker), attacker.getPitch(), attackerGravityDirection), targetGravityDirection).x;
    }
    
    @WrapOperation(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F",
            ordinal = 2
        )
    )
    private float wrapOperation_attack_getYaw_2(PlayerEntity attacker, Operation<Float> original) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(attacker);
        if (gravityDirection == Direction.DOWN) {
            return original.call(attacker);
        }
        
        return RotationUtil.rotPlayerToWorld(original.call(attacker), attacker.getPitch(), gravityDirection).x;
    }
    
    @WrapOperation(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F",
            ordinal = 3
        )
    )
    private float wrapOperation_attack_getYaw_3(PlayerEntity attacker, Operation<Float> original) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(attacker);
        if (gravityDirection == Direction.DOWN) {
            return original.call(attacker);
        }
        
        return RotationUtil.rotPlayerToWorld(original.call(attacker), attacker.getPitch(), gravityDirection).x;
    }

    //TODO: Rotate Death Particles
    
    @ModifyArgs(
        method = "tickMovement",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/Box;expand(DDD)Lnet/minecraft/util/math/Box;"
        )
    )
    private void modify_tickMovement_expand_0(Args args) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) return;
        
        Vec3d vec3d = RotationUtil.maskPlayerToWorld(args.get(0), args.get(1), args.get(2), gravityDirection);
        args.set(0, vec3d.x);
        args.set(1, vec3d.y);
        args.set(2, vec3d.z);
    }
    
    @WrapOperation(
        method = "canChangeIntoPose",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/EntityDimensions;getBoxAt(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Box;"
        )
    )
    private Box wrapOperation_canChangeIntoPose_getBoundingBox(EntityDimensions dimensions, Vec3d pos, Operation<Box> original) {
        Direction gravityDirection = GravityChangerAPI.getGravityDirection((Entity) (Object) this);
        if (gravityDirection == Direction.DOWN) {
            return original.call(dimensions, pos);
        }

        return RotationUtil.makeBoxFromDimensions(dimensions, gravityDirection, pos);
    }
}
