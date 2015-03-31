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

import net.malisis.core.MalisisCore;
import net.malisis.core.block.BoundingBoxType;
import net.malisis.core.util.MultiBlock;
import net.malisis.core.util.chunkblock.ChunkBlockHandler;
import net.malisis.doors.door.DoorDescriptor;
import net.malisis.doors.door.DoorRegistry;
import net.malisis.doors.door.block.CarriageDoor;
import net.malisis.doors.door.block.Door;
import net.malisis.doors.door.movement.CarriageDoorMovement;
import net.malisis.doors.door.sound.CarriageDoorSound;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * @author Ordinastie
 *
 */
public class CarriageDoorTileEntity extends DoorTileEntity
{
	private boolean delete = false;
	private boolean processed = true;
	private ForgeDirection direction = ForgeDirection.NORTH;

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
	public boolean isTopBlock(int x, int y, int z)
	{
		return false;
	}

	@Override
	public boolean isReversed()
	{
		return false;
	}

	@Override
	public boolean isPowered()
	{
		return false;
	}

	@Override
	public void updateEntity()
	{
		if (!processed && getWorld() != null)
		{
			if (delete)
			{
				MalisisCore.log.info("Deleting " + xCoord + "," + yCoord + "," + zCoord);
				getWorld().setBlockToAir(xCoord, yCoord, zCoord);
			}
			else
			{
				MalisisCore.log.info("Adding to chunk : " + xCoord + "," + yCoord + "," + zCoord);
				ChunkBlockHandler.get().updateCoordinates(getWorld().getChunkFromBlockCoords(xCoord, zCoord), xCoord, yCoord, zCoord,
						Blocks.air, getBlockType());
				getWorld().setBlockMetadataWithNotify(xCoord, yCoord, zCoord, Door.dirToInt(direction), 2);
				processed = true;
			}
			return;
		}
		super.updateEntity();
	}

	@Override
	public void readFromNBT(NBTTagCompound tag)
	{
		super.readFromNBT(tag);
		if (tag.hasKey("multiBlock"))
		{
			MultiBlock mb = new MultiBlock(tag);
			delete = !mb.isOrigin(xCoord, yCoord, zCoord);
			direction = mb.getDirection();
			processed = false;
		}

	}

	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return ((CarriageDoor) getBlockType()).getBoundingBox(getWorld(), xCoord, yCoord, zCoord, BoundingBoxType.RENDER)[0].offset(xCoord,
				yCoord, zCoord);
	}

}
