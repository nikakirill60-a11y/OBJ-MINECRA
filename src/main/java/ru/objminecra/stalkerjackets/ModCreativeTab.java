package ru.objminecra.stalkerjackets;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StalkerJackets.MOD_ID);

    public static final RegistryObject<CreativeModeTab> JACKETS = TABS.register("jackets",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.JACKET_STALKER.get()))
                    .title(Component.translatable("itemGroup.stalkerjackets"))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.JACKET_STALKER.get());
                        output.accept(ModItems.JACKET_VETERAN.get());
                        output.accept(ModItems.JACKET_BANDITS.get());
                        output.accept(ModItems.JACKET_RENEGADE.get());
                        output.accept(ModItems.JACKET_CS.get());
                        output.accept(ModItems.JACKET_FREEDOM.get());
                        output.accept(ModItems.JACKET_DOLG.get());
                    })
                    .build());
}
