package dev.takesome.helix.devTools;

import dev.takesome.helix.logging.EngineLog;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Owns the external DevTools JVM. The process is an engine-ui child and dies with the parent. */
final class HtmlDevToolsProcessHost implements Closeable {
    private static final Logger LOG = EngineLog.logger(HtmlDevToolsProcessHost.class);

    private final Consumer<HtmlDevToolsRemoteAction> actions;
    private Process process;
    private ServerSocket server;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread readerThread;
    private Thread shutdownHook;
    private volatile boolean closed;

    HtmlDevToolsProcessHost(Consumer<HtmlDevToolsRemoteAction> actions) {
        this.actions = actions;
    }

    boolean start() {
        if (open()) return true;
        try {
            closed = false;
            String token = UUID.randomUUID().toString();
            server = new ServerSocket(0);
            server.setSoTimeout(7000);
            ProcessBuilder builder = new ProcessBuilder(command(server.getLocalPort(), token));
            builder.redirectError(ProcessBuilder.Redirect.INHERIT);
            process = builder.start();
            socket = server.accept();
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            Object hello = in.readObject();
            if (!token.equals(hello)) throw new IllegalStateException("DevTools child handshake token mismatch");
            readerThread = new Thread(this::readActions, "helix-devtools-child-actions");
            readerThread.setDaemon(true);
            readerThread.start();
            shutdownHook = new Thread(this::closeQuietly, "helix-devtools-child-shutdown");
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            LOG.info("HTML DevTools child process started pid={}", process.pid());
            return true;
        } catch (Exception ex) {
            LOG.warn("HTML DevTools child process start failed: {}", ex.getMessage(), ex);
            closeQuietly();
            return false;
        }
    }

    boolean open() {
        return process != null && process.isAlive() && !closed;
    }

    synchronized void send(HtmlDevToolsSnapshot snapshot) {
        if (!open() || out == null) return;
        try {
            out.writeObject(snapshot == null ? HtmlDevToolsSnapshot.empty(HtmlDevToolsSession.closed()) : snapshot);
            out.flush();
            out.reset();
        } catch (Exception ex) {
            LOG.warn("HTML DevTools child snapshot send failed: {}", ex.getMessage(), ex);
            closeQuietly();
        }
    }

    @Override
    public void close() {
        closeQuietly();
    }

    private void readActions() {
        while (open()) {
            try {
                Object next = in.readObject();
                if (next instanceof HtmlDevToolsRemoteAction action && actions != null) actions.accept(action);
            } catch (Exception ex) {
                if (!closed) LOG.debug("HTML DevTools child action stream closed: {}", ex.getMessage());
                closeQuietly();
                return;
            }
        }
    }

    private List<String> command(int port, String token) {
        String javaHome = System.getProperty("java.home");
        String exe = isWindows() ? "java.exe" : "java";
        String java = Path.of(javaHome, "bin", exe).toString();
        String classpath = System.getProperty("java.class.path", "");
        return List.of(java, "-cp", classpath, HtmlDevToolsChildMain.class.getName(), String.valueOf(port), token);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private synchronized void closeQuietly() {
        closed = true;
        try { if (in != null) in.close(); } catch (Exception ignored) { }
        try { if (out != null) out.close(); } catch (Exception ignored) { }
        try { if (socket != null) socket.close(); } catch (Exception ignored) { }
        try { if (server != null) server.close(); } catch (Exception ignored) { }
        if (process != null && process.isAlive()) process.destroy();
        if (shutdownHook != null) {
            try { Runtime.getRuntime().removeShutdownHook(shutdownHook); } catch (IllegalStateException ignored) { }
        }
        process = null;
        server = null;
        socket = null;
        out = null;
        in = null;
    }
}
