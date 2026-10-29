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

package net.malisis.doors.gui;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;

import net.malisis.core.client.gui.Anchor;
import net.malisis.core.client.gui.ComponentPosition;
import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.UISlot;
import net.malisis.core.client.gui.component.container.UIContainer;
import net.malisis.core.client.gui.component.container.UIPlayerInventory;
import net.malisis.core.client.gui.component.container.UITabGroup;
import net.malisis.core.client.gui.component.decoration.UIImage;
import net.malisis.core.client.gui.component.decoration.UILabel;
import net.malisis.core.client.gui.component.decoration.UITooltip;
import net.malisis.core.client.gui.component.interaction.UIButton;
import net.malisis.core.client.gui.component.interaction.UICheckBox;
import net.malisis.core.client.gui.component.interaction.UIRadioButton;
import net.malisis.core.client.gui.component.interaction.UISelect;
import net.malisis.core.client.gui.component.interaction.UITab;
import net.malisis.core.client.gui.component.interaction.UITextField;
import net.malisis.core.client.gui.element.position.Position;
import net.malisis.core.client.gui.element.position.Positions;
import net.malisis.core.client.gui.event.ComponentEvent.ValueChange;
import net.malisis.core.client.gui.event.component.StateChangeEvent.ActiveStateChange;
import net.malisis.core.client.gui.render.GuiIcon;
import net.malisis.core.client.gui.render.GuiTexture;
import net.malisis.core.inventory.MalisisInventoryContainer;
import net.malisis.core.inventory.MalisisSlot;
import net.malisis.core.renderer.font.FontOptions;
import net.malisis.core.util.TileEntityUtils;
import net.malisis.doors.DoorDescriptor.RedstoneBehavior;
import net.malisis.doors.DoorRegistry;
import net.malisis.doors.MalisisDoors;
import net.malisis.doors.bigdoors.BigDoorSettings;
import net.malisis.doors.bigdoors.Door3x3;
import net.malisis.doors.bigdoors.Door3x3Tile;
import net.malisis.doors.network.DoorFactoryMessage;
import net.malisis.doors.tileentity.DoorFactoryTileEntity;
import net.minecraft.block.BlockPlanks;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.ResourceLocation;

/**
 * @author Ordinastie
 *
 */
public class DoorFactoryGui extends MalisisGui
{
	public static ResourceLocation tabIconsRl = new ResourceLocation(MalisisDoors.modid, "textures/gui/doorfactory_tabicons.png");
	public static GuiTexture tabTexture = new GuiTexture(tabIconsRl, 128, 128);
	public static GuiIcon propIcon = new GuiIcon(tabTexture, 0, 0, 64, 64);
	public static GuiIcon matIcon = new GuiIcon(tabTexture, 64, 0, 64, 64);
	public static GuiIcon dcIcon = new GuiIcon(tabTexture, 0, 64, 64, 64);

	private DoorFactoryTileEntity tileEntity;
	private UISelect<String> selDoorMovement;
	private UITextField tfOpenTime;
	private UITextField tfAutoCloseTime;
	private UICheckBox cbDoubleDoor;
	private UICheckBox cbProximity;
	private UISelect<RedstoneBehavior> selRedstone;
	private UISelect<String> selDoorSound;
	private UIRadioButton rbCreate;
	private UIRadioButton rbEdit;
	private UIContainer contCreate;
	private UIContainer contEdit;
	private UIButton btnCreate;
	private Digicode digicode;
	//in place of the movement for big doors, which have their own
	private UILabel lblBigDoorMovement;
	//the wood of wooden 3x3 doors, or the blocks around them
	private UISelect<String> selBigDoorWood;
	private UILabel lblBigDoorWood;
	private static final String WOOD_AROUND = "around";
	//why the door can't be created or modified: next to the output after clicking the button, and in its tooltip
	private UILabel lblProblem;
	private String shownProblem;
	private long problemShownUntil;

	private static final long PROBLEM_SHOWN_MILLIS = 5000;
	private static final int PROBLEM_COLOR = 0xFF5555;

