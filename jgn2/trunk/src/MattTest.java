import java.nio.*;

import com.captiveimagination.jgn.Message;
import com.captiveimagination.jgn.convert.*;

/**
 * MattTest.java Created: Jun 3, 2006
 */

/**
 * @author Matthew D. Hicks
 */
public class MattTest {
	public static void main(String[] args) throws Exception {
		Test test = new Test();
		test.setW(new short[] {(short) 1, (short) 2, (short) 3});
		test.setX(50);
		test.setY(3.4f);
		test.setZ(500.5f);

		ByteBuffer bb = ByteBuffer.allocate(1000);

		ConversionHandler handler = ConversionHandler
				.getConversionHandler(Test.class);
		int max = 1000000;
		long bytes = 0;
		long time = System.nanoTime();
		for (int i = 0; i < max; i++) {
			bytes += bb.position() * 2;
			bb.clear();
			handler.sendMessage(test, bb);
			bb.flip();
			Test t2 = (Test) handler.receiveMessage(bb);
			// System.out.println("Equals: " + test.equals(t2));
		}
		long finalTime = (System.nanoTime() - time) / 1000000;
		System.out
				.println("Took: " + finalTime + " ms for " + bytes + " bytes");
	}
}
