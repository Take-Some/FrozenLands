package dev.takesome.helix.ui.crash;

import dev.takesome.helix.logging.EngineLog;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Resource-backed markup rendering for the crash window. */
final class CrashWindowTemplates {
    private static final Logger LOG = EngineLog.logger(CrashWindowTemplates.class);

    private CrashWindowTemplates() {
    }

    static String render(CrashWindowModel model) {
        return resource(CrashWindowLayout.TEMPLATE)
                .replace("{{css}}", resource(CrashWindowLayout.CSS_TEMPLATE))
                .replace("{{title}}", xml(model.title()))
                .replace("{{summary}}", xml(model.summary()))
                .replace("{{phase}}", xml(model.phase()))
                .replace("{{context}}", xml(model.context()))
                .replace("{{exceptionType}}", xml(model.exceptionType()))
                .replace("{{fingerprint}}", xml(model.fingerprint()))
                .replace("{{fingerprintShort}}", xml(model.fingerprintShort()))
                .replace("{{module}}", xml(model.module()))
                .replace("{{subsystem}}", xml(model.subsystem()))
                .replace("{{timestamp}}", xml(model.timestamp()))
                .replace("{{errorCode}}", xml(model.errorCode()))
                .replace("{{build}}", xml(model.build()))
                .replace("{{memory}}", xml(model.memory()))
                .replace("{{memoryProgressWidth}}", xml(model.memoryProgressWidth()))
                .replace("{{count}}", xml(model.count()))
                .replace("{{reportPath}}", xml(model.reportPathDisplay()));
    }

    private static String resource(String path) {
        try (InputStream in = CrashWindowTemplates.class.getResourceAsStream(path)) {
            if (in == null) {
                LOG.warn("Missing crash window resource: {}", path);
                throw new IllegalStateException("Missing crash UI template resource: " + path);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            String value = out.toString(StandardCharsets.UTF_8);
            LOG.debug("Loaded crash window resource: {} ({} bytes)", path, value.length());
            return value;
        } catch (Exception error) {
            LOG.warn("Failed to load crash window resource: {}", path, error);
            throw new IllegalStateException("Failed to load crash UI template: " + path, error);
        }
    }

    private static String xml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
