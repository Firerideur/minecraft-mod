package com.firerideur.arcanequest.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Per weapon animations: a custom first person swing for each weapon type,
 * and a matching way of holding it in third person.
 */
public class WeaponClientExtensions implements IClientItemExtensions {
    private static HumanoidModel.ArmPose twoHandedPose;
    private static HumanoidModel.ArmPose spearPose;
    private static HumanoidModel.ArmPose readyPose;

    private final WeaponItem weapon;

    public WeaponClientExtensions(WeaponItem weapon) {
        this.weapon = weapon;
    }

    // ------------------------------------------------------------------
    // Third person
    // ------------------------------------------------------------------

    @Override
    public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
        return switch (weapon.getType()) {
            case LONGSWORD, SCYTHE -> twoHanded();
            case SPEAR -> spear();
            case KATANA, DAGGER -> ready();
        };
    }

    /** Both hands on the hilt, blade raised in front. */
    private static HumanoidModel.ArmPose twoHanded() {
        if (twoHandedPose == null) {
            twoHandedPose = HumanoidModel.ArmPose.create("ARCANEQUEST_TWO_HANDED", true, (model, entity, arm) -> {
                ModelPart main = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                ModelPart off = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
                float side = arm == HumanoidArm.RIGHT ? 1 : -1;
                main.xRot = -0.85F + model.head.xRot * 0.3F;
                main.yRot = -0.35F * side;
                off.xRot = -0.95F + model.head.xRot * 0.3F;
                off.yRot = 0.55F * side;
            });
        }
        return twoHandedPose;
    }

    /** Spear held low and level, pointing forward, off hand guiding the haft. */
    private static HumanoidModel.ArmPose spear() {
        if (spearPose == null) {
            spearPose = HumanoidModel.ArmPose.create("ARCANEQUEST_SPEAR", true, (model, entity, arm) -> {
                ModelPart main = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                ModelPart off = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
                float side = arm == HumanoidArm.RIGHT ? 1 : -1;
                main.xRot = -0.6F;
                main.yRot = -0.1F * side;
                off.xRot = -1.2F;
                off.yRot = 0.7F * side;
            });
        }
        return spearPose;
    }

    /** One handed fighting stance: blade forward. */
    private static HumanoidModel.ArmPose ready() {
        if (readyPose == null) {
            readyPose = HumanoidModel.ArmPose.create("ARCANEQUEST_READY", false, (model, entity, arm) -> {
                ModelPart main = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
                main.xRot = main.xRot * 0.5F - 0.55F;
                main.yRot = 0.0F;
            });
        }
        return readyPose;
    }

    // ------------------------------------------------------------------
    // First person
    // ------------------------------------------------------------------

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swing) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        // Same resting position as vanilla
        pose.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        float bell = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        float back = swing < 0.8F ? 1.0F : (1.0F - swing) / 0.2F; // return to rest at the end
        switch (weapon.getType()) {
            case DAGGER -> {
                // Quick stab straight ahead
                pose.translate(-side * 0.05F * bell, 0.08F * bell, -0.45F * bell);
                pose.mulPose(Axis.XP.rotationDegrees(-15.0F * bell));
            }
            case KATANA -> {
                // Fast horizontal slash from right to left
                float angle = Mth.lerp(smooth(swing), 70.0F, -100.0F) * back * (swing > 0 ? 1 : 0);
                pose.translate(-side * 0.15F * bell, 0.15F * bell, -0.1F * bell);
                pose.mulPose(Axis.YP.rotationDegrees(side * angle));
                pose.mulPose(Axis.ZP.rotationDegrees(side * -70.0F * bell));
            }
            case LONGSWORD -> {
                // Raise above the head, then a heavy chop downward
                float angle = swing < 0.35F ? 55.0F * (swing / 0.35F) : 55.0F - 165.0F * smooth((swing - 0.35F) / 0.65F);
                pose.translate(-side * 0.2F, 0.0F, 0.0F);
                pose.mulPose(Axis.XP.rotationDegrees(angle * back));
                pose.mulPose(Axis.YP.rotationDegrees(side * -20.0F));
                pose.translate(side * 0.2F, 0.0F, 0.0F);
            }
            case SPEAR -> {
                // Long forward thrust, spear kept level
                pose.mulPose(Axis.XP.rotationDegrees(-25.0F));
                pose.translate(0.0F, 0.05F * bell, -0.8F * bell);
            }
            case SCYTHE -> {
                // Wide reaping sweep across the whole screen
                float angle = Mth.lerp(smooth(swing), 100.0F, -160.0F) * back * (swing > 0 ? 1 : 0);
                pose.translate(-side * 0.1F * bell, 0.2F * bell, 0.0F);
                pose.mulPose(Axis.YP.rotationDegrees(side * angle));
                pose.mulPose(Axis.ZP.rotationDegrees(side * -45.0F * bell));
            }
        }
        return true;
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3 - 2 * t);
    }
}
