package com.example.tudursguns.compat;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.function.Function;

/** 1.20.5's AbstractContainerMenu.addStandardInventorySlots. */
public final class Menus {
	private Menus() {
	}

	public static void addPlayerSlots(Function<Slot, Slot> addSlot, Inventory inventory, int x, int y) {
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlot.apply(new Slot(inventory, column + row * 9 + 9, x + column * 18, y + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			addSlot.apply(new Slot(inventory, column, x + column * 18, y + 58));
		}
	}
}
