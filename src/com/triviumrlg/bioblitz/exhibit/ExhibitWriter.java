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

package com.triviumrlg.bioblitz.exhibit;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openjena.atlas.json.JsonArray;
import org.openjena.atlas.json.JsonObject;
import org.openjena.atlas.json.JsonValue;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;
import com.triviumrlg.bioblitz.Vocabs;
import com.triviumrlg.bioblitz.utils.RDFHelper;

public class ExhibitWriter {
	OntModel model;
	JsonObject item;
	JsonArray items;
	JsonObject json;
	RDFHelper helper;

	public ExhibitWriter(OntModel model) {
		this.model = model;
		items = new JsonArray();
		json = new JsonObject();
		helper = new RDFHelper(model);
		setupItems();
	}

	private void startItem() {
		item = new JsonObject();
		items.add(item);
	}

	public void write(PrintStream out) throws IOException {
		out.println(json);
	}

	public void setupItems() {
		json.put("items", items);
		OntClass photosClass = model.createClass(Vocabs.uri("photos", "Photo"));
		Iterator<? extends OntResource> itr = photosClass.listInstances();

		while (itr.hasNext()) {
			OntResource photo = itr.next();

			if (helper.getBoolean(photo, Vocabs.prop("photos", "ignore")))
				continue;

			startItem();

			put("type", "Photo");

			put("source", photo.toString().contains("flickr") ? "Flickr"
					: "Noah");

			put("id", photo.toString());
			put("label", photo, RDFS.label, "Unknown Photo");
			putKeywords("keyword", photo, Vocabs.prop("photos", "keyword"));
			put("thumbnail", photo, Vocabs.prop("photos", "hasThumbnail"));
			put("small_thumbnail", photo,
					Vocabs.prop("photos", "hasSmallThumbnail"));
			put("homepage", photo, Vocabs.prop("photos", "hasHomepage"));

			Resource photographer = photo.getPropertyResourceValue(Vocabs.prop(
					"photos", "hasPhotographer"));
			put("photographer", photographer,
					Vocabs.prop("photos", "photographerName"),
					"Unknown Photographer");

			put("created", photo, DCTerms.created);
			put("submitted", photo, DCTerms.issued);

			String glat = getString(photo, Vocabs.prop("geo", "lat"));
			String glong = getString(photo, Vocabs.prop("geo", "long"));
			String lat_long = (glat != null && glong != null) ? String.format(
					"%s,%s", glat, glong) : null;
			put("lat_long", lat_long);
			put("lat", glat);
			put("long", glong);

			put("species", photo, Vocabs.prop("photos", "species"));

			Map<String, String> taxa;
			// taxa = getTaxa(photo,
			// Vocabs.prop("photos", "hasTaxonTag"));
			// put("kingdom", taxa.get("kingdom"));
			// put("phylum", taxa.get("phylum"));
			// put("order", taxa.get("order"));
			// put("class", taxa.get("class"));
			// put("family", taxa.get("family"));
			// put("genus", taxa.get("genus"));

			taxa = getTaxa(photo, Vocabs.prop("photos", "hasEOLTaxonTag"));
			put("kingdom", taxa.get("kingdom"));
			put("phylum", taxa.get("phylum"));
			put("order", taxa.get("order"));
			put("class", taxa.get("class"));
			put("family", taxa.get("family"));
			put("genus", taxa.get("genus"));

			String state;

			if (helper.getString(photo,
					Vocabs.prop("photos", "scientistNamedSpecies")) != null)
				state = "Classified by Expert";
			else if (helper.getString(photo, Vocabs.prop("photos", "species")) == null)
				state = "Needs Classification";
			else
				state = "Classified by User";

			put("state", state);
		}
	}

	public Map<String, String> getTaxa(OntResource resource, Property property) {
		Map<String, String> map = new HashMap<String, String>();
		NodeIterator nodes = resource.listPropertyValues(property);
		try {
			while (nodes.hasNext()) {
				try {
					Resource taxaNode = nodes.next().asResource();

					String name = taxaNode
							.getProperty(Vocabs.prop("dwc", "scientificName"))
							.getLiteral().getString();
					String rank = taxaNode
							.getProperty(Vocabs.prop("dwc", "taxonRank"))
							.getLiteral().getString();

					map.put(rank, name);
				} catch (Exception e) {
					// e.printStackTrace();
				}
			}

			return map;
		} finally {
			nodes.close();
		}
	}

	public void put(String key, long val) {
		item.put(key, val);
	}

	public void put(String key, boolean val) {
		item.put(key, val);
	}

	public void put(String key, JsonValue val) {
		if (val != null)
			item.put(key, val);
	}

	public void put(String key, RDFNode node) {
		if (node == null)
			return;
		else if (node.isLiteral()) {
			Literal lit = node.asLiteral();
			String type = lit.getDatatypeURI();
			if (type == null)
				put(key, lit.getString());
			else if (type.equals(XSD.xstring.toString()))
				put(key, lit.getString());
			else if (type.equals(XSD.xboolean.toString()))
				put(key, lit.getBoolean());
			else if (type.equals(XSD.date.toString())
					|| type.equals(XSD.dateTime.toString()))
				put(key, lit.getString());
			else if (type.equals(XSD.xlong.toString()))
				put(key, lit.getLong());
			else if (type.equals(XSD.xint.toString()))
				put(key, lit.getInt());
			else
				put(key, lit.getValue().toString());
		} else if (node.isURIResource())
			put(key, node.toString());
	}

	public void put(String key, String value) {
		if (value != null)
			item.put(key, value);
	}

	public void put(String key, Object obj) {
		if (obj == null)
			return;
		else if (obj instanceof RDFNode)
			put(key, (RDFNode) obj);
		else
			put(key, obj.toString());
	}

	public void put(String key, Resource resource, Property property) {
		put(key, resource, property, null);
	}

	public void put(String key, Resource resource, Property property,
			String defaultValue) {
		put(key, getString(resource, property, defaultValue));
	}

	private void putKeywords(String key, OntResource resource, Property property) {
		NodeIterator nodes = resource.listPropertyValues(property);
		try {
			JsonArray list = new JsonArray();

			while (nodes.hasNext()) {
				String keyword = nodes.next().asLiteral().getValue().toString();
				if (!keyword.contains(":"))
					list.add(keyword);
			}

			put(key, list);
		} finally {
			nodes.close();
		}
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

	public static void write(PrintStream out, OntModel model)
			throws IOException {
		new ExhibitWriter(model).write(out);
	}
}
