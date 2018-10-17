package running.proto;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface IPacketBuffer {
	/**
	 * Buffer
	 */
	ByteBuffer getBuffer();
	void setBuffer(ByteBuffer buffer);
	int position();

	/**
	 * 读取数据
	 */
	boolean readBoolean();
	byte readByte();
	short readShort();
	int readInt();
	long readLong();
	int readVar(); //可变长度整型
	String readString();
	<T extends IPacket> T readObject(T t);
	<T extends IPacket> T readObject(Class<T> clazz);
	<T> T readArray(Class<?> cls);
	<T> List<T> readList(Class<T> cls);
	<K, V> Map<K, V> readMap(Class<K> clsK, Class<V> clsV);

	int readStart(); //开始读取一个对象，返回数据长度
	boolean readEnd(int tag); //当前对象是否读取结束

	/**
	 * 读取并检测字段类型，如果不符合，则跳过
	 */
	boolean readSkip();
	boolean readSkip(int t1);
	boolean readSkip(int t1, int t2);
	boolean readSkip(int t1, int t2, int t3);

	/**
	 * 写入数据
	 */
	void writeBoolean(boolean b);
	void writeByte(byte b);
	void writeShort(short s);
	void writeInt(int i);
	void writeLong(long l);
	void writeVar(int v); //可变长度整型
	void writeString(String s);
	void writeObject(IPacket obj);
	void writeArray(boolean[] arr);
	void writeArray(byte[] arr);
	void writeArray(short[] arr);
	void writeArray(int[] arr);
	void writeArray(long[] arr);
	void writeArray(String[] arr);
	void writeList(List<?> list);
	void writeMap(Map<?,?> map);

	int writeStart(); //开始写入一个对象
	void writeEnd(int tag);
}
