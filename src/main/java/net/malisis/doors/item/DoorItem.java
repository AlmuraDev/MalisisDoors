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

package net.malisis.doors.item;

import java.util.List;

import net.malisis.core.MalisisCore;
import net.malisis.core.block.IRegisterable;
import net.malisis.core.renderer.MalisisRendered;
import net.malisis.core.renderer.icon.IIconProvider;
import net.malisis.core.renderer.icon.IMetaIconProvider;
import net.malisis.doors.DoorDescriptor;
import net.malisis.doors.renderer.DoorRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@MalisisRendered(item = DoorRenderer.class)
public class DoorItem extends ItemDoor implements IMetaIconProvider, IRegisterable
{
	private DoorDescriptor descriptor;
	@SideOnly(Side.CLIENT)
	private IIconProvider iconProvider;

	public DoorItem(DoorDescriptor desc)
	{
		super(desc.getBlock());

		this.descriptor = desc;
		this.maxStackSize = desc.getMaxStackSize();
		setUnlocalizedName(desc.getName());
		//setTextureName(desc.getTextureName());
		setCreativeTab(desc.getTab());
	}

	public DoorItem()
	{
		super(null);
	}

	public DoorDescriptor getDescriptor(ItemStack itemStack)
	{
		return descriptor;
	}

	@Override
	public String getName()
	{
		return descriptor.getName();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void createIconProvider(Object object)
	{}

	@Override
	@SideOnly(Side.CLIENT)
	public IIconProvider getIconProvider()
	{
		if (descriptor == null || !(descriptor.getBlock() instanceof IMetaIconProvider))
			return null;
		return ((IMetaIconProvider) descriptor.getBlock()).getIconProvider();
	}

	@Override
	public boolean onItemUse(ItemStack itemStack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if (side != EnumFacing.UP)
			return false;

		IBlockState state = world.getBlockState(pos);

		if (!state.getBlock().isReplaceable(world, pos))
			pos = pos.up();

		Block block = getDescriptor(itemStack).getBlock();
		if (block == null)
		{
			MalisisCore.log.error("Can't place Door : block is null for " + itemStack);
			return false;
		}

		if (!player.canPlayerEdit(pos, side, itemStack) || !player.canPlayerEdit(pos.up(), side, itemStack))
			return false;

		if (!block.canPlaceBlockAt(world, pos))
			return false;

		placeDoor(world, pos, EnumFacing.fromAngle(player.rotationYaw), block);
		--itemStack.stackSize;
		block.onBlockPlacedBy(world, pos, world.getBlockState(pos), player, itemStack);
		return true;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced)
	{
		if (stack.getTagCompound() == null)
			return;

		tooltip.add(EnumChatFormatting.WHITE
				+ StatCollector.translateToLocal("door_movement." + stack.getTagCompound().getString("movement")));
	}
}