	private static String activeTab;

	public DoorFactoryGui(DoorFactoryTileEntity te, MalisisInventoryContainer container)
	{
		setInventoryContainer(container);
		tileEntity = te;
	}

	@Override
	public void construct()
	{
		//the 6.5 window: a window background, a padding of 5 and the title in its top left corner
		UIContainer window = UIContainer.window();
		window.setSize(UIPlayerInventory.INVENTORY_SIZE.width() + 80, 255);
		window.add(label("tile.door_factory.name", 0, 0));

		UIContainer propContainer = getPropertiesContainer();
		UIContainer matContainer = getMaterialsContainer();
		UIContainer dcContainer = getDigicodeContainer();

		UITabGroup tabGroup = new UITabGroup(ComponentPosition.LEFT, UITabGroup.Type.WINDOW);

		UITab tabProp = tab("tab_prop", propIcon, "gui.door_factory.tab_properties");
		UITab tabMat = tab("tab_mat", matIcon, "gui.door_factory.tab_materials");
		UITab tabDc = tab("tab_dc", dcIcon, "gui.door_factory.tab_digicode");

		tabGroup.addTab(tabProp, propContainer);
		tabGroup.addTab(tabMat, matContainer);
		tabGroup.addTab(tabDc, dcContainer);

		tabGroup.setActiveTab(activeTab != null ? activeTab : "tab_prop");
		//attaching adds the tab containers to the window, at its top left and its full size
		tabGroup.attachTo(window, false);
		//as in 6.5: the tabs down the left side from near the top (6.6 centres them), the containers below the title
		tabGroup.setPosition(Position.of(Positions.leftOf(tabGroup, window, -2), () -> window.position().y() + 10));
		placeTabContainer(propContainer, 95);
		placeTabContainer(matContainer, 80);
		placeTabContainer(dcContainer, 80);

		btnCreate = new UIButton("gui.door_factory.create_door");
		//buttons grow to fit their text: 1 high is the text's height
		btnCreate.setSize(80, 1);
		btnCreate.setPosition(0, 110, Anchor.CENTER);
		btnCreate.onClick(this::onCreateDoor);
		UISlot outputSlot = new UISlot(tileEntity.outputSlot);
		outputSlot.setPosition(0, 132, Anchor.CENTER);

		lblProblem = new UILabel("", true);
		lblProblem.setFontOptions(FontOptions.builder().color(PROBLEM_COLOR).scale(0.8F).build());
		lblProblem.setSize(98, 30);
		lblProblem.setPosition(0, 130, Anchor.RIGHT);
		lblProblem.setVisible(false);
		window.add(lblProblem);

		UIPlayerInventory playerInv = new UIPlayerInventory(inventoryContainer.getPlayerInventory());

		window.add(playerInv);

		window.add(btnCreate);
		window.add(outputSlot);

		addToScreen(tabGroup);
		addToScreen(window);

		TileEntityUtils.linkTileEntityToGui(tileEntity, this);
	}

