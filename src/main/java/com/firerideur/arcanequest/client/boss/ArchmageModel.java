package com.firerideur.arcanequest.client.boss;

import com.firerideur.arcanequest.boss.BossCore;
import com.firerideur.arcanequest.boss.CorruptedArchmage;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import static com.firerideur.arcanequest.client.boss.BossAnim.after;
import static com.firerideur.arcanequest.client.boss.BossAnim.lerp;
import static com.firerideur.arcanequest.client.boss.BossAnim.weight;

/** Hooded mage hovering above the ground, staff in hand, with three crystals orbiting around him. */
public class ArchmageModel extends HierarchicalModel<CorruptedArchmage> {
    private static final float ORBIT_RADIUS = 11.0F;

    private final ModelPart root;
    private final ModelPart base;
    private final ModelPart waist;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart staff;
    private final ModelPart robe;
    private final ModelPart[] crystals = new ModelPart[3];
    private float partialTick;

    public ArchmageModel(ModelPart root) {
        this.root = root;
        this.base = root.getChild("base");
        this.waist = base.getChild("waist");
        this.head = waist.getChild("head");
        this.rightArm = waist.getChild("right_arm");
        this.leftArm = waist.getChild("left_arm");
        this.staff = rightArm.getChild("staff");
        this.robe = waist.getChild("robe");
        for (int i = 0; i < crystals.length; i++) {
            crystals[i] = base.getChild("crystal" + i);
        }
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void prepareMobModel(CorruptedArchmage mage, float limbSwing, float limbSwingAmount, float partialTick) {
        this.partialTick = partialTick;
    }

    @Override
    public void setupAnim(CorruptedArchmage mage, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        BossCore core = mage.getCore();
        boolean enraged = core.isEnraged();

        // Floating: no legs, the robe trails behind when moving
        base.y = -3.5F + Mth.sin(age * 0.1F) * 1.0F;
        head.yRot = headYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        waist.xRot = limbSwingAmount * 0.15F;
        robe.xRot = limbSwingAmount * 0.45F + Mth.sin(age * 0.06F) * 0.05F;
        robe.zRot = Mth.cos(age * 0.05F) * 0.03F;

        rightArm.xRot = -0.55F + Mth.sin(age * 0.08F) * 0.04F;
        rightArm.zRot = 0.08F;
        leftArm.xRot = -0.2F + Mth.sin(age * 0.08F + 1.0F) * 0.06F;
        leftArm.zRot = -0.18F;
        if (mage.isCastingSpell()) {
            // Vanilla evoker spells (fangs, vexes): both hands up
            rightArm.xRot = -2.3F;
            rightArm.zRot = 0.35F;
            leftArm.xRot = -2.3F;
            leftArm.zRot = -0.35F;
        }

        float orbitSpeed = enraged ? 0.14F : 0.06F;
        float radius = ORBIT_RADIUS;
        float rise = 0.0F;
        float staffTilt = 0.0F;
        float t = core.attackTime(partialTick);
        switch (core.getAttack()) {
            case CorruptedArchmage.BOLTS -> {
                float k = weight(t, CorruptedArchmage.BOLTS_LENGTH, 5, 5);
                rightArm.xRot = lerp(k, rightArm.xRot, -1.45F);
                staffTilt = -1.1F * k;
                waist.yRot = lerp(k, 0.0F, 0.2F);
            }
            case CorruptedArchmage.METEOR -> {
                float k = weight(t, CorruptedArchmage.METEOR_LENGTH, 8, 6);
                float fall = after(t, CorruptedArchmage.METEOR_FALL, 4);
                float pitch = lerp(fall, -2.9F, -1.3F);
                rightArm.xRot = lerp(k, rightArm.xRot, pitch);
                rightArm.zRot = lerp(k, rightArm.zRot, 0.25F);
                leftArm.xRot = lerp(k, leftArm.xRot, pitch);
                leftArm.zRot = lerp(k, leftArm.zRot, -0.25F);
                head.xRot = lerp(k * (1.0F - fall), head.xRot, -0.45F);
                rise = 7.0F * k * (1.0F - fall);
                orbitSpeed *= 1.0F + 3.0F * k;
            }
            case CorruptedArchmage.NOVA -> {
                float k = weight(t, CorruptedArchmage.NOVA_LENGTH, 6, 6);
                float blast = after(t, CorruptedArchmage.NOVA_BLAST, 2);
                // Arms crossed while gathering, then thrown wide open
                rightArm.xRot = lerp(k, rightArm.xRot, lerp(blast, -1.1F, -0.3F));
                rightArm.zRot = lerp(k, rightArm.zRot, lerp(blast, -0.7F, 1.35F));
                leftArm.xRot = lerp(k, leftArm.xRot, lerp(blast, -1.1F, -0.3F));
                leftArm.zRot = lerp(k, leftArm.zRot, lerp(blast, 0.7F, -1.35F));
                waist.xRot = lerp(k, waist.xRot, lerp(blast, 0.25F, -0.2F));
                radius = lerp(k, ORBIT_RADIUS, lerp(blast, 4.0F, 16.0F));
                orbitSpeed *= 1.0F + 4.0F * k;
            }
            default -> {
            }
        }
        // The staff stays upright in the hand unless pointed at the target
        staff.xRot = -rightArm.xRot + staffTilt;
        staff.zRot = -rightArm.zRot;

        for (int i = 0; i < crystals.length; i++) {
            float a = age * orbitSpeed + i * Mth.TWO_PI / crystals.length;
            ModelPart crystal = crystals[i];
            crystal.x = Mth.cos(a) * radius;
            crystal.z = Mth.sin(a) * radius;
            crystal.y = 2.0F + Mth.sin(age * 0.1F + i * 2.0F) * 2.0F - rise;
            crystal.yRot = age * 0.08F + i;
            crystal.xRot = 0.6F;
        }
    }
}
