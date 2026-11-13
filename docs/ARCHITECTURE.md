# Архитектура Tetogram

Описание относится к commit в `UPSTREAM.json`, а не к старым руководствам по Telegram. Внутренние пакеты `org.telegram.*` сохранены: package Java нужен для JNI, ресурсов и связей классов и не равен Android application ID.

## Модули и потоки данных

`TMessagesProj_App` — оболочка Android-приложения, manifest, flavor, debug-подпись и Firebase-ресурсы. `TMessagesProj` — основная Java-библиотека UI/логики и JNI/C++. `buildSrc` содержит плагины генерации локализации, resource ID и Lottie metadata. `TMessagesProj_Modules/media` — подмодуль Media3, `TMessagesProj/lib/jlatexmath` — подмодуль формул. Варианты Huawei/HockeyApp/Standalone/Tests сохранены, но пока не настроены как выпускаемые Tetogram-варианты.

Общий поток: Android view/fragment → контроллер конкретного аккаунта → `org/telegram/tgnet/ConnectionsManager.java` → JNI `jni/tgnet` → инфраструктура Telegram. Состояние приходит через контроллеры, `MessagesStorage` и `NotificationCenter`, затем отображается существующими ячейками. UI нельзя подменять локальными фиктивными коллекциями.

В таблице пути относительно `TMessagesProj/src/main/java/org/telegram/`, если не указано иное.

| Область | Реальные файлы | Безопасная точка изменения |
| --- | --- | --- |
| Список чатов | `ui/DialogsActivity.java`, `ui/Cells/DialogCell.java` | Менять отрисовку и оформление строки; сохранять dialog ID, папки, selection, RecyclerView recycling и обработку обновлений |
| Переписка | `ui/ChatActivity.java` | Локальные компоненты оформления; сохранять загрузку истории, scroll anchor, replies, поиск, scheduled mode и секретные чаты |
| Отображение сообщений | `ui/Cells/ChatMessageCell.java`, `ui/Cells/ChatActionCell.java`, `messenger/MessageObject.java` | Сначала отделить небольшой визуальный блок; проверить все типы сообщений, accessibility и hit regions |
| Основная навигация/меню | `ui/LaunchActivity.java`, `ui/MainTabsActivity.java`, `ui/MainTabsActivityController.java`, `ui/MainTabsLayout.java`, `ui/ActionBar/DrawerLayoutContainer.java` | Использовать существующую навигацию и жизненный цикл. В текущем upstream есть MainTabs; старые инструкции про DrawerLayoutAdapter не соответствуют этому дереву |
| Базовые экраны и переходы | `ui/ActionBar/BaseFragment.java`, `ui/ActionBar/ActionBarLayout.java` | Уважать lifecycle, back/swipe, tablet layout, восстановление стека и отмену анимаций |
| Профиль | `ui/ProfileActivity.java`, `ui/ProfileActivity2.java` | Проверять фактическое место создания профиля в вызывающем коде: обе реализации есть в исходниках |
| Панель ввода | `ui/Components/ChatActivityEnterView.java`, `ChatActivityEnterViewAnimatedIconView.java` в той же папке | Не менять pipeline отправки при косметических правках; сохранить клавиатуру, запись голоса, редактирование, reply и draft |
| Вложения | `ui/Components/ChatAttachAlert.java`, `ChatAttachAlertPhotoLayout.java`, `ChatAttachAlertDocumentLayout.java`, `ChatAttachAlertAudioLayout.java` | Использовать существующие permission/file URI flows и отправку через SendMessagesHelper |
| Настройки | `ui/SettingsActivity.java`, `ui/HeaderSettingsActivity.java`, `ui/PrivacySettingsActivity.java`, `ui/DataSettingsActivity.java`, `ui/NotificationsSettingsActivity.java` | Добавлять отдельные строки/экран в будущем; не переиспользовать чужие индексы и не менять account-specific storage на глобальный |
| Темы и цвета | `ui/ActionBar/Theme.java`, `ui/ActionBar/theme/ITheme.java` | Использовать существующие Theme key/ResourcesProvider, поддержать day/night и пользовательские темы; не размазывать фиксированные цвета по экранам |
| Уведомления | `messenger/NotificationsController.java`, `messenger/GcmPushListenerService.java`, `messenger/PushListenerController.java` | Сохранять каналы, account routing, mute/privacy, PendingIntent и ограничения Android. FCM-конфигурация сама по себе ещё не подтверждает доставку push от Telegram |
| Анимации | `ui/Components/RLottieDrawable.java`, `RLottieNative.java`, `AnimatedEmojiDrawable.java`, `CubicBezierInterpolator.java`, `messenger/AnimationNotificationsLocker.java` | Соблюдать cancel/recycle и lite mode; JNI tlottie и звонки не отключать ради сборки |
| Отправка сообщений/медиа | `messenger/SendMessagesHelper.java`, `messenger/FileLoader.java`, `messenger/MediaController.java` | Менять UI отдельно от очереди загрузки, шифрования, retry и подтверждения сервера |
| Получение/просмотр медиа | `messenger/ImageLoader.java`, `messenger/MediaDataController.java`, `ui/PhotoViewer.java` | Сохранить кеши, key/IV секретных чатов, жизненный цикл декодеров и Media3 |
| Хранилище/обновления | `messenger/MessagesController.java`, `MessagesStorage.java`, `NotificationCenter.java` | Уважать account instance, очереди SQLite/network/UI, версии схемы и подписки на события |
| Звонки | `messenger/voip/VoIPService.java`, `jni/voip`, `jni/tgcalls`, `jni/voip/webrtc` | Не заменять/удалять реализацию; отдельно тестировать permissions, foreground service, аудиомаршруты и видеозахват |
| Истории | `ui/Stories/StoriesController.java`, `ui/Stories/StoryViewer.java` | Сохранять серверные данные и lifecycle просмотра/публикации |
| Боты и Mini Apps | `ui/bots/`, `ui/web/BotWebViewContainer.java` | Не менять origin, разрешения и мост WebView для косметических изменений |

