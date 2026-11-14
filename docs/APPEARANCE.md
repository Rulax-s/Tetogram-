# Tetogram 0.4.0 — персонализация (в разработке)

Это описание архитектуры и прогресса, не подтверждение готовности всех функций ТЗ или Android UI-тестов.

## Исследованные точки интеграции

- `Theme`/`ThemeColors`: global color tables, native строковые имена ключей, стандартные `.attheme`, fallback и day/night. ID Android/порядковые color IDs в переносимый формат не записываются.
- `ChatActivity.ThemeDelegate`: отдельные цвета, клонированные Paint/Drawable и `MessageDrawable` для чата. Подмена только `getColor` недостаточна: глобальные Paint/пузырьки также должны оставаться независимыми.
- `ThemeActivity`/`ThemePreviewActivity`/`ThemeEditorView`: сохраняется штатная установка Telegram-тем и preview; локальные изменения Tetogram не должны выдавать себя за server theme.
- `ChatBackgroundDrawable`: серверные обои, изображения/узоры/градиенты; готового локального видеослоя здесь нет.
- `WallpaperUpdater`: штатный выбор/редактирование image wallpaper; нельзя считать его готовым timeline-редактором видео.
- `VideoPlayer`: уже подключённый Media3/ExoPlayer, TextureView, repeat mode, speed, отключение audio track в конструкторе. Локальные фоны используют этот стек, без нового собственного декодера.
- `ProfileActivity2`/`ProfileGalleryView`: собственный header/banner должен быть отдельным локальным оформлением, не заменять публичные avatar/photo/video и их загрузку.
- `SectionsActivity`/`SectionMenu`: стабильные UUID, folder keys, account UID, пользовательские circle images. Существующий storage и изображения 0.3.1 не мигрируются разрушительно.
- `LabsPage`: обязательные getListViewForSimpleGlass + isSupportEdgeToEdge, без фиксированных компенсаций status bar.

## Модель и наследование

`AppearanceConfig` — отдельная versioned модель с Properties codec. Иерархия per-property: global → section UUID → stable folder key → dialog ID → forum topic ID. Собственный профиль имеет отдельный profile scope. У каждого resolved значения сохраняется origin для редактора. Если параметр отсутствует на всех уровнях, работает текущая native Telegram Theme/серверная chat theme. Reset удаляет override, а не копирует значение родителя.

Поиск, уведомления и внешние ссылки получают direct context: global → chat → topic. Они не выбирают случайную папку. Origin context списка чатов должен передаваться явно; сообщения и переписки не копируются.

Preset содержит только Style, название и UUID, без account/dialog IDs. Применение/дублирование копирует Style. Для built-in конфигураций ID детерминирован по имени, обе палитры заданы. Export должен включать только preset, выбранные пользователем медиа и format metadata; не все локальные scopes.

Новый storage отдельный от SectionsStore, базы истории и Telegram-session. Пустой appearance документ означает native оформление и не изменяет конфигурацию 0.3.1. Медиа хранятся отдельно в private UID directory; cleanup проверяет ссылки всех scopes и всех, в том числе неактивных, пресетов.

## Проверки

`AppearanceConfigTest` — JVM модель/наследование/контексты входа/независимость аккаунтов и чатов/preset copies/shared-media references/безопасные имена/версия/контраст встроенных палитр. Это не UI, декодирование или плеер. Android UI/жесты/видео/launcher runtime пока NOT RUN.


## Прогресс проверки

Source `3debc10` (модель, native ресурсы, draft editor) успешно прошёл CI 43568045351: JVM/TL-задача, SQLite и APK compilation/verification. Это промежуточная проверка этапа 1, не выпуск полного ТЗ 0.4.0; Android runtime NOT RUN.

## Локальные медиа

Picker копирует выбранный URI на фоновой очереди в private UID directory, поэтому временный grant больше не нужен. ImageLoader выполняет bounded decode и EXIF orientation; PhotoViewer SELECT_TYPE_WALLPAPER даёт native crop/filter/video timeline. Локальный provider не отправляет сообщение, запрещает caption/send submenu/timer и принудительно отключает звук. PhotoViewer получает аккаунт родительского fragment, остальные Activity-only callers сохраняют прежнее поведение.

Видео подготавливается `MediaCodecVideoConvertor`: выбранный time range/crop/фильтры, фактическое ограничение FPS и максимальной стороны, без звука. Отдельный decoder/encoder проекта не добавляется. При ошибке подготовки сохранённые обои не меняются. Лимит исходника 1 GiB ограничивает расход диска; поддержка формата зависит от декодеров устройства. Конвертация и thumbnail extraction вне UI-потока, отмена проверяет job/owner.

`AppearanceMediaView` использует один VideoPlayer(false, audioDisabled=true), TextureView, native repeat и speed. Lease на активную поверхность не позволяет одновременно крутить несколько фоновых preview. Pause/detach/screen-off/power-saver/Lite Mode/reduced animations останавливают и освобождают плеер. Ошибка воспроизведения оставляет обложку. SimpleGlass capture рисует статичную cover, а не второй постоянно работающий decoder. Стабильная высота видеослоя сохраняет crop при появлении клавиатуры; при изменении ширины (ориентации) геометрия пересчитывается.

API 31+ применяет blur RenderEffect. На Android до 12 включённое размытие видео использует статичную размытую обложку, а не имитирует динамический blur. Видео обрезается/вписывается и масштабируется отдельной texture matrix. Слой добавляется ниже сообщений, поля ввода и action bar, не принимает touch. Images/gradients и local video не отправляют server wallpaper requests.

Media cleanup выполняется после atomic commit, проверяет ссылки всех scopes/пресетов и pending draft leases. Cancel/discard сериализованы с сохранением: закрытие редактора во время записи не удаляет только что сохранённый файл. Исходники галереи не меняются. Source и prepared media имеют отдельные UUID-файлы; новая область оформления не затрагивает иконки разделов 0.3.1.

Новые JVM media-policy tests покрывают gates видимости/foreground/screen/mute-independent Lite/battery/animation, границы decode/range/quality и безопасный GC shared/pending assets. Реальные codecs/Surface/жесты/EXIF/device power events/keyboard/blur ещё NOT RUN на Android.
