package com.example.tudursguns.item;

import com.example.tudursguns.armor.ArmorEffects;
import com.example.tudursguns.handheld.ArmorDefinition;
import com.example.tudursguns.handheld.ModDefinitions;
import com.example.tudursguns.registry.ModComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** The one item every piece of armor is; which one comes from its tudursguns:armor component. It is
 * worn through vanilla's own equippable component, which (with its attributes, durability and item
 * model) is filled in from the definition - see ArmorEffects.applyComponents. */
public class ArmorItem extends DefinedItem<ArmorDefinition> {

	public ArmorItem(Properties settings) {
		super(settings, ModDefinitions.ARMOR, ModComponents.ARMOR);
	}

	@Override
	public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level tvmLevel, Entity entity, int tvmSlot, boolean tvmSelected) {
		if (!(tvmLevel instanceof ServerLevel world)) {
			return;
		}
		ArmorDefinition def = ModDefinitions.ARMOR.getServer(com.example.tudursguns.registry.ModComponents.ARMOR.get(stack));
		if (def != null) {
			ArmorEffects.applyComponents(stack, def);
		}
	}

	private static ArmorDefinition definition(ItemStack stack) {
		return ModDefinitions.ARMOR.getAny(com.example.tudursguns.registry.ModComponents.ARMOR.get(stack));
	}

	/** Forge: the slot comes from the definition (vanilla armor has it per item). */
	@Override
	public EquipmentSlot getEquipmentSlot(ItemStack stack) {
		ArmorDefinition def = definition(stack);
		return def != null ? def.slot() : null;
	}

	@Override
	public boolean canEquip(ItemStack stack, EquipmentSlot armorType, Entity entity) {
		return getEquipmentSlot(stack) == armorType;
	}

	@Override
	public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
		ArmorDefinition def = definition(stack);
		ResourceLocation id = com.example.tudursguns.registry.ModComponents.ARMOR.get(stack);
		if (def == null || id == null || def.slot() != slot) {
			return super.getAttributeModifiers(slot, stack);
		}
		return ArmorEffects.attributeModifiers(id, def);
	}

	@Override
	public int getMaxDamage(ItemStack stack) {
		ArmorDefinition def = definition(stack);
		return def != null ? def.durability() : 0;
	}

	@Override
	public boolean isDamageable(ItemStack stack) {
		return getMaxDamage(stack) > 0;
	}

	/** Right-click puts it on, swapping with what's worn (1.20.1's Equipable.swapWithEquipmentSlot). */
	@Override
	public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		EquipmentSlot slot = getEquipmentSlot(held);
		if (slot == null) {
			return net.minecraft.world.InteractionResultHolder.pass(held);
		}
		ItemStack worn = player.getItemBySlot(slot);
		if (net.minecraft.world.item.enchantment.EnchantmentHelper.hasBindingCurse(worn) || ItemStack.matches(held, worn)) {
			return net.minecraft.world.InteractionResultHolder.fail(held);
		}
		ItemStack previous = worn.isEmpty() ? held : worn.copyAndClear();
		ItemStack putOn = held.copyAndClear();
		player.setItemSlot(slot, putOn);
		return net.minecraft.world.InteractionResultHolder.sidedSuccess(previous, level.isClientSide());
	}

	@Override
	protected void appendDetails(ItemStack stack, ArmorDefinition def, Consumer<Component> textConsumer) {
		ArmorDefinition.Protection protection = def.protection();
		percent(textConsumer, "ballistic", protection.ballistic());
		percent(textConsumer, "blast", protection.blast());
		percent(textConsumer, "headshot", protection.headshot());
		if (def.effects().nightVision()) {
			textConsumer.accept(Component.translatable("tooltip.tudursguns.armor.night_vision").withStyle(ChatFormatting.BLUE));
		}
		if (def.effects().gasProtection()) {
			textConsumer.accept(Component.translatable("tooltip.tudursguns.armor.gas_protection").withStyle(ChatFormatting.BLUE));
		}
		if (def.effects().detectionMultiplier() < 1f) {
			textConsumer.accept(Component.translatable("tooltip.tudursguns.armor.camouflage",
					Math.round(100f * (1f - def.effects().detectionMultiplier()))).withStyle(ChatFormatting.BLUE));
		}
	}

	private static void percent(Consumer<Component> textConsumer, String key, float value) {
		if (value > 0f) {
			textConsumer.accept(Component.translatable("tooltip.tudursguns.armor." + key, Math.round(value * 100f)).withStyle(ChatFormatting.BLUE));
		}
	}
}
