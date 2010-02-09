package endrov.frivolous.model;

public abstract class PSF {

	abstract public float[] createPSF(Settings_new settings);
	
	static void shiftQuadrants(int w, int h, float[] x) {
		// TODO: göra en bättre shiftning?
		int k1P, k2P;
		float temp;
		int wHalf = w / 2;
		int hHalf = h / 2;

		for (int k2 = 0; k2 < hHalf; k2++) {
			k2P = k2 + hHalf;
			for (int k1 = 0; k1 < w; k1++) {
				temp = x[k1 + w * k2];
				x[k1 + w * k2] = x[k1 + w * k2P];
				x[k1 + w * k2P] = temp;
			}
		}
		for (int k1 = 0; k1 < wHalf; k1++) {
			k1P = k1 + wHalf;
			for (int k2 = 0; k2 < h; k2++) {
				temp = x[k1 + w * k2];
				x[k1 + w * k2] = x[k1P + w * k2];
				x[k1P + w * k2] = temp;
			}
		}
	}
}
