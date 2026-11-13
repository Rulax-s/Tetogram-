# Результаты проверок

Дата: 2026-11-13. База Telegram 13.2.2: `21f7d109da6ab3506c9e9577f98c6a38771ab335`.

## Полученный APK

- [Успешный CI run 43415735775](https://github.com/Rulax-s/Tetogram-/actions/runs/43415735775).
- Исходный commit APK: `31d813f1fa818e678fcb5ba263180c58c1822588`. Последующие коммиты документации не меняют этот APK.
- [Артефакт Tetogram-debug (ZIP)](https://github.com/Rulax-s/Tetogram-/actions/runs/43415735775/artifacts/14032027793), доступен до 2026-11-27.
- Файл: `Tetogram-debug.apk`, размер 70514065 байт, Android API 23+, ARM64.
- SHA-256: `2c071aca3c458ed5b5266bcabb61e0e4d4d43fe74188d81a4e276c129d48c4c1`.
- Локальная копия: `artifacts/run-43415735775/Tetogram-debug.apk`.
- Копия для установки на телефоне: `/sdcard/Download/Tetogram-debug.apk`; существующие файлы не заменялись.
- Контрольная сумма скачанного APK и копии в Download совпала с checksum из CI.
- Все машинно-читаемые данные: `APK-0.1.0.json`.

## Реально выполненные проверки

- Debian/proot aarch64: Git, Python, curl; JDK/Android SDK/NDK на телефон не устанавливались. Сборка выполнена на GitHub runner ubuntu-24.04, JDK 17, SDK/Build Tools 36, NDK 27.2.12479018, CMake 3.22.1.
- Все 22 необходимых checkout подмодулей совпадают с закреплёнными SHA, включая вложенный QuickJS. Test262 пропущен согласно upstream `update = none`; Android CMake его не использует.
- `check-project.py`, включая проверку подмодулей, завершился успешно локально и в облаке. Проверены source identity, accountType, contact MIME routing, authorities, permissions и ссылки брендинга.
- `actionlint 1.7.12`, Python syntax checks, `bash -n gradlew`, Wrapper SHA-256 и `git diff --check` — PASS.
- Подтверждены наличие и формат owner Secrets без вывода значений. Firebase package из декодированного JSON соответствует `org.tetogram.messenger`. Это не тест доступности Telegram API или доставки FCM.
- Фактический Gradle `:TMessagesProj_App:tasks --all` подтвердил задачу `assembleAfatDebug`.
- `:TMessagesProj_App:assembleAfatDebug` — BUILD SUCCESSFUL.
- `verify-apk.py` на реальном APK — PASS: package `org.tetogram.messenger`, версия `0.1.0`, label Tetogram, debug-флаг, собственная ссылка иконки в merged manifest, уникальные provider authorities и custom permissions.
- XML manifest декодирован непосредственно из APK с помощью Android SDK `apkanalyzer`; проверяются структурированные атрибуты, а не формат текстового дампа. Полный manifest и metadata не выводятся в лог.
- Проверены шесть authorities: собственные FileProvider, NotificationImageProvider, CallNotificationSoundProvider, MlKitInitProvider, FirebaseInitProvider и AndroidX InitializationProvider; все имеют префикс `org.tetogram.messenger.`.
- Проверены четыре ELF64 little-endian AArch64-библиотеки: datastore_shared_counter, language_id_l2c_jni, tmessages.49 и twidget. Проверка повторена по скачанному APK.
- `apksigner verify` — PASS, схемы v1/v2. Debug-сертификат SHA-256: `85fb2f8e1b6fc5fe4132b868594de3628f889045e4505bac488ab7ece85714f8`. Это публичный отпечаток, не приватный ключ.
- Проверены отказы конфигурации при отсутствующих/повреждённых данных и официальном Firebase-образце; переносы base64 проверены с синтетической конфигурацией только в изолированном временном проекте.
- API Hash, Firebase JSON, токены и приватные подписные ключи не добавлялись в Git. Встроенные в APK API-данные принципиально доступны при анализе приложения; исходники содержат только ссылки на owner configuration.

## Что пока не проверено

Приложение не устанавливалось и не запускалось агентом. Авторизация/2FA, сообщения, медиа, push, несколько аккаунтов, звонки, секретные чаты, Premium, истории, боты, Mini Apps, визуальное оформление и совместная установка с Telegram требуют ручной проверки по MANUAL_TESTING.md. Реальные Telegram-аккаунты пользователя не использовались.

Статические идентификаторы для совместной установки проверены; это не заменяет проверку запуска и работы на телефоне. Ограничения passkeys, Firebase PNV, Google Play Billing и внешних провайдеров перечислены в TROUBLESHOOTING.md. Debug APK предназначена для тестирования. Для обновлений с сохранением данных нужен тот же ключ подписи; см. BUILD.md.

## История устранения ошибок

1. [Run 43408428079](https://github.com/Rulax-s/Tetogram-/actions/runs/43408428079): runner не стартовал по billing issue GitHub. Владелец устранил блокировку; исходники для этого не менялись.
2. [Run 43412734080](https://github.com/Rulax-s/Tetogram-/actions/runs/43412734080): подготовка Secrets, Gradle tasks и компиляция прошли. Первоначальный проверяющий скрипт выдал ложную ошибку authorities при разборе текстового дампа aapt2. Проверка заменена на namespaced XML из самого APK; отрицательные тесты подтверждают отклонение чужих authorities/permissions и non-debug manifest.
3. [Run 43415735775](https://github.com/Rulax-s/Tetogram-/actions/runs/43415735775): source-checks, сборка, проверки APK и загрузка artifacts — success. Код приложения и требования уникальности при исправлении проверки не ослаблялись.

## Как повторить сборку

Actions → Tetogram Android APK → Run workflow → master, либо:

```bash
GH_CONFIG_DIR=/data/data/com.termux/files/home/.config/gh \
  gh workflow run android-build.yml --repo Rulax-s/Tetogram- --ref master
```

## 0.2.0: финальная экспериментальная сборка

- [CI 43435783620 — success](https://github.com/Rulax-s/Tetogram-/actions/runs/43435783620), исходный commit `df279bb6a302416a720447705d4e96d684510ce1`.
- [Скачать артефакт APK](https://github.com/Rulax-s/Tetogram-/actions/runs/43435783620/artifacts/14044991271), доступен до 2026-11-27. Метаданные/контрольная сумма — APK-0.2.0.json.
- 11 SQLite contract tests: PASS. JUnit: HistoryPolicyTest (4), HistoryPagePolicyTest (4), фактические compileDebugUnitTestJavaWithJavac/testDebugUnitTest — PASS.
- ARM64 assembleAfatDebug — BUILD SUCCESSFUL (14m 10s). Version 0.2.0, package, label, icon, 6 provider authorities, custom permissions, ELF64/AArch64 и apksigner v1/v2 — PASS.
- Публичный сертификат SHA-256 совпадает с постоянным локальным ключом: `147ade1b03cc3875cea9ac46f4fd86bd6ee374f48afdcd674bd619de5d3e621d`.
- Логи сохранены в build-logs/run-43435783620; APK — artifacts/run-43435783620/Tetogram-debug.apk. CI-сумма, скачанный файл и отдельная копия /sdcard/Download/Tetogram-0.2.0-debug.apk сверены. ELF заголовки повторно проверены локально.
- SQL reopen не подтверждает Android background lifecycle. Все 12 runtime-сценариев новой функции остаются непроверенными; см. MANUAL_TESTING.md. Реальные аккаунты агентом не использовались.
- Старый приватный ключ 0.1.0 утрачен: обновление установленной 0.1.0 поверх новым сертификатом невозможно. Ни приложение, ни пользовательские данные не удалялись. Секреты нового постоянного ключа настроены в CI и не коммитились.
- История редактирования недоступна; файлы медиа не дублируются в архив, поэтому доступность вложений зависит от штатного кеша. Полный список ограничений и изменённых файлов — DELETED_HISTORY.md.

## 0.2.1: исправление критических дефектов отображения

- [CI 43443980427 — success](https://github.com/Rulax-s/Tetogram-/actions/runs/43443980427), APK source `b0a29bea5f445faf6aac068b0f753b23ccde7bd8`.
- 13 SQLite contract tests, JUnit task с 24 тестами — PASS; compileDebugUnitTestJavaWithJavac и testDebugUnitTest реально выполнены. Тесты модели не являются RecyclerView/Activity instrumentation.
- ARM64 assembleAfatDebug — BUILD SUCCESSFUL, 14m 47s. APK package/version 0.2.1/label/icon/providers/permissions/ELF/signature — PASS.
- Скачанный APK и отдельная копия `/sdcard/Download/Tetogram-0.2.1-debug.apk` сверены с CI sha256; source commit проверен. Четыре ELF64/AArch64-библиотеки повторно проверены локально.
- Сертификат совпал с метаданными ранее выданной 0.2.0. Package и DB-схема сохранены, версия повышена; реальная установка обновления на Android агентом не выполнялась.
- Артефакт, размер, SHA-256, сертификат: APK-0.2.1.json. Логи: build-logs/run-43443980427.
- Причины обеих ошибок, production policies, изменённые файлы и оставшиеся Android-регрессии — MESSAGE_INTEGRITY.md. Авторизация, отправка, уведомления, Mini Apps и другие runtime-функции на этой сборке агентом не проверялись.
- GitHub Secrets, Android-настройки и пользовательские данные не менялись, force push не выполнялся. Диагностическое логирование сообщений/сессий не добавлялось.

## 0.2.2: native Reply на удалённые копии

- [CI 43454426141 — success](https://github.com/Rulax-s/Tetogram-/actions/runs/43454426141), APK source `d7dbc837eaa65c55607fda62359b360789b3a939`.
- JUnit task с 40 тестами: 24 прежних regressions, 12 DeletedReplyPolicy, 4 DeletedReplyProtocol; compileDebugUnitTestJavaWithJavac/testDebugUnitTest выполнены. Реальные TL entities/сериализация forum header включены. 13 SQLite tests — PASS.
- ARM64 assembleAfatDebug — BUILD SUCCESSFUL, 9m 34s. Package/version 0.2.2/label/icon/providers/permissions/ELF/signature — PASS.
- Скачанный APK и отдельная копия `/sdcard/Download/Tetogram-0.2.2-debug.apk` совпали с CI sha256; SOURCE_COMMIT сверён. Четыре ELF64/AArch64 библиотеки повторно проверены локально.
- Сертификат совпал с ранее выданной 0.2.1; package/DB-схема сохранены, version code повышен. Метаданные/ссылка/checksum — APK-0.2.2.json; логи — build-logs/run-43454426141.
- Android gestures, preview/cancel, реальная сеть/retry, restart/draft, установка обновления и отображение в официальном Telegram ещё требуют проверки владельцем. Реальные аккаунты агентом не использовались.
- Указанный AyuGram c9c317cf изучен; архитектура, список изменений, поддержанные типы и Android-чек-лист — DELETED_REPLIES.md.
- Source checks и actionlint пройдены. Force push и изменение GitHub Secrets не выполнялись; пользовательские APK/данные не удалялись.
