# Notes de démarche (tenues avec Claude)

Base pour le compte rendu. Ce fichier est mis à jour au fil des choix.
Mesure principale : nombre d'appels à la fonction d'évaluation (et nombre de nœuds de `minmax`).

## 1. Version de base : MinMaxSimplePlayer, profondeur 1

### Premier coup
- Si le plateau est vide, on joue au centre : `(SIZE - 1) / 2` en ligne et en colonne, soit (7, 7) sur 15×15.
- Pourquoi : c'est la case qui offre le plus d'alignements possibles, et on économise 225 évaluations inutiles.

### Parcours des coups
- Version de base : on essaie **toutes les cases vides**. Un seul indice `i` de 0 à `SIZE * SIZE - 1`, avec `row = i / SIZE` et `column = i % SIZE`.
- Attention : `SIZE` est la longueur d'un côté (15), pas le nombre de cases (225).
- On pose la pierre, on évalue, puis on la retire. On ne copie pas le plateau, car copier 225 cases à chaque essai coûterait bien plus cher.

### Comptage d'une suite (`isStarting` / `lenthcut`)
- 4 directions seulement (W, SE, S, SW) : les 4 autres sont les mêmes lignes lues à l'envers.
- On ne compte une suite qu'à partir de sa **première pierre** dans la direction (`isStarting`). Sinon, une suite de 4 serait aussi comptée comme un 3, un 2, etc.
- Une suite est décrite par sa longueur et son nombre d'**extrémités libres** (0, 1 ou 2), stockés dans la classe `Sequence`.
- Une extrémité est libre si la case juste avant le début, ou juste après la fin, est vide et sur le plateau.

### Barème (`scoreSequence`)

| Longueur | 2 bouts libres | 1 bout libre | 0 bout libre |
|---|---|---|---|
| 5 ou plus | 1 000 000 (victoire) | 1 000 000 | 1 000 000 |
| 4 | 100 000 | 10 000 | 0 |
| 3 | 1 000 | 100 | 0 |
| 2 | 10 | 1 | 0 |

Justifications :
- **Facteur 10 entre paliers** : aucune accumulation de petits motifs ne doit battre un motif plus dangereux.
- **4 ouvert ≈ victoire** : l'adversaire ne peut boucher qu'un seul bout.
- **0 bout libre = 0** : la suite ne pourra jamais faire 5.
- **5 ou plus, testé en premier** : c'est une victoire même si la suite est bloquée. Le plateau fourni (`hasWinner`, recherche de "WWWWW" avec `indexOf`) compte aussi 6 pierres alignées comme une victoire. Un 6 peut apparaître d'un coup (`XXX_XX` puis on remplit le trou).
- **Un bout libre = score ouvert / 10** (`HALF_OPEN_FACTOR`). Ça évite de dupliquer le barème.
- Toutes les valeurs sont des constantes nommées (`WIN_SCORE`, `OPEN_SCORE_BY_LENGTH`, ...).

### Évaluation (`evaluate`), version naïve
- Version volontairement simple : elle sert de **référence** pour mesurer chaque optimisation.
- On parcourt toutes les cases. Les cases vides sont sautées.
- Pour chaque pierre, on regarde 4 directions et on ne mesure la suite que si la pierre en est le début (`isStarting`). Sinon, la même suite serait comptée plusieurs fois.
- Le score de la suite va dans *mes points* si la couleur est `playerColor`, sinon dans *points adverses*. On renvoie mes points − points adverses, toujours du point de vue de l'IA.
- Compteurs : deux attributs de la classe. L'un est remis à zéro au début de `play` (appels **par coup**), l'autre n'est jamais remis à zéro (total de la partie).
  - Par coup est la mesure à comparer : le total dépend de la longueur de la partie.
  - Attributs et pas paramètre : en Java, un `int` est passé par valeur, et avec la récursion `evaluate` sera appelée loin de `play`.

