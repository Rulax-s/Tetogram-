# История изменений

## 0.1.0 — подготовка, 2026-11-13

- За основу взят DrKLO/Telegram 13.2.2, commit сохранён в UPSTREAM.json; remote upstream сохранён.
- Настроены `org.tetogram.messenger`, версия 0.1.0, имя Tetogram и собственная векторная/адаптивная иконка T.
- Сохранены Java-пакеты, основные UI/контроллеры, MTProto, JNI, звонки и исходные библиотеки.
- Разделены Android account type, contact MIME, account settings intent, действия медиаплеера и Direct Share; исправлен fallback каталога файлов.
- Сохранены лицензионные assets, которые upstream исключал из упаковки.
- Указаны неофициальный статус и проект Tetogram в информации о приложении; русский выбран default, остальные переводы сохранены.
- Настроены owner API configuration, Firebase в APK-модуле, проверка конфигурации и gitignore. Официальные Firebase-образцы удалены из текущего дерева.
- Настроены ARM64 afatDebug, стандартная debug-подпись AGP и имя Tetogram-debug.apk; добавлена опциональная постоянная debug-подпись из Secret.
- Отключены официальный updater, официально привязанные passkeys/PNV; зафиксированы ограничения сторонних сервисов и покупок.
- Добавлены GitHub Actions, статические проверки, проверка APK и документация для дальнейших изменений.

APK ещё не получена; выпуск 0.1.0 и runtime-готовность не подтверждены.

Брендинг в IntroActivity, DialogsActivity и DialogStoriesCell использует TetogramWordmarkDrawable вместо графического логотипа Telegram. Анимированный знак вводного экрана заменён на T; первая страница сообщает о Telegram API и неофициальном статусе, остальные анимации и страницы сохранены.

Splash Android 12+ использует собственный анимированный знак T вместо официального логотипа.

## Публикация CI — 2026-11-13

- Origin исправлен на Rulax-s/Tetogram-, основная ветка master; upstream сохранён.
- Исходники и workflow опубликованы штатным Git push через существующую авторизацию Termux.
- Подтверждены имена обязательных Actions Secrets без доступа к значениям.
- Подготовка конфигурации принимает base64 с переносами строк; проверена в изолированном временном проекте.
- Первый cloud run 43408428079 заблокирован GitHub по billing issue до старта runner. APK не получена; Firebase JSON и Gradle пока не проверены в облаке.

## 0.1.0 debug APK — 2026-11-13

- Billing-блокировка GitHub устранена владельцем; облачные runner запускаются.
- На реальной сборке подтверждены SDK/NDK, Gradle-задача и соответствие Firebase package/applicationId.
- Исправлена ложная ошибка проверки authorities: вместо поиска по aapt2 dump разбирается XML из APK через apkanalyzer, уникальность по-прежнему обязательна.
- Успешный run 43415735775: APK собрана, проверена, подписана debug-ключом и загружена в Artifacts. Метаданные и SHA-256 сохранены в APK-0.1.0.json.
- APK скачана, checksum сверена; копия размещена в Android Download без замены существующих файлов.
- Установка, авторизация и runtime-функциональность ещё не проверены; release-готовность не заявляется.

## 0.2.0 — Tetogram Labs и локальная история (экспериментальная debug-сборка)

- Добавлен отдельный раздел Labs в стандартные настройки; нижняя навигация не меняется.
- Добавлена одна локальная таблица в существующую базу аккаунта: снимки, пометка удаления, локальные копии в native chat UI, список/поиск/информация/очистка.
- Настройки и предупреждение — на Telegram user ID; default off, исключение secret/TTL/view-once и protected content.
- Retention, исключения, ограничения серверных действий для локальных копий, backup exclusions.
- История редактирования явно недоступна до реализации/предоставления backend; заглушек нет.
- Добавлены SQLite contract tests и JUnit policy tests, CI дополнен запуском тестов.
- Финальный commit df279bb успешно собран в CI run 43435783620: SQL/JUnit, проверка APK и постоянной подписи пройдены. Runtime-сценарии версии 0.2.0 на Android не проверены.
- Исправлено сохранение расположения и подписи сообщения при замене локальной копией в медиаальбоме. Детали в DELETED_HISTORY.md.

