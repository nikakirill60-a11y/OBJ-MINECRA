package ru.objminecra.stalkerjackets;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * STALKER Jackets — OBJ броня для Minecraft 1.20.1 (Forge).
 * Один предмет в слоте нагрудника = визуально и по защите весь комплект.
 */
@Mod(StalkerJackets.MOD_ID)
public class StalkerJackets {
    public static final String MOD_ID = "stalkerjackets";

    public StalkerJackets() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModCreativeTab.TABS.register(bus);
    }
}
