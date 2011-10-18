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

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.SAXException;

import com.aetrion.flickr.FlickrException;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.vocabulary.RDF;
import com.triviumrlg.bioblitz.eol.EOLResourceFactory;
import com.triviumrlg.bioblitz.exhibit.ExhibitWriter;
import com.triviumrlg.bioblitz.flickr.FlickrConnection;
import com.triviumrlg.bioblitz.noah.NoahConnection;
import com.triviumrlg.bioblitz.noah.NoahException;

public class Main {
	public static void main(String[] args) throws Exception {
		LocationMapper map = FileManager.get().getLocationMapper();
		map.addAltPrefix("http://", "jar:");
		FileManager.get().addLocator(new JarLocator());

		Iterator<String> itr = Arrays.asList(args).iterator();

		while (itr.hasNext()) {
			String arg = itr.next();

			if (arg.equals("--get-noah"))
				getProjectNoahModel().write(System.out, "TURTLE");
			else if (arg.equals("--get-flickr"))
				getFlickrModel().write(System.out, "TURTLE");
			else if (arg.equals("--get-all"))
				getAllData().write(System.out, "TURTLE");
			else if (arg.equals("--add-eol")) {
				Model model = itr.hasNext() ? FileManager.get().loadModel(
						itr.next()) : ModelFactory.createDefaultModel();

				addEOL(model).write(System.out, "TURTLE");
			} else if (arg.equals("--add-comment-tags")) {
				Model model = itr.hasNext() ? FileManager.get().loadModel(
						itr.next()) : ModelFactory.createDefaultModel();

				addCommentTags(model).write(System.out, "TURTLE");
			} else if (arg.equals("--rdfs")) {
				List<String> files = new LinkedList<String>();

				while (itr.hasNext())
					files.add(itr.next());

				getRDFS(files).writeAll(System.out, "TURTLE", null);
			} else if (arg.equals("--dump-photo-ontology")) {
				getPhotosDomainModel().write(System.out,
						itr.hasNext() ? itr.next() : "RDF/XML");
			} else if (arg.equals("--dump-noah-ontology")) {
				getNoahDomainModel().write(System.out,
						itr.hasNext() ? itr.next() : "RDF/XML");
			} else if (arg.equals("--dump-flickr-ontology")) {
				getFlickrDomainModel().write(System.out,
						itr.hasNext() ? itr.next() : "RDF/XML");
			} else if (arg.equals("--to-ntriples")) {
				Model model = itr.hasNext() ? FileManager.get().loadModel(
						itr.next()) : ModelFactory.createDefaultModel();
				model.write(System.out, "N-TRIPLES");
			} else if (arg.equals("--report")) {
				Model model = itr.hasNext() ? FileManager.get().loadModel(
						itr.next()) : ModelFactory.createDefaultModel();
				report(model);
			} else if (arg.equals("--make-exhibit")) {

				OntModel model;

				if (itr.hasNext()) {
					model = getRDFS(FileManager.get().loadModel(itr.next()));
				} else {
					model = getRDFS(getAllData());
					addCommentTags(model);
					addEOL(model);
				}

				ExhibitWriter.write(System.out, model);
			}
		}
	}

	public static Model getProjectNoahModel() throws NoahException {
		NoahConnection noah = new NoahConnection();
		Model model = noah.getMissionSpottingsModel(Config
				.property(Config.NOAH_MISSIONID));

		return model;
	}

	public static Model getFlickrModel() throws FlickrException, IOException,
			SAXException {
		FlickrConnection flickr = new FlickrConnection();
		Model model = flickr.getPhotoModel(Config
				.property(Config.FLICKR_GROUPURL));
		flickr.close();

		return model;
	}

	public static OntModel getRDFS(List<String> files) {
		OntModel onto = getBioBlitzOntology();
		for (String file : files)
			onto.addSubModel(FileManager.get().loadModel(file));

		return onto;
	}

	public static OntModel getRDFS(Model model) {
		OntModel onto = getBioBlitzOntology();
		onto.addSubModel(model);

		return onto;
	}

