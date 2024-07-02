package me.otter.mintaddon.util;

import static me.otter.mintaddon.MintAddon.mc;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.explosion.Explosion;

public class DamageUtils {
    // Function to calculate explosion damage
    public static float getExplosionDamage(Vec3d explosionPos, float power, LivingEntity target) {
        if (mc.world.getDifficulty() == Difficulty.PEACEFUL)
            return 0f;

        double maxDist = power * 2;
        Box explosionBox = new Box(
                MathHelper.floor(explosionPos.x - maxDist - 1.0),
                MathHelper.floor(explosionPos.y - maxDist - 1.0),
                MathHelper.floor(explosionPos.z - maxDist - 1.0),
                MathHelper.floor(explosionPos.x + maxDist + 1.0),
                MathHelper.floor(explosionPos.y + maxDist + 1.0),
                MathHelper.floor(explosionPos.z + maxDist + 1.0));

        if (!mc.world.getOtherEntities(null, explosionBox).contains(target)) {
            return 0f;
        }

        if (!target.isInvulnerable()) {
            double distExposure = Math.sqrt(target.squaredDistanceTo(explosionPos)) / maxDist;
            if (distExposure <= 1.0) {
                double xDiff = target.getX() - explosionPos.x;
                double yDiff = target.getEyeY() - explosionPos.y;
                double zDiff = target.getZ() - explosionPos.z;
                double diff = Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
                if (diff != 0.0) {
                    double exposure = Explosion.getExposure(explosionPos, target);
                    double finalExposure = (1.0 - distExposure) * exposure;

                    float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * maxDist + 1.0);

                    if (target instanceof PlayerEntity) {
                        if (mc.world.getDifficulty() == Difficulty.EASY) {
                            toDamage = Math.min(toDamage / 2f + 1f, toDamage);
                        } else if (mc.world.getDifficulty() == Difficulty.HARD) {
                            toDamage = toDamage * 3f / 2f;
                        }
                    }

                    // Armor reduction
                    toDamage = DamageUtil.getDamageLeft(toDamage, mc.world.getDamageSources().explosion(null, null),
                            (float) target.getArmor(), (float) target.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS).getValue());

                    // Enchantment reduction
                    if (target.hasStatusEffect(StatusEffects.RESISTANCE)) {
                        int resistance = 25 - (target.getStatusEffect(StatusEffects.RESISTANCE).getAmplifier() + 1) * 5;
                        float resistanceReduction = toDamage * resistance;
                        toDamage = Math.max(resistanceReduction / 25f, 0f);
                    }

                    if (toDamage > 0f) {
                        int protAmount = EnchantmentHelper.getProtectionAmount(target.getArmorItems(), mc.world.getDamageSources().explosion(null, null));
                        if (protAmount > 0) {
                            toDamage = DamageUtil.getInflictedDamage(toDamage, protAmount);
                        }
                    }

                    return toDamage;
                }
            }
        }

        return 0f;
    }
}