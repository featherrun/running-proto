/**
 * Created by running-proto-builder
 */
package proto.login;

import java.util.*;
import running.proto.*;


/***/
public final class PLogin implements IPacket {
	public String name; //登录名
	public String password; //密钥
	public String platform; //所属平台

	@Override
	public void encode(IPacketBuffer res) {
		int tag = res.writeStart();
		if (this.name != null) {
			res.writeVar(1);
			res.writeVar(7);
			res.writeString(this.name);
		}
		if (this.password != null) {
			res.writeVar(2);
			res.writeVar(7);
			res.writeString(this.password);
		}
		if (this.platform != null) {
			res.writeVar(3);
			res.writeVar(7);
			res.writeString(this.platform);
		}
		res.writeEnd(tag);
	}

	@Override
	public void decode(IPacketBuffer req) {
		int tag = req.readStart();
		while (!req.readEnd(tag)) {
			int code = req.readVar();
			switch (code) {
				case 1: if (req.readSkip(7)) this.name = req.readString(); break;
				case 2: if (req.readSkip(7)) this.password = req.readString(); break;
				case 3: if (req.readSkip(7)) this.platform = req.readString(); break;
				default: req.readSkip(); break;
			}
		}
	}
}
