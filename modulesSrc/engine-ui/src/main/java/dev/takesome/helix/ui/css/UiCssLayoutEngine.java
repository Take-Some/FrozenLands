package dev.takesome.helix.ui.css;


import static dev.takesome.helix.validation.EngineValidator.textOrEmpty;
import dev.takesome.helix.logging.EngineLog;
import dev.takesome.helix.ui.css.properties.layout.AlignItemsCssProperty;
import dev.takesome.helix.ui.css.properties.layout.AlignSelfCssProperty;
import dev.takesome.helix.ui.css.properties.layout.BottomCssProperty;
import dev.takesome.helix.ui.css.properties.layout.BoundsCssProperty;
import dev.takesome.helix.ui.css.properties.layout.BoxSizingCssProperty;
import dev.takesome.helix.ui.css.properties.layout.DisplayCssProperty;
import dev.takesome.helix.ui.css.properties.layout.FlexBasisCssProperty;
import dev.takesome.helix.ui.css.properties.layout.FlexCssProperty;
import dev.takesome.helix.ui.css.properties.layout.FlexDirectionCssProperty;
import dev.takesome.helix.ui.css.properties.layout.FlexGrowCssProperty;
import dev.takesome.helix.ui.css.properties.layout.FlexShrinkCssProperty;
import dev.takesome.helix.ui.css.properties.layout.FlexWrapCssProperty;
import dev.takesome.helix.ui.css.properties.layout.GapCssProperty;
import dev.takesome.helix.ui.css.properties.layout.HeightCssProperty;
import dev.takesome.helix.ui.css.properties.layout.JustifyContentCssProperty;
import dev.takesome.helix.ui.css.properties.layout.LayoutXCssProperty;
import dev.takesome.helix.ui.css.properties.layout.LeftCssProperty;
import dev.takesome.helix.ui.css.properties.layout.MarginBottomCssProperty;
import dev.takesome.helix.ui.css.properties.layout.MarginCssProperty;
import dev.takesome.helix.ui.css.properties.layout.MarginLeftCssProperty;
import dev.takesome.helix.ui.css.properties.layout.MarginRightCssProperty;
import dev.takesome.helix.ui.css.properties.layout.MarginTopCssProperty;
import dev.takesome.helix.ui.css.properties.layout.MaxHeightCssProperty;
import dev.takesome.helix.ui.css.properties.layout.MaxWidthCssProperty;
import dev.takesome.helix.ui.css.properties.layout.MinHeightCssProperty;
import dev.takesome.helix.ui.css.properties.layout.MinWidthCssProperty;
import dev.takesome.helix.ui.css.properties.layout.PaddingBottomCssProperty;
import dev.takesome.helix.ui.css.properties.layout.PaddingCssProperty;
import dev.takesome.helix.ui.css.properties.layout.PaddingLeftCssProperty;
import dev.takesome.helix.ui.css.properties.layout.PaddingRightCssProperty;
import dev.takesome.helix.ui.css.properties.layout.PaddingTopCssProperty;
import dev.takesome.helix.ui.css.properties.layout.PositionCssProperty;
import dev.takesome.helix.ui.css.properties.layout.ResolvedVerticalCssProperty;
import dev.takesome.helix.ui.css.properties.layout.RightCssProperty;
import dev.takesome.helix.ui.css.properties.layout.TopCssProperty;
import dev.takesome.helix.ui.css.properties.layout.WidthCssProperty;
import dev.takesome.helix.ui.css.properties.layout.XCssProperty;
import dev.takesome.helix.ui.css.properties.layout.YCssProperty;
import dev.takesome.helix.ui.css.units.UiCssUnitResolutionContext;
import dev.takesome.helix.ui.dom.UiDomDocument;
import dev.takesome.helix.ui.dom.UiDomElement;
import dev.takesome.helix.ui.dom.UiDomNode;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Resolves registered CSS layout definitions into absolute pixel boxes. */
public final class UiCssLayoutEngine {
    private static final Logger LOG = EngineLog.logger(UiCssLayoutEngine.class);
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
    private static final Set<String> DEBUGGED = ConcurrentHashMap.newKeySet();

    private UiCssUnitResolutionContext lengthContext = UiCssUnitResolutionContext.defaults();

    private final DisplayCssProperty display;
    private final PositionCssProperty position;
    private final FlexDirectionCssProperty flexDirection;
    private final BoundsCssProperty bounds;
    private final XCssProperty x;
    private final YCssProperty y;
    private final LeftCssProperty left;
    private final TopCssProperty top;
    private final RightCssProperty right;
    private final BottomCssProperty bottom;
    private final WidthCssProperty width;
    private final HeightCssProperty height;
    private final MinWidthCssProperty minWidth;
    private final MinHeightCssProperty minHeight;
    private final MaxWidthCssProperty maxWidth;
    private final MaxHeightCssProperty maxHeight;
    private final BoxSizingCssProperty boxSizing;
    private final MarginCssProperty margin;
    private final MarginLeftCssProperty marginLeft;
    private final MarginRightCssProperty marginRight;
    private final MarginTopCssProperty marginTop;
    private final MarginBottomCssProperty marginBottom;
    private final PaddingCssProperty padding;
    private final PaddingLeftCssProperty paddingLeft;
    private final PaddingRightCssProperty paddingRight;
    private final PaddingTopCssProperty paddingTop;
    private final PaddingBottomCssProperty paddingBottom;
    private final GapCssProperty gap;
    private final JustifyContentCssProperty justifyContent;
    private final AlignItemsCssProperty alignItems;
    private final AlignSelfCssProperty alignSelf;
    private final FlexCssProperty flex;
    private final FlexGrowCssProperty flexGrow;
    private final FlexShrinkCssProperty flexShrink;
    private final FlexBasisCssProperty flexBasis;
    private final FlexWrapCssProperty flexWrap;
    private final LayoutXCssProperty layoutX;
    private final ResolvedVerticalCssProperty layoutY;
    private final UiIntrinsicTextMeasurer textMeasurer;

