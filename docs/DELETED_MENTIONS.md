# 0.2.4: имя-упоминание внутри удалённой цитаты

Уточнение оформления после 0.2.3: одна TL_messageEntityBlockquote покрывает `имя\nсохранённый текст`; только один добавленный `\n` отделяет её от ответа. Author bold и настоящее упоминание полностью находятся внутри внешней quote. Пользовательский ответ и его entities остаются снаружи и сдвигаются в UTF-16 без изменения исходных данных. Свайп/Reply UI, обычные server replies, account/owner/dialog guards, forum root, retry и архив не меняются.

## Native Telegram и допустимость вложенности

- MediaDataController.getEntities создаёт TL_inputMessageEntityMentionName из URLSpanUserMention, получая InputUser через MessagesController.getInputUser.
- SendMessagesHelper передаёт entities в reqSend.entities (text) и native media requests. Отдельного @-текста или имитации ссылки не требуется.
- MessageObject преобразует inputMessageEntityMentionName и messageEntityMentionName в URLSpanUserMention с исходным user ID; native клиент открывает соответствующий профиль по этому span.
- [Input constructor](https://core.telegram.org/constructor/inputMessageEntityMentionName) используется при отправке; [received constructor](https://core.telegram.org/constructor/messageEntityMentionName) — в сообщении от сервера. Оба используют UTF-16 offset/length.
- Проверен [TDLib MessageEntity.cpp, c8828d50f5eac2fc7089ce1089d7ce9095c61b5e](https://github.com/tdlib/td/blob/c8828d50f5eac2fc7089ce1089d7ce9095c61b5e/td/telegram/MessageEntity.cpp): MentionName относится к continuous entities; она допустима внутри blockquote. Запрещены частичные пересечения, вложенные blockquote и blockquote внутри continuous entity. Поэтому сортировка: внешний quote, continuous mention, затем bold на том же имени. Ответ со своими quote/pre/code не попадает во внешнюю quote. Bot API для отправки не используется.

## Идентичность автора

Sender user ID берётся из сохранённого messageOwner.from_id.user_id, а не из текущего собеседника, username или текста имени. InputUser запрашивается у контроллера того же аккаунта; если после restart пользователь отсутствует в памяти/не имеет hash, на storage queue читается существующий native users cache. Новая история/DB/сервер не создаются. Callback проверяет Telegram user ID владельца, данные другого account slot не используются.

Принимается TL_inputUser с совпадающим положительным ID и доступным access_hash либо TL_inputUserSelf для собственного sender ID. InputUser клонируется; для outgoing local renderer у Self выставляется правильный display user ID (wire Self остаётся native). Empty, несовпадающий peer, нулевой hash и FromMessage reference, зависящий от удалённого содержимого, не превращаются в фиктивную ссылку. Нет пригодного пользовательского peer — остаётся обычное имя с bold, без mention. Sender-channel/анонимный отправитель не подменяется пользователем: InputUser относится только к пользователям.

Кешированный display name может быть актуальным именем того же ID; сеть выбирает профиль по ID, а не имени. Никакие реальные user access hashes не выводятся в логи/исходники; тесты используют вымышленные значения.

## Автоматические проверки и границы

DeletedMentionTest: внешний quote/name mention/bold, точный 64-bit sender ID и hash после TL round trip, Unicode-диапазоны имени, один разделитель, fallback для недоступного/чужого/непользовательского ID, отказ от FromMessage, привязка Self к своему UID, неизменность InputUser/body entities, received messageEntityMentionName с тем же ID. DeletedBlockquoteTest/DeletedReplyPolicyTest обновлены под новую геометрию. Все прежние regressions и SQLite tests остаются.

В Debian/Termux нет adb, emulator или подключённого инструмента управления Android UI. Поэтому агент **не проверял реальное нажатие в официальном Telegram**, не входил в аккаунты и не отправлял тестовые сообщения. TL round trip доказывает кодирование ID/диапазонов, но не заменяет end-to-end доставку и открытие профиля.

## Обязательная проверка в официальном Telegram — ещё не выполнена

С отдельными разрешёнными владельцами тестовыми аккаунтами/группой: получить и локально сохранить обычное сообщение автора A, удалить его со стороны A; в Tetogram свайпнуть копию и отправить текст B. Затем открыть отправленное сообщение в официальном Telegram:

- [ ] **Блок:** имя A и сохранённый текст находятся внутри одной native blockquote; ответ B — сразу следующей строкой снаружи, без добавленной пустой строки.
- [ ] **Кликабельность:** нажимается только имя A, без необходимости @username или дополнительной кнопки.
- [ ] **Идентичность:** открывается профиль именно A, а не отправителя ответа/текущего аккаунта/другого участника с таким же именем.

Дополнительно: RU/EN, emoji/ZWJ в имени/тексте, одинаковые display names разных sender IDs, несколько аккаунтов, restart с выгруженным users cache, фото-caption, forum General/другая тема, отсутствие User peer → plain name, обычный Reply и retry. Визуальный результат в AyuGram/Tetogram также требует проверки устройства.

## Результат CI

[Run 43472506292 — success](https://github.com/Rulax-s/Tetogram-/actions/runs/43472506292), APK source `d1f4d26dc45340dab4c8a3844e5a1ea575f8c375`. JUnit task с 55 тестами и 13 SQLite tests пройдены; compileDebugUnitTestJavaWithJavac/testDebugUnitTest реально выполнены. ARM64 assembleAfatDebug — BUILD SUCCESSFUL, 9m 19s. APK package/version/icon/providers/permissions/native ELF/signature — PASS.

[Скачать APK в ZIP](https://github.com/Rulax-s/Tetogram-/actions/runs/43472506292/artifacts/14062820798), доступен до 2026-11-27. Метаданные/sha256 — APK-0.2.4.json. Публичный сертификат совпал с выданной 0.2.2 и CI-сборкой 0.2.3; Secrets не менялись. Native UI/авторизация в официальном Telegram агентом не запускались. **Три официальных UI-пункта выше остаются NOT RUN.**

## Изменённые файлы после 0.2.2

- `README.md`
- `TMessagesProj/src/main/java/org/telegram/messenger/SendMessagesHelper.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/tetogram/DeletedReplyPolicy.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/tetogram/DeletedReplyProtocol.java`
- `TMessagesProj/src/test/java/org/telegram/messenger/tetogram/DeletedBlockquoteTest.java`
- `TMessagesProj/src/test/java/org/telegram/messenger/tetogram/DeletedMentionTest.java`
- `TMessagesProj/src/test/java/org/telegram/messenger/tetogram/DeletedReplyPolicyTest.java`
- `TMessagesProj/src/test/java/org/telegram/messenger/tetogram/DeletedReplyProtocolTest.java`
- `docs/APK-0.2.4.json`
- `docs/ARCHITECTURE.md`
- `docs/BUILD.md`
- `docs/CHANGELOG.md`
- `docs/CUSTOMIZATION.md`
- `docs/DELETED_BLOCKQUOTES.md`
- `docs/DELETED_MENTIONS.md`
- `docs/DELETED_REPLIES.md`
- `docs/MANUAL_TESTING.md`
- `docs/TROUBLESHOOTING.md`
- `docs/VALIDATION.md`
- `gradle.properties`
- `scripts/check-project.py`
- `scripts/verify-apk.py`
