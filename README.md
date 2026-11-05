# Journal Phasmo

**Une aide d'identification des entités de Phasmophobia, en français, qui tourne à côté du jeu.**

Créé par **Thibault.P (Shwarzyi)** — gratuit, et qui le restera.

> Aide de jeu non officielle, sans aucun lien avec Kinetic Games.
> Données à jour de la **v0.19** (30 entités, 18 cartes).

---

## Ce que ça fait

**Trouver l'entité**

- Les 7 preuves en trois états : trouvée, exclue, neutre — et les modes 3 / 2 / 1 / 0 preuve
- **76 observations** cochables : ce que l'entité a fait, ce qu'elle n'a pas fait, et dans quelles
  conditions. Les cases rouges éliminent, les violettes ne gardent que les entités compatibles
- Les preuves et les vitesses devenues impossibles se barrent toutes seules
- Un récapitulatif de tout ce que vous avez coché, au-dessus des cartes d'entités
- Les observations mises en favori restent à portée de clic
- Pour chaque entité : capacités, seuils de chasse, vitesses, tests de confirmation et d'élimination

**Mesurer**

- **Compteur de pas** : tapez au rythme des pas pendant la chasse, l'appli donne la vitesse en m/s
  et les entités compatibles. Un métronome joue la vitesse de n'importe quelle entité pour comparer
- Trois **minuteurs** — encens, chasse, recharge — avec les repères utiles annoncés en clair
  (« chasse possible : Démon », « il ne peut pas tuer pendant la grâce », …)

**Tenir compte du contrat**

- **Difficulté** : d'Amateur à Démence, plus les préréglages Apocalypse I / II / III et le mode perso.
  Règle d'un coup le nombre de preuves, la période de grâce, la durée de chasse et la vitesse de l'entité
- **Carte** : les 18 cartes classées par taille — la durée exacte de la chasse, la perte de santé mentale
  et la limite de lumières en découlent. Le jeu n'affiche jamais cette taille
- **Lune de sang** : toutes les vitesses affichées passent en vitesses réelles (+15 %)

**Jouer avec**

- Une **fenêtre de minuteurs flottante**, façon LiveSplit : sans bordure, qui ne prend jamais le focus (option « toujours au-dessus » à cocher si le jeu est en fenêtré sans bordure),
  déplaçable, opacité du décor et du texte réglables séparément
- Des **raccourcis globaux** qui fonctionnent pendant que Phasmophobia a le focus, entièrement
  remappables — touches, pavé numérique, boutons de souris
- Tous vos réglages sont retenus d'une session à l'autre

---

## Installation

1. Allez dans **[Releases](../../releases)** et téléchargez `PhasmoCompanion.exe`.
2. Lancez-le. C'est tout — rien à installer, aucun fichier annexe.

### L'écran bleu « Windows a protégé votre ordinateur »

Il apparaîtra au premier lancement. Ce n'est **pas** un virus détecté : c'est Microsoft Defender
SmartScreen qui ne connaît pas encore ce fichier. Tout programme neuf, distribué hors du Microsoft
Store, déclenche cet écran tant qu'il n'a pas été téléchargé par assez de monde.

Pour passer : cliquez sur **Informations complémentaires**, puis sur le bouton
**Exécuter quand même** qui apparaît en bas.

L'avertissement disparaîtra de lui-même au fil des téléchargements, sans rien changer au fichier.

**Vous n'êtes pas obligé de me croire sur parole.** Trois façons de vérifier :

- **Le code source est entièrement ici.** Rien n'est caché : lisez-le, puis compilez l'exe
  vous-même avec `publier.cmd` (SDK .NET 8 requis). L'appli ne se connecte à aucun serveur,
  n'envoie rien, n'écrit que dans son propre dossier `%AppData%\PhasmoCompanion`.
- **L'empreinte du fichier** est publiée sur la page de la Release (voir ci-dessous).
- **VirusTotal** : déposez l'exe sur [virustotal.com](https://www.virustotal.com) et regardez
  le résultat vous-même.

**Vérifier que le fichier n'a pas été modifié** — dans PowerShell :

```powershell
Get-FileHash -Algorithm SHA256 .\PhasmoCompanion.exe
```

L'empreinte doit correspondre à celle publiée sur la page de la Release.

---

## Sur téléphone (Android et iPhone)

Le journal s'installe comme une application, sans passer par aucun magasin :

1. Ouvrez **https://tibs971.github.io/journal-phasmo/** dans Chrome (Android) ou Safari (iPhone).
2. Menu du navigateur → **Ajouter à l'écran d'accueil**.

Il apparaît alors avec son icône, s'ouvre en plein écran sans barre d'adresse, et **fonctionne
hors ligne** : tout est mis en cache au premier lancement. Les mises à jour se font toutes seules
au lancement suivant, sans rien réinstaller. Aucun avertissement de sécurité, aucun APK à autoriser.

Les minuteurs flottants et les raccourcis clavier n'existent que dans l'application Windows :
sur téléphone, ce sont les minuteurs intégrés à la page qui servent.

## Fenêtre des minuteurs

Clic droit dessus pour : choisir sa **taille** (60 à 125 %), n'afficher que les **minuteurs**
que vous utilisez, et les **renommer** comme vous voulez. Plus l'opacité du décor et de la
fenêtre, le verrouillage de la position et l'option « toujours au-dessus ».

## Raccourcis par défaut

| Touche | Action |
|---|---|
| F1 | Encens — démarrer / pause |
| F2 | Chasse — démarrer / pause |
| F3 | Recharge — démarrer / pause |
| F4 | Remettre les trois minuteurs à zéro |
| F5 | Taper le rythme des pas |
| F6 | Afficher / masquer la fenêtre des minuteurs |
| F7 | Épingler la fenêtre du journal |
| Maj + F1/F2/F3 | Remettre ce minuteur-là à zéro |

Tout se change dans le bouton ⚙, sans recompiler.

**Jouez en fenêtré sans bordure** pour que les minuteurs restent visibles par-dessus le jeu.
Les raccourcis, eux, fonctionnent dans tous les cas.

---

## Sources des données

Compilées depuis le [Zero-Network Cheat Sheet](https://tybayn.github.io/phasmo-cheat-sheet/),
le wiki Phasmophobia et les notes de mise à jour officielles de Kinetic Games, jusqu'à la v0.19.

Quelques valeurs ne sont documentées que par une seule source — elles sont signalées comme
**non vérifiées** directement dans l'application plutôt que présentées comme des certitudes.
Si vous constatez une erreur en jeu, ouvrez une
[issue](../../issues) : c'est le meilleur service à rendre au projet.

---

## Licence

Voir [`LICENCE.txt`](LICENCE.txt).

En résumé : **utilisez-le et partagez-le librement, tel quel, gratuitement.**
Ne le vendez pas, n'en diffusez pas de version modifiée, et ne retirez pas le nom de l'auteur.

© 2026 Thibault.P (Shwarzyi) — créé le 7 octobre 2026.
Les empreintes SHA-256 de chaque fichier sont dans [`EMPREINTES.txt`](EMPREINTES.txt).
