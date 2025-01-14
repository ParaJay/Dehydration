package net.dehydration.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.dehydration.access.ThirstManagerAccess;
import net.dehydration.init.ConfigInit;
import net.dehydration.thirst.ThirstManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements ThirstManagerAccess {
    private ThirstManager thirstManager = new ThirstManager();

    @Override
    public ThirstManager getThirstManager(PlayerEntity player) {
        return this.thirstManager;
    }

    @Shadow
    protected HungerManager hungerManager = new HungerManager();
    @Shadow
    private int sleepTimer;

    public PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "Lnet/minecraft/entity/player/PlayerEntity;tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/HungerManager;update(Lnet/minecraft/entity/player/PlayerEntity;)V", shift = Shift.AFTER))
    private void tickMixin(CallbackInfo info) {
        if (!ConfigInit.CONFIG.excluded_names.contains(this.getName().asString())) {
            this.thirstManager.update((PlayerEntity) (Object) this);
        }
    }

    @Inject(method = "Lnet/minecraft/entity/player/PlayerEntity;tickMovement()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;updateItems()V", shift = Shift.BEFORE))
    private void tickMovementMixin(CallbackInfo info) {
        if (this.world.getDifficulty() == Difficulty.PEACEFUL && this.world.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION)
                && !ConfigInit.CONFIG.excluded_names.contains(this.getName().asString())) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            this.thirstManager.update(player);
            if (this.thirstManager.isNotFull() && this.age % 10 == 0) {
                this.thirstManager.setThirstLevel(this.thirstManager.getThirstLevel() + 1);
            }
        }
    }

    @Inject(method = "readCustomDataFromNbt", at = @At(value = "TAIL"))
    private void readCustomDataFromTagMixin(NbtCompound tag, CallbackInfo info) {
        this.thirstManager.readNbt(tag);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At(value = "TAIL"))
    private void writeCustomDataToTagMixin(NbtCompound tag, CallbackInfo info) {
        this.thirstManager.writeNbt(tag);
    }

    @Inject(method = "Lnet/minecraft/entity/player/PlayerEntity;addExhaustion(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/HungerManager;addExhaustion(F)V", shift = Shift.AFTER))
    private void addExhaustionMixin(float exhaustion, CallbackInfo info) {
        if (!ConfigInit.CONFIG.excluded_names.contains(this.getName().asString())) {
            if (ConfigInit.CONFIG.harder_nether && this.world.getRegistryKey() == World.NETHER) {
                exhaustion *= ConfigInit.CONFIG.nether_factor;
            }
            this.thirstManager.addDehydration(exhaustion / ConfigInit.CONFIG.hydrating_factor);
        }
    }

    @Inject(method = "Lnet/minecraft/entity/player/PlayerEntity;wakeUp(ZZ)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;sleepTimer:I"))
    private void wakeUpMixin(boolean bl, boolean updateSleepingPlayers, CallbackInfo info) {
        if (!this.world.isClient && !ConfigInit.CONFIG.excluded_names.contains(this.getName().asString()) && this.sleepTimer >= 100) {
            int thirstLevel = this.thirstManager.getThirstLevel();
            int hungerLevel = this.hungerManager.getFoodLevel();
            int thirstConsumption = ConfigInit.CONFIG.sleep_thirst_consumption;
            int hungerConsumption = ConfigInit.CONFIG.sleep_hunger_consumption;
            this.thirstManager.setThirstLevel(thirstLevel >= thirstConsumption ? thirstLevel - thirstConsumption : 0);
            this.hungerManager.setFoodLevel(hungerLevel >= hungerConsumption ? hungerLevel - hungerConsumption : 0);
        }

    }

}
