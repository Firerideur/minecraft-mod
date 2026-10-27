package com.firerideur.arcanequest.registry;

import com.firerideur.arcanequest.ArcaneQuest;
import com.firerideur.arcanequest.item.ArcaniteArmorItem;
import com.firerideur.arcanequest.item.BossSummonItem;
import com.firerideur.arcanequest.item.HammerItem;
import com.firerideur.arcanequest.item.LegendaryWeaponItem;
import com.firerideur.arcanequest.item.StaffItem;
import com.firerideur.arcanequest.magic.Spell;
import com.firerideur.arcanequest.weapon.WeaponItem;
import com.firerideur.arcanequest.weapon.WeaponType;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ArcaneQuest.MODID);

    /** Every item, in creative tab order. */
    private static final List<RegistryObject<? extends Item>> TAB_ORDER = new ArrayList<>();

    // Materials
    public static final RegistryObject<Item> RAW_MITHRIL = simple("raw_mithril");
    public static final RegistryObject<Item> MITHRIL_INGOT = simple("mithril_ingot");
    public static final RegistryObject<Item> ARCANITE = simple("arcanite");
    public static final RegistryObject<Item> RAW_ADAMANTITE = register("raw_adamantite", () -> new Item(new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> ADAMANTITE_INGOT = register("adamantite_ingot", () -> new Item(new Item.Properties().fireResistant()));

    // Mithril gear
    public static final RegistryObject<Item> MITHRIL_SWORD = sword("mithril_sword", ModTiers.MITHRIL, 3, -2.4F, false);
    public static final RegistryObject<Item> MITHRIL_PICKAXE = pickaxe("mithril_pickaxe", ModTiers.MITHRIL, false);
    public static final RegistryObject<Item> MITHRIL_AXE = axe("mithril_axe", ModTiers.MITHRIL, 5.5F, -3.0F, false);
    public static final RegistryObject<Item> MITHRIL_SHOVEL = shovel("mithril_shovel", ModTiers.MITHRIL, false);
    public static final RegistryObject<Item> MITHRIL_HOE = hoe("mithril_hoe", ModTiers.MITHRIL, -2.5F, -0.5F, false);
    public static final RegistryObject<Item> MITHRIL_HELMET = armor("mithril_helmet", ModArmorMaterials.MITHRIL, ArmorItem.Type.HELMET, ModArmorMaterials.MITHRIL_DURABILITY, false);
    public static final RegistryObject<Item> MITHRIL_CHESTPLATE = armor("mithril_chestplate", ModArmorMaterials.MITHRIL, ArmorItem.Type.CHESTPLATE, ModArmorMaterials.MITHRIL_DURABILITY, false);
    public static final RegistryObject<Item> MITHRIL_LEGGINGS = armor("mithril_leggings", ModArmorMaterials.MITHRIL, ArmorItem.Type.LEGGINGS, ModArmorMaterials.MITHRIL_DURABILITY, false);
    public static final RegistryObject<Item> MITHRIL_BOOTS = armor("mithril_boots", ModArmorMaterials.MITHRIL, ArmorItem.Type.BOOTS, ModArmorMaterials.MITHRIL_DURABILITY, false);

    // Arcanite mage armor
    public static final RegistryObject<Item> ARCANITE_HELMET = arcaniteArmor("arcanite_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> ARCANITE_CHESTPLATE = arcaniteArmor("arcanite_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> ARCANITE_LEGGINGS = arcaniteArmor("arcanite_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> ARCANITE_BOOTS = arcaniteArmor("arcanite_boots", ArmorItem.Type.BOOTS);

    // Adamantite gear
    public static final RegistryObject<Item> ADAMANTITE_SWORD = sword("adamantite_sword", ModTiers.ADAMANTITE, 3, -2.4F, true);
    public static final RegistryObject<Item> ADAMANTITE_PICKAXE = pickaxe("adamantite_pickaxe", ModTiers.ADAMANTITE, true);
    public static final RegistryObject<Item> ADAMANTITE_AXE = axe("adamantite_axe", ModTiers.ADAMANTITE, 5.0F, -3.0F, true);
    public static final RegistryObject<Item> ADAMANTITE_SHOVEL = shovel("adamantite_shovel", ModTiers.ADAMANTITE, true);
    public static final RegistryObject<Item> ADAMANTITE_HOE = hoe("adamantite_hoe", ModTiers.ADAMANTITE, -4.5F, 0.0F, true);
    public static final RegistryObject<Item> ADAMANTITE_HELMET = armor("adamantite_helmet", ModArmorMaterials.ADAMANTITE, ArmorItem.Type.HELMET, ModArmorMaterials.ADAMANTITE_DURABILITY, true);
    public static final RegistryObject<Item> ADAMANTITE_CHESTPLATE = armor("adamantite_chestplate", ModArmorMaterials.ADAMANTITE, ArmorItem.Type.CHESTPLATE, ModArmorMaterials.ADAMANTITE_DURABILITY, true);
    public static final RegistryObject<Item> ADAMANTITE_LEGGINGS = armor("adamantite_leggings", ModArmorMaterials.ADAMANTITE, ArmorItem.Type.LEGGINGS, ModArmorMaterials.ADAMANTITE_DURABILITY, true);
    public static final RegistryObject<Item> ADAMANTITE_BOOTS = armor("adamantite_boots", ModArmorMaterials.ADAMANTITE, ArmorItem.Type.BOOTS, ModArmorMaterials.ADAMANTITE_DURABILITY, true);

    // Staffs
    public static final RegistryObject<Item> FIRE_STAFF = staff("fire_staff", Spell.FIRE_BOLT, 250, Rarity.COMMON);
    public static final RegistryObject<Item> FROST_STAFF = staff("frost_staff", Spell.FROST_BOLT, 250, Rarity.COMMON);
    public static final RegistryObject<Item> HEALING_STAFF = staff("healing_staff", Spell.HEAL, 200, Rarity.UNCOMMON);
    public static final RegistryObject<Item> ARCANE_STAFF = staff("arcane_staff", Spell.ARCANE_MISSILE, 400, Rarity.UNCOMMON);
    public static final RegistryObject<Item> LIGHTNING_STAFF = staff("lightning_staff", Spell.LIGHTNING, 300, Rarity.RARE);

    // Boss summons
    public static final RegistryObject<Item> CURSED_SKULL = register("cursed_skull",
            () -> new BossSummonItem(ModEntities.SKELETON_KING, new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> FORBIDDEN_GRIMOIRE = register("forbidden_grimoire",
            () -> new BossSummonItem(ModEntities.CORRUPTED_ARCHMAGE, new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> GOLEM_CORE = register("golem_core",
            () -> new BossSummonItem(ModEntities.ADAMANTITE_GOLEM, new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    // Boss drops
    public static final RegistryObject<Item> KINGS_BLADE = register("kings_blade",
            () -> new LegendaryWeaponItem(ModTiers.LEGENDARY, LegendaryWeaponItem.Effect.WITHER, legendary()
                    .attributes(SwordItem.createAttributes(ModTiers.LEGENDARY, 4, -2.4F))));
    public static final RegistryObject<Item> ARCHMAGE_STAFF = staff("archmage_staff", Spell.ARCANE_BARRAGE, 1000, Rarity.EPIC);
    public static final RegistryObject<Item> ADAMANTITE_HAMMER = register("adamantite_hammer",
            () -> new LegendaryWeaponItem(ModTiers.LEGENDARY, LegendaryWeaponItem.Effect.SHOCKWAVE, legendary()
                    .attributes(SwordItem.createAttributes(ModTiers.LEGENDARY, 9, -3.2F))));

    // Spawn eggs
    public static final RegistryObject<Item> SKELETON_KING_SPAWN_EGG = register("skeleton_king_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SKELETON_KING, 0x1C1C1C, 0xD4AF37, new Item.Properties()));
    public static final RegistryObject<Item> CORRUPTED_ARCHMAGE_SPAWN_EGG = register("corrupted_archmage_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CORRUPTED_ARCHMAGE, 0x3B1F5C, 0xB070E0, new Item.Properties()));
    public static final RegistryObject<Item> ADAMANTITE_GOLEM_SPAWN_EGG = register("adamantite_golem_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ADAMANTITE_GOLEM, 0x5A1010, 0xC03030, new Item.Properties()));
    public static final RegistryObject<Item> GOBLIN_SPAWN_EGG = register("goblin_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GOBLIN, 0x5E8C3E, 0x3A2614, new Item.Properties()));
    public static final RegistryObject<Item> GOBLIN_SHAMAN_SPAWN_EGG = register("goblin_shaman_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GOBLIN_SHAMAN, 0x587450, 0x782822, new Item.Properties()));
    public static final RegistryObject<Item> STEELFANG_WOLF_SPAWN_EGG = register("steelfang_wolf_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.STEELFANG_WOLF, 0x404048, 0x9EA2AC, new Item.Properties()));
    public static final RegistryObject<Item> ICE_ELF_SPAWN_EGG = register("ice_elf_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ICE_ELF, 0xCCDEF0, 0x345CAA, new Item.Properties()));
    public static final RegistryObject<Item> ANT_SOLDIER_SPAWN_EGG = register("ant_soldier_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ANT_SOLDIER, 0x6C261C, 0xFF8C1E, new Item.Properties()));
    public static final RegistryObject<Item> HIGH_ORC_SPAWN_EGG = register("high_orc_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HIGH_ORC, 0x687C52, 0x6E4C32, new Item.Properties()));
    public static final RegistryObject<Item> DEMON_KNIGHT_SPAWN_EGG = register("demon_knight_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.DEMON_KNIGHT, 0x2E242C, 0xFF6020, new Item.Properties()));

    // Fusion Forge
    public static final RegistryObject<Item> FUSION_CATALYST = simple("fusion_catalyst");
    public static final RegistryObject<Item> STEEL_INGOT = simple("steel_ingot");
    public static final RegistryObject<Item> ORICHALCUM_INGOT = simple("orichalcum_ingot");
    public static final RegistryObject<Item> ARCANIUM_INGOT = register("arcanium_ingot", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> STARSTEEL_INGOT = register("starsteel_ingot", () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));

    // Weapon Forge parts: templates are not consumed
    public static final Map<WeaponType, RegistryObject<Item>> TEMPLATES = new EnumMap<>(WeaponType.class);
    static {
        for (WeaponType type : WeaponType.values()) {
            TEMPLATES.put(type, register(type.key() + "_template", () -> new Item(new Item.Properties().stacksTo(1))));
        }
    }
    public static final RegistryObject<Item> WEAPON_GRIP = simple("weapon_grip");
    public static final RegistryObject<Item> WEAPON_SHAFT = simple("weapon_shaft");

    /** Weapon materials, in progression order. */
    public enum WeaponMaterial {
        IRON("iron", () -> Tiers.IRON, false, 1),
        STEEL("steel", () -> ModTiers.STEEL, false, 1),
        DIAMOND("diamond", () -> Tiers.DIAMOND, false, 2),
        MITHRIL("mithril", () -> ModTiers.MITHRIL, false, 2),
        ORICHALCUM("orichalcum", () -> ModTiers.ORICHALCUM, false, 2),
        ADAMANTITE("adamantite", () -> ModTiers.ADAMANTITE, true, 3),
        ARCANIUM("arcanium", () -> ModTiers.ARCANIUM, true, 3),
        STARSTEEL("starsteel", () -> ModTiers.STARSTEEL, true, 4);

        public final String key;
        private final Supplier<Tier> tier;
        private final boolean fireResistant;
        /** Mining hammer reach: 1 = 3x3, 2 = 5x5, 3 = 7x7, 4 = 9x9. */
        private final int hammerRadius;

        WeaponMaterial(String key, Supplier<Tier> tier, boolean fireResistant, int hammerRadius) {
            this.key = key;
            this.tier = tier;
            this.fireResistant = fireResistant;
            this.hammerRadius = hammerRadius;
        }
    }

    /** Every new weapon, by material then type: e.g. WEAPONS.get(MITHRIL).get(KATANA). */
    public static final Map<WeaponMaterial, Map<WeaponType, RegistryObject<Item>>> WEAPONS = new EnumMap<>(WeaponMaterial.class);
    static {
        for (WeaponMaterial material : WeaponMaterial.values()) {
            Map<WeaponType, RegistryObject<Item>> byType = new EnumMap<>(WeaponType.class);
            for (WeaponType type : WeaponType.values()) {
                byType.put(type, register(material.key + "_" + type.key(),
                        () -> new WeaponItem(material.tier.get(), type, props(material.fireResistant))));
            }
            WEAPONS.put(material, byType);
        }
    }

    // Mining hammers: square size grows with the metal (3x3 to 9x9)
    public static final RegistryObject<Item> HAMMER_TEMPLATE = register("hammer_template", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final Map<WeaponMaterial, RegistryObject<Item>> MINING_HAMMERS = new EnumMap<>(WeaponMaterial.class);
    static {
        for (WeaponMaterial material : WeaponMaterial.values()) {
            MINING_HAMMERS.put(material, register(material.key + "_mining_hammer",
                    () -> new HammerItem(material.tier.get(), material.hammerRadius, props(material.fireResistant))));
        }
    }

    private ModItems() {}

    public static List<RegistryObject<? extends Item>> tabOrder() {
        return Collections.unmodifiableList(TAB_ORDER);
    }

    static void registerBlockItem(String name, RegistryObject<Block> block) {
        register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Item> RegistryObject<T> register(String name, Supplier<T> item) {
        RegistryObject<T> object = ITEMS.register(name, item);
        TAB_ORDER.add(object);
        return object;
    }

    private static RegistryObject<Item> simple(String name) {
        return register(name, () -> new Item(new Item.Properties()));
    }

    private static Item.Properties props(boolean fireResistant) {
        Item.Properties properties = new Item.Properties();
        return fireResistant ? properties.fireResistant() : properties;
    }

    private static Item.Properties legendary() {
        return new Item.Properties().rarity(Rarity.EPIC).fireResistant();
    }

    private static RegistryObject<Item> sword(String name, Tier tier, int damage, float speed, boolean fireResistant) {
        return register(name, () -> new SwordItem(tier, props(fireResistant).attributes(SwordItem.createAttributes(tier, damage, speed))));
    }

    private static RegistryObject<Item> pickaxe(String name, Tier tier, boolean fireResistant) {
        return register(name, () -> new PickaxeItem(tier, props(fireResistant).attributes(DiggerItem.createAttributes(tier, 1.0F, -2.8F))));
    }

    private static RegistryObject<Item> axe(String name, Tier tier, float damage, float speed, boolean fireResistant) {
        return register(name, () -> new AxeItem(tier, props(fireResistant).attributes(DiggerItem.createAttributes(tier, damage, speed))));
    }

    private static RegistryObject<Item> shovel(String name, Tier tier, boolean fireResistant) {
        return register(name, () -> new ShovelItem(tier, props(fireResistant).attributes(DiggerItem.createAttributes(tier, 1.5F, -3.0F))));
    }

    private static RegistryObject<Item> hoe(String name, Tier tier, float damage, float speed, boolean fireResistant) {
        return register(name, () -> new HoeItem(tier, props(fireResistant).attributes(DiggerItem.createAttributes(tier, damage, speed))));
    }

    private static RegistryObject<Item> armor(String name, RegistryObject<ArmorMaterial> material, ArmorItem.Type type,
                                              int durability, boolean fireResistant) {
        return register(name, () -> new ArmorItem(holder(material), type, props(fireResistant).durability(type.getDurability(durability))));
    }

    private static RegistryObject<Item> arcaniteArmor(String name, ArmorItem.Type type) {
        return register(name, () -> new ArcaniteArmorItem(holder(ModArmorMaterials.ARCANITE), type,
                new Item.Properties().durability(type.getDurability(ModArmorMaterials.ARCANITE_DURABILITY))));
    }

    private static RegistryObject<Item> staff(String name, Spell spell, int durability, Rarity rarity) {
        return register(name, () -> new StaffItem(spell, new Item.Properties().durability(durability).rarity(rarity)));
    }

    private static Holder<ArmorMaterial> holder(RegistryObject<ArmorMaterial> material) {
        return material.getHolder().orElseThrow(() -> new IllegalStateException("Armor material not registered yet: " + material.getId()));
    }
}