## 0.2.1 — исправление идентичности и порядка (debug APK)

- Локальные удалённые сообщения сохраняют stable ID исходной строки; каждый фрагмент получает независимый MessageObject/TL payload.
- Сопоставление по account/dialog/message; согласованы индексы обычного/filtered списка и альбомов.
- Packed ephemeral IDs исключены из regular cursor bounds/trim; overlay сохраняет native порядок серверной страницы.
- Native ephemeral comparator переиспользован в SQLite и live insertion для поздно пришедших эфемерных сообщений.
- Добавлены регрессионные тесты policies. Android-проверки ещё требуются; подробности — MESSAGE_INTEGRITY.md.
- GitHub Secrets и правила видимости/TTL не менялись.

- CI 43443980427: 13 SQLite tests, JUnit task (24 tests), сборка APK и проверка подписи — success; Android runtime-регрессии пока не проверены.

## 0.2.2 — штатный Reply на удалённые копии (debug APK)

- Свайп и стандартный OPTION_REPLY открывают обычную панель ответа на локальную копию.
- По подходу AyuGram c9c317cf перед отправкой добавляются автор/короткая текстовая цитата и удаляется недействительная reply-цель; forum root сохраняется.
- Unicode-safe обрезка 20 символов, клонирование entities со сдвигом UTF-16 включая разделители; исходный архив не меняется.
- Native retry не добавляет цитату повторно; draft/удаление во время набора разрешаются через существующий архив. Сетевой draft не отправляет удалённый reply ID.
- Ephemeral/secret и чужой account/dialog исключены; типы вложений без caption явно отклоняются для local Reply.
- Добавлены JVM regression tests. Runtime-проверки на Android ещё требуются; DELETED_REPLIES.md.

- CI 43454426141 — success: JUnit task с 40 тестами, 13 SQLite tests, сборка/проверка APK. Метаданные — APK-0.2.2.json; Android runtime ещё требует ручной проверки.

## 0.2.3 — Telegram blockquote в удалённом Reply (промежуточная debug APK)

- Текстовый `>` заменён настоящей TL_messageEntityBlockquote; имя автора вынесено в отдельную bold entity перед цитатой.
- UTF-16 диапазоны рассчитаны по конечной строке; entities ответа клонируются и сдвигаются без потери форматирования.
- Добавленная цитата не пересекается с автором и ответом, включая его собственные blockquote/pre/code.
- Свайп/Reply-панель, retry, owner/account/dialog checks и forum routing сохранены; локальная история не меняется.
- Новые TL regression tests и Android-чек-лист: DELETED_BLOCKQUOTES.md.

## 0.2.4 — кликабельный автор внутри цитаты (debug APK)

- Одна blockquote содержит имя и текст; добавлен только один перенос перед ответом.
- Настоящий TL_inputMessageEntityMentionName по исходному sender user ID/InputUser; без пригодного peer — обычное имя без фиктивной ссылки.
- Native users cache подгружается на storage queue при необходимости; owner/account/dialog сохраняются.
- Канонический порядок вложенных entities: quote → mention → bold. Body entities не изменяются, original history не переписывается.
- Добавлены TL-тесты sender ID/вложенности/fallback/Unicode/разделителя. Проверка нажатия в официальном Telegram требует Android UI; DELETED_MENTIONS.md.

- CI 43472506292 — success: JUnit task (55 tests), 13 SQLite tests, APK и подпись проверены. Реальный тест трёх условий в официальном Telegram пока NOT RUN; DELETED_MENTIONS.md.

## 0.2.5 — локальный Reply и проверки разрешений

