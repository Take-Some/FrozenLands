package dev.takesome.helix.ui.legacy.render;

/**
 * Data description of a nine-slice panel.
 *
 * source='regions' uses explicit coordinates.
 * source='alpha' extracts the 3x3 grid from connected alpha runs in the asset.
 */
public final class UiNineSliceDefinition {
    public String id;
    public String material;
    public String texture;
    public String source = "regions";

    public UiRegionDefinition topLeft;
    public UiRegionDefinition top;
    public UiRegionDefinition topRight;
    public UiRegionDefinition left;
    public UiRegionDefinition center;
    public UiRegionDefinition right;
    public UiRegionDefinition bottomLeft;
    public UiRegionDefinition bottom;
    public UiRegionDefinition bottomRight;
}
