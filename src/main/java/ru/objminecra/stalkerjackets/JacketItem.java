package ru.objminecra.stalkerjackets;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import ru.objminecra.stalkerjackets.client.JacketArmorModel;

import java.util.List;
import java.util.function.Consumer;

/**
 * Куртка: надевается в слот нагрудника, но рендерит ВЕСЬ костюм (голова/тело/руки/ноги)
 * по OBJ-модели и даёт защиту полного комплекта.
 */
public class JacketItem extends ArmorItem {
    private final String armorTexture;
    private final JacketMaterial jacketMaterial;

    public JacketItem(JacketMaterial material, Properties properties) {
        super(material, Type.CHESTPLATE, properties);
        this.jacketMaterial = material;
        this.armorTexture = StalkerJackets.MOD_ID + ":textures/entity/" + material.getRegName() + ".png";
    }

    public JacketMaterial getJacketMaterial() {
        return jacketMaterial;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return armorTexture;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.stalkerjackets.fullset").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.stalkerjackets.slot").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private JacketArmorModel model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack,
                                                          EquipmentSlot slot, HumanoidModel<?> fallback) {
                if (model == null) {
                    model = new JacketArmorModel(
                            Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR));
                }
                // Копируем позу (повороты/пивоты) с модели сущности, затем принудительно
                // показываем ВСЕ части тела: один слот нагрудника рисует весь костюм.
                model.head.copyFrom(fallback.head);
                model.hat.copyFrom(fallback.hat);
                model.body.copyFrom(fallback.body);
                model.rightArm.copyFrom(fallback.rightArm);
                model.leftArm.copyFrom(fallback.leftArm);
                model.rightLeg.copyFrom(fallback.rightLeg);
                model.leftLeg.copyFrom(fallback.leftLeg);
                model.crouching = fallback.crouching;
                model.riding = fallback.riding;
                model.young = fallback.young;
                model.attackTime = fallback.attackTime;
                model.setAllVisible(true);
                return model;
            }
        });
    }
}
