package gravity_changer.mixin.client;

import gravity_changer.api.GravityChangerAPI;
import gravity_changer.util.RotationUtil;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Redirect(
        method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private Vec3d modify_setupTransforms_Vec3d_0(AbstractClientPlayerEntity instance, float partialTick) {
        Vec3d viewVector = instance.getRotationVec(partialTick);
        
        Direction gravityDirection = GravityChangerAPI.getGravityDirection(instance);
        if (gravityDirection == Direction.DOWN) {
            return viewVector;
        }
        
        return RotationUtil.vecWorldToPlayer(viewVector, gravityDirection);
    }
}
