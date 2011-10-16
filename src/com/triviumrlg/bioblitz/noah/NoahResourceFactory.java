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

import org.openjena.atlas.json.JsonArray;
import org.openjena.atlas.json.JsonValue;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.triviumrlg.bioblitz.Config;
import com.triviumrlg.bioblitz.Vocabs;
import com.triviumrlg.bioblitz.utils.JSONObject;
import com.triviumrlg.bioblitz.utils.RDFHelper;

public class NoahResourceFactory {
	private Model model;
	private RDFHelper helper;
	private String base;
	private String spotting_fmt;
	private String user_fmt;
	private String comment_fmt;

	public NoahResourceFactory(Model model) {
		this.model = model;
		base = Vocabs.uri("noah");

		model.setNsPrefixes(Vocabs.getMap());

		helper = new RDFHelper(model);

		spotting_fmt = Config.property(Config.NOAH_URI_SPOTTING);
		user_fmt = Config.property(Config.NOAH_URI_USER);
		comment_fmt = Config.property(Config.NOAH_URI_COMMENT);
	}

	public Resource spottingResource(String id) {
		String uri = String.format(spotting_fmt, helper.encodeURIPart(id));
		Resource spotting = model.createResource(uri);
		helper.addLiteral(spotting, base + "spotting_id", id);
		helper.addResource(spotting, base + "hasHomepage",
				"http://www.projectnoah.org/spottings/" + id);

		return spotting;
	}

	public Resource authorResource(String username) {
		String uri = String.format(user_fmt, helper.encodeURIPart(username));
		Resource author = model.createResource(uri);
		helper.addLiteral(author, base + "username", username);

		return author;
	}

	public Resource commentResource(String id, String spottingId) {
		String uri = String.format(comment_fmt,
				helper.encodeURIPart(spottingId), helper.encodeURIPart(id));
		Resource comment = model.createResource(uri);
		helper.addLiteral(comment, base + "commentId", id);

		return comment;
	}

	public Resource createSpotting(JSONObject result) {
		Resource spotting = spottingResource(result.getString("id"));

		String[] simpleKeys = { "name", "description", "habitat", "notes",
				"category", "wiki_title", "eol_title", "wiki_link", "eol_link",
				"urban_area_name", "province_name", "country_name", "needs_id",
				"comment_count", "flag_count", "like_count",
				"suggestion_count", "scientific" };

		for (String key : simpleKeys)
			helper.addLiteral(spotting, base + key, result.get(key));

		helper.addDate(spotting, base + "spotted_on",
				result.getString("spotted_on"));
		helper.addDate(spotting, base + "submitted_on",
				result.getString("submitted_on"));

		Resource author = createAuthor(result.getString("author_name"));

		helper.addResource(spotting, base + "hasAuthor", author);

		JSONObject images = result.getObject("images");
		helper.addResource(spotting, base + "hasPrimaryImage",
				images.getString("primary"));

		for (String secondary : images.getStringList("secondary"))
			helper.addResource(spotting, base + "hasSecondaryImage", secondary);

		JsonArray location = result.getArray("location");
		if (location.size() == 2) {
			helper.addLiteral(spotting, base + "latitude", location.get(0));
			helper.addLiteral(spotting, base + "longitude", location.get(1));
		}

		for (JsonValue tag : result.getArray("tags"))
			helper.addLiteral(spotting, base + "tag", tag);

		helper.addResource(spotting, base + "hasPrimaryThumbnail",
				images.getString("primary") + "=s240-c");

		helper.addResource(spotting, base + "hasPrimarySmallThumbnail",
				images.getString("primary") + "=s75-c");

		return spotting;
	}

	public Resource createAuthor(String authorName) {
		return createAuthor(authorName, null);
	}

	public Resource createAuthor(String authorName, String image) {
		// Merge all anonymous posters into one virtual account
		if (authorName == null) {
			authorName = "__Anonymous User__";
		}

		Resource author = authorResource(authorName);

		helper.addResource(author, base + "userImage", image);

		return author;
	}

	public Resource createComment(JSONObject result) {
		Resource comment = commentResource(result.getString("id"),
				result.getString("spotting"));

		String[] simpleKeys = { "text", "vote_count" };

		for (String key : simpleKeys)
			helper.addLiteral(comment, base + key, result.get(key));

		helper.addDate(comment, base + "created", result.getString("created"));

		JSONObject authorObj = result.getObject("author");

		Resource author = createAuthor(authorObj.getString("username"),
				authorObj.getString("image"));

		helper.addResource(comment, base + "hasAuthor", author);

		helper.addResource(spottingResource(result.getString("spotting")), base
				+ "hasComment", comment);

		return comment;
	}
}
