package com.firerideur.arcanequest.client.dungeon;

import com.firerideur.arcanequest.dungeon.DungeonMob;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** A chest: closed and still while dormant, then hopping around and chomping with its lid. */
public class MimicModel<T extends DungeonMob> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart lid;

    public MimicModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("base").getChild("body");
        this.lid = body.getChild("lid");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T mob, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        if (mob.isDormant()) {
            return;
        }
        // Chomping, faster when attacking
        float speed = mob.isAggressive() ? 0.6F : 0.25F;
        lid.xRot = -0.35F - Mth.abs(Mth.sin(age * speed)) * (mob.isAggressive() ? 0.7F : 0.35F);
        if (attackTime > 0) {
            lid.xRot = -1.1F + Mth.sin(attackTime * Mth.PI) * 1.0F;
        }
        body.yRot = headYaw * Mth.DEG_TO_RAD * 0.5F;
        body.zRot = Mth.cos(limbSwing * 0.8F) * 0.08F * limbSwingAmount;
    }
}
