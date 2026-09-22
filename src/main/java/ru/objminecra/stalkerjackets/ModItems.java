package ru.objminecra.stalkerjackets;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, StalkerJackets.MOD_ID);

    public static final RegistryObject<Item> JACKET_STALKER = register(JacketMaterial.STALKER, false);
    public static final RegistryObject<Item> JACKET_VETERAN = register(JacketMaterial.VETERAN, false);
    public static final RegistryObject<Item> JACKET_BANDITS = register(JacketMaterial.BANDITS, false);
    public static final RegistryObject<Item> JACKET_RENEGADE = register(JacketMaterial.RENEGADE, false);
    public static final RegistryObject<Item> JACKET_CS = register(JacketMaterial.CS, false);
    public static final RegistryObject<Item> JACKET_FREEDOM = register(JacketMaterial.FREEDOM, false);
    public static final RegistryObject<Item> JACKET_DOLG = register(JacketMaterial.DOLG, true);

    private static RegistryObject<Item> register(JacketMaterial material, boolean fireResistant) {
        Item.Properties props = new Item.Properties().stacksTo(1).rarity(material.getRarity());
        if (fireResistant) {
            props.fireResistant();
        }
        return ITEMS.register(material.getRegName(), () -> new JacketItem(material, props));
    }
}
