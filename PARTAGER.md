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

### Ce qu'il faut dire aux gens, et pourquoi

Ton `.exe` n'est pas signé par un certificat payant. Microsoft Defender SmartScreen
fonctionne par réputation : la doc Microsoft dit qu'il vérifie « *downloaded files
against a list of files that are well known and downloaded frequently. If the file isn't
on that list, Microsoft Defender SmartScreen shows a warning, advising caution* », qu'il
vérifie « *the digital signature used to sign a file* », et que « *if there's no
reputation, the item is marked as a higher risk and presents a warning to the user* ».

Autrement dit : **tes premiers utilisateurs verront un avertissement.** Ce n'est pas un
bug de ton application, c'est le fonctionnement normal pour un fichier neuf et non
signé. La doc Microsoft ne décrit pas la fenêtre exacte ni la manœuvre pour passer outre,
donc je ne te l'invente pas — mais annonce-le franchement dans ta Release plutôt que de
laisser les gens le découvrir :

> Windows peut afficher un avertissement au premier lancement : l'application est
> nouvelle et n'est pas signée par un certificat payant. Le code source est entièrement
> public dans ce dépôt, et `EMPREINTES.txt` permet de vérifier que le fichier téléchargé
> est bien celui publié ici.

Un certificat de signature de code coûte cher et se renouvelle. Ce n'est pas la peine
pour une aide de jeu gratuite — mais la transparence, si.

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