    public UiCssLayoutEngine() {
        this(UiCssPropertyRegistry.loadBuiltins(), UiIntrinsicTextMeasurer.heuristic());
    }

    public UiCssLayoutEngine(UiCssPropertyRegistry registry) {
        this(registry, UiIntrinsicTextMeasurer.heuristic());
    }

    public UiCssLayoutEngine(UiCssPropertyRegistry registry, UiIntrinsicTextMeasurer textMeasurer) {
        Objects.requireNonNull(registry, "registry");
        this.textMeasurer = textMeasurer == null ? UiIntrinsicTextMeasurer.heuristic() : textMeasurer;
        this.display = registry.requireType(DisplayCssProperty.class);
        this.position = registry.requireType(PositionCssProperty.class);
        this.flexDirection = registry.requireType(FlexDirectionCssProperty.class);
        this.bounds = registry.requireType(BoundsCssProperty.class);
        this.x = registry.requireType(XCssProperty.class);
        this.y = registry.requireType(YCssProperty.class);
        this.left = registry.requireType(LeftCssProperty.class);
        this.top = registry.requireType(TopCssProperty.class);
        this.right = registry.requireType(RightCssProperty.class);
        this.bottom = registry.requireType(BottomCssProperty.class);
        this.width = registry.requireType(WidthCssProperty.class);
        this.height = registry.requireType(HeightCssProperty.class);
        this.minWidth = registry.requireType(MinWidthCssProperty.class);
        this.minHeight = registry.requireType(MinHeightCssProperty.class);
        this.maxWidth = registry.requireType(MaxWidthCssProperty.class);
        this.maxHeight = registry.requireType(MaxHeightCssProperty.class);
        this.boxSizing = registry.requireType(BoxSizingCssProperty.class);
        this.margin = registry.requireType(MarginCssProperty.class);
        this.marginLeft = registry.requireType(MarginLeftCssProperty.class);
        this.marginRight = registry.requireType(MarginRightCssProperty.class);
        this.marginTop = registry.requireType(MarginTopCssProperty.class);
        this.marginBottom = registry.requireType(MarginBottomCssProperty.class);
        this.padding = registry.requireType(PaddingCssProperty.class);
        this.paddingLeft = registry.requireType(PaddingLeftCssProperty.class);
        this.paddingRight = registry.requireType(PaddingRightCssProperty.class);
        this.paddingTop = registry.requireType(PaddingTopCssProperty.class);
        this.paddingBottom = registry.requireType(PaddingBottomCssProperty.class);
        this.gap = registry.requireType(GapCssProperty.class);
        this.justifyContent = registry.requireType(JustifyContentCssProperty.class);
        this.alignItems = registry.requireType(AlignItemsCssProperty.class);
        this.alignSelf = registry.requireType(AlignSelfCssProperty.class);
        this.flex = registry.requireType(FlexCssProperty.class);
        this.flexGrow = registry.requireType(FlexGrowCssProperty.class);
        this.flexShrink = registry.requireType(FlexShrinkCssProperty.class);
        this.flexBasis = registry.requireType(FlexBasisCssProperty.class);
        this.flexWrap = registry.requireType(FlexWrapCssProperty.class);
        this.layoutX = registry.requireType(LayoutXCssProperty.class);
        this.layoutY = registry.requireType(ResolvedVerticalCssProperty.class);
    }

    public UiCssLayoutResult layout(UiDomDocument document, float viewportWidth, float viewportHeight) {
        Objects.requireNonNull(document, "document");
        UiCssLayoutResult result = new UiCssLayoutResult();
        UiDomElement root = document.root();
        float safeViewportW = Math.max(0f, viewportWidth);
        float safeViewportH = Math.max(0f, viewportHeight);
        float rootFontSize = rootFontSize(root, safeViewportW, safeViewportH);
        this.lengthContext = UiCssUnitResolutionContext.viewport(safeViewportW, safeViewportH, rootFontSize);
        debugOnce("length-context|" + safeViewportW + "x" + safeViewportH + "|" + rootFontSize,
                "UI CSS length context viewport={}x{} rootFontSize={}", safeViewportW, safeViewportH, rootFontSize);
        UiCssBox rootBox = resolveExplicitBox(root, null, 0f, 0f, safeViewportW, safeViewportH);
        writeBox(root, rootBox);
        result.put(root, rootBox);
        layoutChildren(root, rootBox, result);
        return result;
    }

    private void layoutChildren(UiDomElement parent, UiCssBox parentBox, UiCssLayoutResult result) {
        if (display.read(parent).hidden()) return;
        if (display.read(parent).flex()) layoutFlexChildren(parent, parentBox, result);
        else layoutFlowChildren(parent, parentBox, result);
    }

