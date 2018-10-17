/*
 * Copyright 2013-2018 featherrun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package running.proto;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

public class PacketBuffer implements IPacketBuffer {
	protected ByteBuffer buffer;

	@Override
	public ByteBuffer getBuffer() {
		return buffer;
	}

	@Override
	public void setBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public int position() {
		return buffer.position();
	}

	//------------------------------------------------------------------------

	/**
	 * 读取数据
	 */
	@SuppressWarnings("unchecked")
	protected <T> T readValue(Class<T> cls) {
		if (IPacket.class.isAssignableFrom(cls)) {
			return (T) readObject((Class<? extends IPacket>) cls);
		} else if (cls == Boolean.class) {
			return (T) Boolean.valueOf(readBoolean());
		} else if (cls == Byte.class) {
			return (T) Byte.valueOf(readByte());
		} else if (cls == Short.class) {
			return (T) Short.valueOf(readShort());
		} else if (cls == Integer.class) {
			return (T) Integer.valueOf(readInt());
		} else if (cls == Long.class) {
			return (T) Long.valueOf(readLong());
		} else if (cls == String.class) {
			return (T) readString();
		} else {
			return null;
		}
	}

	public boolean readBoolean() {
		return buffer.get() == 1;
	}
	public byte readByte() {
		return buffer.get();
	}
	public short readShort() {
		return buffer.getShort();
	}
	public int readInt() {
		return buffer.getInt();
	}
	public long readLong() {
		return buffer.getLong();
	}

	public int readVar() {
		byte b = buffer.get();
		if ((b & 0x80) == 0) {
			return b;
		}
		int result = b & 0x7f;
		int offset = 7;
		for (; offset < 32; offset += 7) {
			b = buffer.get();
			result |= (b & 0x7f) << offset;
			if ((b & 0x80) == 0) {
				return result;
			}
		}
		return 0;
	}

	public String readString() {
		int size = readVar();
		byte[] bytes = new byte[size];
		buffer.get(bytes);
		return new String(bytes, Charset.defaultCharset());
	}

	public <T extends IPacket> T readObject(T t) {
		t.decode(this);
		return t;
	}

	@SuppressWarnings("unchecked")
	public <T extends IPacket> T readObject(Class<T> clazz) {
		IPacket t;
		try {
			t = clazz.newInstance();
		} catch (Exception e) {
			skip(readVar());
			return null;
		}
		return (T) readObject(t);
	}

	@SuppressWarnings("unchecked")
	public <T> T readArray(Class<?> cls) {
		int size = readVar();
		if (size == 0) {
			return null;
		} else if (cls == boolean.class) {
			boolean[] arr = new boolean[size];
			for (int i = 0; i < size; i++) {
				arr[i] = readBoolean();
			}
			return (T) arr;
		} else if (cls == byte.class) {
			byte[] arr = new byte[size];
			for (int i = 0; i < size; i++) {
				arr[i] = readByte();
			}
			return (T) arr;
		} else if (cls == short.class) {
			short[] arr = new short[size];
			for (int i = 0; i < size; i++) {
				arr[i] = readShort();
			}
			return (T) arr;
		} else if (cls == int.class) {
			int[] arr = new int[size];
			for (int i = 0; i < size; i++) {
				arr[i] = readInt();
			}
			return (T) arr;
		} else if (cls == long.class) {
			long[] arr = new long[size];
			for (int i = 0; i < size; i++) {
				arr[i] = readLong();
			}
			return (T) arr;
		} else if (cls == String.class) {
			String[] arr = new String[size];
			for (int i = 0; i < size; i++) {
				arr[i] = readString();
			}
			return (T) arr;
		} else {
			Object arr = Array.newInstance(cls, size);
			for (int i = 0; i < size; i++) {
				Array.set(arr, i, readValue(cls));
			}
			return (T) arr;
		}
	}

	public <T> List<T> readList(Class<T> cls) {
		int size = readVar();
		if (size == 0) {
			return null;
		} else {
			List<T> list = new ArrayList<>();
			for (int i = 0; i < size; i++) {
				list.add(readValue(cls));
			}
			return list;
		}
	}

	public <K, V> Map<K, V> readMap(Class<K> clsK, Class<V> clsV) {
		int size = readVar();
		if (size == 0) {
			return null;
		} else {
			Map<K, V> map = new HashMap<>();
			for (int i = 0; i < size; i++) {
				map.put(readValue(clsK), readValue(clsV));
			}
			return map;
		}
	}

	public int readStart() {
		return this.readInt() + this.position();
	}

	public boolean readEnd(int tag) {
		return this.position() >= tag;
	}

	/**
	 * 检查字段类型
	 */
	public boolean readSkip() {
		return readSkip(0, 0, 0);
	}

	public boolean readSkip(int t1) {
		return readSkip(t1, 0, 0);
	}

	public boolean readSkip(int t1, int t2) {
		return readSkip(t1, t2, 0);
	}

	public boolean readSkip(int t1, int t2, int t3) {
		int r1 = readVar();
		int r2 = 0;
		int r3 = 0;
		if (t1 == PacketType.MAP) {
			r2 = readVar();
			r3 = readVar();
		} else if (t1 == PacketType.LIST || t1 == PacketType.ARRAY) {
			r2 = readVar();
		}
		if (r1 == t1 && r2 == t2 && r3 == t3) {
			return true;
		}
		if (t1 == PacketType.MAP) {
			int size = readVar();
			for (int i = 0; i < size; i++) {
				readSkipType(r2);
				readSkipType(r3);
			}
		} else if (t1 == PacketType.LIST || t1 == PacketType.ARRAY) {
			int size = readVar();
			for (int i = 0; i < size; i++) {
				readSkipType(r2);
			}
		} else {
			readSkipType(t1);
		}
		return false;
	}

	protected void readSkipType(int t) {
		switch (t) {
			case PacketType.BOOLEAN: skip(1); break;
			case PacketType.BYTE: skip(1); break;
			case PacketType.SHORT: skip(2); break;
			case PacketType.INT: skip(4); break;
			case PacketType.LONG: skip(8); break;
			case PacketType.VAR: readVar(); break;
			case PacketType.STRING: skip(readVar()); break;
			default: skip(readInt()); break;
		}
	}

	protected void skip(int add) {
		buffer.position(buffer.position() + add);
	}


	//------------------------------------------------------------------------

	/**
	 * 写入数据
	 */
	protected void writeValue(Object t) {
		if (t == null || t instanceof IPacket) {
			writeObject((IPacket) t);
		} else {
			Class<?> cls = t.getClass();
			if (cls == Boolean.class) {
				writeBoolean((Boolean) t);
			} else if (cls == Byte.class) {
				writeByte((Byte) t);
			} else if (cls == Short.class) {
				writeShort((Short) t);
			} else if (cls == Integer.class) {
				writeInt((Integer) t);
			} else if (cls == Long.class) {
				writeLong((Long) t);
			} else if (cls == String.class) {
				writeString((String) t);
			}
		}
	}

	public void writeBoolean(boolean b) {
		buffer.put((byte) (b ? 1 : 0));
	}
	public void writeByte(byte b) {
		buffer.put(b);
	}
	public void writeShort(short s) {
		buffer.putShort(s);
	}
	public void writeInt(int i) {
		buffer.putInt(i);
	}
	public void writeLong(long l) {
		buffer.putLong(l);
	}

	public void writeVar(int v) {
		while (true) {
			if ((v & ~0x7F) == 0) {
				buffer.put((byte) v);
				return;
			} else {
				buffer.put((byte) ((v & 0x7F) | 0x80));
				v >>>= 7;
			}
		}
	}

	public void writeString(String s) {
		if (s == null || s.isEmpty()) {
			writeVar(0);
		} else {
			byte[] bytes = s.getBytes();
			writeVar(bytes.length);
			buffer.put(bytes);
		}
	}

	public void writeObject(IPacket obj) {
		if (obj == null) {
			writeInt(0);
		} else {
			obj.encode(this);
		}
	}

	public void writeArray(boolean[] arr) {
		if (arr == null) {
			writeVar(0);
		} else {
			writeVar(arr.length);
			for (boolean t : arr) {
				writeBoolean(t);
			}
		}
	}

	public void writeArray(byte[] arr) {
		if (arr == null) {
			writeVar(0);
		} else {
			writeVar(arr.length);
			for (byte t : arr) {
				writeByte(t);
			}
		}
	}

	public void writeArray(short[] arr) {
		if (arr == null) {
			writeVar(0);
		} else {
			writeVar(arr.length);
			for (short t : arr) {
				writeShort(t);
			}
		}
	}

	public void writeArray(int[] arr) {
		if (arr == null) {
			writeVar(0);
		} else {
			writeVar(arr.length);
			for (int t : arr) {
				writeInt(t);
			}
		}
	}

	public void writeArray(long[] arr) {
		if (arr == null) {
			writeVar(0);
		} else {
			writeVar(arr.length);
			for (long t : arr) {
				writeLong(t);
			}
		}
	}

	public void writeArray(String[] arr) {
		if (arr == null) {
			writeVar(0);
		} else {
			writeVar(arr.length);
			for (String t : arr) {
				writeString(t);
			}
		}
	}

	public void writeList(List<?> list) {
		if (list == null) {
			writeVar(0);
		} else {
			writeVar(list.size());
			for (Object t : list) {
				writeValue(t);
			}
		}
	}

	public void writeMap(Map<?,?> map) {
		if (map == null) {
			writeVar(0);
		} else {
			writeVar(map.size());
			for (Map.Entry<?,?> t : map.entrySet()) {
				writeValue(t.getKey());
				writeValue(t.getValue());
			}
		}
	}

	public int writeStart() {
		skip(4);
		return this.position();
	}

	public void writeEnd(int tag) {
		this.buffer.putInt(tag-4, this.position()-tag);
	}
}
