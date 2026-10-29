package net.malisis.doors.bigdoors;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.malisis.core.MalisisCore;
import net.malisis.core.block.BoundingBoxType;
import net.malisis.core.block.MalisisBlock;
import net.malisis.core.block.component.DirectionalComponent;
import net.malisis.core.renderer.DefaultRenderer;
import net.malisis.core.renderer.MalisisRendered;
import net.malisis.core.renderer.icon.Icon;
import net.malisis.core.renderer.icon.provider.IBlockIconProvider;
import net.malisis.core.util.AABBUtils;
import net.malisis.core.util.MBlockState;
import net.malisis.core.util.TileEntityUtils;
import net.malisis.core.util.chunkcollision.ChunkCollision;
import net.malisis.core.util.chunkcollision.IChunkCollidable;
import net.malisis.doors.MalisisDoors;
import net.malisis.doors.block.Door;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

@MalisisRendered(block = Door3x3Renderer.class, item = DefaultRenderer.Item.class)
public class Door3x3 extends MalisisBlock implements ITileEntityProvider, IChunkCollidable
{
	public enum Type
	{
		OAK("big_door_oak_3x3", BlockPlanks.EnumType.OAK, false),
		SPRUCE("big_door_spruce_3x3", BlockPlanks.EnumType.SPRUCE, false),
		BIRCH("big_door_birch_3x3", BlockPlanks.EnumType.BIRCH, false),
		JUNGLE("big_door_jungle_3x3", BlockPlanks.EnumType.JUNGLE, false),
		ACACIA("big_door_acacia_3x3", BlockPlanks.EnumType.ACACIA, false),
		DARK_OAK("big_door_dark_oak_3x3", BlockPlanks.EnumType.DARK_OAK, false),
		IRON("big_door_iron_3x3", null, false),
		RUSTY("big_door_rusty_3x3", null, false),
		VANILLA("big_door_vanilla_3x3", BlockPlanks.EnumType.OAK, true);

		public String name;
		public boolean metal;
		/** The wood the door is made of, null for metal doors. A wooden door keeps its pattern in any other wood. */
		public BlockPlanks.EnumType wood;
		/** Whether the door is the vanilla door of its wood, stretched to 3x3 (see {@link StretchedDoorIcon}). */
		public boolean vanilla;

		Type(String name, BlockPlanks.EnumType wood, boolean vanilla)
		{
			this.name = name;
			this.metal = wood == null;
			this.wood = wood;
			this.vanilla = vanilla;
		}
	}

	/** The item NBT key of the door's wood (chosen in the door factory, or kept from where the door was placed before). */
	public static final String NBT_WOOD = "doorWood";

	private Type type;
	private AxisAlignedBB defaultBoundingBox = new AxisAlignedBB(-1, 0, 1 - Door.DOOR_WIDTH, 2, 3, 1);

	public Door3x3(Type type)
	{
		super(type.metal ? Material.IRON : Material.WOOD);
		this.type = type;
		setName(type.name);

		setHardness(type.metal ? 5.0F : 3.0F);
		setResistance(type.metal ? 200 : 10.0f);
		setSoundType(type.metal ? SoundType.METAL : SoundType.WOOD);
		setCreativeTab(MalisisDoors.tab);
		addComponent(new DirectionalComponent());

		if (MalisisCore.isClient())
			addComponent(Door3x3IconProvider.get(type));
	}

	public Type getType()
	{
		return type;
	}

	/**
	 * Checks if the item is a wooden 3x3 door, whose wood can be chosen in the door factory.
	 *
	 * @param itemStack the item stack
	 * @return true, if it is a wooden 3x3 door
	 */
	public static boolean isWooden(ItemStack itemStack)
	{
		if (itemStack.isEmpty())
			return false;
		Block block = Block.getBlockFromItem(itemStack.getItem());
		return block instanceof Door3x3 && !((Door3x3) block).type.metal;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase player, ItemStack itemStack)
	{
		super.onBlockPlacedBy(world, pos, state, player, itemStack);

		ChunkCollision.get().replaceBlocks(world, new MBlockState(world, pos));

		Door3x3Tile te = TileEntityUtils.getTileEntity(Door3x3Tile.class, world, pos);
		if (te != null)
		{
			te.setFrameState(MBlockState.fromNBT(itemStack.getTagCompound()));
			if (!type.metal)
				setPlacedWood(te, world, pos, itemStack);
			//settings set in the door factory
			BigDoorSettings.applyFromItem(itemStack, te);
		}
	}

	/**
	 * Sets the wood of the placed door: the item's (chosen in the door factory, or kept from where the door was before), otherwise the most
	 * common wood around the door (so it matches the planks it is set in), otherwise the door's own.
	 */
	private void setPlacedWood(Door3x3Tile te, World world, BlockPos pos, ItemStack itemStack)
	{
		NBTTagCompound nbt = itemStack.getTagCompound();
		BlockPlanks.EnumType itemWood = nbt != null ? Door3x3Tile.woodFromName(nbt.getString(NBT_WOOD)) : null;
		if (itemWood != null)
		{
			te.setWood(itemWood);
			return;
		}

		//the columns on each side of the door, and the row above it
		EnumFacing side = DirectionalComponent.getDirection(world, pos).rotateY();
		List<BlockPos> around = new ArrayList<>();
		for (int i = 0; i < 3; i++)
		{
			around.add(pos.offset(side, 2).up(i));
			around.add(pos.offset(side, -2).up(i));
			around.add(pos.offset(side, i - 1).up(3));
		}

		Map<BlockPlanks.EnumType, Integer> counts = new EnumMap<>(BlockPlanks.EnumType.class);
		for (BlockPos p : around)
		{
			BlockPlanks.EnumType wood = getWood(world.getBlockState(p));
			if (wood != null)
				counts.merge(wood, 1, Integer::sum);
		}

		BlockPlanks.EnumType wood = counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
		te.setWood(wood);
	}

