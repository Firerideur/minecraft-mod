package com.firerideur.arcanequest.client.dungeon;

import com.firerideur.arcanequest.dungeon.DungeonMob;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

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
    /** Absent for the naga, which has a tail instead. */
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart cape;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    /** Tail segments, from the body to the tip. */
    private final List<ModelPart> tail = new ArrayList<>();
    private final Style style;

    public BipedMobModel(ModelPart root, Style style) {
        this.root = root;
        this.style = style;
        ModelPart base = root.getChild("base");
        this.body = base.getChild("body");
        this.head = body.getChild("head");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightLeg = base.hasChild("right_leg") ? base.getChild("right_leg") : null;
        this.leftLeg = base.hasChild("left_leg") ? base.getChild("left_leg") : null;
        this.cape = body.hasChild("cape") ? body.getChild("cape") : null;
        this.rightWing = body.hasChild("right_wing") ? body.getChild("right_wing") : null;
        this.leftWing = body.hasChild("left_wing") ? body.getChild("left_wing") : null;
        ModelPart segment = body.hasChild("tail") ? body.getChild("tail") : null;
        for (int i = 2; segment != null; i++) {
            tail.add(segment);
            segment = segment.hasChild("tail" + i) ? segment.getChild("tail" + i) : null;
        }
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T mob, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        if (mob.isDormant()) {
            // A statue: frozen, crouched, wings folded
            body.xRot = 0.35F;
            head.xRot = -0.35F;
            rightArm.xRot = -0.9F;
            leftArm.xRot = -0.9F;
            return;
        }
        head.yRot = headYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float walk = limbSwing * 0.6662F;
        if (rightLeg != null && leftLeg != null) {
            rightLeg.xRot = Mth.cos(walk) * 1.3F * limbSwingAmount;
            leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.3F * limbSwingAmount;
        }
        for (int i = 0; i < tail.size(); i++) {
            // Slithering: a wave running down the tail
            tail.get(i).yRot = Mth.sin(age * 0.15F + walk * 1.5F - i * 0.9F) * (0.15F + 0.25F * limbSwingAmount);
        }
        if (rightWing != null && leftWing != null) {
            float flap = Mth.sin(age * 0.3F) * 0.3F;
            rightWing.yRot += flap;
            leftWing.yRot -= flap;
        }
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
