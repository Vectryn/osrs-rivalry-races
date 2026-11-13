package com.osrsrivalry.races;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class RivalryRacesDevClient
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(RivalryRacesPlugin.class);
		RuneLite.main(args);
	}
}
