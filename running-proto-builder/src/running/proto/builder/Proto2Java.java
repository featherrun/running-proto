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

import running.core.Logger;
import running.core.Running;
import running.help.ClassBuilder;
import running.proto.PacketType;
import running.util.FileUtils;

public class Proto2Java {
	final Logger logger = Running.getLogger(Proto.class);
	final FileUtils fileUtils = Running.get(FileUtils.class);

	final String templateClass;
	final String templateConst;
	final String templateEnum;
	final Proto proto;

	public Proto2Java(final Proto proto, final String output) {
		templateClass = fileUtils.getResource("template/java.proto.class.template");
		templateConst = fileUtils.getResource("template/java.proto.const.template");
		templateEnum = fileUtils.getResource("template/java.proto.enum.template");
		this.proto = proto;

		fileUtils.delete(output + proto.getSimpleName());
		fileUtils.mkdirs(output);
		for (Proto.ProtoPackage protoPackage : proto.packages) {
			createPackage(protoPackage, output);
		}
		createPacketMod(output);
		createPacketMethod(output);
	}

	private void createPackage(final Proto.ProtoPackage protoPackage, final String output) {
		String path = output + protoPackage.name.replace('.', '/') + '/';
		fileUtils.mkdirs(path);
		for (Proto.ProtoClass protoClass : protoPackage.classes) {
			createClass(protoClass, path);
		}
	}

	private void createClass(final Proto.ProtoClass protoClass, final String output) {
		String clazz;
		if (protoClass.isEnum) {
			clazz = templateEnum;
		} else if (protoClass.isConstant) {
			clazz = templateConst;
		} else if (protoClass.isAbstract) {
			clazz = templateClass;
			clazz = clazz.replace("final class", "abstract class");
		} else {
			clazz = templateClass;
		}
		clazz = clazz.replace("__time__", protoClass.protoPackage.getTime());
		clazz = clazz.replace("__package__", protoClass.protoPackage.name);
		clazz = clazz.replace("__desc__", protoClass.desc != null ? protoClass.desc : "");
		clazz = clazz.replaceAll("__class__", protoClass.name);
		clazz = clazz.replace("__imports__", getImports(protoClass));
		clazz = clazz.replace("__fields__", getFields(protoClass));
		clazz = clazz.replace("__encodes__", getEncodes(protoClass));
		clazz = clazz.replace("__decodes__", getDecodes(protoClass));

		String path = output + protoClass.name + ".java";
		fileUtils.save(path, clazz);
		logger.info(path);
	}

	private String getImports(final Proto.ProtoClass protoClass) {
		if (protoClass.imports == null) {
			return "";
		}
		ClassBuilder builder = new ClassBuilder();
		for (Proto.ProtoClass importClass : protoClass.imports) {
			if (importClass != null) {
				builder.append("import ").append(importClass.fullName).append(";").appendLine();
			}
		}
		builder.deleteLine();
		return builder.toString();
	}

	private String getFields(final Proto.ProtoClass protoClass) {
		ClassBuilder builder = new ClassBuilder(1);
		if (protoClass.isEnum) {
			for (Proto.ProtoField field : protoClass.fields) {
				builder.append(field.name).append("((short) ").append(field.code).append(", \"").append(field.name).append("\"),");
				if (field.desc != null) {
					builder.append(" //").appendLine(field.desc);
				} else {
					builder.appendLine();
				}
			}
		} else if (protoClass.isConstant) {
			for (Proto.ProtoField field : protoClass.fields) {
				builder.append("public static final ").append(field.type).append(" ").append(field.name).append(" = ").append(field.code).append(";");
				if (field.desc != null) {
					builder.append(" //").appendLine(field.desc);
				} else {
					builder.appendLine();
				}
			}
		} else {
			for (Proto.ProtoField field : protoClass.fields) {
				builder.append("public ").append(field.type).append(" ").append(field.name).append(";");
				if (field.desc != null) {
					builder.append(" //").appendLine(field.desc);
				} else {
					builder.appendLine();
				}
			}
		}
		builder.deleteLine();
		return builder.toString();
	}

