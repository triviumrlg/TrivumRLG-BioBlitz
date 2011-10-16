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

package com.triviumrlg.bioblitz.eol;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Species implements Serializable {
	private static final long serialVersionUID = 9203684743125823349L;
	private String eol_page_id;
	private String name;
	private Map<String, String> taxa;

	public Species(String name) {
		this.name = name;
		taxa = new HashMap<String, String>();
		name = null;
	}

	public void setEolPageId(String eol_page_id) {
		this.eol_page_id = eol_page_id;
	}

	public void addTaxon(String name, String value) {
		taxa.put(name, value);
	}

	public Map<String, String> getTaxa() {
		return taxa;
	}

	public String getEOLPageId() {
		return eol_page_id;
	}
	
	public String getName() {
		return name;
	}	
}
