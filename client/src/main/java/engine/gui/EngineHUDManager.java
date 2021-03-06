package engine.gui;

import engine.Platform;
import engine.client.hud.HUDControl;
import engine.client.hud.HUDManager;
import engine.event.Listener;
import engine.event.game.GameStartEvent;
import engine.event.game.GameTerminationEvent;
import engine.gui.layout.AnchorPane;
import engine.gui.stage.Stage;
import engine.registry.Registries;
import engine.registry.Registry;

import java.util.Collection;
import java.util.List;

public final class EngineHUDManager implements HUDManager {

    private final Stage stage;
    private final AnchorPane contentPane;

    private Registry<HUDControl> hudControls;

    public EngineHUDManager(Stage stage) {
        this.stage = stage;
        this.contentPane = new AnchorPane();
        stage.setScene(new Scene(contentPane));
        Platform.getEngine().getEventBus().register(this);
    }

    @Override
    public float getScaleX() {
        return stage.getUserScaleX();
    }

    @Override
    public float getScaleY() {
        return stage.getUserScaleY();
    }

    @Override
    public void setScale(float scaleX, float scaleY) {
        stage.setUserScale(scaleX, scaleY);
    }

    @Override
    public void toggleVisible() {
        setVisible(!isVisible());
    }

    @Override
    public void setVisible(boolean visible) {
        contentPane.setVisible(visible);
        getControls().stream().filter(Node::isVisible).forEach(hudControl ->
                hudControl.onVisibleChanged(visible));
    }

    @Override
    public boolean isVisible() {
        return contentPane.isVisible();
    }

    @Override
    public HUDControl getControl(String name) {
        return hudControls == null ? null : hudControls.getValue(name);
    }

    @Override
    public Collection<HUDControl> getControls() {
        return hudControls == null ? List.of() : hudControls.getValues();
    }

    @Listener
    public void onGameReady(GameStartEvent.Post event) {
        Registries.getRegistryManager().getRegistry(HUDControl.class).ifPresent(registry -> {
            hudControls = registry;
            contentPane.getChildren().addAll(registry.getValues());
        });
    }

    @Listener
    public void onGameMarkedStop(GameTerminationEvent.Marked event) {
        contentPane.getChildren().clear();
        hudControls = null;
    }
}
