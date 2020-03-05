package engine.gui;

import com.github.mouse0w0.observable.collection.ObservableCollections;
import com.github.mouse0w0.observable.collection.ObservableMap;
import com.github.mouse0w0.observable.value.*;
import engine.gui.event.*;
import engine.gui.input.KeyEvent;
import engine.gui.input.MouseActionEvent;
import engine.gui.input.MouseEvent;
import engine.gui.input.ScrollEvent;
import engine.gui.misc.Bounds;
import engine.gui.misc.Point;
import engine.gui.rendering.ComponentRenderer;
import engine.input.MouseButton;

import java.util.HashMap;

public abstract class Node implements EventTarget {

    final MutableObjectValue<Scene> scene = new SimpleMutableObjectValue<>();
    final MutableObjectValue<Parent> parent = new SimpleMutableObjectValue<>();

    private final MutableObjectValue<Bounds> layoutBounds = new SimpleMutableObjectValue<>(Bounds.EMPTY);

    private MutableFloatValue layoutX;
    private MutableFloatValue layoutY;

    final MutableBooleanValue focused = new SimpleMutableBooleanValue(false);
    final MutableBooleanValue hover = new SimpleMutableBooleanValue(false);
    final MutableBooleanValue pressed = new SimpleMutableBooleanValue(false);

    private MutableBooleanValue visible;
    private MutableBooleanValue disabled;

    private ComponentRenderer renderer;

    private EventHandlerManager eventHandlerManager = new EventHandlerManager();

    public Node() {
    }

    public final ObservableObjectValue<Scene> scene() {
        return scene;
    }

    public final Scene getScene() {
        return scene.get();
    }

    public final ObservableObjectValue<Parent> parent() {
        return parent.toUnmodifiable();
    }

    public final Parent getParent() {
        return parent.get();
    }

    public final ObservableObjectValue<Bounds> layoutBounds() {
        return layoutBounds.toUnmodifiable();
    }

    public final Bounds getLayoutBounds() {
        return layoutBounds.get();
    }

    void position(float x, float y, float width, float height) {
        layoutBounds.set(new Bounds(x, y, width, height));
        layoutX().set(x);
        layoutY().set(y);
    }

    public final MutableFloatValue layoutX() {
        if (layoutX == null) {
            layoutX = new SimpleMutableFloatValue();
            layoutX.addChangeListener((observable, oldValue, newValue) -> {
                if (newValue != getLayoutBounds().getMinX()) requestParentLayout();
            });
        }
        return layoutX;
    }

    public final float getLayoutX() {
        return layoutX == null ? 0 : layoutX.get();
    }

    public final void setLayoutX(float x) {
        layoutX().set(x);
    }

    public final MutableFloatValue layoutY() {
        if (layoutY == null) {
            layoutY = new SimpleMutableFloatValue();
            layoutY.addChangeListener((observable, oldValue, newValue) -> {
                if (newValue != getLayoutBounds().getMinY()) requestParentLayout();
            });
        }
        return layoutY;
    }

    public final float getLayoutY() {
        return layoutX == null ? 0 : layoutY.get();
    }

    public final void setLayoutY(float y) {
        layoutY().set(y);
    }

    public final void relocate(float x, float y) {
        setLayoutX(x);
        setLayoutY(y);
    }

    public final float getWidth() {
        return getLayoutBounds().getWidth();
    }

    public final float getHeight() {
        return getLayoutBounds().getHeight();
    }

    public final ObservableBooleanValue focused() {
        return focused.toUnmodifiable();
    }

    public final boolean isFocused() {
        return focused.get();
    }

    public final ObservableBooleanValue hover() {
        return hover.toUnmodifiable();
    }

    public final boolean isHover() {
        return hover.get();
    }

    public final ObservableBooleanValue pressed() {
        return pressed.toUnmodifiable();
    }

    public final boolean isPressed() {
        return pressed.get();
    }

    public final MutableBooleanValue visible() {
        if (visible == null) {
            visible = new SimpleMutableBooleanValue(true);
        }
        return visible;
    }

    public final boolean isVisible() {
        return visible == null || visible.get();
    }

    public final void setVisible(boolean visible) {
        visible().set(visible);
    }

