# Сервер «Друзья в Тетограме»

Маленький сервер, который знает только одно: кто из подключившихся пользователей Тетограма открыл приложение за последние 5 минут и не выключил «Показывать другим». Сообщения, контакты и списки чатов сюда не попадают. Хранятся только хеши токенов и Telegram ID, подтверждённые через бота; сигналы «я здесь» держатся в памяти.

Бесплатного тарифа Cloudflare Workers хватает (Durable Objects на SQLite входят в бесплатный план).

## Запуск (один раз, ~10 минут)

1. **Бот.** В Telegram откройте @BotFather → `/newbot` → придумайте имя и username (например `TetogramPresenceBot`). Сохраните токен — его никому не пересылайте.
2. **Cloudflare.** Зарегистрируйтесь на dash.cloudflare.com (бесплатно). На компьютере с Node.js:
   ```
   cd server/presence
   npx wrangler login
   ```
3. Впишите username бота в `wrangler.toml` → `BOT_USERNAME`.
4. Секреты (каждая команда спросит значение):
   ```
   npx wrangler secret put BOT_TOKEN        # токен от BotFather
   npx wrangler secret put WEBHOOK_SECRET   # любая длинная случайная строка (буквы/цифры)
   npx wrangler secret put ADMIN_KEY        # ещё одна длинная случайная строка
   ```
5. Запуск: `npx wrangler deploy`. Wrangler покажет адрес вида `https://tetogram-presence.<имя>.workers.dev`.
6. Привязать бота: откройте в браузере `https://tetogram-presence.<имя>.workers.dev/setup?key=<ADMIN_KEY>` — должно быть `"ok":true`.
7. В Тетограме: Тетолабс → «Друзья в Тетограме» → «Сервер присутствия» → вставьте адрес → «Подключиться через бота» → в боте «Старт».

Пришлите разработчику адрес сервера (не токены) — его можно вшить по умолчанию, чтобы друзьям ничего не вводить.

## API

`POST /nonce` → `{nonce, bot}`; бот получает `/start <nonce>` и выдаёт токен; `POST /claim {nonce}` → `{token, uid}`; `POST /beat {token, visible}`; `POST /status {token, ids[≤100]}` → `{online}`; `POST /forget {token}`.
