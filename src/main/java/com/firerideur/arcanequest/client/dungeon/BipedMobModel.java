package com.firerideur.arcanequest.client.dungeon;

import com.firerideur.arcanequest.dungeon.DungeonMob;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Animation shared by the humanoid dungeon monsters (goblins, elves, orcs, demon knights):
 * walking, swinging the weapon held in the right hand, and aiming for ranged ones.
 */
public class BipedMobModel<T extends DungeonMob> extends HierarchicalModel<T> {
    /** How the monster fights, for its arm poses. */
    public enum Style { MELEE, BOW, CASTER }

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart cape;
    private final Style style;

    public BipedMobModel(ModelPart root, Style style) {
        this.root = root;
        this.style = style;
        ModelPart base = root.getChild("base");
        this.body = base.getChild("body");
        this.head = body.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = base.getChild("right_leg");
        this.leftLeg = base.getChild("left_leg");
        this.cape = body.hasChild("cape") ? body.getChild("cape") : null;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T mob, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = headYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float walk = limbSwing * 0.6662F;
        rightLeg.xRot = Mth.cos(walk) * 1.3F * limbSwingAmount;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.3F * limbSwingAmount;
        float sway = Mth.cos(age * 0.09F) * 0.05F + 0.05F;
        rightArm.zRot = sway;
        leftArm.zRot = -sway;
        // The weapon arm is held a little forward
        rightArm.xRot = -0.35F + Mth.cos(walk + Mth.PI) * 0.6F * limbSwingAmount;
        leftArm.xRot = Mth.cos(walk) * 0.8F * limbSwingAmount;
        if (cape != null) {
            cape.xRot = 0.1F + limbSwingAmount * 0.6F + Mth.sin(age * 0.07F) * 0.04F;
        }
        if (mob.isAggressive()) {
            switch (style) {
                case BOW -> {
                    // Bow in the left hand, drawing with the right
                    leftArm.xRot = -Mth.HALF_PI + head.xRot;
                    leftArm.yRot = 0.1F + head.yRot;
                    rightArm.xRot = -Mth.HALF_PI + head.xRot;
                    rightArm.yRot = -0.5F + head.yRot;
                }
                case CASTER -> {
                    rightArm.xRot = -2.2F + Mth.sin(age * 0.3F) * 0.15F;
                    leftArm.xRot = -1.3F;
                    leftArm.zRot = -0.4F;
                }
                case MELEE -> {
                    rightArm.xRot = -1.0F;
                    leftArm.xRot = -0.6F;
                }
            }
        }
        if (attackTime > 0) {
            float swing = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -0.35F - swing * 2.0F;
            body.yRot = Mth.sin(Mth.sqrt(attackTime) * Mth.TWO_PI) * 0.2F;
        }
    }
}
