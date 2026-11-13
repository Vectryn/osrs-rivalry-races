package com.osrsrivalry.races;

import com.google.gson.annotations.SerializedName;
import java.util.Locale;

final class RaceSnapshot
{
	enum Status
	{
		@SerializedName("waiting") WAITING,
		@SerializedName("active") ACTIVE,
		@SerializedName("finished") FINISHED,
		@SerializedName("expired") EXPIRED
	}

	enum Winner
	{
		@SerializedName("host") HOST,
		@SerializedName("guest") GUEST,
		@SerializedName("tie") TIE
	}

	static final class Score
	{
		Integer host;
		Integer guest;
		Long observedAt;

		boolean isValid()
		{
			return host != null && host >= 0 && host <= XP_CAP
				&& guest != null && guest >= 0 && guest <= XP_CAP && observedAt != null && observedAt > 0;
		}
	}

	private static final int XP_CAP = 200_000_000;
	String id;
	String host;
	String guest;
	String skill;
	Integer targetXP;
	Integer days;
	Long endsAt;
	Status status;
	Winner winner;
	Score score;

	boolean isValid(String requestedId)
	{
		if (!requestedId.equals(id) || !RaceLink.isPlayerName(host) || !RaceLink.isPlayerName(guest)
			|| skill == null || !skill.matches("[a-z]{1,24}") || targetXP == null || targetXP < 1 || targetXP > XP_CAP
			|| days == null || days < 1 || days > 30 || status == null || (score != null && !score.isValid()))
		{
			return false;
		}
		return status != Status.ACTIVE || (score != null && endsAt != null && endsAt > 0);
	}

	boolean isOngoing()
	{
		return status == Status.WAITING || status == Status.ACTIVE;
	}

	String skillName()
	{
		return skill.substring(0, 1).toUpperCase(Locale.ROOT) + skill.substring(1);
	}
}
