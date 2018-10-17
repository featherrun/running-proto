/**
 * Created by running-proto-builder
 */
package proto.login;

import java.util.*;
import running.proto.*;


/**测试*/
public final class PSomeRpc implements IPacket {
	public String some;
	public List<String> someList;
	public Map<Integer,String> someMap;
	public int[] someArray;

	@Override
	public void encode(IPacketBuffer res) {
		int tag = res.writeStart();
		if (this.some != null) {
			res.writeVar(1);
			res.writeVar(7);
			res.writeString(this.some);
		}
		if (this.someList != null) {
			res.writeVar(2);
			res.writeVar(12);
			res.writeVar(7);
			res.writeList(this.someList);
		}
		if (this.someMap != null) {
			res.writeVar(3);
			res.writeVar(13);
			res.writeVar(4);
			res.writeVar(7);
			res.writeMap(this.someMap);
		}
		if (this.someArray != null) {
			res.writeVar(4);
			res.writeVar(11);
			res.writeVar(4);
			res.writeArray(this.someArray);
		}
		res.writeEnd(tag);
	}

	@Override
	public void decode(IPacketBuffer req) {
		int tag = req.readStart();
		while (!req.readEnd(tag)) {
			int code = req.readVar();
			switch (code) {
				case 1: if (req.readSkip(7)) this.some = req.readString(); break;
				case 2: if (req.readSkip(12, 7)) this.someList = req.readList(String.class); break;
				case 3: if (req.readSkip(13, 4, 7)) this.someMap = req.readMap(Integer.class, String.class); break;
				case 4: if (req.readSkip(11, 4)) this.someArray = req.readArray(); break;
				default: req.readSkip(); break;
			}
		}
	}
}
