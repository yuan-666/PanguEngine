package nullengine.client.gui;

import com.github.mouse0w0.observable.value.*;
import nullengine.client.gui.event.type.KeyEvent;
import nullengine.client.gui.event.type.MouseActionEvent;
import nullengine.client.gui.event.type.MouseEvent;
import nullengine.client.gui.event.type.ScrollEvent;
import nullengine.client.gui.input.KeyCode;
import nullengine.client.gui.input.MouseButton;
import nullengine.client.input.keybinding.KeyModifier;
import nullengine.client.rendering.display.callback.*;
import org.apache.commons.lang3.Validate;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Scene {

    private final MutableFloatValue width = new SimpleMutableFloatValue();
    private final MutableFloatValue height = new SimpleMutableFloatValue();

    private final MutableObjectValue<Parent> root = new SimpleMutableObjectValue<>();

    public Scene(Parent root) {
        setRoot(root);
    }

    public ObservableFloatValue width() {
        return width.toUnmodifiable();
    }

    public float getWidth() {
        return width.get();
    }

    public ObservableFloatValue height() {
        return height.toUnmodifiable();
    }

    public float getHeight() {
        return height.get();
    }

    public void setSize(float width, float height) {
        this.width.set(width);
        this.height.set(height);
        updateRoot();
    }

    public ObservableObjectValue<Parent> root() {
        return root.toUnmodifiable();
    }

    public Parent getRoot() {
        return root.get();
    }

    public void setRoot(Parent root) {
        Validate.notNull(root);
        if (root.parent().get() != null) {
            throw new IllegalStateException(root + " is already inside a container and cannot be set as root");
        }

        this.root.set(root);
        root.scene.setValue(this);
        updateRoot();
    }

    private void updateRoot() {
        Parent root = this.root.get();
        root.width.set(getWidth() - root.x().get());
        root.height.set(getHeight() - root.y().get());
        root.needsLayout();
    }

    public void update() {
        root.get().layout();
    }

    private float lastScreenX = Float.NaN;
    private float lastScreenY = Float.NaN;

    public final CursorCallback cursorCallback = (window, xPos, yPos) -> {
        if (!Float.isNaN(lastScreenX) && !Float.isNaN(lastScreenY)) {
            var root = this.root.get();
            var olds = root.getPointingLastChildComponents(lastScreenX, lastScreenY);
            lastScreenX = (float) xPos;
            lastScreenY = (float) yPos;
            var news = root.getPointingLastChildComponents(lastScreenX, lastScreenY);
            var toRemove = new ArrayList<Node>();
            for (var i : news) {
                if (olds.contains(i)) {
                    var pair = i.relativePos(lastScreenX, lastScreenY);
                    new MouseEvent(MouseEvent.MOUSE_MOVED, i, i, pair.getLeft(), pair.getRight(), lastScreenX, lastScreenY).fireEvent();
                    toRemove.add(i);
                } else {
                    var pair = i.relativePos(lastScreenX, lastScreenY);
                    new MouseEvent(MouseEvent.MOUSE_ENTERED, i, i, pair.getLeft(), pair.getRight(), lastScreenX, lastScreenY).fireEvent();
                }
            }
            olds.removeAll(toRemove);
            for (var i : olds) {
                var pair = i.relativePos(lastScreenX, lastScreenY);
                new MouseEvent(MouseEvent.MOUSE_EXITED, i, i, pair.getLeft(), pair.getRight(), lastScreenX, lastScreenY).fireEvent();
            }
        }
    };

    public final MouseCallback mouseCallback = (window, button, action, modifiers) -> {
        if (!Float.isNaN(lastScreenX) && !Float.isNaN(lastScreenY)) {
            var root = this.root.get();
            var targets = root.getPointingLastChildComponents(lastScreenX, lastScreenY);
            if (action == GLFW.GLFW_PRESS) {
                for (var target : targets) {
                    if (target.disabled.get()) continue;
                    target.focused.set(true);
                }
                var a = root.getChildrenRecursive().stream().filter(component -> component.focused().get()).collect(Collectors.toList());
                var lastA = this.getLastChildNodeFromList(a);
                for (var target : lastA) {
                    if (target.disabled.get()) continue;
                    target.focused.set(false);
                }
            }
            for (var target : targets) {
                if (action == GLFW.GLFW_PRESS) {
                    var pair = target.relativePos(lastScreenX, lastScreenY);
                    new MouseActionEvent(MouseActionEvent.MOUSE_PRESSED, target, target, pair.getLeft(), pair.getRight(), lastScreenX, lastScreenY, MouseButton.valueOf(button)).fireEvent(target);
                } else if (action == GLFW.GLFW_RELEASE) {
                    var pair = target.relativePos(lastScreenX, lastScreenY);
                    new MouseActionEvent(MouseActionEvent.MOUSE_RELEASED, target, target, pair.getLeft(), pair.getRight(), lastScreenX, lastScreenY, MouseButton.valueOf(button)).fireEvent(target);
                }
            }

        }
    };

    public final ScrollCallback scrollCallback = (window, xOffset, yOffset) -> {
        var root = this.root.get();
        var targets = root.getChildrenRecursive().stream().filter(component -> component.focused().get()).collect(Collectors.toList());
        for (var target : targets) {
            var pair = target.relativePos(lastScreenX, lastScreenY);
            new ScrollEvent(ScrollEvent.ANY, target, target, pair.getLeft(), pair.getRight(), lastScreenX, lastScreenY, xOffset, yOffset).fireEvent();
        }
    };

    public final KeyCallback keyCallback = (window, key, scanCode, action, mods) -> {
        var root = this.root.get();
        var a = root.getChildrenRecursive().stream().filter(component -> component.focused().get()).collect(Collectors.toList());
        var targets = getLastChildNodeFromList(a);
        for (Node target : targets) {
            if (action == GLFW.GLFW_PRESS) {
                new KeyEvent(KeyEvent.KEY_PRESSED, target, KeyCode.valueOf(key), KeyModifier.of(mods), true).fireEvent();
            } else if (action == GLFW.GLFW_RELEASE) {
                new KeyEvent(KeyEvent.KEY_RELEASED, target, KeyCode.valueOf(key), KeyModifier.of(mods), false).fireEvent();
            }
        }
    };

    public final CharModsCallback charModsCallback = (window, codepoint, mods) -> {
        var root = this.root.get();
        var a = root.getChildrenRecursive().stream().filter(component -> component.focused().get()).collect(Collectors.toList());
        var targets = getLastChildNodeFromList(a);
        for (Node target : targets) {
            new KeyEvent(KeyEvent.KEY_TYPED, target, KeyCode.KEY_UNDEFINED, String.valueOf((char) codepoint), KeyModifier.of(mods), true).fireEvent();
        }
    };

    private List<Node> getLastChildNodeFromList(List<Node> nodes) {
        var list = new ArrayList<>(nodes);
        var toRemove = new ArrayList<Node>();
        for (var i : list) {
            if (!i.parent().isEmpty())
                toRemove.add(i.parent().get());
        }
        list.removeAll(toRemove);
        return list;
    }
}
