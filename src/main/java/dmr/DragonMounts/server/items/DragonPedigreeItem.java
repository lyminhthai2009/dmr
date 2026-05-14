package dmr.DragonMounts.server.items;

import dmr.DragonMounts.server.entity.TameableDragonEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class DragonPedigreeItem extends Item {
    public DragonPedigreeItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof TameableDragonEntity dragon && !player.level().isClientSide) {
            
            // TÍNH NĂNG 1: QUẢN TRỊ (SHIFT + CLICK) ĐỔI GIỚI TÍNH
            if (player.isShiftKeyDown()) {
                if (!dragon.isOwnedBy(player) && !player.hasPermissions(2)) {
                    player.sendSystemMessage(Component.literal("§c✖ Chỉ chủ nhân hoặc Admin mới được phép đổi giới tính."));
                    return InteractionResult.SUCCESS;
                }

                boolean newGender = !dragon.isMale();
                dragon.getPersistentData().putBoolean("FixedGenderVIP", newGender);
                
                ServerLevel sl = (ServerLevel) dragon.level();
                sl.playSound(null, dragon.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.2f);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, dragon.getX(), dragon.getY() + 1.0, dragon.getZ(), 15, 0.5, 0.5, 0.5, 0.1);

                player.sendSystemMessage(Component.literal("§d✨ [Phép Màu] §fRồng đã đổi giới tính thành: " + (newGender ? "§bĐực (♂)" : "§dCái (♀)")));
                player.swing(hand);
                return InteractionResult.SUCCESS;
            }

            // TÍNH NĂNG 2: BẢNG GIA PHẢ RPG
            String dragonName = dragon.hasCustomName() ? dragon.getCustomName().getString() : "Rồng Vô Danh";
            String ownerName = (dragon.isTame() && dragon.getOwner() != null) ? dragon.getOwner().getName().getString() : "Hoang Dã";
            String agePhase = dragon.isAdult() ? "§aTrưởng thành" : (dragon.isJuvenile() ? "§eGiai đoạn lớn" : "§bSơ sinh");
            String genderStr = dragon.isMale() ? "§bĐực (♂)" : "§dCái (♀)";
            
            float stability = dragon.getStability();
            String stabRank = stability >= 1.0f ? "§bThuần Chủng" : stability >= 0.7f ? "§aKhỏe Mạnh" : stability >= 0.4f ? "§eCận Huyết Nhẹ" : "§cĐột Biến Trầm Trọng";
            String stabColor = stability >= 0.8f ? "§a" : (stability >= 0.5f ? "§e" : "§c");
            
            var father = dragon.getPersistentData().contains("FatherUUID") ? "§7[Đã Ghi Nhận]" : "§8[Tự Nhiên]";
            var mother = dragon.getPersistentData().contains("MotherUUID") ? "§7[Đã Ghi Nhận]" : "§8[Tự Nhiên]";

            double maxHp = dragon.getAttributeValue(Attributes.MAX_HEALTH);
            double currentHp = dragon.getHealth();
            double damage = dragon.getAttributeValue(Attributes.ATTACK_DAMAGE);
            double speed = dragon.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double armor = dragon.getAttributeValue(Attributes.ARMOR);

            player.sendSystemMessage(Component.literal("§8§m======================================"));
            player.sendSystemMessage(Component.literal("      §6§l📜 HỒ SƠ CHỨNG NHẬN RỒNG 📜"));
            player.sendSystemMessage(Component.literal("§eTên: §f" + dragonName + " §7| §eChủ: §f" + ownerName));
            player.sendSystemMessage(Component.literal("§eHệ Rồng: §a" + dragon.getBreed().getName().getString() + " §7| §eTuổi: " + agePhase));
            
            player.sendSystemMessage(Component.literal("§d⚜ §nDi Truyền§r §d⚜"));
            player.sendSystemMessage(Component.literal(" §7▪ §eGiới tính: " + genderStr + " §7| §eBố/Mẹ: " + father + " §f/ " + mother));
            player.sendSystemMessage(Component.literal(" §7▪ §eGen: " + stabColor + String.format("%.0f", stability * 100) + "% §7(" + stabRank + "§7)"));
            
            player.sendSystemMessage(Component.literal("§c⚔ §nThể Chất§r §c⚔"));
            player.sendSystemMessage(Component.literal(" §7▪ §eHP: §c" + String.format("%.1f", currentHp) + " §7/ §c" + String.format("%.1f", maxHp) + " ❤"));
            player.sendSystemMessage(Component.literal(" §7▪ §eĐam: §4" + String.format("%.1f", damage) + " ⚔ §7| §eGiáp: §9" + String.format("%.0f", armor) + " 🛡"));
            player.sendSystemMessage(Component.literal(" §7▪ §eTốc độ: §f" + String.format("%.3f", speed) + " ⚡"));
            
            if (stability < 0.5f) {
                player.sendSystemMessage(Component.literal("§4⚠ §lCẢNH BÁO:§r §cRồng có dấu hiệu yếu đi do cận huyết!"));
            }
            player.sendSystemMessage(Component.literal("§8§m======================================"));
            
            ServerLevel serverLevel = (ServerLevel) dragon.level();
            serverLevel.playSound(null, dragon.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);
            
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}