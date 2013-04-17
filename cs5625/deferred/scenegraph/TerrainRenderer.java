package cs5625.deferred.scenegraph;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.media.opengl.GL2;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;

import cs5625.deferred.materials.LambertianMaterial;
import cs5625.deferred.materials.Material;
import cs5625.deferred.materials.TerrainMaterial;
import cs5625.deferred.misc.OpenGLException;
import cs5625.deferred.rendering.ShaderProgram;

public class TerrainRenderer extends SceneObject {
	
	private List<TerrainBlockRenderer> blocks; 
	
	private boolean needSetup = true;
	private Material terrainMaterial;
	private boolean isTest;
	
	public TerrainRenderer(boolean isTest) {
		blocks = new LinkedList<TerrainBlockRenderer>();
		this.isTest = isTest;
		if (isTest) {
			terrainMaterial = new LambertianMaterial(new Color3f(1.0f, 0.0f, 0.0f));
		} else {
			terrainMaterial = new TerrainMaterial();
		}
	}
	
	// set up blocks according to view frustum
	public void setup(GL2 gl) throws OpenGLException, IOException {
		if (!needSetup) {
			return;
		}
		/*
		TerrainBlockRenderer tbr = new TerrainBlockRenderer(gl, new Vector3f(0, 0, 0));
		tbr.fillTexture3D(gl, width, height);
		terrainMaterial.setDiffuseTexture3D(tbr.getTexture3D());
		blocks.add(tbr);
		
		needSetup = false;
		TerrainBlockRenderer tbr = new TerrainBlockRenderer(gl, new Vector3f(-1, -1, -1));
		tbr.fillTexture3D(gl);
		blocks.add(tbr);
		TerrainBlockRenderer tbr2 = new TerrainBlockRenderer(gl, new Vector3f(-1, -1, -2));
		tbr.fillTexture3D(gl);
		blocks.add(tbr2);
		OpenGLException.checkOpenGLError(gl);
		*/
		for (float z = -5; z <= 5; z += 1) {
			for (float x = -2; x < 2; x += 1) {
				for (float y = -1; y < 2; y += 1) {
					TerrainBlockRenderer tbr = new TerrainBlockRenderer(gl, new Vector3f(x, y, z));
					tbr.fillTexture3D(gl);
					if (!isTest) {
						tbr.renderPolygons(gl);
					}
					blocks.add(tbr);
					System.out.println(x + " " + y + " " + z);
					OpenGLException.checkOpenGLError(gl);
				}
			}
		}
		needSetup = false;
		System.out.println("finished");
	}
	
	public void renderTerrain(GL2 gl) throws OpenGLException {
		for (TerrainBlockRenderer block : blocks) {
			if (!isTest) {
				((TerrainMaterial) terrainMaterial).setDensityFunction(block.getTexture3D());
				((TerrainMaterial) terrainMaterial).setLowerCorner(block.getLowerCorner());
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
}
