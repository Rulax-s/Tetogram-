# Подготовленные сезонные значки

Используются только найденные и визуально просмотренные изображения владельца из Pictures. Они не перерисовываются и не заменяются другими персонажами.

| Вариант | Исходное имя файла | Ресурс |
|---|---|---|
| Мику / весна | file_0000000012748210bf6fedd711126a3b.png | tetogram_miku_spring_art |
| Мику / лето | file_0000000022b881f48a1326a821fdd786.png | tetogram_miku_summer_art |
| Мику / осень | file_00000000c1148210b4c30af479bd427b.png | tetogram_miku_autumn_art |
| Мику / зима | file_00000000567c81f4bc3edc8359c54626.png | tetogram_miku_winter_art |
| Тето / весна | file_000000000f3481f49df71744031d5784.png | tetogram_teto_spring_art |
| Тето / осень | file_00000000d65881f4ba0cc21916ab7df4.png | tetogram_teto_autumn_art → existing tetogram_artwork |
| Тето / зима | file_000000000d3081f4a4b7f5f59972b907.png | tetogram_teto_winter_art |

**Отсутствует:** подготовленная летняя иконка Тето (`tetogram_teto_summer_art.png`). Отдельный summer alias не создаётся без artwork. Явный выбор предупреждает об отсутствии; Auto в июне–августе использует подписанный classic fallback. Это ограничение ресурса, а не готовая летняя иконка.

Авто: март–май весна, июнь–август лето, сентябрь–ноябрь осень, декабрь–февраль зима; используется Calendar устройства. Refresh при запуске/возврате и date/timezone broadcast, без фонового декодера или keep-alive. Полностью остановленное приложение обновит icon при следующем запуске.

В Android adaptive foreground слой 108dp, значимое содержимое должно оставаться внутри safe zone 66dp; внешняя область используется OEM для маски/эффектов. [Официальные требования](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), [InsetDrawable fractions API26](https://developer.android.com/reference/android/graphics/drawable/InsetDrawable).

Новые PNG — готовые непрозрачные композиции с тёмными углами. Чтобы не обрезать персонажа/самолётик и не делать дополнительный crop, foreground inset24% помещает композицию в 56.16dp при 108dp слое. Native launcher viewport72dp даёт default preview scale0.78. Применяется только внешняя маска лаунчера; custom shortcut bitmap остаётся квадратным, без дополнительной круглой alpha-маски. В редакторе видны circle/square/squircle и пользовательский scale; увеличение может сознательно вывести изображение за безопасную область. Растровые исходники не редактировались. Осенний Тето использует существующий ресурс без дублирования PNG.

Реальная нормализация OEM может отличаться. Samsung One UI на устройстве агентом **NOT RUN**; source/geometry checks этого не подтверждают. Для full-bleed варианта без полей понадобятся исходные foreground/background layers без уже оформленных углов; отсутствующие части композиции не выдумываются.
