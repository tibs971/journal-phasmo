# Partager Journal Phasmo

Trois choses différentes à partager, et elles ne se partagent pas de la même façon.

| ce que tu partages | comment | ce que la personne reçoit |
|---|---|---|
| l'application Windows | une **Release** GitHub | un seul `.exe`, rien à installer |
| la version téléphone / navigateur | **GitHub Pages** | un lien à ouvrir, installable hors ligne |
| le projet lui-même | le **dépôt** | de quoi relire, corriger, recompiler |

---

## 1. Le dépôt

Envoie le contenu de ce dossier sur `github.com/tibs971/<nom-du-dépôt>`. Le `.gitignore`
est déjà là : `bin/`, `obj/`, `dist/` et l'archive ne partent pas.

Deux fichiers méritent d'être lus par ceux qui arrivent : `LISEZ-MOI.md` pour l'usage, et
`source/LISEZ-MOI.md` pour comprendre que `phasmo.html` est **généré** et qu'une
correction se fait dans `source/donnees.json`.

`EMPREINTES.txt` contient le SHA-256 de chaque fichier. Quelqu'un de méfiant — et il a
raison de l'être avec un `.exe` trouvé sur internet — peut vérifier que ce qu'il a
téléchargé est bien ce que tu as publié. Pense à le régénérer après chaque modification,
sinon il devient un mensonge.

## 2. L'application Windows, par une Release

1. Lance `publier.cmd`. Il produit `dist\PhasmoCompanion.exe` : un fichier unique, qui
   embarque .NET et le journal. Rien à installer chez les autres.
2. Sur GitHub : **Releases** → *Draft a new release* → crée un tag (`v1.0.0`), un titre,
   et **glisse le `.exe` en pièce jointe**.
3. Mets aussi le `.exe` dans un `.zip` : certains navigateurs et certains antivirus
   bloquent le téléchargement direct d'un exécutable.

### L'avertissement Windows : pourquoi, et quoi faire

Au premier lancement, Windows affiche « **Windows a protégé votre ordinateur** — Microsoft
Defender SmartScreen a empêché le démarrage d'une application non reconnue », avec
« Éditeur : Éditeur inconnu ». Il faut cliquer sur *Informations complémentaires* puis
*Exécuter quand même*.

Changer de site d'hébergement n'y changera rien : **SmartScreen juge le fichier, pas
l'endroit d'où il vient.** Et comme le fichier n'est pas signé, chaque nouvelle version
repart de zéro — la réputation ne s'accumule jamais.

Les options réelles, d'après la [documentation de Microsoft](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/code-signing-options)
(page mise à jour le 29 août 2026) :

| route | coût | ce que ça donne |
|---|---|---|
| **Microsoft Store**, au format MSIX | gratuit | « No warnings » — Microsoft resigne le paquet, l'avertissement disparaît |
| **SignPath Foundation** (projets libres) | gratuit | signature de niveau OV offerte aux projets open source |
| **Azure Artifact Signing** | ~9,99 $/mois | pas d'avertissement en moins au départ ; réservé aux particuliers **USA et Canada** |
| certificat **OV** | 150–300 $/an | ton nom s'affiche au lieu d'« éditeur inconnu » ; la réputation s'accumule |
| certificat **EV** | 400 $+/an | **rien de plus que l'OV depuis 2024** |

Ce que Microsoft écrit sur l'EV : « *That behavior was removed in 2024. EV-signed files now
go through the same reputation-building process as OV certificates.* » Et : « *Paying the EV
premium solely to avoid SmartScreen warnings is no longer justified.* »

**La seule route sans avertissement que Microsoft documente est le Store**, au format MSIX :
« *Microsoft re-signs your package automatically, meaning users never see a SmartScreen
warning.* »

Attention : signer **ne supprime pas** l'avertissement du premier téléchargement. Microsoft
est clair là-dessus, y compris pour son propre service : « *Azure Artifact Signing does not
provide instant SmartScreen trust* ». Signer sert à afficher ton nom et à faire monter la
réputation au fil des versions.

Une correction de ce que je disais plus haut : mettre le `.exe` dans un `.zip` n'enlève pas
SmartScreen. Le zip sert à contourner les navigateurs et antivirus qui bloquent le
téléchargement direct d'un exécutable, rien de plus.

### En attendant, sois franc dans ta Release

> Windows affichera un avertissement au premier lancement : l'application est nouvelle et
> n'est pas signée par un certificat payant. Cliquez sur « Informations complémentaires »
> puis « Exécuter quand même ». Le code source est entièrement public dans ce dépôt, et
> `EMPREINTES.txt` permet de vérifier que le fichier téléchargé est bien celui publié ici.

C'est plus honnête et plus rassurant que de laisser les gens le découvrir seuls.

## 3. La version téléphone, par GitHub Pages

GitHub Pages ne sait publier que la racine du dépôt ou un dossier `/docs` : la doc dit
« *the source folder can either be the root of the repository (`/`) on the source branch
or a `/docs` folder on the source branch* ». Ton journal est dans `web/`, donc on passe
par un workflow, ce que la doc recommande elle-même pour ce cas.

Le workflow est déjà écrit : `.github/workflows/pages.yml`. Il ne reste **qu'un réglage,
une seule fois** :

> **Settings → Pages → Build and deployment → Source → GitHub Actions**

Ensuite chaque envoi qui touche `web/` republie le site tout seul. Le workflow vérifie
au passage que `index.html`, le manifeste et le service worker sont bien là, et s'arrête
si l'un manque — mieux vaut un échec visible qu'un site cassé en ligne.

Une fois en ligne, le lien s'ouvre sur n'importe quel téléphone, et Chrome propose
« Ajouter à l'écran d'accueil » : le journal s'installe et fonctionne **hors ligne**,
sans compte développeur, sans examen, sans les 12 testeurs de Google Play.

Un point à savoir : « *GitHub Pages sites are publicly available on the internet, even if
the repository for the site is private* ». Et Pages sur un dépôt **privé** demande un
compte payant — en gratuit, il faut un dépôt public. Ce qui te va, puisque tu partages.

## 4. Où le faire savoir

Le jeu a une communauté active : Reddit, les discussions Steam, les serveurs Discord
dédiés. Présente-le pour ce qu'il est — une aide gratuite, sans publicité, hors ligne, et
**en français**, ce qui te distingue d'emblée.

Rappelle à chaque fois que c'est non officiel et sans lien avec Kinetic Games. C'est déjà
écrit en pied de page du journal, dans le `.exe` et dans la licence, mais le dire dans
l'annonce évite tout malentendu. La *Fan Content Creation Policy* interdit d'utiliser du
matériel tiré du jeu : ton projet la respecte — aucune texture, aucun son, aucune image,
tout est fait à la main.
