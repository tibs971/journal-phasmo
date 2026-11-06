# source — la source unique du Journal Phasmo

`phasmo.html`, `web/index.html` et l'artefact en ligne sont **générés** depuis ce dossier.
Ne corrige jamais une page directement : corrige ici, puis reconstruis.

## Le seul fichier à corriger

`donnees.json` — 20 tables :

| table | contenu |
|---|---|
| `GHOSTS` | les 30 entités : preuves, seuil de chasse, vitesses, capacités, indices, confirmations, éliminations, idées fausses |
| `OBS` | les 112 entrées de l'enquête (98 cases, 9 en-têtes de groupe, 5 encarts) |
| `GCAT` | les paliers de vitesse de base atteignables, par entité |
| `HIDCAT` / `HIDSPD` | les vitesses quand l'entité n'a jamais détecté personne |
| `DEPCOND` | les départs conditionnels, et le geste qui lève le doute |
| `CAT` | les noms des cinq paliers |
| `EV` / `EV_ORDER` | les sept preuves |
| `EQUIP`, `DIFFS`, `SIZES`, `MAPS`, `CURSED`, `MYTHS` | équipement, difficultés, tailles de carte, cartes, objets maudits, idées reçues |
| `HUNTSET`, `JUMP`, `LOSLBL`, `BLINK`, `KEYLBL` | durées de chasse, accélération d'un coup, libellés de ligne de vue, clignotement, noms des raccourcis |

Les observations des groupes « Preuves forcées » et « Vitesse de départ » ne sont **pas** dans
`donnees.json` : elles sont calculées depuis `GHOSTS`, `GCAT`, `HIDCAT` et `DEPCOND` au
chargement. Les corriger se fait donc en corrigeant ces tables, pas en ajoutant des cases.

## Reconstruire

```
node construire.js ../phasmo.html     # la page de l'appli Windows
node construire.js                     # ou standalone.html, pour tester à part
```

Le script valide `donnees.json` avant d'écrire et s'arrête sur :

- une table manquante
- une entité citée par une observation ou une table sans exister dans `GHOSTS`
- deux entités ou deux observations avec le même identifiant
- une fiche sans `id` ou sans nom

## Vérifier

```
node teq.js ancienne-page.html ../phasmo.html
```

Plus de mille états comparés un par un. Toute ligne « DIVERGENT » que tu n'as pas voulue est
une régression.

## Note sur les champs des observations

- `g` : la cocher ne garde que ces entités · `x` : la cocher les élimine · `flag` : signale sans éliminer
- `s` : le sous-titre explicatif · `t` : le libellé de la case
- `gAll`/`gHid` et `sAll`/`sHid` : les deux versions, mode normal et mode « resté caché »
- `grp` : une entrée qui ne contient que ça est un en-tête de groupe · `info` : un encart de texte