    private void layoutFlowChildren(UiDomElement parent, UiCssBox parentBox, UiCssLayoutResult result) {
        Flow flow = flow(parent);
        Insets insets = padding(parent, parentBox);
        float resolvedGap = gap.read(parent, UiCssLength.ZERO).resolve(lengthContext, flow == Flow.ROW ? parentBox.width() : parentBox.height(), 0f);
        float cursor = 0f;
        for (UiDomNode childNode : parent.children()) {
            if (!(childNode instanceof UiDomElement child)) continue;
            UiCssBox childBox = childBox(parentBox, insets, flow, cursor, child);
            Insets m = margin(child, parentBox);
            cursor += (flow == Flow.ROW ? childBox.width() + m.left + m.right : childBox.height() + m.top + m.bottom) + resolvedGap;
            commitChild(child, childBox, result);
        }
    }

    private void layoutFlexChildren(UiDomElement parent, UiCssBox parentBox, UiCssLayoutResult result) {
        Flow flow = flow(parent);
        Insets insets = padding(parent, parentBox);
        float mainSize = Math.max(0f, flow == Flow.ROW ? parentBox.width() - insets.left - insets.right : parentBox.height() - insets.top - insets.bottom);
        float crossSize = Math.max(0f, flow == Flow.ROW ? parentBox.height() - insets.top - insets.bottom : parentBox.width() - insets.left - insets.right);
        float resolvedGap = gap.read(parent, UiCssLength.ZERO).resolve(lengthContext, mainSize, 0f);
        List<FlexLine> lines = flexLines(parent, parentBox, flow, mainSize, crossSize, resolvedGap);
        String justify = justifyContent.read(parent);
        String align = alignItems.read(parent);
        float crossCursor = 0f;
        debugOnce("flex|" + summary(parent) + '|' + flow + '|' + justify + '|' + align,
                "UI flex layout parent={} flow={} lines={} justify='{}' align='{}' wrap='{}' main={} cross={} gap={}",
                summary(parent), flow, lines.size(), justify, align, flexWrap.read(parent), mainSize, crossSize, resolvedGap);
        for (FlexLine line : lines) {
            float free = mainSize - line.mainSize(flow, resolvedGap);
            line.applyFlex(free);
            float used = line.mainSize(flow, resolvedGap);
            float remaining = Math.max(0f, mainSize - used);
            float mainCursor = justifyOffset(justify, remaining, line.items.size());
            float itemGap = justifyGap(justify, remaining, resolvedGap, line.items.size());
            for (FlexItem item : line.items) {
                float cross = item.cross;
                String itemAlign = alignSelf(item.element, align);
                if ("stretch".equals(itemAlign)) cross = Math.max(0f, line.crossSize - item.crossMargin(flow));
                float crossOffset = alignOffset(itemAlign, Math.max(0f, line.crossSize - cross - item.crossMargin(flow)));
                float itemW = flow == Flow.ROW ? item.main : cross;
                float itemH = flow == Flow.ROW ? cross : item.main;
                float x0 = flow == Flow.ROW
                        ? parentBox.x() + insets.left + mainCursor + item.margin.left
                        : parentBox.x() + insets.left + crossCursor + crossOffset + item.margin.left;
                float y0 = flow == Flow.ROW
                        ? parentBox.y() + parentBox.height() - insets.top - crossCursor - crossOffset - item.margin.top - itemH
                        : parentBox.y() + parentBox.height() - insets.top - mainCursor - item.margin.top - itemH;
                UiCssBox box = relativeOffset(item.element, new UiCssBox(x0, y0, itemW, itemH), parentBox.width(), parentBox.height());
                commitChild(item.element, box, result);
                mainCursor += item.main + item.mainMargin(flow) + itemGap;
            }
            crossCursor += line.crossSize + resolvedGap;
        }
        for (UiDomNode childNode : parent.children()) {
            if (childNode instanceof UiDomElement child && outOfFlow(child)) commitChild(child, resolveExplicitBox(child, parentBox, parentBox.x(), parentBox.y(), parentBox.width(), parentBox.height()), result);
        }
    }

    private List<FlexLine> flexLines(UiDomElement parent, UiCssBox parentBox, Flow flow, float mainSize, float crossSize, float gapValue) {
        ArrayList<FlexLine> lines = new ArrayList<>();
        FlexLine current = new FlexLine();
        boolean wrap = "wrap".equals(flexWrap.read(parent));
        for (UiDomNode childNode : parent.children()) {
            if (!(childNode instanceof UiDomElement child) || outOfFlow(child)) continue;
            FlexItem item = flexItem(child, parentBox, flow, mainSize, crossSize);
            if (display.read(child).hidden()) item = item.hidden();
            float next = current.items.isEmpty() ? item.outerMain(flow) : current.mainSize(flow, gapValue) + gapValue + item.outerMain(flow);
            if (wrap && !current.items.isEmpty() && next > mainSize) {
                lines.add(current);
                current = new FlexLine();
            }
            current.add(item, flow);
        }
        if (!current.items.isEmpty()) lines.add(current);
        return lines;
    }

    private FlexItem flexItem(UiDomElement element, UiCssBox parent, Flow flow, float mainRef, float crossRef) {
        Insets m = margin(element, parent);
        Float basis = flexBasisValue(element, mainRef);
        float main = basis == null ? (flow == Flow.ROW ? resolvedWidth(element, mainRef, 0f) : resolvedHeight(element, mainRef, 0f)) : basis;
        float cross = flow == Flow.ROW ? resolvedHeight(element, crossRef, crossRef) : resolvedWidth(element, crossRef, crossRef);
        return new FlexItem(element, m, Math.max(0f, main), Math.max(0f, cross), flexGrow(element), flexShrink(element));
    }

