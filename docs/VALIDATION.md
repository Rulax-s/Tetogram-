# Результаты проверок

Дата подготовки: 2026-11-13. Базовый SHA: `21f7d109da6ab3506c9e9577f98c6a38771ab335` (Telegram 13.2.2).

## Выполнено

- Окружение: Debian/proot, aarch64; Git 2.47.3, Python, curl. На старте доступно около 1 ГБ RAM и 28 ГБ диска; тяжёлая сборка вынесена в CI.
- Получен основной официальный Git snapshot, сохранены исходная история/лицензия и remote upstream. Все 22 необходимых checkout подмодулей, включая вложенный QuickJS, совпадают с закреплёнными SHA; snapshot — `SUBMODULES-CHECKOUT.txt`. Test262 пропущен по upstream `update = none`, в Android CMake он не используется.
- `python3 scripts/check-project.py` — PASS: исходные package/version/label, ARM64-конфигурация, debug-подпись без upstream ключа, provider authorities, custom permissions, account type и contact MIME routing, отсутствие tracked Firebase JSON и наличие документации. Проверены ссылки брендинга в launcher, splash, intro и заголовках; визуальный результат на Android не проверен.
- `actionlint 1.7.12 .github/workflows/android-build.yml` — PASS.
- Python AST-проверка всех трёх scripts — PASS; `bash -n gradlew` — PASS.
- SHA-256 локального Wrapper JAR — PASS. В Wrapper добавлена официальная SHA-256 Gradle 8.13 distribution.
- `git diff --check` — PASS. Проверено наличие всех 14 ARM64 prebuilt `.a`, на которые ссылается основной CMake. Это не проверка линковки.
- Проверены три отказа `prepare-ci.py`: отсутствующая конфигурация, некорректный base64/JSON, официальный Firebase-проект вместо собственного. Файлы с тестовыми credentials не создавались, APK с ними не собиралась.
- Новые ключи локализации `TetogramAbout` и `TetogramIntro` проверены на Java hash collisions среди всех строк текущего дерева. Облачный перевод не заменяет брендинг, описание использует встроенную систему language packs.
- Собственные API/Firebase/подписные секреты не предоставлялись, не записывались и не публиковались. Из текущего дерева удалены upstream Firebase-образцы; публичная upstream-история с образцами сохранена. Это не полный аудит всех upstream-файлов на секреты.

## Не выполнено / не подтверждено

- Локальный вызов `./gradlew :TMessagesProj_App:tasks --all` завершился до запуска Gradle: `JAVA_HOME is not set and no 'java' command could be found in your PATH`. Лог: игнорируемый `build-logs/local-gradle-tasks.log`.
- В новом CI run 43412734080 фактический список Gradle подтвердил `assembleAfatDebug`; проверка Wrapper и списка задач завершилась успешно.
- Изменения опубликованы в `Rulax-s/Tetogram-`, ветка `master`, через существующую авторизацию GitHub CLI Termux. В репозитории подтверждено наличие трёх Secret names; значения не читались и не выводились.
- [Первый запуск Actions 43408428079](https://github.com/Rulax-s/Tetogram-/actions/runs/43408428079) для commit `e070528f793132bafa6cc4efcdbfb5b4d8ffe965` завершился failure до выделения runner: `The job was not started because your account is locked due to a billing issue.` Шагов и build logs нет; сохранена аннотация GitHub. Исправлять исходники по этому сообщению оснований нет.
- [Повторный запуск 43412734080](https://github.com/Rulax-s/Tetogram-/actions/runs/43412734080): runner стартовал; cloud source-checks и Prepare owner configuration прошли. Firebase package name из Secret совпал с applicationId `org.tetogram.messenger`; Telegram API-поля прошли проверку формата. Реальные значения не выводились и локально не извлекались. Подмодули и SDK/NDK установлены. Выполняется компиляция APK.
- Дополнительно проверено принятие base64 с переносами строк в изолированном временном проекте с синтетической конфигурацией; реальные Secrets и рабочий Firebase-файл не использовались.
- APK, merged manifest, конечная подпись и состав `.so` не проверены на реальном артефакте. Для этого подготовлен `scripts/verify-apk.py` в CI.
- Приложение не устанавливалось. Авторизация, сообщения, медиа, уведомления, несколько аккаунтов, звонки, Premium, Mini Apps и совместная установка не проверены. Реальные аккаунты пользователя не использовались.

После первого cloud run обновить этот файл, указать ссылку на run/artifact, SHA исходников/APK, результаты verify-apk.py и отдельно результаты MANUAL_TESTING. Не заменять статус на «готово» только по успеху статических проверок.

## Действие владельца для продолжения

Откройте https://github.com/settings/billing и устраните проблему, на которую указывает GitHub. Аннотация не раскрывает конкретную причину блокировки; платёжные данные агент не запрашивает и не меняет. После снятия блокировки запустите workflow в master:

```bash
GH_CONFIG_DIR=/data/data/com.termux/files/home/.config/gh \
  gh workflow run android-build.yml --repo Rulax-s/Tetogram- --ref master
```

Или используйте Actions → Tetogram Android APK → Run workflow. Повтор без снятия блокировки не исправляет сборку.
