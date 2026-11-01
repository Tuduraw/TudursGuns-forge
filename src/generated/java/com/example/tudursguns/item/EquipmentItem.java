package com.example.tudursguns.item;

import com.example.tudursguns.handheld.EquipmentDefinition;
import com.example.tudursguns.handheld.ModDefinitions;
import com.example.tudursguns.registry.ModComponents;
import com.example.tudursguns.weapon.EquipmentActions;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The one item every piece of support equipment is; which one comes from its tudursguns:equipment
 * component. What it does is in EquipmentDefinition / EquipmentActions; the binoculars' view, the
 * rangefinder, the mine detector's outlines and the laser beam are drawn client-side. */
public class EquipmentItem extends DefinedItem<EquipmentDefinition> {

	private static final int HOLD_TICKS = 72000;

	public EquipmentItem(Properties settings) {
		super(settings, ModDefinitions.EQUIPMENT, ModComponents.EQUIPMENT);
	}

	public static EquipmentDefinition definition(ItemStack stack) {
		return stack.getItem() instanceof EquipmentItem ? ModDefinitions.EQUIPMENT.getAny(com.example.tudursguns.registry.ModComponents.EQUIPMENT.get(stack)) : null;
	}

	@Override
	public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level world, Player user, InteractionHand hand) {
		return new net.minecraft.world.InteractionResultHolder<>(this.tvm$use(world, user, hand), user.getItemInHand(hand));
	}

	private InteractionResult tvm$use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);
		EquipmentDefinition def = definition(stack);
		if (def == null || user.getVehicle() != null) {
			return InteractionResult.PASS;
		}
		if (def.type() == EquipmentDefinition.Type.DETONATOR) {
			if (user instanceof ServerPlayer player) {
				EquipmentActions.detonate(player, stack, hand, def);
			}
			return InteractionResult.SUCCESS;
		}
		if (!def.isHeld()) {
			return InteractionResult.PASS;
		}
		user.startUsingItem(hand);
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		EquipmentDefinition def = definition(stack);
		return def != null && def.type() == EquipmentDefinition.Type.FIRST_AID ? def.useTicks() : HOLD_TICKS;
	}

	@Override
	public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!(user instanceof ServerPlayer player)) {
			return;
		}
		EquipmentDefinition def = definition(stack);
		if (def == null || player.getVehicle() != null) {
			player.releaseUsingItem();
			return;
		}
		int ticksUsed = getUseDuration(stack) - remainingUseTicks;
		InteractionHand hand = player.getUsedItemHand();
		boolean keepGoing = switch (def.type()) {
			case REPAIR_KIT -> EquipmentActions.repairTick(player, stack, hand, def, ticksUsed);
			case DEFUSE_KIT -> EquipmentActions.defuseTick(player, stack, hand, def);
			case LASER_DESIGNATOR -> {
				EquipmentActions.laserTick(player, def);
				yield true;
			}
			default -> true;
		};
		if (!keepGoing) {
			player.releaseUsingItem();
		}
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		EquipmentDefinition def = definition(stack);
		if (def != null && def.type() == EquipmentDefinition.Type.FIRST_AID && user instanceof ServerPlayer player) {
			EquipmentActions.firstAid(player, stack, player.getUsedItemHand(), def);
		}
		return stack;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
		tvm$releaseUsing(stack, world, user, remainingUseTicks);
	}

	private boolean tvm$releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
		if (user instanceof ServerPlayer player) {
			EquipmentActions.clearLaser(player);
			EquipmentActions.stopDefusing(player);
		}
		return false;
	}

	/** Server side: stack size and durability follow the definition (see ArmorEffects.applyComponents
	 * for why this is done here). */
	@Override
	public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level tvmLevel, Entity entity, int tvmSlot, boolean tvmSelected) {
		if (!(tvmLevel instanceof ServerLevel world)) {
			return;
		}
		EquipmentDefinition def = definition(stack);
		if (def != null) {
			applyComponents(stack, def);
		}
	}

	public static void applyComponents(ItemStack stack, EquipmentDefinition def) {
		// Forge 1.20.1: uses and stack size come from getMaxDamage / getMaxStackSize below.
	}

	@Override
	public int getMaxDamage(ItemStack stack) {
		EquipmentDefinition def = definition(stack);
		return def != null ? def.uses() : 0;
	}

	@Override
	public boolean isDamageable(ItemStack stack) {
		return getMaxDamage(stack) > 0;
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		EquipmentDefinition def = definition(stack);
		if (def == null) {
			return 1;
		}
		return def.uses() > 0 ? 1 : def.maxStack();
	}

	@Override
	protected void appendDetails(ItemStack stack, EquipmentDefinition def, Consumer<Component> textConsumer) {
		textConsumer.accept(Component.translatable("tooltip.tudursguns.equipment." + def.type().name().toLowerCase(Locale.ROOT))
				.withStyle(ChatFormatting.GRAY));
		def.movement().appendTooltip(textConsumer);
	}
}
