package dev.takesome.helix.devTools;

import javax.swing.SwingUtilities;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

/** Entrypoint for the engine-ui-owned DevTools child process. */
public final class HtmlDevToolsChildMain {
    private HtmlDevToolsChildMain() { }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("port and token are required");
        int port = Integer.parseInt(args[0]);
        String token = args[1];
        System.setProperty("helix.ui.devToolsChildProcess", "false");
        Socket socket = new Socket("127.0.0.1", port);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        out.writeObject(token);
        out.flush();

        HtmlDevToolsRuntime.setRemoteActionSink(action -> {
            synchronized (out) {
                try {
                    out.writeObject(action);
                    out.flush();
                    out.reset();
                } catch (Exception ex) {
                    System.exit(0);
                }
            }
        });

        SwingUtilities.invokeLater(() -> HtmlDevToolsWindow.open(HtmlInspectionTarget.empty()));
        try {
            while (true) {
                Object next = in.readObject();
                if (next instanceof HtmlDevToolsSnapshot snapshot) {
                    HtmlDevToolsRuntime.setRemoteSnapshot(snapshot);
                    SwingUtilities.invokeLater(() -> HtmlDevToolsWindow.refresh(HtmlInspectionTarget.empty()));
                }
            }
        } finally {
            HtmlDevToolsRuntime.setRemoteActionSink(null);
            HtmlDevToolsRuntime.clearRemoteSnapshot();
            socket.close();
            System.exit(0);
        }
    }
}
