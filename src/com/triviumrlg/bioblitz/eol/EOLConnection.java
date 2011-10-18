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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.openjena.atlas.json.JSON;
import org.openjena.atlas.json.JsonValue;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.triviumrlg.bioblitz.Config;
import com.triviumrlg.bioblitz.utils.JSONObject;

public class EOLConnection {
	private Client client;
	private String endPoint;
	private SpeciesCache cache;

	public EOLConnection() {
		this(Config.property(Config.EOL_ENDPOINT));
	}

	public EOLConnection(String endPoint) {
		this.endPoint = endPoint;

		if (!this.endPoint.endsWith("/"))
			this.endPoint += "/";

		client = Client.create();

		String logfile;
		if ((logfile = Config.property(Config.EOL_LOGFILE)) != null) {
			try {
				client.addFilter(new LoggingFilter(new PrintStream(
						new FileOutputStream(logfile))));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(
						"FileNotFoundException while writing to " + logfile, e);
			}
		}

		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					Config.property(Config.EOL_CACHEFILE)));
			try {
				cache = (SpeciesCache) in.readObject();
			} finally {
				in.close();
			}
		} catch (Exception e) {
			cache = new SpeciesCache();
		}
	}

	public WebResource getWebResource(String endPoint, String path) {
		try {
			return client.resource(endPoint
					+ URLEncoder.encode(path, "UTF-8").replaceAll("\\+", "%20")
					+ ".json");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UnsupportedEncodingException", e);
		}
	}

	public Species speciesTaxaSearch(String speciesName, String pageId) {
		try {
			if (pageId == null) {
				WebResource webResource = getWebResource(endPoint
						+ "search/1.0/", speciesName);

				MultivaluedMap<String, String> params = new MultivaluedMapImpl();
				params.add("exact", "1");

				JSONObject output = getResponse(webResource, params);

				Integer totalResults = output.getInteger("totalResults");
				if (totalResults == null || totalResults != 1)
					return null;

				pageId = new JSONObject(output.getArray("results").get(0)
						.getAsObject()).getString("id");
			}

			Species species = new Species(speciesName);
			species.setEolPageId(pageId);
			WebResource webResource;
			MultivaluedMap<String, String> params;
			JSONObject output;

			params = new MultivaluedMapImpl();
			params.add("common_names", "1");

			webResource = getWebResource(endPoint + "pages/1.0/", pageId);
			output = getResponse(webResource, params);

			Map<String, JSONObject> taxonMap = new HashMap<String, JSONObject>();
			JSONObject defaultTaxon = null;

			for (JsonValue taxonVal : output.getArray("taxonConcepts")) {
				JSONObject taxon = new JSONObject(taxonVal.getAsObject());
				taxonMap.put(taxon.getString("nameAccordingTo"), taxon);
				defaultTaxon = defaultTaxon == null ? taxon : defaultTaxon;
			}

			// Prefer ITIS, and then the 2000 ITIS Catalogue, and finally the
			// first
			// taxon found
			JSONObject taxon = taxonMap
					.get("Integrated Taxonomic Information System (ITIS)");
			if (taxon == null)
				taxon = taxonMap
						.get("Species 2000 & ITIS Catalogue of Life: Annual Checklist 2010");
			if (taxon == null)
				taxon = defaultTaxon;
			if (taxon == null)
				return null;

			webResource = getWebResource(endPoint + "hierarchy_entries/1.0/",
					taxon.getString("identifier"));
			params = new MultivaluedMapImpl();
			params.add("common_names", "1");
			output = getResponse(webResource, params);

			for (JsonValue ancestorVal : output.getArray("ancestors")) {
				JSONObject ancestor = new JSONObject(ancestorVal.getAsObject());
				species.addTaxon(capitalize(ancestor.getString("taxonRank")),
						capitalize(ancestor.getString("scientificName")));
			}

			return species;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return null;
		}
	}

	public Species speciesTaxaSearch(String speciesName) {
		Species species;

		if (cache.hasSpecies(speciesName))
			return cache.get(speciesName);

		species = speciesTaxaSearch(speciesName, null);
		cache.put(speciesName, species);

		return species;
	}

	public JSONObject getResponse(WebResource webResource,
			MultivaluedMap<String, String> params) {
		int trys = 2;
		RuntimeException ex = new RuntimeException("failed");
		while (trys-- > 0) {
			try {
				return new JSONObject(JSON.parse(webResource
						.queryParams(params).get(String.class)));
			} catch (Exception e) {
				ex = new RuntimeException("failed", e);
			}

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
			}
		}

		throw ex;
	}

	public String capitalize(String str) {
		if (str == null || str.isEmpty())
			return str;

		return Character.toUpperCase(str.charAt(0))
				+ str.substring(1).toLowerCase();
	}

	public void close() {
		try {
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(Config.property(Config.EOL_CACHEFILE)));
			try {
				out.writeObject(cache);
			} finally {
				out.close();
			}
		} catch (Exception e) {
		}
	}
}
