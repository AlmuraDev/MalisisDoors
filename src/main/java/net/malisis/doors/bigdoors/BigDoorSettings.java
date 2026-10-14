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

package net.malisis.doors.bigdoors;

import net.malisis.doors.DoorDescriptor;
import net.malisis.doors.DoorDescriptor.RedstoneBehavior;
import net.malisis.doors.DoorRegistry;
import net.malisis.doors.block.BigDoor;
import net.malisis.doors.tileentity.DoorTileEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * The door factory settings of the big doors ({@link BigDoor} and {@link Door3x3}): opening time, auto close time, redstone behavior,
 * proximity detection and sound. Movement and double door don't apply, a big door has its own movement and is already a double door.<br>
 * They're kept in their own compound of the item NBT, because the big door items already store their frame block in the root
 * ({@code block}/{@code metadata}), which DoorDescriptor.writeNBT() would overwrite.
 */
public class BigDoorSettings
{
	/** The item NBT compound holding the settings. */
	public static final String NBT_KEY = "doorSettings";

	/**
	 * A big door tile entity, which remembers whether its settings were set by the door factory.
	 */
	public interface IConfigurable
	{
		public boolean hasCustomSettings();

		public void setCustomSettings(boolean custom);
	}

	/**
	 * Checks if the item is a big door.
	 *
	 * @param itemStack the item stack
	 * @return true, if is a big door
	 */
	public static boolean isBigDoor(ItemStack itemStack)
	{
		if (itemStack.isEmpty())
			return false;
		Block block = Block.getBlockFromItem(itemStack.getItem());
		return block instanceof BigDoor || block instanceof Door3x3;
	}

	/**
	 * Writes the settings of the descriptor into the big door item.
	 *
	 * @param itemStack the big door item
	 * @param desc the descriptor
	 */
	public static void writeToItem(ItemStack itemStack, DoorDescriptor desc)
	{
		NBTTagCompound settings = new NBTTagCompound();
		settings.setInteger("openingTime", desc.getOpeningTime());
		settings.setInteger("autoCloseTime", desc.getAutoCloseTime());
		settings.setInteger("redstoneBehavior", desc.getRedstoneBehavior().ordinal());
		settings.setBoolean("proximityDetection", desc.hasProximityDetection());
		if (desc.getSound() != null)
			settings.setString("sound", DoorRegistry.getId(desc.getSound()));

		NBTTagCompound nbt = itemStack.getTagCompound() != null ? itemStack.getTagCompound() : new NBTTagCompound();
		nbt.setTag(NBT_KEY, settings);
		itemStack.setTagCompound(nbt);
	}

	/**
	 * Applies the settings of the big door item (if it has any) to the placed door.
	 *
	 * @param itemStack the big door item
	 * @param te the placed door
	 */
	public static void applyFromItem(ItemStack itemStack, DoorTileEntity te)
	{
		if (itemStack.getTagCompound() == null || !itemStack.getTagCompound().hasKey(NBT_KEY))
			return;

		NBTTagCompound settings = itemStack.getTagCompound().getCompoundTag(NBT_KEY);
		DoorDescriptor desc = te.getDescriptor();
		if (settings.getInteger("openingTime") > 0)
			desc.setOpeningTime(settings.getInteger("openingTime"));
		desc.setAutoCloseTime(Math.max(0, settings.getInteger("autoCloseTime")));
		int redstone = settings.getInteger("redstoneBehavior");
		if (redstone >= 0 && redstone < RedstoneBehavior.values().length)
			desc.setRedstoneBehavior(RedstoneBehavior.values()[redstone]);
		desc.setProximityDetection(settings.getBoolean("proximityDetection"));
		if (settings.hasKey("sound") && DoorRegistry.getSound(settings.getString("sound")) != null)
			desc.setSound(DoorRegistry.getSound(settings.getString("sound")));

		if (te instanceof IConfigurable)
			((IConfigurable) te).setCustomSettings(true);
		te.markDirty();
	}

	/**
	 * Writes the settings of the door into its dropped item, if they were set by the door factory (so unchanged doors still stack with
	 * new ones).
	 *
	 * @param itemStack the dropped item
	 * @param te the door
	 */
	public static void writeToDrop(ItemStack itemStack, DoorTileEntity te)
	{
		if (te instanceof IConfigurable && ((IConfigurable) te).hasCustomSettings())
			writeToItem(itemStack, te.getDescriptor());
	}

	/**
	 * Whether the door can be opened or closed by hand, according to its redstone behavior.
	 *
	 * @param te the door
	 * @return true, if the player can use it
	 */
	public static boolean canUseByHand(DoorTileEntity te)
	{
		RedstoneBehavior behavior = te.getDescriptor().getRedstoneBehavior();
		if (behavior == RedstoneBehavior.REDSTONE_ONLY)
			return false;
		//locked while powered (read from the world: with this behavior, redstone doesn't update the door's powered state)
		return behavior != RedstoneBehavior.REDSTONE_LOCK || !te.getWorld().isBlockPowered(te.getPos());
	}

	/**
	 * Whether the door opens and closes with redstone, according to its redstone behavior.
	 *
	 * @param te the door
	 * @return true, if redstone opens and closes it
	 */
	public static boolean reactsToRedstone(DoorTileEntity te)
	{
		RedstoneBehavior behavior = te.getDescriptor().getRedstoneBehavior();
		return behavior != RedstoneBehavior.HAND_ONLY && behavior != RedstoneBehavior.REDSTONE_LOCK;
	}

	/**
	 * Whether a player is near the door, for proximity detection. The area is the door's bounds, widened by 1.5 blocks in front and behind.
	 *
	 * @param te the door
	 * @param bounds the door's bounds in the world
	 * @return true, if a player is near
	 */
	public static boolean hasPlayerNear(DoorTileEntity te, AxisAlignedBB bounds)
	{
		return bounds != null && !te.getWorld().getEntitiesWithinAABB(EntityPlayer.class, bounds.grow(1.5D, 0, 1.5D)).isEmpty();
	}
}
