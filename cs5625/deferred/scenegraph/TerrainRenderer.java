package cs5625.deferred.scenegraph;

import geometry.Explosion;
import geometry.ExplosionHandler;
import geometry.QuadSampler;
import geometry.SuperBlock;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
	public static int NUM_VOXELS = 15;
	public static int LOW_NUM_VOXELS = 5;
	public static float CLOSE_DISTANCE = 100;

	private BlockingQueue<QuadSampler> explodedBlocks;
	private List<QuadSampler> blocksToRender;

	
	private ExplosionHandler explosionHandler;

	public TerrainRenderer(boolean isTest, ExplosionHandler explosionHandler,
			List<QuadSampler> blocksToRender) {
		blocks = new HashMap<Point3f, TerrainBlockRenderer>();
		this.explosionHandler = explosionHandler;
		explodedBlocks = new LinkedBlockingQueue<QuadSampler>();
		this.blocksToRender = blocksToRender;

		this.isTest = isTest;
		if (isTest) {
			terrainMaterial = new LambertianMaterial(new Color3f(1.0f, 0.0f, 0.0f));
		} else {
			terrainMaterial = new TerrainMaterial();
		}
	}

	
	public void renderPolygons(GL2 gl, Camera camera) throws OpenGLException, IOException {
		if (!needSetup) {
			return;
		}
		
		SuperBlock renderArea = SuperBlock.midpointDistanceBlock(camera.getPosition(), camera.getFar() + 1);
		
		Set<QuadSampler> lastExplodedBlocks = new HashSet<QuadSampler>();
		explodedBlocks.drainTo(lastExplodedBlocks);
		List<SuperBlock> farBlocks = new LinkedList<SuperBlock>();
		for (SuperBlock block : blocks.values()) {
			if (!renderArea.containsBlock(block)) {
				farBlocks.add(block);
			}
		}
		for (SuperBlock block : farBlocks){
			if (blocks.containsKey(block.getMinPoint())) {
				((TerrainBlockRenderer)block).releaseGPUResources(gl);
				blocks.remove(block.getMinPoint());
			}
		}
		for (QuadSampler explodedBlock : lastExplodedBlocks) {
			TerrainBlockRenderer block = blocks.get(explodedBlock.getMinPoint());
			if (block != null) {
				block.releaseGPUResources(gl);
				blocks.remove(explodedBlock.getMinPoint());
			}
		}
		
		if (blocksToRender != null) {
			for (QuadSampler quad : blocksToRender) {
				if (!blocks.containsKey(quad.getMinPoint())) {
					TerrainBlockRenderer renderer = new TerrainBlockRenderer(gl, quad.getMinPoint(),
							NUM_VOXELS, quad.getSideLength(), explosionHandler.getExplosions(quad));;
					blocks.put(quad.getMinPoint(), renderer);
					if (!isTest) {
						renderer.renderPolygons(gl);
						OpenGLException.checkOpenGLError(gl);
					}
				}
			}
		}
		Tuple3f minPoint = renderArea.getMinPoint();
		minPoint.x = (float) (Math.floor(minPoint.x/BLOCK_SIZE) * BLOCK_SIZE);
		minPoint.y = (float) (Math.floor(minPoint.y/BLOCK_SIZE) * BLOCK_SIZE);
		minPoint.z = (float) (Math.floor(minPoint.z/BLOCK_SIZE) * BLOCK_SIZE);
		int diameter = (int)(Math.ceil(renderArea.getSideLength()/BLOCK_SIZE) * BLOCK_SIZE);
		for (float x = minPoint.x; x < diameter + minPoint.x; x+=BLOCK_SIZE) {
			for (float y = minPoint.y; y < diameter + minPoint.y; y+=BLOCK_SIZE) {
				for (float z = minPoint.z; z < diameter + minPoint.z; z += BLOCK_SIZE) {
					Point3f lowerCorner = new Point3f(x, y, z);
					if (camera.inFrustum(new QuadSampler(lowerCorner, BLOCK_SIZE))) {
						if (!blocks.containsKey(lowerCorner)) {
							TerrainBlockRenderer renderer = new TerrainBlockRenderer(gl, lowerCorner,
									NUM_VOXELS, BLOCK_SIZE,
									explosionHandler.getExplosions(new QuadSampler(lowerCorner, BLOCK_SIZE)));
							blocks.put(lowerCorner, renderer);
							renderer.fillTexture3D(gl);
							OpenGLException.checkOpenGLError(gl);
							if (!isTest) {
								renderer.renderPolygons(gl);
								OpenGLException.checkOpenGLError(gl);
							}
						}
					}
				
				}
			}
		}

		needSetup = false;
	}

	public void renderTerrain(GL2 gl) throws OpenGLException {
		for (TerrainBlockRenderer block : blocks.values()) {
			if (!isTest) {
				((TerrainMaterial) terrainMaterial).setExplosionPositions(block.getExplosionPositions());
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
		this.update(o, null);
	}

	public float evaluate(Point3f point) throws OpenGLException {
		Point3f blockMin = new Point3f();
		blockMin.x = (float) (Math.floor(point.x / BLOCK_SIZE) * BLOCK_SIZE);
		blockMin.y = (float) (Math.floor(point.y / BLOCK_SIZE) * BLOCK_SIZE);
		blockMin.z = (float) (Math.floor(point.z / BLOCK_SIZE) * BLOCK_SIZE);
		Point3f difference = new Point3f(point);
		difference.sub(blockMin);
		difference.scale((float) (1.0/BLOCK_SIZE));
		if (blocks.containsKey(blockMin)) {
			TerrainBlockRenderer block = blocks.get(blockMin);
			float sampled = block.getTexture3D().sample3D(difference);
			return sampled;
		}
		return 10000;
	}


	@Override
	public void update(Observerable o, Object obj) {
		this.needSetup = true;
		if (obj == null) {
			return;
		}
		if (obj instanceof Explosion) {
			Explosion explosion = (Explosion)obj;
			List<QuadSampler> affectedBlocks = explosion.getAffectedBlocks(BLOCK_SIZE);
			for (QuadSampler affectedBlock : affectedBlocks) {
				explodedBlocks.offer(affectedBlock);
			}
		}
	}

}
