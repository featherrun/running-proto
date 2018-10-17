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

import running.core.ILogger;
import running.core.PrintLogger;
import running.core.Running;
import running.proto.builder.*;
import running.util.*;

public class Main {
	static {
		Running.set(ILogger.class, new PrintLogger());
		Running.set(FileUtils.class, new FileUtils());
		Running.set(StringUtils.class, new StringUtils());
		Running.set(TimeUtils.class, new TimeUtils());
		Running.set(PropertiesUtils.class, new PropertiesUtils());
		Running.set(JsonUtils.class, new JsonUtils());
	}

	public static void main(String[] args) {
		final ILogger logger = Running.getLogger(Main.class);
		final PropertiesUtils propertiesUtils = Running.get(PropertiesUtils.class);

		propertiesUtils.load(args);
		String path = propertiesUtils.getProperty("input", "./proto/");
		String output = propertiesUtils.getProperty("output", "./output/");
		String type = propertiesUtils.getProperty("type", "Java").toLowerCase();
		String jsonOutput = propertiesUtils.getProperty("json-output");
		String javaActionOutput = propertiesUtils.getProperty("java-action-output");

		Proto proto = new Proto(path);
		switch (type) {
			case "java":
				new Proto2Java(proto, output);
				break;
			case "typescript":
			case "ts":
				new Proto2TypeScript(proto, output);
				break;
			case "json":
				new Proto2Json(proto, output);
				break;
			default:
				logger.warn("unknown type.");
				break;
		}
		if (jsonOutput != null && !jsonOutput.isEmpty()) {
			new Proto2Json(proto, jsonOutput);
		}
		if (javaActionOutput != null && !javaActionOutput.isEmpty()) {
			new Proto2JavaAction(proto, javaActionOutput);
		}
	}
}
