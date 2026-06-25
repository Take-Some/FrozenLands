package dev.takesome.helix.ui.workspace.menu;

import dev.takesome.helix.ui.input.UiInputEvent;
import dev.takesome.helix.ui.model.TextAlign;
import dev.takesome.helix.ui.model.UiColor;
import dev.takesome.helix.ui.uiComponents.label.UiLabelNode;
import dev.takesome.helix.ui.uiComponents.button.UiButtonNode;
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
    private static final float MENU_BUTTON_WIDTH = 420f;
    private static final float MENU_BUTTON_HEIGHT = 56f;
    private static final float MENU_BUTTON_GAP = 16f;

    private final WorkspaceMenuActions actions;
    private final float width;
    private final float height;

    private ExitConfirmationOverlayNode exitConfirmationOverlay;

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

        exitConfirmationOverlay = buildExitConfirmationOverlay();

        root.add(background);
        root.add(logo);
        root.add(menu);
        root.add(footer);
        root.add(exitConfirmationOverlay);

        return root;
    }

    private ContainerNode buildMenuContainer() {
        float totalHeight = MENU_BUTTON_HEIGHT * 4f + MENU_BUTTON_GAP * 3f;

        ContainerNode menu = new ContainerNode();
        menu.setBounds(
                width * 0.5f - MENU_BUTTON_WIDTH * 0.5f,
                height * 0.5f - totalHeight * 0.5f - 24f,
                MENU_BUTTON_WIDTH,
                totalHeight
        );

        UiButtonNode newProject = button("New Project", 0f);
        newProject.setOnClick(new Runnable() {
            @Override
            public void run() {
                actions.createProject();
            }
        });

        UiButtonNode openProject = button("Open Project", MENU_BUTTON_HEIGHT + MENU_BUTTON_GAP);
        openProject.setOnClick(new Runnable() {
            @Override
            public void run() {
                actions.openProjectList();
            }
        });

        UiButtonNode settings = button("Settings", (MENU_BUTTON_HEIGHT + MENU_BUTTON_GAP) * 2f);
        settings.setOnClick(new Runnable() {
            @Override
            public void run() {
                actions.openSettings();
            }
        });

        UiButtonNode shutdown = button("Exit", (MENU_BUTTON_HEIGHT + MENU_BUTTON_GAP) * 3f);
        shutdown.setOnClick(new Runnable() {
            @Override
            public void run() {
                showExitConfirmation();
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
        button.setBounds(0f, y, MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
        return button;
    }

    private ExitConfirmationOverlayNode buildExitConfirmationOverlay() {
        float dialogWidth = Math.min(500f, Math.max(360f, width - 96f));
        float dialogHeight = 250f;
        float dialogX = width * 0.5f - dialogWidth * 0.5f;
        float dialogY = height * 0.5f - dialogHeight * 0.5f;

        ExitConfirmationOverlayNode overlay = new ExitConfirmationOverlayNode();
        overlay.setBounds(0f, 0f, width, height);
        overlay.setVisible(false);
        overlay.setEnabled(false);

        UiPanelNode dim = new UiPanelNode(new UiColor(0f, 0f, 0f, 0.62f));
        dim.setBounds(0f, 0f, width, height);

        UiPanelNode dialog = new UiPanelNode(new UiColor(0.055f, 0.058f, 0.072f, 1f));
        dialog.setBounds(dialogX, dialogY, dialogWidth, dialogHeight);
        dialog.setBorder(new UiColor(0.44f, 0.72f, 0.95f, 0.92f), 1.25f);
        dialog.setShadow(new UiColor(0f, 0f, 0f, 0.55f), 0f, -8f, 20f, 3f);

        UiLabelNode title = new UiLabelNode("Confirm exit", 1.22f, UiColor.WHITE, TextAlign.CENTER);
        title.setBounds(32f, dialogHeight - 74f, dialogWidth - 64f, 42f);

        UiLabelNode message = new UiLabelNode("Exit FrozenLands?", 0.88f, new UiColor(0.76f, 0.80f, 0.88f, 0.96f), TextAlign.CENTER);
        message.setBounds(42f, dialogHeight - 128f, dialogWidth - 84f, 34f);

        UiButtonNode cancel = modalButton("Cancel", dialogWidth * 0.5f - 176f, 38f, 160f, 48f, false);
        cancel.setOnClick(new Runnable() {
            @Override
            public void run() {
                hideExitConfirmation();
            }
        });

        UiButtonNode confirm = modalButton("Exit", dialogWidth * 0.5f + 16f, 38f, 160f, 48f, true);
        confirm.setOnClick(new Runnable() {
            @Override
            public void run() {
                actions.requestShutdown();
            }
        });

        dialog.add(title);
        dialog.add(message);
        dialog.add(cancel);
        dialog.add(confirm);

        overlay.add(dim);
        overlay.add(dialog);
        return overlay;
    }

    private UiButtonNode modalButton(String label, float x, float y, float w, float h, boolean destructive) {
        UiButtonNode button = new UiButtonNode(label);
        button.setBounds(x, y, w, h);
        button.setFontScale(0.86f);
        if (destructive) {
            button.setColors(
                    new UiColor(0.42f, 0.075f, 0.095f, 1f),
                    new UiColor(0.57f, 0.10f, 0.13f, 1f),
                    new UiColor(0.34f, 0.055f, 0.075f, 1f),
                    new UiColor(0.16f, 0.12f, 0.13f, 0.65f),
                    UiColor.WHITE
            );
            button.setBorder(new UiColor(0.96f, 0.35f, 0.40f, 0.92f), 1f);
        } else {
            button.setColors(
                    new UiColor(0.10f, 0.12f, 0.15f, 1f),
                    new UiColor(0.15f, 0.18f, 0.22f, 1f),
                    new UiColor(0.075f, 0.09f, 0.12f, 1f),
                    new UiColor(0.10f, 0.10f, 0.11f, 0.65f),
                    new UiColor(0.88f, 0.91f, 0.96f, 1f)
            );
            button.setBorder(new UiColor(0.46f, 0.52f, 0.62f, 0.88f), 1f);
        }
        return button;
    }

    private void showExitConfirmation() {
        if (exitConfirmationOverlay == null) return;
        exitConfirmationOverlay.setEnabled(true);
        exitConfirmationOverlay.setVisible(true);
    }

    private void hideExitConfirmation() {
        if (exitConfirmationOverlay == null) return;
        exitConfirmationOverlay.setEnabled(false);
        exitConfirmationOverlay.setVisible(false);
    }

    /**
     * Retained overlay barrier: it is part of the already-mounted menu tree and
     * does not request a scene/content rebuild when confirmation is shown.
     */
    private static final class ExitConfirmationOverlayNode extends ContainerNode {
        @Override
        protected boolean onInput(UiInputEvent event) {
            return event != null && event.isPointerEvent();
        }
    }
}