	/**
	 * The wood of a block (planks, logs, wooden slabs...), if it has a wood variant.
	 */
	private static BlockPlanks.EnumType getWood(IBlockState state)
	{
		for (IProperty<?> property : state.getPropertyKeys())
			if (property.getValueClass() == BlockPlanks.EnumType.class)
				return (BlockPlanks.EnumType) state.getValue(property);
		return null;
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
	{
		if (world.isRemote)
			return true;

		Door3x3Tile te = TileEntityUtils.getTileEntity(Door3x3Tile.class, world, pos);
		if (te == null || !BigDoorSettings.canUseByHand(te))
			return true;

		te.openOrCloseDoor();
		return true;
	}

	@Override
	public AxisAlignedBB[] getBoundingBoxes(IBlockAccess world, BlockPos pos, IBlockState state, BoundingBoxType type)
	{
		if (type == BoundingBoxType.PLACEDBOUNDINGBOX)
			return new AxisAlignedBB[] { defaultBoundingBox };

		Door3x3Tile te = TileEntityUtils.getTileEntity(Door3x3Tile.class, world, pos);
		if (te == null)
			return AABBUtils.identities();

		AxisAlignedBB[] aabbs = new AxisAlignedBB[] { defaultBoundingBox };
		if ((type == BoundingBoxType.COLLISION || type == BoundingBoxType.RAYTRACE || type == BoundingBoxType.RENDER)
				&& (te.isOpened() || te.isMoving()))
			aabbs = new AxisAlignedBB[] {	new AxisAlignedBB(-1, 0, -.5f, Door.DOOR_WIDTH - 1, 3, 1),
											new AxisAlignedBB(2 - Door.DOOR_WIDTH, 0, -.5f, 2, 3, 1) };

		return aabbs;
	}

	@Override
	public int blockRange()
	{
		return 3;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata)
	{
		return new Door3x3Tile(type.metal);
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
	{
		if (!player.capabilities.isCreativeMode)
		{
			Door3x3Tile te = TileEntityUtils.getTileEntity(Door3x3Tile.class, world, pos);
			if (te != null)
				spawnAsEntity(world, pos, te.getDroppedItemStack());
		}

		return super.removedByPlayer(state, world, pos, player, willHarvest);
	}

	@Override
	public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
	{
		return new ArrayList<>();
	}

	@Override
	public boolean isOpaqueCube(IBlockState state)
	{
		return false;
	}

	@Override
	public boolean isFullCube(IBlockState state)
	{
		return false;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state)
	{
		return EnumBlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer)
	{
		return true;
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos fromPos)
	{
		Door3x3Tile te = TileEntityUtils.getTileEntity(Door3x3Tile.class, world, pos);
		if (te == null || !BigDoorSettings.reactsToRedstone(te))
			return;

		boolean powered = world.isBlockPowered(pos);
		if (powered || neighbor.getDefaultState().canProvidePower())
			te.setPowered(powered);
	}

	public static class Door3x3IconProvider implements IBlockIconProvider
	{
		Icon itemIcon;
		Icon doorIcon;
		//the door in each other wood: the vanilla door of that wood, or this door's pattern in its planks
		Map<BlockPlanks.EnumType, Icon> woodIcons = new EnumMap<>(BlockPlanks.EnumType.class);

		public Door3x3IconProvider(Type type)
		{
			if (type.vanilla)
			{
				doorIcon = itemIcon = StretchedDoorIcon.from(type.wood);
				for (BlockPlanks.EnumType wood : Door3x3Tile.WOODS)
					woodIcons.put(wood, StretchedDoorIcon.from(wood));
				return;
			}

			itemIcon = Icon.from(MalisisDoors.modid + ":items/" + type.name);
			doorIcon = Icon.from(MalisisDoors.modid + ":blocks/" + type.name);
			if (type.wood != null)
				for (BlockPlanks.EnumType wood : Door3x3Tile.WOODS)
					if (wood != type.wood)
						woodIcons.put(wood, PlanksDoorIcon.from(type, wood));
		}

		/**
		 * The door's icon in the wood.
		 *
		 * @param wood the wood, null for the door's own
		 * @return the icon
		 */
		public Icon getDoorIcon(BlockPlanks.EnumType wood)
		{
			return wood != null && woodIcons.containsKey(wood) ? woodIcons.get(wood) : doorIcon;
		}

		@Override
		public Icon getIcon(IBlockState state, EnumFacing side)
		{
			return doorIcon;
		}

		@Override
		public Icon getIcon(ItemStack itemStack, EnumFacing side)
		{
			NBTTagCompound nbt = itemStack.getTagCompound();
			return getDoorIcon(nbt != null ? Door3x3Tile.woodFromName(nbt.getString(NBT_WOOD)) : null);
		}

		public static Door3x3IconProvider get(Type type)
		{
			return new Door3x3IconProvider(type);
		}
	}
}
