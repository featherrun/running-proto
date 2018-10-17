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

import running.core.ClassBuilder;
import running.core.ILogger;
import running.core.Running;
import running.proto.PacketType;
import running.util.FileUtils;

public class Proto2TypeScript {
	final ILogger logger = Running.getLogger(Proto.class);
	final FileUtils fileUtils = Running.get(FileUtils.class);

	final String separator = System.getProperty("line.separator");
	final String template;
	final String templateNS;
	final String templateClass;
	final String templateConst;
	final String templateEnum;
	final Proto proto;

	public Proto2TypeScript(final Proto proto, final String output) {
		String temp = fileUtils.getResource("template/typescript.proto.template");
		int jump = 1 + separator.length();
		template = temp.substring(temp.indexOf("#P"+separator)+jump+1, temp.indexOf("P#"));
		templateNS = temp.substring(temp.indexOf("#N"+separator)+jump+1, temp.indexOf("N#"));
		templateClass = temp.substring(temp.indexOf("#CLASS"+separator)+jump+5, temp.indexOf("CLASS#"));
		templateConst = temp.substring(temp.indexOf("#CONST"+separator)+jump+5, temp.indexOf("CONST#"));
		templateEnum = temp.substring(temp.indexOf("#ENUM"+separator)+jump+4, temp.indexOf("ENUM#"));
		this.proto = proto;

		String firstName = proto.getSimpleName();
		String text = template;
		text = text.replace("__package__", firstName);
		text = text.replace("__time__", proto.getTime());
		text = text.replace("__ns_list__", getPackageList());
		text = text.replace("__PacketMod__", getPacketMod());
		text = text.replace("__PacketMethod__", getPacketMethod());
		text = text.replace("__actions__", getActions());

		String path = output + firstName + ".ts";
		fileUtils.mkdirs(output);
		fileUtils.save(path, text);
		logger.info(path);
	}

	private String getPackageList() {
		StringBuilder nsList = new StringBuilder();
		for (Proto.ProtoPackage protoPackage : proto.packages) {
			nsList.append(getPackage(protoPackage));
			nsList.append(separator);
		}
		return nsList.toString();
	}

	private String getPackage(final Proto.ProtoPackage protoPackage) {
		StringBuilder classes = new StringBuilder();
		for (Proto.ProtoClass protoClass : protoPackage.classes) {
			classes.append(getClasses(protoClass));
		}
		String text = templateNS;
		text = text.replace("__ns__", protoPackage.name.substring(protoPackage.name.indexOf('.')+1));
		text = text.replace("__classes__", classes.toString());
		return text;
	}

	private String getClasses(final Proto.ProtoClass protoClass) {
		String clazz;
		if (protoClass.isEnum) {
			clazz = templateEnum;
		} else if (protoClass.isConstant) {
			clazz = templateConst;
		} else if (protoClass.isAbstract) {
			clazz = templateClass;
		} else {
			clazz = templateClass;
		}
		clazz = clazz.replace("__class__", protoClass.name);
		clazz = clazz.replace("__desc__", protoClass.desc != null ? protoClass.desc : "");
		clazz = clazz.replace("__ID__", String.valueOf(protoClass.id));
		clazz = clazz.replace("__fields__", getFields(protoClass));
		return clazz;
	}

	private String getFields(final Proto.ProtoClass protoClass) {
		ClassBuilder builder = new ClassBuilder(3);
		if (protoClass.isEnum) {
			for (Proto.ProtoField field : protoClass.fields) {
				builder.append(field.name).append("=").append(field.code).append(",");
				if (field.desc != null) {
					builder.append(" //").appendLine(field.desc);
				} else {
					builder.appendLine();
				}
			}
		} else if (protoClass.isConstant) {
			for (Proto.ProtoField field : protoClass.fields) {
				builder.append(field.name).append(":").append(field.code).append(",");
				if (field.desc != null) {
					builder.append(" //").appendLine(field.desc);
				} else {
					builder.appendLine();
				}
			}
		} else {
			for (Proto.ProtoField field : protoClass.fields) {
				builder.append(field.name).append(":").append(getType(field)).append(";");
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

	private String getType(final Proto.ProtoField field) {
		switch (field.t1) {
			case PacketType.OBJECT:
				return getType(field.t1, field.type);
			case PacketType.ARRAY:
			case PacketType.LIST:
				return getType(field.t2, field.type2)+"[]";
			case PacketType.MAP:
				return "any";
			default:
				return getSimpleType(field.t1);
		}
	}

	private String getType(final int t, final String tName) {
		switch (t) {
			case PacketType.OBJECT:
				Proto.ProtoClass protoClass = proto.findClass(tName);
				return protoClass != null ? protoClass.fullName : tName;
			default:
				return getSimpleType(t);
		}
	}

	private String getSimpleType(final int type) {
		switch (type) {
			case PacketType.BOOLEAN:
				return "boolean";
			case PacketType.BYTE:
			case PacketType.SHORT:
			case PacketType.INT:
			case PacketType.LONG:
			case PacketType.VAR:
				return "number";
			case PacketType.STRING:
				return "string";
		}
		return "?";
	}

	private String getPacketMod() {
		ClassBuilder builder = new ClassBuilder(2);
		for (Proto.ProtoPackage protoPackage : proto.packages) {
			builder.append(protoPackage.getSimpleName()).append(":").append(protoPackage.mod).appendLine(",");
		}
		builder.deleteLine();
		return builder.toString();
	}

	private String getPacketMethod() {
		ClassBuilder builder = new ClassBuilder(2);
		for (Proto.ProtoPackage protoPackage : proto.packages) {
			builder.append("//").append(protoPackage.getSimpleName()).appendLine();
			for (Proto.ProtoAction protoAction : protoPackage.actions) {
				if (!protoAction.isClient()) {
					continue;
				}
				builder.append(protoAction.getConstName()).append(": ").append(protoAction.action).append(",");
				if (protoAction.desc != null) {
					builder.append(" //").appendLine(protoAction.desc);
				} else {
					builder.appendLine();
				}
			}
		}
		builder.deleteLine();
		return builder.toString();
	}

	private String getActions() {
		ClassBuilder builder = new ClassBuilder(2);
		for (Proto.ProtoPackage protoPackage : proto.packages) {
			builder.append("//").appendLine(protoPackage.name);
			for (Proto.ProtoAction action : protoPackage.actions) {
				if (!action.isClient()) {
					continue;
				}
				int val = (action.protoPackage.mod << 16) + action.action;
				builder.appendLine(String.format("%s_%s: %d, //%d-%d %s",
						action.type,
						action.getSimpleName(),
						val,
						action.protoPackage.mod,
						action.action,
						(action.desc != null ? action.desc : "")
				));
			}
		}
		builder.deleteLine();
		return builder.toString();
	}
}
