# Tetogram Labs и локальные удалённые сообщения (0.2.0)

Статус: финальный commit df279bb6a302416a720447705d4e96d684510ce1 прошёл 11 SQL contract tests, 8 JUnit tests и Android-сборку в CI 43435783620. APK проверен и подписан постоянным сертификатом 0.2.0. Проверки на устройстве всё ещё требуются. Версия 0.1.0 подтверждена владельцем как установленная и работающая; это не подтверждает новую функцию.

## Аудит перед изменениями

Рабочее дерево было чистым. Локальная и удалённая master: e77a801. Других веток/рабочих деревьев и реализации v2 в доступном репозитории не обнаружено. `MessagesController.deletedHistory` является служебным индексом Telegram, а не архивом сообщений. Поэтому создана одна таблица `tetogram_deleted_v1` внутри существующей SQLite-базы `MessagesStorage` каждого аккаунта, без отдельной DB, Room или сервера.

Референс: [AyuGram4A](https://github.com/AyuGram/AyuGram4A), ветка rewrite, commit 7013145676d36d82ee13c02a89f72097b7490dcd. Изучены AyuMessagesController, DeletedMessage, DeletedMessageDao и архитектура локальных снимков. Реализация Tetogram написана для текущего официального Telegram; чужие ключи, proprietary hooks, AyuSync, ghost mode и обходы ephemeral-защиты не переносились. Автор AyuGram: Radolyn; источники референса сохраняют GPL/copyright. Код референса не копировался.

## Интерфейс

Настройки → Tetogram Labs (колба), штатный BaseFragment/UniversalRecyclerView. Нижняя навигация не менялась. Русский и английский тексты добавлены в существующую систему локализации; другие языки используют английский fallback.

Сохранение выключено по умолчанию. Первое включение для каждого Telegram user ID требует предупреждения о приватности, локальных копиях и возможном конфликте с Telegram API terms. Сохранение, показ в чате, красная отметка, исключения и retention хранятся в versioned SharedPreferences `tetogram_labs_v1_<userId>`, а не в общих настройках Telegram и не по номеру переиспользуемого account slot.

Срок: 7/30/90 дней либо без ограничения; default 30. Возраст считается от первого локального сохранения. Очистка выполняется при обращении к архиву и раз в минуту в открытом чате. Когда процесс не работает, физическая очистка возможна при следующем запуске/обращении; срок проверяется до выдачи записей.

В меню обычного чата есть «Удалённые сообщения»: локальная постраничная выборка по времени удаления, поиск RU/EN без учёта регистра, полный текст/метаданные, переход к исходному ID и удаление локальных копий. Никакой отдельной фиктивной переписки нет. В окне обычной переписки используются исходные пузыри и механизм группировки Telegram.

Красная корзина и текст «Удалено» встроены в layout времени сообщения. Если красная отметка отключена, нейтральная подпись «Удалено» остаётся: копия не выдаётся за существующее серверное сообщение. У копии собственное локальное информационное меню; редактирование/удаление на сервере, пересылка, реакции, swipe-reply, inline-кнопки и multi-select для неё отключены. Нативные операции обычных сообщений не меняются.

История редактирования и отметки предыдущих версий явно обозначены недоступными: такого backend в исходной master нет. Фиктивные переключатели не добавлены. Раздел «Интерфейс» только документирует точки будущего расширения; новых настроек нижней навигации не реализовано.

## Хранение и события

- Снимки записываются из обоих штатных путей MessagesStorage: получение/изменение сообщений и загрузка обычной истории. Дополнительных сетевых запросов ради архива нет.
- Таблица содержит account user ID, dialog ID, message ID, channel ID, sender ID, текст/подпись, нормализованный поисковый текст, thread/topic ID, исходную дату, время первого сохранения, время удаления, сериализованный TL Message. PK(account,did,mid) исключает дубликаты. Содержимое хранится только в приватной базе приложения.
- Ключ канала отделён от глобального пространства message ID личных чатов/обычных групп. Миграции используют исходные dialog ID; native mergeDialogId продолжает обслуживать старую часть истории.
- Для remote updateDeleteMessages/updateDeleteChannelMessages и удаления из push архив помечается на storage queue. Повтор события не меняет первую дату удаления. Если записи нет, содержимое не создаётся и не ищется на сервере.
- Штатные storage удаления, pts/state, counters и обновление списка диалогов остаются. UI получает отдельный десериализованный MessageObject локальной копии; исходный server object и messages_v2 не восстанавливаются и не помечаются как живые.
- Страницы UI сливаются по ID, но native end-of-history использует число серверных сообщений отдельно от overlay. При заполнении промежутка локальными сообщениями отображаемая страница ограничивается count, оставляя следующий ID как cursor; локальные элементы не должны пропускать большие разрывы.
- После выключения показа, исключения, очистки или expiration открытый чат сверяет overlay с архивом, затем использует штатное удаление элементов UI. При смене диалога устаревший callback не применяется.

Секретные чаты, outgoing, service/ephemeral, сообщения с ttl/ttl_period/media ttl/expire_date и защищённый noforwards-контент не архивируются. Для сообщения, ставшего исключённым/исчезающим, старый снимок удаляется при получении обновления. Исходное сообщение или оригинальные медиа-файлы не восстанавливаются после удаления; наличие TL metadata не обещает сохранности вложения, очищенного обычным Telegram-кешем.

Полная очистка требует подтверждения и удаляет архив текущего аккаунта, с secure_delete и WAL checkpoint. Копии других аккаунтов не затрагиваются, серверные сообщения не удаляются. Ошибки очистки и чтения показываются как ошибки, а не как успешная очистка/пустой результат. Выключение функции само по себе не стирает архив; для этого есть отдельная очистка.

Полные Android cloud backups/device transfers исключают app files/databases/preferences через backup rules. Исходный selective BackupAgent для saved_tokens не менялся и архив не включает. Собственного экспорта, Firebase-хранилища и синхронизации истории нет. Выход из аккаунта/удаление данных приложения/пересоздание основной DB удаляет и архив.

## Проверки

`python3 scripts/test-history-storage.py`: 11 проверок реального SQLite со схемой и marking SQL из Java: приём/удаление, несколько сообщений/порядок, группа, одинаковые ID разных каналов, закрытие и повторное открытие DB, неизвестные сообщения, очистка без изменения штатного кеша, два аккаунта, повторные события, retention/русский поиск, topic separation.

`HistoryPolicyTest` (JUnit в CI): default-off gate, encrypted/outgoing/excluded/protected content, разные TTL/одноразовое медиа/expiry/ephemeral, повреждённые retention values. Это pure Java tests, не Android UI-тесты.

Нужно проверить на устройстве все 12 сценариев из запроса, включая реальное удаление при открытом/закрытом приложении, restart, переключение аккаунтов, live scroll, альбомы, thread/topic navigation, очистку при открытом чате, смену темы и маленький экран. SQL reopen не является подтверждением Android background lifecycle. Агент не использует реальные Telegram-аккаунты без отдельного разрешения.

## Размещение кода

- messenger/tetogram/LabsSettings: персистентные настройки пользователя.
- messenger/tetogram/HistoryPolicy: проверяемые правила допуска.
- messenger/tetogram/DeletedHistory: единственный backend, существующая storage queue/SQLite.
- ui/tetogram/LabsPage, LabsActivity: модуль настроек и категории.
- ui/tetogram/ExcludedChatsActivity, HistoryActivity: исключения/локальная история.
- MessagesStorage, MessagesController: небольшие hooks записи/уведомлений.
- MessageObject, ChatActivity, ChatMessageCell: флаги overlay, native bubbles/пагинация, безопасные действия и красная отметка.

Для обновления установленной 0.1.0 есть отдельное ограничение подписи: старый приватный ключ не сохранён владельцем. Приложение/данные агентом не удалялись. Новая стабильная тестовая подпись описана в BUILD.md.

## Изменённые файлы относительно исходной master e77a801

- `.github/workflows/android-build.yml`
- `.gitignore`
- `README.md`
- `TMessagesProj/build.gradle`
- `TMessagesProj/src/main/AndroidManifest.xml`
- `TMessagesProj/src/main/java/org/telegram/messenger/MessageObject.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/MessagesStorage.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/SendMessagesHelper.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/tetogram/DeletedHistory.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/tetogram/HistoryPagePolicy.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/tetogram/HistoryPolicy.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/tetogram/LabsSettings.java`
- `TMessagesProj/src/main/java/org/telegram/ui/Cells/ChatMessageCell.java`
- `TMessagesProj/src/main/java/org/telegram/ui/ChatActivity.java`
- `TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java`
- `TMessagesProj/src/main/java/org/telegram/ui/tetogram/ExcludedChatsActivity.java`
- `TMessagesProj/src/main/java/org/telegram/ui/tetogram/HistoryActivity.java`
- `TMessagesProj/src/main/java/org/telegram/ui/tetogram/LabsActivity.java`
- `TMessagesProj/src/main/java/org/telegram/ui/tetogram/LabsPage.java`
- `TMessagesProj/src/main/res/drawable/tetogram_labs.xml`
- `TMessagesProj/src/main/res/values-ru/strings.xml`
- `TMessagesProj/src/main/res/values/strings.xml`
- `TMessagesProj/src/main/res/xml/tetogram_backup_rules.xml`
- `TMessagesProj/src/main/res/xml/tetogram_data_extraction_rules.xml`
- `TMessagesProj/src/test/java/org/telegram/messenger/tetogram/HistoryPagePolicyTest.java`
- `TMessagesProj/src/test/java/org/telegram/messenger/tetogram/HistoryPolicyTest.java`
- `TMessagesProj_App/build.gradle`
- `docs/APK-0.2.0.json`
- `docs/ARCHITECTURE.md`
- `docs/BUILD.md`
- `docs/CHANGELOG.md`
- `docs/CUSTOMIZATION.md`
- `docs/DELETED_HISTORY.md`
- `docs/MANUAL_TESTING.md`
- `docs/VALIDATION.md`
- `gradle.properties`
- `gradle/tetogram-config.gradle`
- `scripts/check-project.py`
- `scripts/prepare-ci.py`
- `scripts/test-history-storage.py`
- `scripts/verify-apk.py`
- `tetogram.properties.example`
