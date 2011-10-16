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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.openjena.atlas.json.JsonValue;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class RDFHelper {
	private Model model;
	private static DatatypeFactory dataTypeFactory;

	public RDFHelper(Model model) {
		this.model = model;
	}

	static {
		try {
			dataTypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("DatatypeConfigurationException", e);
		}
	}

	public String encodeURIPart(String str) {
		try {
			return (new URI("http", str, null)).getRawSchemeSpecificPart();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("URISyntaxExemption", e);
		}
	}

	public void addDate(Resource subject, String property, Date val) {
		if (val == null)
			return;

		Calendar cal = Calendar.getInstance();
		cal.setTime(val);

		subject.addLiteral(model.createProperty(property), cal);
	}

	public void addDate(Resource subject, String property, Object val) {
		if (val == null)
			return;

		Calendar cal = dataTypeFactory.newXMLGregorianCalendar(val.toString())
				.toGregorianCalendar();

		subject.addLiteral(model.createProperty(property), cal);
	}

	public void addLiteral(Resource subject, String property, JsonValue val) {
		if (val == null)
			return;

		if (val.isBoolean())
			subject.addLiteral(model.createProperty(property), val
					.getAsBoolean().value());
		else if (val.isNumber())
			subject.addLiteral(model.createProperty(property), val.getNumber()
					.value());
		else if (val.isString()) {
			String str = val.getAsString().value();
			if (str.startsWith("http:"))
				addResource(subject, property, str);
			else
				subject.addLiteral(model.createProperty(property), str);
		}
	}

	public void addLiteral(Resource subject, String property, Boolean val) {
		if (val == null)
			return;

		subject.addLiteral(model.createProperty(property), val.booleanValue());
	}

	public void addLiteral(Resource subject, String property, Long val) {
		if (val == null)
			return;

		subject.addLiteral(model.createProperty(property), val.longValue());
	}

	public void addLiteral(Resource subject, String property, Integer val) {
		if (val == null)
			return;

		subject.addLiteral(model.createProperty(property), val.intValue());
	}

	public void addLiteral(Resource subject, String property, Float val) {
		if (val == null)
			return;

		subject.addLiteral(model.createProperty(property), val.floatValue());
	}

	public void addLiteral(Resource subject, String property, Double val) {
		if (val == null)
			return;

		subject.addLiteral(model.createProperty(property), val.doubleValue());
	}

	public void addLiteral(Resource subject, String property, String val) {
		if (val == null)
			return;

		subject.addLiteral(model.createProperty(property), val);
	}

	public void addResource(Resource subject, String property, String val) {
		if (val != null && !val.isEmpty())
			addResource(subject, property, model.createResource(val));
	}

	public void addResource(Resource subject, String property, Resource val) {
		subject.addProperty(model.createProperty(property), val);
	}

	public String getString(Resource resource, Property property) {
		return getString(resource, property, null);
	}

	public String getString(Resource resource, Property property,
			String defaultValue) {
		Statement first = resource == null ? null : resource
				.getProperty(property);
		RDFNode object = first == null ? null : first.getObject();

		if (object == null || object.isAnon())
			return defaultValue;
		else if (object.isURIResource())
			return object.asResource().getURI();
		else if (object.isLiteral())
			return object.asLiteral().getString();
		else
			return defaultValue;
	}

	public boolean getBoolean(Resource resource, Property property) {
		Statement first = resource == null ? null : resource
				.getProperty(property);
		RDFNode object = first == null ? null : first.getObject();

		if (object == null || !object.isLiteral())
			return false;

		return object.asLiteral().getLexicalForm().equals("true");
	}
}
