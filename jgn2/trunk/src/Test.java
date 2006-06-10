import com.captiveimagination.jgn.*;
import com.captiveimagination.jgn.message.*;

public class Test extends Message {
	// String v;
	short[] w;

	int x;

	float y;

	double z;

	// public void setV(String v)
	// {
	// this.v = v;
	// }

	// public String getV()
	// {
	// return v;
	// }

	public void setW(short[] w) {
		this.w = w;
	}

	public short[] getW() {
		return w;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getX() {
		return x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public float getY() {
		return y;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public double getZ() {
		return z;
	}

	public boolean equals(Object o) {
		if (o instanceof Test) {
			Test t = (Test) o;
			if (getX() == t.getX()) {
				if (getY() == t.getY()) {
					if (getZ() == t.getZ()) {
						if (getW() == t.getW()) {
							return true;
						} else if ((getW() != null) && (t.getW() != null)) {
							if (getW().length == t.getW().length) {
								for (int i = 0; i < getW().length; i++) {
									if (getW()[i] != t.getW()[i]) return false;
								}
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
}
