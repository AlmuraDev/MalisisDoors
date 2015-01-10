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

package net.malisis.doors.door.block;

import net.malisis.core.block.BoundingBoxType;
import net.malisis.core.util.MultiBlock;
import net.malisis.core.util.TileEntityUtils;
import net.malisis.doors.MalisisDoors;
import net.malisis.doors.door.tileentity.RustyHatchTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author Ordinastie
 *
 */
public class RustyHatch extends Block implements ITileEntityProvider
{
	private IIcon handleIcon;

	public static int renderId;

	public RustyHatch()
	{
		super(Material.iron);
		setHardness(3.0F);
		setResistance(10000);
		setStepSound(soundTypeMetal);
		setBlockName("rustyHatch");
		setCreativeTab(MalisisDoors.tab);
	}

	@Override
	public void registerBlockIcons(IIconRegister register)
	{
		blockIcon = register.registerIcon(MalisisDoors.modid + ":rusty_hatch");
		handleIcon = register.registerIcon(MalisisDoors.modid + ":rusty_hatch_handle");
	}

	public IIcon getHandleIcon()
	{
		return handleIcon;
	}

	@Override
	public String getItemIconName()
	{
		return MalisisDoors.modid + ":rusty_hatch_item";
	}

	@Override
	public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side)
	{
		if (side == 0 || side == 1)
			return false;

		ForgeDirection dir = ForgeDirection.getOrientation(side).getOpposite();
		return world.getBlock(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ).isSideSolid(world, x, y, z, dir);
	}

	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata)
	{
		return (side - 2) | (hitY > 0.5F ? Door.FLAG_TOPBLOCK : 0);
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack itemStack)
	{
		int metadata = world.getBlockMetadata(x, y, z);
		ForgeDirection side = ForgeDirection.getOrientation((metadata & 3) + 2);
		AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(0, 0, 0, 2, 3, 2);
		if ((metadata & Door.FLAG_TOPBLOCK) == 0)
			aabb.offset(0, -2, 0);
		MultiBlock mb = new MultiBlock(world, x, y, z);
		mb.setDirection(side);
		mb.setBounds(aabb);
		if (!mb.placeBlocks())
			itemStack.stackSize++;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
	{
		if (world.isRemote)
			return true;

		RustyHatchTileEntity te = TileEntityUtils.getTileEntity(RustyHatchTileEntity.class, world, x, y, z);
		if (te == null)
			return true;

		te.openOrCloseDoor();
		return true;
	}

	@Override
	public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z)
	{
		MultiBlock.destroy(world, x, y, z);
		world.setBlockToAir(x, y, z);
		return true;
	}

	protected AxisAlignedBB setBlockBounds(AxisAlignedBB aabb)
	{
		if (aabb == null)
			return null;
		setBlockBounds((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ);
		return aabb;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z)
	{
		RustyHatchTileEntity te = TileEntityUtils.getTileEntity(RustyHatchTileEntity.class, world, x, y, z);
		if (te == null || te.isMoving() || te.getMovement() == null)
			return;

		AxisAlignedBB aabb = te.getMovement().getBoundingBox(te, te.isTopBlock(x, y, z), BoundingBoxType.RAYTRACE);
		if (aabb == null)
			aabb = AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1, 1);

		aabb.offset(-x, -y, -z);
		setBlockBounds(aabb);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z)
	{
		RustyHatchTileEntity te = TileEntityUtils.getTileEntity(RustyHatchTileEntity.class, world, x, y, z);
		if (te == null || te.isMoving() || te.getMovement() == null)
			return AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);

		AxisAlignedBB aabb = te.getMovement().getBoundingBox(te, te.isTopBlock(x, y, z), BoundingBoxType.SELECTION);
		if (aabb == null)
			return AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);

		return aabb;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)
	{
		RustyHatchTileEntity te = TileEntityUtils.getTileEntity(RustyHatchTileEntity.class, world, x, y, z);
		if (te == null || te.isMoving() || te.getMovement() == null)
			return null;

		AxisAlignedBB aabb = te.getMovement().getBoundingBox(te, te.isTopBlock(x, y, z), BoundingBoxType.COLLISION);
		if (aabb == null)
			return null;

		return setBlockBounds(aabb);
	}

	@Override
	public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_)
	{
		return new RustyHatchTileEntity();
	}

	@Override
	public boolean isLadder(IBlockAccess world, int x, int y, int z, EntityLivingBase entity)
	{
		RustyHatchTileEntity te = MultiBlock.getOriginProvider(RustyHatchTileEntity.class, world, x, y, z);
		if (te == null)
			return false;

		return te.shouldLadder(x, y, z);
	}

	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}

	@Override
	public int getRenderType()
	{
		return renderId;
	}

}
