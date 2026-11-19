// Tetogram presence server: "online in Tetogram right now". Cloudflare Worker + one Durable Object.
// Stores only Telegram IDs that proved ownership through the bot, as hashed tokens. Heartbeats live in memory.
const ONLINE_MS = 5 * 60 * 1000;      // a beat counts for five minutes
const NONCE_MS = 10 * 60 * 1000;      // bot link is valid ten minutes
const MAX_IDS = 100;                  // per status request

const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } });
const hex = (bytes) => [...new Uint8Array(bytes)].map((b) => b.toString(16).padStart(2, '0')).join('');
const random = (n) => hex(crypto.getRandomValues(new Uint8Array(n)));
const sha256 = async (text) => hex(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text)));

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === 'GET' && url.pathname === '/') return json({ service: 'tetogram-presence', ok: true });
    if (url.pathname === '/setup') return setup(url, env);
    if (request.method !== 'POST') return json({ error: 'method' }, 405);
    if (url.pathname === '/telegram' && request.headers.get('x-telegram-bot-api-secret-token') !== env.WEBHOOK_SECRET) return json({ error: 'forbidden' }, 403);
    const stub = env.PRESENCE.get(env.PRESENCE.idFromName('main'));
    return stub.fetch(request);
  },
};

// One-time: https://<worker>/setup?key=<ADMIN_KEY> registers the bot webhook.
async function setup(url, env) {
  if (!env.ADMIN_KEY || url.searchParams.get('key') !== env.ADMIN_KEY) return json({ error: 'forbidden' }, 403);
  const hook = `${url.origin}/telegram`;
  const r = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/setWebhook`, {
    method: 'POST', headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ url: hook, secret_token: env.WEBHOOK_SECRET, allowed_updates: ['message'] }),
  });
  return json(await r.json());
}

export class Presence {
  constructor(state, env) { this.state = state; this.env = env; this.beats = new Map(); this.nonces = new Map(); }

  async fetch(request) {
    const path = new URL(request.url).pathname;
    let body = {};
    try { body = await request.json(); } catch (e) { return json({ error: 'json' }, 400); }
    switch (path) {
      case '/nonce': return this.nonce();
      case '/claim': return this.claim(body);
      case '/telegram': return this.telegram(body);
      case '/beat': return this.beat(body);
      case '/status': return this.status(body);
      case '/forget': return this.forget(body);
      default: return json({ error: 'not found' }, 404);
    }
  }

  sweep() { const now = Date.now(); for (const [k, v] of this.nonces) if (now - v.at > NONCE_MS) this.nonces.delete(k); }
  nonce() { this.sweep(); if (this.nonces.size > 10000) return json({ error: 'busy' }, 429); const n = random(12); this.nonces.set(n, { at: Date.now() }); return json({ nonce: n, bot: this.env.BOT_USERNAME }); }

  async telegram(update) {
    const msg = update.message; const text = msg && msg.text || '';
    const m = /^\/start\s+([0-9a-f]{24})$/.exec(text);
    if (!msg || !msg.from || msg.chat.type !== 'private') return json({ ok: true });
    let reply = 'Откройте «Друзья в Тетограме» в Тетолабс и нажмите «Подключиться».';
    if (m && this.nonces.has(m[1])) {
      const token = random(32);
      await this.state.storage.put('t:' + await sha256(token), msg.from.id);
      this.nonces.set(m[1], { at: this.nonces.get(m[1]).at, uid: msg.from.id, token });
      reply = 'Готово! Вернитесь в Тетограм.';
    }
    await fetch(`https://api.telegram.org/bot${this.env.BOT_TOKEN}/sendMessage`, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ chat_id: msg.chat.id, text: reply }) });
    return json({ ok: true });
  }

  claim({ nonce }) {
    const n = typeof nonce === 'string' && this.nonces.get(nonce);
    if (!n) return json({ error: 'unknown' }, 404);
    if (!n.token) return json({ pending: true });
    this.nonces.delete(nonce);
    return json({ token: n.token, uid: n.uid });
  }

  async who(token) { if (typeof token !== 'string' || token.length !== 64) return null; return (await this.state.storage.get('t:' + await sha256(token))) || null; }

  async beat({ token, visible }) {
    const uid = await this.who(token); if (!uid) return json({ error: 'token' }, 401);
    if (visible) this.beats.set(uid, Date.now()); else this.beats.delete(uid);
    return json({ ok: true });
  }

  async status({ token, ids }) {
    const uid = await this.who(token); if (!uid) return json({ error: 'token' }, 401);
    if (!Array.isArray(ids)) return json({ error: 'ids' }, 400);
    const now = Date.now(); const online = [];
    for (const id of ids.slice(0, MAX_IDS)) { const at = this.beats.get(Number(id)); if (at && now - at < ONLINE_MS) online.push(Number(id)); }
    return json({ online });
  }

  async forget({ token }) {
    const uid = await this.who(token); if (!uid) return json({ ok: true });
    await this.state.storage.delete('t:' + await sha256(token)); this.beats.delete(uid);
    return json({ ok: true });
  }
}