	private UIContainer getPropertiesContainer()
	{
		UIContainer propContainer = container(95);

		//Door movement
		int y = 2;
		selDoorMovement = select(100, getSortedList(DoorRegistry.listMovements().keySet(), "door_movement."), "door_movement.%s", 0, y);
		propContainer.add(label("gui.door_factory.door_movement", 0, y + 2));
		propContainer.add(selDoorMovement);
		lblBigDoorMovement = new UILabel("gui.door_factory.door_movement.big_door");
		lblBigDoorMovement.setPosition(-4, y + 2, Anchor.RIGHT);
		lblBigDoorMovement.setFontOptions(FontOptions.builder().color(0x404040).build());
		lblBigDoorMovement.setVisible(false);
		propContainer.add(lblBigDoorMovement);

		//Opening time
		y += 12;
		tfOpenTime = textField(y);
		propContainer.add(label("gui.door_factory.door_open_time", 0, y + 2));
		propContainer.add(tfOpenTime);

		//Auto close time
		y += 12;
		tfAutoCloseTime = textField(y);
		propContainer.add(label("gui.door_factory.door_auto_close_time", 0, y + 2));
		propContainer.add(tfAutoCloseTime);

		//Double door
		y += 12;
		cbDoubleDoor = checkBox(y);
		propContainer.add(label("gui.door_factory.door_double_door", 0, y + 2));
		propContainer.add(cbDoubleDoor);

		//Proximity detection
		y += 12;
		cbProximity = checkBox(y);
		propContainer.add(label("gui.door_factory.proximity_detection", 0, y + 2));
		propContainer.add(cbProximity);

		//Redstone behavior
		y += 12;
		selRedstone = select(100, Lists.newArrayList(RedstoneBehavior.values()), "gui.door_factory.redstone_behavior.%s", 0, y);
		propContainer.add(label("gui.door_factory.redstone_behavior", 0, y + 2));
		propContainer.add(selRedstone);

		//Door sound
		y += 12;
		selDoorSound = select(100, getSortedList(DoorRegistry.listSounds().keySet(), "gui.door_factory.door_sound."),
				"gui.door_factory.door_sound.%s", 0, y);
		propContainer.add(label("gui.door_factory.door_sound", 0, y + 2));
		propContainer.add(selDoorSound);

		return propContainer;
	}

	//6.6 components are made without their gui, and their setters don't chain: these make the ones this screen uses

	private UILabel label(String text, int x, int y)
	{
		UILabel label = new UILabel(text);
		label.setPosition(x, y);
		return label;
	}

	private UIContainer container(int height)
	{
		UIContainer container = new UIContainer();
		placeTabContainer(container, height);
		return container;
	}

	/**
	 * Below the window title, the width of the window.
	 */
	private void placeTabContainer(UIContainer container, int height)
	{
		container.setSize(UIComponent.INHERITED, height);
		container.setPosition(0, 15);
	}

	private UITab tab(String name, GuiIcon icon, String tooltip)
	{
		UIImage image = new UIImage(icon);
		image.setSize(16, 16);
		UITab tab = new UITab(image);
		tab.setName(name);
		tab.setTooltip(new UITooltip(tooltip));
		tab.register(this);
		return tab;
	}

	/**
	 * @param labelPattern the translation key of an option's label, %s being the option
	 */
	private <T> UISelect<T> select(int width, List<T> options, String labelPattern, int x, int y)
	{
		UISelect<T> select = new UISelect<>(width, options);
		select.setPosition(x, y, Anchor.RIGHT);
		select.setLabelFunction(option -> I18n.format(String.format(labelPattern, option)));
		select.register(this);
		return select;
	}

	private UITextField textField(int y)
	{
		UITextField textField = new UITextField("");
		textField.setSize(30, 0);
		textField.setPosition(-5, y, Anchor.RIGHT);
		textField.register(this);
		return textField;
	}

	private UICheckBox checkBox(int y)
	{
		UICheckBox checkBox = new UICheckBox();
		checkBox.setPosition(-15, y, Anchor.RIGHT);
		checkBox.register(this);
		return checkBox;
	}

	private UISlot slot(MalisisSlot slot, int x, int y, int anchor)
	{
		UISlot uiSlot = new UISlot(slot);
		uiSlot.setPosition(x, y, anchor);
		return uiSlot;
	}

	private ImmutableList<String> getSortedList(Set<String> set, String prefix)
	{
		return FluentIterable.from(set).toSortedList((String s1, String s2) -> {
			return I18n.format(prefix + s1).compareTo(I18n.format(prefix + s2));
		});
	}

