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
import running.util.FileUtils;
import running.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class Proto2JavaAction {
	final ILogger logger = Running.getLogger(Proto.class);
	final FileUtils fileUtils = Running.get(FileUtils.class);
	final StringUtils stringUtils = Running.get(StringUtils.class);

	final String packageName = "action";
	final String templateRunnable;
	final String template;
	final Proto proto;

	private Map<Proto.ProtoAction, String> runnableMap;

	public Proto2JavaAction(final Proto proto, final String output) {
		templateRunnable = fileUtils.getResource("template/java.action.runnable.template");
		template = fileUtils.getResource("template/java.action.template");
		this.proto = proto;

		fileUtils.mkdirs(output);
		runnableMap = new LinkedHashMap<>();
		for (Proto.ProtoPackage protoPackage : proto.packages) {
			createPackage(protoPackage, output);
		}

		String clazz = template;
		String className = stringUtils.firstUpperCase(packageName);
		clazz = clazz.replace("__package__", packageName);
		clazz = clazz.replace("__class__", className);
		clazz = clazz.replace("__fields__", getFields());
		String path = output + packageName + "/" + className + ".java";
		fileUtils.save(path, clazz);
		logger.info(path);
	}

	private void createPackage(final Proto.ProtoPackage protoPackage, final String output) {
		String name = packageName + "." + protoPackage.getSimpleName();
		String path = output + name.replace('.', '/') + '/';
		fileUtils.mkdirs(path);
		for (Proto.ProtoAction protoAction : protoPackage.actions) {
			createActionRunnable(name, protoAction, path);
		}
	}

	private void createActionRunnable(final String packageName, final Proto.ProtoAction action, final String output) {
		if (action.type.toLowerCase().equals("msg")) {
			return;
		}

		String className = stringUtils.firstUpperCase(action.type) + stringUtils.firstUpperCase(action.getSimpleName());
		runnableMap.put(action, packageName + "." + className);

		String path = output + className + ".java";
		if (Files.exists(Paths.get(path))) { //仅生成一次
			return;
		}

		String clazz = templateRunnable;
		String parameterClass = stringUtils.isNotEmpty(action.parameter) ? action.protoPackage.findClass(action.parameter).fullName : "";
		clazz = clazz.replace("__package__", packageName);
		clazz = clazz.replace("__desc__", action.desc != null ? action.desc : "");
		clazz = clazz.replace("__mod__", String.valueOf(action.protoPackage.mod));
		clazz = clazz.replace("__action__", String.valueOf(action.action));
		clazz = clazz.replace("__class__", className);
		clazz = clazz.replace("__parameter__", parameterClass);

		fileUtils.save(path, clazz);
		logger.info(path);
	}

	private String getFields() {
		ClassBuilder builder = new ClassBuilder(1);
		for (Proto.ProtoPackage protoPackage : proto.packages) {
			builder.append("//").appendLine(protoPackage.name);
			for (Proto.ProtoAction action : protoPackage.actions) {
				String cls = runnableMap.get(action);
				int val = (action.protoPackage.mod << 16) + action.action;
				builder.appendLine(String.format("%s_%s(%d, %s), //%d-%d %s",
						action.type,
						action.getSimpleName(),
						val,
						(cls == null ? "null" : "new "+cls+"()"),
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
