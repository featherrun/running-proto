/**
 * Created by running-proto-builder
 */
package proto.login;

import java.util.*;
import running.proto.*;
import proto.role.PRole;

/***/
public final class PLoginResult implements IPacket {
	public byte result; //登录结果, 0-成功, 1-失败
	public PRole roleData;

	@Override
	public void encode(IPacketBuffer res) {
		int tag = res.writeStart();
		if (this.result != 0) {
			res.writeVar(1);
			res.writeVar(2);
			res.writeByte(this.result);
		}
		if (this.roleData != null) {
			res.writeVar(2);
			res.writeVar(8);
			res.writeObject(this.roleData);
		}
		res.writeEnd(tag);
	}

	@Override
	public void decode(IPacketBuffer req) {
		int tag = req.readStart();
		while (!req.readEnd(tag)) {
			int code = req.readVar();
			switch (code) {
				case 1: if (req.readSkip(2)) this.result = req.readByte(); break;
				case 2: if (req.readSkip(8)) this.roleData = req.readObject(new PRole()); break;
				default: req.readSkip(); break;
			}
		}
	}
}
