/*
 * Copyright (c) <2011> <joe@triviumrlg.com>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.triviumrlg.bioblitz.utils;

import java.util.LinkedList;
import java.util.List;

import org.openjena.atlas.json.JsonArray;
import org.openjena.atlas.json.JsonNull;
import org.openjena.atlas.json.JsonObject;
import org.openjena.atlas.json.JsonValue;

public class JSONObject {
	JsonObject obj;

	public JSONObject() {
		this.obj = new JsonObject();
	}

	public JSONObject(JsonObject obj) {
		this.obj = obj;
	}

	public JsonArray getArray(String key) {
		JsonValue val = get(key);

		if (val == null || !val.isArray())
			return new JsonArray();

		return val.getAsArray();
	}

	public List<String> getStringList(String key) {
		List<String> list = new LinkedList<String>();

		for (JsonValue val : getArray(key)) {
			list.add(val.isString() ? val.getAsString().value() : val
					.toString());
		}

		return list;
	}

	public JSONObject getObject(String key) {
		JsonValue val = get(key);

		if (val == null || !val.isObject())
			return new JSONObject(null);

		return new JSONObject(val.getAsObject());
	}

	public String getString(String key) {
		JsonValue val = get(key);

		if (val == null)
			return null;

		return val.isString() ? val.getAsString().value() : val.toString();
	}

	public Integer getInteger(String key) {
		JsonValue val = get(key);

		if (val == null)
			return null;

		return val.isNumber() ? val.getNumber().value().intValue() : null;
	}

	public JsonValue get(String key) {
		if (obj == null)
			return null;

		JsonValue val = obj.get(key);

		if (val instanceof JsonNull)
			return null;

		return val;
	}

	public boolean hasKey(String key) {
		return obj != null && obj.hasKey(key);
	}

	@Override
	public String toString() {
		return obj == null ? "" : obj.toString();
	}
}