    public final MutableBooleanValue disabled() {
        if (disabled == null) {
            disabled = new SimpleMutableBooleanValue(false);
        }
        return disabled;
    }

    public final boolean isDisabled() {
        return disabled != null && disabled.get();
    }

    public final void setDisabled(boolean visible) {
        visible().set(visible);
    }

    public final void requestParentLayout() {
        Parent parent = parent().get();
        if (parent != null && !parent.isNeedsLayout()) {
            parent.needsLayout();
        }
    }

    public float minWidth() {
        return prefWidth();
    }

    public float minHeight() {
        return prefHeight();
    }

    abstract public float prefWidth();

    abstract public float prefHeight();

    public float maxWidth() {
        return prefWidth();
    }

    public float maxHeight() {
        return prefHeight();
    }

    public boolean contains(float x, float y) {
        return getLayoutBounds().contains(x, y);
    }

    public ComponentRenderer getRenderer() {
        if (renderer == null)
            renderer = createDefaultRenderer();
        return renderer;
    }

    public void overrideRenderer(ComponentRenderer r) {
        renderer = r;
    }

    protected abstract ComponentRenderer createDefaultRenderer();

    private ObservableMap<Object, Object> properties;

    public final ObservableMap<Object, Object> getProperties() {
        if (properties == null) {
            properties = ObservableCollections.observableMap(new HashMap<>());
        }
        return properties;
    }

    public boolean hasProperties() {
        return properties != null && !properties.isEmpty();
    }

    public void forceFocus() {
        focused.set(true);
    }

    public Point relativePos(float x, float y) {
        if (parent().isEmpty()) {
            return new Point(x, y);
        } else {
            return parent().get().relativePos(x - getLayoutX(), y - getLayoutY());
        }
    }

    public final EventDispatchChain buildEventDispatchChain(EventDispatchChain tail) {
        if (preprocessEventHandler == null) {
            preprocessEventHandler = (event, eventDispatchChain) -> {
                preprocessEvent(event);
                return event;
            };
        }
        tail.append(preprocessEventHandler);

        var node = this;
        while (node != null) {
            tail = tail.append(node.eventHandlerManager);
            node = node.parent.get();
        }

        Scene scene = getScene();
        if (scene != null) {
            tail = scene.buildEventDispatchChain(tail);
        }
        return tail;
    }

    private EventDispatcher preprocessEventHandler;

    private void preprocessEvent(Event event) {
        if (!(event instanceof MouseEvent)) return;

        EventType<? extends Event> eventType = event.getEventType();
        if (eventType == MouseActionEvent.MOUSE_PRESSED) {
            if (((MouseActionEvent) event).getButton() != MouseButton.MOUSE_BUTTON_PRIMARY) return;
            for (var node = this; node != null; node = node.getParent()) {
                node.pressed.set(true);
            }
            return;
        }

        if (eventType == MouseActionEvent.MOUSE_RELEASED) {
            if (((MouseActionEvent) event).getButton() != MouseButton.MOUSE_BUTTON_PRIMARY) return;
            for (var node = this; node != null; node = node.getParent()) {
                node.pressed.set(false);
            }
            return;
        }

        if (event.getTarget() == this) {
            if (eventType == MouseEvent.MOUSE_ENTERED) {
                hover.set(true);
            } else if (eventType == MouseEvent.MOUSE_EXITED) {
                hover.set(false);
            }
        }
    }

    public final <T extends Event> void addEventHandler(EventType<T> eventType, EventHandler<T> eventHandler) {
        eventHandlerManager.addEventHandler(eventType, eventHandler);
    }

    public final <T extends Event> void removeEventHandler(EventType<T> eventType, EventHandler<T> eventHandler) {
        eventHandlerManager.removeEventHandler(eventType, eventHandler);
    }

    // ===== Event handlers =====
    public final MutableObjectValue<EventHandler<MouseEvent>> onMouseEntered() {
        return eventHandlerManager.onMouseEntered();
    }

    public final EventHandler<MouseEvent> getOnMouseEntered() {
        return eventHandlerManager.getOnMouseEntered();
    }

