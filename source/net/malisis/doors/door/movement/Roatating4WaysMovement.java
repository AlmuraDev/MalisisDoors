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

package net.malisis.doors.door.movement;

import static net.malisis.doors.door.block.Door.*;
import net.malisis.core.block.BoundingBoxType;
import net.malisis.core.renderer.RenderParameters;
import net.malisis.core.renderer.animation.Animation;
import net.malisis.core.renderer.animation.transformation.Rotation;
import net.malisis.core.renderer.animation.transformation.Transformation;
import net.malisis.core.renderer.model.MalisisModel;
import net.malisis.doors.door.DoorState;
import net.malisis.doors.door.tileentity.DoorTileEntity;
import net.minecraft.util.AxisAlignedBB;

/**
 * @author Ordinastie
 *
 */
public class Roatating4WaysMovement implements IDoorMovement
{

	@Override
	public AxisAlignedBB getBoundingBox(DoorTileEntity tileEntity, boolean topBlock, BoundingBoxType type)
	{
		int dir = tileEntity.getDirection();
		boolean opened = tileEntity.isOpened();
		boolean reversed = tileEntity.isReversed();

		float x = 0;
		float y = 0;
		float z = 0;
		float X = 1;
		float Y = 1;
		float Z = 1;

		if (!opened)
		{
			if (dir == DIR_NORTH)
				Z = DOOR_WIDTH;
			else if (dir == DIR_WEST)
				X = DOOR_WIDTH;
			else if (dir == DIR_EAST)
				x = 1 - DOOR_WIDTH;
			else if (dir == DIR_SOUTH)
				z = 1 - DOOR_WIDTH;
		}
		else
		{
			if (topBlock == !reversed)
			{
				if (topBlock)
					y = 1 - DOOR_WIDTH;
				else
					Y = type == BoundingBoxType.SELECTION ? DOOR_WIDTH : 0;
			}
			else
			{
				if ((dir == DIR_NORTH && !reversed) || (dir == DIR_SOUTH && reversed))
					x = 1 - DOOR_WIDTH;
				else if ((dir == DIR_SOUTH && !reversed) || (dir == DIR_NORTH && reversed))
					X = DOOR_WIDTH;
				else if ((dir == DIR_EAST && !reversed) || (dir == DIR_WEST && reversed))
					z = 1 - DOOR_WIDTH;
				else if ((dir == DIR_WEST && !reversed) || (dir == DIR_EAST && reversed))
					Z = DOOR_WIDTH;
			}

		}

		if (type == BoundingBoxType.SELECTION && !opened)
		{
			if (!topBlock)
				Y++;
			else
				y--;
		}

		return AxisAlignedBB.getBoundingBox(x, y, z, X, Y, Z);
	}

	private Transformation getTransformation(DoorTileEntity tileEntity, boolean topBlock)
	{
		float angle = 90;
		float hingeX = 0.5F - DOOR_WIDTH / 2;
		float hingeY = -0.5F + DOOR_WIDTH / 2;
		float hingeZ = -0.5F + DOOR_WIDTH / 2;
		int axisX = 0;
		int axisY = 0;

		if (topBlock)
		{
			angle = -angle;
			hingeY = 1 - hingeY;
		}

		if (tileEntity.isReversed())
			hingeX = -hingeX;

		if (topBlock != tileEntity.isReversed())
		{
			axisX = 1;
		}
		else
		{
			axisY = 1;
		}

		Rotation rotation = new Rotation(angle);
		rotation.aroundAxis(axisX, axisY, 0).offset(hingeX, hingeY, hingeZ);;
		rotation.reversed(tileEntity.getState() == DoorState.CLOSING || tileEntity.getState() == DoorState.CLOSED);
		rotation.forTicks(tileEntity.getDescriptor().getOpeningTime());

		return rotation;
	}

	@Override
	public Animation[] getAnimations(DoorTileEntity tileEntity, MalisisModel model, RenderParameters rp)
	{
		return new Animation[] { new Animation(model.getShape("top"), getTransformation(tileEntity, true)),
				new Animation(model.getShape("bottom"), getTransformation(tileEntity, false)) };
	}

	@Override
	public boolean isSpecial()
	{
		return false;
	}

}
