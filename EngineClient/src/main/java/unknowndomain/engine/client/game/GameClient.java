package unknowndomain.engine.client.game;

import org.joml.Matrix4d;
import unknowndomain.engine.api.Engine;
import unknowndomain.engine.api.block.Block;
import unknowndomain.engine.api.client.display.Camera;
import unknowndomain.engine.api.game.Game;
import unknowndomain.engine.api.world.World;
import unknowndomain.engine.client.EngineClient;
import unknowndomain.engine.client.block.Grass;
import unknowndomain.engine.client.block.model.*;
import unknowndomain.engine.client.camera.CameraController;
import unknowndomain.engine.client.world.FlatWorld;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class GameClient implements Game {

	private Map<String, World> worldsMap = new HashMap<>();
	
	private Transformation transformation;
	private ShaderProgram shaderProgram;
	private Camera camera;
	private CameraController cameraController;

	private final EngineClient engine;
	
	public GameClient(EngineClient engine) {
		this.engine = engine;
		transformation=new Transformation();

		try {
			shaderProgram =new ShaderProgram();
	    	shaderProgram.createVertexShader(Utils.getVertex());
	        shaderProgram.createFragmentShader(Utils.getFragment());
	        shaderProgram.link();
	        shaderProgram.createUniform("projectionMatrix");
	        shaderProgram.createUniform("modelViewMatrix");
	        shaderProgram.createUniform("texture_sampler");//纹理
	    	}catch(Exception ex) {
	    		ex.printStackTrace();
	    	}
		camera=engine.getRenderer().getRendererGame().getCamera();
		cameraController=new CameraController(engine.getKeyBindingManager(),camera);
	}
	
	@Override
	public Collection<World> getWorlds() {
		return worldsMap.values();
	}

	@Override
	public World getWorld(String name) {
		return worldsMap.get(name);
	}
	private static final float FOV = (float) Math.toRadians(60.0f);//视角
    private static final float Z_NEAR = 0.01f;//Z最小值
    private static final float Z_FAR = 1000.f;//Z最大值
	@Override
	public void tick() {
        Collection<World> worlds=getWorlds();
        for(World world:worlds) {
        	FlatWorld flatWorld=(FlatWorld)world;
        	Collection<Block> allBlock=flatWorld.getAllBlock();
        	shaderProgram.bind();
            
            // Update projection Matrix
            Matrix4d projectionMatrix = transformation.getProjectionMatrix(FOV, engine.getGameWindow().getWidth(), engine.getGameWindow().getHeight(), Z_NEAR, Z_FAR);
            shaderProgram.setUniform("projectionMatrix", projectionMatrix);
            
            // Update view Matrix

            
            shaderProgram.setUniform("texture_sampler", 0);
            
            Matrix4d viewMatrix = transformation.getViewMatrix(camera);
            for(Block block:allBlock) {
            	Grass grass=(Grass) block;
            	GameItem gameItem=grass.getGameItem();
                // Render the mes for this game item
            	Matrix4d modelViewMatrix = transformation.getModelViewMatrix(gameItem, viewMatrix);
                shaderProgram.setUniform("modelViewMatrix", modelViewMatrix);
                System.out.println("render");
                gameItem.getMesh().render();
        	}
            

            shaderProgram.unbind();
        	
        }
        
	}

	@Override
	public void addWorld(World world) {
		worldsMap.put(world.getName(), world);
	}
	
	
}
