package com.firerideur.arcanequest.client.boss;

import com.firerideur.arcanequest.boss.BossCore;
import com.firerideur.arcanequest.boss.SkeletonKing;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import static com.firerideur.arcanequest.client.boss.BossAnim.after;
import static com.firerideur.arcanequest.client.boss.BossAnim.lerp;
import static com.firerideur.arcanequest.client.boss.BossAnim.weight;

/** Crowned skeleton with a cape: walks like a humanoid, plays each special attack's wind-up and strike. */
public class SkeletonKingModel extends HierarchicalModel<SkeletonKing> implements ArmedModel {
    private final ModelPart root;
    private final ModelPart base;
    private final ModelPart waist;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart cape;
    private final ModelPart tabard;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private float partialTick;

    public SkeletonKingModel(ModelPart root) {
        this.root = root;
        this.base = root.getChild("base");
        this.waist = base.getChild("waist");
        this.head = waist.getChild("head");
        this.jaw = head.getChild("jaw");
        this.rightArm = waist.getChild("right_arm");
        this.leftArm = waist.getChild("left_arm");
        this.cape = waist.getChild("cape");
        this.tabard = waist.getChild("tabard");
        this.rightLeg = base.getChild("right_leg");
        this.leftLeg = base.getChild("left_leg");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void prepareMobModel(SkeletonKing king, float limbSwing, float limbSwingAmount, float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(SkeletonKing king, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = headYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;

        float walk = limbSwing * 0.6662F;
        rightLeg.xRot = Mth.cos(walk) * 1.2F * limbSwingAmount;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.2F * limbSwingAmount;
        rightArm.xRot = -0.35F + Mth.cos(walk + Mth.PI) * 0.5F * limbSwingAmount;
        leftArm.xRot = Mth.cos(walk) * 0.6F * limbSwingAmount;
        float sway = Mth.cos(age * 0.09F) * 0.04F + 0.05F;
        rightArm.zRot = sway;
        leftArm.zRot = -sway;
        cape.xRot = 0.08F + limbSwingAmount * 0.5F + Mth.sin(age * 0.07F) * 0.04F;
        tabard.xRot = Math.min(0.0F, Math.min(rightLeg.xRot, leftLeg.xRot)) * 0.7F;
        jaw.xRot = 0.04F + Math.max(0.0F, Mth.sin(age * 0.05F)) * 0.08F;

        if (attackTime > 0.0F) {
            // Plain sword swing: raise, then cut down
            rightArm.xRot = -0.35F - Mth.sin(attackTime * Mth.PI) * 1.8F;
            waist.yRot = Mth.sin(Mth.sqrt(attackTime) * Mth.TWO_PI) * 0.2F;
        }

        BossCore core = king.getCore();
        float t = core.attackTime(partialTick);
        switch (core.getAttack()) {
            case SkeletonKing.WHIRLWIND -> whirlwind(t);
            case SkeletonKing.ERUPTION -> eruption(t);
            case SkeletonKing.VOLLEY -> volley(t);
            case SkeletonKing.SUMMON -> summon(t);
            default -> {
            }
        }
    }

    private void whirlwind(float t) {
        if (t < SkeletonKing.WHIRLWIND_WINDUP) {
            // Sword raised high with both hands, leaning back, trembling with power
            float k = weight(t, SkeletonKing.WHIRLWIND_WINDUP + 4, 8, 4);
            raiseBothArms(k, -2.9F, 0.3F);
            waist.xRot = lerp(k, waist.xRot, -0.15F);
            waist.yRot = Mth.sin(t * 1.7F) * 0.04F * k;
            jaw.xRot = lerp(k, jaw.xRot, 0.45F);
            return;
        }
        float spin = t - SkeletonKing.WHIRLWIND_WINDUP;
        float k = weight(spin, SkeletonKing.WHIRLWIND_LENGTH - SkeletonKing.WHIRLWIND_WINDUP, 3, 6);
        base.yRot = spin * 0.9F;
        rightArm.xRot = lerp(k, rightArm.xRot, -0.15F);
        rightArm.zRot = lerp(k, rightArm.zRot, 1.45F);
        leftArm.xRot = lerp(k, leftArm.xRot, -0.15F);
        leftArm.zRot = lerp(k, leftArm.zRot, -1.45F);
        rightLeg.zRot = 0.15F * k;
        leftLeg.zRot = -0.15F * k;
        cape.xRot = lerp(k, cape.xRot, 1.1F);
        jaw.xRot = lerp(k, jaw.xRot, 0.3F);
    }

    private void eruption(float t) {
        float k = weight(t, SkeletonKing.ERUPTION_LENGTH, 6, 6);
        float plunge = after(t, SkeletonKing.ERUPTION_SLAM - 2, 3);
        raiseBothArms(k, lerp(plunge, -2.8F, -0.55F), 0.25F);
        waist.xRot = lerp(k, waist.xRot, lerp(plunge, -0.12F, 0.5F));
        head.xRot = lerp(k * plunge, head.xRot, -0.3F);
        cape.xRot = lerp(k * plunge, cape.xRot, 0.5F);
        jaw.xRot = lerp(k, jaw.xRot, 0.35F);
    }

    private void volley(float t) {
        float k = weight(t, SkeletonKing.VOLLEY_LENGTH, 5, 5);
        float recoil = Math.max(0.0F, 1.0F - Math.abs(t - SkeletonKing.VOLLEY_SHOT) / 3.0F);
        leftArm.xRot = lerp(k, leftArm.xRot, -1.55F + recoil * 0.35F);
        leftArm.yRot = lerp(k, leftArm.yRot, 0.15F);
        rightArm.xRot = lerp(k, rightArm.xRot, 0.3F);
        waist.yRot = lerp(k, waist.yRot, 0.25F);
    }

    private void summon(float t) {
        float k = weight(t, SkeletonKing.SUMMON_LENGTH, 8, 6);
        rightArm.xRot = lerp(k, rightArm.xRot, -2.7F);
        rightArm.zRot = lerp(k, rightArm.zRot, 0.55F);
        leftArm.xRot = lerp(k, leftArm.xRot, -2.7F);
        leftArm.zRot = lerp(k, leftArm.zRot, -0.55F);
        head.xRot = lerp(k, head.xRot, -0.55F);
        jaw.xRot = lerp(k, jaw.xRot, 0.55F);
        waist.xRot = lerp(k, waist.xRot, -0.18F);
    }

    private void raiseBothArms(float k, float pitch, float inward) {
        rightArm.xRot = lerp(k, rightArm.xRot, pitch);
        rightArm.zRot = lerp(k, rightArm.zRot, -inward);
        leftArm.xRot = lerp(k, leftArm.xRot, pitch);
        leftArm.zRot = lerp(k, leftArm.zRot, inward);
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack poseStack) {
        root.translateAndRotate(poseStack);
        base.translateAndRotate(poseStack);
        waist.translateAndRotate(poseStack);
        (side == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(poseStack);
    }
}
