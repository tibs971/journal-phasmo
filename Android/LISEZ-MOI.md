# Journal Phasmo — application Android

Aide d'identification des entités de *Phasmophobia*, entièrement hors ligne.
Application non officielle, gratuite et sans but commercial, sans lien avec Kinetic Games.

## L'essentiel en une phrase

Le contenu du journal — les 30 fiches, les 98 observations, les tables de vitesses — n'est
pas écrit en Kotlin : il est lu depuis `app/src/main/assets/donnees.json`, **le même fichier
que la page web et l'application Windows**. Une correction faite une fois vaut pour les trois.

## Première ouverture

1. Ouvre le dossier dans Android Studio (*Open*, pas *Import*).
2. Laisse-le télécharger ce qu'il lui manque — c'est long la première fois.
3. Lance les tests avant toute autre chose :

```
gradlew test
```

Si ça passe, le moteur d'identification de l'application rend **exactement** les mêmes
résultats que la page web, sur 1 300 états différents. C'est le test `MoteurTest`, et c'est
la seule chose qui prouve que le portage est fidèle — ne construis rien avant qu'il passe.

Aucun téléphone ni émulateur n'est nécessaire pour ce test : le moteur est du Kotlin pur.

## Le plus simple : double-clic

```
publier-android.cmd
```

Comme `publier.cmd` du projet Windows. Il retrouve tout seul le SDK Android et le JDK —
y compris ceux installés par Unity avec le module *Android Build Support* —, **lance
d'abord le test du moteur**, et ne construit l'application que si les 1 300 états sont
conformes à la page web. Résultat dans `dist\JournalPhasmo.apk`.

Si le SDK manque, le script ne se contente pas d'échouer : il affiche quoi faire.

**Il s'adapte à ce que tu as déjà.** Plutôt que d'exiger un niveau d'API précis — ce qui
obligerait Gradle à télécharger, donc à écrire dans le SDK et à faire accepter des
licences —, le script regarde quelles plateformes et quels build-tools sont installés,
prend les plus récents, et les passe à Gradle. C'est ce qui permet de construire avec le
SDK livré par Unity, qui est dans `Program Files` et donc en **lecture seule**.

