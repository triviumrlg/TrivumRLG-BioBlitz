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

package com.triviumrlg.bioblitz.flickr;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.photos.Extras;
import com.aetrion.flickr.photos.GeoData;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.comments.Comment;
import com.aetrion.flickr.photos.comments.CommentsInterface;
import com.aetrion.flickr.tags.Tag;
import com.aetrion.flickr.tags.TagsInterface;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.triviumrlg.bioblitz.Config;
import com.triviumrlg.bioblitz.flickr.cache.PhotoCache;
import com.triviumrlg.bioblitz.flickr.cache.PhotoData;

public class FlickrConnection {
	private Flickr client;
	private String apiKey;
	private String endPoint;
	private String endPointHostname;
	private TagsInterface tagsClient;
	private CommentsInterface commentsClient;
	private int limit;
	private PhotoCache cache;

	public FlickrConnection() {
		this(Config.property(Config.FLICKR_APIKEY), Config
				.property(Config.FLICKR_SECRET), Config
				.property(Config.FLICKR_ENDPOINT));
	}

	public FlickrConnection(String apiKey, String secret, String endPoint) {
		this.apiKey = apiKey;
		setEndPoint(endPoint);

		limit = Config.property(Config.FLICKR_LIMIT) == null ? 0 : Integer
				.parseInt(Config.property(Config.FLICKR_LIMIT));

		try {
			client = new Flickr(this.apiKey, new REST(endPointHostname));
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		client.setSharedSecret(secret);

		tagsClient = client.getTagsInterface();
		commentsClient = client.getCommentsInterface();

		if ("true".equalsIgnoreCase(Config.property(Config.FLICKR_DUMPSTREAM))) {
			Flickr.debugRequest = true;
			Flickr.debugStream = true;
		}

		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					Config.property(Config.FLICKR_CACHEFILE)));
			try {
				cache = (PhotoCache) in.readObject();
			} finally {
				in.close();
			}
		} catch (Exception e) {
			cache = new PhotoCache();
		}
	}

	public void setEndPoint(String endPoint) {
		this.endPoint = endPoint;

		if (!this.endPoint.startsWith("http"))
			this.endPoint = "http://" + this.endPoint;

		if (!this.endPoint.endsWith("/"))
			this.endPoint += "/";

		try {
			URL url = new URL(this.endPoint);
			endPointHostname = url.getHost();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Bad endpoint format", e);
		}
	}

	public Model getPhotoModel(String groupURL) throws FlickrException,
			IOException, SAXException {
		Model model = ModelFactory.createDefaultModel();
		final FlickrResourceFactory factory = new FlickrResourceFactory(model);
		final int limit = this.limit;

		String groupId = getGroupId(groupURL);

		visitPhotos(groupId, new PhotoVisitor() {
			@Override
			public boolean visit(int num, Photo photo) {
				List<Tag> tags;
				List<Comment> comments;
				GeoData geo;
				PhotoData cache_item = null;

				try {
					cache_item = cache.get(photo.getId());
					if (cache_item != null
							&& !cache_item.getLastUpdate().equals(
									photo.getLastUpdate())) {
						cache.remove(photo.getId());
						cache_item = null;
					}

					if (cache_item == null) {
						tags = new LinkedList<Tag>();
						for (Object t : tagsClient.getListPhoto(photo.getId())
								.getTags())
							tags.add((Tag) t);

						comments = new LinkedList<Comment>();
						for (Object c : commentsClient.getList(photo.getId()))
							comments.add((Comment) c);

						geo = null;

						if (photo.hasGeoData())
							geo = photo.getGeoData();

						cache_item = new PhotoData(photo.getLastUpdate(), tags,
								comments, geo);
						cache.put(photo.getId(), cache_item);
					}

					factory.createPhoto(photo, cache_item.getTags(),
							cache_item.getComments(), cache_item.getGeo());
					return limit == 0 || num < limit;
				} catch (Exception e) {
					throw new RuntimeException("Could not process flickr data",
							e);
				}
			}
		});

		return model;
	}

	public String getGroupId(String groupURL) throws FlickrException,
			IOException, SAXException {
		return client.getUrlsInterface().lookupGroup(groupURL).getId();
	}

	public void visitPhotos(String groupId, PhotoVisitor visitor)
			throws IOException, SAXException, FlickrException {
		visitPhotos(groupId, visitor, 0, 0);
	}

	public void visitPhotos(String groupId, PhotoVisitor visitor, int num,
			int page) throws IOException, SAXException, FlickrException {
		PhotoList list = client.getPoolsInterface().getPhotos(groupId, null,
				Extras.ALL_EXTRAS, limit < 500 ? limit : 500, page);

		for (Object photoObj : list) {
			Photo photo = (Photo) photoObj;

			if (!visitor.visit(++num, photo))
				return;
		}

		if (list.getPage() < list.getPages())
			visitPhotos(groupId, visitor, num, list.getPage() + 1);
	}

	private interface PhotoVisitor {
		public boolean visit(int num, Photo photo);
	}

	public void close() {
		try {
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(
							Config.property(Config.FLICKR_CACHEFILE)));
			try {
				out.writeObject(cache);
			} finally {
				out.close();
			}
		} catch (Exception e) {
		}
	}
}