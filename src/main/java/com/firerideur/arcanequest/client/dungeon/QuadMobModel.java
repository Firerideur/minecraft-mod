package com.firerideur.arcanequest.client.dungeon;

import com.firerideur.arcanequest.dungeon.DungeonMob;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** Animation of the many-legged dungeon monsters (wolves, ants): diagonal gait, head, wagging tail, bite. */
public class QuadMobModel<T extends DungeonMob> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final List<ModelPart> legs = new ArrayList<>();
    /** Spider legs stick out sideways: they swing horizontally. */
    private final boolean sidewaysLegs;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightClaw;
    private final ModelPart leftClaw;

    public QuadMobModel(ModelPart root) {
        this(root, false);
    }

    public QuadMobModel(ModelPart root, boolean sidewaysLegs) {
        this.root = root;
        this.sidewaysLegs = sidewaysLegs;
        this.body = root.getChild("base").getChild("body");
        this.head = body.getChild("head");
        this.tail = body.hasChild("tail") ? body.getChild("tail") : null;
        ModelPart legHolder = body.hasChild("leg0") ? body : root.getChild("base");
        for (int i = 0; legHolder.hasChild("leg" + i); i++) {
            legs.add(legHolder.getChild("leg" + i));
        }
        this.rightWing = body.hasChild("right_wing") ? body.getChild("right_wing") : null;
        this.leftWing = body.hasChild("left_wing") ? body.getChild("left_wing") : null;
        this.rightClaw = body.hasChild("right_claw") ? body.getChild("right_claw") : null;
        this.leftClaw = body.hasChild("left_claw") ? body.getChild("left_claw") : null;
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T mob, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = headYaw * Mth.DEG_TO_RAD * 0.8F;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.6F;
        float walk = limbSwing * 0.8F;
        for (int i = 0; i < legs.size(); i++) {
            // Diagonal pairs move together
            float phase = ((i % 2) + (i / 2)) % 2 == 0 ? 0.0F : Mth.PI;
            float swing = Mth.cos(walk + phase) * limbSwingAmount;
            if (sidewaysLegs) {
                legs.get(i).yRot += swing * 0.5F;
            } else {
                legs.get(i).xRot += swing * 1.1F;
            }
        }
        if (tail != null) {
            tail.yRot = Mth.cos(age * 0.15F) * 0.15F + Mth.cos(walk) * 0.3F * limbSwingAmount;
        }
        body.zRot = Mth.cos(walk) * 0.03F * limbSwingAmount;
        if (rightWing != null && leftWing != null) {
            // Insect wings buzz very fast
            float buzz = Mth.sin(age * 2.6F) * 0.5F;
            rightWing.zRot += buzz;
            leftWing.zRot -= buzz;
        }
        if (rightClaw != null && leftClaw != null) {
            // Raptorial claws held up, slashing on attack
            float slash = attackTime > 0 ? Mth.sin(attackTime * Mth.PI) * 1.2F : 0.0F;
            rightClaw.xRot += -0.3F + slash + Mth.sin(age * 0.1F) * 0.05F;
            leftClaw.xRot += -0.3F + slash + Mth.cos(age * 0.1F) * 0.05F;
        }
        if (attackTime > 0) {
            // Lunge and bite
            float bite = Mth.sin(attackTime * Mth.PI);
            head.xRot += bite * 0.6F;
            body.xRot = -bite * 0.15F;
        }
    }
}