    public final void setOnMouseEntered(EventHandler<MouseEvent> onMouseEntered) {
        eventHandlerManager.setOnMouseEntered(onMouseEntered);
    }

    public final MutableObjectValue<EventHandler<MouseEvent>> onMouseExited() {
        return eventHandlerManager.onMouseExited();
    }

    public final EventHandler<MouseEvent> getOnMouseExited() {
        return eventHandlerManager.getOnMouseExited();
    }

    public final void setOnMouseExited(EventHandler<MouseEvent> onMouseExited) {
        eventHandlerManager.setOnMouseExited(onMouseExited);
    }

    public final MutableObjectValue<EventHandler<MouseEvent>> onMouseMoved() {
        return eventHandlerManager.onMouseMoved();
    }

    public final EventHandler<MouseEvent> getOnMouseMoved() {
        return eventHandlerManager.getOnMouseMoved();
    }

    public final void setOnMouseMoved(EventHandler<MouseEvent> onMouseMoved) {
        eventHandlerManager.setOnMouseMoved(onMouseMoved);
    }

    public final MutableObjectValue<EventHandler<MouseActionEvent>> onMousePressed() {
        return eventHandlerManager.onMousePressed();
    }

    public final EventHandler<MouseActionEvent> getOnMousePressed() {
        return eventHandlerManager.getOnMousePressed();
    }

    public final void setOnMousePressed(EventHandler<MouseActionEvent> onMousePressed) {
        eventHandlerManager.setOnMousePressed(onMousePressed);
    }

    public final MutableObjectValue<EventHandler<MouseActionEvent>> onMouseReleased() {
        return eventHandlerManager.onMouseReleased();
    }

    public final EventHandler<MouseActionEvent> getOnMouseReleased() {
        return eventHandlerManager.getOnMouseReleased();
    }

    public final void setOnMouseReleased(EventHandler<MouseActionEvent> onMouseReleased) {
        eventHandlerManager.setOnMouseReleased(onMouseReleased);
    }

    public final MutableObjectValue<EventHandler<MouseActionEvent>> onMouseClicked() {
        return eventHandlerManager.onMouseClicked();
    }

    public final EventHandler<MouseActionEvent> getOnMouseClicked() {
        return eventHandlerManager.getOnMouseClicked();
    }

    public final void setOnMouseClicked(EventHandler<MouseActionEvent> onMouseClicked) {
        eventHandlerManager.setOnMouseClicked(onMouseClicked);
    }

    public final MutableObjectValue<EventHandler<KeyEvent>> onKeyPressed() {
        return eventHandlerManager.onKeyPressed();
    }

    public final EventHandler<KeyEvent> getOnKeyPressed() {
        return eventHandlerManager.getOnKeyPressed();
    }

    public final void setOnKeyPressed(EventHandler<KeyEvent> onKeyPressed) {
        eventHandlerManager.setOnKeyPressed(onKeyPressed);
    }

    public final MutableObjectValue<EventHandler<KeyEvent>> onKeyReleased() {
        return eventHandlerManager.onKeyReleased();
    }

    public final EventHandler<KeyEvent> getOnKeyReleased() {
        return eventHandlerManager.getOnKeyReleased();
    }

    public final void setOnKeyReleased(EventHandler<KeyEvent> onKeyReleased) {
        eventHandlerManager.setOnKeyReleased(onKeyReleased);
    }

    public final MutableObjectValue<EventHandler<KeyEvent>> onKeyTyped() {
        return eventHandlerManager.onKeyTyped();
    }

    public final EventHandler<KeyEvent> getOnKeyTyped() {
        return eventHandlerManager.getOnKeyTyped();
    }

    public final void setOnKeyTyped(EventHandler<KeyEvent> onKeyTyped) {
        eventHandlerManager.setOnKeyTyped(onKeyTyped);
    }

    public final MutableObjectValue<EventHandler<ScrollEvent>> onScroll() {
        return eventHandlerManager.onScroll();
    }

    public final EventHandler<ScrollEvent> getOnScroll() {
        return eventHandlerManager.getOnScroll();
    }

    public final void setOnScroll(EventHandler<ScrollEvent> onScroll) {
        eventHandlerManager.setOnScroll(onScroll);
    }
}
