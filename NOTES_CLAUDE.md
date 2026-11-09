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

### À venir
- `evaluate` : somme des scores de mes suites − somme des scores des suites adverses, toujours du point de vue de l'IA.
- `play` profondeur 1 : garder le coup qui maximise `evaluate`.
- Compteurs d'appels (`evaluate` et `minmax`).

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
