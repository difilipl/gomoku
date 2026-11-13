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

## 1 ter. Coups candidats (branche `candidate-moves`, 2026-11-11)

### Principe
- On n'essaie plus toutes les cases vides, seulement celles qui ont une pierre (de n'importe quelle couleur) à au plus 2 cases en ligne et en colonne (carré 5 × 5 autour de la case).
- Distance 2 plutôt que 1 : à distance 1, on raterait des coups qui préparent un motif à trou comme `X _ X`.
- Méthode `isNeighbour(row, column)` : deux boucles sur l'écart de ligne et de colonne, de -2 à +2 ; `continue` si la case sort du plateau ; `return true` dès qu'une pierre est trouvée ; `false` sinon.
- Coût : au plus 24 cases lues, bien moins qu'un `evaluate` (tout le plateau × 4 directions). Et chaque case éliminée évite tout un sous-arbre.
- Condition dans `play` et `minMax` : `case vide && isNeighbour` (le test le moins cher d'abord, `&&` s'arrête au premier faux).
- Piège évité : `!isColor(r, c, null)` vaut `true` hors plateau, d'où le test `isValid` d'abord.

### Mesures (même ouverture : IA en (7, 7), Noir tape « 8 9 » = case (7, 8), car `HumanPlayer` retire 1)
- Profondeur 1 : 28 appels (avant 223). Calcul : 25 + 25 − 20 (recouvrement 5 × 4) − 2 cases occupées.
- Profondeur 2 : 1 050 mesurés (avant 49 506).
- Profondeur 3 : 48 380 mesurés (avant 10 940 826).
- Le gain est d'environ (223 / 28)^d : il grandit avec la profondeur, car on réduit b dans O(b^d).

### Limites
- Mesuré en début de partie : en milieu de partie, il y a plus de candidats (souvent 60 à 100), le gain baisse mais reste grand.
- Ce n'est pas exact (contrairement à l'alpha-bêta) : on suppose qu'un coup loin de toute pierre n'est jamais le meilleur, ce qui est presque toujours vrai au Gomoku.

## 1 quater. Coupure sur victoire (branche `win-cutoff`, 2026-11-11)

