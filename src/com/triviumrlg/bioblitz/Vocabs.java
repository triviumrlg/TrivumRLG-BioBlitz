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

package com.triviumrlg.bioblitz;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class Vocabs {
	private static Map<String, String> map;

	static {
		map = new HashMap<String, String>();
		map.put("photos", "http://triviumrlg.net/bioblitz/photos#");
		map.put("dct", "http://purl.org/dc/terms/");
		map.put("dwc", "http://rs.tdwg.org/dwc/terms/");
		map.put("flickr", "http://triviumrlg.net/bioblitz/flickr#");
		map.put("foaf", "http://xmlns.com/foaf/0.1/");
		map.put("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
		map.put("noah", "http://triviumrlg.net/bioblitz/noah#");
		map.put("owl", "http://www.w3.org/2002/07/owl#");
		map.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		map.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		map.put("xsd", "http://www.w3.org/2001/XMLSchema#");
	}

	public static Map<String, String> getMap() {
		return map;
	}

	public static String SPARQLPrefixes() {
		StringBuffer buf = new StringBuffer();

		for (Entry<String, String> e : map.entrySet())
			buf.append(String.format("PREFIX %s: <%s>\n", e.getKey(),
					e.getValue()));

		return buf.toString();
	}

	public static String uri(String prefix) {
		return map.get(prefix);
	}

	public static String uri(String prefix, String local) {
		return map.get(prefix) + local;
	}

	public static Property prop(String prefix) {
		return ResourceFactory.createProperty(uri(prefix));
	}

	public static Property prop(String prefix, String local) {
		return ResourceFactory.createProperty(uri(prefix, local));
	}

	public static String httpURL(String prefix) {
		return map.get(prefix).replaceFirst("#$", "");
	}
}
