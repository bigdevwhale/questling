# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`, пост о выходе — в `post/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow уже кладёт в GitHub Release `Questling-v<версия>.aab` рядом с
  `Questling.apk`. Для первой загрузки — **`Questling-v1.2.1.aab`** из v1.2.1 (подписан релизным ключом; в ней
  погодный запрос называет себя `Questling/1.0`, а не чужим `Pumpling/1.0` — MET Norway этого требует).
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках перестаёт работать. Не удаляйте этот блок.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.questling`.
  2. **Select key** → ключ с SHA-256 `e92aa23aae9a716c9f0cadce579cc80f2efb949e424496b6439d6304ddc2e026`
     (сертификат CN=Questling, O=StillCraft — им подписаны все релизы, проверено по `Questling.apk` из v1.2.0).
  3. **Upload APK** → скопировать сниппет и запустить workflow **Package registration proof**
     (Actions → Run workflow → вставить сниппет). Он соберёт релизный APK с файлом
     `assets/adi-registration.properties`, подпишет тем же ключом и проверит отпечаток; APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Тогда APK из GitHub и версия из Play
  обновляют друг друга. Если оставить «Google создаст ключ подписи», они друг на друга не встанут.
  Это важно и для Lingvale: мост проверяет подпись, а Lingvale сверяет подпись Questling.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд. Если аккаунт уже
  прошёл это требование с другими приложениями Stillcraft, можно сразу в production.
- **Политика конфиденциальности** — `PRIVACY.md` в корне (на двух языках). После push ссылка:
  `https://github.com/bigdevwhale/questling/blob/main/PRIVACY.md`
- **Автозагрузка из CI** (`.github/workflows/play.yml`) — после каждого успешного Release берёт AAB из
  релиза и отправляет в Play. «What's new»: en-US — из текста релиза (раздел версии в `CHANGELOG.md`),
  ru-RU — из скрытого блока `<!-- tg ... -->` того же раздела, обрезается до 500 символов. Если такой релиз
  на треке уже есть, обновляются только заметки. До первой ручной загрузки workflow не нужен, поэтому:
  1. В репозитории Settings → Environments → создать **`google-play`** с Required reviewer (вы) —
     иначе job запустится без подтверждения сразу после релиза.
  2. Play Console → Users and permissions → пригласить `play-publisher@intense-context-514611-h3.iam.gserviceaccount.com`
     с правом выпускать релизы для Questling.
  3. В Google Cloud у пула `github` (Workload Identity Federation) разрешить репозиторию
     `bigdevwhale/questling` действовать от имени этого сервисного аккаунта — как для остальных приложений.
  Пока это не сделано, отклоняйте запуск «Google Play» после релиза (или он упадёт на авторизации — без вреда).
  По умолчанию workflow выкладывает в **production**; пока идёт закрытый тест, запускайте его вручную
  с треком `alpha` (closed testing).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Questling: Game Diary & Pets` |
| Default language | English (United States) – en-US |
| App or game | **App** |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

**App, а не Game.** Questling — дневник игр с питомцами как оформлением, а не игра: так же, как Reelkin
для фильмов. В разделе Games он соседствовал бы с самими играми и проигрывал бы им.

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…6.png` | `graphics/screenshots-ru/1…6.png` |
| Tablet screenshots | можно не загружать (только телефон, портрет) | — |
| Video | пусто | — |

Длины: названия 28 и 22, короткие описания 78 и 77, полные около 3000 символов.

Скриншоты 1080×1920 собирает `tools/play.py` из снимков README (`docs/screenshots/<язык>/*.png`, 1080×2400,
демо-сборка `-Pdemo -Pshots`, `tools/capture.sh` на 13 Pro): статус-бар закрашивается чистым (9:41), Play не берёт
картинки длиннее двух ширин, поэтому экран уходит за нижний край под подписью. Подписи — Lora, шрифт приложения,
кремом и золотом на тёмном фоне трактира с искрами очага. Порядок: трактир, журнал, питомцы, трофеи, коллекция,
карточка игры. Баннер и пост — сам трактир с ночным небом в окне и четырьмя питомцами на ковре. Иконка — d20 на
тёмно-красном фоне (`icon_bg`), квадрат без скругления: Play скругляет сам. Как пересобрать:

```bash
tools/capture.sh en && tools/capture.sh ru   # только если поменялись экраны
python tools/play.py                         # графика для Play (нужен pip install pillow)
```

