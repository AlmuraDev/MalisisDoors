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

package net.malisis.doors.renderer;

import java.util.HashSet;
import java.util.Set;

import net.malisis.core.renderer.BaseRenderer;
import net.malisis.core.renderer.RenderParameters;
import net.malisis.core.renderer.animation.AnimationRenderer;
import net.malisis.core.renderer.animation.transformation.ChainedTransformation;
import net.malisis.core.renderer.animation.transformation.ParallelTransformation;
import net.malisis.core.renderer.animation.transformation.Rotation;
import net.malisis.core.renderer.animation.transformation.Transformation;
import net.malisis.core.renderer.animation.transformation.Translation;
import net.malisis.core.renderer.element.Face;
import net.malisis.core.renderer.element.Shape;
import net.malisis.core.renderer.preset.ShapePreset;
import net.malisis.core.util.TileEntityUtils;
import net.malisis.doors.door.Door;
import net.malisis.doors.door.DoorState;
import net.malisis.doors.entity.GarageDoorTileEntity;
import net.minecraft.client.renderer.DestroyBlockProgress;

/**
 * @author Ordinastie
 * 
 */
public class GarageDoorRenderer extends BaseRenderer
{
	private GarageDoorTileEntity tileEntity;
	protected int direction;
	protected boolean opened;
	protected boolean reversed;
	protected boolean topBlock;

	protected Shape baseShape;
	protected Shape s;
	protected RenderParameters rp;
	protected AnimationRenderer ar = new AnimationRenderer(this);

	public GarageDoorRenderer()
	{
		rp = new RenderParameters();
		rp.renderAllFaces.set(true);
		rp.calculateAOColor.set(false);
		rp.useBlockBounds.set(false);
		rp.useBlockBrightness.set(false);
		rp.calculateBrightness.set(false);
		rp.interpolateUV.set(false);
	}

	@Override
	public void render()
	{
		if (renderType == TYPE_ITEM_INVENTORY)
		{
			enableBlending();
			s = ShapePreset.Cube().setSize(Door.DOOR_WIDTH, 1, 1);
			s.translate(0.5F - Door.DOOR_WIDTH / 2, 0, 0);
			rp.icon.set(null);
			blockMetadata = Door.FLAG_TOPBLOCK;
			drawShape(s, rp);
			return;
		}

		tileEntity = TileEntityUtils.getTileEntity(GarageDoorTileEntity.class, world, x, y, z);
		if (tileEntity == null || !tileEntity.isTopDoor())
		{
			getBlockDamage = false;
			return;
		}

		getBlockDamage = true;

		direction = tileEntity.getDirection();
		opened = tileEntity.isOpened();
		reversed = tileEntity.isReversed();

		rp.brightness.set(world.getLightBrightnessForSkyBlocks(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, 0));
		rp.icon.set(null);

		enableBlending();
		renderTileEntity();
	}

	protected void renderTileEntity()
	{
		s = ShapePreset.Cube().setSize(Door.DOOR_WIDTH, 1, 1);
		s.rotate(-90 * tileEntity.getDirection(), 0, 1, 0);
		s.translate(0.5F - Door.DOOR_WIDTH / 2, 0, 0);

		int t = GarageDoorTileEntity.maxOpenTime;
		//set the start timer
		ar.setStartTime(tileEntity.startTime);

		//create door list from childs + top
		Set<GarageDoorTileEntity> doors = new HashSet<>(tileEntity.getChildDoors());
		doors.add(tileEntity);

		for (GarageDoorTileEntity te : doors)
		{
			blockMetadata = te.blockMetadata;
			y = te.yCoord;
			int delta = tileEntity.yCoord - te.yCoord;
			int delta2 = doors.size() - (delta + 1);

			Transformation verticalAnim = new Translation(0, -delta, 0, 0, 0, 0).forTicks(t * delta, 0);
			//@formatter:off
			Transformation topRotate = new ParallelTransformation(
					new Translation(0, 1, 0).forTicks(t, 0), 
					new Rotation(0, -90).aroundAxis(0, 0, 1).offset(-0.5F, -0.5F, 0).forTicks(t, 0)
			);
			//@formatter:on
			Transformation horizontalAnim = new Translation(0, 0, 0, 0, delta2, 0).forTicks(t * delta2, 0);

			Transformation chained = new ChainedTransformation(verticalAnim, topRotate, horizontalAnim);
			if (tileEntity.getState() == DoorState.CLOSING || tileEntity.getState() == DoorState.CLOSED)
				chained.reversed(true);

			Shape tempShape = new Shape(s);
			ar.animate(tempShape, chained);
			drawShape(tempShape, rp);
		}
	}

	@Override
	public void renderDestroyProgress()
	{
		rp.icon.set(damagedIcons[destroyBlockProgress.getPartialBlockDamage()]);
		int y = this.y - destroyBlockProgress.getPartialBlockY();
		s.translate(0.005F, -y, 0);
		s.scale(1.011F);
		s.applyMatrix();
		Shape tempShape = new Shape(new Face[] { s.getFaces()[2], s.getFaces()[3] });
		drawShape(tempShape, rp);
	}

	@Override
	protected boolean isCurrentBlockDestroyProgress(DestroyBlockProgress dbp)
	{
		if (dbp.getPartialBlockX() == x && dbp.getPartialBlockY() == y && dbp.getPartialBlockZ() == z)
			return true;

		for (GarageDoorTileEntity te : tileEntity.getChildDoors())
		{
			if (dbp.getPartialBlockX() == te.xCoord && dbp.getPartialBlockY() == te.yCoord && dbp.getPartialBlockZ() == te.zCoord)
				return true;
		}
		return false;
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId)
	{
		return true;
	}
}
