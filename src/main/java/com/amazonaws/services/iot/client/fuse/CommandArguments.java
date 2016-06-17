/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.services.iot.client.fuse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommandArguments {

	private final Map<String, Set<String>> arguments = new HashMap<>();

	private CommandArguments(String[] args) {
		String name = null;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			if (name == null) {
				if (arg.startsWith("-")) {
					name = arg.replaceFirst("^-+", "");
					if (name.length() < 1) {
						name = null;
					}
				}
				continue;
			}

			if (arg.startsWith("-")) {
				putArg(arguments, name, null);

				name = arg.replaceFirst("^-+", "");
				if (name.length() < 1) {
					name = null;
				}
			} else {
				putArg(arguments, name, arg);
				name = null;
			}
		}

		if (name != null) {
			putArg(arguments, name, null);
		}
	}

	private static void putArg(Map<String, Set<String>> arguments, String name, String value) {
		Set<String> values = arguments.get(name.toLowerCase());
		if (values == null) {
			values = new HashSet<String>();
			arguments.put(name.toLowerCase(), values);
		}
		values.add(value);
	}

	public static CommandArguments parse(String[] args) {
		return new CommandArguments(args);
	}

	public String get(String name) {
		Set<String> values = arguments.get(name.toLowerCase());
		if (values == null) {
			return null;
		}

		for (String value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	public Set<String> getAll(String name) {
		return arguments.get(name.toLowerCase());
	}

	public String get(String name, String defaultValue) {
		String value = get(name.toLowerCase());
		if (value == null) {
			value = defaultValue;
		}
		return value;
	}

	public String getNotNull(String name) {
		String value = get(name);
		if (value == null) {
			throw new RuntimeException("Missing required argumment for " + name);
		}
		return value;
	}

	public String getNotNull(String name, String defaultValue) {
		String value = get(name, defaultValue);
		if (value == null) {
			throw new RuntimeException("Missing required argumment for " + name);
		}
		return value;
	}

}
