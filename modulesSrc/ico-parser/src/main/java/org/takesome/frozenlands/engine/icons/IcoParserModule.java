package org.takesome.frozenlands.engine.icons;

import org.takesome.frozenlands.engine.EngineContext;
import org.takesome.frozenlands.engine.icons.selection.IcoImageSelector;
import org.takesome.frozenlands.engine.modules.EngineModule;
import org.takesome.frozenlands.engine.modules.ModuleCommand;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IcoParserModule implements EngineModule {
    public static final String MODULE_ID = "engine.icoParser";

    private final IcoFileParser parser = new IcoFileParser();
    private final Map<String, ModuleCommand> commands;
    private EngineContext context;

    public IcoParserModule() {
        Map<String, ModuleCommand> map = new LinkedHashMap<>();
        map.put("status", ModuleCommand.of("status", "Return IcoParser module status.", this::status));
        map.put("inspect", ModuleCommand.of("inspect", "Decode an ICO file and return frame metadata.", this::inspect));
        map.put("best", ModuleCommand.of("best", "Decode an ICO file and return the best matching frame metadata.", this::best));
        this.commands = Map.copyOf(map);
    }

    @Override
    public String id() {
        return MODULE_ID;
    }

    @Override
    public String description() {
        return "ICO parser module for desktop/window icon pipelines.";
    }

    @Override
    public Map<String, ModuleCommand> commands() {
        return commands;
    }

    @Override
    public void onRegister(EngineContext context) {
        this.context = context;
    }

    private Map<String, Object> status(Map<String, Object> arguments) {
        return Map.of(
                "ok", true,
                "module", MODULE_ID,
                "formats", List.of("ico", "png-backed-ico", "dib-backed-ico"),
                "commands", commands.keySet()
        );
    }

    private Map<String, Object> inspect(Map<String, Object> arguments) {
        try {
            IcoImageSet set = parser.parseSet(resolvePath(arguments));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("count", set.images().length);
            result.put("sizes", set.availableSizes().stream().map(IcoParserModule::dimensionMap).toList());
            result.put("frames", set.info().stream().map(IcoParserModule::infoMap).toList());
            return result;
        } catch (IOException | RuntimeException error) {
            return error(error);
        }
    }

    private Map<String, Object> best(Map<String, Object> arguments) {
        try {
            IcoImageSet set = parser.parseSet(resolvePath(arguments));
            int targetWidth = intArgument(arguments, "width", 256);
            int targetHeight = intArgument(arguments, "height", targetWidth);
            BufferedImage best = IcoImageSelector.getBestMatchingIcon(set.images(), targetWidth, targetHeight);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ok", true);
            result.put("target", Map.of("width", targetWidth, "height", targetHeight));
            result.put("available", set.availableSizes().stream().map(IcoParserModule::dimensionMap).toList());
            result.put("best", best == null ? null : imageMap(best));
            return result;
        } catch (IOException | RuntimeException error) {
            return error(error);
        }
    }

    private Path resolvePath(Map<String, Object> arguments) throws IOException {
        Object pathValue = arguments == null ? null : arguments.get("path");
        if (pathValue == null || String.valueOf(pathValue).isBlank()) {
            throw new IOException("Missing required argument: path");
        }
        Path path = Path.of(String.valueOf(pathValue));
        if (!path.isAbsolute()) {
            path = Path.of("").toAbsolutePath().resolve(path).normalize();
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException("ICO file not found: " + path);
        }
        return path;
    }

    private static int intArgument(Map<String, Object> arguments, String key, int fallback) {
        Object value = arguments == null ? null : arguments.get(key);
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return Math.max(0, Integer.parseInt(String.valueOf(value)));
    }

    private static Map<String, Object> infoMap(IcoImageInfo info) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("width", info.width());
        map.put("height", info.height());
        map.put("bitDepth", info.bitDepth());
        map.put("colors", info.colors());
        map.put("format", info.format());
        map.put("dataSize", info.dataSize());
        return map;
    }

    private static Map<String, Object> dimensionMap(Dimension dimension) {
        return Map.of("width", dimension.width, "height", dimension.height);
    }

    private static Map<String, Object> imageMap(BufferedImage image) {
        return Map.of(
                "width", image.getWidth(),
                "height", image.getHeight(),
                "type", image.getType(),
                "alpha", image.getColorModel().hasAlpha()
        );
    }

    private static Map<String, Object> error(Exception error) {
        return Map.of(
                "ok", false,
                "type", error.getClass().getSimpleName(),
                "message", error.getMessage() == null ? "" : error.getMessage()
        );
    }
}
