package com.example.tudursguns.block;

import com.example.tudursguns.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Soldier post: sends out and supplies one soldier - see SoldierPostBlockEntity. */
public class SoldierPostBlock extends BaseEntityBlock {

	/** Forge 1.20.1: runs the block entity's removal cleanup (1.21.5 preRemoveSideEffects) and drops its inventory when the block is broken or replaced. */
	@Override
	public void onRemove(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock())) {
			com.example.tudursvehiclemod.compat.Compat.preRemove(level, pos, state);
		}
		super.onRemove(state, level, pos, newState, moved);
	}

	public SoldierPostBlock(Properties settings) {
		super(settings);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SoldierPostBlockEntity(pos, state);
	}

	@Override
	public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, net.minecraft.world.InteractionHand tvmHand, BlockHitResult hit) {
		if (!world.isClientSide() && world.getBlockEntity(pos) instanceof SoldierPostBlockEntity post) {
			player.openMenu(post);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
		return world.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.SOLDIER_POST, SoldierPostBlockEntity::tick);
	}
}
