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
- Наличие `assembleAfatDebug` пока подтверждено анализом DSL, а не выводом Gradle. CI проверяет фактический список до сборки.
- GitHub Actions не запускался: не обнаружены доступный репозиторий Tetogram и необходимые owner Secrets.
- APK, merged manifest, конечная подпись и состав `.so` не проверены на реальном артефакте. Для этого подготовлен `scripts/verify-apk.py` в CI.
- Приложение не устанавливалось. Авторизация, сообщения, медиа, уведомления, несколько аккаунтов, звонки, Premium, Mini Apps и совместная установка не проверены. Реальные аккаунты пользователя не использовались.

После первого cloud run обновить этот файл, указать ссылку на run/artifact, SHA исходников/APK, результаты verify-apk.py и отдельно результаты MANUAL_TESTING. Не заменять статус на «готово» только по успеху статических проверок.
