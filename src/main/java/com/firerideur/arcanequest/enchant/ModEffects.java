package com.firerideur.arcanequest.enchant;

import com.firerideur.arcanequest.ArcaneQuest;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ArcaneQuest.MODID);

    public static final RegistryObject<MobEffect> BLEEDING = EFFECTS.register("bleeding", BleedingEffect::new);

    private ModEffects() {}
}
