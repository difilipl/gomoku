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

## 1 bis. Min-Max récursif sans optimisation (branche `minmax-depth2`, 2026-11-10)

### Principe
- `minMax(depth, isMyTurn)` renvoie un **score** (pas un coup) : c'est `play` qui garde le meilleur coup.
- `depth` = nombre de demi-coups qui restent à explorer. Cas de base : `depth == 0` → on renvoie `evaluate()`. Les évaluations ne se font qu'aux feuilles.
- `isMyTurn` (booléen) : à mon tour je pose `playerColor` et je garde le **plus grand** score ; au tour adverse je pose `getEnemyColor()` et je garde le **plus petit** (l'adversaire choisit le coup le pire pour moi, car `evaluate` est toujours de mon point de vue).
- Valeurs de départ : `Integer.MIN_VALUE` pour un max, `Integer.MAX_VALUE` pour un min.
- Appel récursif : profondeur − 1 et `!isMyTurn` (les tours alternent).
- `play` est le premier niveau : elle pose déjà ma pierre, puis appelle `minMax(DEPTH - 1, false)`. Avec `DEPTH = 1`, on retrouve exactement la version précédente (mêmes coups, mêmes appels).
- `DEPTH` est une constante (`private static final`), réglable.
- Couleur adverse : `CellColor` n'a que deux valeurs, et `playerColor` n'est connu qu'après `setColor` (méthode `final` de `AbstractPlayer`), d'où une méthode `getEnemyColor()` plutôt qu'un attribut fixé au constructeur.

### Mesures (même ouverture : IA au centre, puis Noir en 8 9)
- Profondeur 1 : 223 appels pour le premier coup calculé (223 cases vides).
- Profondeur 2 : 49 506 appels = 223 × 222, environ 70 ms.
- Profondeur 3 : 10 940 826 appels = 223 × 222 × 221, environ 8 s (mesuré par Claude, sur une autre machine).
- Chaque niveau multiplie le travail par le nombre de cases vides : complexité **O(b^d)**, avec b ≈ 220 coups possibles et d la profondeur. Une profondeur 4 prendrait de l'ordre d'une demi-heure par coup.
- Conclusion : sans optimisation, la profondeur 3 est déjà à la limite. Il faut réduire b (coups candidats) et couper des branches (alpha-bêta).

### À vérifier
- Le barème asymétrique suppose que l'adversaire joue juste après la position évaluée. En profondeur 2 (et toute profondeur paire), c'est l'IA qui rejoue après la feuille : l'hypothèse s'inverse (mon 4 à un bout libre devient une victoire, le 4 adverse à un bout libre se bouche).
  - `depth` ne peut pas servir à le savoir (il vaut toujours 0 à la feuille). C'est `isMyTurn` qui dit qui joue dans la position évaluée : on le passe à `evaluate`.
  - Les deux tableaux sont en fait « barème du joueur qui va jouer » (`ENEMY_SCORES` actuel) et « barème du joueur qui attend » (`MY_SCORES` actuel). Règle : si c'est à moi de jouer à la feuille, on inverse (mes suites → barème « qui va jouer », suites adverses → barème « qui attend »).
  - À coder plus tard, en renommant les tableaux (`TO_MOVE_SCORES` / `WAITING_SCORES`).
- Ajouter un compteur de nœuds `minMax` (en plus des appels à `evaluate`).

### Préparé pour la coupure sur victoire (version suivante)
- On n'évalue que la position finale de chaque suite de coups : elle contient toutes les pierres posées en route. Défaut : une victoire au milieu du chemin n'arrête pas la recherche, et une victoire immédiate vaut autant qu'une victoire lointaine.
- Correction : juste après avoir posé une pierre, vérifier si elle fait 5. Seule cette pierre peut créer un nouveau 5, donc on regarde seulement autour d'elle (4 directions, dans les deux sens), pas tout le plateau.
- Si oui, on arrête de descendre : victoire → `WIN_SCORE + depth` ; défaite → `-(WIN_SCORE + depth)`. `depth` est la profondeur restante : plus elle est grande, plus la fin est proche. L'IA préfère donc gagner vite et retarder une défaite.
- On ne coupe que sur un 5 (certain), pas sur un 4 ouvert (estimation) : la coupure ne doit pas changer le résultat.

## 2. Versions suivantes (optimisations à comparer)
Pour chaque version, noter le nombre d'appels avant et après.
- Coups candidats : uniquement les cases proches des pierres déjà posées.
- Coupure sur victoire ou défaite.
- Élagage alpha-bêta (exact, ne change pas le coup choisi).
- Ordre des coups pour que l'élagage coupe plus tôt.

## Tableau des mesures

| Version | Profondeur | Appels à evaluate | Nœuds minmax | Remarque |
|---|---|---|---|---|
| Min-Max simple | 1 | 223 (1er coup) | non compté | toutes les cases vides |
| Min-Max simple | 2 | 49 506 (1er coup) | non compté | 223 × 222, ~70 ms |
| Min-Max simple | 3 | 10 940 826 (1er coup) | non compté | 223 × 222 × 221, ~8 s |
