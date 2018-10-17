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
import running.util.TimeUtils;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Proto {
	final static Pattern reg_package = Pattern.compile("package\\s+([a-z0-9_\\.]+)", Pattern.CASE_INSENSITIVE);
	final static Pattern reg_abstract = Pattern.compile("abstract\\s+([a-z0-9_]+)([^\\{]*)\\{([^\\n]*)([^\\}]+)\\}", Pattern.CASE_INSENSITIVE);
	final static Pattern reg_class = Pattern.compile("class\\s+([a-z0-9_]+)([^\\{]*)\\{([^\\n]*)([^\\}]+)\\}", Pattern.CASE_INSENSITIVE);
	final static Pattern reg_class_field = Pattern.compile("([a-z0-9_<>,\\[\\]]+)\\s+([a-z0-9_]+)\\s?=\\s?([0-9]+);(.*)", Pattern.CASE_INSENSITIVE);
	final static Pattern reg_enum = Pattern.compile("enum\\s+([a-z0-9_]+)([^\\{]*)\\{([^\\n]*)([^\\}]+)\\}", Pattern.CASE_INSENSITIVE);
	final static Pattern reg_enum_field = Pattern.compile("(\\s?)([a-z0-9_]+)\\s?=\\s?([0-9]+);(.*)", Pattern.CASE_INSENSITIVE);
	final static Pattern reg_constant = Pattern.compile("const\\s+([a-z0-9_]+)([^\\{]*)\\{([^\\n]*)([^\\}]+)\\}", Pattern.CASE_INSENSITIVE);
	final static Pattern reg_constant_field = Pattern.compile("([a-z0-9_<>,\\[\\]]+)\\s+([a-z0-9_]+)\\s?=\\s?([0-9]+);(.*)", Pattern.CASE_INSENSITIVE);
	final static Pattern reg_mod = Pattern.compile("@mod\\s+([0-9]+)", Pattern.CASE_INSENSITIVE);
	final static Pattern reg_action = Pattern.compile("@([a-z]+)\\s+([a-z0-9_]*)\\s?\\(([a-z0-9_]*)\\)\\s?=\\s?([0-9]+);(.*)", Pattern.CASE_INSENSITIVE);

	static ILogger logger = Running.getLogger(Proto.class);
	static FileUtils fileUtils = Running.get(FileUtils.class);
	static TimeUtils timeUtils = Running.get(TimeUtils.class);

	public final String path;
	public long lastModified;
	public final List<ProtoPackage> packages = new LinkedList<>();
	public final Map<String, ProtoClass> classMap = new LinkedHashMap<>();

	public Proto(String path) {
		this.path = path;
		File inputDir = new File(path);
		logger.debug(inputDir.getAbsolutePath());
		if (!inputDir.isDirectory()) {
			return;
		}
		File[] inputFiles = inputDir.listFiles();
		if (inputFiles == null) {
			return;
		}
		for (File file : inputFiles) {
			lastModified = Math.max(lastModified, file.lastModified());
			ProtoPackage protoPackage = new ProtoPackage(this, file);
			packages.add(protoPackage);
			for (ProtoClass clazz : protoPackage.classes) {
				classMap.put(clazz.name, clazz);
			}
		}

		//引用调查
		for (ProtoPackage protoPackage : packages) {
			for (ProtoClass clazz : protoPackage.classes) {
				clazz.parseImports();
			}
		}
	}

	public String getSimpleName() {
		String firstName = null;
		if (!packages.isEmpty()) {
			Proto.ProtoPackage first = packages.get(0);
			firstName = first.name.substring(0, first.name.indexOf('.'));
		}
		if (firstName == null || firstName.isEmpty()) {
			firstName = "proto";
		}
		return firstName;
	}

	public String getTime() {
		return timeUtils.dateToString(new Date(lastModified));
	}

	/**
	 * 查找所有类
	 * @param name
	 * @return
	 */
	public ProtoClass findClass(String name) {
		return classMap.get(name);
	}

	/**
	 * 协议包
	 */
	public static class ProtoPackage {
		public final Proto proto;
		public final File file;
		public String filename;
		public String content;
		public String name;
		public short mod = -1;
		public List<ProtoAction> actions = new LinkedList<>();
		public List<ProtoClass> classes = new LinkedList<>();

		public ProtoPackage(Proto proto, File file) {
			this.proto = proto;
			this.file = file;
			parse();
		}

		public String getTime() {
			return timeUtils.dateToString(new Date(file.lastModified()));
		}

		public String getSimpleName() {
			return name.substring(name.indexOf('.')+1);
		}

		/**
		 * 查找当前包内的类
		 * @param name
		 * @return
		 */
		public ProtoClass findClass(String name) {
			for (ProtoClass clazz : classes) {
				if (clazz.name.equals(name)) {
					return clazz;
				}
			}
			return null;
		}

		protected void parse() {
			filename = file.getName();
			filename = filename.substring(0, filename.indexOf("."));
			content = fileUtils.read(file.getPath());
			parseName();
			parseMod();
			parseActions();
			parseClasses(reg_abstract, reg_class_field, false, false, true);
			parseClasses(reg_class, reg_class_field, false, false, false);
			parseClasses(reg_enum, reg_enum_field, true, false, false);
			parseClasses(reg_constant, reg_constant_field, false, true, false);
		}

		protected void parseName() {
			Matcher mat_package = reg_package.matcher(content);
			if (mat_package.find()) {
				name = mat_package.group(1).toLowerCase();
			} else {
				logger.warn("Not found package:" + filename);
			}
		}

		protected void parseMod() {
			Matcher mat_mod = reg_mod.matcher(content);
			if (mat_mod.find()) {
				mod = Short.parseShort(mat_mod.group(1));
			} else {
				logger.warn("Not found mod:" + filename);
			}
		}

		protected void parseActions() {
			Matcher mat_action = reg_action.matcher(content);
			while (mat_action.find()) {
				ProtoAction action = new ProtoAction();
				action.protoPackage = this;
				actions.add(action);
				action.type = mat_action.group(1);
				action.name = mat_action.group(2);
				action.parameter = mat_action.group(3);
				action.action = Short.parseShort(mat_action.group(4));
				String desc = mat_action.group(5).trim();
				if (desc.contains("//")) {
					action.desc = desc.substring(desc.indexOf("//")+2);
				}
			}
		}

		protected void parseClasses(Pattern regClass, Pattern regField, boolean isEnum, boolean isConstant, boolean isAbstract) {
			int ID = mod*100;
			if (ID <= 0) {
				ID += Short.MAX_VALUE;
			}
			Matcher mat_class = regClass.matcher(content);
			while (mat_class.find()) {
				ID++;
				ProtoClass clazz = new ProtoClass();
				clazz.protoPackage = this;
				classes.add(clazz);
				clazz.id = ID;
				clazz.isEnum = isEnum;
				clazz.isConstant = isConstant;
				clazz.isAbstract = isAbstract;
				clazz.name = mat_class.group(1);
				clazz.fullName = name + "." + clazz.name;
				String desc = (mat_class.group(2) + mat_class.group(3)).trim();
				if (desc.contains("//")) {
					clazz.desc = desc.substring(desc.indexOf("//")+2);
				}
				String[] fields = mat_class.group(4).trim().split("\n");
				for (String f : fields) {
					Matcher mat_field = regField.matcher(f);
					if (mat_field.find()) {
						ProtoField field = new ProtoField();
						field.type = isEnum ? "enum" : mat_field.group(1);
						field.name = mat_field.group(2);
						field.code = Integer.parseInt(mat_field.group(3));
						String field_desc = mat_field.group(4).trim();
						if (field_desc.contains("//")) {
							field.desc = field_desc.substring(field_desc.indexOf("//")+2);
						}
						field.parseTypes();
						clazz.fields.add(field);
					}
				}
			}
		}
	}

	/**
	 * 协议消息规则
	 */
	public static class ProtoAction {
		public ProtoPackage protoPackage;
		public String type; //cmd,msg,rpc,job
		public String name;
		public String parameter;
		public short action;
		public String desc;

		public String getSimpleName() {
			if (name != null && !name.isEmpty()) {
				return name;
			} else {
				String pre2 = parameter.substring(0, 2);
				if (pre2.toUpperCase().equals(pre2)) {
					return parameter.substring(1); //去除单字母前缀
				} else {
					return parameter;
				}
			}
		}

		public String getConstName() {
			String sb = type + "_" +
					protoPackage.getSimpleName() + "_" +
					getSimpleName();
			return sb.toUpperCase();
		}

		public boolean isClient() {
			return type != null && (type.equals("cmd") || type.equals("msg"));
		}
	}

	/**
	 * 协议对象描述
	 */
	public static class ProtoClass implements Comparable<ProtoClass> {
		public ProtoPackage protoPackage;
		public int id;
		public boolean isEnum;
		public boolean isConstant;
		public boolean isAbstract;
		public String name;
		public String fullName;
		public String desc;
		public List<ProtoField> fields = new LinkedList<>();
		public List<ProtoClass> imports = new LinkedList<>();

		public void parseImports() {
			Set<ProtoClass> imports = new HashSet<>();
			for (ProtoField field : fields) {
				if (field.t1 == PacketType.OBJECT && protoPackage.findClass(field.type) == null) {
					imports.add(protoPackage.proto.findClass(field.type));
				}
				if (field.t2 == PacketType.OBJECT && protoPackage.findClass(field.type2) == null) {
					imports.add(protoPackage.proto.findClass(field.type2));
				}
				if (field.t3 == PacketType.OBJECT && protoPackage.findClass(field.type3) == null) {
					imports.add(protoPackage.proto.findClass(field.type3));
				}
			}
			if (!imports.isEmpty()) {
				this.imports.addAll(imports);
				Collections.sort(this.imports);
			}
		}

		@Override
		public int compareTo(ProtoClass o) {
			return fullName.compareTo(o.fullName);
		}
	}

	public static class ProtoField {
		public String type;
		public String name;
		public String desc;
		public int code;

		public String type2;
		public String type3;
		public int t1;
		public int t2;
		public int t3;

		public void parseTypes() {
			if (type.contains("[]")) {
				type2 = type.substring(0, type.indexOf("[")).trim();
				t1 = PacketType.ARRAY;
				t2 = getPacketType(type2);
			} else if (type.contains("List<") || type.contains("List ")) {
				type2 = type.substring(type.indexOf("<") + 1, type.indexOf(">")).trim();
				t1 = PacketType.LIST;
				t2 = getPacketType(type2);
			} else if (type.contains("Map<") || type.contains("Map ")) {
				type2 = type.substring(type.indexOf("<") + 1, type.indexOf(",")).trim();
				type3 = type.substring(type.indexOf(",") + 1, type.indexOf(">")).trim();
				t1 = PacketType.MAP;
				t2 = getPacketType(type2);
				t3 = getPacketType(type3);
			} else {
				t1 = getPacketType(type);
			}
		}

		public int getPacketType(String type) {
			type = type.trim().toLowerCase();
			switch (type) {
				case "boolean":
					return PacketType.BOOLEAN;
				case "byte":
					return PacketType.BYTE;
				case "short":
					return PacketType.SHORT;
				case "int":
				case "integer":
				case "enum":
					return PacketType.INT;
				case "long":
					return PacketType.LONG;
				case "var":
					return PacketType.VAR;
				case "string":
					return PacketType.STRING;
				default:
					return PacketType.OBJECT;
			}
		}

		public String getDefaultValue() {
			return getDefaultValue(t1);
		}

		public String getDefaultValue(int t) {
			if (t == PacketType.BOOLEAN) {
				return "false";
			} else if (t == PacketType.BYTE || t == PacketType.SHORT || t == PacketType.INT || t == PacketType.LONG) {
				return "0";
			} else {
				return "null";
			}
		}

		public String getFunction() {
			switch (t1) {
				case PacketType.BOOLEAN: return "Boolean";
				case PacketType.BYTE: return "Byte";
				case PacketType.SHORT: return "Short";
				case PacketType.INT: return "Int";
				case PacketType.LONG: return "Long";
				case PacketType.STRING: return "String";
				case PacketType.ARRAY: return "Array";
				case PacketType.LIST: return "List";
				case PacketType.MAP: return "Map";
				default: return "Object";
			}
		}
	}

}
