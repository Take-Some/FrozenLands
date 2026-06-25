package org.takesome.frozenlands.engine.shaders;

import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.LightScatteringFilter;
import org.takesome.frozenlands.engine.EngineContext;

public class LSF extends  Shaders {

    LightScatteringFilter lsf;
    FilterPostProcessor fpp;

    public LSF(EngineContext kernelInterface, Vector3f lightDir) {
        super(kernelInterface);
        this.fpp = kernelInterface.getFpp();
        lsf = new LightScatteringFilter(lightDir);
    }

    public void setLightDensity(float destiny){
        lsf.setLightDensity(destiny);
    }

    public void compile(){
        this.fpp.addFilter(lsf);
    }
}