	private UIContainer getMaterialsContainer()
	{
		UIContainer matContainer = container(80);

		rbCreate = new UIRadioButton("rbDoor", "gui.door_factory.rb_create");
		rbCreate.setPosition(30, 0);
		rbCreate.register(this);
		rbEdit = new UIRadioButton("rbDoor", "gui.door_factory.rb_edit");
		rbEdit.setPosition(100, 0);
		rbEdit.register(this);

		matContainer.add(rbCreate);
		matContainer.add(rbEdit);

		//below the radio buttons, to the bottom of the tab
		contCreate = new UIContainer();
		contCreate.setSize(UIComponent.INHERITED, -14);
		contCreate.setPosition(0, 14);

		int y = 0;
		contCreate.add(label("gui.door_factory.frame_type", 0, y + 5));
		contCreate.add(label("gui.door_factory.top_material", 0, y + 23));
		contCreate.add(label("gui.door_factory.bottom_material", 0, y + 41));

		contCreate.add(slot(tileEntity.frameSlot, -10, y, Anchor.RIGHT));
		contCreate.add(slot(tileEntity.topMaterialSlot, -10, y + 18, Anchor.RIGHT));
		contCreate.add(slot(tileEntity.bottomMaterialSlot, -10, y + 36, Anchor.RIGHT));

		contEdit = new UIContainer();
		contEdit.setSize(UIComponent.INHERITED, -14);
		contEdit.setPosition(0, 14);

		contEdit.add(label("gui.door_factory.door_edit_slot", 0, 23));
		contEdit.add(slot(tileEntity.doorEditSlot, -10, 18, Anchor.RIGHT));

		//wooden 3x3 doors only
		List<String> woods = Lists.newArrayList(WOOD_AROUND);
		for (BlockPlanks.EnumType wood : Door3x3Tile.WOODS)
			woods.add(wood.getName());
		selBigDoorWood = select(80, woods, "gui.door_factory.big_door_wood.%s", -4, 42);
		lblBigDoorWood = label("gui.door_factory.big_door_wood", 0, 44);
		contEdit.add(lblBigDoorWood);
		contEdit.add(selBigDoorWood);

		matContainer.add(contCreate);
		matContainer.add(contEdit);

		return matContainer;
	}

	private UIContainer getDigicodeContainer()
	{
		UIContainer dcContainer = container(80);

		digicode = new Digicode();
		digicode.setPosition(0, 0, Anchor.CENTER);
		digicode.register(this);
		dcContainer.add(digicode);

		return dcContainer;
	}

	@Override
	public void update()
	{
		//big doors have their own movement and are already double doors: those settings don't apply to them
		boolean bigDoor = !tileEntity.isCreate() && BigDoorSettings.isBigDoor(tileEntity.doorEditSlot.getItemStack());
		selDoorMovement.setVisible(!bigDoor);
		lblBigDoorMovement.setVisible(bigDoor);
		cbDoubleDoor.setEnabled(!bigDoor);
		boolean woodenDoor = !tileEntity.isCreate() && Door3x3.isWooden(tileEntity.doorEditSlot.getItemStack());
		selBigDoorWood.setVisible(woodenDoor);
		lblBigDoorWood.setVisible(woodenDoor);

		//the reason the button does nothing: in its tooltip, and shown next to the output for a while after it is clicked
		String problem = tileEntity.getCreateProblem();
		if (!Objects.equals(problem, shownProblem))
		{
			shownProblem = problem;
			btnCreate.setTooltip(problem == null ? (UITooltip) null : new UITooltip(TextFormatting.RED + I18n.format(problem)));
			lblProblem.setText(problem == null ? "" : I18n.format(problem));
		}
		lblProblem.setVisible(problem != null && System.currentTimeMillis() < problemShownUntil);
	}

	@Override
	public void updateGui()
	{
		boolean isCreate = tileEntity.isCreate();
		if (isCreate)
			rbCreate.setSelected();
		else
			rbEdit.setSelected();
		contCreate.setVisible(isCreate);
		contEdit.setVisible(!isCreate);
		btnCreate.setText(isCreate ? "gui.door_factory.create_door" : "gui.door_factory.edit_door");

		selDoorMovement.setSelectedOption(DoorRegistry.getId(tileEntity.getDoorMovement()));
		tfOpenTime.setText(Integer.toString(tileEntity.getOpeningTime()));
		tfAutoCloseTime.setText(Integer.toString(tileEntity.getAutoCloseTime()));
		cbDoubleDoor.setChecked(tileEntity.isDoubleDoor());
		cbProximity.setChecked(tileEntity.hasProximityDetection());
		selRedstone.select(tileEntity.getRedstoneBehavior());
		selDoorSound.setSelectedOption(DoorRegistry.getId(tileEntity.getDoorSound()));
		selBigDoorWood.setSelectedOption(tileEntity.getBigDoorWood() != null ? tileEntity.getBigDoorWood().getName() : WOOD_AROUND);
	}

