package com.example.tudursguns.handheld;

import com.example.tudursguns.registry.ModComponents;
import com.example.tudursguns.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A recipe of the gun crafting table, read from data/<namespace>/gun_recipe/<name>.json - in a data
 * pack, or in an addon pack under tudursvehiclemod-addons/ (so a weapon pack brings its own recipes
 * without a data pack in every world).
 *
 * result: what's made; ingredients: what's taken from the player's inventory. Both are ItemRefs -
 * a vanilla/modded item ("item") or tag ("tag", ingredients only), or one of this mod's data-driven
 * things by id ("weapon", "attachment", "ammo", "throwable", "mine", "armor", "equipment") - with a
 * count. category: a heading to group recipes under in the table (optional). */
public record GunRecipeDefinition(ItemRef result, List<ItemRef> ingredients, Optional<String> category) {

	public static final Codec<GunRecipeDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemRef.CODEC.fieldOf("result").forGetter(GunRecipeDefinition::result),
			ItemRef.CODEC.listOf().fieldOf("ingredients").forGetter(GunRecipeDefinition::ingredients),
			Codec.STRING.optionalFieldOf("category").forGetter(GunRecipeDefinition::category)
	).apply(instance, GunRecipeDefinition::new));

	/** One kind of item (exactly one of the id fields set) and how many. */
	public record ItemRef(Optional<ResourceLocation> item, Optional<ResourceLocation> tag, Optional<ResourceLocation> weapon,
			Optional<ResourceLocation> attachment, Optional<ResourceLocation> ammo, Optional<ResourceLocation> throwable, Optional<ResourceLocation> mine,
			Optional<ResourceLocation> armor, Optional<ResourceLocation> equipment, int count) {

		public static final Codec<ItemRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ResourceLocation.CODEC.optionalFieldOf("item").forGetter(ItemRef::item),
				ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(ItemRef::tag),
				ResourceLocation.CODEC.optionalFieldOf("weapon").forGetter(ItemRef::weapon),
				ResourceLocation.CODEC.optionalFieldOf("attachment").forGetter(ItemRef::attachment),
				ResourceLocation.CODEC.optionalFieldOf("ammo").forGetter(ItemRef::ammo),
				ResourceLocation.CODEC.optionalFieldOf("throwable").forGetter(ItemRef::throwable),
				ResourceLocation.CODEC.optionalFieldOf("mine").forGetter(ItemRef::mine),
				ResourceLocation.CODEC.optionalFieldOf("armor").forGetter(ItemRef::armor),
				ResourceLocation.CODEC.optionalFieldOf("equipment").forGetter(ItemRef::equipment),
				Codec.intRange(1, 6400).optionalFieldOf("count", 1).forGetter(ItemRef::count)
		).apply(instance, ItemRef::new));

		public boolean matches(ItemStack stack) {
			if (stack.isEmpty()) {
				return false;
			}
			if (this.item.isPresent()) {
				return stack.is(BuiltInRegistries.ITEM.get(this.item.get()));
			}
			if (this.tag.isPresent()) {
				return stack.is(TagKey.create(Registries.ITEM, this.tag.get()));
			}
			if (this.weapon.isPresent()) {
				return stack.is(ModItems.HANDHELD_WEAPON) && this.weapon.get().equals(com.example.tudursguns.registry.ModComponents.WEAPON.get(stack));
			}
			if (this.attachment.isPresent()) {
				return stack.is(ModItems.ATTACHMENT) && this.attachment.get().equals(com.example.tudursguns.registry.ModComponents.ATTACHMENT.get(stack));
			}
			if (this.ammo.isPresent()) {
				return stack.is(ModItems.AMMO) && this.ammo.get().equals(com.example.tudursguns.registry.ModComponents.AMMO_TYPE.get(stack));
			}
			if (this.throwable.isPresent()) {
				return stack.is(ModItems.THROWABLE) && this.throwable.get().equals(com.example.tudursguns.registry.ModComponents.THROWABLE.get(stack));
			}
			if (this.mine.isPresent()) {
				return stack.is(ModItems.MINE) && this.mine.get().equals(com.example.tudursguns.registry.ModComponents.MINE.get(stack));
			}
			if (this.armor.isPresent()) {
				return stack.is(ModItems.ARMOR) && this.armor.get().equals(com.example.tudursguns.registry.ModComponents.ARMOR.get(stack));
			}
			if (this.equipment.isPresent()) {
				return stack.is(ModItems.EQUIPMENT) && this.equipment.get().equals(com.example.tudursguns.registry.ModComponents.EQUIPMENT.get(stack));
			}
			return false;
		}

		/** Stacks this stands for, for display (a tag: each of its items, cycled through by the screen). */
		public List<ItemStack> displayStacks() {
			List<ItemStack> stacks = new ArrayList<>();
			if (this.tag.isPresent()) {
				for (Holder<Item> entry : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, this.tag.get()))) {
					stacks.add(new ItemStack(entry, this.count));
				}
				return stacks;
			}
			ItemStack stack = createStack();
			if (!stack.isEmpty()) {
				stacks.add(stack);
			}
			return stacks;
		}

		/** The stack this makes (a result) - empty for a tag. A weapon comes with a full magazine when
		 * its weapon file is known on this side. */
		public ItemStack createStack() {
			ItemStack stack = this.item.map(id -> new ItemStack(BuiltInRegistries.ITEM.get(id)))
					.or(() -> this.weapon.map(ModItems::weaponStack))
					.or(() -> this.attachment.map(ModItems::attachmentStack))
					.or(() -> this.ammo.map(ModItems::ammoStack))
					.or(() -> this.throwable.map(ModItems::throwableStack))
					.or(() -> this.mine.map(ModItems::mineStack))
					.or(() -> this.armor.map(ModItems::armorStack))
					.or(() -> this.equipment.map(ModItems::equipmentStack))
					.orElse(ItemStack.EMPTY);
			if (!stack.isEmpty()) {
				stack.setCount(Math.min(this.count, stack.getMaxStackSize()));
			}
			return stack;
		}
	}
}
