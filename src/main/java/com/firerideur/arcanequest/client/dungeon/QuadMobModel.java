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

    public QuadMobModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("base").getChild("body");
        this.head = body.getChild("head");
        this.tail = body.hasChild("tail") ? body.getChild("tail") : null;
        ModelPart legHolder = body.hasChild("leg0") ? body : root.getChild("base");
        for (int i = 0; legHolder.hasChild("leg" + i); i++) {
            legs.add(legHolder.getChild("leg" + i));
        }
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
            legs.get(i).xRot += Mth.cos(walk + phase) * 1.1F * limbSwingAmount;
        }
        if (tail != null) {
            tail.yRot = Mth.cos(age * 0.15F) * 0.15F + Mth.cos(walk) * 0.3F * limbSwingAmount;
        }
        body.zRot = Mth.cos(walk) * 0.03F * limbSwingAmount;
        if (attackTime > 0) {
            // Lunge and bite
            float bite = Mth.sin(attackTime * Mth.PI);
            head.xRot += bite * 0.6F;
            body.xRot = -bite * 0.15F;
        }
    }
}
