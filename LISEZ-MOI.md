# Journal Phasmo — application Windows

Deux fenêtres, comme LiveSplit :

- **Les minuteurs** : petite fenêtre flottante sans bordure, qui ne prend jamais le focus (option « toujours au-dessus » à cocher si le jeu est en fenêtré sans bordure),
  déplaçable, opacité réglable. C'est elle que tu gardes visible en jouant.
- **Le journal** : fenêtre d'application classique avec tout le reste — difficulté du contrat,
  preuves, les 30 entités, observations, mesure de vitesse, équipement, objets maudits, météo.

Les deux se pilotent avec des **raccourcis globaux**, qui fonctionnent pendant que Phasmophobia
a le focus (`RegisterHotKey`, exactement comme LiveSplit).

## Pour toi : compiler

1. Mettre tous les fichiers dans un même dossier.
2. Double-cliquer `PhasmoCompanion.csproj` → Visual Studio → **F5**.

Sans Visual Studio : installer le SDK .NET 8, puis dans le dossier, `dotnet run`.

## Pour partager l'appli

> **Extrayez d'abord l'archive.** Double-cliquer `publier.cmd` depuis l'aperçu du .zip ne
> marche pas : Windows recopie le script seul dans un dossier temporaire, et la compilation
> s'arrête sur `MSBUILD : error MSB1003: Spécifiez un fichier projet ou solution`.
> Clic droit sur le zip → **Extraire tout**, ouvrir le dossier obtenu, et lancer `publier.cmd`
> depuis là — il doit se trouver à côté de `PhasmoCompanion.csproj`.
> En terminal, même chose : `cd` dans ce dossier avant `dotnet publish`.

Double-clique **`publier.cmd`**. Il fabrique le dossier `dist` avec **un seul fichier** :
`PhasmoCompanion.exe`. Le journal et .NET sont embarqués dedans.

Tu envoies cet exe, tes amis double-cliquent. Rien d'autre à installer, rien à mettre à côté.

Deux points à leur dire :
- Windows affichera peut-être « Windows a protégé votre ordinateur » au premier lancement,
  parce que l'exe n'est pas signé numériquement (une signature coûte cher). Il faut cliquer
  « Informations complémentaires » puis « Exécuter quand même ».
- L'appli a besoin du **Microsoft Edge WebView2 Runtime**, déjà présent sur Windows 11 et sur la
  quasi-totalité des Windows 10. S'il manque, un message le dit et il s'installe gratuitement
  depuis le site de Microsoft.

## Réglages dans l'application (bouton ⚙)

Pas besoin de Visual Studio pour régler quoi que ce soit :

Le bouton ⚙ ouvre un panneau **dans la fenêtre** (pas de seconde fenêtre).

- **Raccourcis** : clique le bouton d'une action, appuie sur la touche voulue (avec Ctrl, Alt
  ou Maj si tu veux). Échap annule, un bouton remet les valeurs d'origine.
  Le panneau « Raccourcis clavier » du journal lui-même pilote les mêmes touches : ce que tu y
  choisis pour l'encens, la chasse, la recharge et le tap devient aussitôt le raccourci global
  des minuteurs, et Maj + la même touche remet ce minuteur à zéro. Tant que le panneau
  est ouvert, les raccourcis globaux sont désactivés — sans ça, appuyer sur F1 déclencherait un
  minuteur au lieu d'être enregistré. Si une touche est déjà prise par un autre logiciel, la
  barre du haut le dit.
- **Transparence des minuteurs**, deux réglages séparés :
  - *Décor* : cadre, fonds et contours s'effacent, les chiffres restent nets. À 0 %, il ne reste
    que le texte, en blanc et en gras, avec une ombre portée pour rester lisible sur le jeu ;
    une poignée ⠿ en haut à gauche permet toujours d'attraper la fenêtre.
  - *Fenêtre entière* : fait aussi pâlir le texte.

  Les deux se retrouvent aussi dans le clic droit sur la fenêtre des minuteurs.

## Raccourcis par défaut (fonctionnent en jeu)

