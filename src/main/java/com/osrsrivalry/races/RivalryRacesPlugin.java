package com.osrsrivalry.races;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "OSRS Rivalry Races",
	internalName = "osrs-rivalry-races",
	description = "Watch shared OSRS Rivalry skill races while you play.",
	tags = {"rivalry", "competition", "races", "hiscores", "xp"}
)
public class RivalryRacesPlugin extends Plugin
{
	private static final Logger LOG = LoggerFactory.getLogger(RivalryRacesPlugin.class);
	private static final String WATCH_FILE = "watched-race.txt";

	@Inject
	private ClientToolbar toolbar;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private RaceApi api;

	private RacePanel panel;
	private NavigationButton button;

	@Override
	protected void startUp()
	{
		SwingUtilities.invokeLater(() -> {
			panel = new RacePanel(api, this::saveRace);
			BufferedImage icon = ImageUtil.loadImageResource(RivalryRacesPlugin.class, "crest.png");
			button = NavigationButton.builder().tooltip("OSRS Rivalry Races").icon(icon).panel(panel).priority(8).build();
			toolbar.addNavigation(button);
			RacePanel target = panel;
			executor.execute(() -> {
				String saved = readRace();
				SwingUtilities.invokeLater(() -> target.restore(saved));
			});
		});
	}

	@Override
	protected void shutDown()
	{
		SwingUtilities.invokeLater(() -> {
			if (panel != null)
			{
				panel.dispose();
				toolbar.removeNavigation(button);
				panel = null;
				button = null;
			}
		});
	}

	private String readRace()
	{
		try
		{
			Filepath file = getPluginDirectory().joinSegment(WATCH_FILE);
			if (file.isFile() && file.size() == 36)
			{
				try (BufferedReader reader = file.openBufferedReader())
				{
					String id = reader.readLine();
					return RaceLink.isId(id) ? id : null;
				}
			}
		}
		catch (IOException ex)
		{
			LOG.debug("Could not read the saved race selection", ex);
		}
		return null;
	}

	private void saveRace(String id)
	{
		executor.execute(() -> {
			try
			{
				Filepath directory = getPluginDirectory();
				Filepath file = directory.joinSegment(WATCH_FILE);
				if (id == null)
				{
					file.deleteIfExists();
				}
				else
				{
					directory.createDirectories();
					file.write(id);
				}
			}
			catch (IOException ex)
			{
				LOG.debug("Could not update the saved race selection", ex);
			}
		});
	}
}
