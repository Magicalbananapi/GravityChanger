package gravity_changer.mixin.debug;

import gravity_changer.api.GravityChangerAPI;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

//NOT INCLUDED BY DEFAULT, IT'S ANNOYING, TODO: FIND A BETTER WAY
@Mixin(Entity.class)
public class EntityMixin_Debug {
    @Shadow private Vec3d pos;

    @Inject(method = "setPos", at = @At("HEAD"))
    private void debugOnSetPos(double x, double y, double z, CallbackInfo ci) {
        Entity this_ = (Entity) (Object) this;
        if (this_ instanceof ItemEntity) {
            String str = "%s ItemEntity#setPosRaw(%s, %s, %s) grav %s %s".formatted(
                this_.getWorld().isClient() ? "client" : "server", x, y, z,
                GravityChangerAPI.getGravityDirection(this_),
                GravityChangerAPI.getGravityStrength(this_)
            );
            System.out.println(str);
        }
    }
}
