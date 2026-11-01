package com.example.tudursguns.compat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1.20.5+ ItemStack helpers in their 1.20.1 form. */
public final class Items {
	private Items() {
	}

	/** stack.hurtAndBreak(amount, entity, hand): wear it, and break it (with the break effect) at zero. */
	public static void hurt(ItemStack stack, int amount, LivingEntity entity, InteractionHand hand) {
		stack.hurtAndBreak(amount, entity, e -> e.broadcastBreakEvent(hand));
	}

	public static void hurt(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
		stack.hurtAndBreak(amount, entity, e -> e.broadcastBreakEvent(slot));
	}

	/** stack.consume(count, entity): used up, except in creative. */
	public static void consume(ItemStack stack, int count, LivingEntity entity) {
		if (!(entity instanceof Player player && player.getAbilities().instabuild)) {
			stack.shrink(count);
		}
	}
}
