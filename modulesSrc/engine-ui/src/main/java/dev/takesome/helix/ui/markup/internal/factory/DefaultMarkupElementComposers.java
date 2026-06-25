package dev.takesome.helix.ui.markup.internal.factory;

import dev.takesome.helix.ui.node.Node;

final class ContainerMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupContainerFactory containers;
    ContainerMarkupElementComposer(UiMarkupContainerFactory containers) { this.containers = containers; }
    public String id() { return "container"; }
    public Node compose(UiMarkupComposeContext context) {
        return containers.container(context.element(), context.style(), context.parentW(), context.parentH(), context.nodes());
    }
}

final class PanelMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupContainerFactory containers;
    PanelMarkupElementComposer(UiMarkupContainerFactory containers) { this.containers = containers; }
    public String id() { return "panel"; }
    public Node compose(UiMarkupComposeContext context) {
        return containers.panel(context.element(), context.style(), context.parentW(), context.parentH(), context.nodes());
    }
}


final class ElementKindMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupContainerFactory containers;
    private final String kind;

    ElementKindMarkupElementComposer(UiMarkupContainerFactory containers, String kind) {
        this.containers = containers;
        this.kind = kind;
    }

    public String id() { return kind; }

    public Node compose(UiMarkupComposeContext context) {
        return containers.element(context.element(), context.style(), context.parentW(), context.parentH(), kind);
    }
}

final class ImageMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupImageFactory images;
    ImageMarkupElementComposer(UiMarkupImageFactory images) { this.images = images; }
    public String id() { return "image"; }
    public Node compose(UiMarkupComposeContext context) {
        return images.image(context.element(), context.style(), context.parentW(), context.parentH());
    }
}

final class IconMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupIconFactory icons;
    IconMarkupElementComposer(UiMarkupIconFactory icons) { this.icons = icons; }
    public String id() { return "icon"; }
    public Node compose(UiMarkupComposeContext context) {
        return icons.icon(context.element(), context.style(), context.parentW(), context.parentH());
    }
}

final class TextMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupTextFactory text;
    TextMarkupElementComposer(UiMarkupTextFactory text) { this.text = text; }
    public String id() { return "text"; }
    public Node compose(UiMarkupComposeContext context) {
        return text.label(context.element(), context.style(), context.parentW(), context.parentH(), context.nodes());
    }
}

final class ButtonMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupButtonFactory buttons;
    ButtonMarkupElementComposer(UiMarkupButtonFactory buttons) { this.buttons = buttons; }
    public String id() { return "button"; }
    public Node compose(UiMarkupComposeContext context) {
        return buttons.button(context.element(), context.style(), context.parentW(), context.parentH(), context.nodes());
    }
}

final class InputMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupInputFactory inputs;
    InputMarkupElementComposer(UiMarkupInputFactory inputs) { this.inputs = inputs; }
    public String id() { return "in" + "put"; }
    public Node compose(UiMarkupComposeContext context) {
        String type = context.element().attribute("type", "").trim();
        if (("check" + "box").equalsIgnoreCase(type)) return inputs.checkbox(context.element(), context.style(), context.parentW(), context.parentH(), context.nodes());
        if ("range".equalsIgnoreCase(type)) return inputs.slider(context.element(), context.style(), context.parentW(), context.parentH(), context.nodes());
        return inputs.input(context.element(), context.style(), context.parentW(), context.parentH());
    }
}

final class SliderMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupInputFactory inputs;

    SliderMarkupElementComposer(UiMarkupInputFactory inputs) { this.inputs = inputs; }

    public String id() { return "slider"; }

    public Node compose(UiMarkupComposeContext context) {
        return inputs.slider(context.element(), context.style(), context.parentW(), context.parentH(), context.nodes());
    }
}

final class ComboBoxMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupInputFactory inputs;

    ComboBoxMarkupElementComposer(UiMarkupInputFactory inputs) { this.inputs = inputs; }

    public String id() { return "combo_box"; }

    public Node compose(UiMarkupComposeContext context) {
        return inputs.comboBox(context.element(), context.style(), context.parentW(), context.parentH(), context.nodes());
    }
}

final class InputCaptureMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupInputFactory inputs;

    InputCaptureMarkupElementComposer(UiMarkupInputFactory inputs) { this.inputs = inputs; }

    public String id() { return "input_capture"; }

    public Node compose(UiMarkupComposeContext context) {
        return inputs.inputCapture(context.element(), context.style(), context.parentW(), context.parentH());
    }
}

final class CheckboxMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupInputFactory inputs;

    CheckboxMarkupElementComposer(UiMarkupInputFactory inputs) { this.inputs = inputs; }

    public String id() { return "checkbox"; }

    public Node compose(UiMarkupComposeContext context) {
        return inputs.checkbox(context.element(), context.style(), context.parentW(), context.parentH(), context.nodes());
    }
}

final class MenuListMarkupElementComposer implements UiMarkupElementComposer {
    private final UiMarkupMenuListFactory menuLists;

    MenuListMarkupElementComposer(UiMarkupMenuListFactory menuLists) { this.menuLists = menuLists; }

    public String id() { return "menu-list"; }

    public Node compose(UiMarkupComposeContext context) {
        return menuLists.menuList(context.element(), context.style(), context.parentW(), context.parentH(), context.nodes());
    }
}


final class StyleMarkupElementComposer implements UiMarkupElementComposer {
    public String id() { return "style"; }

    public Node compose(UiMarkupComposeContext context) {
        return null;
    }
}
