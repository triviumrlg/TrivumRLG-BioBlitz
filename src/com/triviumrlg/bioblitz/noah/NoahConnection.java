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

package com.triviumrlg.bioblitz.noah;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;

import org.openjena.atlas.json.JSON;
import org.openjena.atlas.json.JsonObject;
import org.openjena.atlas.json.JsonValue;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.triviumrlg.bioblitz.Config;
import com.triviumrlg.bioblitz.utils.JSONObject;

public class NoahConnection {
	private Client client;
	private String apiKey;
	private String endPoint;
	private int limit;

	public NoahConnection() {
		this(Config.property(Config.NOAH_APIKEY), Config
				.property(Config.NOAH_ENDPOINT));

	}

	public NoahConnection(String apiKey, String endPoint) {
		this.apiKey = apiKey;
		this.endPoint = endPoint;

		if (!this.endPoint.endsWith("/"))
			this.endPoint += "/";

		limit = Config.property(Config.NOAH_LIMIT) == null ? 0 : Integer
				.parseInt(Config.property(Config.NOAH_LIMIT));

		client = Client.create();

		String logfile;
		if ((logfile = Config.property(Config.NOAH_LOGFILE)) != null) {
			try {
				client.addFilter(new LoggingFilter(new PrintStream(
						new FileOutputStream(logfile))));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(
						"FileNotFoundException while writing to " + logfile, e);
			}
		}
	}

	public Model getMissionSpottingsModel(String missionId)
			throws NoahException {
		Model model = ModelFactory.createDefaultModel();
		final NoahResourceFactory factory = new NoahResourceFactory(model);
		final List<String> spottingIds = new LinkedList<String>();
		final int limit = this.limit;

		visitSpottings(missionId, new ResultsVisitor() {
			@Override
			public boolean visit(int num, JSONObject result) {
				factory.createSpotting(result);
				spottingIds.add(result.getString("id"));

				return limit == 0 || num <= limit;
			}
		});

		for (String id : spottingIds) {
			visitComments(id, new ResultsVisitor() {
				@Override
				public boolean visit(int num, JSONObject result) {
					factory.createComment(result);
					return true;
				}
			});
		}

		return model;
	}

	public void visitSpottings(String missionId, ResultsVisitor visitor)
			throws NoahException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("mission", missionId);

		visitResults(endPoint + "spottings", params, visitor, 0, null);
	}

	public void visitComments(String spottingsId, ResultsVisitor visitor)
			throws NoahException {
		visitResults(endPoint + "spottings/" + spottingsId + "/comments", null,
				visitor, 0, null);
	}

	public void visitResults(String url, Map<String, String> queryParams,
			ResultsVisitor visitor, int number, String cursor)
			throws NoahException {
		WebResource webResource = client.resource(url);

		MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		params.add("api_key", apiKey);
		params.add("limit", limit > 0 && limit < 100 ? Integer.toString(limit)
				: "100");

		if (queryParams != null)
			for (Entry<String, String> e : queryParams.entrySet())
				params.add(e.getKey(), e.getValue());

		if (cursor != null)
			params.add("cursor", cursor);

		JSONObject output = new JSONObject(JSON.parse(webResource.queryParams(
				params).get(String.class)));

		if (output.hasKey("error"))
			throw new NoahException(output);

		for (JsonValue obj : output.getArray("results")) {
			JsonObject result = obj.getAsObject();

			if (!visitor.visit(++number, new JSONObject(result)))
				return;
		}

		if (output.getString("cursor") != null)
			visitResults(url, queryParams, visitor, number,
					output.getString("cursor"));
	}
}
