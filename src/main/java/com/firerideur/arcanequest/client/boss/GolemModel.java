package com.firerideur.arcanequest.client.boss;

import com.firerideur.arcanequest.boss.AdamantiteGolem;
import com.firerideur.arcanequest.boss.BossCore;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.firerideur.arcanequest.client.boss.BossAnim.after;
import static com.firerideur.arcanequest.client.boss.BossAnim.lerp;
import static com.firerideur.arcanequest.client.boss.BossAnim.weight;

/** Hunched colossus walking on its huge fists, with crystals on its back and a molten core. */
public class GolemModel extends HierarchicalModel<AdamantiteGolem> {
    private static final float HUNCH = 0.35F;

    private final ModelPart root;
    private final ModelPart chest;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private float partialTick;

    public GolemModel(ModelPart root) {
        this.root = root;
        ModelPart base = root.getChild("base");
        this.chest = base.getChild("waist").getChild("chest");
        this.head = chest.getChild("head");
        this.rightArm = chest.getChild("right_arm");
        this.rightForearm = rightArm.getChild("right_forearm");
        this.leftArm = chest.getChild("left_arm");
        this.leftForearm = leftArm.getChild("left_forearm");
        this.rightLeg = base.getChild("right_leg");
        this.leftLeg = base.getChild("left_leg");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void prepareMobModel(AdamantiteGolem golem, float limbSwing, float limbSwingAmount, float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(AdamantiteGolem golem, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = headYaw * Mth.DEG_TO_RAD * 0.7F;
        // The head stays level while the chest leans forward
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F - HUNCH;

        float walk = limbSwing * 0.5F;
        rightLeg.xRot = Mth.cos(walk) * 1.0F * limbSwingAmount;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.0F * limbSwingAmount;
        rightArm.xRot = Mth.cos(walk + Mth.PI) * 0.7F * limbSwingAmount;
        leftArm.xRot = Mth.cos(walk) * 0.7F * limbSwingAmount;
        rightForearm.xRot = -0.15F;
        leftForearm.xRot = -0.15F;
        // Hunched, breathing slowly
        chest.xRot = HUNCH + Mth.sin(age * 0.05F) * 0.025F;
        rightArm.zRot = 0.06F + Mth.sin(age * 0.05F) * 0.02F;
        leftArm.zRot = -0.06F - Mth.sin(age * 0.05F) * 0.02F;

        int punch = golem.getAttackAnimationTick();
        if (punch > 0) {
            float swing = triangle(punch - partialTick, 10.0F);
            rightArm.xRot = -2.0F + 1.5F * swing;
            leftArm.xRot = -2.0F + 1.5F * swing;
        }

        BossCore core = golem.getCore();
        float t = core.attackTime(partialTick);
        switch (core.getAttack()) {
            case AdamantiteGolem.SLAM -> {
                float k = weight(t, AdamantiteGolem.SLAM_LENGTH, 8, 10);
                float down = after(t, AdamantiteGolem.SLAM_IMPACT - 3, 3);
                float armPitch = lerp(down, -2.95F, -0.75F);
                rightArm.xRot = lerp(k, rightArm.xRot, armPitch);
                leftArm.xRot = lerp(k, leftArm.xRot, armPitch);
                rightArm.zRot = lerp(k, rightArm.zRot, -0.2F);
                leftArm.zRot = lerp(k, leftArm.zRot, 0.2F);
                rightForearm.xRot = lerp(k, rightForearm.xRot, lerp(down, -0.5F, 0.0F));
                leftForearm.xRot = rightForearm.xRot;
                chest.xRot = lerp(k, chest.xRot, lerp(down, -0.3F, 0.6F));
                head.xRot = lerp(k, head.xRot, lerp(down, -0.35F, -0.3F));
            }
            case AdamantiteGolem.CHARGE -> {
                float k = weight(t, AdamantiteGolem.CHARGE_LENGTH, 6, 6);
                boolean rushing = t >= AdamantiteGolem.CHARGE_WINDUP;
                chest.xRot = lerp(k, chest.xRot, rushing ? 0.75F : 0.6F);
                head.xRot = lerp(k, head.xRot, -0.55F);
                if (rushing) {
                    float stride = age * 0.9F;
                    rightLeg.xRot = Mth.cos(stride) * 1.1F;
                    leftLeg.xRot = Mth.cos(stride + Mth.PI) * 1.1F;
                    rightArm.xRot = lerp(k, rightArm.xRot, 0.5F + Mth.cos(stride + Mth.PI) * 0.5F);
                    leftArm.xRot = lerp(k, leftArm.xRot, 0.5F + Mth.cos(stride) * 0.5F);
                } else {
                    // Pawing the ground
                    rightLeg.xRot = Math.min(0.0F, Mth.sin(t * 0.8F)) * 0.6F;
                    rightArm.xRot = lerp(k, rightArm.xRot, 0.6F);
                    leftArm.xRot = lerp(k, leftArm.xRot, 0.6F);
                }
            }
            case AdamantiteGolem.STUNNED -> {
                float k = weight(t, AdamantiteGolem.STUN_LENGTH, 4, 8);
                chest.xRot = lerp(k, chest.xRot, 0.55F + Mth.sin(age * 0.3F) * 0.05F);
                head.zRot = Mth.sin(age * 0.4F) * 0.3F * k;
                head.xRot = lerp(k, head.xRot, 0.3F);
                rightArm.xRot = lerp(k, rightArm.xRot, -0.15F);
                leftArm.xRot = lerp(k, leftArm.xRot, -0.15F);
                rightArm.zRot = lerp(k, rightArm.zRot, 0.25F);
                leftArm.zRot = lerp(k, leftArm.zRot, -0.25F);
            }
            default -> {
            }
        }
    }

    private static float triangle(float value, float period) {
        return (Math.abs(value % period - period * 0.5F) - period * 0.25F) / (period * 0.25F);
    }
}
