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

package com.triviumrlg.bioblitz.flickr.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PhotoData implements Serializable {
	private static final long serialVersionUID = 5843961352085810768L;
	private Date last_update;
	private List<Tag> tags;
	private List<Comment> comments;
	private GeoData geo;

	public PhotoData(Date last_update, List<com.aetrion.flickr.tags.Tag> tags,
			List<com.aetrion.flickr.photos.comments.Comment> comments,
			com.aetrion.flickr.photos.GeoData geo) {
		this.last_update = last_update;

		this.tags = new ArrayList<Tag>(tags.size());
		for (com.aetrion.flickr.tags.Tag t : tags) {
			this.tags.add(new Tag(t.getId(), t.getAuthor(), t.getAuthorName(),
					t.getRaw(), t.getValue()));
		}

		this.comments = new ArrayList<Comment>(comments.size());

		for (com.aetrion.flickr.photos.comments.Comment c : comments) {
			this.comments.add(new Comment(c.getId(), c.getAuthor(), c
					.getAuthorName(), c.getDateCreate(), c.getPermaLink(), c
					.getText()));
		}

		if (geo != null) {
			this.geo = new GeoData(geo.getLongitude(), geo.getLatitude(),
					geo.getAccuracy());
		}
	}

	public Date getLastUpdate() {
		return last_update;
	}

	public List<Tag> getTags() {
		return tags;
	}

	public List<Comment> getComments() {
		return comments;
	}

	public GeoData getGeo() {
		return geo;
	}
}