**Обложки на скриншотах.** В демо — настоящие игры (Catan, The Witcher 3, Civilization VI, Stardew Valley…) с
обложками из Википедии. Обычно Play такое пропускает: это журнал пользователя, а не реклама чужих игр (так
прошёл Songling с обложками альбомов). Если ревью придерётся («Impersonation / Intellectual property»),
переснимите демо с вымышленными названиями без обложек (`DemoSeed.kt`) и пересоберите графику. В текстах
карточки чужие названия намеренно не упоминаются.

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | App → **Entertainment** |
| Tags (до 5) | из списка Play ближайшие: Entertainment, Board games, Video games, Notebook, Collection management (если какого-то нет — Productivity / Lifestyle) |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/questling/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

**Категория.** Трекеры игр и коллекций (как и дневники фильмов) живут в **Entertainment**: там их ищут
по «game tracker», «backlog», «board game log». Категорию можно сменить позже.

**Имя разработчика** (Settings → Developer account) — **Stillcraft**, одинаковое для всех приложений:
тогда Play сам покажет под карточкой «Другие приложения этого разработчика».

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*
(Аккаунтов нет. Две необязательные функции — подхват скриншотов и игры на телефоне — включаются системными
разрешениями, но не входом.)

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types** (это не игра).
- Насилия, страха, сексуального контента, ненормативной лексики, наркотиков — **No**. Обложки игр
  пользователь добавляет сам, приложение их не продвигает.
- **Gambling / simulated gambling** — **No**: кубики — инструмент для настольной игры, ставок нет; золото
  только зарабатывается сессиями и тратится на мебель для трактира, за деньги его не купить.
- **Users interact / share content** — **No**: общения между пользователями нет; карточки уходят через
  системное «Поделиться».
- **Shares location** — **No**: координаты города уходят только в погодный сервис, другим людям — нет.
- **Digital purchases** — No. **Web browser / unrestricted internet** — No.
- Ожидаемый результат: Everyone / PEGI 3 / 0+.

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over**. Младше 13 не отмечать: иначе действует политика Families с
  отдельными требованиями.
- *Could your store listing unintentionally appeal to children?* → **No**: это дневник игр со статистикой,
  счётом и игроками; пиксельные питомцы — оформление дневника. Если Google не согласится, ответ меняется на
  Yes без последствий для 13+.

**News apps** — No. **COVID-19** — My app is not a publicly available COVID-19 app.
**Government apps** — No. **Financial features** — *My app doesn't provide any financial features.*
**Health apps** — ничего не отмечать (перерывы и «пора размяться» — забота питомца, а не функция здоровья).

**Data safety**

Нам ничего не приходит, но Play считает «сбором» любую передачу с телефона, в том числе в чужие
сервисы. Честный минимум:

- *Does your app collect or share any of the required user data types?* → **Yes**.
- *Is all of the user data collected by your app encrypted in transit?* → **Yes** (всё по HTTPS).
- *Do you provide a way for users to request that their data is deleted?* → **No** (аккаунтов нет,
  на серверах ничего не хранится; данные удаляются вместе с приложением).
- Типы данных:
  - **App activity → In-app search history** — названия игр, которые ищут в каталоге (уходят в Wikidata и
    Википедию). Collected: **Yes**. Shared: **No** (передача по действию пользователя, которую он ожидает, —
    исключение из «sharing»). **Processed ephemerally: Yes**. Required: **No** (игру можно заполнить вручную).
    Purpose: **App functionality**.
  - **Location → Approximate location** — координаты города, угаданного по часовому поясу, для погоды в окне
    трактира (Open-Meteo / MET Norway). Collected: **Yes**. Shared: **No**. **Processed ephemerally: Yes**.
    Required: **Yes** (в настройках погода не выключается). Purpose: **App functionality**.
- Больше ничего не отмечать: сессии, игры, игроки, фото и скриншоты («Photos»), список приложений
  («App info and performance → Other / Installed apps») и время в играх с телефона не уходят. Мост в
  Lingvale — обмен между двумя приложениями на телефоне, без сети. Файл резервной копии и «Поделиться»
  пользователь отправляет сам — по определению Google это не сбор.
- Итог в карточке: «Collects: Location, App activity», «No data shared with third parties»,
  «Data is encrypted in transit».

