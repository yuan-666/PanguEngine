package nullengine.client.rendering.management;

import nullengine.client.rendering.display.Window;
import nullengine.client.rendering.display.WindowHelper;
import nullengine.client.rendering.util.GPUInfo;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface RenderManager {

    Thread getRenderingThread();

    boolean isRenderingThread();

    GPUInfo getGPUInfo();

    WindowHelper getWindowHelper();

    Window getPrimaryWindow();

    ResourceFactory getResourceFactory();

    void attachHandler(RenderHandler handler);

    Future<Void> submitTask(Runnable runnable);

    <V> Future<V> submitTask(Callable<V> callable);

    void render(float tpf);

    void init();

    void dispose();
}
