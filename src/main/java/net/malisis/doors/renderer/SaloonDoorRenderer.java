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

import net.malisis.core.renderer.animation.Animation;
import net.malisis.core.renderer.model.MalisisModel;
import net.malisis.core.util.TransformBuilder;
import net.malisis.doors.MalisisDoors;
import net.malisis.doors.block.SaloonDoorBlock;
import net.malisis.doors.tileentity.SaloonDoorTileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/**
 * @author Ordinastie
 *
 */
public class SaloonDoorRenderer extends DoorRenderer
{
	public SaloonDoorRenderer()
	{
		super(false);
		registerFor(SaloonDoorTileEntity.class);
	}

	@Override
	protected void initialize()
	{
		ResourceLocation rl = new ResourceLocation(MalisisDoors.modid + ":models/saloon_door.obj");
		model = new MalisisModel(rl);

		initParams();

		gui = new TransformBuilder().translate(.15F, -0.2F, 0).rotate(30, 45, 0).scale(.56F).get();

		ensureBlock(SaloonDoorBlock.class);
	}

	@Override
	protected void renderTileEntity()
	{
		initialize();

		ar.setStartTime(tileEntity.getTimer().getStart());

		setup();

		if (!tileEntity.isHingeLeft())
			model.rotate(180, 0, 1, 0, 0, 0, 0);

		if (tileEntity.getMovement() != null)
		{
			Animation<?>[] anims = tileEntity.getMovement().getAnimations(tileEntity, model, rp);
			ar.animate(anims);
		}

		next(GL11.GL_POLYGON);
		//model.render(this, rp);
		rp.brightness.set(blockState.getPackedLightmapCoords(world, pos));
		model.render(this, rp);
	}

	@Override
	protected void renderItem()
	{
		next(GL11.GL_POLYGON);
		model.render(this, rp);
	}
}
