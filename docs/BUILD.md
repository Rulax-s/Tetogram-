# Сборка Tetogram

## Проверенная структура upstream

Зафиксированный commit указан в `UPSTREAM.json`. Изучены README, LICENSE, settings.gradle, корневой и модульные build.gradle, gradle.properties, Dockerfile, manifests, BuildVars и buildSrc.

| Компонент | Значение в исходниках |
| --- | --- |
| Gradle Wrapper | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| JDK в upstream Dockerfile / CI Tetogram | 17 |
| compileSdk / targetSdk | 36 / 36 |
| Build Tools | 36.0.0 |
| NDK | 27.2.12479018 |
| CMake | 3.22.1 |
| minSdk | 23 |
| Модуль приложения | TMessagesProj_App |
| Flavor / build type | afat / debug |
| Целевая ABI | arm64-v8a |

Задача `:TMessagesProj_App:assembleAfatDebug` подтверждена фактическим выводом Gradle в CI runs 43412734080 и 43415735775: шаг `:TMessagesProj_App:tasks --all` и проверка имени завершились успешно. Локально на телефоне JDK/SDK не устанавливались. Наличие задачи отдельно от успешной компиляции APK.

`TMessagesProj` теперь библиотека, а APK выпускает `TMessagesProj_App`. Также существуют AppHuawei, AppHockeyApp, AppStandalone, AppTests, jlatexmath и Media3. Первая поддерживаемая конфигурация Tetogram — только `TMessagesProj_App`/afatDebug; остальные модули сохранены, их выпуск требует отдельной настройки сервисов и подписи.

## Действия владельца

В Rulax-s/Tetogram- репозиторий и три обязательных Secret names уже настроены. Прежняя billing-блокировка снята владельцем: runner нового запуска 43412734080 стартовал, а проверка owner configuration прошла успешно. Повторно получать API-данные для этой ошибки не требуется.

