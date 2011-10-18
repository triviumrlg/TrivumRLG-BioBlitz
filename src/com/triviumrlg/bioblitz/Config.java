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

import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

public class Config {
	private static Config theConfig;

	public static String NOAH_ENDPOINT = "bioblitz.noah.endpoint";
	public static String NOAH_LOGFILE = "bioblitz.noah.logfile";
	public static String NOAH_APIKEY = "bioblitz.noah.apikey";
	public static String NOAH_MISSIONID = "bioblitz.noah.missionID";
	public static String NOAH_LIMIT = "bioblitz.noah.limit";
	public static String NOAH_URI_SPOTTING = "bioblitz.noah.uri.spotting";
	public static String NOAH_URI_USER = "bioblitz.noah.uri.user";
	public static String NOAH_URI_COMMENT = "bioblitz.noah.uri.comment";

	public static String FLICKR_ENDPOINT = "bioblitz.flickr.endpoint";
	public static String FLICKR_DUMPSTREAM = "bioblitz.flickr.dumpstream";
	public static String FLICKR_APIKEY = "bioblitz.flickr.apikey";
	public static String FLICKR_GROUPURL = "bioblitz.flickr.groupURL";
	public static String FLICKR_SECRET = "bioblitz.flickr.secret";
	public static String FLICKR_LIMIT = "bioblitz.flickr.limit";
	public static String FLICKR_CACHEFILE = "bioblitz.flickr.cachefile";

	public static String EOL_ENDPOINT = "bioblitz.eol.endpoint";
	public static String EOL_LOGFILE = "bioblitz.eol.logfile";
	public static String EOL_CACHEFILE = "bioblitz.eol.cachefile";

	public static String SCIENTISTFILE = "bioblitz.scientistfile";

	private static Properties defaultProps() {
		Properties props = new Properties(System.getProperties());

		props.setProperty(NOAH_ENDPOINT, "http://www.projectnoah.org/api/v1/");
		props.setProperty(NOAH_MISSIONID, "6986014");
		props.setProperty(NOAH_URI_SPOTTING,
				"http://www.projectnoah.org/spottings/%s#spotting");
		props.setProperty(NOAH_URI_USER,
				"http://www.projectnoah.org/users/%s#user");
		props.setProperty(NOAH_URI_COMMENT,
				"http://www.projectnoah.org/spottings/%s#comment_%s");

		props.setProperty(FLICKR_ENDPOINT, "http://api.flickr.com/");
		props.setProperty(FLICKR_GROUPURL,
				"http://www.flickr.com/groups/1725510@N21");
		props.setProperty(FLICKR_CACHEFILE, "flickr_cache.obj");

		props.setProperty(EOL_ENDPOINT, "http://eol.org/api/");
		props.setProperty(EOL_CACHEFILE, "eol_cache.obj");

		props.setProperty(SCIENTISTFILE, "scientists.ttl");

		return props;
	}

	public static Config get() {
		if (theConfig == null)
			theConfig = new Config();

		return theConfig;
	}

	public static String property(String name, String value) {
		return get().setProperty(name, value);
	}

	public static String property(String name) {
		return get().getProperty(name);
	}

	private Properties props;

	private Config() {
		props = defaultProps();

		try {
			ResourceBundle b = ResourceBundle.getBundle("BioBlitzImages");

			for (String key : b.keySet())
				props.setProperty(key, b.getString(key));
		} catch (MissingResourceException e) {
			// e.printStackTrace();
		}
	}

	public Properties getProperties() {
		return props;
	}

	public String getProperty(String name) {
		return props.getProperty(name);
	}

	public Set<String> getPropertyNames() {
		return props.stringPropertyNames();
	}

	public String setProperty(String name, String value) {
		return (String) props.setProperty(name, value);
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();

		buf.append("Config [\n");

		for (String name : getPropertyNames())
			buf.append(String.format("  %s = %s\n", name, getProperty(name)));

		buf.append("]");
		return buf.toString();
	}
}
