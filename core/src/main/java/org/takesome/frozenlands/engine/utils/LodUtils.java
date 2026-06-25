package org.takesome.frozenlands.engine.utils;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.LodControl;
import jme3tools.optimize.LodGenerator;

public class LodUtils {
    private LodUtils() {}

    public static void setUpTreeModelLod(Spatial model) {
        setUpModelLod(model);
    }

    public static void setUpCharacterModelLod(Spatial model) {
        setUpModelLod(model);
    }

    public static void setUpModelLod(Spatial model) {
        if (model == null) {
            return;
        }
        if (model instanceof Geometry geometry) {
            createModelLod(geometry);
            return;
        }
        if (model instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                setUpModelLod(child);
            }
        }
    }

    private static void createModelLod(Geometry geometry) {
        if (geometry.getControl(LodControl.class) != null) {
            return;
        }
        LodGenerator lod = new LodGenerator(geometry);
        lod.bakeLods(LodGenerator.TriangleReductionMethod.COLLAPSE_COST, 0.25f, 0.5f, 0.75f);
        LodControl lc = new LodControl();
        geometry.addControl(lc);
    }
}
