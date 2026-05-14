package dmr.DragonMounts.server.entity.dragon;

import dmr.DragonMounts.server.ai.DragonAI;
import dmr.DragonMounts.server.entity.TameableDragonEntity;
import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Abstract class that implements dragon combat functionality.
 * This extends the dragon entity hierarchy with combat capabilities.
 */
abstract class DragonCombatComponent extends DragonBreedableComponent {

    protected DragonCombatComponent(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (isInvulnerableTo(src)) return false;
        getDragon().stopSitting();
        boolean flag = super.hurt(src, amount);

        if (flag && src.getEntity() instanceof LivingEntity attacker) {
            DragonAI.wasHurtBy(getDragon(), attacker);
        }
        return flag;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource src) {
        Entity srcEnt = src.getEntity();
        if (srcEnt != null && (srcEnt == this || hasPassenger(srcEnt))) return true;

        Level level = level();
        if (src == level.damageSources().dragonBreath()
                || src == level.damageSources().cactus()
                || src == level.damageSources().inWall()) {
            return true;
        }

        return (getDragon().getBreed() != null && getDragon().getBreed().getImmunities().contains(src.getMsgId())) || super.isInvulnerableTo(src);
    }

    /**
     * Checks if the dragon wants to attack a specific target.
     */
    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        // VIP: Không đánh rồng khác nếu có chung chủ nhân
        if (target instanceof TameableDragonEntity otherDragon) {
            if (Objects.equals(otherDragon.getOwnerUUID(), this.getOwnerUUID())) return false;
        }

        // VIP: Không đánh bất kỳ ai đang ngồi trên lưng mình (bao gồm cả bạn bè)
        if (this.hasPassenger(target)) return false;

        // VIP: Tuyệt đối không đánh chủ nhân
        if (Objects.equals(target, owner)) return false;

        // Gốc: Không đánh các thú nuôi khác của cùng chủ nhân
        if (target instanceof TamableAnimal tameable) {
            return !Objects.equals(tameable.getOwner(), owner);
        }
        return true;
    }

    @Override
    public boolean doHurtTarget(Entity entityIn) {
        DamageSource damageSource = level().damageSources().mobAttack(this);
        boolean attacked = entityIn.hurt(
                damageSource, (float) getAttribute(Attributes.ATTACK_DAMAGE).getValue());
        if (attacked) {
            if (level() instanceof ServerLevel serverlevel) {
                EnchantmentHelper.doPostAttackEffects(serverlevel, entityIn, damageSource);
            }
        }

        if (attacked) {
            triggerAnim("head-controller", "bite");
        }

        return attacked;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isHatchling() && !hasControllingPassenger() && super.canAttack(target);
    }

    @Override
    public void setTarget(LivingEntity target) {
        super.setTarget(target);
        getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        if (target != null) {
            getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        }
    }

    @Override
    public boolean fireImmune() {
        return (super.fireImmune()
                || (getDragon().getBreed() != null
                        && getDragon().getBreed().getImmunities().contains("onFire")));
    }

    @Override
    public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
        return !getDragon().canFly() && super.causeFallDamage(pFallDistance, pMultiplier, pSource);
    }

    @Override
    public void swing(InteractionHand hand) {
        playSound(getDragon().getAttackSound(), 1, 0.7f);
        super.swing(hand);
    }

    @Nullable @Override
    public LivingEntity getTarget() {
        return this.getTargetFromBrain();
    }

    @Override
    protected ResourceKey<LootTable> getDefaultLootTable() {
        return getBreed().getDeathLootTable() != null
                ? ResourceKey.create(Registries.LOOT_TABLE, getBreed().getDeathLootTable())
                : super.getDefaultLootTable();
    }

    public double getHealthRelative() {
        return getHealth() / (double) getMaxHealth();
    }

    public int getMaxDeathTime() {
        return 60;
    }

    @Override
    protected void tickDeath() {
        // unmount any riding entities
        ejectPassengers();

        // freeze at place
        setDeltaMovement(Vec3.ZERO);
        setYRot(yRotO);
        setYHeadRot(yHeadRotO);

        if (deathTime >= getMaxDeathTime()) remove(RemovalReason.KILLED); // actually delete entity after the time is up

        deathTime++;
    }
}