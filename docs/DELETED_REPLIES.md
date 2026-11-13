# Reply на локальную удалённую копию — 0.2.2

Референс: [AyuGram c9c317cf5b0877763e0e973acaf334251a5d38a8](https://github.com/AyuGram/AyuGram4A/commit/c9c317cf5b0877763e0e973acaf334251a5d38a8), автор ZavaruKitsu, GPL. Изучен полный patch SendMessagesHelper.java/AyuUtils.java. В нём ayuDeleted обнаруживается перед отправкой: имя автора в группе + `> ` + shortify(messageText,20), затем replyToMsg=null. Отдельной системы Reply не создаётся. Медиа без текста покрываются native messageText (Photo/Video/Document и т.п.); автоматической пересылки файла там нет. Tetogram использует тот же подход, адаптированный независимо к текущему Telegram.

## Интерфейс и отправка

Свайп проходит через штатный sliding reply ChatActivity, меню через стандартный OPTION_REPLY, предпросмотр/отмена — через showFieldPanelForReply/ChatActivityEnterView. В native меню локальной копии разрешены Reply и существующая информация о локальном сохранении; серверные edit/delete/forward/reaction по-прежнему запрещены. Обычный тап сохраняет открытие локальной информации. Старые bot keyboard удалённого сообщения не активируются. Штатные ограничения возможности отправки и форумной темы сохраняются.

В SendMessagesHelper изменяются только локальные переменные подготовки: обычный server Reply проходит без преобразования, а tetogramLocalDeleted даёт текстовый префикс. В личном чате имя опускается; в группе берётся реальный from peer (User/Chat) текущего аккаунта, при отсутствии имени — явный sender ID, без подстановки другого пользователя. Текст берётся из сохранённого messageOwner.message, иначе из native messageText с типом вложения. Исходный TL Message и архив не меняются; удалённые файлы не прикладываются.

Цитата ограничена 20 пользовательскими символами (при обрезке 19 + …), включая цельные surrogate/emoji ZWJ/flag/combining-кластеры. Это Unicode-безопасная адаптация исходной UTF-16 обрезки AyuGram. Префикс `имя\n> цитата\n\n` добавляется к тексту или подписи. Entities клонируются полностью и смещаются на фактическую UTF-16 длину префикса **вместе с двумя переводами строки**; все остальные поля/длины сохраняются. Исправлена неточность исходного patch AyuGram, где shift использовал только prefix.length().

При передаче retryMessageObject преобразование не выполняется: подготовленные text/caption/entities уже находятся в сохранённом outgoing MessageObject. Повтор подготовки тех же исходных параметров также не накапливает префиксы: ни входные строки, ни entities не мутируются. Цитата появляется только на отправке, не в редакторе до неё.

Telegram user ID владельца архива, account slot и dialog ID проверяются до преобразования; повторное использование слота другим аккаунтом не разрешает отправить старую цитату. Перенос локального Reply в другой чат запрещён. Ephemeral, secret и отправка с ephemeralReceiverBotId не преобразуются в обычное публичное сообщение: некорректный local target отклоняется. Для отдельной форумной темы удалённый reply ID заменяется валидным текущим top/root ID либо native forum top из metadata; General использует штатную неявную маршрутизацию без Reply на сообщение 1; текстовая цитата не переводит сообщение в другой диалог. Нативная маршрутизация темы остаётся в reply_to.

## Draft и перезапуск

Native drafts продолжают храниться штатным MediaDataController. После восстановления Reply-панели выполняется поиск account/dialog/message в существующей таблице архива. Такой же поиск перед отправкой учитывает потерю UI-флага при десериализации draft и удаление сообщения во время набора. Callback проверяет UID аккаунта; обновление предпросмотра не применяется после отмены/смены Reply.

Перед синхронизацией draft удалённый reply ID убирается из сетевого запроса (текст draft не цитируется); для forum сохраняется только корень темы. У локального native draft сохраняется reply ID; дублирующий payload удалённого сообщения убирается из его preferences/cache после подтверждения архива. Preview восстанавливается из существующей SQLite-таблицы. При очистке/истечении архива дополнительной постоянной копии текста в draft не остаётся. Защита от устаревших callback сверяет текущий draft. Новая база, отдельный формат ответов и отдельный экран не создаются.

## Область поддержки и проверки

Поддержаны текст и подписи фото/видео/файлов; для входящего сообщения только с медиа цитируется обозначение вложения. Contact/location/poll/todo/invoice/game/rich article не имеют нужного обычного текстового caption-поля: при попытке local Reply показано явное сообщение, сетевой запрос не отправляется. Это не меняет обычную отправку таких вложений без local Reply. Очень длинный ответ вместе с префиксом подчиняется серверному лимиту текста/подписи, автоматического обрезания пользовательского ответа нет.

DeletedReplyPolicyTest проверяет формат группы/личного чата, 20 символов, суррогаты/семейные emoji/флаги/диакритику/skin tone/keycap, UTF-16 смещение, сохранение entities и их исходных offsets, подпись/пустую подпись фото, retry gate, повторную подготовку и независимые ответы, контекст аккаунта/чата/secret/ephemeral, повторное использование account slot. DeletedReplyProtocolTest использует настоящие TL entities (bold, URL, custom emoji) и TL-сериализацию forum header. Существующие 0.2.1 регрессии идентичности/порядка и SQLite-тесты сохраняются. Это JVM policies/SQLite, не Android instrumentation.

Ручная проверка (агентом пока не выполнена):

- [ ] Свайп и стандартный Reply через long press; автор и preview исходного сообщения.
- [ ] Текст не меняется до отправки, отмена ничего не отправляет.
- [ ] Несколько ответов разным авторам, эмодзи/ссылки/bold/custom emoji entities.
- [ ] Фото с подписью и без; цитирование входящего фото/видео/файла без caption.
- [ ] Ошибка сети/повтор отправки не добавляют второй префикс.
- [ ] Draft после перезапуска; удаление исходного сообщения во время набора.
- [ ] Forum General/отдельная тема, личный чат, группа, два аккаунта; без переноса в другой чат.
- [ ] Результат отображается обычным текстом/подписью в официальном Telegram.
- [ ] Ephemeral visibility, secret chats, порядок, stable IDs, archive/очистка/уведомления не регрессируют.

## История CI

Run 43452138709 остановился на компиляции нового TL-теста: document_id доступен у TL_messageEntityCustomEmoji, а не базового MessageEntity. Исправлено обращение к конкретному типу; тест не отключался. Итог финального запуска приведён ниже.

## Результат проверки

[CI 43454426141 — success](https://github.com/Rulax-s/Tetogram-/actions/runs/43454426141), APK source `d7dbc837eaa65c55607fda62359b360789b3a939`. JUnit task с 40 тестами (12 DeletedReplyPolicy, 4 DeletedReplyProtocol, 24 существующих policy regressions) и 13 SQLite tests пройдены; compileDebugUnitTestJavaWithJavac/testDebugUnitTest реально выполнены. ARM64 APK — BUILD SUCCESSFUL, 9m 34s; package/version/icon/providers/permissions/ELF/signature — PASS.

[Артефакт APK](https://github.com/Rulax-s/Tetogram-/actions/runs/43454426141/artifacts/14052874018), доступен до 2026-11-27. Метаданные/sha256 — APK-0.2.2.json. Сертификат совпал с ранее выданной 0.2.1; Secrets не менялись. Жесты, доставка, реальный restart/установка обновления и просмотр в официальном Telegram на устройстве агентом не проверялись. Нельзя считать эти runtime-сценарии подтверждёнными только по JVM/TL tests.


## Изменённые файлы

- `README.md`
- `TMessagesProj/src/main/java/org/telegram/messenger/MediaDataController.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/MessageObject.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/SendMessagesHelper.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/tetogram/DeletedHistory.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/tetogram/DeletedReplyPolicy.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/tetogram/DeletedReplyProtocol.java`
- `TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java`
- `TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java`
- `TMessagesProj/src/main/res/values-ru/strings.xml`
- `TMessagesProj/src/main/res/values/strings.xml`
- `TMessagesProj/src/test/java/org/telegram/messenger/tetogram/DeletedReplyPolicyTest.java`
- `TMessagesProj/src/test/java/org/telegram/messenger/tetogram/DeletedReplyProtocolTest.java`
- `docs/APK-0.2.2.json`
- `docs/ARCHITECTURE.md`
- `docs/BUILD.md`
- `docs/CHANGELOG.md`
- `docs/CUSTOMIZATION.md`
- `docs/DELETED_REPLIES.md`
- `docs/MANUAL_TESTING.md`
- `docs/TROUBLESHOOTING.md`
- `docs/VALIDATION.md`
- `gradle.properties`
- `scripts/check-project.py`
- `scripts/verify-apk.py`
