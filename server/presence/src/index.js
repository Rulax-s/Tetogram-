// Tetogram presence server: "online in Tetogram right now". Cloudflare Worker + one Durable Object.
// Nothing is stored on disk: heartbeats (Telegram ID → time) live in memory and expire after five minutes.
const ONLINE_MS = 5 * 60 * 1000;
const MAX_IDS = 100;

const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } });
const validId = (id) => Number.isSafeInteger(id) && id > 0;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === 'GET' && url.pathname === '/') return json({ service: 'tetogram-presence', ok: true });
    if (request.method !== 'POST' || request.headers.get('x-tetogram') !== '1') return json({ error: 'forbidden' }, 403);
    return env.PRESENCE.get(env.PRESENCE.idFromName('main')).fetch(request);
  },
};

export class Presence {
  constructor(state, env) { this.beats = new Map(); }

  async fetch(request) {
    const path = new URL(request.url).pathname;
    let body;
    try { body = await request.json(); } catch (e) { return json({ error: 'json' }, 400); }
    if (path === '/beat') return this.beat(body);
    if (path === '/status') return this.status(body);
    return json({ error: 'not found' }, 404);
  }

  beat({ uid, visible }) {
    if (!validId(uid)) return json({ error: 'uid' }, 400);
    if (visible) this.beats.set(uid, Date.now()); else this.beats.delete(uid);
    if (this.beats.size > 50000) this.sweep();
    return json({ ok: true });
  }

  status({ ids }) {
    if (!Array.isArray(ids)) return json({ error: 'ids' }, 400);
    const now = Date.now(); const online = [];
    for (const id of ids.slice(0, MAX_IDS)) { const at = this.beats.get(id); if (at && now - at < ONLINE_MS) online.push(id); }
    return json({ online });
  }

  sweep() { const now = Date.now(); for (const [k, v] of this.beats) if (now - v >= ONLINE_MS) this.beats.delete(k); }
}
