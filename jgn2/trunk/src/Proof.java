/*
 * Created on 31-mei-2006
 */
import java.io.UTFDataFormatException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Proof {
	static interface Prop {
		public String getName();

		public void get(Message message, ByteBuffer bb) throws Exception;

		public void set(Message message, ByteBuffer bb) throws Exception;
	}

	private static class Message {
		// stuff
	}

	private static class Test extends Message {
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
	}

	public static final void main(String[] args) throws Exception {
		{
			long a = System.nanoTime();
			long b = System.nanoTime();
			System.out.println((b) - a + "ns");
			if (true) System.exit(0);
		}
		Prop[] props = investigate(Test.class);
		ByteBuffer bb = ByteBuffer.allocate(1000);

		Test a = new Test();
		// a.setV("abx..xyz");
		a.setW(new short[] {1, 2, 3});
		a.setX(4);
		a.setY(5.6f);
		a.setZ(78.9f);

		bb.clear();
		get(a, props, bb);
		bb.flip();

		Thread.sleep(1000);

		int size = bb.remaining();
		System.out.println();
		System.out.println("length of serialized Test: " + size);
		System.out.println();

		Thread.sleep(1000);

		Test b = new Test();
		set(b, props, bb);
		// System.out.println("a.v=" + a.getV());
		System.out.println("a.w=" + a.getW()[0] + "," + a.getW()[1] + "," + a
				.getW()[2]);
		System.out.println("a.x=" + a.getX());
		System.out.println("a.y=" + a.getY());
		System.out.println("a.z=" + a.getZ());
		System.out.println();
		// System.out.println("b.v=" + b.getV());
		System.out.println("b.w=" + b.getW()[0] + "," + b.getW()[1] + "," + b
				.getW()[2]);
		System.out.println("b.x=" + b.getX());
		System.out.println("b.y=" + b.getY());
		System.out.println("b.z=" + b.getZ());

		Thread.sleep(1000);

		System.out.println();
		System.out.println("starting test...");
		long t0, t1;
		int runs = 10;
		int loops = 4 * 1024;

		for (int i = 0; i < runs; i++) {
			boolean print = i > runs / 2;

			// reflection get
			t0 = System.nanoTime();
			for (int j = 0; j < loops; j++) {
				bb.clear();
				get(a, props, bb);
				bb.flip();
			}
			t0 = System.nanoTime() - t0;

			// reflection set
			t1 = System.nanoTime();
			for (int j = 0; j < loops; j++) {
				bb.position(0);
				bb.limit(size);
				set(a, props, bb);
			}
			t1 = System.nanoTime() - t1;

			//

			if (print) {
				System.out
						.println("  reflection average speed:\tserialize=" + (t0 / loops) + "ns (" + 1000000L / (t0 / loops) + "k/s)\tdeserialize=" + (t1 / loops) + "ns (" + 1000000L / (t1 / loops) + "k/s)");
			}
		}
	}

	private static Prop[] investigate(Class clazz) throws Exception {
		List<Prop> props = new ArrayList<Prop>();

		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			String name = method.getName();
			if (!name.startsWith("get")) continue;

			// getter
			Method getter = method;
			String property = name.substring(3);

			// setter
			Method setter = null;
			for (Method m : methods)
				if (m.getName().equals("set" + property)) setter = m;

			// no setter found
			if (setter == null) continue;

			// returnType of getter
			// [must be]
			// first and only parameter of setter
			Class returnType = getter.getReturnType();
			Class[] parameters = setter.getParameterTypes();
			if (parameters.length != 1 || parameters[0] != returnType) continue;

			Class type = returnType;

			// ignore if
			// - not a primitive
			// - - not a primitive[]
			// - - not a String
			if (!type.isPrimitive()) {
				if (type.isArray() && !type.getComponentType().isPrimitive()) if (type != String.class) continue;
			}

			// get prop
			Prop prop = createPropForType(property, type, getter, setter);
			System.out
					.println("found getter/setter: " + type.getSimpleName() + " " + property);
			props.add(prop);
		}

		// ensure the methods are sorted in the same
		// order whatever the order of occurence
		Comparator<Prop> methodComparator = new Comparator<Prop>() {
			public int compare(Prop o1, Prop o2) {
				return o1.getName().compareTo(o2.getName());
			}
		};
		Collections.sort(props, methodComparator);

		return props.toArray(new Prop[props.size()]);
	}

	/**
	 * REFLECTION GET/SET
	 */

	public static final void get(Test t, Prop[] props, ByteBuffer bb) throws Exception {
		for (int i = 0; i < props.length; i++)
			props[i].get(t, bb);
	}

	public static final void set(Test t, Prop[] props, ByteBuffer bb) throws Exception {
		for (int i = 0; i < props.length; i++)
			props[i].set(t, bb);
	}

	/**
	 * PROPS
	 */

	private static final Prop createPropForType(String name, Class type,
			Method getter, Method setter) {
		if (type.isArray()) {
			if (type == boolean[].class) return new BooleanArrayProp(name, getter, setter);
			if (type == byte[].class) return new ByteArrayProp(name, getter, setter);
			if (type == char[].class) return new CharArrayProp(name, getter, setter);
			if (type == short[].class) return new ShortArrayProp(name, getter, setter);
			if (type == int[].class) return new IntArrayProp(name, getter, setter);
			if (type == long[].class) return new LongArrayProp(name, getter, setter);
			if (type == float[].class) return new FloatArrayProp(name, getter, setter);
			if (type == double[].class) return new DoubleArrayProp(name, getter, setter);
		} else {
			if (type == boolean.class) return new BooleanProp(name, getter, setter);
			if (type == byte.class) return new ByteProp(name, getter, setter);
			if (type == char.class) return new CharProp(name, getter, setter);
			if (type == short.class) return new ShortProp(name, getter, setter);
			if (type == int.class) return new IntProp(name, getter, setter);
			if (type == long.class) return new LongProp(name, getter, setter);
			if (type == float.class) return new FloatProp(name, getter, setter);
			if (type == double.class) return new DoubleProp(name, getter, setter);
		}

		if (type == String.class) return new StringProp(name, getter, setter);

		throw new IllegalArgumentException("unsupported type: " + type);
	}

	// boolean
	private static class BooleanProp implements Prop {
		final String name;

		final Method getter, setter;

		BooleanProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			bb.put(((Boolean) getter.invoke(message, EMPTY_ARRAY))
					.booleanValue() ? (byte) 1 : (byte) 0);
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			setter.invoke(message, new Object[] {new Boolean(bb.get() == 1)});
		}
	}

	// byte
	private static class ByteProp implements Prop {
		final String name;

		final Method getter, setter;

		ByteProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			bb.put(((Byte) getter.invoke(message, EMPTY_ARRAY)).byteValue());
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			setter.invoke(message, new Object[] {new Byte(bb.get())});
		}
	}

	// char
	private static class CharProp implements Prop {
		final String name;

		final Method getter, setter;

		CharProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			bb.putChar(((Character) getter.invoke(message, EMPTY_ARRAY))
					.charValue());
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			setter.invoke(message, new Object[] {new Character(bb.getChar())});
		}
	}

	// short
	private static class ShortProp implements Prop {
		final String name;

		final Method getter, setter;

		ShortProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			bb.putShort(((Short) getter.invoke(message, EMPTY_ARRAY))
					.shortValue());
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			setter.invoke(message, new Object[] {new Short(bb.getShort())});
		}
	}

	// int
	private static class IntProp implements Prop {
		final String name;

		final Method getter, setter;

		IntProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			bb.putInt(((Integer) getter.invoke(message, EMPTY_ARRAY))
					.intValue());
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			setter.invoke(message, new Object[] {new Integer(bb.getInt())});
		}
	}

	// long
	private static class LongProp implements Prop {
		final String name;

		final Method getter, setter;

		LongProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			bb
					.putLong(((Long) getter.invoke(message, EMPTY_ARRAY))
							.longValue());
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			setter.invoke(message, new Object[] {new Long(bb.getLong())});
		}
	}

	// float
	private static class FloatProp implements Prop {
		final String name;

		final Method getter, setter;

		FloatProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			bb.putFloat(((Float) getter.invoke(message, EMPTY_ARRAY))
					.floatValue());
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			setter.invoke(message, new Object[] {new Float(bb.getFloat())});
		}
	}

	// double
	private static class DoubleProp implements Prop {
		final String name;

		final Method getter, setter;

		DoubleProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			bb.putDouble(((Double) getter.invoke(message, EMPTY_ARRAY))
					.doubleValue());
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			setter.invoke(message, new Object[] {new Double(bb.getDouble())});
		}
	}

	// arrays

	// boolean[]
	private static class BooleanArrayProp implements Prop {
		final String name;

		final Method getter, setter;

		BooleanArrayProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			boolean[] array = (boolean[]) getter.invoke(message, EMPTY_ARRAY);
			if (array == null) {
				bb.putInt(-1);
				return;
			}
			bb.putInt(array.length);
			for (int i = 0; i < array.length; i++)
				bb.put(array[i] ? (byte) 1 : (byte) 0);
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			int len = bb.getInt();
			boolean[] array = null;
			if (len != -1) {
				array = new boolean[len];
				for (int i = 0; i < array.length; i++)
					array[i] = bb.get() == 1;
			}
			setter.invoke(message, new Object[] {array});
		}
	}

	// byte[]
	private static class ByteArrayProp implements Prop {
		final String name;

		final Method getter, setter;

		ByteArrayProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			byte[] array = (byte[]) getter.invoke(message, EMPTY_ARRAY);
			if (array == null) {
				bb.putInt(-1);
				return;
			}
			bb.putInt(array.length);
			bb.put(array);
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			int len = bb.getInt();
			byte[] array = null;
			if (len != -1) {
				array = new byte[len];
				bb.get(array, 0, array.length);
			}
			setter.invoke(message, new Object[] {array});
		}
	}

	// char[]
	private static class CharArrayProp implements Prop {
		final String name;

		final Method getter, setter;

		CharArrayProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			char[] array = (char[]) getter.invoke(message, EMPTY_ARRAY);
			if (array == null) {
				bb.putInt(-1);
				return;
			}
			bb.putInt(array.length);
			for (int i = 0; i < array.length; i++)
				bb.putChar(array[i]);
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			int len = bb.getInt();
			char[] array = null;
			if (len != -1) {
				array = new char[len];
				for (int i = 0; i < array.length; i++)
					array[i] = bb.getChar();
			}
			setter.invoke(message, new Object[] {array});
		}
	}

	// short[]
	private static class ShortArrayProp implements Prop {
		final String name;

		final Method getter, setter;

		ShortArrayProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			short[] array = (short[]) getter.invoke(message, EMPTY_ARRAY);
			if (array == null) {
				bb.putInt(-1);
				return;
			}
			bb.putInt(array.length);
			for (int i = 0; i < array.length; i++)
				bb.putShort(array[i]);
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			int len = bb.getInt();
			short[] array = null;
			if (len != -1) {
				array = new short[len];
				for (int i = 0; i < array.length; i++)
					array[i] = bb.getShort();
			}
			setter.invoke(message, new Object[] {array});
		}
	}

	// int[]
	private static class IntArrayProp implements Prop {
		final String name;

		final Method getter, setter;

		IntArrayProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			int[] array = (int[]) getter.invoke(message, EMPTY_ARRAY);
			if (array == null) {
				bb.putInt(-1);
				return;
			}
			bb.putInt(array.length);
			for (int i = 0; i < array.length; i++)
				bb.putInt(array[i]);
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			int len = bb.getInt();
			int[] array = null;
			if (len != -1) {
				array = new int[len];
				for (int i = 0; i < array.length; i++)
					array[i] = bb.getInt();
			}
			setter.invoke(message, new Object[] {array});
		}
	}

	// long[]
	private static class LongArrayProp implements Prop {
		final String name;

		final Method getter, setter;

		LongArrayProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			long[] array = (long[]) getter.invoke(message, EMPTY_ARRAY);
			if (array == null) {
				bb.putInt(-1);
				return;
			}
			bb.putInt(array.length);
			for (int i = 0; i < array.length; i++)
				bb.putLong(array[i]);
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			int len = bb.getInt();
			long[] array = null;
			if (len != -1) {
				array = new long[len];
				for (int i = 0; i < array.length; i++)
					array[i] = bb.getLong();
			}
			setter.invoke(message, new Object[] {array});
		}
	}

	// float[]
	private static class FloatArrayProp implements Prop {
		final String name;

		final Method getter, setter;

		FloatArrayProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			float[] array = (float[]) getter.invoke(message, EMPTY_ARRAY);
			if (array == null) {
				bb.putInt(-1);
				return;
			}
			bb.putInt(array.length);
			for (int i = 0; i < array.length; i++)
				bb.putFloat(array[i]);
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			int len = bb.getInt();
			float[] array = null;
			if (len != -1) {
				array = new float[len];
				for (int i = 0; i < array.length; i++)
					array[i] = bb.getFloat();
			}
			setter.invoke(message, new Object[] {array});
		}
	}

	// double[]
	private static class DoubleArrayProp implements Prop {
		final String name;

		final Method getter, setter;

		DoubleArrayProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			double[] array = (double[]) getter.invoke(message, EMPTY_ARRAY);
			if (array == null) {
				bb.putInt(-1);
				return;
			}
			bb.putInt(array.length);
			for (int i = 0; i < array.length; i++)
				bb.putDouble(array[i]);
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			int len = bb.getInt();
			double[] array = null;
			if (len != -1) {
				array = new double[len];
				for (int i = 0; i < array.length; i++)
					array[i] = bb.getDouble();
			}
			setter.invoke(message, new Object[] {array});
		}
	}

	// String
	private static class StringProp implements Prop {
		final String name;

		final Method getter, setter;

		StringProp(String name, Method getter, Method setter) {
			this.name = name;
			this.getter = getter;
			this.setter = setter;
		}

		public String getName() {
			return name;
		}

		public void get(Message message, ByteBuffer bb) throws Exception {
			String str = (String) getter.invoke(message, EMPTY_ARRAY);
			if (str == null) {
				bb.putShort((short) 0xFFFF);
				return;
			}

			// UTF8
			byte[] utf8;
			{
				int strlen = str.length();
				int utflen = 0;
				int c, count = 0;

				for (int i = 0; i < strlen; i++) {
					c = str.charAt(i);
					if ((c >= 0x0001) && (c <= 0x007F)) utflen++;
					else if (c > 0x07FF) utflen += 3;
					else utflen += 2;
				}

				if (utflen > 65535) throw new UTFDataFormatException("encoded string too long: " + utflen + " bytes");

				utf8 = new byte[utflen + 2];
				utf8[count++] = (byte) ((utflen >>> 8) & 0xFF);
				utf8[count++] = (byte) ((utflen >>> 0) & 0xFF);

				int i = 0;
				for (i = 0; i < strlen; i++) {
					c = str.charAt(i);
					if (!((c >= 0x0001) && (c <= 0x007F))) break;
					utf8[count++] = (byte) c;
				}

				for (; i < strlen; i++) {
					c = str.charAt(i);
					if ((c >= 0x0001) && (c <= 0x007F)) {
						utf8[count++] = (byte) c;
					} else if (c > 0x07FF) {
						utf8[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
						utf8[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
						utf8[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
					} else {
						utf8[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
						utf8[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
					}
				}
			}

			bb.put(utf8, 0, utf8.length);
		}

		public void set(Message message, ByteBuffer bb) throws Exception {
			int utflen = bb.getShort() & 0xFFFF;
			String utf8 = null;
			if (utflen != 0xFFFF) {
				byte[] bytearr = new byte[utflen];
				char[] chararr = new char[utflen];

				int c, char2, char3;
				int count = 0;
				int chararr_count = 0;

				bb.get(bytearr, 0, utflen);

				while (count < utflen) {
					c = bytearr[count] & 0xFF;
					if (c > 127) break;
					count++;
					chararr[chararr_count++] = (char) c;
				}

				while (count < utflen) {
					c = bytearr[count] & 0xFF;
					switch (c >> 4) {
						case 0:
						case 1:
						case 2:
						case 3:
						case 4:
						case 5:
						case 6:
						case 7:
							/* 0xxxxxxx */
							count++;
							chararr[chararr_count++] = (char) c;
							break;
						case 12:
						case 13:
							/* 110x xxxx 10xx xxxx */
							count += 2;
							if (count > utflen) throw new UTFDataFormatException("malformed input: partial character at end");
							char2 = bytearr[count - 1];
							if ((char2 & 0xC0) != 0x80) throw new UTFDataFormatException("malformed input around byte " + count);
							chararr[chararr_count++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
							break;
						case 14:
							/* 1110 xxxx 10xx xxxx 10xx xxxx */
							count += 3;
							if (count > utflen) throw new UTFDataFormatException("malformed input: partial character at end");
							char2 = bytearr[count - 2];
							char3 = bytearr[count - 1];
							if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) throw new UTFDataFormatException("malformed input around byte " + (count - 1));
							chararr[chararr_count++] = (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));
							break;
						default:
							/* 10xx xxxx, 1111 xxxx */
							throw new UTFDataFormatException("malformed input around byte " + count);
					}
				}
				// The number of chars produced may be less than utflen
				utf8 = new String(chararr, 0, chararr_count);
			}
			setter.invoke(message, new Object[] {utf8});
		}
	}

	// 
	static final Object[] EMPTY_ARRAY = new Object[0];
}