package com.firerideur.arcanequest.registry;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.fusion.FusionForgeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ArcaneQuest.MODID);

    @SuppressWarnings("DataFlowIssue") // the data fixer type is unused
    public static final RegistryObject<BlockEntityType<FusionForgeBlockEntity>> FUSION_FORGE = BLOCK_ENTITIES.register("fusion_forge",
            () -> BlockEntityType.Builder.of(FusionForgeBlockEntity::new, ModBlocks.FUSION_FORGE.get()).build(null));

    private ModBlockEntities() {}
}
