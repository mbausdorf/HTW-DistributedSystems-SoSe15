package de.htw.ds.edge;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import de.sb.java.TypeMetadata;
import de.sb.java.net.SocketAddress;


/**
 * This facade provides common services for HTTP redirect handlers.
 */
@TypeMetadata(copyright = "2014-2015 Sascha Baumeister, all rights reserved", version = "0.3.0", authors = "Sascha Baumeister")
final class HttpRedirectHandlers {
	static private final InetSocketAddress[] NODE_ADDRESSES = new InetSocketAddress[48];
	static {
		final Properties properties = new Properties();
		try (InputStream byteSource = HttpRedirectHandlers.class.getResourceAsStream("edgetube.properties")) {
			properties.load(byteSource);

			for (final Map.Entry<Object,Object> entry : properties.entrySet()) {
				final float timezoneOffset = Float.parseFloat(entry.getKey().toString());
				final InetSocketAddress socketAddress = new SocketAddress(entry.getValue().toString()).toInetSocketAddress();
				final int index = offsetToIndex(timezoneOffset);
				NODE_ADDRESSES[index] = socketAddress;
			}
		} catch (final IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}


	/**
	 * Returns a node address corresponding to the given timezone offset. The node addresses are
	 * configured in file "node-addresses.properties".
	 * @param timezoneOffset a timezone offset in hours
	 * @return a node address
	 */
	static public InetSocketAddress nodeAddress (final float timezoneOffset) {
		return NODE_ADDRESSES[offsetToIndex(timezoneOffset)];
	}


	/**
	 * Returns an index corresponding to the given timezone offset. The calculation assumes that
	 * time zones are allocated in half hour slots, therefore it generated indices within range
	 * [0,47].
	 * @param timezoneOffset a timezone offset in hours
	 * @return an index within range [0, 47]
	 */
	static private int offsetToIndex (float timezoneOffset) {
		while (timezoneOffset < -12.0f)
			timezoneOffset += 24.0f;
		while (timezoneOffset >= 12.0f)
			timezoneOffset -= 24.0f;
		return Math.round(2.0f * (timezoneOffset + 12.0f));
	}


	/**
	 * Parses the parameters contained within the given URI query, and returns them as a map.
	 * @param uriQuery the URI query, or {@code null}
	 * @return the URI query parameters
	 */
	static public Map<String,String> parseQueryParameters (final String uriQuery) {
		final Map<String,String> result = new HashMap<String,String>();
		if (uriQuery == null) return result;

		for (final String association : uriQuery.split("&")) {
			final int offset = association.indexOf('=');
			final String key = association.substring(0, offset);
			final String value = association.substring(offset + 1);
			result.put(key, value);
		}

		return result;
	}


	/**
	 * Prevents instantiation.
	 */
	private HttpRedirectHandlers () {}
}