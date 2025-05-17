package gravity_changer;

import gravity_changer.plating.GravityPlatingBlockEntity;
import net.minecraft.component.DataComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.UnaryOperator;

public class ModComponents {
    protected static void initialize() {
        //To notify console that the components successfully registered.
        GravityChangerMod.LOGGER.info("Registering {} components", GravityChangerMod.NAMESPACE);
    }

    public static <T> DataComponentType<T> register(String path, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of("tutorial", path), builderOperator.apply(DataComponentType.builder()).build());
    }

    public static final DataComponentType<?> SIDEDATA = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(GravityChangerMod.NAMESPACE, "side_data"),
            DataComponentType.<GravityPlatingBlockEntity.SideData>builder().codec(null).build()
    );
}
