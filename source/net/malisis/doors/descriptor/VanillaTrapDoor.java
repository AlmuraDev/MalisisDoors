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

package net.malisis.doors.descriptor;

import net.malisis.doors.DoorRegistry;
import net.malisis.doors.TrapDoorDescriptor;
import net.malisis.doors.movement.TrapDoorMovement;
import net.malisis.doors.sound.VanillaDoorSound;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;

/**
 * @author Ordinastie
 *
 */
public class VanillaTrapDoor extends TrapDoorDescriptor
{
	public static enum Type
	{
		WOOD, IRON
	}

	public VanillaTrapDoor(Type type)
	{
		//Block
		setOpeningTime(6);
		setMaterial(type == Type.IRON ? Material.iron : Material.wood);
		setHardness(type == Type.IRON ? 5.0F : 3.0F);
		setSoundType(type == Type.IRON ? Block.soundTypeMetal : Block.soundTypeWood);
		setName(type == Type.IRON ? "iron_trapdoor" : "trapdoor");
		setTextureName("minecraft", type == Type.IRON ? "blocks/iron_trapdoor" : "blocks/trapdoor");

		//te
		setRequireRedstone(type == Type.IRON);
		setMovement(DoorRegistry.getMovement(TrapDoorMovement.class));
		setSound(DoorRegistry.getSound(VanillaDoorSound.class));

		//item
		setTab(CreativeTabs.tabRedstone);
	}
}
