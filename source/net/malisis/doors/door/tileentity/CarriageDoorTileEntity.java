/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Ordinastie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.malisis.doors.door.tileentity;

import net.malisis.core.block.IBlockDirectional;
import net.malisis.core.util.MBlockState;
import net.malisis.core.util.TileEntityUtils;
import net.malisis.core.util.chunkcollision.ChunkCollision;
import net.malisis.doors.door.DoorDescriptor;
import net.malisis.doors.door.DoorRegistry;
import net.malisis.doors.door.DoorState;
import net.malisis.doors.door.movement.CarriageDoorMovement;
import net.malisis.doors.door.sound.CarriageDoorSound;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

/**
 * @author Ordinastie
 *
 */
public class CarriageDoorTileEntity extends DoorTileEntity
{
	public CarriageDoorTileEntity()
	{
		DoorDescriptor descriptor = new DoorDescriptor();
		descriptor.setMovement(DoorRegistry.getMovement(CarriageDoorMovement.class));
		descriptor.setSound(DoorRegistry.getSound(CarriageDoorSound.class));
		descriptor.setDoubleDoor(false);
		descriptor.setOpeningTime(20);
		setDescriptor(descriptor);
	}

	@Override
	public EnumFacing getDirection()
	{
		return IBlockDirectional.getDirection(worldObj, pos);
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
		return false;
	}

	@Override
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

	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return TileEntityUtils.getRenderingBounds(this);
	}
}