## Нативная часть

`TMessagesProj/jni/CMakeLists.txt` определяет `tmessages.49` и `twidget`. В сборку входят MTProto (`tgnet`), SQLite, криптография, медиа, WebRTC/звонки, Lottie, widget engine. Часть библиотек импортируется из `jni/prebuild/lib/<ABI>`, часть компилируется из исходников и подмодулей. Предсобранные ARM64 `.a` присутствуют в upstream; это не доказательство корректной линковки конечного APK. Не удалять зависимости для обхода ошибок.

## Изоляция Tetogram

- `gradle.properties`: package, версия и versionCode.
- `gradle/tetogram-config.gradle`: чтение owner configuration и проверка перед `preBuild`.
- `BuildVars.java`: поля BuildConfig вместо официальных API-данных, отключён официальный updater и сервисы, привязанные к официальной идентичности.
- `TetogramConfig.java`: вычисление SMS Retriever hash по фактическому package и сертификату.
- `ContactsController.java`, `res/xml/auth*`, `sync_contacts.xml`, `contacts.xml`, MIME-проверки в LaunchActivity: собственный тип Android-аккаунта и MIME-идентификаторы контактов.
- Provider authorities и пользовательские permissions используют `${applicationId}`. Java-имена activities/services могут совпадать между APK, поскольку Android различает компоненты по паре package/class.
- Приватные действия медиаплеера и категория Direct Share имеют префикс Tetogram; legacy RemoteViews intents ограничены своим package.
- Fallback `ApplicationLoader.getFilesDirFixed()` обращается к собственному dataDir; путь официального Telegram не используется.
- `res/mipmap-anydpi*/tetogram_launcher.xml`, `drawable/tetogram_mark.xml`: своя векторная и адаптивная иконка. Алиасы launcher сохранены, но все используют брендинг Tetogram.
- `LocaleController`: русский язык по умолчанию и защита названия/описания Tetogram от подмены облачным переводом Telegram. Остальная облачная локализация остаётся.

