package gravity_changer.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import gravity_changer.RotationAnimation;
import gravity_changer.api.GravityChangerAPI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//Verified Working 1.20.6
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Camera camera;

    //TODO: Figure out a better way to do this than shift.
    @Inject(
        method = "Lnet/minecraft/client/render/GameRenderer;renderWorld",
        at = @At(
            value = "INVOKE",
            target = "Lorg/joml/Matrix4f;rotationXYZ(FFF)Lorg/joml/Matrix4f;",
            shift = At.Shift.BY, by = 2
        )
    )
    private void inject_renderWorld(float tickDelta, long limitTime, CallbackInfo ci, @Local(ordinal = 1) Matrix4f matrix4f2) {

        if (this.camera.getFocusedEntity() != null) {
            Entity focusedEntity = this.camera.getFocusedEntity();
            Direction gravityDirection = GravityChangerAPI.getGravityDirection(focusedEntity);

            RotationAnimation animation = GravityChangerAPI.getRotationAnimation(focusedEntity);
            if (animation == null) {
                return;
            }
            long timeMs = focusedEntity.getWorld().getTime() * 50 + (long) (tickDelta * 50);
            Quaternionf currentGravityRotation = animation.getCurrentGravityRotation(gravityDirection, timeMs);

            if (animation.isInAnimation()) {
                // make sure that frustum culling updates when running rotation animation
                MinecraftClient.getInstance().worldRenderer.scheduleTerrainUpdate();
            }
            matrix4f2.rotate(currentGravityRotation);
        }
    }
}
