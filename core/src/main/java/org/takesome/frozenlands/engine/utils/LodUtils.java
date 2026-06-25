package org.takesome.frozenlands.engine.utils;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.LodControl;
import jme3tools.optimize.LodGenerator;

public class LodUtils {
    private LodUtils() {}

    public static void setUpTreeModelLod(Spatial model) {
        //Structure specific just for "Models/Fir1/fir1_androlo.j3o"!
        ((Node) ((Node) model).getChild(0)).getChildren().forEach(geometry -> {
            createModelLod(geometry);
        });
    }

    public static void setUpCharacterModelLod(Spatial model) {
        Geometry geometry = (Geometry) ((Node) model).getChild(0);
        createModelLod(geometry);
    }
    private static void createModelLod(Spatial geometry) {
        LodGenerator lod = new LodGenerator((Geometry) geometry);
        lod.bakeLods(LodGenerator.TriangleReductionMethod.COLLAPSE_COST, 0.25f, 0.5f, 0.75f);
        LodControl lc = new LodControl();
        geometry.addControl(lc);
    }
}