package net.malisis.doors.bigdoors;

import com.google.common.base.MoreObjects;

import net.malisis.core.block.IBoundingBox;
import net.malisis.core.block.component.DirectionalComponent;
import net.malisis.core.util.MBlockState;
import net.malisis.core.util.chunkcollision.ChunkCollision;
import net.malisis.core.util.syncer.Sync;
import net.malisis.core.util.syncer.Syncable;
import net.malisis.doors.DoorDescriptor;
import net.malisis.doors.DoorRegistry;
import net.malisis.doors.DoorState;
import net.malisis.doors.movement.Door3x3Movement;
import net.malisis.doors.sound.RustyDoorSound;
import net.malisis.doors.sound.WoodenDoorSound;
import net.malisis.doors.tileentity.DoorTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

@Syncable("TileEntity")
public class Door3x3Tile extends DoorTileEntity implements BigDoorSettings.IConfigurable
{
	private IBlockState frameState;
	/** Whether the settings were set by the door factory (see {@link BigDoorSettings}). */
	private boolean customSettings;

	public Door3x3Tile()
	{
		DoorDescriptor descriptor = new DoorDescriptor();
		descriptor.setMovement(DoorRegistry.getMovement(Door3x3Movement.class));
		descriptor.setSound(DoorRegistry.getSound(WoodenDoorSound.class));
		descriptor.setDoubleDoor(false);
		descriptor.setOpeningTime(15);
		setDescriptor(descriptor);

		frameState = Blocks.QUARTZ_BLOCK.getDefaultState();
	}

	public Door3x3Tile(boolean metal)
	{
		this();
		if (metal)
			getDescriptor().setSound(DoorRegistry.getSound(RustyDoorSound.class));
	}

	public IBlockState getFrameState()
	{
		return frameState;
	}

	public void setFrameState(IBlockState state)
	{
		if (state != null)
			frameState = state;
	}

	@Override
	public boolean hasCustomSettings()
	{
		return customSettings;
	}

	@Override
	public void setCustomSettings(boolean custom)
	{
		customSettings = custom;
	}

	//the whole 3x3 door, not a single door's area
	@Override
	protected boolean hasPlayer()
	{
		return BigDoorSettings.hasPlayerNear(this, getRenderBoundingBox());
	}

	@Override
	public EnumFacing getDirection()
	{
		return DirectionalComponent.getDirection(getWorld(), pos);
	}

	@Override
	public IBlockState getBlockState()
	{
		return null;
	}

	@Override
	public boolean isOpened()
	{
		return state == DoorState.OPENED;
	}

	@Override
	public boolean isTopBlock(BlockPos pos)
	{
		return false;
	}

	@Override
	public boolean isHingeLeft()
	{
		return true;
	}

	@Override
	public boolean isPowered()
	{
		return getWorld().getRedstonePowerFromNeighbors(getPos()) > 0;
	}

	@Override
	@Sync("state")
	public void setDoorState(DoorState newState)
	{
		boolean moving = this.moving;
		MBlockState state = null;

		if (getWorld() != null)
		{
			state = new MBlockState(pos, getBlockType());
			ChunkCollision.get().updateBlocks(getWorld(), state);
		}

		super.setDoorState(newState);
		if (getWorld() != null && moving && !this.moving)
			ChunkCollision.get().replaceBlocks(getWorld(), state);
	}

	public ItemStack getDroppedItemStack()
	{
		ItemStack stack = new ItemStack(this.getBlockType());
		NBTTagCompound nbt = new NBTTagCompound();
		MBlockState.toNBT(nbt, this.frameState);
		stack.setTagCompound(nbt);
		BigDoorSettings.writeToDrop(stack, this);
		return stack;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag)
	{
		super.readFromNBT(tag);
		frameState = MoreObjects.firstNonNull(MBlockState.fromNBT(tag), Blocks.QUARTZ_BLOCK.getDefaultState());
		customSettings = tag.getBoolean("customSettings");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag)
	{
		super.writeToNBT(tag);
		MBlockState.toNBT(tag, frameState);
		tag.setBoolean("customSettings", customSettings);

		return tag;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return IBoundingBox.getRenderingBounds(getWorld(), pos);
	}
}
