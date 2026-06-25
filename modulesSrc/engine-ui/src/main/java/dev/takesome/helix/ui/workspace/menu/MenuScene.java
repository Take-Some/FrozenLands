package dev.takesome.helix.ui.workspace.menu;

import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.uiComponents.button.UiButtonNode;
import dev.takesome.helix.ui.uiComponents.label.UiLabelNode;
import dev.takesome.helix.ui.uiComponents.panel.UiPanelNode;
import dev.takesome.helix.ui.node.ContainerNode;
import dev.takesome.helix.ui.node.SceneNode;
import dev.takesome.helix.ui.scene.NodeScene;

/**
 * Retained-mode workspace menu scene.
 */
public final class MenuScene extends NodeScene {
    private static final float DEFAULT_WIDTH = 1280f;
    private static final float DEFAULT_HEIGHT = 720f;

    private final WorkspaceMenuActions actions;
    private final float width;
    private final float height;

    public MenuScene(WorkspaceMenuActions actions) {
        this(actions, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public MenuScene(WorkspaceMenuActions actions, float width, float height) {
        this.actions = actions == null ? WorkspaceMenuActions.NO_OP : actions;
        this.width = Math.max(1f, width);
        this.height = Math.max(1f, height);
    }

    @Override
    protected SceneNode buildSceneRoot() {
        SceneNode root = new SceneNode(width, height);

        UiPanelNode background = new UiPanelNode(new UiColor(0.025f, 0.027f, 0.035f, 1f));
        background.setBounds(0f, 0f, width, height);

        UiLabelNode logo = new UiLabelNode("NorthStar", 2.15f, UiColor.WHITE, TextAlign.CENTER);
        logo.setBounds(width * 0.5f - 240f, height - 170f, 480f, 80f);

        ContainerNode menu = buildMenuContainer();

        UiLabelNode footer = new UiLabelNode("NorthStar Suite V2", 0.82f, new UiColor(0.72f, 0.72f, 0.78f, 0.9f), TextAlign.LEFT);
        footer.setBounds(32f, 32f, 480f, 32f);

        root.add(background);
        root.add(logo);
        root.add(menu);
        root.add(footer);

        return root;
    }

    private ContainerNode buildMenuContainer() {
        float buttonWidth = 420f;
        float buttonHeight = 56f;
        float gap = 16f;
        float totalHeight = buttonHeight * 4f + gap * 3f;

        ContainerNode menu = new ContainerNode();
        menu.setBounds(
                width * 0.5f - buttonWidth * 0.5f,
                height * 0.5f - totalHeight * 0.5f - 24f,
                buttonWidth,
                totalHeight
        );

        UiButtonNode newProject = button("New Project", 0f);
        newProject.setOnClick(new Runnable() {
            @Override
            public void run() {
                actions.createProject();
            }
        });

        UiButtonNode openProject = button("Open Project", buttonHeight + gap);
        openProject.setOnClick(new Runnable() {
            @Override
            public void run() {
                actions.openProjectList();
            }
        });

        UiButtonNode settings = button("Settings", (buttonHeight + gap) * 2f);
        settings.setOnClick(new Runnable() {
            @Override
            public void run() {
                actions.openSettings();
            }
        });

        UiButtonNode shutdown = button("Exit", (buttonHeight + gap) * 3f);
        shutdown.setOnClick(new Runnable() {
            @Override
            public void run() {
                actions.requestShutdown();
            }
        });

        menu.add(newProject);
        menu.add(openProject);
        menu.add(settings);
        menu.add(shutdown);

        return menu;
    }

    private UiButtonNode button(String label, float y) {
        UiButtonNode button = new UiButtonNode(label);
        button.setBounds(0f, y, 420f, 56f);
        return button;
    }
}
