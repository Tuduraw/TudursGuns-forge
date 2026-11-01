package com.example.tudursguns.item;

import com.example.tudursguns.handheld.DefinitionSet;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import com.example.tudursvehiclemod.compat.mc.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** An item type whose stacks each name a definition through a component (see ModItems): the stack
 * is named after its definition, and its tooltip describes it. */
public abstract class DefinedItem<T> extends Item {

	private final DefinitionSet<T> definitions;
	private final DataComponentType<ResourceLocation> component;

	protected DefinedItem(Properties settings, DefinitionSet<T> definitions, DataComponentType<ResourceLocation> component) {
		super(settings);
		this.definitions = definitions;
		this.component = component;
	}

	@Override
	public Component getName(ItemStack stack) {
		String name = this.definitions.name(this.component.get(stack));
		return name != null ? Component.literal(name) : super.getName(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level tvmLevel, java.util.List<net.minecraft.network.chat.Component> tvmTooltip, TooltipFlag type) {
		java.util.function.Consumer<net.minecraft.network.chat.Component> textConsumer = tvmTooltip::add;
		ResourceLocation id = this.component.get(stack);
		T def = this.definitions.getAny(id);
		if (def == null) {
			return;
		}
		appendDetails(stack, def, textConsumer);
		if (type.isAdvanced()) {
			textConsumer.accept(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	/** Sets a component only when it differs - item components filled in from a definition are
	 * re-applied every inventory tick, and an unchanged stack shouldn't be marked dirty. */
	public static <C> void setIfChanged(ItemStack stack, DataComponentType<C> type, C value) {
		if (!Objects.equals(type.get(stack), value)) {
			type.set(stack, value);
		}
	}

	/** Forge 1.20.1 stand-in for the components' ignoreSwapAnimation: the held item is only re-equipped
	 * (lowered and raised) when it becomes a different definition, not when its rounds, reload or mode change. */
	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged || !ItemStack.isSameItem(oldStack, newStack)
				|| !Objects.equals(this.component.get(oldStack), this.component.get(newStack));
	}

	/** The tooltip lines describing the definition. */
	protected abstract void appendDetails(ItemStack stack, T def, Consumer<Component> tooltip);
}
