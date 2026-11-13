package com.osrsrivalry.races;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;
import okhttp3.Call;

final class RacePanel extends PluginPanel
{
	private static final String PROMPT = "Paste the watch link from your shared race.";
	private static final Color GOLD = new Color(0xD9B76D);
	private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
		.withZone(ZoneId.systemDefault());

	private final RaceApi api;
	private final Consumer<String> save;
	private final NumberFormat number = NumberFormat.getIntegerInstance();
	private final Timer poll = new Timer(60_000, event -> fetch());
	private final JTextField input = new JTextField();
	private final JButton open = new JButton("Open");
	private final JButton refresh = new JButton("Refresh");
	private final JButton clear = new JButton("Clear");
	private final JLabel title = new JLabel("Shared skill race");
	private final JLabel message = new JLabel();
	private final JLabel rules = new JLabel();
	private final JLabel timing = new JLabel();
	private final JLabel observed = new JLabel();
	private final ScoreRow host = new ScoreRow(GOLD);
	private final ScoreRow guest = new ScoreRow(ColorScheme.LIGHT_GRAY_COLOR);
	private String raceId;
	private String watchUrl;
	private RaceSnapshot latest;
	private Call pending;
	private boolean selected;
	private boolean terminal;
	private boolean userChangedSelection;
	private boolean disposed;

	RacePanel(RaceApi api, Consumer<String> save)
	{
		this.api = api;
		this.save = save;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		poll.setRepeats(false);

		JLabel label = new JLabel("Race watch link");
		label.setLabelFor(input);
		JButton watch = new JButton("Watch");
		JPanel link = new JPanel(new BorderLayout(4, 0));
		link.setOpaque(false);
		link.add(input, BorderLayout.CENTER);
		link.add(watch, BorderLayout.EAST);
		addRow(label);
		addRow(link);

		JPanel actions = new JPanel(new GridLayout(1, 3, 3, 0));
		actions.setOpaque(false);
		open.setMargin(new Insets(3, 4, 3, 4));
		open.setToolTipText("Open race in your browser");
		open.getAccessibleContext().setAccessibleName("Open race in your browser");
		refresh.setMargin(new Insets(3, 4, 3, 4));
		clear.setMargin(new Insets(3, 4, 3, 4));
		actions.add(open);
		actions.add(refresh);
		actions.add(clear);
		addRow(actions);

		title.setForeground(GOLD);
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		addRow(title);
		addRow(message);
		addRow(rules);
		addRow(host);
		addRow(guest);
		addRow(timing);
		addRow(observed);

		JLabel disclosure = new JLabel(wrapped("Watching sends the race ID and your IP address to OSRS Rivalry."));
		disclosure.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		addRow(disclosure);

		watch.addActionListener(event -> watch());
		input.addActionListener(event -> watch());
		refresh.addActionListener(event -> fetch());
		open.addActionListener(event -> LinkBrowser.browse(watchUrl));
		clear.addActionListener(event -> clear());
		showPrompt(PROMPT);
	}

	private void addRow(JComponent component)
	{
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		component.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
		add(component);
	}

	void restore(String id)
	{
		if (disposed || userChangedSelection || id == null)
		{
			return;
		}
		raceId = id;
		showPrompt("Opening saved race…");
		if (selected)
		{
			fetch();
		}
	}

	@Override
	public void onActivate()
	{
		selected = true;
		if (!terminal)
		{
			fetch();
		}
	}

	@Override
	public void onDeactivate()
	{
		selected = false;
		poll.stop();
	}

	void dispose()
	{
		disposed = true;
		onDeactivate();
		cancelRequest();
	}

	private void watch()
	{
		RaceLink link;
		try
		{
			link = RaceLink.parse(input.getText());
		}
		catch (IllegalArgumentException ex)
		{
			if (input.getText().contains("#"))
			{
				input.setText(watchUrl);
			}
			message.setText(wrapped(ex.getMessage()));
			return;
		}
		cancelRequest();
		userChangedSelection = true;
		raceId = link.id;
		watchUrl = link.watchUrl;
		latest = null;
		terminal = false;
		input.setText(watchUrl);
		save.accept(raceId);
		showPrompt("Loading race…");
		fetch();
	}

	private void clear()
	{
		cancelRequest();
		poll.stop();
		userChangedSelection = true;
		raceId = null;
		watchUrl = null;
		latest = null;
		terminal = false;
		input.setText("");
		save.accept(null);
		showPrompt(PROMPT);
		input.requestFocusInWindow();
	}

	private void cancelRequest()
	{
		if (pending != null)
		{
			pending.cancel();
			pending = null;
		}
	}

	private void fetch()
	{
		if (disposed || raceId == null || pending != null)
		{
			return;
		}
		poll.stop();
		pending = api.read(raceId, (call, result) -> SwingUtilities.invokeLater(() -> showResult(call, result)));
		updateButtons();
	}

