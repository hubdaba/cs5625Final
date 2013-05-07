package cs5625.deferred.rendering;

import java.nio.FloatBuffer;
import java.util.HashMap;

public interface Attributable {
	public String[] getRequiredVertexAttributes();
	public HashMap<String, FloatBuffer> getVertexAttribData();
	public int getVertexCount();
}
