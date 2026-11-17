// Service worker for the GitHub Pages copy: network first, cached copy when offline.
const CACHE = "acctcalc-v1";
const ASSETS = ["./", "./index.html", "./apple-touch-icon.png"];

self.addEventListener("install", e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", e => {
  e.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", e => {
  const req = e.request;
  if (req.method !== "GET" || new URL(req.url).origin !== location.origin) return;
  e.respondWith((async () => {
    const cache = await caches.open(CACHE);
    const network = fetch(req).then(res => {
      if (res.ok) cache.put(req, res.clone());
      return res;
    });
    const cached = () => cache.match(req, { ignoreSearch: true })
      .then(r => r || (req.mode === "navigate" ? cache.match("./index.html") : undefined));
    try {
      // weak signal: if the network takes over 4 s and a cached copy exists, use it
      const timeout = new Promise((_, rej) => setTimeout(() => rej(new Error("timeout")), 4000));
      return await Promise.race([network, timeout]);
    } catch (err) {
      const hit = await cached();
      if (hit) { network.catch(() => {}); return hit; }
      return network;          // nothing cached: keep waiting for the network
    }
  })());
});
