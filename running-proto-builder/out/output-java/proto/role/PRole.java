/**
 * Created by running-proto-builder
 */
package proto.role;

import java.util.*;
import running.proto.*;


/***/
public final class PRole implements IPacket {
	public String uid;
	public String nickname;
	public int level;

	@Override
	public void encode(IPacketBuffer res) {
		int tag = res.writeStart();
		if (this.uid != null) {
			res.writeVar(1);
			res.writeVar(7);
			res.writeString(this.uid);
		}
		if (this.nickname != null) {
			res.writeVar(2);
			res.writeVar(7);
			res.writeString(this.nickname);
		}
		if (this.level != 0) {
			res.writeVar(3);
			res.writeVar(4);
			res.writeInt(this.level);
		}
		res.writeEnd(tag);
	}

	@Override
	public void decode(IPacketBuffer req) {
		int tag = req.readStart();
		while (!req.readEnd(tag)) {
			int code = req.readVar();
			switch (code) {
				case 1: if (req.readSkip(7)) this.uid = req.readString(); break;
				case 2: if (req.readSkip(7)) this.nickname = req.readString(); break;
				case 3: if (req.readSkip(4)) this.level = req.readInt(); break;
				default: req.readSkip(); break;
			}
		}
	}
}
