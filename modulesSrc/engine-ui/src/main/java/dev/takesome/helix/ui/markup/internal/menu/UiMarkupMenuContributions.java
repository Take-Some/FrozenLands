package dev.takesome.helix.ui.markup.internal.menu;

import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.dom.UiDomNode;
import dev.takesome.helix.ui.markup.internal.parse.UiMarkupXmlParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class UiMarkupMenuContributions {
    private UiMarkupMenuContributions() {
    }

    public static List<UiDomElement> load(String slot) {
        ArrayList<UiDomElement> result = new ArrayList<>();
        if (slot == null || slot.isBlank()) return result;

        java.io.File dir = new java.io.File(System.getProperty("helix.modules.dir", "modules"));
        java.io.File[] files = dir.listFiles();
        if (files == null) return result;

        String entryName = "META-INF/helix/menu/" + slot.trim() + ".ui.html";
        UiMarkupXmlParser parser = new UiMarkupXmlParser();

        for (java.io.File file : files) {
            if (file == null || !file.isFile() || !file.getName().endsWith(".jar")) continue;
            try (JarFile jar = new JarFile(file)) {
                JarEntry entry = jar.getJarEntry(entryName);
                if (entry == null) continue;
                try (InputStream input = jar.getInputStream(entry)) {
                    String source = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (source.isEmpty()) continue;
                    UiDomElement root = authoredRoot(parser.parse(source).dom());
                    if ("menu-contribution".equals(root.tagName()) || "fragment".equals(root.tagName()) || "buttons".equals(root.tagName())) {
                        result.addAll(elementChildren(root));
                    } else {
                        result.add(root);
                    }
                }
            } catch (Exception ignored) {
                // Optional module contribution is invalid; keep base menu alive.
            }
        }

        return result;
    }

    private static UiDomElement authoredRoot(UiDomDocument document) {
        UiDomElement renderRoot = document.renderRoot();
        if ("body".equals(renderRoot.tagName())) {
            List<UiDomElement> children = elementChildren(renderRoot);
            if (children.size() == 1) return children.get(0);
        }
        return renderRoot;
    }

    private static List<UiDomElement> elementChildren(UiDomElement parent) {
        if (parent == null || parent.childCount() == 0) return List.of();
        ArrayList<UiDomElement> out = new ArrayList<>();
        for (UiDomNode child : parent.children()) {
            if (child instanceof UiDomElement element) out.add(element);
        }
        return out;
    }
}
