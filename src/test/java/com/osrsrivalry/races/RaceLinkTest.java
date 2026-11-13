package com.osrsrivalry.races;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Locale;
import org.junit.Test;

public class RaceLinkTest
{
	private static final String ID = "12345678-1234-4abc-8def-123456789abc";
	private static final String WATCH_URL = "https://osrsrivalry.com/race/Rival%20One/" + ID;

	@Test
	public void recognizesCanonicalLinkAndUsesOnlyIdForApiRequest()
	{
		RaceLink link = RaceLink.parse("  https://osrsrivalry.com/race/Rival%20One/" + ID.toUpperCase(Locale.ROOT) + "/  ");
		assertEquals(ID, link.id);
		assertEquals(WATCH_URL, link.watchUrl);
		assertEquals("https://races.osrsrivalry.com/races/" + ID, RaceLink.apiUrl(link.id).toString());
	}

	@Test
	public void directsPrivateInviteHolderToCopyWatchLink()
	{
		IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
			RaceLink.parse(WATCH_URL + "#join=11111111-1111-4111-8111-111111111111"));
		assertEquals("Open the invite in a browser and use Copy watch link.", error.getMessage());
	}

	@Test
	public void rejectsOtherOriginsAndMalformedPaths()
	{
		String[] invalid = {
			"http://osrsrivalry.com/race/Rival/" + ID,
			"https://osrsrivalry.com.attacker.example/race/Rival/" + ID,
			"https://attacker.example@osrsrivalry.com/race/Rival/" + ID,
			"https://osrsrivalry.com:444/race/Rival/" + ID,
			"https://osrsrivalry.com/race/Rival%2fOne/" + ID,
			"https://osrsrivalry.com/race/Rival/%31" + ID.substring(1),
			"https://osrsrivalry.com/race/Rival/12345678-1234-1abc-8def-123456789abc",
			WATCH_URL + "?join=private",
			WATCH_URL + "/extra",
		};
		for (String link : invalid)
		{
			assertThrows(link, IllegalArgumentException.class, () -> RaceLink.parse(link));
		}
		assertThrows(IllegalArgumentException.class, () -> RaceLink.apiUrl("../other"));
	}
}