    private UiCssBox childBox(UiCssBox parentBox, Insets insets, Flow flow, float cursor, UiDomElement child) {
        if (display.read(child).hidden()) return new UiCssBox(parentBox.x(), parentBox.y(), 0f, 0f);
        if (outOfFlow(child)) return resolveExplicitBox(child, parentBox, parentBox.x(), parentBox.y(), parentBox.width(), parentBox.height());
        return flow == Flow.ROW ? resolveRowChild(child, parentBox, insets, cursor) : resolveColumnChild(child, parentBox, insets, cursor);
    }

    private void commitChild(UiDomElement child, UiCssBox box, UiCssLayoutResult result) {
        writeBox(child, box);
        result.put(child, box);
        layoutChildren(child, box, result);
    }

    private UiCssBox resolveColumnChild(UiDomElement element, UiCssBox parent, Insets insets, float cursorY) {
        Insets m = margin(element, parent);
        float contentW = Math.max(0f, parent.width() - insets.left - insets.right - m.left - m.right);
        float x0 = parent.x() + insets.left + m.left;
        float w = resolvedWidth(element, contentW, contentW);
        float h = resolvedHeight(element, parent.height(), 0f);
        float y0 = parent.y() + parent.height() - insets.top - cursorY - m.top - h;
        return relativeOffset(element, new UiCssBox(x0, y0, w, h), parent.width(), parent.height());
    }

    private UiCssBox resolveRowChild(UiDomElement element, UiCssBox parent, Insets insets, float cursorX) {
        Insets m = margin(element, parent);
        float contentH = Math.max(0f, parent.height() - insets.top - insets.bottom - m.top - m.bottom);
        float x0 = parent.x() + insets.left + cursorX + m.left;
        float y0 = parent.y() + insets.top + m.top;
        float w = resolvedWidth(element, parent.width(), 0f);
        float h = resolvedHeight(element, contentH, contentH);
        return relativeOffset(element, new UiCssBox(x0, y0, w, h), parent.width(), parent.height());
    }

    private UiCssBox resolveExplicitBox(UiDomElement element, UiCssBox parentBox, float originX, float originY, float referenceW, float referenceH) {
        UiCssBounds explicitBounds = bounds.read(element).orElse(null);
        String rawWidth = explicitBounds == null ? width.raw(element) : "";
        String rawHeight = explicitBounds == null ? height.raw(element) : "";
        boolean intrinsicWidth = explicitBounds == null && intrinsicWidthRequested(element, rawWidth);
        boolean intrinsicHeight = explicitBounds == null && intrinsicHeightRequested(rawHeight);
        UiCssLength wLength = explicitBounds == null ? safeLength(rawWidth, UiCssLength.AUTO, intrinsicWidth) : explicitBounds.width();
        UiCssLength hLength = explicitBounds == null ? safeLength(rawHeight, UiCssLength.AUTO, intrinsicHeight) : explicitBounds.height();
        float fallbackW = parentBox == null ? referenceW : (intrinsicWidth ? intrinsicWidth(element, referenceW, 0f) : 0f);
        float fallbackH = parentBox == null ? referenceH : (intrinsicHeight ? intrinsicHeight(element, referenceW, referenceH, 0f) : 0f);
        float w = clampWidth(element, resolveBoxSizedWidth(element, wLength.resolve(lengthContext, referenceW, fallbackW), referenceW, explicitBounds != null || (!rawWidth.isBlank() && !intrinsicWidth)), referenceW);
        float h = clampHeight(element, resolveBoxSizedHeight(element, hLength.resolve(lengthContext, referenceH, fallbackH), referenceH, explicitBounds != null || (!rawHeight.isBlank() && !intrinsicHeight)), referenceH);
        float x0 = originX + resolveExplicitOffset(element, explicitBounds, Axis.X, w, referenceW);
        float y0 = originY + resolveExplicitOffset(element, explicitBounds, Axis.Y, h, referenceH);
        return new UiCssBox(x0, y0, w, h);
    }

    private float resolveExplicitOffset(UiDomElement element, UiCssBounds explicitBounds, Axis axis, float size, float reference) {
        if (explicitBounds != null) {
            UiCssLength resolved = axis == Axis.X ? explicitBounds.x() : explicitBounds.y();
            return resolved.resolve(lengthContext, reference, 0f);
        }
        return explicitOffset(element, axis, size, reference, UiCssLength.AUTO);
    }

    private UiCssBox relativeOffset(UiDomElement element, UiCssBox flowBox, float referenceW, float referenceH) {
        if (!position.read(element).relative()) return flowBox;
        float dx = explicitOffset(element, Axis.X, flowBox.width(), referenceW, UiCssLength.ZERO);
        float dy = explicitOffset(element, Axis.Y, flowBox.height(), referenceH, UiCssLength.ZERO);
        return new UiCssBox(flowBox.x() + dx, flowBox.y() + dy, flowBox.width(), flowBox.height());
    }

    private boolean outOfFlow(UiDomElement element) {
        return position.read(element).outOfFlow() || bounds.read(element).isPresent() || hasExplicitPosition(element);
    }

    private boolean hasExplicitPosition(UiDomElement element) {
        return !x.raw(element).isBlank() || !y.raw(element).isBlank() || !left.raw(element).isBlank() || !top.raw(element).isBlank() || !right.raw(element).isBlank() || !bottom.raw(element).isBlank();
    }