### Principe
- Juste après avoir posé une pierre (dans `play` et dans `minMax`), `isWinningMove(row, column, color)` regarde si **cette** pierre fait un 5. Seule elle peut créer un nouveau 5, donc on ne regarde qu'autour d'elle.
- `isWinningMove` : pour chacune des 4 directions, une seule boucle de `i = -(WIN_LENGTH - 1)` à `+(WIN_LENGTH - 1)` couvre les deux sens. Compteur +1 si la case est de ma couleur, remis à 0 sinon (une suite doit être continue : `X X _ X X X` n'est pas un 5). Test `>= WIN_LENGTH` à chaque case, pour ne pas rater un 5 suivi d'une case vide. `isColor` gère les bords.
- Coût : au plus 4 × 9 cases lues, contre tout le plateau pour `evaluate`.
- Dans `minMax`, le test est fait **avant** l'appel récursif (sinon on ne coupe rien). Si le coup gagne : on retire la pierre, puis `return WIN_SCORE + depth` si c'est moi, `return -(WIN_SCORE + depth)` si c'est l'adversaire.
- `depth` (profondeur restante) et pas `DEPTH` (constante) : plus il reste de profondeur, plus la fin est proche. L'IA préfère gagner vite et perdre le plus tard possible.
- Dans `play`, un coup gagnant est joué tout de suite (rien ne peut faire mieux qu'un 5). La pierre est retirée avant de renvoyer le coup.
- On ne coupe que sur un 5 (certain), jamais sur une estimation : la coupure ne change pas le coup choisi, elle évite seulement du travail inutile.

### Mesures
Test 1 (tu joues 8 9, 8 10, 8 11, 8 12) : identique à la version précédente, car aucun 5 n'est possible avant la fin. L'IA gagne en 4 coups (et préfère finir son 4 plutôt que bloquer le tien).
- Profondeur 1 : 28, 40, 52 appels. Profondeur 2 : 1 050, 1 598, 2 364. Profondeur 3 : 48 380, 83 531, 131 851.

Test 2, profondeur 2 (tu joues 6 6, 6 8, 5 6, 4 6, 3 6, 7 5, 5 7, 1 4) : l'IA bloque ton 4 en 2 6, puis gagne en 4 9.
- Appels : 1 830, 2 006, 2 276, 2 741, **431**, 3 357, 3 587.
- Sans la coupure : mêmes coups, mais **3 021** appels pour le coup du blocage. Tout coup qui ne bloque pas est éliminé dès qu'une réponse adverse fait 5, sans évaluer le reste de son sous-arbre.
- Conclusion : la coupure change peu en début de partie, mais beaucoup dès qu'il y a des menaces (ici 7 fois moins d'appels).

## 1 quinquies. Élagage alpha-bêta (branche `alpha-beta`, 2026-11-12)

### Principe
- Pendant la recherche, on garde deux bornes, passées en paramètres à chaque appel récursif :
  - **alpha** : le meilleur score que MAX (l'IA) est déjà sûre d'obtenir sur le chemin actuel (un plancher, il ne fait que monter) ;
  - **bêta** : le meilleur score que MIN (l'adversaire) est déjà sûr d'obtenir, donc le pire pour l'IA (un plafond, il ne fait que descendre).
- Au départ : alpha = −∞, bêta = +∞ (en pratique les scores de défaite et de victoire).
- Nœud MAX : après chaque fils, alpha = max(alpha, valeur du fils). Nœud MIN : bêta = min(bêta, valeur du fils).
- Dès que **alpha ≥ bêta**, on arrête la boucle des coups de ce nœud : un des deux joueurs a déjà mieux ailleurs, il ne laissera jamais la partie arriver ici.
- On coupe des branches que **l'un ou l'autre** joueur ne choisira jamais (pas seulement l'adversaire), et on ne les évalue pas du tout.
- Exemple : un nœud MIN a déjà vu un fils à 2, il vaudra donc **au plus** 2 (on ne connaît pas sa valeur exacte, seulement une borne). Si l'IA a déjà 3 ailleurs, ce nœud ne sera jamais choisi : on saute ses autres fils.
- **Exact** : on ne coupe que ce qui ne peut pas changer le choix du parent. À même profondeur, l'IA doit jouer les mêmes coups que sans élagage, avec moins d'appels à `evaluate`. (Différent de l'idée « arrêter quand le score baisse trop », qui est une coupure heuristique pouvant se tromper.)
- Plus les bons coups sont essayés en premier, plus on coupe tôt : d'où l'ordre des coups, version suivante.

### Exercice à la main (profondeur 3)
Racine = MAX (l'IA), niveau 1 = MIN, niveau 2 = MAX, puis les feuilles (valeurs de `evaluate`), lues de gauche à droite.

```
                         [MAX racine]
            /                  |                  \
         [MIN A]            [MIN B]             [MIN C]
         /     \            /     \             /     \
     [MAX A1] [MAX A2]  [MAX B1] [MAX B2]  [MAX C1] [MAX C2]
      /  \     /  \      /  \     /  \      /  \     /  \
     3    5   6    9    1    2   8    4    7    4   2    6
```

Questions :
1. Quelle valeur remonte à la racine, et quel coup (A, B ou C) l'IA joue-t-elle ?
2. Quelles feuilles ne sont jamais évaluées ? Pour chaque coupure, donner les valeurs de alpha et bêta au moment du test.
3. Combien d'appels à `evaluate` sur 12 feuilles ?

Corrigé : racine = 6, l'IA joue C. Coupures : la feuille 9 de A2 (A2 reçoit (−∞, 5), voit 6 : 6 ≥ 5) et tout B2 (B1 reçoit (5, +∞) et vaut 4, donc β de B = 4 ≤ α = 5). 9 appels sur 12. Dans un autre ordre (C2, C1, A, B), seulement 7 : d'où l'intérêt de l'ordre des coups.

### Écriture dans `minMax` (commit 9736deb)
- Signature `minMax(depth, isMyTurn, alpha, beta)`. `play` lance avec `Integer.MIN_VALUE` et `Integer.MAX_VALUE` (pas `-Integer.MIN_VALUE` : déborde et redonne `MIN_VALUE`).
- Ordre dans la boucle, après l'appel récursif : retirer la pierre, mettre à jour alpha (mon tour) ou bêta (tour adverse), mettre à jour bestScore, puis `if (alpha >= beta) break;`.
- Pièges rencontrés : mettre à jour alpha au lieu de bêta au tour adverse ; sortir avant de retirer la pierre (plateau corrompu) ; sortir avant de mettre à jour bestScore (un nœud coupé renverrait ±infini et le parent le choisirait à tort) ; `continue` au lieu de `break` (ne sort pas de la boucle).
- On renvoie bestScore, jamais alpha ou bêta. Un nœud coupé renvoie une borne (« au moins » / « au plus »), suffisante pour que le parent l'écarte.
- Les `int` sont passés par copie : un fils qui change son alpha ne change pas celui du parent, seule sa valeur remonte.

### Mesures (test 1, mêmes coups que sans élagage à chaque profondeur)
- Profondeur 1 : 28 / 40 / 52, inchangé (pas de boucle dans `minMax`, rien à couper).
- Profondeur 2 : 1 050 / 1 598 / 2 364, inchangé. Raison : `play` passe toujours alpha = `MIN_VALUE`, donc les nœuds MIN de profondeur 1 ne peuvent jamais couper.
- Profondeur 3 : **17 468 / 30 517 / 33 579** (avant 48 380 / 83 531 / 131 851), soit 2,7 à 3,9 fois moins.
- Profondeur 4 (même ouverture, l'IA défend : 7 9, 7 8, 9 7, 8 13) : 353 984 / 1 060 563 / 1 459 757 / 2 409 916, contre 2 613 694 / 4 983 757 / 6 688 377 / 3 934 244 sans élagage.

### Version suivante : alpha à la racine
- Dans `play`, passer `bestScore` comme alpha au lieu de `MIN_VALUE` : la racine est un nœud MAX, son alpha est la valeur du meilleur coup déjà trouvé. Toujours exact (test `score > bestScore` strict : un fils coupé renvoie une borne ≤ bestScore, il n'est jamais choisi).
- Prévu au harnais puis confirmé par mec : profondeur 2 → 300 / 381 / 352, profondeur 3 → 7 314 / 14 625 / 8 652, profondeur 4 → 132 819 / 307 204 / 207 558 / 1 591 781, mêmes coups (commit 0b0bdd0).

### Expérience non notée : Min-Max sélectif (branche `selective-depth10`, écrite par Claude)
- `SelectiveMinMaxPlayer` : tri des candidats par une heuristique locale (attaque + défense), on ne garde que les 3 meilleurs à chaque niveau, profondeur 10. Garde-fou : un coup qui bloque un 5 adverse est toujours gardé. Pas exact.
- Deux parties contre `MinMaxSimplePlayer` (profondeur 3) : la sélective gagne les deux, en premier (double 4 au 45e coup) et en second (4 ouvert au 60e coup). 17 ms par coup contre 140 à 520 ms, ~30 000 appels contre 2 à 6,6 millions.
- Leçons : effet d'horizon (à profondeur 3, un double 4 préparé n'est vu qu'une fois inévitable) ; quand tous les coups perdent, ils ont le même score et `play` garde le premier parcouru (coup absurde en coin). Pistes : evaluate qui reconnaît les combinaisons gagnantes (4 ouvert, double 4, 4 + 3 ouvert, double 3 ouvert), prolonger la recherche sur les coups forcés.

## 1 sexies. Ordre des coups (branche `move-ordering`, 2026-11-13)

### Principe
- L'alpha-bêta coupe d'autant plus que le meilleur coup est essayé en premier : alpha et bêta sont bons dès le premier fils, et les frères suivants sont coupés tout de suite. Dans le meilleur cas, on passe d'environ b^d feuilles à environ b^(d/2).
- L'ordre ne change que la vitesse : on essaie exactement les mêmes coups candidats, la valeur trouvée est la même. Seule exception : si deux coups ont exactement le même score, `play` garde le premier rencontré, qui peut changer avec l'ordre (coup différent, même valeur).
- On ne trie pas avec `evaluate` (elle lit tout le plateau : trop cher pour chaque candidat de chaque nœud), mais avec une note **locale** de la case.

### Note d'un coup (`moveScore`, `lineThrough`)
- `lineThrough(row, column, color, d)` : la suite de `color` qui passerait par la case si `color` y jouait. On part de la case (comptée pour 1, sans y poser la pierre) et on avance de chaque côté (d puis -d) tant que c'est `color` ; la case où l'on s'arrête donne l'extrémité : libre si vide, fermée si pierre adverse ou bord. Deux boucles séparées, car les deux côtés n'ont pas la même longueur. Gère les lignes avec un trou comblé par le coup (X X _ X → 4).
- `moveScore = attaque + défense` sur les 4 directions, chaque suite notée par `scoreSequence` :
  - attaque : la suite que je ferais en jouant là ;
  - défense : la suite que l'adversaire ferait s'il jouait là (en prenant la case, je la lui enlève). Sans la défense, la case qui bloque un 4 adverse serait notée presque 0 et essayée en dernier.
- `ownerMovesNext` : constante `ORDERING_OWNER_MOVES_NEXT = false` pour l'attaque et la défense. Après le coup de `color`, c'est l'adversaire qui joue ; et une seule valeur pour toutes les cases garde un tri cohérent, quel que soit le joueur du nœud. (Une version avec `playerColor == color` notait l'attaque et la défense avec des barèmes différents selon que le nœud était à l'IA ou à l'humain.)
- L'adversaire de `color` se calcule à part (`otherColor`) : `getEnemyColor()` donne l'adversaire de l'IA, pas celui de `color`.

### Liste triée (`orderedMoves`)
- Record `ScoredMove(Coords coords, int score)`, privé dans `MinMaxSimplePlayer` (seule classe qui s'en sert).
- Pourquoi pas une `Map` : avec le score comme clé, deux cases de même score s'écrasent ; avec la case comme clé, une `HashMap` n'a pas d'ordre. On n'a jamais besoin de chercher par clé, seulement de parcourir dans l'ordre : une liste de records triée suffit.
- `orderedMoves(color)` : même filtre que les anciennes boucles (case vide et `isNeighbour`), un `ScoredMove` par candidat, tri par score décroissant (`Comparator.comparingInt(ScoredMove::score).reversed()`).
- La liste est une **variable locale** de `minMax` et de `play`, pas un attribut : sinon l'appel récursif du niveau en dessous l'écraserait pendant que le niveau courant la parcourt.
- Coût : le parcours du plateau remplace celui des anciennes boucles ; le surcoût est la note des ~30 candidats (8 `lineThrough` chacun). Les feuilles (profondeur 0) renvoient `evaluate` sans chercher de coups, donc ne trient pas : le tri ne touche que les nœuds internes, environ 30 fois moins nombreux.

### Mesures (test 1 : IA au centre, Noir 8 9, 8 10, 8 11, 8 12)
- Profondeur 3 : **1 181 / 1 918 / 2 446** (avant 7 314 / 14 625 / 8 652), soit 3,5 à 7,6 fois moins ; ~10 ms par coup. IA : 7 9, 6 10, 5 11, 4 12 (gagne, Noir ne bloque pas).
- Profondeur 4 : **4 256 / 7 968 / 8 643 / 3 415** (avant 132 819 / 307 204 / 207 558 / 1 591 781), soit 24 à 466 fois moins ; 40 à 60 ms par coup. Meilleurs scores : 0 / 20 / 120 / 220.
- Coups à la profondeur 4 : 7 9, 7 8, **9 8**, 8 13, contre 7 9, 7 8, **9 7**, 8 13 avant. Le 3e coup diffère : cas d'égalité attendu (même score, autre coup rencontré en premier) — à confirmer en affichant le score de 9 7 et 9 8.
- Profondeur 5 (exacte) : **65 215 / 116 158 / 113 628 / 4 198** appels, 160 à 220 ms par coup. Coups : 7 9, 7 8, 9 8, 8 13 (comme à la profondeur 4). Meilleurs scores : 990 / 1 021 / 1 120 / 2 020.
- Conséquence : la profondeur 4 passe de plusieurs secondes à quelques dizaines de millisecondes, et la profondeur 5 exacte devient jouable (moins d'un quart de seconde par coup), alors qu'elle était hors de portée sans l'ordre des coups.

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
| + coups candidats (distance 2) | 1 | 28 (1er coup) | non compté | mesuré |
| + coups candidats (distance 2) | 2 | 1 050 (1er coup) | non compté | mesuré |
| + coups candidats (distance 2) | 3 | 48 380 (1er coup) | non compté | mesuré |
| + coupure sur victoire | 1 / 2 / 3 | 28 / 1 050 / 48 380 (1er coup) | non compté | identique en début de partie |
| + coupure sur victoire | 2 | 431 (coup du blocage, test 2) | non compté | 3 021 sans la coupure |
| + alpha-bêta | 1 / 2 | 28 / 1 050 (1er coup) | non compté | inchangé : la racine ne passe pas son alpha |
| + alpha-bêta | 3 | 17 468 (1er coup) | non compté | mesuré, mêmes coups (avant 48 380) |
| + alpha-bêta | 4 | 353 984 (1er coup) | non compté | 2 613 694 sans élagage |
| + alpha à la racine | 2 / 3 / 4 | 300 / 7 314 / 132 819 (1er coup) | non compté | mesuré, mêmes coups |
| + ordre des coups | 3 | 1 181 (1er coup) | non compté | mesuré (avant 7 314), ~10 ms/coup |
| + ordre des coups | 4 | 4 256 (1er coup) | non compté | mesuré (avant 132 819), 40-60 ms/coup ; 3e coup différent (égalité ?) |
| + ordre des coups | 5 | 65 215 (1er coup) | non compté | mesuré, 160-220 ms/coup |