1. На https://my.telegram.org откройте **API development tools**, создайте своё приложение и получите `api_id`/`api_hash`. Не используйте значения официального клиента.
2. В https://console.firebase.google.com создайте собственный проект, зарегистрируйте Android-приложение **org.tetogram.messenger**, настройте Cloud Messaging и скачайте `google-services.json`. Не переименовывайте package_name внутри чужого JSON: это не создаёт Firebase-приложение.
3. Создайте на GitHub [fork DrKLO/Telegram](https://github.com/DrKLO/Telegram/fork) с именем **Tetogram-**. Это сохранит полную upstream-историю на сервере без её загрузки на телефон. Репозиторий уже создан владельцем; Secrets настроены владельцем в GitHub. Их значения нельзя получить через API, проверка выполняется внутри CI. Обнаружена действующая авторизация GitHub CLI в конфигурации Termux; Debian использует её через GH_CONFIG_DIR без копирования токена. Подключение GitHub доступно для чтения, но попытка записи через него вернула 403; публикация выполняется штатным CLI.
4. В репозитории откройте **Settings → Secrets and variables → Actions → New repository secret** и добавьте:

| Secret | Требование |
| --- | --- |
| TELEGRAM_API_ID | Обязателен, собственный числовой ID |
| TELEGRAM_API_HASH | Обязателен, собственная строка из 32 hex-символов |
| GOOGLE_SERVICES_JSON_BASE64 | Обязателен, base64 собственного Firebase JSON |
| DEBUG_KEYSTORE_BASE64 | Рекомендуется для обновления APK без удаления данных |
| GOOGLE_MAPS_API_KEY | Необязателен; без него карты Google не считаются рабочими |
| GOOGLE_AUTH_CLIENT_ID | Необязателен; собственный OAuth Web Client ID для интеграции Google |
| SAFETYNET_KEY | Необязателен; только собственная конфигурация поддерживаемого сервиса |

Подготовьте base64 **в локальный файл**, не выводя в чат:

```bash
base64 -w 0 /путь/к/google-services.json > /tmp/tetogram-firebase.base64
```

Скопируйте содержимое в Secret через GitHub. Не добавляйте этот файл в Git. Firebase JSON и ключи тоже попадают в APK; Secrets защищают репозиторий и логи, а не от анализа приложения. Ограничьте ключи на стороне Google по своему package name, сертификату и API.

Для 0.2.0 обязательны оба Secrets: `DEBUG_KEYSTORE_BASE64` (PKCS12) и `DEBUG_KEYSTORE_PASSWORD`. Постоянный ключ уже настроен. Не заменяйте его при следующих сборках: это нарушит совместимость обновлений. Alias — `androiddebugkey`. Приватный пароль и ключ храните в резервной копии вне Git. CI прекращает сборку при отсутствии конфигурации подписи, не создавая случайный ключ.

Зарегистрируйте SHA-1/SHA-256 сертификата в собственном Firebase/Google OAuth, если интеграция этого требует. Для выпуска нужен отдельный release-ключ; release CI пока не настроен.

## Публикация локального репозитория

Рабочая директория: `~/projects/Tetogram`, ветка `master`. Remote `upstream` указывает на DrKLO/Telegram, `origin` подготовлен для `Rulax-s/Tetogram-`. Репозиторий доступен через подключение GitHub с правом записи; default branch — master. Сначала проверьте `git status` и подмодули, затем выполните авторизованный push в созданный fork своим обычным способом:

```bash
cd ~/projects/Tetogram
git remote -v
# origin уже подготовлен: https://github.com/Rulax-s/Tetogram-.git
git push -u origin master
```

После push установите ветку `master` как default branch в настройках fork и разрешите Actions в его вкладке Actions. При публикации в совершенно пустой репозиторий неглубокая история может быть отклонена сервером; fork уже содержит родительский commit и избегает этой проблемы.

Нужна собственная GitHub-авторизация с правом записи исходников и workflows. Не помещайте токен в URL remote или в файлы проекта. Если push запросит пароль, используйте штатную GitHub-аутентификацию/credential manager; не отправляйте токен в переписку. Для подключения репозитория к Codex разрешите GitHub-приложению доступ к Tetogram.

Клон неглубокий для экономии трафика. Перед переносом официальных обновлений:

```bash
git fetch --unshallow upstream
git fetch upstream master
```

Не выполняйте слепой reset на upstream: потеряются изменения форка. Сливайте или переносите обновления отдельной веткой с повторными проверками.

## GitHub Actions

Workflow: `.github/workflows/android-build.yml`. Push исходников и ручной `workflow_dispatch` запускают сборку; pull request выполняет только статическую проверку без Secrets. Если это первая публикация, workflow должен находиться в default branch, чтобы появилась кнопка ручного запуска.

Последовательность: статические проверки → проверка конфигурации → рекурсивные подмодули закреплённых версий → JDK 17 → SDK 36/Build Tools 36/NDK 27.2/CMake 3.22.1 → кэш зависимостей → проверка wrapper и списка задач → ARM64 debug APK → проверка package/version/label/authorities/permissions/подписи/.so → Artifacts. Кэшируются только скачанные Gradle/Maven-зависимости и Wrapper; Gradle build/configuration cache отключены, чтобы не кэшировать сгенерированные API-поля проекта. Логи сохраняются и при ошибке, но не содержат намеренного вывода конфигурации.

Использование actions сверено с [checkout](https://github.com/actions/checkout), [setup-java](https://github.com/actions/setup-java), [setup-gradle](https://github.com/gradle/actions), [setup-android](https://github.com/android-actions/setup-android) и [upload-artifact](https://github.com/actions/upload-artifact). Workflow выполнен успешно в run 43415735775; APK и build logs загружены в Artifacts. Ссылки, SHA-256 и проверенный исходный commit приведены в VALIDATION.md.

Откройте **Actions → Tetogram Android APK → Run workflow**. Успех загрузки исходников или списка задач не равен успеху сборки. При ошибке скачайте `Tetogram-build-logs`, исправьте причину и повторите сборку.

Проверенный выход успешной сборки:

```text
TMessagesProj_App/build/outputs/apk/afat/debug/Tetogram-debug.apk
```

Артефакт `Tetogram-debug` содержит `Tetogram-debug.apk`, SHA-256 и commit исходников. Ссылка на реальный artifact появляется только после успешного запуска.

## Локальная сборка в подходящем Linux x86_64

На телефон SDK/NDK не установлены. Стандартный Linux Android NDK содержит host-инструменты x86_64 и не является готовой сборочной средой для Debian ARM64/proot. Dockerfile upstream исследован как описание окружения; Android Studio для CI не требуется.

В Linux с JDK 17 и Android SDK:

```bash
git submodule update --init --recursive --depth 1
python3 scripts/check-project.py --submodules
sdkmanager 'platforms;android-36' 'build-tools;36.0.0' 'ndk;27.2.12479018' 'cmake;3.22.1'
cp tetogram.properties.example tetogram.properties
```

Заполните игнорируемый `tetogram.properties` собственными данными и положите собственный JSON в `TMessagesProj_App/google-services.json`. Можно вместо properties использовать переменные окружения. Затем:

```bash
./gradlew :TMessagesProj_App:tasks --all
./gradlew --no-daemon --no-build-cache --no-configuration-cache --max-workers=2 \
  '-Dorg.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g' \
  :TMessagesProj_App:assembleAfatDebug --stacktrace
python3 scripts/verify-apk.py TMessagesProj_App/build/outputs/apk/afat/debug/Tetogram-debug.apk
```

## Установка непосредственно на телефон

После **успешной** сборки войдите в GitHub в браузере телефона, откройте выполненный workflow и скачайте ZIP `Tetogram-debug` из Artifacts. Распакуйте ZIP, при желании сверьте SHA-256, откройте `Tetogram-debug.apk` файловым менеджером и разрешите установку из этого источника по запросу Android. Официальный Telegram удалять не нужно. Перед входом ознакомьтесь с неофициальным статусом клиента и выполните `MANUAL_TESTING.md`.

APK имеет другой package, собственные account type/provider identifiers и отдельный каталог данных. Переходы `tg://` и `t.me` сохранены: Android может предложить выбрать клиент — это ожидаемо и не заменяет официальный Telegram. Совместная установка должна быть подтверждена реальным тестом.

Первая проверенная APK также скопирована в Android Download: `/sdcard/Download/Tetogram-debug.apk`. Откройте её файловым менеджером и подтвердите установку из этого источника. Агент установку и вход не выполнял.

## Постоянная подпись 0.2.0

После подтверждения владельца, что старый ключ не сохранён, создан отдельный постоянный тестовый PKCS12-ключ. Его приватная локальная копия: TMessagesProj/config/tetogram-debug.keystore (chmod 600, игнорируется Git). В GitHub настроены Secrets DEBUG_KEYSTORE_BASE64 и DEBUG_KEYSTORE_PASSWORD, материал ключа не выводился и не коммитился. CI сохраняет ключ только внутри рабочего проекта, не заменяя общий ~/.android/debug.keystore. Alias: androiddebugkey. Случайный пароль хранится в DEBUG_KEYSTORE_PASSWORD (GitHub Secret), а локально — в игнорируемом tetogram.properties с chmod 600; реальное значение не коммитилось. Для release нужен отдельный ключ.

Владелец сообщил, что прежний ключ 0.1.0 не сохранён. Подпись первой 0.2.0-сборки отличалась от 0.1.0; восстановить приватный ключ по APK/сертификату невозможно. Поэтому новая 0.2.0 не обновит установленную 0.1.0 поверх. Агент не удаляет приложение, не очищает его данные и не обходит проверку подписи Android. Владелец самостоятельно решает вопрос чистой установки после сохранения нужных данных либо использует отдельное устройство. Будущие сборки 0.2.x с новым постоянным ключом смогут обновляться друг поверх друга.

### Установка экспериментальной 0.2.0 на телефоне

После успешного CI скачайте ZIP `Tetogram-debug` в разделе Artifacts выбранного run (GitHub может потребовать вход). Распакуйте ZIP в файловом менеджере и откройте `Tetogram-debug.apk`. Разрешение установки из этого источника Android запрашивает у владельца устройства. Не включайте его заранее для других источников.

Сертификат 0.2.0 отличается от установленной 0.1.0. Android отклонит обновление поверх старой версии. Проверка на другом устройстве сохраняет текущую установку и её данные. Если владелец выберет чистую установку на том же устройстве, сначала следует учесть потерю локальных данных старого приложения: секретные чаты, кеш и локальная история не восстанавливаются обычным входом в облачный аккаунт. Автоматическое удаление/очистка приложением или скриптами не выполняется.

После первого входа: Настройки → Tetogram Labs → Сохранять удалённые сообщения. Прочитайте предупреждение и включите сохранение вручную; до этого снимки не создаются. Для проверки используйте обычное входящее сообщение, сохранённое до удаления. Чужие сообщения, не полученные клиентом, функция восстановить не может. Полный план проверки — MANUAL_TESTING.md.

Публичный SHA-256 нового постоянного debug-сертификата 0.2.0: `147ade1b03cc3875cea9ac46f4fd86bd6ee374f48afdcd674bd619de5d3e621d`. Он не является приватным ключом. При подключении сервисов, проверяющих сертификат приложения, используйте этот отпечаток; APK должен пройти независимую проверку подписи в CI.

### Обновление 0.2.0 → 0.2.1

Сборка 0.2.1 использует тот же постоянный сертификат, что ранее выданная APK из run 43435783620. Package сохранён, version code повышен, DB-схема неизменна. Откройте `/sdcard/Download/Tetogram-0.2.1-debug.apk` либо скачайте/распакуйте артефакт из run 43443980427. Удалять приложение/локальную историю для обновления с этой 0.2.0 не нужно. Фактическое обновление и регрессионные сценарии проверяет владелец на Android. Для других старых APK с иным сертификатом ограничения подписи остаются прежними; не удаляйте данные ради обхода ошибки установки.