### Pièges rencontrés
- `row` / `column` en attributs : `evaluate` les modifie pendant que `play` s'en sert dans sa propre boucle, donc `play` perd la case qu'il examinait. Ces variables doivent être **locales** à chaque méthode.
- Compteurs : attributs d'instance et non `static`, sinon deux IA qui jouent l'une contre l'autre partagent le même compteur.

### Choix du coup (`play`), profondeur 1
- Pour chaque case vide : on pose ma pierre, on appelle `evaluate`, on retire la pierre, et on garde le coup au meilleur score.
- Meilleur score de départ : `Integer.MIN_VALUE` et pas 0. Si l'adversaire menace, tous les scores sont négatifs, et avec 0 aucun coup ne serait retenu.
- La pierre est retirée juste après l'évaluation, pour que le plateau soit intact pour la case suivante.
- Nombre d'appels attendu en profondeur 1 : un par case vide, soit au plus 225 − nombre de pierres posées.

### Test de la profondeur 1 (partie contre un humain, 2026-11-09)
- L'IA (blanc) a perdu en 6 coups. L'humain a fait une diagonale de (3,3) à (6,6), bloquée d'un côté par une pierre blanche, avec (2,2) libre.
- Au tour 11, bloquer en (2,2) rapportait environ +10 000, alors que se faire un 4 ouvert en colonne 8 rapportait environ +90 000 (100 000 − 10 000). L'IA a attaqué.
- Elle ne voit pas que l'adversaire joue juste après et gagne avant elle. Ce n'est pas un bug, c'est la **limite de la profondeur 1**, et ça justifie le passage au Min-Max en profondeur 2.
- Temps par coup : 0 à 4 ms.

### Piste : barème asymétrique (version à comparer)
`evaluate` est toujours appelée juste après mon coup, donc c'est toujours à l'adversaire de jouer. Une même menace ne vaut pas la même chose selon sa couleur.

| Motif | Mes suites | Suites adverses (soustraites) |
|---|---|---|
| 5 ou plus | 1 000 000 | 1 000 000 |
| 4, 2 bouts libres | 100 000 | 500 000 |
| 4, 1 bout libre | 1 000 | 500 000 |
| 3, 2 bouts libres | 1 000 | 50 000 |
| 3, 1 bout libre | 100 | 100 |
| 2, 2 bouts libres | 10 | 10 |
| 2, 1 bout libre | 1 | 1 |

- Un 4 adverse avec au moins un bout libre est complété au coup suivant : presque une défaite.
- Un 3 ouvert adverse devient un 4 ouvert : très dangereux, mais moins que mon 4 ouvert, avec lequel je gagne avant lui.
- Mon 4 avec un seul bout libre sera simplement bouché.
- Vérification sur le tour 11 de la partie test : bloquer en (2,2) vaut environ +1 000, jouer (9,8) vaut 100 000 − 500 000 = −400 000. L'IA bloque.
- Implémenté : tableaux `MY_SCORES` / `ENEMY_SCORES` et paramètre `isMine` dans `scoreSequence`. La constante `USE_ASYMMETRIC_SCALE` permet de revenir au barème symétrique pour comparer.
- Vérifié sur la position du tour 11 : barème symétrique → (9, 8), partie perdue ; barème asymétrique → (2, 2), menace bloquée.
- Limite : c'est un pansement. Il ne voit pas les pièges à deux coups, seul le Min-Max le fait.

### À venir
- Récursion `minmax` (profondeur 2 puis 3), compteur de nœuds.

## 2. Versions suivantes (optimisations à comparer)
Pour chaque version, noter le nombre d'appels avant et après.
- Coups candidats : uniquement les cases proches des pierres déjà posées.
- Coupure sur victoire ou défaite.
- Élagage alpha-bêta (exact, ne change pas le coup choisi).
- Ordre des coups pour que l'élagage coupe plus tôt.

## Tableau des mesures

| Version | Profondeur | Appels à evaluate | Nœuds minmax | Remarque |
|---|---|---|---|---|
| | | | | |