Вариант строже — «No data collected» — формально неверен: поиск и координаты города уходят с телефона.

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Photo and video permissions** (появится после загрузки AAB: `READ_MEDIA_IMAGES`) — **главный риск ревью.**
Play разрешает широкий доступ к фото только тем, кому он нужен для основной функции; для разового выбора
положен системный Photo Picker. Questling им и пользуется для обложек и фото из галереи, а
`READ_MEDIA_IMAGES` нужен ровно для одного: найти скриншоты, которые телефон сам сделал во время сессии —
Photo Picker не умеет отбирать по времени, а пользователь не помнит, какие снимки из сотни были за эту сессию.
- Основная функция: **Other** (если нет ближе). Описание:
  > Questling is a diary of game sessions. When a session ends, it offers the screenshots the phone made
  > between the session's start and end, so the player can attach them without hunting through the gallery.
  > To find them it has to read the dates of the device's screenshots, which the Photo Picker cannot filter by
  > time. The feature is optional and off until the player turns it on; the app only reads screenshots inside
  > the session's time window and copies the ones the player ticks into its private storage. Pictures never
  > leave the device. For covers and other photos the app uses the system Photo Picker without any permission.
- **Если отклонят:** уберите разрешение из сборки для Play — `READ_MEDIA_IMAGES` и `READ_EXTERNAL_STORAGE`
  с `tools:node="remove"` в манифесте (или отдельным флейвором `play`), а подхват скриншотов спрячьте, когда
  разрешения нет в манифесте (`Media.readPermission`). Фото через Photo Picker продолжат работать. APK на
  GitHub может остаться с полной функцией.

**Usage access (`PACKAGE_USAGE_STATS`)** — отдельной формы в Play Console нет, но ревьюер смотрит, что
доступ объяснён до перехода в системные настройки и нужен для основной функции. В приложении переключатель
«Find sessions on the phone» с пояснением, и только он открывает `ACTION_USAGE_ACCESS_SETTINGS`. Если
спросят — это поиск сессий игр на телефоне, данные не покидают устройство (раздел в `PRIVACY.md`).

**Список приложений** — `QUERY_ALL_PACKAGES` нет: в манифесте `<queries>` только с интентом
`MAIN/LAUNCHER` (приложения со значком), это не требует декларации Package visibility.

**Прочие разрешения** — особых деклараций не нужно: `POST_NOTIFICATIONS` (часы сессии и «пора размяться»)
и `INTERNET` не чувствительные; `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE` и общий
`FOREGROUND_SERVICE` может добавить WorkManager. Если Play покажет форму **Foreground service permissions** —
ответ: приложение не запускает своих фоновых служб, разрешение пришло из библиотеки WorkManager.
Местоположения, точных будильников и `MANAGE_EXTERNAL_STORAGE` нет.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Closed testing** (для нового аккаунта; иначе сразу Production или Internal testing) → Create new release.
2. Загрузить `Questling-v1.2.1.aab` из GitHub Release.
3. Release name — номер версии. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

   В `release_notes.txt` сейчас «First release on Google Play» и новинки 1.2.0–1.2.1 (≤500 символов).
4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели;
   с декларацией фото — дольше.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран переключается сразу;
   «Look it up» находит игру с обложкой; сессия с таймером идёт в фоне и показывает уведомление;
   «Find sessions on the phone» открывает доступ к статистике и находит игру; Настройки → Резервная копия
   сохраняет zip; Lingvale (если стоит из Play) видит трактир.

## Следующие версии

- Ничего отдельно писать не нужно: `play.yml` берёт «What's new» из раздела версии в `CHANGELOG.md`
  (en-US — видимый текст, ru-RU — блок `<!-- tg -->`). Релиз уходит в Play после подтверждения в
  окружении `google-play`.
- Скриншоты переснимать, только если поменялись показанные экраны: `tools/capture.sh en|ru` →
  `python tools/play.py`.

## Риск: кнопка «Поддержать»

В настройках есть раздел «Поддержать»: ссылка открывает страницу поддержки StillCraft
(`https://bigdevwhale.github.io/stillcraft/support/`) в браузере. В приложении ничего не продаётся и не
открывается, поэтому под правило о платежах за цифровые товары она обычно не попадает (Glow прошёл с такой же).
Если ревью всё же придерётся (Payments policy), уберите ссылку из сборки для Play (`Community.SUPPORT` в
`ui/tavern/Tavern.kt`).

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.questling`, строку
«Get it on Google Play» — в заметки релиза (`release.yml`, как в Glow), ссылку — на страницу сайта.
