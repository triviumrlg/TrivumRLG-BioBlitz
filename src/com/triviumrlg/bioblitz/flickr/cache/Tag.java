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

public class Tag implements Serializable {
	private static final long serialVersionUID = 1280955016765136217L;
	private String id;
	private String author;
	private String authorName;
	private String raw;
	private String value;

	public Tag(String id, String author, String authorName, String raw,
			String value) {
		this.id = id;
		this.author = author;
		this.authorName = authorName;
		this.raw = raw;
		this.value = value;
	}

	public String getId() {
		return id;
	}

	public String getAuthor() {
		return author;
	}

	public String getAuthorName() {
		return authorName;
	}

	public String getRaw() {
		return raw;
	}

	public String getValue() {
		return value;
	}
}
