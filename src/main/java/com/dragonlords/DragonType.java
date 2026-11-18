package com.dragonlords;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** One entry per dragon. Add a constant (+ texture + biome json) to add a new dragon. */
public enum DragonType {
    //   id        hp     breath chargeDmg scale tameFood                 breath               aux                      ambient                    pitch sound                                   egg1     egg2
    FIRE ("fire",  300.0, 7.0f,  40.0f, 2.4f, Items.COOKED_PORKCHOP, ParticleTypes.FLAME,         ParticleTypes.LARGE_SMOKE,  ParticleTypes.FLAME,          0.75f, SoundEvents.BLAZE_SHOOT,            0xB5200F, 0xFF9A1F),
    ICE  ("ice",   280.0, 6.0f,  38.0f, 2.2f, Items.SALMON,          ParticleTypes.SNOWFLAKE,     ParticleTypes.ITEM_SNOWBALL, ParticleTypes.SNOWFLAKE,     1.00f, SoundEvents.SNOW_GOLEM_SHOOT,       0xAAD2EB, 0xF0FAFF),
    FOREST("forest",260.0, 6.0f, 36.0f, 2.0f, Items.COOKED_CHICKEN,  ParticleTypes.SNEEZE,        ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.SPORE_BLOSSOM_AIR, 0.90f, SoundEvents.LLAMA_SPIT,      0x287837, 0x8CC846),
    OCEAN("ocean", 280.0, 6.5f,  38.0f, 2.3f, Items.COD,             ParticleTypes.SPLASH,        ParticleTypes.BUBBLE_POP,    ParticleTypes.FALLING_WATER, 0.85f, SoundEvents.GENERIC_SPLASH,         0x196E96, 0x50DCC8),
    STORM("storm", 320.0, 7.0f,  42.0f, 2.6f, Items.COOKED_MUTTON,   ParticleTypes.ELECTRIC_SPARK,ParticleTypes.CLOUD,        ParticleTypes.ELECTRIC_SPARK, 0.70f, SoundEvents.LIGHTNING_BOLT_IMPACT, 0x4B5069, 0x78AAFF);

    public final String id;
    public final double health;
    public final float breathDamage, chargeDamage, scale, voicePitch;
    public final Item tameFood;
    public final ParticleOptions breath, breathAux, ambient;
    public final SoundEvent breathSound;
    public final int eggPrimary, eggSecondary;

    DragonType(String id, double health, float breathDamage, float chargeDamage, float scale, Item tameFood,
               ParticleOptions breath, ParticleOptions breathAux, ParticleOptions ambient,
               float voicePitch, SoundEvent breathSound, int eggPrimary, int eggSecondary) {
        this.id = id; this.health = health; this.breathDamage = breathDamage; this.chargeDamage = chargeDamage;
        this.scale = scale; this.tameFood = tameFood; this.breath = breath; this.breathAux = breathAux;
        this.ambient = ambient; this.voicePitch = voicePitch; this.breathSound = breathSound;
        this.eggPrimary = eggPrimary; this.eggSecondary = eggSecondary;
    }

    public String entityName() { return id + "_dragon"; }

    public static DragonType byEntityPath(String path) {
        for (DragonType t : values()) if (path.equals(t.entityName())) return t;
        return FIRE;
    }
}
