package dmr.DragonMounts.server.entity.dragon;

import dmr.DragonMounts.config.ServerConfig;
import dmr.DragonMounts.network.packets.DragonAgeSyncPacket;
import dmr.DragonMounts.registry.DragonBreedsRegistry;
import dmr.DragonMounts.registry.ModBlocks;
import dmr.DragonMounts.registry.ModEntities;
import dmr.DragonMounts.server.blocks.DMREggBlock;
import dmr.DragonMounts.server.entity.TameableDragonEntity;
import dmr.DragonMounts.util.BreedingUtils;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

abstract class DragonBreedableComponent extends DragonBreedComponent {

    protected DragonBreedableComponent(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    public boolean canMate(Animal mate) {
        if (mate == this) return false;
        if (!(mate instanceof TameableDragonEntity)) return false;
        if (!canReproduce()) return false;

        TameableDragonEntity dragonMate = (TameableDragonEntity) mate;

        // Chỉ cho phép Đực x Cái
        if (this.getDragon().isMale() == dragonMate.isMale()) return false;

        if (!dragonMate.isTame()) return false;
        if (!dragonMate.canReproduce()) return false;
        return isInLove() && dragonMate.isInLove();
    }

    public void spawnChildFromBreeding(ServerLevel level, Animal animal) {
        if (!(animal instanceof TameableDragonEntity)) return;
        TameableDragonEntity mate = (TameableDragonEntity) animal;

        float stability = (this.getDragon().getStability() + mate.getStability()) / 2.0f;
        
        // Trích xuất an toàn tránh Null (Rồng hoang dã)
        UUID myFather = getPersistentData().contains("FatherUUID") ? getPersistentData().getUUID("FatherUUID") : null;
        UUID mateFather = mate.getPersistentData().contains("FatherUUID") ? mate.getPersistentData().getUUID("FatherUUID") : null;
        UUID myMother = getPersistentData().contains("MotherUUID") ? getPersistentData().getUUID("MotherUUID") : null;
        UUID mateMother = mate.getPersistentData().contains("MotherUUID") ? mate.getPersistentData().getUUID("MotherUUID") : null;

        if ((myFather != null && myFather.equals(mateFather)) || (myMother != null && myMother.equals(mateMother))) {
            stability *= 0.5f; // Phạt Cận Huyết
        }

        var state = ModBlocks.DRAGON_EGG_BLOCK.get().defaultBlockState().setValue(DMREggBlock.HATCHING, true);
        var eggOutcomes = DragonBreedsRegistry.getEggOutcomes(getDragon(), level, mate);
        var offSpringBreed = eggOutcomes.get(getRandom().nextInt(eggOutcomes.size()));
        var variant = !offSpringBreed.getVariants().isEmpty() ? offSpringBreed.getVariants().get(getRandom().nextInt(offSpringBreed.getVariants().size())) : null;
        
        var egg = DMREggBlock.place(level, blockPosition(), state, offSpringBreed, variant);

        if (egg != null) {
            // Chuẩn hóa NBT: Đực là Bố, Cái là Mẹ
            UUID realFather = this.getDragon().isMale() ? this.getDragon().getDragonUUID() : mate.getDragonUUID();
            UUID realMother = !this.getDragon().isMale() ? this.getDragon().getDragonUUID() : mate.getDragonUUID();
            
            egg.getPersistentData().putUUID("FatherUUID", realFather);
            egg.getPersistentData().putUUID("MotherUUID", realMother);
            egg.getPersistentData().putFloat("StabilityVIP", stability);
        }

        if (ServerConfig.ENABLE_RANDOM_STATS) getDragon().setEggBreedAttributes(mate, () -> egg);

        if (hasCustomName() && animal.hasCustomName()) {
            egg.setCustomName(Component.literal(BreedingUtils.generateCustomName(getDragon(), animal)));
        }

        getDragon().updateOwnerData();
    }

    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob mob) { return ModEntities.DRAGON_ENTITY.get().create(level); }
    public boolean canReproduce() { return isTame(); }
    public boolean isFoodItem(ItemStack stack) { var food = stack.getItem().getFoodProperties(stack, this); return food != null && stack.is(ItemTags.MEAT); }
    public void setAge(int pAge) { super.setAge(pAge); if (!level().isClientSide) PacketDistributor.sendToPlayersTrackingEntity(this, new DragonAgeSyncPacket(getId(), pAge)); updateAgeProperties(); }
    public void updateAgeProperties() { refreshDimensions(); getDragon().updateAgeAttributes(); }
    public float getAgeProgress() { float growth = -(getDragon().getBreed().getGrowthTime() * 20); float min = Math.min(getAge(), 0) * 20; float ageProgress = 1 - (min / growth); return Mth.clamp(ageProgress, 0, 1); }
    public boolean isAdult() { return getAgeProgress() >= 1f; }
    @Override public boolean isBaby() { return !isAdult(); }
    public boolean isJuvenile() { return getAgeProgress() >= 0.5f && getAgeProgress() < 1f; }
    public boolean isHatchling() { return getAgeProgress() < 0.5f; }
    @Override public void setBaby(boolean baby) { setAge(baby ? -getDragon().getBreed().getGrowthTime() : 0); updateAgeProperties(); }
    public float getScale() { var scale = getBreed() != null ? getBreed().getSizeModifier() : 1; return scale * (isBaby() ? 0.5f : 1f); }
    public boolean isFood(ItemStack stack) { var list = getBreed().getBreedingItems(); return (!stack.isEmpty() && (list != null && !list.isEmpty() ? list.contains(stack.getItem()) : stack.is(ItemTags.FISHES))); }
    @Override public void setInLove(@Nullable Player player) { super.setInLove(player); getDragon().stopSitting(); getDragon().setWanderTarget(Optional.of(GlobalPos.of(level.dimension(), blockPosition()))); }
}