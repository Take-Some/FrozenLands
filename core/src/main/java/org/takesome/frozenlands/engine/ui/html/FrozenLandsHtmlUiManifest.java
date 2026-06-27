package org.takesome.frozenlands.engine.ui.html;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class FrozenLandsHtmlUiManifest {
    private String schema;
    private String name;
    private String runtime;
    private String entry;
    private String startupScreen = "hud";
    private Map<String, String> documents = Collections.emptyMap();
    private List<String> styles = Collections.emptyList();
    private List<String> scripts = Collections.emptyList();
    private String bindings;
    private Map<String, Object> legacy = Collections.emptyMap();

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public String getStartupScreen() {
        return startupScreen == null || startupScreen.isBlank() ? "hud" : startupScreen;
    }

    public void setStartupScreen(String startupScreen) {
        this.startupScreen = startupScreen;
    }

    public Map<String, String> getDocuments() {
        return documents == null ? Collections.emptyMap() : Collections.unmodifiableMap(documents);
    }

    public void setDocuments(Map<String, String> documents) {
        this.documents = documents == null ? Collections.emptyMap() : documents;
    }

    public List<String> getStyles() {
        return styles == null ? Collections.emptyList() : Collections.unmodifiableList(styles);
    }

    public void setStyles(List<String> styles) {
        this.styles = styles == null ? Collections.emptyList() : styles;
    }

    public List<String> getScripts() {
        return scripts == null ? Collections.emptyList() : Collections.unmodifiableList(scripts);
    }

    public void setScripts(List<String> scripts) {
        this.scripts = scripts == null ? Collections.emptyList() : scripts;
    }

    public String getBindings() {
        return bindings;
    }

    public void setBindings(String bindings) {
        this.bindings = bindings;
    }

    public Map<String, Object> getLegacy() {
        return legacy == null ? Collections.emptyMap() : Collections.unmodifiableMap(legacy);
    }

    public void setLegacy(Map<String, Object> legacy) {
        this.legacy = legacy == null ? Collections.emptyMap() : legacy;
    }

    public String document(String key) {
        return getDocuments().get(key);
    }
}