- Новые ответы на удалённые сообщения внутри Tetogram используют штатное оформление Reply; серверная блок-цитата и упоминание сохраняются.
- Добавлены локальные связи в той же базе аккаунта, remap временных ID, восстановление при загрузке, проверка префикса, fallback после очистки исходника и штатный переход с подсветкой.
- Исправлены требования обоих media grants, обработка selected-only callback, gates MediaStore и повторная проверка notification grant. Добавлены Android 14 partial-access manifest/request и Photo Picker внешнего выбора.
- versionCode 7; applicationId и постоянная подпись без изменений. Добавлены JVM/SQLite регрессии и безопасная диагностика; подробности/ручной checklist в LOCAL_REPLY_AND_PERMISSIONS.md.
- Реальная причина возможного отзыва полного grant при обновлении на устройстве не подтверждена; ADB-сравнение подготовлено. Новые UI-сценарии не объявляются проверенными на Android.

- При cloud-проверке исправлена ошибочная точка вызова сохранения связи: вызов находится в подготовке SendMessageParams после создания отправляемого MessageObject, а не в ветке пересылки.

- Итоговый source bdb7ab3169f1918b6d56160b25ccc73d231f77e8 успешно собран в run 43484887361: JVM/TL task (70 методов), 19 SQLite tests, проверка APK и подписи пройдены. versionCode APK 79. Скачанный артефакт проверен отдельно, сертификат совпадает с 0.2.4, файл помещён в Android Download. Метаданные в APK-0.2.5.json; ручные Android/официальный клиент тесты NOT RUN.

## 0.3.0 — пользовательские разделы

- Добавлены независимые local-account разделы со стабильными UUID, именем/эмодзи, порядком, default, ручными чатами/исключениями и автоматическими правилами.
- Папки необязательны: прямой список без вкладок, серверные и локальные папки, optional общая вкладка, отдельный порядок и last-folder key. Серверные папки не создаются/не удаляются настройками раздела.
- Плавающие compose/camera заменены одной стрелкой с вертикальным прокручиваемым меню разделов; compose/camera/archive сохранены в ⋮.
- Нижняя навигация: Чаты / Настройки / Профиль; штатные Контакты и Звонки доступны через Настройки.
- Добавлены 19 JVM-регрессий конфигурации и документация SECTIONS.md. versionCode input 8; постоянная подпись и applicationId сохранены. Android UI пока требует ручной проверки.

- Убраны предложения добавить/скрыть вкладку звонков в CallLogActivity: они несовместимы с согласованными тремя нижними пунктами; остальные функции журнала не меняются.

## 0.3.1 — локализация, оформление, анимации и unread

- Исправлен источник LOC_ERR:null: отдельные Tetogram XML включены в native binary localization generation; CI проверяет все Tetogram-ключи и реальные APK assets/bindings, RU/EN fallback и внешние language packs сохраняются.
- Плавное stagger open/reverse close меню, вращение стрелки, cancellation tokens, системный duration scale, scroll threshold; long press открывает editor по owner/UUID с haptic.
- Круглый общий preview/menu component, любые Unicode emoji, theme icons, permissionless single photo picker, штатный avatar crop PhotoViewer без upload, локальный JPEG512 и checked-save/cancel/cleanup; безопасная миграция schema 1→2.
- Native unread/mute union badges без дубликатов, mandatory muted exclusion для пользовательских разделов, show/compact prefs и refresh по native событиям/expiry.
- Добавлен opt-out системный временный 📩 «Непрочитанные», не сохраняемый как обычный раздел: no folders, optional muted, active-empty retention и явный Back/выход.
- versionCode input 9, version 0.3.1. applicationId и постоянная подпись сохранены. Новые JVM/CI проверки добавлены; реальные Android gestures/crop/animations остаются NOT RUN агентом. Подробности — SECTIONS.md.

- По первому cloud-логу исправлена область видимости layout root при расчёте высоты меню. Для системного unread добавлен явный no-pagination guard, включая аккаунт без пользовательских разделов.
