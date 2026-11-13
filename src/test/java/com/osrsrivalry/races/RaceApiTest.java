package com.osrsrivalry.races;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

public class RaceApiTest
{
	private static final String ID = "12345678-1234-4abc-8def-123456789abc";
	private static final String RULES = "\"id\":\"" + ID + "\",\"host\":\"Rival One\",\"guest\":\"Rival Two\","
		+ "\"skill\":\"fishing\",\"targetXP\":100000,\"days\":3,";
	private final RaceApi api = new RaceApi(new OkHttpClient(), new Gson());

	@Test
	public void decodesTheWorkersScoreAndTiming()
	{
		RaceApi.Result result = api.readResponse(ID, response(200, "{" + RULES + "\"status\":\"active\","
			+ "\"endsAt\":1800000000000,\"score\":{\"host\":73000,\"guest\":41000,\"observedAt\":1799999000000}}"));
		assertEquals(RaceSnapshot.Status.ACTIVE, result.race.status);
		assertEquals("Fishing", result.race.skillName());
		assertEquals(73_000, result.race.score.host.intValue());
		assertEquals(41_000, result.race.score.guest.intValue());
		assertEquals(1_799_999_000_000L, result.race.score.observedAt.longValue());
		assertFalse(result.terminal);
	}

	@Test
	public void keepsWaitingOpenAndStopsOnTerminalRaceStates()
	{
		RaceApi.Result waiting = api.readResponse(ID, response(200, "{" + RULES + "\"status\":\"waiting\"}"));
		assertEquals(RaceSnapshot.Status.WAITING, waiting.race.status);
		assertFalse(waiting.terminal);
		RaceApi.Result finished = api.readResponse(ID, response(200, "{" + RULES + "\"status\":\"finished\",\"winner\":null}"));
		assertEquals(RaceSnapshot.Status.FINISHED, finished.race.status);
		assertNull(finished.race.score);
		assertTrue(finished.terminal);
		RaceApi.Result expired = api.readResponse(ID, response(200, "{" + RULES + "\"status\":\"expired\"}"));
		assertEquals(RaceSnapshot.Status.EXPIRED, expired.race.status);
		assertTrue(expired.terminal);
	}

	@Test
	public void distinguishesMissingRacesFromTemporaryFailures()
	{
		assertTrue(api.readResponse(ID, response(404, "{}")).terminal);
		assertTrue(api.readResponse(ID, response(410, "{}")).terminal);
		assertFalse(api.readResponse(ID, response(429, "{}")).terminal);
		assertFalse(api.readResponse(ID, response(503, "{}")).terminal);
		assertFalse(api.readResponse(ID, response(200, "not JSON")).terminal);
		assertNull(api.readResponse("12345678-1234-4abc-8def-123456789abd",
			response(200, "{" + RULES + "\"status\":\"waiting\"}")).race);
	}

	private static Response response(int code, String json)
	{
		return new Response.Builder()
			.request(new Request.Builder().url(RaceLink.apiUrl(ID)).build())
			.protocol(Protocol.HTTP_1_1)
			.code(code)
			.message("Fixture")
			.body(ResponseBody.create(MediaType.get("application/json"), json))
			.build();
	}
}
