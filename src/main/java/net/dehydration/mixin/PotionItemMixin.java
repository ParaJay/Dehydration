package net.dehydration.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.At;

import net.dehydration.access.ThirstManagerAccess;
import net.dehydration.init.ConfigInit;
import net.dehydration.init.EffectInit;
import net.dehydration.thirst.ThirstManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

@Mixin(PotionItem.class)
public abstract class PotionItemMixin {

    @Inject(method = "finishUsing", at = @At(value = "HEAD"))
    public void finishUsingMixin(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable<ItemStack> info) {
        if (user instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) user;
            Potion potion = PotionUtil.getPotion(stack);
            if (!world.isClient && this.isBadPotion(potion) && world.random.nextFloat() >= ConfigInit.CONFIG.potion_bad_thirst_chance) {
                player.addStatusEffect(new StatusEffectInstance(EffectInit.THIRST, ConfigInit.CONFIG.potion_bad_thirst_duration, 0, false, false, true));
            }
            ThirstManager thirstManager = ((ThirstManagerAccess) player).getThirstManager(player);
            thirstManager.add(ConfigInit.CONFIG.potion_thirst_quench);
        }
    }

    private boolean isBadPotion(Potion potion) {
        if (potion == Potions.WATER || potion == Potions.AWKWARD || potion == Potions.THICK || potion == Potions.HARMING || potion == Potions.LONG_POISON || potion == Potions.LONG_SLOWNESS
                || potion == Potions.LONG_WEAKNESS || potion == Potions.MUNDANE || potion == Potions.POISON || potion == Potions.SLOWNESS || potion == Potions.STRONG_HARMING
                || potion == Potions.STRONG_POISON || potion == Potions.STRONG_SLOWNESS || potion == Potions.WEAKNESS) {
            return true;
        } else
            return false;
    }

    @Inject(method = "appendTooltip", at = @At(value = "TAIL"))
    private void appendTooltipMixin(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context, CallbackInfo info) {
        if (isBadPotion(PotionUtil.getPotion(stack))) {
            tooltip.add(new TranslatableText("item.dehydration.dirty_potion.tooltip").formatted(Formatting.DARK_GREEN));
        }

    }

}
