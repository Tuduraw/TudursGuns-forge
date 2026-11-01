package com.example.tudursguns.item;

import com.example.tudursguns.handheld.AmmoDefinition;
import com.example.tudursguns.handheld.ModDefinitions;
import com.example.tudursguns.registry.ModComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** The one item every ammo definition (magazine, clip, shell box) is; which one comes from its
 * tudursguns:ammo_type component. Weapons naming it reload from it. */
public class AmmoItem extends DefinedItem<AmmoDefinition> {

	public AmmoItem(Properties settings) {
		super(settings, ModDefinitions.AMMO, ModComponents.AMMO_TYPE);
	}

	/** Forge: the stack size comes from the definition. */
	@Override
	public int getMaxStackSize(ItemStack stack) {
		AmmoDefinition def = ModDefinitions.AMMO.getAny(com.example.tudursguns.registry.ModComponents.AMMO_TYPE.get(stack));
		return def != null ? def.maxStack() : 16;
	}

	@Override
	protected void appendDetails(ItemStack stack, AmmoDefinition def, Consumer<Component> textConsumer) {
		textConsumer.accept(Component.translatable("tooltip.tudursguns.ammo_rounds", def.rounds()).withStyle(ChatFormatting.GRAY));
	}
}
