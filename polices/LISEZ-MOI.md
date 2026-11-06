# Polices embarquées dans la page

Elles ne sont plus chargées depuis Google : elles sont **dans le fichier**, réduites aux
seuls caractères que le journal affiche. Quatre polices pour 44 Ko au total.

Pourquoi : la page web s'installe et se veut utilisable **hors ligne**. Tant qu'elle
chargeait ses polices depuis internet, elle perdait sa typographie dès qu'il n'y avait
plus de réseau — ce qui est précisément le moment où l'on comptait sur elle.

| police | rôle | licence |
|---|---|---|
| **Atkinson Hyperlegible** | tout le texte lu | SIL OFL 1.1 |
| **IBM Plex Mono** | les valeurs mesurées | SIL OFL 1.1 |
| **Special Elite** | le titre seul (11 caractères) | Apache 2.0 |

Les trois licences autorisent la redistribution dans une application, y compris
commerciale. Leur texte complet est à côté et **doit rester livré** avec le projet.

Atkinson Hyperlegible est dessinée par le Braille Institute pour rester lisible en
mauvaises conditions de vision. Ce n'est pas un choix de style : le journal se lit vite,
souvent dans le noir. Ses lettres proches — I, l, 1 — sont dessinées différemment, ce qui
compte quand on lit « 1,53 m/s » à la volée.

## Refaire la réduction

Si tu ajoutes des caractères inhabituels dans `donnees.json`, il faut refaire la
réduction, sinon ils s'afficheront dans la police de secours. Les polices complètes se
récupèrent sur github.com/google/fonts, et la réduction se fait avec `fontTools` :

```
python -m fontTools.subset <police.ttf> --text="<les caracteres>" --output-file=<sortie.ttf> --layout-features= --no-hinting
```
