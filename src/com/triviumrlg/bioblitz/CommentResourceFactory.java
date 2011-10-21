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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

public class CommentResourceFactory {
	private Model base_model;
	private Model model;
	private Map<String, Integer> scientists;

	public CommentResourceFactory(Model base_model) {
		this.base_model = base_model;
		try {
			this.model = FileManager.get().loadModel(
					Config.property(Config.SCIENTISTFILE));
		} catch (Exception e) {
			this.model = ModelFactory.createDefaultModel();
		}

		this.model.setNsPrefixes(Vocabs.getMap());
		this.base_model.add(this.model);
		loadScientists();
	}

	public Model getModel() {
		return base_model;
	}

	private void loadScientists() {
		scientists = new HashMap<String, Integer>();

		StringBuffer queryStr = new StringBuffer();
		queryStr.append(Vocabs.SPARQLPrefixes());
		queryStr.append("SELECT ?index ?member\n");
		queryStr.append("WHERE {\n");
		queryStr.append("  photos:Scientists photos:priorityList [\n");
		queryStr.append("    <http://jena.hpl.hp.com/ARQ/list#index> (?index ?member)\n");
		queryStr.append("  ] .\n");
		queryStr.append("}\n");

		Query query = QueryFactory.create(queryStr.toString());
		QueryExecution ex = QueryExecutionFactory.create(query, model);

		try {
			ResultSet results = ex.execSelect();

			while (results.hasNext()) {
				QuerySolution sol = results.next();
				scientists.put(sol.get("member").asResource().getURI(), sol
						.get("index").asLiteral().getInt());
			}
		} finally {
			ex.close();
		}
	}

	public void processComments() {
		List<Map<String, String>> comments = new LinkedList<Map<String, String>>();

		StringBuffer queryStr = new StringBuffer();
		queryStr.append(Vocabs.SPARQLPrefixes());
		queryStr.append("SELECT ?photo ?author ?created ?text\n");
		queryStr.append("WHERE {\n");
		queryStr.append("  ?photo photos:hasComment ?comment .\n");
		queryStr.append("  ?comment dct:created ?created .\n");
		queryStr.append("  ?comment photos:text ?text .\n");
		queryStr.append("  ?comment photos:hasPhotographer ?author .\n");
		queryStr.append("}\n");
		queryStr.append("ORDER BY ?created\n");

		Query query = QueryFactory.create(queryStr.toString());
		QueryExecution ex = QueryExecutionFactory.create(query, base_model);

		try {
			ResultSet results = ex.execSelect();

			while (results.hasNext()) {
				QuerySolution sol = results.next();
				Map<String, String> comment = new HashMap<String, String>();
				comments.add(comment);

				comment.put("photo", sol.getResource("photo").getURI());
				comment.put("author", sol.getResource("author").getURI());
				comment.put("created", sol.getLiteral("created").getString());
				comment.put("text", sol.getLiteral("text").getString());
			}
		} finally {
			ex.close();
		}

		processComments(comments);
		base_model.add(model);
	}

	private class SpeciesTag {
		private String species;
		private String photo;
		private int priority;

		public SpeciesTag(String species, String photo, int priority) {
			this.species = species;
			this.photo = photo;
			this.priority = priority;
		}

		public String getSpecies() {
			return species;
		}

		public String getPhoto() {
			return photo;
		}

		public int getPriority() {
			return priority;
		}
	}

	private void processComments(List<Map<String, String>> comments) {
		Set<String> ignore_list = new HashSet<String>();
		Map<String, SpeciesTag> species = new HashMap<String, SpeciesTag>();

		Pattern tag_pattern = Pattern
				.compile("\\[\\[\\s*([^=]+)\\s*=\\s*([^\\]]+)\\s*\\]\\]");
		Pattern href_pattern = Pattern.compile(".*href=\\\"([^\"]*).*");

		for (Map<String, String> comment : comments) {
			Matcher matcher = tag_pattern.matcher(comment.get("text"));
			while (matcher.find()) {
				String name = matcher.group(1).trim();
				String value = matcher.group(2).trim();

				if (name.equalsIgnoreCase("noah_user")) {
					if (value.contains(" href=\"")) {
						Matcher hm = href_pattern.matcher(value);
						if (hm.find()) {
							value = hm.group(1);
						}
						if (!value.startsWith("http://"))
							value = "http://" + value;

						value += "#user";
					}

					model.createResource(comment.get("author")).addProperty(
							Vocabs.prop("owl", "sameAs"),
							model.createResource(value));
				} else if (name.equalsIgnoreCase("ignore")) {
					if (value.equalsIgnoreCase("true"))
						ignore_list.add(comment.get("photo"));
					else
						ignore_list.remove(comment.get("photo"));
				} else if (name.equalsIgnoreCase("species")) {
					if (scientists.containsKey(comment.get("author"))) {
						String author = comment.get("author");
						String photo = comment.get("photo");
						int priority = scientists.get(author);
						SpeciesTag tag = species.get(photo);

						if (tag == null || tag.getPriority() > priority)
							tag = new SpeciesTag(value, photo, priority);

						species.put(photo, tag);
					}
				}
			}
		}

		for (SpeciesTag tag : species.values()) {
			model.createResource(tag.getPhoto()).addProperty(
					Vocabs.prop("photos", "scientistNamedSpecies"),
					model.createTypedLiteral(tag.getSpecies()));
		}

		for (String uri : ignore_list) {
			model.createResource(uri).addProperty(
					Vocabs.prop("photos", "ignore"),
					model.createTypedLiteral(true));
		}
	}
}
