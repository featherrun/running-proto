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

package running.proto.builder;

import running.core.ILogger;
import running.core.Running;
import running.proto.PacketType;
import running.util.FileUtils;
import running.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;

public class Proto2Json {
	final ILogger logger = Running.getLogger(Proto.class);
	final FileUtils fileUtils = Running.get(FileUtils.class);
	final JsonUtils jsonUtils = Running.get(JsonUtils.class);

	public Proto2Json(final Proto proto, final String output) {
		JsonProto jp = new JsonProto();
		jp.time = (int) (proto.lastModified / 1000);
		for (Proto.ProtoPackage protoPackage : proto.packages) {
			for (Proto.ProtoClass protoClass : protoPackage.classes) {
				JsonProtoClass jpc = new JsonProtoClass();
				jp.classes.add(jpc);
				jpc.id = protoClass.id;
				jpc.name = protoClass.fullName;
				for (Proto.ProtoField field : protoClass.fields) {
					JsonProtoField jpf = new JsonProtoField();
					jpc.fields.add(jpf);
					jpf.code = field.code;
					jpf.name = field.name;
					jpf.type = field.t1;
					jpf.t1 = field.t2;
					jpf.t2 = field.t3;
					if (field.t1 == PacketType.OBJECT) {
						jpf.type = proto.findClass(field.type).id;
					}
					if (field.t2 == PacketType.OBJECT) {
						jpf.t1 = proto.findClass(field.type2).id;
					}
					if (field.t3 == PacketType.OBJECT) {
						jpf.t2 = proto.findClass(field.type3).id;
					}
				}
			}
		}

		String json = jsonUtils.stringify(jp);
		String path = output;
		int i1 = path.lastIndexOf('.');
		int i2 = path.lastIndexOf('/');
		if (i1 <= i2) {
			fileUtils.mkdirs(output);
			path += proto.getSimpleName()+".txt";
		} else {
			fileUtils.mkdirs(output.substring(0, i2));
		}
		fileUtils.save(path, json);
		logger.info(path);
	}


	public static class JsonProto {
		public int time;
		public List<JsonProtoClass> classes = new ArrayList<>();
	}

	public static class JsonProtoClass {
		public int id;
		public String name;
		public List<JsonProtoField> fields = new ArrayList<>();
	}

	public static class JsonProtoField {
		public int code;
		public String name;
		public int type;
		public int t1;
		public int t2;
	}

}
