package dmr.DragonMounts.server.entity.dragon;

import dmr.DragonMounts.ModConstants.NBTConstants;
import dmr.DragonMounts.server.entity.DragonAgroState;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;

public abstract class AbstractDragonEntity extends DragonAttributeComponent {

    protected static final EntityDataAccessor<String> uuidDataAccessor =
            SynchedEntityData.defineId(AbstractDragonEntity.class, EntityDataSerializers.STRING);

    @Setter
    @Getter
    protected DragonAgroState agroState = DragonAgroState.NEUTRAL;

    protected AbstractDragonEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        setDragonUUID(UUID.randomUUID());
    }

    // =========================================================
    // HỆ THỐNG DI TRUYỀN & GIỚI TÍNH (VIP SERVER-SIDE)
    // =========================================================

    public boolean isMale() {
        if (getPersistentData().contains("FixedGenderVIP")) {
            return getPersistentData().getBoolean("FixedGenderVIP");
        }
        // Server tự quay số 50/50 an toàn
        boolean randomGender = this.random.nextBoolean();
        getPersistentData().putBoolean("FixedGenderVIP", randomGender);
        return randomGender;
    }

    public float getStability() {
        return getPersistentData().contains("StabilityVIP") ? getPersistentData().getFloat("StabilityVIP") : 1.0f;
    }

    // =========================================================

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!level.isClientSide) {
            getInventory().removeListener(this);
        }
    }

    public boolean isServer() {
        return !level.isClientSide;
    }

    @Override
    public double getTick(Object o) {
        return tickCount;
    }

    @Override
    public int getAge() {
        return age;
    }

    @Override
    public void ageUp(int p_146741_, boolean p_146742_) {
        super.ageUp(p_146741_, p_146742_);
        updateAgeProperties();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("agroState", agroState.name());
        if (getDragonUUID() != null) {
            compound.putString(NBTConstants.DRAGON_UUID, getDragonUUID().toString());
        }

        // Lưu Gen
        compound.putBoolean("FixedGenderVIP", isMale());
        compound.putFloat("StabilityVIP", getStability());
        if (getPersistentData().contains("FatherUUID")) compound.putUUID("FatherUUID", getPersistentData().getUUID("FatherUUID"));
        if (getPersistentData().contains("MotherUUID")) compound.putUUID("MotherUUID", getPersistentData().getUUID("MotherUUID"));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("agroState")) {
            agroState = DragonAgroState.valueOf(compound.getString("agroState"));
        }
        if (compound.contains(NBTConstants.DRAGON_UUID)) {
            setDragonUUID(UUID.fromString(compound.getString(NBTConstants.DRAGON_UUID)));
        }

        // Đọc Gen
        if (compound.contains("FixedGenderVIP")) {
            getPersistentData().putBoolean("FixedGenderVIP", compound.getBoolean("FixedGenderVIP"));
        }
        if (compound.contains("StabilityVIP")) {
            getPersistentData().putFloat("StabilityVIP", compound.getFloat("StabilityVIP"));
        } else {
            getPersistentData().putFloat("StabilityVIP", 1.0f);
        }
        if (compound.contains("FatherUUID")) getPersistentData().putUUID("FatherUUID", compound.getUUID("FatherUUID"));
        if (compound.contains("MotherUUID")) getPersistentData().putUUID("MotherUUID", compound.getUUID("MotherUUID"));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(uuidDataAccessor, "");
    }

    public UUID getDragonUUID() {
        String id = entityData.get(uuidDataAccessor);
        return !id.isBlank() ? UUID.fromString(id) : null;
    }

    public void setDragonUUID(UUID uuid) {
        entityData.set(uuidDataAccessor, uuid.toString());
    }
}