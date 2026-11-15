# Tetogram

Tetogram — независимый неофициальный Android-клиент Telegram на базе официальных исходников [DrKLO/Telegram](https://github.com/DrKLO/Telegram). Не связан с Telegram и не является его официальным приложением.

Цель первой версии — сохранить существующие чаты, навигацию, темы, медиа, звонки и MTProto. Собственный сервер сообщений, Bot API и демонстрационные чаты не используются. Дополнительные возможности Tetogram Labs и разделов описаны ниже; новые UI-сценарии требуют проверки на устройстве.

**Полный набор 8 иконок / code189:** [Скачать APK (ZIP)](https://github.com/Rulax-s/Tetogram-/actions/runs/43716941404/artifacts/14163223260) · [успешный CI](https://github.com/Rulax-s/Tetogram-/actions/runs/43716941404). Найден повторно сохранённый JPEG и подключён как летняя Тето по указанию владельца. Все восемь сезонных aliases имеют реальные изображения; Summer больше не заблокирована. Исходник 1536×1536 скопирован без перекодирования, байты JPEG внутри APK сверены с подготовленным файлом. Проверено: 241 JVM, 19 SQLite, 31 source contracts, 4 PNG/JPEG tests, APK identity/signature/ARM64/8 launcher variants/349 EN-RU keys. Android/One UI/установка агентом **NOT RUN**. Файл на телефоне: `/sdcard/Download/Tetogram-0.4.0-189-debug.apk`. Устанавливать поверх прежней версии без удаления приложения. [Метаданные](docs/APK-0.4.0-eight-icons.json), [JUnit](docs/TESTS-0.4.0-eight-icons.json), [изменённые файлы](docs/CHANGED-0.4.0-eight-icons.txt), [ручной checklist](docs/SEASONAL_LAUNCHER.md). Артефакт до 2026-11-29.

**Предыдущая сборка code179 (7/8 изображений):** [Скачать APK (ZIP)](https://github.com/Rulax-s/Tetogram-/actions/runs/43711390552/artifacts/14161670900) · [успешный CI](https://github.com/Rulax-s/Tetogram-/actions/runs/43711390552). Общие настройки APK, Auto/manual с запомненным сезоном, native aliases, галерея/предпросмотр и отдельный экран личных ярлыков. В этой исторической сборке было 7/8 изображений; летняя Тето добавлена в code189 выше. Проверено: 241 JVM tests (24 launcher-focused), 19 SQLite, 31 source contracts, APK package/certificate/ARM64/8 canonical components и 349 EN/RU keys. Android/Samsung/открытие из launcher/уведомления/deep links агентом **NOT RUN**. Файл на телефоне: `/sdcard/Download/Tetogram-0.4.0-179-debug.apk`; устанавливать поверх прежней версии, не удаляя приложение. [Реализация/ограничения/ручная приёмка](docs/SEASONAL_LAUNCHER.md), [подготовленные файлы](assets/launcher/seasonal/README.md), [APK metadata](docs/APK-0.4.0-seasonal-launcher.json), [JUnit](docs/TESTS-0.4.0-seasonal-launcher.json), [изменённые файлы](docs/CHANGED-0.4.0-seasonal-launcher.txt). Артефакт до 2026-11-29.

**Способ открытия разделов / code159:** [Скачать APK (ZIP)](https://github.com/Rulax-s/Tetogram-/actions/runs/43701619455/artifacts/14157183821) · [успешный CI](https://github.com/Rulax-s/Tetogram-/actions/runs/43701619455). В разделе Tetogram Labs → Разделы можно выбрать прежнюю плавающую стрелку (по умолчанию) или повторное нажатие активной вкладки «Чаты». Один SectionMenu, настройки по UID аккаунта, привязка к кнопке и штатные long press/бейджи. Проверено: 221 JVM tests, включая 11 новых по opening mode; 19 SQLite, 22 source contracts, APK package/certificate/ARM64 и 335 EN/RU keys. Android navigation/animations/Insets и установка обновления агентом **NOT RUN**. Файл на телефоне: `/sdcard/Download/Tetogram-0.4.0-159-debug.apk`; устанавливать поверх прежней версии без удаления приложения. [Реализация и ручной checklist](docs/SECTION_OPENING.md), [APK metadata](docs/APK-0.4.0-sections-opening.json), [JUnit](docs/TESTS-0.4.0-sections-opening.json), [изменённые файлы](docs/CHANGED-0.4.0-sections-opening.txt). Артефакт до 2026-11-29.

**Редактор оформления 0.4.0 / code149:** [Скачать ZIP с APK](https://github.com/Rulax-s/Tetogram-/actions/runs/43698098220/artifacts/14156035977) · [успешная сборка](https://github.com/Rulax-s/Tetogram-/actions/runs/43698098220). Отдельные категории, область/источник наследования, закреплённые Apply/Cancel, живой preview, native палитра и сезонные значки. Проверено: 210 JVM tests (94 appearance), 19 SQLite, 14 source contracts, APK identity/signature/ARM64 и 331 EN/RU localization keys. Android UI/One UI/кодеки/жесты **NOT RUN**. Файл на телефоне: `/sdcard/Download/Tetogram-0.4.0-149-debug.apk`. [Изменения и ручная приёмка](docs/APPEARANCE_EDITOR.md), [иконки](docs/ICON_ASSETS.md), [метаданные APK](docs/APK-0.4.0-editor.json), [реальный JUnit report](docs/TESTS-0.4.0-editor.json), [файлы](docs/CHANGED-0.4.0-editor.txt). Установить поверх прежней версии без удаления приложения; package и сертификат сохранены. Артефакт до 2026-11-29.

- Application ID: `org.tetogram.messenger`, включая debug без `.beta`.
- Версия: `0.4.0`, текущая сборка — code189; предыдущая сборка — code179. Исходная версия Telegram: `13.2.2`.
- Android 6.0+ (API 23), первая конфигурация — ARM64 (`arm64-v8a`).
- Русский язык при первом запуске; остальные переводы и выбор языка сохранены.
- Значок по умолчанию — белая T на фиолетовом фоне; восемь сезонных Tetogram/Mikugram вариантов и персонализированный ярлык описаны в ICON_ASSETS.md.

**Тестовая APK 0.4.0, versionCode 139:** [скачать ZIP с APK](https://github.com/Rulax-s/Tetogram-/actions/runs/43675297320/artifacts/14146850490) (до 29 ноября 2026), [успешная сборка](https://github.com/Rulax-s/Tetogram-/actions/runs/43675297320), [метаданные/контрольная сумма](docs/APK-0.4.0.json). Копия на телефоне: `/sdcard/Download/Tetogram-0.4.0-139-debug.apk`. 189 выполненных JVM-тестов (73 appearance), 19 SQLite, 6 source Insets contracts и 279 EN/RU bindings прошли. Подпись совпадает с 0.3.1, package сохранён; установка поверх старой APK без удаления приложения. Android UI/codec/gestures/сенсоры агентом NOT RUN; полная приёмка исходного ТЗ не объявляется. [Поддерживаемые параметры/ограничения](docs/APPEARANCE_KEYS.md), [ручная матрица](docs/APPEARANCE_MANUAL_TESTING.md), [изменённые файлы](docs/CHANGED-0.4.0.txt).

**Разработка 0.4.0:** отдельный локальный appearance storage, наследование global/section/folder/chat/topic, семь пресетов, редакторы и native preview, индивидуальные цвета и формы сообщений, фото/видеофоны, баннер собственного профиля, персонализированный ярлык, переносимые пресеты. Полная спецификация ещё в работе; подтверждённые ограничения и различие JVM/Android проверок — [APPEARANCE.md](docs/APPEARANCE.md). Настройки оформления не заменяют историю, сессию или конфигурацию разделов.

**Статус 0.3.1:** исправлены языковые пакеты Tetogram (LOC_ERR:null), добавлены плавное меню с long press, любые emoji, встроенные и пользовательские иконки со штатным avatar crop Telegram, unread-бейджи и временный раздел 📩 «Непрочитанные». Он не сохраняется как пользовательский раздел; папки необязательны. [Устройство разделов и ручные проверки](docs/SECTIONS.md). Миграция сохраняет UUID, чаты, папки и настройки 0.3.0.

**Исправление отступов, versionCode 109:** все экраны LabsPage подключены к native SimpleGlass и WindowInsets; поле поиска истории размещено внутри списка. Без фиксированных status-bar padding и глобальных изменений edge-to-edge. [Причина, реализация и Android checklist](docs/SETTINGS_INSETS.md).

[CI 43561916169](https://github.com/Rulax-s/Tetogram-/actions/runs/43561916169) успешно выполнил шесть статических Insets-contract проверок, JVM/TL-задачу (116 методов в исходниках), 19 SQLite tests, ARM64-сборку, проверку APK/подписи и 108 Tetogram-ключей в EN/RU packs. [Скачать ZIP с APK](https://github.com/Rulax-s/Tetogram-/actions/runs/43561916169/artifacts/14100819601) (до 2026-11-28). [Метаданные APK](docs/APK-0.3.1-insets.json), [изменённые файлы этого исправления](docs/CHANGED-0.3.1-insets.txt). Файл на телефоне: `/sdcard/Download/Tetogram-0.3.1-109-debug.apk`.

Версия приложения остаётся 0.3.1, versionCode APK повышен с 99 до 109. Application ID и постоянный сертификат сохранены; удалять приложение для обновления не требуется. Сообщения, локальная история и Reply не меняются. Android UI/отступы, установка обновления, темы/навигация/ориентации и жесты агентом **NOT RUN**: сборка не подтверждает визуальный результат. [Метаданные предыдущей 0.3.1](docs/APK-0.3.1.json), [Local Reply и разрешения](docs/LOCAL_REPLY_AND_PERMISSIONS.md).

Новый постоянный сертификат отличается от 0.1.0: старый ключ утрачен. Обновление поверх установленной 0.1.0 невозможно; приложение и его данные агентом не удалялись. Перед установкой прочитайте [BUILD.md](docs/BUILD.md). Версия 0.1.0 подтверждена владельцем как работающая.

Сборка требует собственных `api_id`/`api_hash` и Firebase Android-приложения. Данные официального Telegram не используются для сборки. Получить API-данные: [my.telegram.org](https://my.telegram.org). Секреты вводятся в GitHub Secrets, не в исходники и не в переписку. Значения, встроенные в APK, технически доступны при анализе APK.

Документация:

- [Сборка, публикация в GitHub и установка APK](docs/BUILD.md)
- [Архитектура](docs/ARCHITECTURE.md)
- [Будущие изменения](docs/CUSTOMIZATION.md)
- [Неисправности и ограничения сервисов](docs/TROUBLESHOOTING.md)
- [История изменений](docs/CHANGELOG.md)
- [Ручное тестирование](docs/MANUAL_TESTING.md)
- [Исходный commit и версия](docs/UPSTREAM.json)
- [Оригинальный README Telegram](docs/UPSTREAM_README.md)

Исходный код — GNU GPL v2 или более поздняя версия, см. [LICENSE](LICENSE). Уведомления авторов сохранены; сторонние библиотеки имеют собственные лицензии. При распространении APK предоставляйте соответствующие исходники с подмодулями, изменениями и инструкциями сборки. Соблюдайте [условия Telegram API](https://core.telegram.org/api/terms). Debug APK предназначена для тестирования, не для публикации в магазине.

[Tetogram Labs и сохранение удалённых сообщений](docs/DELETED_HISTORY.md): Android-сборка и автоматические проверки пройдены; runtime-проверки новых функций ещё не выполнены.
