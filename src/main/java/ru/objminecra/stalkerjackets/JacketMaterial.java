package ru.objminecra.stalkerjackets;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Характеристики курток. Так как весь сет сидит на ОДНОМ предмете (нагрудник),
 * значения брони/прочности заданы сразу за полный комплект.
 */
public enum JacketMaterial implements ArmorMaterial {
    //                                                           прочность(x) броня прочность зачарование ремонт
    STALKER   ("jacket_stalker",   20, 7,  0.0F, 0.00F, 12, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),   Rarity.COMMON),
    VETERAN   ("jacket_veteran",   25, 12, 1.0F, 0.00F, 12, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),   Rarity.UNCOMMON),
    BANDITS   ("jacket_bandits",   22, 10, 1.0F, 0.00F, 12, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),   Rarity.UNCOMMON),
    RENEGADE  ("jacket_renegade",  23, 11, 1.0F, 0.00F, 12, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),   Rarity.UNCOMMON),
    CS        ("jacket_cs",        26, 14, 1.0F, 0.00F, 12, SoundEvents.ARMOR_EQUIP_IRON,    () -> Ingredient.of(Items.IRON_INGOT), Rarity.RARE),
    FREEDOM   ("jacket_freedom",   30, 16, 2.0F, 0.05F, 14, SoundEvents.ARMOR_EQUIP_IRON,    () -> Ingredient.of(Items.IRON_INGOT), Rarity.RARE),
    DOLG      ("jacket_dolg",      35, 20, 3.0F, 0.10F, 15, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(Items.DIAMOND),    Rarity.EPIC);

    private static final Map<ArmorItem.Type, Integer> HEALTH_FOR_TYPE = new EnumMap<>(ArmorItem.Type.class);

    static {
        HEALTH_FOR_TYPE.put(ArmorItem.Type.BOOTS, 13);
        HEALTH_FOR_TYPE.put(ArmorItem.Type.LEGGINGS, 15);
        HEALTH_FOR_TYPE.put(ArmorItem.Type.CHESTPLATE, 16);
        HEALTH_FOR_TYPE.put(ArmorItem.Type.HELMET, 11);
    }

    private final String name;
    private final int durabilityMultiplier;
    private final int defense;
    private final float toughness;
    private final float knockbackResistance;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final Supplier<Ingredient> repairIngredient;
    private final Rarity rarity;

    JacketMaterial(String name, int durabilityMultiplier, int defense, float toughness, float knockbackResistance,
                   int enchantmentValue, SoundEvent equipSound, Supplier<Ingredient> repairIngredient, Rarity rarity) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.repairIngredient = repairIngredient;
        this.rarity = rarity;
    }

    /** Имя регистрации предмета; оно же — имя файлов текстур (item + entity). */
    public String getRegName() {
        return name;
    }

    public Rarity getRarity() {
        return rarity;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return HEALTH_FOR_TYPE.get(type) * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return defense;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }

    @Override
    public String getName() {
        return StalkerJackets.MOD_ID + ":" + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }
}
