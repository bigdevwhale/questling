# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

## [1.2.0] - 2026-11-18

### Added
- **Covers and pictures are in the backup:** the backup is now a zip with the journal, the games' covers and the sessions' screenshots and photos, so a restore brings the pictures back too. Older backups (a bare JSON file) still load; the pictures on the phone stay as they are for those.
- **Moving to a new phone through Lingvale:** Lingvale can now gather the backups of all our apps into one file and load them back on another phone. Questling hands it the same backup file the settings save and takes one back the same way. Only Lingvale signed with our key is answered, loading happens only after you confirm it in Lingvale, and nothing leaves the phone.

<!-- tg
В резервную копию теперь попадают обложки игр и снимки сессий. Старые копии по-прежнему читаются.
Переезд на новый телефон через Lingvale: деревня собирает копии всех наших приложений в один файл и возвращает их на новом телефоне.
-->

## [1.1.0] - 2026-11-16

### Added
- **Lingvale, the village of the pets from all our apps:** Questling now has a small read-only bridge, so in Lingvale you can build the tavern and its residents move in with their days. The bridge answers only Lingvale signed with our key; it changes nothing and nothing leaves the phone.

<!-- tg
Питомцы Questling теперь могут жить в Lingvale – общей деревне наших приложений.
В Lingvale для них можно построить таверну, и жильцы переедут туда сами вместе со своими днями.
Данные остаются в Questling: деревня их только читает.
-->

## [1.0.1] - 2026-11-15

- Phone games are found again: on Android 11 and newer the tracker could not see other apps, so no game was ever recognised.
- A session played while Questling stayed in the background is found when you come back to the app, not only after a restart (#1).
- A game still on screen when the app looks is offered later as one whole session instead of being cut in two or lost; going back to a game after a look counts as a new session.

<!-- tg
Questling 1.0.1: игры на телефоне снова находятся сами.
Раньше на Android 11 и новее трекер не видел игры, а сессия, сыгранная при свёрнутом Questling, терялась до перезапуска приложения. Теперь находится каждая сессия, как только вы возвращаетесь в Questling.
-->

## [1.0.0] - 2026-11-14

The first release of Questling.

- A diary of everything you play – video games, board games, card games, tabletop RPGs, phone games – set in a tavern at the crossroads of worlds.
- Sessions with a running clock or written down afterwards: the result, everybody's score, a moment (a boss, a chapter, the credits, 100%), the mood, a note, pictures.
- Games with a catalogue lookup (Wikidata and Wikipedia) for the cover, year, maker and genres; status, a rating out of ten, notes on where you stopped.
- Screenshots made during a session are offered when it ends; photos from the gallery or the phone's own camera; an album.
- Phone game sessions found through usage access, each confirmed with one tap.
- Table tools: dice from d4 to d100, «who goes first», a turn timer, a score sheet.
- Players: hours together and the score between you.
- 36 creatures drawn as pixel art – out of game worlds and ordinary animals – found by the way you play; six stats, a class and a favourite genre each.
- Nearly 300 trophies in four metals, as ladders and secret ones; three weekly quests; a quarterly season of thirty levels; a shop for the tavern. Nothing is sold for money.
- The summary: hours, a calendar, games, genres, platforms, wins and losses, habits, the year in games; nine kinds of share cards.
- The pet yawns after a long stretch without a break; breaks are rewarded, and long days feed the creatures less.

<!-- tg
Questling – дневник всего, во что вы играете: видеоигры, настолки, карточные игры, ролёвки и игры на телефоне.
Запускаете таймер или записываете сессию потом. Приложение запомнит, чем всё закончилось, кто победил и на чём вы остановились, а скриншоты сессии подхватит само.
Пока вы играете, в трактире растут питомцы. Их 36, и каждый приходит за то, как именно вы играете. А ещё здесь почти 300 трофеев, задания недели и сезон.
-->
