# OSRS Rivalry Races

Watch a shared OSRS Rivalry skill race in the RuneLite sidebar. On the race page, choose **Copy watch link**, paste it into the sidebar, and select **Watch**. The panel shows the target, XP recorded for both players, the lead, time left, and the most recent hiscores check. **Open** opens the race page in a browser. **Clear** removes the race from the sidebar and from this device.

The panel checks for updated standings once a minute while selected, until the race ends. The OSRS Rivalry server records public Jagex hiscores readings, which can lag behind gameplay. A request sends the race ID to `races.osrsrivalry.com`, and that service receives the requesting IP address. The plugin stores only the selected race ID in RuneLite’s local plugin data. Private invite links are accepted on the website and should stay between the two players.

## Build and test

With JDK 17 installed, run `./gradlew build` from this directory, or `gradlew.bat build` on Windows. The build targets Java 11. Use `./gradlew run` to open the RuneLite development client. Open the OSRS Rivalry Races sidebar, paste a watch link, and check the same standings on the website. A game login is optional for the sidebar.

## Plugin Hub submission

Use this directory as the root of the public plugin repository. Follow the [Plugin Hub submission instructions](https://github.com/runelite/plugin-hub#submitting-a-plugin) for `osrs-rivalry-races`. Include this disclosure in the Hub manifest with the repository and commit fields.

```properties
warning=Watching a race submits the race ID and your IP address to the OSRS Rivalry server, which is not controlled or verified by RuneLite developers.
```
