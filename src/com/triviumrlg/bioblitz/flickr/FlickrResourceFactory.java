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

import java.util.List;

import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photos.GeoData;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.comments.Comment;
import com.aetrion.flickr.tags.Tag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.triviumrlg.bioblitz.Vocabs;
import com.triviumrlg.bioblitz.utils.RDFHelper;

public class FlickrResourceFactory {
	private Model model;
	private String base;
	private RDFHelper helper;

	public FlickrResourceFactory(Model model) {
		this.model = model;
		this.base = Vocabs.uri("flickr");

		model.setNsPrefixes(Vocabs.getMap());

		helper = new RDFHelper(model);
	}

	public Resource createUser(String userId, String userName) {
		String uri = String.format("http://www.flickr.com/people/%s/#user",
				helper.encodeURIPart(userId));
		Resource user = model.createResource(uri);
		helper.addLiteral(user, base + "userId", userId);
		helper.addLiteral(user, base + "userName", userName);

		return user;
	}

	public Resource createPhoto(Photo photo, List<Tag> tags,
			List<Comment> comments, GeoData geo) {
		Resource photoSub = model.createResource(photo.getUrl() + "#photo");

		helper.addResource(photoSub, base + "hasHomepage", photo.getUrl());

		User user = photo.getOwner();

		helper.addResource(photoSub, base + "hasAuthor",
				createUser(user.getId(), user.getUsername()));

		helper.addLiteral(photoSub, base + "description",
				photo.getDescription());
		helper.addLiteral(photoSub, base + "photoId", photo.getId());
		helper.addLiteral(photoSub, base + "title", photo.getTitle());
		helper.addLiteral(photoSub, base + "licenseId", photo.getLicense());
		helper.addLiteral(photoSub, base + "pathAlias", photo.getPathAlias());
		helper.addLiteral(photoSub, base + "placeId", photo.getPlaceId());

		helper.addResource(photoSub, base + "hasLargeImage",
				photo.getLargeUrl());
		helper.addResource(photoSub, base + "hasMediumImage",
				photo.getMediumUrl());
		helper.addResource(photoSub, base + "hasSmallSqaureImage",
				photo.getSmallSquareUrl());
		helper.addResource(photoSub, base + "hasSmallImage",
				photo.getSmallUrl());
		helper.addResource(photoSub, base + "hasThumbnail",
				photo.getThumbnailUrl());

		helper.addDate(photoSub, base + "dateAdded", photo.getDateAdded());
		helper.addDate(photoSub, base + "datePosted", photo.getDatePosted());
		helper.addDate(photoSub, base + "dateTaken", photo.getDateTaken());
		helper.addDate(photoSub, base + "lastUpdated", photo.getLastUpdate());

		if (geo != null) {
			helper.addLiteral(photoSub, base + "latitude", geo.getLatitude());
			helper.addLiteral(photoSub, base + "longitude", geo.getLongitude());
			helper.addLiteral(photoSub, base + "geoAccuracy", geo.getAccuracy());
		}

		for (Tag tag : tags) {
			Resource tagSub = model.createResource();

			helper.addResource(tagSub, base + "hasAuthor",
					createUser(tag.getAuthor(), tag.getAuthorName()));

			helper.addLiteral(tagSub, base + "tagId", tag.getId());
			helper.addLiteral(tagSub, base + "raw", tag.getRaw());
			helper.addLiteral(tagSub, base + "value", tag.getValue());

			helper.addResource(photoSub, base + "hasTag", tagSub);

			helper.addLiteral(photoSub, base + "keyword", tag.getRaw());
		}

		for (Comment comment : comments) {
			Resource cmntSub = model.createResource(comment.getPermaLink());

			helper.addResource(cmntSub, base + "hasAuthor",
					createUser(comment.getAuthor(), comment.getAuthorName()));

			helper.addDate(cmntSub, base + "dateCreated",
					comment.getDateCreate());
			helper.addLiteral(cmntSub, base + "commentId", comment.getId());
			helper.addLiteral(cmntSub, base + "text", comment.getText());

			helper.addResource(photoSub, base + "hasComment", cmntSub);
		}

		return photoSub;
	}
}