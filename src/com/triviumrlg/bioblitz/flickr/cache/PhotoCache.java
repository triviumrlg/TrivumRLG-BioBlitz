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

package com.triviumrlg.bioblitz.flickr.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PhotoCache implements Serializable {
	private static final long serialVersionUID = 7946768189652872793L;

	private Map<String, PhotoData> map;

	public PhotoCache() {
		map = new HashMap<String, PhotoData>();
	}

	public void put(String name, PhotoData item) {
		map.put(name, item);
	}

	public PhotoData get(String name) {
		return map.get(name);
	}

	public boolean hasItem(String name) {
		return map.containsKey(name);
	}

	public PhotoData remove(String name) {
		return map.remove(name);
	}

	public Map<String, PhotoData> getItemMap() {
		return map;
	}
}
