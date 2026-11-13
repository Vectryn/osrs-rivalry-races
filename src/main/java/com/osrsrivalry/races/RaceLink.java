package com.osrsrivalry.races;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.HttpUrl;

final class RaceLink
{
	private static final String WATCH_HOST = "osrsrivalry.com";
	private static final HttpUrl WATCH_ORIGIN = HttpUrl.get("https://osrsrivalry.com/");
	private static final HttpUrl API_ORIGIN = HttpUrl.get("https://races.osrsrivalry.com/");
	private static final String UUID_V4 = "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
	private static final Pattern RACE_ID = Pattern.compile(UUID_V4);
	private static final Pattern WATCH_PATH = Pattern.compile("/race/([^/]+)/(" + UUID_V4 + ")/?", Pattern.CASE_INSENSITIVE);
	private static final Pattern PLAYER_NAME = Pattern.compile("(?=.*[A-Za-z0-9])[A-Za-z0-9 _-]{1,12}");
	private static final String BAD_LINK = "Paste an OSRS Rivalry race watch link.";

	final String id;
	final String watchUrl;

	private RaceLink(String host, String id)
	{
		this.id = id;
		watchUrl = watchUrl(host, id);
	}

	static RaceLink parse(String text)
	{
		URI uri;
		try
		{
			uri = new URI(text.trim());
		}
		catch (URISyntaxException ex)
		{
			throw new IllegalArgumentException(BAD_LINK);
		}

		if (!"https".equalsIgnoreCase(uri.getScheme()) || !WATCH_HOST.equalsIgnoreCase(uri.getRawAuthority())
			|| uri.getRawQuery() != null)
		{
			throw new IllegalArgumentException(BAD_LINK);
		}
		if (uri.getFragment() != null)
		{
			throw new IllegalArgumentException("Open the invite in a browser and use Copy watch link.");
		}

		Matcher path = WATCH_PATH.matcher(uri.getPath());
		if (!path.matches() || !WATCH_PATH.matcher(uri.getRawPath()).matches() || !isPlayerName(path.group(1)))
		{
			throw new IllegalArgumentException(BAD_LINK);
		}
		return new RaceLink(path.group(1), path.group(2).toLowerCase(Locale.ROOT));
	}

	static boolean isId(String id)
	{
		return id != null && RACE_ID.matcher(id).matches();
	}

	static boolean isPlayerName(String name)
	{
		return name != null && PLAYER_NAME.matcher(name).matches();
	}

	static String watchUrl(String host, String id)
	{
		return WATCH_ORIGIN.newBuilder().addPathSegment("race").addPathSegment(host).addPathSegment(id).build().toString();
	}

	static HttpUrl apiUrl(String id)
	{
		if (!isId(id))
		{
			throw new IllegalArgumentException("Invalid race ID");
		}
		return API_ORIGIN.newBuilder().addPathSegment("races").addPathSegment(id).build();
	}
}