	private void showResult(Call call, RaceApi.Result result)
	{
		if (disposed || pending != call)
		{
			return;
		}
		pending = null;
		terminal = result.terminal || (result.race == null && terminal);
		if (result.race != null)
		{
			latest = result.race;
			boolean useCanonicalLink = input.getText().isEmpty() || input.getText().equals(watchUrl);
			watchUrl = RaceLink.watchUrl(latest.host, latest.id);
			if (useCanonicalLink)
			{
				input.setText(watchUrl);
			}
			renderRace(null);
		}
		else if (latest != null && !result.terminal)
		{
			renderRace("Showing last available standings. " + result.message);
		}
		else
		{
			latest = null;
			showPrompt(result.message);
		}
		if (selected && !terminal)
		{
			poll.restart();
		}
	}

	private void renderRace(String error)
	{
		RaceSnapshot race = latest;
		title.setText(race.skillName() + " race");
		message.setText(wrapped(error == null ? statusText(race) : statusText(race) + "<br><br>" + error));
		rules.setText("Target: " + number.format(race.targetXP) + " XP");
		rules.setVisible(true);
		boolean hasScore = race.score != null;
		host.setVisible(hasScore);
		guest.setVisible(hasScore);
		observed.setVisible(hasScore);
		if (hasScore)
		{
			host.update(race.host, race.score.host, race.targetXP, number);
			guest.update(race.guest, race.score.guest, race.targetXP, number);
			observed.setText(wrapped("Last hiscores check: " + LOCAL_TIME.format(Instant.ofEpochMilli(race.score.observedAt))));
		}
		timing.setVisible(race.isOngoing());
		if (race.status == RaceSnapshot.Status.ACTIVE)
		{
			timing.setText(timeLeft(race.endsAt));
		}
		else if (race.status == RaceSnapshot.Status.WAITING)
		{
			timing.setText(wrapped((race.days == 1 ? "24 hours" : race.days + " days") + " from acceptance"));
		}
		updateButtons();
	}

	private String statusText(RaceSnapshot race)
	{
		switch (race.status)
		{
			case WAITING:
				return race.host + " vs " + race.guest + "<br>Waiting for " + race.guest + " to accept.";
			case ACTIVE:
				int gap = race.score.host - race.score.guest;
				if (gap == 0)
				{
					return "Tied";
				}
				return (gap > 0 ? race.host : race.guest) + " leads by " + number.format(Math.abs(gap)) + " XP";
			case EXPIRED:
				return "This race invite has expired.";
			case FINISHED:
				if (race.winner == null)
				{
					return "Result unavailable. No hiscores reading after the race started.";
				}
				if (race.winner == RaceSnapshot.Winner.TIE)
				{
					return "Race finished in a tie.";
				}
				return (race.winner == RaceSnapshot.Winner.HOST ? race.host : race.guest) + " wins!";
			default:
				throw new IllegalStateException("Unrecognized race status");
		}
	}

	private static String timeLeft(long deadline)
	{
		long remaining = deadline - System.currentTimeMillis();
		if (remaining <= 0)
		{
			return "Deadline reached";
		}
		long minutes = remaining / 60_000 + (remaining % 60_000 == 0 ? 0 : 1);
		long days = minutes / 1_440;
		long hours = minutes % 1_440 / 60;
		return "Time left: " + (days > 0 ? days + "d " : "") + (hours > 0 ? hours + "h " : "") + minutes % 60 + "m";
	}

	private void showPrompt(String text)
	{
		title.setText("Shared skill race");
		message.setText(wrapped(text));
		rules.setVisible(false);
		host.setVisible(false);
		guest.setVisible(false);
		timing.setVisible(false);
		observed.setVisible(false);
		updateButtons();
	}

	private void updateButtons()
	{
		open.setEnabled(watchUrl != null);
		refresh.setEnabled(raceId != null && pending == null);
		clear.setEnabled(raceId != null);
	}

	private static String wrapped(String text)
	{
		return "<html><body style='width:145pt'>" + text + "</body></html>";
	}

	private static final class ScoreRow extends JPanel
	{
		private final JLabel name = new JLabel();
		private final JLabel xp = new JLabel();
		private final JProgressBar bar = new JProgressBar();

		ScoreRow(Color color)
		{
			setLayout(new BorderLayout(0, 3));
			setOpaque(false);
			JPanel names = new JPanel(new BorderLayout(4, 0));
			names.setOpaque(false);
			names.add(name, BorderLayout.CENTER);
			names.add(xp, BorderLayout.EAST);
			add(names, BorderLayout.NORTH);
			bar.setForeground(color);
			bar.setPreferredSize(new Dimension(0, 6));
			add(bar, BorderLayout.SOUTH);
		}

		void update(String player, int gain, int target, NumberFormat number)
		{
			name.setText(player);
			xp.setText(number.format(gain) + " XP");
			bar.setMaximum(target);
			bar.setValue(Math.min(gain, target));
			bar.getAccessibleContext().setAccessibleName(player + " race progress");
		}
	}
}
