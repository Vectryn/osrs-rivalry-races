package com.osrsrivalry.races;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javax.inject.Inject;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

final class RaceApi
{
	private static final String UNAVAILABLE = "Couldn’t reach the race service. Try again.";
	private final OkHttpClient http;
	private final Gson gson;

	@Inject
	RaceApi(OkHttpClient http, Gson gson)
	{
		this.http = http.newBuilder()
			.followRedirects(false)
			.followSslRedirects(false)
			.callTimeout(15, TimeUnit.SECONDS)
			.build();
		this.gson = gson;
	}

	Call read(String id, BiConsumer<Call, Result> done)
	{
		Request request = new Request.Builder()
			.url(RaceLink.apiUrl(id))
			.header("Accept", "application/json")
			.get()
			.build();
		Call call = http.newCall(request);
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call completed, IOException ex)
			{
				done.accept(completed, new Result(null, UNAVAILABLE, false));
			}

			@Override
			public void onResponse(Call completed, Response response)
			{
				done.accept(completed, readResponse(id, response));
			}
		});
		return call;
	}

	Result readResponse(String id, Response response)
	{
		try (ResponseBody body = response.body())
		{
			if (response.code() == 404 || response.code() == 410)
			{
				return new Result(null, "Race unavailable. Check the watch link.", true);
			}
			if (response.code() == 429)
			{
				return new Result(null, "Too many requests. Try again in a minute.", false);
			}
			if (!response.isSuccessful() || body == null)
			{
				return new Result(null, UNAVAILABLE, false);
			}

			RaceSnapshot race = gson.fromJson(body.charStream(), RaceSnapshot.class);
			return race != null && race.isValid(id)
				? new Result(race, null, !race.isOngoing())
				: new Result(null, "Couldn’t load these standings. Try again.", false);
		}
		catch (JsonParseException ex)
		{
			return new Result(null, "Couldn’t load these standings. Try again.", false);
		}
	}

	static final class Result
	{
		final RaceSnapshot race;
		final String message;
		final boolean terminal;

		Result(RaceSnapshot race, String message, boolean terminal)
		{
			this.race = race;
			this.message = message;
			this.terminal = terminal;
		}
	}
}
