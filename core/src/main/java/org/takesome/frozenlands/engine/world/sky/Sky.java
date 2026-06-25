package org.takesome.frozenlands.engine.world.sky;

import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SkyFactory;
import jme3utilities.sky.SkyControl;
import jme3utilities.sky.StarsOption;
import jme3utilities.sky.Updater;
import org.takesome.frozenlands.engine.EngineContext;

public class Sky {

    private String skyTexture = "textures/FullskiesSunset0068.dds";
    private Vector3f sunDirection = new Vector3f(-1f, -1f, -1f);
    private ColorRGBA sunColor = ColorRGBA.White;
    private ColorRGBA ambientColor = ColorRGBA.DarkGray;
    private DirectionalLight sun;
    private final Node rootNode;
    private final AssetManager assetManager;
    private final Camera camera;
    public Sky(EngineContext kernelInterface){
        this.rootNode = kernelInterface.getRootNode();
        this.assetManager = kernelInterface.getAssetManager();
        this.camera = kernelInterface.getCamera();
    }

    public void addSky(){
        var gi = new AmbientLight(ambientColor);
        sun = new DirectionalLight(sunDirection.normalizeLocal());
        sun.setColor(sunColor.mult(1f));

        Spatial sky = SkyFactory.createSky(assetManager, skyTexture, SkyFactory.EnvMapType.CubeMap);
        sky.setShadowMode(RenderQueue.ShadowMode.Off);
        rootNode.attachChild(sky);
        SkyControl skyControl = new SkyControl(assetManager, camera, .5f, StarsOption.TopDome, true);
        rootNode.addControl(skyControl);
        skyControl.setCloudiness(0.8f);
        skyControl.setCloudsYOffset(0.4f);
        skyControl.setTopVerticalAngle(1.78f);
        skyControl.getSunAndStars().setHour(11);
        Updater updater = skyControl.getUpdater();
        updater.setAmbientLight(gi);
        updater.setMainLight(sun);
        skyControl.setEnabled(true);
        rootNode.addLight(sun);
    }


    public DirectionalLight getSun() {
        return this.sun;
    }
}
