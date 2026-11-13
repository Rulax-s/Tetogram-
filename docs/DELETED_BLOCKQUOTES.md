# 0.2.3: настоящая Telegram blockquote для удалённого Reply

Проверена чистая master 8e13279; после fetch origin/master совпала. Изменение ограничено подготовкой outgoing-текста/entities. Свайп, стандартная Reply-панель, маршрутизация темы, owner/account/dialog guards и retry gate 0.2.2 сохранены. Локальная история/схема/снимки не изменяются.

## Штатный механизм Telegram

- MediaDataController.getEntities преобразует QuoteSpan в TLRPC.TL_messageEntityBlockquote, копируя offset/length/collapsed.
- MessageObject.addEntitiesToText преобразует TL_messageEntityBlockquote в QuoteSpan.putQuote. Рисование, тема и фон остаются штатными; собственная имитация quote UI не добавляется.
- TLRPC.TL_messageEntityBlockquote сериализует flags (collapsed), offset, length. Используется актуальный класс из текущего Telegram, не layer180 legacy.
- [Telegram entities](https://core.telegram.org/api/entities) задаёт offset/length в UTF-16 и исключает trailing whitespace из диапазонов; [blockquote constructor](https://core.telegram.org/constructor/messageEntityBlockquote) описывает collapsed.

## Подготовка сообщения

В группе формируется `автор\nцитата\n\nответ`. Author — отдельная TL_messageEntityBold от 0 до UTF-16 длины имени. Цитата — отдельная TL_messageEntityBlockquote (collapsed=false) только над сохранённым текстом, без символа `>`. Личный чат сохраняет прежнее поведение без имени. Фото/видео/файлы используют ту же схему в caption.

DeletedReplyPolicy.Result содержит рассчитанные по конечной строке authorLength/quoteOffset/quoteLength/entityShift. Сохранена Unicode-safe обрезка цитаты до 20 символов. В snippet нормализуются CRLF/CR в LF и убираются внешние пробельные символы; внутренние переносы остаются. Исходная строка архива не меняется. Для пустого видимого snippet используется «…» с ненулевой длиной entity. Текст ответа пользователя сохраняется буквально, включая его переносы и whitespace.

DeletedReplyProtocol.formatEntities клонирует entities ответа, сдвигает их на длину префикса и двух разделителей, добавляет bold/blockquote и стабильно сортирует по offset, затем length DESC. Дополнительные entities занимают непересекающиеся диапазоны. Форматирование ответа, включая его собственные quote/pre/code/custom emoji, не оборачивается новой blockquote. Entities исходной удалённой цитаты не вкладываются в неё: цитируется её текст. Так новая quote не создаёт вложенных blockquote или пересечения с code/pre.

Обычный server Reply не вызывает преобразование. У local Reply недействительная цель по-прежнему удаляется; для отдельной темы используется валидный root, для General — штатная неявная маршрутизация. Повторная отправка использует сохранённые outgoing entities без повторного префикса. Ограничения типов вложений и длины итогового сообщения из DELETED_REPLIES.md сохраняются.

## Проверки

DeletedBlockquoteTest проверяет настоящие TL entities: bold имени, quote текста/emoji/переносов, отдельный ответ, UTF-16 диапазоны, отсутствие пересечения с user pre/blockquote/bold, collapsed пользовательской цитаты, подпись без текста ответа, TL serialization round trip, пустой snippet, неизменность оригиналов/повторную подготовку и normal/retry gate. Существующие DeletedReplyPolicyTest обновлены под текст без `>`; прочие регрессии сохраняются.

Android runtime-проверки пока не выполнены агентом. JVM/TL tests не подтверждают реальную отправку, жесты или визуальное отображение на устройстве.

- [ ] Обычный Reply остаётся связанным с серверным сообщением.
- [ ] Local Reply: свайп/контекстный пункт, стандартный preview/отмена.
- [ ] Имя отдельно жирным, сохранённый текст в native quote, ответ снаружи.
- [ ] Многострочная цитата, emoji/ZWJ, многострочный ответ с bold/italic/link/custom emoji/pre/собственной quote.
- [ ] Фото с подписью и без; retry не дублирует entities/цитату.
- [ ] Форум General/отдельная тема, два аккаунта, восстановленный draft.
- [ ] Визуальная проверка отправленного результата в официальном Telegram, AyuGram и Tetogram, светлая/тёмная тема.

Конкретный скриншот в этом сообщении не был прикреплён: соответствие обеспечивается заявленным нативным типом entity, а не сравнением пикселей с изображением.
