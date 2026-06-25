package dev.takesome.helix.ui.workspace.menu;

/**
 * Boundary between generic menu UI and the owning workspace/application.
 */
public interface WorkspaceMenuActions {
    WorkspaceMenuActions NO_OP = new WorkspaceMenuActions() {
        @Override
        public void createProject() {
        }

        @Override
        public void openProjectList() {
        }

        @Override
        public void openSettings() {
        }

        @Override
        public void requestShutdown() {
        }
    };

    void createProject();

    void openProjectList();

    void openSettings();

    void requestShutdown();
}
