package org.takesome.frozenlands.engine.shaders;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.shadow.DirectionalLightShadowFilter;
import org.takesome.frozenlands.engine.EngineContext;

import java.util.Map;
import org.takesome.frozenlands.engine.world.sky.Sky;

public class Shaders extends BaseAppState {
    private final FilterPostProcessor fpp;
    private final EngineContext kernelInterface;
    private final AssetManager assetManager;
    private final ViewPort viewPort;
    private final ShaderRuntimeSettings runtimeSettings = new ShaderRuntimeSettings();
    private final ShadowSettings shadowSettings = new ShadowSettings();
    private LSF lsf;
    private Bloom bloom;
    private DOF dof;
    private DirectionalLightShadowFilter shadowFilter;

    public Shaders(EngineContext kernelInterface){
        this.kernelInterface = kernelInterface;
        this.fpp = kernelInterface.getFpp();
        this.assetManager = kernelInterface.getAssetManager();
        this.viewPort = kernelInterface.getViewPort();
    }

    @Override
    protected void initialize(Application application) {
        bloom = new Bloom(kernelInterface);
        bloom.setBloom(runtimeSettings.bloomIntensity());
        bloom.setExposurePover(runtimeSettings.bloomExposurePower());
        bloom.compile();

        lsf = new LSF(kernelInterface, runtimeSettings.lightDirection());
        lsf.setLightDensity(runtimeSettings.lightDensity());
        lsf.compile();

        dof = new DOF(kernelInterface);
        dof.setFocusDistance(runtimeSettings.focusDistance());
        dof.setFocusRange(runtimeSettings.focusRange());
        dof.compile();

        shadowFilter = new DirectionalLightShadowFilter(assetManager,
                shadowSettings.getShadowMapSize(), shadowSettings.getSplits());
        shadowFilter.setLight(kernelInterface.requireService(Sky.class).getSun());
        applyShadowFilterSettings();
        fpp.addFilter(shadowFilter);
        fpp.addFilter(new FXAAFilter());
        viewPort.addProcessor(fpp);
    }

    @Override
    protected void cleanup(Application application) {
        viewPort.removeProcessor(fpp);
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    public LSF getLsf() { return lsf; }
    public Bloom getBloom() { return bloom; }
    public DOF getDof() { return dof; }
    public DirectionalLightShadowFilter getShadowFilter() { return shadowFilter; }
    public ShadowSettings getShadowSettings() { return shadowSettings; }

    public ShadowSettings applyShadowSettings(Map<String, Object> values) {
        shadowSettings.update(values);
        applyShadowFilterSettings();
        return shadowSettings;
    }

    private void applyShadowFilterSettings() {
        if (shadowFilter == null) {
            return;
        }
        shadowFilter.setShadowIntensity(shadowSettings.getShadowIntensity());
        shadowFilter.setEdgesThickness(shadowSettings.getEdgesThickness());
        shadowFilter.setShadowZExtend(shadowSettings.getShadowZExtend());
    }
}