	@Subscribe
	public void onCheckedEvent(UICheckBox.CheckEvent event)
	{
		if (event.getComponent() == cbDoubleDoor)
			tileEntity.setDoubleDoor(event.isChecked());
		else if (event.getComponent() == cbProximity)
			tileEntity.setProximityDetection(event.isChecked());

		DoorFactoryMessage.sendDoorInformations(tileEntity);
	}

	@Subscribe
	public void onSelectEvent(UISelect.SelectEvent<?> event)
	{
		if (event.getNewValue() == null)
			return;

		if (event.getComponent() == selRedstone)
			tileEntity.setRedstoneBehavior((RedstoneBehavior) event.getNewValue());
		else if (event.getComponent() == selDoorMovement)
			tileEntity.setDoorMovement(DoorRegistry.getMovement((String) event.getNewValue()));
		else if (event.getComponent() == selDoorSound)
			tileEntity.setDoorSound(DoorRegistry.getSound((String) event.getNewValue()));
		else if (event.getComponent() == selBigDoorWood)
			tileEntity.setBigDoorWood(Door3x3Tile.woodFromName((String) event.getNewValue()));

		DoorFactoryMessage.sendDoorInformations(tileEntity);
	}

	@Subscribe
	public void onRbSelectEvent(UIRadioButton.SelectEvent event)
	{
		boolean isCreate = event.getNewValue() == rbCreate;

		tileEntity.setCreate(isCreate);
		contCreate.setVisible(isCreate);
		contEdit.setVisible(!isCreate);
		btnCreate.setText(isCreate ? "gui.door_factory.create_door" : "gui.door_factory.edit_door");

		DoorFactoryMessage.sendDoorInformations(tileEntity);
	}

	@Subscribe
	public void onGuiChangeEvent(ValueChange<?, ?> event)
	{
		if (event.getComponent() != tfOpenTime && event.getComponent() != tfAutoCloseTime)
			return;

		try
		{
			int value = Integer.decode((String) event.getNewValue());

			if (event.getComponent() == tfOpenTime)
				tileEntity.setOpeningTime(value);
			if (event.getComponent() == tfAutoCloseTime)
				tileEntity.setAutoCloseTime(value);

			DoorFactoryMessage.sendDoorInformations(tileEntity);
		}
		catch (NumberFormatException e)
		{
			//parsing failed, replace the value of the textfield by the value already in the TE
			//tfOpenTime.setText(Integer.toString(tileEntity.getOpeningTime()));
		}
	}

	@Subscribe
	public void onDigicodeChange(Digicode.CodeChangeEvent event)
	{
		tileEntity.setCode(event.getCode());
		DoorFactoryMessage.sendDoorInformations(tileEntity);
	}

	//the create/edit button
	private void onCreateDoor()
	{
		//something missing: say what instead of doing nothing
		if (tileEntity.getCreateProblem() != null)
		{
			problemShownUntil = System.currentTimeMillis() + PROBLEM_SHOWN_MILLIS;
			return;
		}
		problemShownUntil = 0;
		DoorFactoryMessage.sendCreateDoor(tileEntity);
	}

	@Subscribe
	public void onTabActivation(ActiveStateChange<UITab> event)
	{
		if (event.getState())
		{
			activeTab = event.getComponent().getName();

			if ("tab_dc".equals(activeTab))
				registerKeyListener(digicode);
			else
				unregisterKeyListener(digicode);
		}
	}

}