| Touche | Effet |
|---|---|
| F1 | Encens : démarrer / pause |
| F2 | Chasse : démarrer / pause (en l'arrêtant, la recharge démarre toute seule) |
| F3 | Recharge : démarrer / pause |
| F4 | Remettre les 3 minuteurs à zéro |
| F5 | Taper le rythme des pas (mesure de vitesse, dans le journal) |
| F6 | Afficher / masquer la fenêtre des minuteurs |
| F7 | Épingler / désépingler la fenêtre du journal |
| Maj + F1/F2/F3 | Remettre ce minuteur à zéro |
| Maj + F5 | Remettre la mesure de vitesse à zéro |

Tout cela se change dans ⚙.

## Fenêtre des minuteurs

- Glisser pour déplacer, clic droit pour les options (opacité, verrouillage de la position,
  « lancer la recharge à la fin de la chasse », masquer).
- La ligne du haut rappelle **tes** touches, pas des touches figées : « F1/F2/F3 démarrer ou
  pause · F4 remise à zéro · Professionnel ». Elle se met à jour dès que tu changes un
  raccourci dans ⚙ ou dans le journal, et le menu du clic droit aussi.
- Chaque minuteur à l'arrêt affiche sa propre touche (« F2 pour démarrer »).
- Repères affichés : encens 60 s (Démon) / 90 s / 180 s (Esprit), recharge 20 s (Démon) / 25 s,
  et surtout **la période de grâce et la durée de chasse de la difficulté choisie dans le
  journal** : changer de difficulté change ce que la fenêtre annonce.
- Position et opacité retenues dans `%AppData%\PhasmoCompanion\timers.txt`.

## Plein écran

> **Le jeu bascule tout seul du plein écran au mode fenêtré ?** C'est Windows : dès qu'une
> fenêtre « toujours au-dessus » s'affiche par-dessus un jeu en plein écran **exclusif**, il
> perd ce mode. Le journal n'est donc plus épinglé par défaut — le bouton 📌 (ou F7) reste là
> si vous le voulez. Pour les minuteurs, l'option « Toujours au-dessus du jeu » est dans le
> menu du clic droit ; décochez-la si vous jouez en plein écran exclusif, vous les retrouverez
> en passant par Alt+Tab. En **plein écran sans bordure**, rien de tout cela ne se produit :
> c'est le mode à préférer pour garder les minuteurs visibles en jeu.


Une fenêtre « toujours au-dessus » s'affiche par-dessus un jeu en fenêtré ou en fenêtré sans
bordure, mais pas en plein écran **exclusif** — c'est la limite de LiveSplit, et la raison pour
laquelle Discord, lui, injecte une DLL dans le jeu. Phasmophobia étant un jeu Unity, son mode
plein écran est en pratique un plein écran fenêtré, donc l'overlay s'affiche. Si ce n'est pas le
cas chez quelqu'un, il passe en fenêtré sans bordure dans les options d'affichage.
Les raccourcis, eux, marchent dans tous les cas.

## Fenêtre du journal

La barre de titre est maison : **—** réduit, **▢** agrandit (ou double-clic sur la barre),
**❐** restaure, **✕** ferme. Glisser la barre déplace la fenêtre — et si elle est agrandie,
elle se restaure d'elle-même pour suivre la souris.

Agrandie, elle s'arrête à la zone de travail et laisse la barre des tâches visible : sans cela
Windows la dimensionne à l'écran entier et la barre de titre passe sous le bord haut, hors
d'atteinte.

## Ce qui est retenu d'une fois sur l'autre

Rien n'est à refaire au lancement suivant :

- **Les raccourcis** — dans `%AppData%\PhasmoCompanion\hotkeys.txt` côté appli, et dans le
  stockage du journal côté page. Au démarrage, l'appli propose ses touches au journal ; si le
  journal a déjà les siennes, ce sont elles qui gagnent, et l'appli s'y range.
- **La difficulté, la carte, les favoris, l'enquête en cours, la Lune de sang** — retenus par le
  journal lui-même.
- **Position, opacité du décor, opacité de la fenêtre et options des minuteurs** —
  `%AppData%\PhasmoCompanion\timers.txt`, en lignes `clé=valeur` : une clé absente garde sa
  valeur par défaut au lieu de faire tout retomber à zéro.
- **Taille, position, opacité, épinglage et état agrandi de la fenêtre principale** —
  `%AppData%\PhasmoCompanion\window.txt`.
- **Minuteurs affichés ou masqués** — retrouvés dans l'état où vous les aviez laissés.

Détail technique, au cas où : la page est recopiée dans `%AppData%\PhasmoCompanion\page` et
servie sous un vrai nom d'hôte (`https://journal.phasmo`). Sans cela, le navigateur embarqué
considère la page comme sans origine et lui refuse tout stockage — c'est exactement ce qui
faisait oublier les raccourcis à chaque fermeture.

## Signature d'auteur

L'application contient un panneau de signature, ouvert par une combinaison de touches connue
de son auteur et protégé par deux codes successifs. Les codes ne sont écrits nulle part :
seules des empreintes SHA-256 salées figurent dans le code, donc ils n'apparaissent pas en
clair dans l'exécutable.

La combinaison exige l'**Alt gauche** et refuse l'**Alt droit (AltGr)**. Sous Windows, AltGr
équivaut à Ctrl + Alt, et un raccourci enregistré normalement ne sait pas distinguer les deux
touches Alt : il se déclencherait en tapant un caractère obtenu avec AltGr. D'où le hook
clavier de `SignatureHook.cs`, qui lit l'état de chaque touche séparément — et qui ne consomme
jamais aucune touche, le jeu reçoit tout normalement.

Le panneau final affiche l'auteur, la date de création, l'outil utilisé et sa version, et
surtout l'**empreinte SHA-256 de l'exécutable**. C'est elle qui a une vraie valeur de preuve :
publiez-la quelque part d'horodaté et vous pourrez montrer plus tard ce qui existait et quand.

Soyons clairs sur ce que cela fait et ne fait pas : c'est une **signature**, pas une serrure.
Quelqu'un qui décompile l'exe peut retirer la vérification. Ce qui protège vraiment une
paternité, c'est une trace datée et extérieure — un dépôt Git public, une archive en ligne,
un envoi daté — pas un code caché dans le programme.

## Mettre à jour les données du jeu

Le journal est embarqué dans l'exe. Pour le mettre à jour sans recompiler, pose un fichier
`phasmo.html` plus récent **à côté de l'exe** : s'il est complet, il remplace la version
embarquée. Sinon l'appli ignore le fichier et garde la sienne — c'est ce qui évite la fenêtre
blanche si le fichier est tronqué au téléchargement.

En cas de souci d'affichage, **F12** ouvre les outils de développement de la page.
