package net.malisis.doors.bigdoors;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;

import net.malisis.core.MalisisCore;
import net.malisis.core.renderer.icon.Icon;
import net.malisis.doors.MalisisDoors;
import net.minecraft.block.BlockPlanks;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The vanilla 3x3 door of a wood, made from the vanilla door of the resource pack when the textures are loaded: each half of the 3x3 door (1.5 blocks
 * wide, 3 tall) is the vanilla door (upper over lower, 1 wide, 2 tall) scaled by 1.5, the right half mirrored so the handles meet in the
 * middle.<br>
 * With only the vanilla textures (no resource pack door), the mod's own 3x3 door texture is used.<br>
 * A resource pack can also provide the texture itself (<code>blocks/big_door_3x3/door_&lt;name&gt;.png</code>), used instead.
 */
@SideOnly(Side.CLIENT)
public class StretchedDoorIcon extends Icon
{
	private final ResourceLocation upper;
	private final ResourceLocation lower;
	private final ResourceLocation fallback;

	private StretchedDoorIcon(String name, String vanillaDoor, ResourceLocation fallback)
	{
		super(name);
		this.upper = new ResourceLocation("minecraft", "textures/blocks/door_" + vanillaDoor + "_upper.png");
		this.lower = new ResourceLocation("minecraft", "textures/blocks/door_" + vanillaDoor + "_lower.png");
		this.fallback = fallback;
	}

	/**
	 * Gets the icon of the vanilla 3x3 door of the wood.
	 *
	 * @param wood the wood
	 * @return the icon
	 */
	public static Icon from(BlockPlanks.EnumType wood)
	{
		//the oak door is door_wood
		String vanillaDoor = wood == BlockPlanks.EnumType.OAK ? "wood" : wood.getName();
		String name = MalisisDoors.modid + ":blocks/big_door_3x3/door_" + vanillaDoor;
		Icon icon = registeredIcons.get(name);
		if (icon == null)
		{
			icon = new StretchedDoorIcon(name, vanillaDoor, new ResourceLocation(MalisisDoors.modid, "textures/blocks/big_door_"
					+ wood.getName() + "_3x3.png"));
			registeredIcons.put(name, icon);
		}
		return icon;
	}

	@Override
	public boolean hasCustomLoader(IResourceManager manager, ResourceLocation location)
	{
		//a texture for it in a resource pack
		try
		{
			IOUtils.closeQuietly(manager.getResource(location));
			return false;
		}
		catch (IOException e)
		{
			return true;
		}
	}

	/**
	 * @return false when loaded, true if it failed (the missing texture is shown)
	 */
	@Override
	public boolean load(IResourceManager manager, ResourceLocation location, Function<ResourceLocation, TextureAtlasSprite> textureGetter)
	{
		try
		{
			IResource upperResource = manager.getResource(upper);
			IResource lowerResource = manager.getResource(lower);
			BufferedImage image;
			if (isVanilla(upperResource) && isVanilla(lowerResource))
				image = read(manager.getResource(fallback));
			else
				image = stretch(read(upperResource), read(lowerResource));

			setIconWidth(image.getWidth());
			setIconHeight(image.getHeight());
			int[][] frame = new int[1][];
			frame[0] = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
			setFramesTextureData(Collections.singletonList(frame));
			return false;
		}
		catch (IOException | RuntimeException e)
		{
			MalisisCore.log.error("Failed to make the door texture {} from {} and {}", location, upper, lower, e);
			return true;
		}
	}

	private static boolean isVanilla(IResource resource)
	{
		return "default".equalsIgnoreCase(resource.getResourcePackName());
	}

	/**
	 * Makes the 3x3 door from the vanilla door halves.
	 */
	private static BufferedImage stretch(BufferedImage upper, BufferedImage lower)
	{
		//each block drawn at the door's resolution
		int size = Math.max(upper.getWidth(), lower.getWidth());
		int half = size * 3 / 2;
		int height = size * 3;

		BufferedImage image = new BufferedImage(half * 2, height, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < height; y++)
		{
			//upper on the top half, lower on the bottom half (first frame only, for animated doors)
			BufferedImage door = y < height / 2 ? upper : lower;
			int w = door.getWidth();
			int v = (y % (height / 2)) * w / (height / 2);
			for (int x = 0; x < half * 2; x++)
			{
				int doorX = x < half ? x : half * 2 - 1 - x;
				image.setRGB(x, y, door.getRGB(doorX * w / half, v));
			}
		}
		return image;
	}

	private static BufferedImage read(IResource resource) throws IOException
	{
		try
		{
			return TextureUtil.readBufferedImage(resource.getInputStream());
		}
		finally
		{
			IOUtils.closeQuietly(resource);
		}
	}
}