Si ça ne suffit pas, la solution définitive est d'installer
[Android Studio](https://developer.android.com/studio) : il installe un SDK dans un
dossier où tu peux écrire, et son *SDK Manager* fait accepter les licences tout seul. Le
script prendra alors ce SDK-là en priorité, sans que tu aies rien à changer. Tu en auras
de toute façon besoin pour l'association du téléphone en Wi-Fi et pour lire les journaux
de l'application.

### Pour une version signée, utilisable sur Google Play

```
creer-cle.cmd        (une seule fois)
publier-android.cmd
```

`creer-cle.cmd` fabrique ta clé de signature et note son mot de passe dans
`keystore.properties`. Les deux fichiers sont déjà dans `.gitignore`, donc ils ne
partiront jamais sur GitHub. **Sauvegarde-les ailleurs le jour où tu les crées** : sans
eux, plus aucune mise à jour de l'application n'est possible, jamais — ni par toi, ni par
Google. Une fois la clé en place, `publier-android.cmd` produit en plus le
`dist\JournalPhasmo.aab` que demande la console Google Play.

### Ce que Google change en ce moment, et qui te concerne

Google verrouille l'installation d'applications hors Play. Sa page de vérification des
développeurs dit : « *Effective September 30, 2026, Protections begin for users
installing apps from participating stores in select regions on certified Android
devices* » — pour l'instant le Brésil, l'Indonésie, Singapour et la Thaïlande — et
« *Verification capability will be expanded globally for all apps on certified Android
devices in 2027* ».

Trois conséquences concrètes :

- **Sur ton téléphone, par le câble, aucun problème, ni maintenant ni après.** La FAQ est
  explicite : « *Apps installed using ADB won't require verification* ».
- **Partager l'APK à des amis marche aujourd'hui** depuis la France, et la FAQ dit que
  pour un partage direct « *these new verification requirements won't apply to your app
  yet* ». Le mot important est *yet*.
- **Les deux routes durables** sont donc Google Play, et la version web du projet
  Windows, qui n'est soumise à rien de tout ça et s'installe depuis le navigateur.

Il existe aussi un **compte à distribution limitée**, gratuit et sans pièce d'identité,
pour partager avec un petit nombre d'appareils autorisés. Les pages de Google se
contredisent sur la limite — 20 sur l'une, 50 sur l'autre — donc je ne te donne pas de
chiffre ferme.

Une chose que je n'ai pas pu vérifier : ce que Play Protect affiche exactement au moment
d'installer une application hors Play. La page d'aide concernée n'a pas répondu, et je
préfère ne rien affirmer.

## Les autres chemins, à la main

Trois chemins. Le deuxième est le plus simple si tu veux juste voir l'écran.

### Par le câble, depuis Android Studio

1. Sur le téléphone : **Paramètres → À propos du téléphone**, puis appuie **sept fois**
   sur « Numéro de build ». Les options pour développeurs apparaissent.
2. **Paramètres → Système → Options pour développeurs → Débogage USB** : active.
3. Branche le câble. Le téléphone demande d'autoriser l'ordinateur : accepte.
4. Dans Android Studio, le téléphone apparaît en haut ; clique sur ▶ *Run*.

L'application s'installe et se lance. Chaque modification se renvoie d'un clic — c'est le
chemin à prendre dès que tu veux itérer.

### Par un fichier APK, sans câble

```
gradlew assembleDebug
```

Le fichier sort ici :

```
app/build/outputs/apk/debug/app-debug.apk
```

Envoie-le toi-même sur ton téléphone (Drive, Telegram, un câble en simple transfert de
fichiers), ouvre-le depuis le gestionnaire de fichiers, et autorise l'installation
d'applications depuis cette source quand Android le demande. C'est une version de
**débogage**, signée automatiquement : parfaite pour essayer, pas pour publier.

### Sans fil, si ton téléphone est en Android 11 ou plus

**Options pour développeurs → Débogage sans fil → Associer l'appareil avec un code**,
puis dans Android Studio : *Pair devices using Wi-Fi*. Pratique quand le téléphone est
posé à côté du clavier, ce qui est justement son usage ici.

### Et plus tard, par Google Play

Le canal de **test interne** de la console distribue une version à toi-même et à
quelques personnes en quelques minutes, sans attendre les 12 testeurs ni les 14 jours du
test fermé. Mais il demande un `.aab` **signé**, donc ta clé de signature — voir plus
bas, c'est le seul fichier du projet que tu ne dois jamais perdre ni versionner.

## Ce qui est fait, ce qui ne l'est pas

Fait :

- lecture de `donnees.json` (lecteur JSON écrit à la main, sans dépendance)
- le moteur complet : `reason` et `evidenceReason` portés à l'identique, les deux
  générateurs d'observations et le mode « resté caché »
- **quatre destinations** dans une barre de navigation basse, qui devient un rail
  latéral en paysage
- **écran Enquête** : preuves et vitesses en puces à trois états, nombre de preuves en
  groupe de boutons à choix unique, 98 observations en groupes repliables, bandeau fixe
  en haut avec les entités restantes
- **écran Entités** : les 30 fiches, celles qui tiennent puis les écartées avec leur raison
- **écran Chasse** : quatre minuteurs, 2 × 2 en portrait et en rangée en paysage, plus
  les vitesses des entités encore possibles
- **écran Guide** : difficultés, équipement, cartes, objets maudits, idées reçues
- thème clair et sombre suivant le réglage du téléphone, sans une couleur en dur
- l'enquête est gardée : tourner le téléphone ou fermer l'application ne la perd pas
- le test de non-régression à 1 300 cas

Pas encore fait :

- la mesure de vitesse au Tap, et les durées de chasse selon la difficulté et la carte
  (les données sont dans `donnees.json`, le calcul n'est pas câblé — je n'afficherai pas
  des durées tant qu'elles ne seront pas justes)
- la recherche dans tout le journal
- la santé moyenne et le comportement en ligne de vue, qui existent dans le moteur mais
  n'ont pas encore de contrôle à l'écran

## Parti pris visuel

**Le relevé, pas l'affiche d'horreur.** L'horreur est dans le contenu ; l'interface est
un instrument qu'on lit dans le noir, du coin de l'oeil, une main sur le clavier. Ce que
fait vraiment le journal, c'est rayer 29 noms sur 30 — donc c'est l'élimination qui porte
le design : les noms écartés sont réellement barrés, et la raison est écrite à côté.

**Un seul encadré dans toute l'application** : le panneau des entités restantes, en haut
de l'enquête. Partout ailleurs, des filets et des retraits. Découper chaque bloc en
cartes identiques, avec le même arrondi et la même ombre quelle que soit l'importance,
aplatit la hiérarchie au lieu de la créer — c'était le défaut de la première version.

**Typographie.** Le texte en **Atkinson Hyperlegible**, dessinée par le Braille Institute
pour rester lisible en mauvaises conditions de vision : c'est un choix de fonction, pas de
style. Les valeurs mesurées en **IBM Plex Mono**, en largeur fixe, pour que deux vitesses
posées l'une sous l'autre se comparent d'un coup d'oeil au lieu de se lire. Les deux sont
sous licence SIL OFL, embarquées dans `res/font`, licences dans `polices/`.

**Couleurs.** Base ardoise froide, jamais du noir pur. Accent **ambre** — la couleur des
lampes qui préservent la vision nocturne et des afficheurs d'EMF. **Cyan** froid pour ce
qui est retenu. Et le **rouge est réservé à la chasse** : employé partout il ne voulait
plus rien dire, employé rarement il redevient une alarme.

**Casse normale pour les titres de section.** Les libellés tout en majuscules au-dessus de
chaque bloc se lisent plus lentement, ce qui compte ici plus qu'ailleurs.

## Thèmes et remise à zéro

Une barre de titre coiffe l'application, avec deux actions dans son menu.

**Nouvelle enquête** efface tout ce qui est coché — preuves, vitesses, observations,
entités écartées à la main, votre réponse — après confirmation, en annonçant ce qui part.
Le thème et les noms donnés aux minuteurs sont **conservés** : ce sont des réglages, pas
des relevés, et les perdre au milieu d'une partie serait une mauvaise surprise.

**Thème** propose cinq choix :

| thème | pour quoi |
|---|---|
| Suivre le téléphone | le réglage clair ou sombre du système |
| Ardoise | la palette de référence, pensée pour une pièce éteinte |
| Clair | préparer un contrat en plein jour |
| Carnet | papier et encre sépia, le carnet de l'enquêteur |
| Vision nocturne | une seule teinte verte, comme un écran de caméra infrarouge |

« Vision nocturne » n'est pas une coquetterie : le vert préserve l'adaptation de l'oeil
au noir, et comme tout y est de la même teinte, les états se distinguent par la
**luminosité**. L'ambre de l'alarme, seule couleur étrangère, saute alors littéralement
aux yeux quand une chasse commence.

Il n'y a **pas de thème « sang »** comme sur la page web, et c'est volontaire : le rouge
sert ici à signaler la chasse. Peindre toute l'application en rouge lui retirerait ce
sens. Si tu le veux quand même, dis-le — mais il faudra alors donner à la chasse un autre
signal que la couleur.

## Choix d'interface, et pourquoi

**Barre basse plutôt que menu à trois traits.** La doc de Google dit « *The navigation
drawer is being deprecated in the Material 3 expressive update* », et son remplaçant —
le rail étendu — n'existe que sur grand écran. Au-delà de la règle : un menu coûte deux
gestes, la barre un seul, et on s'en sert une main sur le clavier. Quatre destinations,
quand la doc en autorise trois à cinq.

**Puces à trois états, un appui à la fois.** Un appui retient, deux écartent, trois
reviennent au neutre — exactement le geste de la page web et de l'application Windows.
La doc donne les puces filtre comme « *a good alternative to toggle buttons or
checkboxes* », et leur zone tactile fait 48 dp même quand la puce n'en mesure que 32,
donc on peut taper sans regarder. Une légende sous chaque bloc dit le geste, parce que
le double appui ne se devine pas.

**Cases à cocher pour les 98 observations, pas de croix.** Une observation est binaire :
soit on l'a constatée, soit non. Les cas négatifs sont déjà écrits comme des
observations à part — « N'a PAS foncé de loin… », « Pas de souffle glacé… ». Un
troisième état n'aurait rien signifié.

**Le bandeau du haut ne défile pas.** Pendant qu'on parcourt les observations, le nombre
d'entités restantes et leurs noms restent visibles. C'est le geste réel du jeu : on jette
un oeil, on ne lit pas.

**La croix d'une entité s'annule.** Écarter une entité à la main affiche « Annuler »
pendant six secondes, et l'écran Entités propose « Remettre dans la liste ». Un appui
malheureux ne condamne pas la partie.

**Le bouton segmenté est déprécié**, remplacé côté design par le *button group*. Côté
bibliothèque Android, ce composant n'existe qu'en version alpha : le projet reste sur la
version stable 1.12.0 et son prédécesseur `MaterialButtonToggleGroup`, qui fait le même
travail. Pas d'alpha dans une application destinée à être publiée.

## Une question qui reste ouverte

Dans la table `DIFFS` de `donnees.json`, six clés ne sont pas affichées dans le Guide
parce que je ne suis pas certain de ce qu'elles désignent : `gs`, `ps`, `pre`, `free`,
`sure` et `lvl`. Plutôt que de leur coller un libellé inventé, je ne les montre pas.
Dis-moi ce qu'elles veulent dire et je les ajoute.

## Deux choix irréversibles

**L'identifiant d'application** est `io.github.tibs971.journalphasmo`, construit sur ton
compte GitHub puisque tu n'as pas de nom de domaine. Une fois l'application publiée sur
Google Play, **il ne peut plus jamais changer** — ni pour cette application, ni en en
republiant une autre sous le même nom. Si tu veux autre chose, c'est maintenant :
`app/build.gradle.kts`, le champ `applicationId`, et `namespace` juste au-dessus.

**La clé de signature.** Google Play identifie ton application par sa clé. Si tu la perds,
tu ne peux plus publier de mise à jour, jamais. Génère-la depuis Android Studio
(*Build → Generate Signed App Bundle*), sauvegarde le fichier `.jks` **et** son mot de passe
ailleurs que sur ton ordinateur. Je ne peux pas la générer pour toi et je ne dois pas :
elle ne doit exister que chez toi.

## Corriger le contenu

Tout se passe dans le projet Windows, dossier `source` :

```
cd  <projet Windows>/source
#  on corrige donnees.json, puis :
node construire.js ../phasmo.html                 # la page web et l'appli Windows
node exporter.js   ../phasmo.html reference-moteur.json   # la nouvelle reference du moteur
node vers-android.js  <ce projet>                 # recopie ici
gradlew test                                       # et on verifie
```

L'ordre compte : la référence doit être refaite **après** la page, sinon le test compare
l'application à l'ancien comportement.

## Avant de publier sur Google Play

Ce que la console t'imposera, et que ni toi ni moi ne pouvons contourner :

- un test fermé avec **au moins 12 testeurs inscrits**, sur **au moins 14 jours**, avant
  même de pouvoir *demander* l'accès en production (règle des comptes personnels)
- une déclaration de sécurité des données : celle-ci est simple, l'application ne collecte
  rien, ne demande aucune permission et n'a aucun accès réseau — vérifiable dans
  `AndroidManifest.xml`, qui ne déclare aucune permission
- une classification du contenu
- une fiche Play Store : description, captures, icône

Point à régler avant de publier : la *Fan Content Creation Policy* de Kinetic Games
interdit d'utiliser du matériel tiré du jeu. L'application la respecte — aucune texture,
aucun son, aucune image du jeu, tout est fait à la main. Reste à vérifier ce que leur
politique dit d'une distribution via une boutique, même gratuite et sans publicité.

## Structure

```
app/src/main/assets/donnees.json      le contenu — source unique, ne pas editer ici
app/src/main/java/.../Json.kt         lecteur JSON minimal, sans dependance
app/src/main/java/.../Donnees.kt      le modele + les deux generateurs d'observations
app/src/main/java/.../Etat.kt         ce que le joueur a coche
app/src/main/java/.../Moteur.kt       les regles d'identification
app/src/main/java/.../MainActivity.kt l'ecran d'enquete
app/src/test/resources/               donnees.json + reference-moteur.json pour le test
app/src/test/java/.../MoteurTest.kt   la preuve que le portage est fidele
```

Aucune dépendance pour l'application elle-même : ni AndroidX, ni Compose, ni bibliothèque
JSON. Seul `junit` pour les tests. C'est volontaire — moins de versions à faire concorder,
moins de choses qui cassent à la prochaine mise à jour d'Android Studio.
