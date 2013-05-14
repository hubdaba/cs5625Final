package cs5625.deferred.rendering;

public class ShadowCamera extends Camera {
	protected int width=0, height=0;

	public int getShadowWidth() {
		return width;
	}

	public void setShadowWidth(int width) {
		this.width = width;
	}

	public int getShadowHeight() {
		return height;
	}

	public void setShadowHeight(int height) {
		this.height = height;
	}
}
