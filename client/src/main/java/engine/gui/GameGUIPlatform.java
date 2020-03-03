package engine.gui;

import engine.Platform;
import engine.graphics.GraphicsEngine;
import engine.graphics.display.Window;
import engine.graphics.texture.FrameBuffer;
import engine.gui.internal.ClipboardHelper;
import engine.gui.internal.GUIPlatform;
import engine.gui.internal.SceneHelper;
import engine.gui.internal.StageHelper;
import engine.gui.internal.impl.SceneHelperImpl;
import engine.gui.internal.impl.StageHelperImpl;
import engine.gui.internal.impl.glfw.GLFWClipboardHelper;

public final class GameGUIPlatform extends GUIPlatform {

    private final StageHelperImpl stageHelper = new StageHelperImpl();
    private final SceneHelper sceneHelper = new SceneHelperImpl();
    private final ClipboardHelper clipboardHelper = new GLFWClipboardHelper();

    private final Window primaryWindow = GraphicsEngine.getGraphicsBackend().getPrimaryWindow();

    private Stage guiStage;
    private Stage hudStage;

    public GameGUIPlatform() {
        setInstance(this);
        Stage.getStages().addChangeListener(change -> {
            if (change.getList().isEmpty()) Platform.getEngine().terminate();
        });
        initGUIStage();
        initHUDStage();
    }

    private void initGUIStage() {
        Stage stage = stageHelper.getPrimaryStage();
        StageHelper.setWindow(stage, primaryWindow);
        StageHelper.getShowingProperty(stage).set(true);
        StageHelper.doVisibleChanged(stage, true);
        stageHelper.enableInput(stage, primaryWindow);
        guiStage = stage;
    }

    private void initHUDStage() {
        Stage stage = new Stage();
        StageHelper.setPrimary(stage, true);
        StageHelper.setWindow(stage, primaryWindow);
        StageHelper.getShowingProperty(stage).set(true);
        StageHelper.doVisibleChanged(stage, true);
        hudStage = stage;
    }

    public Stage getGUIStage() {
        return guiStage;
    }

    public Stage getHUDStage() {
        return hudStage;
    }

    public void render(Stage stage, FrameBuffer frameBuffer) {
//        Scene scene = stage.getScene();
//        if (scene == null) return;
//
//        Window window = StageHelper.getWindow(stage);
//        if (SceneHelper.getViewportWidth(scene) != window.getWidth() ||
//                SceneHelper.getViewportHeight(scene) != window.getHeight()) {
//            SceneHelper.setViewport(scene, window.getWidth(), window.getHeight(),
//                    window.getContentScaleX(), window.getContentScaleY());
//        }
//
//        scene.update();
//        renderHandler.getRenderer().render(scene, frameBuffer, false);
    }

    @Override
    public StageHelper getStageHelper() {
        return stageHelper;
    }

    @Override
    public SceneHelper getSceneHelper() {
        return sceneHelper;
    }

    @Override
    public ClipboardHelper getClipboardHelper() {
        return clipboardHelper;
    }

    public void dispose() {
    }
}
