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
