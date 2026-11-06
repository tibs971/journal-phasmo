/* Journal Phasmo — service worker.
   Le journal tient dans un seul fichier : on le met en cache à l'installation,
   on le sert immédiatement, et on rafraîchit en arrière-plan quand le réseau est là. */
const CACHE = "journal-phasmo-v3";
const FICHIERS = ["./", "./index.html", "./manifest.webmanifest",
                  "./icone-192.png", "./icone-512.png", "./icone-maskable-512.png"];

self.addEventListener("install", e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(FICHIERS)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", e => {
  e.waitUntil(caches.keys()
    .then(noms => Promise.all(noms.filter(n => n !== CACHE).map(n => caches.delete(n))))
    .then(() => self.clients.claim()));
});

self.addEventListener("fetch", e => {
  if (e.request.method !== "GET") return;
  e.respondWith(caches.match(e.request).then(enCache => {
    const reseau = fetch(e.request).then(rep => {
      if (rep && rep.status === 200 && rep.type === "basic")
        caches.open(CACHE).then(c => c.put(e.request, rep.clone()));
      return rep;
    }).catch(() => enCache);
    return enCache || reseau;          // hors ligne : le cache suffit
  }));
});
