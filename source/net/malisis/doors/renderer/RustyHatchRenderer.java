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

import net.malisis.core.renderer.BaseRenderer;
import net.malisis.core.renderer.RenderParameters;
import net.malisis.core.renderer.animation.AnimationRenderer;
import net.malisis.core.renderer.element.Face;
import net.malisis.core.renderer.element.Shape;
import net.malisis.core.renderer.model.MalisisModel;
import net.malisis.core.util.MultiBlock;
import net.malisis.doors.MalisisDoors;
import net.malisis.doors.block.RustyHatch;
import net.malisis.doors.entity.RustyHatchTileEntity;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

/**
 * @author Ordinastie
 *
 */
public class RustyHatchRenderer extends BaseRenderer
{
	private ResourceLocation rl;
	private MalisisModel model;
	private Shape frame;
	private Shape hatch;
	private Shape handle;
	private Shape ladder;
	private AnimationRenderer ar = new AnimationRenderer(this);
	private RustyHatchTileEntity tileEntity;

	private boolean topBlock;
	private ForgeDirection direction;

	@Override
	protected void initShapes()
	{
		rl = new ResourceLocation(MalisisDoors.modid, "models/rustyhatch.obj");
		model = MalisisModel.load(rl);
		frame = model.getShape("frame");
		hatch = model.getShape("door");
		handle = model.getShape("handle");
		ladder = model.getShape("ladder");
	}

	@Override
	protected void initParameters()
	{
		rp = new RenderParameters();
		rp.useBlockBounds.set(false);
	}

	@Override
	public void render()
	{
		if (renderType == TYPE_ITEM_INVENTORY)
		{
			renderItem();
			return;
		}

		tileEntity = MultiBlock.getOriginProvider(RustyHatchTileEntity.class, world, x, y, z);
		if (tileEntity == null)
			return;
		topBlock = tileEntity.isTopBlock(x, y, z);
		direction = ForgeDirection.getOrientation(tileEntity.getDirection());

		if (renderType == TYPE_ISBRH_WORLD)
		{
			renderBlock();
		}
		else if (renderType == TYPE_TESR_WORLD)
			renderTileEntity();
	}

	private void renderBlock()
	{
		if (!MultiBlock.isOrigin(world, x, y, z))
		{
			if (!tileEntity.shouldLadder(x, y, z)/* || y == tileEntity.getMultiBlock().getY()*/)
				return;

			ladder.resetState();
			ladder.translate(direction.getOpposite().offsetX, topBlock ? -1 : 0, direction.getOpposite().offsetZ);
			setup(ladder);

			rp.icon.set(((RustyHatch) block).getHandleIcon());
			drawShape(ladder, rp);
		}
		else
		{
			frame.resetState();
			setup(frame);
			for (Face f : frame.getFaces())
			{
				//System.err.println(f.toString());
			}

			rp.icon.set(Blocks.anvil.getIcon(1, 0));
			drawShape(frame, rp);
		}
	}

	private void renderTileEntity()
	{
		if (!MultiBlock.isOrigin(world, x, y, z))
			return;
		tileEntity = (RustyHatchTileEntity) super.tileEntity;

		hatch.resetState();
		handle.resetState();
		setup(hatch);
		setup(handle);

		ar.setStartTime(tileEntity.getStartTime());

		if (tileEntity.getMovement() != null)
		{
			ar.animate(hatch, tileEntity.getMovement().getBottomTransformation(tileEntity));
			ar.animate(handle, tileEntity.getMovement().getBottomTransformation(tileEntity));
			ar.animate(handle, tileEntity.getMovement().getTopTransformation(tileEntity));
		}

		next(GL11.GL_POLYGON);
		rp.icon.reset();
		drawShape(hatch, rp);

		rp.icon.set(((RustyHatch) block).getHandleIcon());
		drawShape(handle, rp);
	}

	private void renderItem()
	{
		bindTexture(TextureMap.locationBlocksTexture);
		handle.resetState();
		handle.scale(1.5F);
		switch (itemRenderType)
		{
			case INVENTORY:
				handle.translate(0.20F, 0, 0.15F);
				handle.rotate(90, 0, 0, 1);
				break;
			case EQUIPPED:
				handle.translate(-0.5F, -.75F, 0);
				handle.rotate(100, 0, 0, 1);
				break;
			case EQUIPPED_FIRST_PERSON:
				handle.translate(0, -0.1F, -0.2F);
				handle.rotate(90, 0, 0, 1);
			default:
				break;
		}

		rp.icon.set(((RustyHatch) MalisisDoors.Blocks.rustyHatch).getHandleIcon());
		drawShape(handle, rp);
	}

	private void setup(Shape s)
	{
		if (topBlock)
			s.translate(0, 1 - 0.125F, 0);

		if (direction == ForgeDirection.SOUTH)
			s.rotate(-90, 0, 1, 0);
		else if (direction == ForgeDirection.NORTH)
			s.rotate(90, 0, 1, 0);
		else if (direction == ForgeDirection.WEST)
			s.rotate(180, 0, 1, 0);
	}

	@Override
	public boolean shouldRender3DInInventory(int modelId)
	{
		return false;
	}

}
