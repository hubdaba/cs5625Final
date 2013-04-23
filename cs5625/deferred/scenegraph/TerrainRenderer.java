package cs5625.deferred.scenegraph;

import geometry.QuadSampler;
import geometry.SuperBlock;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	public static float BLOCK_SIZE = 16;
	
	private List<TerrainBlockRenderer> nonemptyBlocks;
	
	private SuperBlock renderArea;

	public TerrainRenderer(boolean isTest) {
		blocks = new HashMap<Point3f, TerrainBlockRenderer>();
		nonemptyBlocks = new LinkedList<TerrainBlockRenderer>();
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
		if (renderArea != null && renderArea.getMidPoint().equals(camera.getPosition())) {
			return;
		}
		SuperBlock prevRenderArea = null;
		// if it was setup before, check the previous render area as to not re setup blocks
		if (renderArea != null) {
			prevRenderArea = renderArea;
		}
		
		Point3f cameraPos = camera.getPosition();
		Util.round(cameraPos, 1);
		renderArea = SuperBlock.midpointDistanceBlock(cameraPos, camera.getFar() + 1);
		
		List<TerrainBlockRenderer> blocksToRemove = new LinkedList<TerrainBlockRenderer>();
		for (TerrainBlockRenderer nonemptyBlock : nonemptyBlocks) {
			if (!renderArea.containsBlock(nonemptyBlock)) {
				blocksToRemove.add(nonemptyBlock);
			}
		}
		for (TerrainBlockRenderer blockToRemove : blocksToRemove) {
			nonemptyBlocks.remove(blockToRemove);
			blocks.remove(blockToRemove.getMinPoint());
			blockToRemove.releaseGPUResources(gl);
		}
		
		List<QuadSampler> stack = new LinkedList<QuadSampler>();
		Tuple3f minPoint = renderArea.getMinPoint();

		
		int diameter = (int)Math.ceil(renderArea.getSideLength());
		diameter = Integer.highestOneBit(diameter);
		diameter = diameter << 1;
		stack.add(new QuadSampler(new Vector3f(minPoint), diameter));
		while (!stack.isEmpty() && !(stack.get(0).getSideLength() <= (BLOCK_SIZE + 0.1))) {
			QuadSampler node = stack.remove(0);
			if (node.hasPolygons()) {
				float blockSize = node.getSideLength();
				SuperBlock currBlock = new SuperBlock(node.getMinPoint(), blockSize);
				List<Point3f> corners = currBlock.getCorners();
				for (Point3f corner : corners) {
					Point3f subBlockCorner = new Point3f(corner);
					subBlockCorner.sub(currBlock.getMinPoint());
					subBlockCorner.scale(0.5f);
					subBlockCorner.add(currBlock.getMinPoint());
					SuperBlock subBlock = new SuperBlock(subBlockCorner, blockSize / 2.0f);
					if (prevRenderArea == null) {
						stack.add(new QuadSampler(new Vector3f(subBlock.getMinPoint()), blockSize / 2.0f));
					} else if (prevRenderArea.completelyContains(subBlock)) {
						continue;
					} else {
						stack.add(new QuadSampler(new Vector3f(subBlock.getMinPoint()), blockSize / 2.0f));
					}
				}
			}
		}
		for (QuadSampler sampler : stack) {
			nonemptyBlocks.add(new TerrainBlockRenderer(gl, sampler.getMinPoint(), 10, sampler.getSideLength()));
		}
		
	}
	
	public void renderPolygons(GL2 gl, Camera camera) throws OpenGLException, IOException {
		
		if (!needSetup) {
			return;
		}
		for (TerrainBlockRenderer nonemptyBlock : nonemptyBlocks) {
			Point3f lowerCorner = nonemptyBlock.getMinPoint();
			
			if (camera.inFrustum(nonemptyBlock)) {
				if (!blocks.containsKey(lowerCorner)) {
					blocks.put(lowerCorner, nonemptyBlock);
					nonemptyBlock.fillTexture3D(gl);
					if (!isTest) {
						nonemptyBlock.renderPolygons(gl);
					}
				}
			}

		}
	}

	public void renderTerrain(GL2 gl) throws OpenGLException {
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