	public static Model addCommentTags(Model model) {
		CommentResourceFactory comments = new CommentResourceFactory(model);
		comments.processComments();
		return comments.getModel();
	}

	public static Model addEOL(Model base_data) {
		EOLResourceFactory eol = new EOLResourceFactory();

		StmtIterator itr = base_data.listStatements(null, RDF.type,
				Vocabs.prop("photos", "Photo"));
		try {
			while (itr.hasNext()) {
				Statement s = itr.next();

				eol.addEOLTaxa(s.getSubject());
			}
		} finally {
			itr.close();
		}

		base_data.add(eol.getModel());
		eol.close();
		return base_data;
	}

	public static Model getAllData() throws Exception {
		final Model[] models = new Model[2];

		Thread noahThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					System.err.println("Starting Noah fetch");
					long startTime = System.currentTimeMillis();
					models[0] = getProjectNoahModel();
					long endTime = System.currentTimeMillis();
					long seconds = (endTime - startTime) / 1000;
					System.err.println("Finished Noah fetch in " + seconds
							+ " seconds");
				} catch (NoahException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		});

		Thread flickrThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					System.err.println("Starting Flickr fetch");
					long startTime = System.currentTimeMillis();
					models[1] = getFlickrModel();
					long endTime = System.currentTimeMillis();
					long seconds = (endTime - startTime) / 1000;
					System.err.println("Finished Flickr fetch in " + seconds
							+ " seconds");
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		});

		noahThread.start();
		flickrThread.start();

		noahThread.join();
		flickrThread.join();

		Model all = ModelFactory.createDefaultModel();
		all.setNsPrefixes(Vocabs.getMap());
		for (Model m : models) {
			all.add(m);
			m.close();
		}

		return all;
	}

	private static void report(Model model) {
		reportClasses(model);
		reportProps(model);
	}

	private static void reportClasses(Model model) {
		StringBuffer queryStr = new StringBuffer();
		queryStr.append(Vocabs.SPARQLPrefixes());
		queryStr.append("SELECT ?class (count(?s) as ?count)\n");
		queryStr.append("WHERE {\n");
		queryStr.append("  ?class a owl:Class .\n");
		queryStr.append("  ?s a ?class .\n");
		queryStr.append("}\n");
		queryStr.append("GROUP BY ?class\n");
		queryStr.append("ORDER BY DESC(?count) ?class\n");

		Query query = QueryFactory.create(queryStr.toString());
		QueryExecution ex = QueryExecutionFactory.create(query, model);

		try {
			ResultSet results = ex.execSelect();
			ResultSetFormatter.out(results, query);
		} finally {
			ex.close();
		}
	}

	private static void reportProps(Model model) {
		StringBuffer queryStr = new StringBuffer();
		queryStr.append(Vocabs.SPARQLPrefixes());
		queryStr.append("SELECT ?property (count(?s) as ?count)\n");
		queryStr.append("WHERE {\n");
		queryStr.append("  ?s ?property ?o .\n");
		queryStr.append("}\n");
		queryStr.append("GROUP BY ?property\n");
		queryStr.append("ORDER BY DESC(?count) ?property\n");

		Query query = QueryFactory.create(queryStr.toString());
		QueryExecution ex = QueryExecutionFactory.create(query, model);

		try {
			ResultSet results = ex.execSelect();
			ResultSetFormatter.out(results, query);
		} finally {
			ex.close();
		}
	}

	private static OntModel getBioBlitzOntology() {
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF,
				getPhotosDomainModel());
	}

	private static Model getPhotosDomainModel() {
		return FileManager.get().loadModel(Vocabs.httpURL("photos"));
	}

	private static Model getNoahDomainModel() {
		return FileManager.get().loadModel(Vocabs.httpURL("noah"));
	}

	private static Model getFlickrDomainModel() {
		return FileManager.get().loadModel(Vocabs.httpURL("flickr"));
	}
}
