# Локальные ключи оформления Tetogram

Схема документа: 1. Scope и происхождение значения хранятся отдельно от Style. Пустой override означает наследование; сохранённое false/0 — явное значение. Отсутствие локальных параметров сохраняет native Telegram Theme/ChatTheme. Это описание реализованного API, а не гарантия Android визуальных проверок.

## Цвета

`light.<native ThemeColors name>` / `dark.<name>` — signed ARGB int, не ordinal/resource ID. Native message/link/time/status/reply/reaction/panel роли доступны расширенному редактору; фактическое применение зависит от соответствующего native компонента. `light.list.<name>` / `dark.list.<name>` изолируют палитру списка от чата. `light.banner.chat_wallpaper` и dark-вариант задают фон собственного профиля.

Собственные роли: `bubble.inGradient1..3`, `bubble.inOutline`, `bubble.outOutline`, `group.senderName`, `list.outline`, `section.background`, `section.activeBackground`, `section.text`, `section.activeText`, `section.outline`, `section.activeOutline`, `section.badge`, `section.badgeText`, `banner.text`, `banner.secondaryText`. Они также получают light./dark. префикс. Цветные Unicode emoji и фотографии не тонируются как монохромные встроенные иконки.

## Форма сообщений

Для `bubble.in` и `bubble.out`: `Radius`, `TopLeft`, `TopRight`, `BottomLeft`, `BottomRight` (0–32 dp); `Tail` / `Shadow` (boolean), `OutlineWidth` (0–4 dp). Native padding/layout учитывает scoped radius; произвольный mesh хвостика, отдельные внутренние padding каждого типа медиа и собственные message transition types не добавлены. Сохраняются native правила album/header/caption/compact/transition geometry.

## Фоны

Домены: `wallpaper`, `list.wallpaper`, `banner.wallpaper`. Поля: `file`, `cover.file`, `source.file` (private UUID basename); `type` (image/video/solid/gradient), `base` (global/telegram), `fit` (fill/fit), `zoom` (1–4), `panX/panY` (−1..1), `angle` (0–360), `dim/opacity` (0..1), `blur`, `speed` (.25..2), `loop/animate/gradientMotion/parallax/pattern` (boolean), `patternIntensity` (0–100), `fps` (12–60), `quality` (360–1920).

FPS/quality применяются при подготовке/подтверждении Edit current video, а не фиктивным ограничителем кадров ExoPlayer. Native crop/trim/filter/cover editor может иметь ограничения конкретного codec. Звук удаляется при подготовке и отключён в плеере. Режим без анимаций оставляет cover. До API 31 blur video использует static cover. Pattern — встроенный SVG Telegram; native Wallpapers/Themes сохраняются отдельно. Source.file не экспортируется. Native ImageReceiver/TextureView используют одну Viewport geometry для fit/crop.

## Список и навигация

`list.rowOpacity`, `list.navigationOpacity` (0..1), `list.radius` (0–32 dp), `list.sideInset` (0–24 dp), `list.rowGap` (0–20 dp), `list.avatarSize` (24–52/56 dp), `list.outlineWidth` (0–4 dp), `list.separators` (boolean). Per-row image использует ImageReceiver; видео не декодируется отдельно для каждой строки. Общий/section/folder list background имеет единственную активную media surface. Архив использует palette presentation без section filtering. `section.outlineWidth` (0–6 dp) меняет штатный кружочек, не badges/count policy.

## Профиль, эффекты, launcher

Собственный локальный профиль: `banner.height` (120–480 dp, ограничен native header), `banner.radius` (0–40 dp), `banner.parallax` (0..1, scroll effect), `banner.avatarAlignment` (−1..1, native expanded horizontal endpoint). Серверная avatar/gallery не заменяется. Иные profile layouts/transitions остаются native.

Глобальные `effects.enabled`, `effects.durationScale` (.25..2), `effects.reduceOnBattery` управляют подключёнными native переходами; native Lite Mode сохранён. Они не добавляют собственную систему анимации сообщений.

`launcher.label`, `launcher.emoji`, `launcher.icon.file`, `launcher.iconScale` (.45–1.1, default .78) используются только для явного pinned shortcut. Native brand/season alias selector действует на устройство, см. APPEARANCE_EDITOR.md и ICON_ASSETS.md; произвольное системное имя/иконка установленной APK не обещаются. Preset не создаёт shortcut автоматически. Android/launcher может потребовать ручное удаление плитки.

## Безопасность и расширение

Account UID, section UUID, folder/dialog/topic mapping не входят в переносимый preset. Медиа лежат в private UID directory; все scopes, неактивные presets и pending leases участвуют в GC. Экспорт медиа только opt-in, импорт bounded/checked с новыми UUID. Неподдерживаемые параметры нельзя добавлять в UI до реализации применения и native preview. Отдельные кнопки/фиктивные toggles ради полноты списка недопустимы.

Реальные аппаратные проверки — APPEARANCE_MANUAL_TESTING.md. Предпросмотры используют изолированные синтетические данные, а рабочие списки/MessageObject/сессии остаются native.