	private String getEncodes(final Proto.ProtoClass protoClass) {
		if (protoClass.isEnum || protoClass.isConstant || protoClass.isAbstract) {
			return "";
		} else {
			ClassBuilder builder = new ClassBuilder(2);
			for (Proto.ProtoField field : protoClass.fields) {
				builder.append("if (this.").append(field.name).append(" != ").append(field.getDefaultValue()).append(") ").startWith("{");
				builder.append("res.writeVar(").append(field.code).appendLine(");");
				builder.append("res.writeVar(").append(field.t1).appendLine(");");
				if (field.t2 > 0) builder.append("res.writeVar(").append(field.t2).appendLine(");");
				if (field.t3 > 0) builder.append("res.writeVar(").append(field.t3).appendLine(");");
				builder.append("res.write").append(field.getFunction()).append("(this.").append(field.name).appendLine(");");
				builder.endWith("}");
			}
			builder.deleteLine();
			return builder.toString();
		}
	}

	private String getDecodes(final Proto.ProtoClass protoClass) {
		if (protoClass.isEnum || protoClass.isConstant || protoClass.isAbstract) {
			return "";
		} else {
			ClassBuilder builder = new ClassBuilder(4);
			for (Proto.ProtoField field : protoClass.fields) {
				builder.append("case ").append(field.code).append(": ");
				builder.append("if (req.readSkip(").append(field.t1);
				if (field.t2 > 0) builder.append(", ").append(field.t2);
				if (field.t3 > 0) builder.append(", ").append(field.t3);
				builder.append(")) ");
				builder.append("this.").append(field.name).append(" = ").append("req.read").append(field.getFunction());
				builder.append("(");
				if (field.t1 == PacketType.OBJECT) {
					builder.append("new ").append(field.type).append("()");
				} else if (field.t1 == PacketType.LIST || field.t1 == PacketType.ARRAY) {
					builder.append(field.type2).append(".class");
				} else if (field.t1 == PacketType.MAP) {
					builder.append(field.type2).append(".class, ");
					builder.append(field.type3).append(".class");
				}
				builder.appendLine("); break;");
			}
			builder.deleteLine();
			return builder.toString();
		}
	}

	/**
	 * 生成模块常量
	 * @param output
	 */
	private void createPacketMod(final String output) {
		ClassBuilder builder = new ClassBuilder(1);
		for (Proto.ProtoPackage protoPackage : proto.packages) {
			builder.append("public final static short ").append(protoPackage.getSimpleName()).append(" = ").append(protoPackage.mod).appendLine(";");
		}
		builder.deleteLine();
		saveConstFile(output, "PacketMod", builder.toString());
	}

	private void createPacketMethod(final String output) {
		ClassBuilder builder = new ClassBuilder(1);
		for (Proto.ProtoPackage protoPackage : proto.packages) {
			builder.append("//").append(protoPackage.getSimpleName()).appendLine();
			for (Proto.ProtoAction protoAction : protoPackage.actions) {
				builder.append("public final static short ").append(protoAction.getConstName()).append(" = ").append(protoAction.action).append(";");
				if (protoAction.desc != null) {
					builder.append(" //").appendLine(protoAction.desc);
				} else {
					builder.appendLine();
				}
			}
		}
		builder.deleteLine();
		saveConstFile(output, "PacketMethod", builder.toString());
	}

	private void saveConstFile(final String output, final String className, final String fields) {
		String clazz = templateConst;
		clazz = clazz.replace("__package__", proto.getSimpleName());
		clazz = clazz.replace("__desc__", "");
		clazz = clazz.replace("__class__", className);
		clazz = clazz.replace("__fields__", fields);
		clazz = clazz.replace("__time__", proto.getTime());

		String dir = output + proto.getSimpleName() + "/";
		String path = dir + className + ".java";
		fileUtils.mkdirs(dir);
		fileUtils.save(path, clazz);
		logger.info(path);
	}
}
