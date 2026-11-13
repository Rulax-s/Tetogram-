# Tetogram 0.1.0

Tetogram — независимый неофициальный Android-клиент Telegram на базе официальных исходников [DrKLO/Telegram](https://github.com/DrKLO/Telegram). Не связан с Telegram и не является его официальным приложением.

Цель первой версии — сохранить существующие чаты, навигацию, темы, медиа, звонки и MTProto. Собственный сервер сообщений, Bot API и демонстрационные чаты не используются. Новые пользовательские функции не добавлены.

- Application ID: `org.tetogram.messenger`, включая debug без `.beta`.
- Версия: `0.1.0`, исходная версия Telegram: `13.2.2`.
- Android 6.0+ (API 23), первая конфигурация — ARM64 (`arm64-v8a`).
- Русский язык при первом запуске; остальные переводы и выбор языка сохранены.
- Временная иконка — белая буква T на фиолетовом фоне, без логотипа Telegram.

**Статус:** ARM64 debug APK 0.1.0 успешно собрана и проверена в [GitHub Actions 43415735775](https://github.com/Rulax-s/Tetogram-/actions/runs/43415735775). [Скачать ZIP с Tetogram-debug.apk](https://github.com/Rulax-s/Tetogram-/actions/runs/43415735775/artifacts/14032027793) (Artifacts доступны до 2026-11-27). Установка, вход и работа функций Telegram на устройстве ещё не проверены. Подробности — [результаты проверок](docs/VALIDATION.md), [метаданные APK и SHA-256](docs/APK-0.1.0.json).

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
