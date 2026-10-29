package net.malisis.doors.bigdoors;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
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
 * A 3x3 door's pattern in another wood, made when the textures are loaded: the door is drawn with the planks texture of the wood (turned
 * upright, one planks tile per block), with the windows and the dark details (frame, handles) of the door texture. Both come from the
 * active resource packs, so the door matches the planks around it.<br>
 * A resource pack can still provide its own texture for it (<code>blocks/big_door_3x3/&lt;door&gt;_&lt;wood&gt;.png</code>), used instead.
 */
@SideOnly(Side.CLIENT)
public class PlanksDoorIcon extends Icon
{
	/** The most pixels per block, so high resolution packs don't make huge textures. */
	private static final int MAX_BLOCK_SIZE = 256;
	/** How much darker than the door's plain wood a pixel must be to darken the planks. */
	private static final float DARK_DETAIL = 0.85F;

	private final ResourceLocation door;
	private final ResourceLocation planks;

	private PlanksDoorIcon(String name, ResourceLocation door, ResourceLocation planks)
	{
		super(name);
		this.door = door;
		this.planks = planks;
	}

	/**
	 * Gets the icon of the door in the wood.
	 *
	 * @param type the door
	 * @param wood the wood
	 * @return the icon
	 */
	public static Icon from(Door3x3.Type type, BlockPlanks.EnumType wood)
	{
		String name = MalisisDoors.modid + ":blocks/big_door_3x3/" + type.wood.getName() + "_" + wood.getName();
		Icon icon = registeredIcons.get(name);
		if (icon == null)
		{
			icon = new PlanksDoorIcon(name,
					new ResourceLocation(MalisisDoors.modid, "textures/blocks/" + type.name + ".png"),
					new ResourceLocation("minecraft", "textures/blocks/planks_" + wood.getTranslationKey() + ".png"));
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
			BufferedImage doorImage = read(manager, door);
			BufferedImage planksImage = read(manager, planks);
			int doorWidth = doorImage.getWidth();
			int doorHeight = doorImage.getHeight();
			//first frame only, for animated planks
			int planksSize = Math.min(planksImage.getWidth(), planksImage.getHeight());

			//the door is 3 blocks wide, each drawn at the planks resolution
			int doorBlock = doorWidth / 3;
			int block = Math.max(doorBlock, Math.min(planksSize, MAX_BLOCK_SIZE));
			int width = doorWidth * block / doorBlock;
			int height = doorHeight * block / doorBlock;

			float middle = middleLuminance(doorImage);
			int[] pixels = new int[width * height];
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					int doorColor = doorImage.getRGB(x * doorWidth / width, y * doorHeight / height);
					//windows
					if ((doorColor >>> 24) <= 128)
						continue;

					//planks turned upright, so the grain follows the door boards
					int u = (y % block) * planksSize / block;
					int v = planksSize - 1 - (x % block) * planksSize / block;
					int planksColor = planksImage.getRGB(u, v);

					//the door's dark details only (frame lines, handles): its highlights and small variations would show as light streaks
					float shade = Math.max(0.35F, Math.min(1, luminance(doorColor) / middle));
					shade = shade > DARK_DETAIL ? 1 : shade / DARK_DETAIL;
					pixels[y * width + x] = 0xFF000000 | shade(planksColor >> 16, shade) << 16 | shade(planksColor >> 8, shade) << 8
							| shade(planksColor, shade);
				}
			}

			setIconWidth(width);
			setIconHeight(height);
			int[][] frame = new int[1][];
			frame[0] = pixels;
			setFramesTextureData(Collections.singletonList(frame));
			return false;
		}
		catch (IOException | RuntimeException e)
		{
			MalisisCore.log.error("Failed to make the door texture {} from {} and {}", location, door, planks, e);
			return true;
		}
	}

	private static BufferedImage read(IResourceManager manager, ResourceLocation location) throws IOException
	{
		IResource resource = manager.getResource(location);
		try
		{
			return TextureUtil.readBufferedImage(resource.getInputStream());
		}
		finally
		{
			IOUtils.closeQuietly(resource);
		}
	}

	/** The median luminance of the opaque pixels: the door's plain wood, neither shaded nor highlighted. */
	private static float middleLuminance(BufferedImage image)
	{
		int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
		double[] lum = Arrays.stream(pixels).filter(argb -> (argb >>> 24) > 128).mapToDouble(PlanksDoorIcon::luminance).sorted().toArray();
		return lum.length == 0 ? 1 : Math.max(1, (float) lum[lum.length / 2]);
	}

	private static int shade(int channel, float shade)
	{
		return Math.min(255, (int) ((channel & 0xFF) * shade));
	}

	private static float luminance(int argb)
	{
		return 0.299F * ((argb >> 16) & 0xFF) + 0.587F * ((argb >> 8) & 0xFF) + 0.114F * (argb & 0xFF);
	}
}
