package running.proto;

public interface IPacket {
	void encode(IPacketBuffer buffer);
	void decode(IPacketBuffer buffer);
}