    private Flow flow(UiDomElement element) {
        String raw = flexDirection.raw(element).toLowerCase(Locale.ROOT);
        if (raw.contains("reverse")) warnOnce("flex-reverse|" + summary(element), "UI CSS flex reverse direction is not fully supported yet element={} raw='{}'; using non-reverse axis", summary(element), raw);
        return display.read(element).flex() && flexDirection.read(element).row() ? Flow.ROW : Flow.COLUMN;
    }

    private float rootFontSize(UiDomElement root, float viewportWidth, float viewportHeight) {
        String raw = textOrEmpty(root, item -> item.style("font-size", ""));
        if (raw == null || raw.isBlank()) return UiCssUnitResolutionContext.DEFAULT_ROOT_FONT_SIZE;
        try {
            return Math.max(1f, UiCssLength.parse(raw).resolve(
                    UiCssUnitResolutionContext.viewport(viewportWidth, viewportHeight, UiCssUnitResolutionContext.DEFAULT_ROOT_FONT_SIZE),
                    viewportWidth,
                    UiCssUnitResolutionContext.DEFAULT_ROOT_FONT_SIZE
            ));
        } catch (RuntimeException ex) {
            warnOnce("root-font-size|" + raw, "UI CSS invalid root font-size raw='{}' fallback={} reason='{}'", raw, UiCssUnitResolutionContext.DEFAULT_ROOT_FONT_SIZE, ex.getMessage());
            return UiCssUnitResolutionContext.DEFAULT_ROOT_FONT_SIZE;
        }
    }

    private float explicitOffset(UiDomElement element, Axis axis, float size, float reference, UiCssLength fallback) {
        String raw = axis.primary(this).raw(element);
        if (!raw.isBlank()) return offset(axis, raw, size, reference);
        raw = axis.startProperty(this).raw(element);
        if (!raw.isBlank()) return offset(axis, raw, size, reference);
        raw = axis.endProperty(this).raw(element);
        if (!raw.isBlank()) return Math.max(0f, reference - size - UiCssLength.parse(raw).resolve(lengthContext, reference, 0f));
        return fallback.resolve(lengthContext, reference, 0f);
    }

    private float offset(Axis axis, String raw, float size, float reference) {
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (axis.start(value)) return 0f;
        if (axis.end(value)) return reference - size;
        if (Axis.CENTER.matches(value)) return (reference - size) * 0.5f;
        try {
            return UiCssLength.parse(value).resolve(lengthContext, reference, 0f);
        } catch (IllegalArgumentException ex) {
            warnOnce("offset|" + raw, "UI CSS layout invalid offset raw='{}' axis={} reference={} size={} reason='{}'", raw, axis, reference, size, ex.getMessage());
            return 0f;
        }
    }

    private float resolvedWidth(UiDomElement element, float reference, float fallback) {
        String raw = width.raw(element);
        boolean intrinsic = intrinsicWidthRequested(element, raw);
        UiCssLength length = safeLength(raw, UiCssLength.AUTO, intrinsic);
        if (intrinsic) {
            float value = intrinsicWidth(element, reference, intrinsicKeyword(raw) ? 0f : fallback);
            debugOnce("intrinsic-width|" + summary(element),
                    "UI CSS intrinsic width element={} text='{}' width={} reference={} fallback={}",
                    summary(element), abbreviatedText(element), value, reference, fallback);
            return clampWidth(element, resolveBoxSizedWidth(element, value, reference, false), reference);
        }
        return clampWidth(element, resolveBoxSizedWidth(element, length.resolve(lengthContext, reference, fallback), reference, !raw.isBlank()), reference);
    }

    private float resolvedHeight(UiDomElement element, float reference, float fallback) {
        String raw = height.raw(element);
        boolean intrinsic = intrinsicHeightRequested(raw);
        UiCssLength length = safeLength(raw, UiCssLength.AUTO, intrinsic);
        if (intrinsic) {
            float value = intrinsicHeight(element, reference, reference, intrinsicKeyword(raw) ? 0f : fallback);
            debugOnce("intrinsic-height|" + summary(element),
                    "UI CSS intrinsic height element={} text='{}' height={} reference={} fallback={}",
                    summary(element), abbreviatedText(element), value, reference, fallback);
            return clampHeight(element, resolveBoxSizedHeight(element, value, reference, false), reference);
        }
        return clampHeight(element, resolveBoxSizedHeight(element, length.resolve(lengthContext, reference, fallback), reference, !raw.isBlank()), reference);
    }


    private UiCssLength safeLength(String raw, UiCssLength fallback, boolean intrinsic) {
        if (raw == null || raw.isBlank() || intrinsic) return fallback;
        return UiCssLength.parse(raw);
    }

    private boolean intrinsicWidthRequested(UiDomElement element, String rawWidth) {
        if (intrinsicKeyword(rawWidth)) return true;
        String raw = element.style("fit-text", element.attribute("fit-text", ""));
        if (raw == null || raw.isBlank()) return false;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "true".equals(value) || "1".equals(value) || "yes".equals(value) || "on".equals(value) || "width".equals(value);
    }

    private boolean intrinsicHeightRequested(String rawHeight) {
        return intrinsicKeyword(rawHeight);
    }

    private boolean intrinsicKeyword(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return "fit-content".equals(value) || "max-content".equals(value) || "min-content".equals(value);
    }

