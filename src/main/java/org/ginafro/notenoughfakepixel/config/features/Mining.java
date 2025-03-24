package org.ginafro.notenoughfakepixel.config.features;

import com.google.gson.annotations.Expose;
import org.ginafro.notenoughfakepixel.config.gui.core.config.annotations.*;

public class Mining {

    @Expose
    @ConfigOption(name = "Enable Mining Ability Notifier", desc = "Notify when mining ability is ready.")
    @ConfigEditorBoolean
    public boolean miningAbilityNotifier = true;

    @Expose
    @ConfigOption(name = "Disable Don Espresso Messages", desc = "Disable Don Espresso event messages.")
    @ConfigEditorBoolean
    public boolean miningDisableDonEspresso = true;

    @Expose
    @ConfigOption(name = "Fix Drill Animation Reset", desc = "Fix drill animation resetting on fuel update.")
    @ConfigEditorBoolean
    public boolean miningDrillFix = true;

    @Expose
    @ConfigOption(name = "Puzzler Solver", desc = "Enable Puzzler block solver.")
    @ConfigEditorBoolean
    public boolean miningPuzzlerSolver = true;

    @Expose
    @ConfigOption(name = "Remove Ghosts Invisibility", desc = "Remove invisibility from ghosts.")
    @ConfigEditorBoolean
    public boolean miningShowGhosts = true;

    // Overlay Subcategory
    @Expose
    @ConfigOption(name = "Mining Overlay Settings", desc = "Settings for the mining overlay.")
    @ConfigEditorAccordion(id = 0)
    public boolean overlayAccordion = false;

    @Expose
    @ConfigOption(name = "Mining Overlay", desc = "Enable the mining overlay in Dwarven Mines.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean miningOverlay = true;

    @Expose
    @ConfigOption(name = "Mining Overlay Offset X", desc = "Horizontal offset of the mining overlay.")
    @ConfigEditorSlider(minValue = 0.0f, maxValue = 1800.0f, minStep = 1.0f)
    @ConfigAccordionId(id = 0)
    public float miningOverlayOffsetX = 10.0f;

    @Expose
    @ConfigOption(name = "Mining Overlay Offset Y", desc = "Vertical offset of the mining overlay.")
    @ConfigEditorSlider(minValue = 0.0f, maxValue = 1250.0f, minStep = 1.0f)
    @ConfigAccordionId(id = 0)
    public float miningOverlayOffsetY = 10.0f;

    @Expose
    @ConfigOption(name = "Mining Overlay Scale", desc = "Scale of the mining overlay text.")
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 5.0f, minStep = 0.1f)
    @ConfigAccordionId(id = 0)
    public float miningOverlayScale = 1.0f;

    @Expose
    @ConfigOption(name = "Mining Overlay Background Color", desc = "Background color of the mining overlay.")
    @ConfigEditorColour
    @ConfigAccordionId(id = 0)
    public String miningOverlayBackgroundColor = "0:150:0:0:0";

    @Expose
    @ConfigOption(name = "Show Ability Cooldown", desc = "Show the mining ability cooldown in the overlay.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean miningAbilityCooldown = true;

    @Expose
    @ConfigOption(name = "Show Mithril Powder", desc = "Show mithril powder in the overlay.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean miningMithrilPowder = true;

    @Expose
    @ConfigOption(name = "Show Drill Fuel", desc = "Show drill fuel in the overlay.")
    @ConfigEditorBoolean
    @ConfigAccordionId(id = 0)
    public boolean miningDrillFuel = true;

    @Expose
    @ConfigOption(name = "Edit Mining Overlay Position", desc = "Adjust the mining overlay position visually")
    @ConfigEditorButton(runnableId = "editMiningOverlayPosition", buttonText = "Edit Position")
    @ConfigAccordionId(id = 0)
    public String editMiningOverlayPositionButton = "";
}