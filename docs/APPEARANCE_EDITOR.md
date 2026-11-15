# Переработка редактора оформления 0.4.0 / code149

Состояние: реализация и проверки исходников; результат Android-компиляции фиксируется отдельно. Скриншоты интерфейса в текущем запросе не были доступны как вложения. Исправления основаны на описанных проблемах и текущем коде. Реальные One UI/жесты/увеличенный шрифт/video lifecycle требуют телефона.

## Навигация и единый источник значения

Главное меню содержит девять назначений: пресеты; цвета и стиль; обои; список; переписки; профили; папки/разделы; название/иконка; дополнительно. Import/export/native Themes находятся у пресетов. Технические эффекты — только в «Дополнительно». Настройки фона — только в «Обои»; другие категории дают ссылку, сохраняя текущую область. Category — состояние интерфейса, а не новый scope/ключ в AppearanceConfig.

AppearanceEditorPolicy определяет единственную категорию ключа, тип/default, допустимую область и preview surface. Existing Style keys, Properties schema1, UID directories, UUID/IDs и пресеты сохраняются. Legacy native chat-list colors остаются fallback; редактируется существующий list-domain override, а второй пункт с тем же назначением не показывается. Не создаётся параллельный storage тем.

Scope остаётся global → section → folder → chat → topic; profile имеет global → profile. Название текущей области показывается в заголовке/описании, источник каждого цвета/числа — под названием. Локальные режимы/переопределения не становятся глобальными при переходе между категориями. Несовместимые с областью категории не предлагаются. Для строки отдельного чата фон подписан как оформление строки, а не всего списка.

У обоев три независимые существующие поверхности: chat/list/banner. Выбор поверхности определяет редактируемые ключи; простой выбор вида предпросмотра этого не делает. Reset обоев удаляет только выбранный домен. Изменение категории или Simple/Advanced не меняет draft.

## Черновик, кнопки и системные Insets

Undo и дополнительные действия — в SimpleActionBar. Save as preset, просмотр другой сцены, временное скрытие/полный экран и Reset — в дополнительных действиях. У preview также есть видимые Expand/Hide. Сохранение не занимает строки списка. Apply/Cancel закреплены внизу, Apply выключена при равенстве baseline/draft. Выход с изменениями требует Apply/Discard/Cancel; swipe-back при dirty не обходит проверку. На время подготовки медиа случайный выход блокируется.

Сохранение редактируемого пресета явно подписано как изменение пресета, а не приложения. При возвращении из вложенного редактора новые версии других scopes могут быть приняты без потери текущего draft; конфликт одного и того же scope сохраняет optimistic revision protection.

LabsPage размещает top preview и bottom dock по реальным WindowInsets/измеренным размерам через BaseFragmentSimpleGlass. Отдельные экраны не компенсируют status bar собственным padding. Header, вырезы и нижняя навигация остаются native. Multiline settings rows используют SP и wrap-content, значение переносится под название; boolean имеет native Switch и растущий текстовый столбец. Долгое нажатие явно заданного switch возвращает наследование.

## Предпросмотр и палитра

Preview расположен над RecyclerView. Перестройка строк не отсоединяет его медиаслой. AppearancePreview обновляет существующий MediaView, меняя только scene/resources; смена типа сцены не требует новой видеоповерхности, если источник тот же. MediaView хранит позицию при фактическом lifecycle stop и сбрасывает её только для нового файла. Профиль показывает безопасное демонстрационное имя, круглый пиктографический avatar и многострочное описание, без реальных данных.

Цвет сразу открывает native ColorPicker. Рядом находятся HEX/RGB/HSV/HSL ввод, alpha, recent/favorites, pipette и live sample. Copy/add favorite — в popup «⋮», Inherit — в самом диалоге. Raw native keys доступны поиску; русские fallback names получаются из словаря ролей. Поиск и view-mode не меняют draft и не сбрасывают видео к началу. GPU/decoder/scroll/large-font поведение на Android ещё NOT RUN.

## Фон списка

Transparent / Glass / Cards меняют существующие list.rowOpacity/radius/sideInset/rowGap/separators. Новый независимый opacity не создаётся. Для плоской поверхности заливка применяется один раз к всему viewport, включая пустое место ниже последнего чата. Для карточек и individual chat/topic row она остаётся локальной. Это устраняет потенциальную границу между tinted rows и нетонированным пустым фоном. SimpleGlass capture включает тот же tint; второй декодер не создаётся. Старые конфигурации с отдельными карточками сохраняются.

## Иконки и технические ограничения

Brand/season реального APK-icon — device-wide состояние LauncherIconController. Android не поддерживает разные установленные icons одновременно для разных Telegram-аккаунтов. Выбор применяется после Apply, и подпись/package не меняются. Personal shortcut draft остаётся UID-based в AppearanceStore; shortcut создаётся только после отдельного подтверждения. Источники изображений и отсутствующий Teto summer — ICON_ASSETS.md.

В основном редакторе нет старого Premium/Turbo/Nox selector. Классические варианты отделены; существующие aliases не удалены из Manifest ради совместимости обновления. Три маски preview и zoom используют общий renderer с генерацией adaptive shortcut bitmap; дополнительная круглая маска в bitmap не запекается. Это не доказательство корректности One UI: нужен реальный launcher test.

## Проверки

- JVM: single category ownership/reset, supported scopes, dirty/undo semantics, unchanged schema/hierarchy, media identity, names/search labels, calendar months/asset availability/safe-zone geometry, uniform list tint versus cards/local row. Исполнение подтверждается CI JUnit report.
- Source contracts: девять маршрутов, отсутствие save rows, общие Insets, preview ownership, direct color picker, SP/wrapping и matching aliases/resources. Они не запускают Android.
- Телефон: новая установка/обновление, большой шрифт, gesture/3-button, portrait/landscape, Mode → Back → Undo → Apply/Cancel, возврат из scopes/pickers, account switch, видео при изменении цвета, все aliases/masks на One UI, дата/часовой пояс, Gallery crop/zoom и отмена. Статус агента: NOT RUN.
