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

import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.triviumrlg.bioblitz.Vocabs;
import com.triviumrlg.bioblitz.utils.RDFHelper;

public class EOLResourceFactory {
	private Model model;
	private RDFHelper helper;
	private EOLConnection eol;

	public EOLResourceFactory() {
		this(ModelFactory.createDefaultModel());
	}

	public EOLResourceFactory(Model model) {
		this.model = model;
		helper = new RDFHelper(model);
		eol = new EOLConnection();
	}

	public Model getModel() {
		return model;
	}

	public void addEOLTaxa(Resource photo) {
		String pro_species = helper.getString(photo,
				Vocabs.prop("photos", "scientistNamedSpecies"));

		if (pro_species != null) {
			addEolTaxonTags(photo, pro_species);
			helper.addLiteral(model.createResource(photo.getURI()),
					Vocabs.uri("photos", "species"), pro_species);

			return;
		}

		Pattern machinePattern = Pattern.compile("^(taxonomy):(.*)=(.*)$");
		Pattern eolPagePattern = Pattern.compile("eol.org/pages/([^/]+)");

		StmtIterator itr = photo
				.listProperties(Vocabs.prop("flickr", "hasTag"));

		try {
			String species = null;

			while (itr.hasNext()) {
				Statement has_tag = itr.next();

				if (!has_tag.getObject().isResource())
					continue;

				Resource tag = has_tag.getObject().asResource();
				String raw = helper
						.getString(tag, Vocabs.prop("flickr", "raw"));

				if (raw == null)
					continue;

				Matcher matcher = machinePattern.matcher(raw);
				if (matcher.find()) {
					String rank = matcher.group(2);
					String name = matcher.group(3);

					addTaxonTag(photo, rank, name);

					if (rank.equalsIgnoreCase("species") && species == null)
						species = name;

					if (rank.equalsIgnoreCase("binomial"))
						species = name;
				}
			}

			if (species != null) {
				addEolTaxonTags(photo, species);

				helper.addLiteral(model.createResource(photo.getURI()),
						Vocabs.uri("photos", "species"), species);
			}
		} finally {
			itr.close();
		}

		String scientific = helper.getString(photo,
				Vocabs.prop("noah", "scientific"));

		if (scientific != null) {
			addTaxonTag(photo, "species", scientific);

			String pageId = helper.getString(photo,
					Vocabs.prop("noah", "eol_link"));

			Matcher matcher = pageId == null ? null : eolPagePattern
					.matcher(pageId);
			if (matcher != null && matcher.find()) {
				addEolTaxonTagsByPageId(photo, scientific, matcher.group(1));
			} else
				addEolTaxonTags(photo, scientific);
			helper.addLiteral(model.createResource(photo.getURI()),
					Vocabs.uri("photos", "species"), scientific);
		}
	}

	private Resource createTaxonTag(String rank, String name) {
		Resource taxon = model.createResource();

		helper.addLiteral(taxon, Vocabs.uri("dwc", "taxonRank"),
				rank.toLowerCase());
		helper.addLiteral(taxon, Vocabs.uri("dwc", "scientificName"), name);

		return taxon;
	}

	private void addTaxonTag(Resource resource, String rank, String name) {
		if (resource == null || rank == null || name == null)
			return;

		helper.addResource(model.createResource(resource.getURI()),
				Vocabs.uri("photos", "hasTaxonTag"), createTaxonTag(rank, name));
	}

	private void addEolTaxonTags(Resource resource, String scientificName) {
		if (resource == null || scientificName == null
				|| scientificName.isEmpty())
			return;

		Species species = eol.speciesTaxaSearch(scientificName);
		processSpecies(resource, species);
	}

	private void addEolTaxonTagsByPageId(Resource resource,
			String scientificName, String pageId) {
		if (resource == null || scientificName == null
				|| scientificName.isEmpty())
			return;

		Species species = eol.speciesTaxaSearch(scientificName, pageId);
		processSpecies(resource, species);
	}

	private void processSpecies(Resource resource, Species species) {
		if (species != null) {
			for (Entry<String, String> entry : species.getTaxa().entrySet()) {
				if (entry.getValue() == null || entry.getKey() == null)
					continue;

				Resource eolTaxon = createTaxonTag(entry.getKey(),
						entry.getValue());

				helper.addResource(model.createResource(resource.getURI()),
						Vocabs.uri("photos", "hasEOLTaxonTag"), eolTaxon);
			}

			helper.addLiteral(model.createResource(resource.getURI()),
					Vocabs.uri("photos", "eolPageId"), species.getEOLPageId());
		}
	}

	public void close() {
		eol.close();
	}
}