    private float intrinsicWidth(UiDomElement element, float reference, float fallback) {
        Insets p = padding(element, new UiCssBox(0f, 0f, Math.max(1f, reference), Math.max(1f, reference)));
        float textWidth = intrinsicTextContentWidth(element);
        float childrenWidth = intrinsicChildrenWidth(element, Math.max(1f, reference));
        float extra = number(firstStyle(element, "fit-extra-width", "text-fit-extra", "intrinsic-extra"), 0f, "fit-extra-width", element);
        return Math.max(0f, Math.max(Math.max(textWidth, childrenWidth), fallback) + p.left + p.right + extra);
    }

    private float intrinsicHeight(UiDomElement element, float referenceW, float referenceH, float fallback) {
        Insets p = padding(element, new UiCssBox(0f, 0f, Math.max(1f, referenceW), Math.max(1f, referenceH)));
        float textHeight = intrinsicTextMetrics(element).height();
        float childrenHeight = intrinsicChildrenHeight(element, referenceW, referenceH);
        return Math.max(0f, Math.max(Math.max(textHeight, childrenHeight), fallback) + p.top + p.bottom);
    }

    private float intrinsicTextContentWidth(UiDomElement element) {
        return intrinsicTextMetrics(element).width();
    }

    private UiIntrinsicTextMetrics intrinsicTextMetrics(UiDomElement element) {
        String text = element.textContent();
        if (text == null || text.isBlank()) return UiIntrinsicTextMetrics.ZERO;
        String normalized = text.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) return UiIntrinsicTextMetrics.ZERO;
        float fallbackFontSize = fontSize(element, 1f);
        return textMeasurer.measure(normalized, fontId(element), fontScale(element), fallbackFontSize);
    }

    private float intrinsicChildrenWidth(UiDomElement element, float reference) {
        Flow childFlow = flow(element);
        float resolvedGap = gap.read(element, UiCssLength.ZERO).resolve(lengthContext, reference, 0f);
        float row = 0f;
        float column = 0f;
        int count = 0;
        for (UiDomNode childNode : element.children()) {
            if (!(childNode instanceof UiDomElement child) || outOfFlow(child) || display.read(child).hidden()) continue;
            Insets m = margin(child, new UiCssBox(0f, 0f, reference, reference));
            float childWidth = preferredWidth(child, reference) + m.left + m.right;
            if (childFlow == Flow.ROW) {
                if (count > 0) row += resolvedGap;
                row += childWidth;
            } else {
                column = Math.max(column, childWidth);
            }
            count++;
        }
        return childFlow == Flow.ROW ? row : column;
    }

    private float intrinsicChildrenHeight(UiDomElement element, float referenceW, float referenceH) {
        Flow childFlow = flow(element);
        float resolvedGap = gap.read(element, UiCssLength.ZERO).resolve(lengthContext, childFlow == Flow.ROW ? referenceW : referenceH, 0f);
        float row = 0f;
        float column = 0f;
        int count = 0;
        for (UiDomNode childNode : element.children()) {
            if (!(childNode instanceof UiDomElement child) || outOfFlow(child) || display.read(child).hidden()) continue;
            Insets m = margin(child, new UiCssBox(0f, 0f, referenceW, referenceH));
            float childHeight = preferredHeight(child, referenceW, referenceH) + m.top + m.bottom;
            if (childFlow == Flow.ROW) {
                row = Math.max(row, childHeight);
            } else {
                if (count > 0) column += resolvedGap;
                column += childHeight;
            }
            count++;
        }
        return childFlow == Flow.ROW ? row : column;
    }

    private float preferredWidth(UiDomElement element, float reference) {
        String raw = width.raw(element);
        if (intrinsicWidthRequested(element, raw) || (!element.textContent().isBlank() && (raw.isBlank() || percentLength(raw)))) {
            return intrinsicWidth(element, reference, 0f);
        }
        if (!raw.isBlank()) return UiCssLength.parse(raw).resolve(lengthContext, reference, 0f);
        return intrinsicChildrenWidth(element, reference);
    }

    private boolean percentLength(String raw) {
        return raw != null && raw.trim().endsWith("%");
    }

    private float preferredHeight(UiDomElement element, float referenceW, float referenceH) {
        String raw = height.raw(element);
        if (intrinsicHeightRequested(raw)) return intrinsicHeight(element, referenceW, referenceH, 0f);
        if (!raw.isBlank()) return UiCssLength.parse(raw).resolve(lengthContext, referenceH, 0f);
        if (!element.textContent().isBlank()) return intrinsicHeight(element, referenceW, referenceH, 0f);
        return intrinsicChildrenHeight(element, referenceW, referenceH);
    }

    private float fontSize(UiDomElement element, float scale) {
        String raw = firstStyle(element, "font-size");
        if (!raw.isBlank()) {
            try {
                return Math.max(1f, UiCssLength.parse(raw).resolve(lengthContext, 1f, 16f) * Math.max(0.01f, scale));
            } catch (RuntimeException ignored) {
            }
        }
        return (titleFont(element) ? 32f : 14f) * Math.max(0.01f, scale);
    }

    private boolean titleFont(UiDomElement element) {
        String font = firstStyle(element, "font-family", "font").toLowerCase(Locale.ROOT);
        String tag = element.tagName();
        return font.contains("title") || font.contains("pixel") || "h1".equals(tag) || "h2".equals(tag);
    }

    private String fontId(UiDomElement element) {
        String raw = firstStyle(element, "font-family", "font");
        return UiCssFontFamilyResolver.resolveEngineFontId(raw.isBlank() ? "standart" : raw, element.computedStyle());
    }

    private float fontScale(UiDomElement element) {
        String scale = firstStyle(element, "scale", "font-scale");
        if (!scale.isBlank()) {
            try { return Math.max(0.01f, Float.parseFloat(scale.trim())); }
            catch (RuntimeException ignored) { }
        }
        String rawSize = firstStyle(element, "font-size");
        if (!rawSize.isBlank()) {
            try { return Math.max(0.01f, UiCssLength.parse(rawSize).resolve(lengthContext, 1f, 16f) / 16f); }
            catch (RuntimeException ignored) { }
        }
        return 1f;
    }

    private String firstStyle(UiDomElement element, String... names) {
        for (String name : names) {
            String value = element.style(name, "");
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private String abbreviatedText(UiDomElement element) {
        String text = element.textContent();
        if (text == null) return "";
        String normalized = text.replace('\n', ' ').replace('\r', ' ').replaceAll("\s+", " ").trim();
        return normalized.length() <= 48 ? normalized : normalized.substring(0, 47) + "…";
    }

    private float resolveBoxSizedWidth(UiDomElement element, float value, float reference, boolean explicit) {
        if (!explicit || !"content-box".equals(boxSizing.read(element))) return value;
        Insets p = padding(element, new UiCssBox(0f, 0f, reference, reference));
        debugOnce("content-box-w|" + summary(element), "UI CSS content-box width expanded element={} width={} paddingLeft={} paddingRight={}", summary(element), value, p.left, p.right);
        return value + p.left + p.right;
    }

    private float resolveBoxSizedHeight(UiDomElement element, float value, float reference, boolean explicit) {
        if (!explicit || !"content-box".equals(boxSizing.read(element))) return value;
        Insets p = padding(element, new UiCssBox(0f, 0f, reference, reference));
        debugOnce("content-box-h|" + summary(element), "UI CSS content-box height expanded element={} height={} paddingTop={} paddingBottom={}", summary(element), value, p.top, p.bottom);
        return value + p.top + p.bottom;
    }

    private float clampWidth(UiDomElement element, float value, float reference) {
        return clamp(element, Axis.X, value, minWidth.read(element, UiCssLength.AUTO), maxWidth.read(element, UiCssLength.AUTO), reference);
    }

    private float clampHeight(UiDomElement element, float value, float reference) {
        return clamp(element, Axis.Y, value, minHeight.read(element, UiCssLength.AUTO), maxHeight.read(element, UiCssLength.AUTO), reference);
    }

    private float clamp(UiDomElement element, Axis axis, float value, UiCssLength min, UiCssLength max, float reference) {
        float out = value;
        Float minValue = min.auto() ? null : min.resolve(lengthContext, reference, out);
        Float maxValue = max.auto() ? null : max.resolve(lengthContext, reference, out);
        if (minValue != null && maxValue != null && minValue > maxValue) {
            warnOnce("minmax|" + summary(element) + '|' + axis, "UI CSS layout min/max conflict element={} axis={} min={} max={} reference={}; using min as effective max", summary(element), axis, minValue, maxValue, reference);
            maxValue = minValue;
        }
        if (minValue != null) out = Math.max(out, minValue);
        if (maxValue != null) out = Math.min(out, maxValue);
        return Math.max(0f, out);
    }

    private Float flexBasisValue(UiDomElement element, float reference) {
        String raw = flexBasis.raw(element);
        if (raw.isBlank()) {
            String shorthand = flex.raw(element);
            if (!shorthand.isBlank() && !"none".equalsIgnoreCase(shorthand.trim())) {
                String[] parts = shorthand.trim().split("\\s+");
                if (parts.length > 2) raw = parts[2];
            }
        }
        if (raw.isBlank() || "auto".equalsIgnoreCase(raw.trim())) return null;
        try {
            return Math.max(0f, UiCssLength.parse(raw).resolve(lengthContext, reference, 0f));
        } catch (RuntimeException ex) {
            warnOnce("basis|" + raw + '|' + summary(element), "UI CSS layout invalid flex-basis element={} raw='{}' reference={}", summary(element), raw, reference);
            return null;
        }
    }

    private float flexGrow(UiDomElement element) {
        String shorthand = flex.raw(element);
        if (!shorthand.isBlank()) {
            if ("none".equalsIgnoreCase(shorthand.trim())) return 0f;
            String[] parts = shorthand.trim().split("\\s+");
            if (parts.length > 0) return number(parts[0], 0f, "flex-grow", element);
        }
        return number(flexGrow.raw(element), 0f, "flex-grow", element);
    }

    private float flexShrink(UiDomElement element) {
        String shorthand = flex.raw(element);
        if (!shorthand.isBlank()) {
            if ("none".equalsIgnoreCase(shorthand.trim())) return 0f;
            String[] parts = shorthand.trim().split("\\s+");
            if (parts.length > 1) return number(parts[1], 1f, "flex-shrink", element);
        }
        return number(flexShrink.raw(element), 1f, "flex-shrink", element);
    }

    private float number(String raw, float fallback, String property, UiDomElement element) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Math.max(0f, Float.parseFloat(raw.trim()));
        } catch (RuntimeException ex) {
            warnOnce("number|" + property + '|' + raw + '|' + summary(element), "UI CSS layout invalid number element={} property='{}' raw='{}' fallback={}", summary(element), property, raw, fallback);
            return fallback;
        }
    }

    private String alignSelf(UiDomElement element, String parentAlign) {
        String value = alignSelf.read(element);
        return "auto".equals(value) ? parentAlign : value;
    }

    private float justifyOffset(String justify, float remaining, int count) {
        if ("center".equals(justify)) return remaining * 0.5f;
        if ("flex-end".equals(justify)) return remaining;
        if ("space-around".equals(justify) && count > 0) return remaining / count * 0.5f;
        return 0f;
    }

    private float justifyGap(String justify, float remaining, float fallbackGap, int count) {
        if (count <= 1) return fallbackGap;
        if ("space-between".equals(justify)) return fallbackGap + remaining / (count - 1);
        if ("space-around".equals(justify)) return fallbackGap + remaining / count;
        return fallbackGap;
    }

    private float alignOffset(String align, float remaining) {
        if ("center".equals(align)) return remaining * 0.5f;
        if ("flex-end".equals(align)) return remaining;
        return 0f;
    }

    private Insets margin(UiDomElement element, UiCssBox reference) {
        UiCssLength all = margin.read(element, UiCssLength.ZERO);
        float fallbackX = all.resolve(lengthContext, reference.width(), 0f);
        float fallbackY = all.resolve(lengthContext, reference.height(), 0f);
        float l = marginLeft.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float r = marginRight.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float t = marginTop.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        float b = marginBottom.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        return new Insets(l, t, r, b);
    }

    private Insets padding(UiDomElement element, UiCssBox reference) {
        UiCssLength all = padding.read(element, UiCssLength.ZERO);
        float fallbackX = all.resolve(lengthContext, reference.width(), 0f);
        float fallbackY = all.resolve(lengthContext, reference.height(), 0f);
        float l = paddingLeft.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float r = paddingRight.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.width(), fallbackX);
        float t = paddingTop.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        float b = paddingBottom.read(element, UiCssLength.AUTO).resolve(lengthContext, reference.height(), fallbackY);
        return new Insets(l, t, r, b);
    }

    private void writeBox(UiDomElement element, UiCssBox box) {
        element.setComputedStyle(layoutX.name(), px(box.x()));
        element.setComputedStyle(layoutY.name(), px(box.y()));
        element.setComputedStyle(width.name(), px(box.width()));
        element.setComputedStyle(height.name(), px(box.height()));
    }

    private String px(float value) {
        if (value == Math.rint(value)) return Math.round(value) + "px";
        return value + "px";
    }

    private String summary(UiDomElement element) {
        if (element == null) return "<null>";
        String id = element.id();
        return element.tagName() + (id.isBlank() ? "" : "#" + id);
    }

    private static void warnOnce(String key, String message, Object... args) {
        if (WARNED.add(key)) LOG.warn(message, args);
    }

    private static void debugOnce(String key, String message, Object... args) {
        if (DEBUGGED.add(key)) LOG.debug(message, args);
    }

    private enum Flow { ROW, COLUMN }

    private enum Axis {
        X, Y;
        static final CenterKeywords CENTER = new CenterKeywords();
        UiCssBasePropertySpec primary(UiCssLayoutEngine engine) { return this == X ? engine.x : engine.y; }
        UiCssBasePropertySpec startProperty(UiCssLayoutEngine engine) { return this == X ? engine.left : engine.top; }
        UiCssBasePropertySpec endProperty(UiCssLayoutEngine engine) { return this == X ? engine.right : engine.bottom; }
        boolean start(String value) { return this == X ? "left".equals(value) || "start".equals(value) : "top".equals(value) || "start".equals(value); }
        boolean end(String value) { return this == X ? "right".equals(value) || "end".equals(value) : "bottom".equals(value) || "end".equals(value); }
    }

    private static final class CenterKeywords {
        boolean matches(String value) { return "center".equals(value) || "middle".equals(value); }
    }

    private record Insets(float left, float top, float right, float bottom) { }

    private static final class FlexLine {
        private final ArrayList<FlexItem> items = new ArrayList<>();
        private float crossSize;
        void add(FlexItem item, Flow flow) { items.add(item); crossSize = Math.max(crossSize, item.cross + item.crossMargin(flow)); }
        float mainSize(Flow flow, float gapValue) {
            float out = 0f;
            for (FlexItem item : items) out += item.outerMain(flow);
            return out + Math.max(0, items.size() - 1) * gapValue;
        }
        void applyFlex(float free) {
            if (items.isEmpty() || free == 0f) return;
            float total = 0f;
            for (FlexItem item : items) total += free > 0f ? item.grow : item.shrink * item.main;
            if (total <= 0f) return;
            for (FlexItem item : items) {
                float share = free > 0f ? item.grow / total : (item.shrink * item.main) / total;
                item.main = Math.max(0f, item.main + free * share);
            }
        }
    }

    private static final class FlexItem {
        private final UiDomElement element;
        private final Insets margin;
        private float main;
        private final float cross;
        private final float grow;
        private final float shrink;
        private FlexItem(UiDomElement element, Insets margin, float main, float cross, float grow, float shrink) {
            this.element = element;
            this.margin = margin;
            this.main = main;
            this.cross = cross;
            this.grow = grow;
            this.shrink = shrink;
        }
        private FlexItem hidden() { return new FlexItem(element, margin, 0f, 0f, 0f, 0f); }
        private float outerMain(Flow flow) { return main + mainMargin(flow); }
        private float mainMargin(Flow flow) { return flow == Flow.ROW ? margin.left + margin.right : margin.top + margin.bottom; }
        private float crossMargin(Flow flow) { return flow == Flow.ROW ? margin.top + margin.bottom : margin.left + margin.right; }
    }
}
