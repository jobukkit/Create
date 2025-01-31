package com.simibubi.create.content.contraptions.components.structureMovement.bearing;

import java.util.List;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.INamedIconOptions;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.block.Block;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class WindmillBearingTileEntity extends MechanicalBearingTileEntity {

	protected ScrollOptionBehaviour<RotationDirection> movementDirection;
	protected float lastGeneratedSpeed;

	public WindmillBearingTileEntity(TileEntityType<? extends MechanicalBearingTileEntity> type) {
		super(type);
	}

	@Override
	public void updateGeneratedRotation() {
		super.updateGeneratedRotation();
		lastGeneratedSpeed = getGeneratedSpeed();
	}
	
	@Override
	public void onSpeedChanged(float prevSpeed) {
		boolean cancelAssembly = assembleNextTick;
		super.onSpeedChanged(prevSpeed);
		assembleNextTick = cancelAssembly;
	}

	private boolean isOutdoors() {
		BlockPos pos = this.getBlockPosition().up(2);
		if (world == null || !world.isSkyVisible(pos))
			return false;
		for (BlockPos wind = new BlockPos(pos.getX(), 180, pos.getZ()); wind.getY() > pos.getY(); wind = wind.down()) {
			if (!world.isAirBlock(wind))
				return false;
		}
		return true;
	}

	@Override
	public float getGeneratedSpeed() {
		if (!running || !isOutdoors())
			return 0;
		if (movedContraption == null)
			return lastGeneratedSpeed;
		int sails = ((BearingContraption) movedContraption.getContraption()).getSailBlocks() / 8;
		return MathHelper.clamp(sails, 1, 16) * getAngleSpeedDirection();
	}

	@Override
	protected boolean isWindmill() {
		return true;
	}

	protected float getAngleSpeedDirection() {
		RotationDirection rotationDirection = RotationDirection.values()[movementDirection.getValue()];
		return (rotationDirection == RotationDirection.CLOCKWISE ? 1 : -1);
	}

	@Override
	public void write(CompoundNBT compound, boolean clientPacket) {
		compound.putFloat("LastGenerated", lastGeneratedSpeed);
		super.write(compound, clientPacket);
	}

	@Override
	protected void read(CompoundNBT compound, boolean clientPacket) {
		lastGeneratedSpeed = compound.getFloat("LastGenerated");
		super.read(compound, clientPacket);
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		behaviours.remove(movementMode);
		movementDirection = new ScrollOptionBehaviour<>(RotationDirection.class,
			Lang.translate("contraptions.windmill.rotation_direction"), this, getMovementModeSlot());
		movementDirection.requiresWrench();
		movementDirection.withCallback($ -> onDirectionChanged());
		behaviours.add(movementDirection);
	}

	private void onDirectionChanged() {
		if (!running)
			return;
		if (!world.isRemote)
			updateGeneratedRotation();
	}

	@Override
	public boolean isWoodenTop() {
		return true;
	}

	static enum RotationDirection implements INamedIconOptions {

		CLOCKWISE(AllIcons.I_REFRESH), COUNTER_CLOCKWISE(AllIcons.I_ROTATE_CCW),

		;

		private String translationKey;
		private AllIcons icon;

		private RotationDirection(AllIcons icon) {
			this.icon = icon;
			translationKey = "generic." + Lang.asId(name());
		}

		@Override
		public AllIcons getIcon() {
			return icon;
		}

		@Override
		public String getTranslationKey() {
			return translationKey;
		}

	}

}
