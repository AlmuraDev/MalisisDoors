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

package net.malisis.doors.entity;

import java.util.Set;

import net.malisis.core.block.IBlockDirectional;
import net.malisis.core.util.AABBUtils;
import net.malisis.core.util.TileEntityUtils;
import net.malisis.doors.block.GarageDoor;
import net.malisis.doors.door.DoorState;
import net.malisis.doors.door.tileentity.DoorTileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;

import com.google.common.collect.Sets;

/**
 * @author Ordinastie
 *
 */
public class GarageDoorTileEntity extends DoorTileEntity
{
	public static final int maxOpenTime = 10;

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

	public boolean isTop()
	{
		return isTop(pos);
	}

	public boolean isTop(BlockPos pos)
	{
		return getTopDoor() == this;
	}

	@Override
	public int getOpeningTime()
	{
		return getTopDoor().getDoors().size() * maxOpenTime;
	}

	public GarageDoorTileEntity getTopDoor()
	{
		GarageDoorTileEntity te = getDoor(EnumFacing.UP);
		return te != null ? te.getTopDoor() : this;
	}

	@Override
	public boolean isPowered()
	{
		return (boolean) worldObj.getBlockState(pos).getValue(GarageDoor.POWERED);
	}

	public GarageDoorTileEntity getDoor(EnumFacing dir)
	{

		GarageDoorTileEntity te = TileEntityUtils.getTileEntity(GarageDoorTileEntity.class, worldObj, pos.offset(dir));
		if (te == null || te.getDirection() != getDirection())
			return null;

		if (dir.getAxis() != Axis.Y)
			return te;

		return te.isPowered() == te.isPowered() && te.getState() == getState() ? te : null;
	}

	public Set<GarageDoorTileEntity> getDoors()
	{
		Set<GarageDoorTileEntity> childDoors = Sets.newHashSet();
		GarageDoorTileEntity te = this;
		while (te != null)
		{
			childDoors.add(te);
			te = te.getDoor(EnumFacing.DOWN);
		}
		return childDoors;
	}

	@Override
	public void setPowered(boolean powered)
	{
		if (!isTop())
		{
			getTopDoor().setPowered(powered);
			return;
		}

		if ((boolean) worldObj.getBlockState(pos).getValue(GarageDoor.POWERED) == powered && !isMoving())
			return;

		if ((state == DoorState.OPENING && powered) || (state == DoorState.CLOSING && !powered))
			return;

		DoorState newState = powered ? DoorState.OPENING : DoorState.CLOSING;
		for (GarageDoorTileEntity te : getDoors())
		{
			te.setDoorState(newState);
			worldObj.setBlockState(te.getPos(), worldObj.getBlockState(te.getPos()).withProperty(GarageDoor.POWERED, powered));
		}

		EnumFacing dir = getDirection().rotateY();
		GarageDoorTileEntity te = getDoor(dir);
		if (te != null)
			te.setPowered(powered);

		te = getDoor(dir.getOpposite());
		if (te != null)
			te.setPowered(powered);
	}

	@Override
	public void playSound()
	{}

	@Override
	public void update()
	{
		if (state == DoorState.CLOSED || state == DoorState.OPENED)
			return;

		if (timer.elapsedTick() > getOpeningTime())
			setDoorState(state == DoorState.CLOSING ? DoorState.CLOSED : DoorState.OPENED);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		Set<GarageDoorTileEntity> childDoors = getDoors();
		return AABBUtils.identity(pos).expand(childDoors.size(), childDoors.size(), childDoors.size());
	}

}
