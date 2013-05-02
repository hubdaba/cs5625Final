package cs5625.deferred.scenegraph;

import geometry.QuadSampler;
import geometry.SuperBlock;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.opengl.GL2;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import cs5625.deferred.materials.LambertianMaterial;
import cs5625.deferred.materials.Material;
import cs5625.deferred.materials.TerrainMaterial;
import cs5625.deferred.misc.Observer;
import cs5625.deferred.misc.Observerable;
import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.misc.Util;
import cs5625.deferred.rendering.Camera;

public class TerrainRenderer extends SceneObject implements Observer {

	private Map<Point3f, TerrainBlockRenderer> blocks;

	private boolean needSetup = false;
	
	private Material terrainMaterial;
	private boolean isTest;
	public static float BLOCK_SIZE = 32;
	
	private Set<QuadSampler> nonemptyBlocks;
	
	private SuperBlock renderArea;
	private Point3f cameraPos;

	public TerrainRenderer(boolean isTest) {
		blocks = new HashMap<Point3f, TerrainBlockRenderer>();
		nonemptyBlocks = new HashSet<QuadSampler>();
		this.isTest = isTest;
		if (isTest) {
			terrainMaterial = new LambertianMaterial(new Color3f(1.0f, 0.0f, 0.0f));
		} else {
			terrainMaterial = new TerrainMaterial();
		}
	}

	// set up blocks according to view frustum
	public void setupPosition(GL2 gl, Camera camera) throws OpenGLException, IOException {
		
		if (!needSetup) {
			return;
		}
		
		// if camera didn't move then don't change anything
		if (cameraPos != null && cameraPos.equals(camera.getPosition())) {
			return;
		}
		
		Point3f cameraPosition = camera.getPosition();
		Util.round(cameraPosition, 1);
		renderArea = SuperBlock.midpointDistanceBlock(cameraPosition, camera.getFar() + 1);
		cameraPos = cameraPosition;
		
		
		List<QuadSampler> blocksToRemove = new LinkedList<QuadSampler>();
		for (QuadSampler nonemptyBlock : nonemptyBlocks) {
			if (!renderArea.containsBlock(nonemptyBlock)) {
				blocksToRemove.add(nonemptyBlock);
			}
		}
		for (QuadSampler blockToRemove : blocksToRemove) {
			nonemptyBlocks.remove(blockToRemove);
			TerrainBlockRenderer block = blocks.get(blockToRemove.getMinPoint());
			if (block != null) {
				block.releaseGPUResources(gl);
			}
			blocks.remove(blockToRemove.getMinPoint());
		}
		
		Tuple3f minPoint = renderArea.getMinPoint();
		int diameter = (int)Math.ceil(renderArea.getSideLength());
		for (float x = minPoint.x; x < diameter + minPoint.x; x+=BLOCK_SIZE) {
			for (float y = minPoint.y; y < diameter + minPoint.y; y+=BLOCK_SIZE) {
				for (float z = minPoint.z; z < diameter + minPoint.z; z += BLOCK_SIZE) {
					QuadSampler quad = new QuadSampler(new Vector3f(x, y, z), BLOCK_SIZE);
					if (nonemptyBlocks.contains(quad)) {
						continue;
					}
					if (quad.hasPolygons()) {
						nonemptyBlocks.add(quad);
					}
				}
			}
		}
		
	}
	
	public void renderPolygons(GL2 gl, Camera camera) throws OpenGLException, IOException {
		
		if (!needSetup) {
			return;
		}
		for (QuadSampler nonemptyBlock : nonemptyBlocks) {
			Point3f lowerCorner = nonemptyBlock.getMinPoint();
			
			if (camera.inFrustum(nonemptyBlock)) {
				if (!blocks.containsKey(lowerCorner)) {
					TerrainBlockRenderer renderer = new TerrainBlockRenderer(gl, nonemptyBlock.getMinPoint(),
							20, nonemptyBlock.getSideLength()); 
					blocks.put(lowerCorner, renderer);
					renderer.fillTexture3D(gl);
					if (!isTest) {
						renderer.renderPolygons(gl);
					}
				}
			}
		}
	}

	public void renderTerrain(GL2 gl) throws OpenGLException {
		System.out.println(blocks.size());
		for (TerrainBlockRenderer block : blocks.values()) {
			if (!isTest) {
				((TerrainMaterial) terrainMaterial).setDensityFunction(block.getTexture3D());
				((TerrainMaterial) terrainMaterial).setLowerCorner(block.getMinPoint());
				((TerrainMaterial) terrainMaterial).setNumVoxels(block.getNumVoxels());
				((TerrainMaterial) terrainMaterial).setBlockSize(block.getSideLength());
				terrainMaterial.bind(gl);
				block.renderTerrain(gl);
				terrainMaterial.unbind(gl);
			} else {
				((LambertianMaterial) terrainMaterial).setDiffuseTexture3D(block.getTexture3D());
				terrainMaterial.bind(gl);
				block.testTexture(gl);
				terrainMaterial.unbind(gl);
			}
		}
	}

	public Material getMaterial() {
		return this.terrainMaterial;
	}

	@Override
	public void update(Observerable o) {
		needSetup = true;
		
	}

}