Не добавлены аналитика, сторонний сервер сессий или передача данных владельцу форка. Это описание внесённых изменений, а не полный аудит всего upstream и всех его внешних интеграций.

Брендинг в IntroActivity, DialogsActivity и DialogStoriesCell использует TetogramWordmarkDrawable вместо графического логотипа Telegram. Анимированный знак вводного экрана заменён на T; первая страница сообщает о Telegram API и неофициальном статусе, остальные анимации и страницы сохранены.

## Tetogram Labs / 0.2.0

Отдельный namespace настроек и local history: [DELETED_HISTORY.md](DELETED_HISTORY.md). Основная база и storage queue Telegram сохранены; local-only MessageObject не записывается обратно в messages_v2 и не отправляется на сервер. Новые UI-настройки добавлять через ui/tetogram/LabsPage, без изменений MainTabs.

## 0.2.1: идентичность строк и mixed timeline

DeletedRowPolicy проверяет account/dialog/message и создаёт независимую строку из notification payload. DeletedHistory.copyForRow переносит native stable params через MessageObject.copyStableParams. При любой будущей замене MessageObject сохраняйте adapter stable ID; не используйте mid или 0 как общий RecyclerView ID.

MessageTimelinePolicy содержит выделенное без изменения семантики правило сортировки MessagesStorage. Packed ephemeral IDs не принадлежат обычной серверной последовательности. HistoryPagePolicy.merge сохраняет порядок исходной страницы и не включает ephemeral/pending в выбор regular cursor. Не заменяйте эти механизмы общей сортировкой по ID или timestamp. Разбор причин и регрессии — MESSAGE_INTEGRITY.md.

## 0.2.2: Reply на локальную копию

Reply остаётся native: ChatActivity swipe/OPTION_REPLY → обычная панель ChatActivityEnterView → SendMessagesHelper. DeletedReplyPolicy формирует неизменяющий входы префикс/UTF-16 shift; DeletedReplyProtocol клонирует TL entities и формирует forum header. DeletedHistory.find разрешает native draft ID через ту же account DB; MediaDataController не синхронизирует удалённую reply-цель и убирает дублирующий draft payload. UI поле tetogramHistoryOwner связывает копию с Telegram user ID, а не только переиспользуемым slot. Подробности/границы проверок — DELETED_REPLIES.md.

## 0.2.3: native blockquote entities

DeletedReplyPolicy.Result теперь содержит authorLength/quoteOffset/quoteLength/entityShift для конечной UTF-16 строки. DeletedReplyProtocol.formatEntities клонирует entities ответа, добавляет отдельные bold автора и TL_messageEntityBlockquote текста. Диапазоны префикса не пересекаются с ответом и его собственными blockquote/pre/code. Рендерер не меняется: MessageObject → QuoteSpan. Архив и стандартный Reply UI не меняются; DELETED_BLOCKQUOTES.md содержит детали и проверки.

## 0.2.4: mention автора внутри quote

Внешний TL_messageEntityBlockquote охватывает имя и текст. Внутри — TL_inputMessageEntityMentionName по raw from_id.user_id/InputUser того же аккаунта, затем bold имени. Диапазон ответа начинается после одного переноса. Канонический порядок соблюдает правила TDLib: quote → continuous mention → splittable bold. При необходимости Sender User загружается из native users cache на storage queue с проверкой owner UID. Не используйте receiver ID/username или фиктивный URL вместо source sender ID. При недоступном InputUser остаётся plain name; DELETED_MENTIONS.md.

Пользовательские разделы 0.3.0: `messenger/tetogram/SectionConfig`, `SectionsStore`, `SectionDialogs`; редактор `ui/tetogram/SectionsActivity`, плавающее меню `SectionMenu`. Интеграция ограничена главным DialogsActivity, его adapter/empty cell и MainTabsActivity. См. [SECTIONS.md](SECTIONS.md) для ключей, вкладок и ручного чек-листа. Данные разделов не содержат копий сообщений.
