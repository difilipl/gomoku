# Gomoku — IA Min-Max et optimisations
 
Projet de cours (IUT / TP « Qualité algorithmique ») : écrire une IA de Gomoku (15×15, 5 pierres alignées) en Java, puis l'accélérer étape par étape et **mesurer** chaque gain.
 
Chaque optimisation a sa propre branche et ses commits, pour que l'historique raconte la démarche.
 
## Ce que fait l'IA
 
`MinMaxSimplePlayer` choisit son coup avec un Min-Max récursif :
 
- **Évaluation** d'une position : on repère les suites de pierres de chaque joueur (longueur, nombre d'extrémités libres) et on les note avec un barème par paliers. Le score est toujours du point de vue de l'IA.
- **Min-Max** de profondeur réglable, avec une pierre posée puis retirée (pas de copie du plateau).
- **Coups candidats** : seules les cases à distance 2 d'une pierre sont essayées.
- **Coupure sur victoire** : on arrête dès qu'un coup fait 5, et l'IA préfère gagner vite et perdre tard.
- **Élagage alpha-bêta**, avec l'alpha de la racine transmis.
- **Ordre des coups** : les candidats sont triés par une note locale (attaque + défense) pour que l'alpha-bêta coupe tôt.
- **Reconnaissance des combinaisons** dans l'évaluation (4 + 3 ouvert, double 4, double 3 ouvert).
## Mesures
 
Nombre d'appels à la fonction d'évaluation pour le premier coup calculé (même ouverture) :
 
| Version | Profondeur 3 |
|---|---|
| Min-Max simple (toutes les cases vides) | 10 940 826 |
| + coups candidats | 48 380 |
| + alpha-bêta | 17 468 |
| + alpha transmis à la racine | 7 314 |
| + ordre des coups | 1 181 |
 
Avec l'ordre des coups, la profondeur 5 exacte devient jouable (environ 160 à 220 ms par coup).
 
Un essai de *killer moves* (branche `killer-moves`) s'est révélé **plus lent** que le tri local : je l'ai abandonné et noté pourquoi.
 
## Expérience : Min-Max sélectif
 
`SelectiveMinMaxPlayer` ne garde que les 3 meilleurs coups à chaque niveau et cherche en profondeur 10. Ce n'est pas exact, mais il est très rapide (environ 15 ms par coup). Contre l'IA exacte en profondeur 5, le bilan est de 1 victoire partout. Le détail est dans `NOTES.md`.
 
## Structure
 
```
gamecore/   plateau, règles, réseau (fournis par l'enseignant)
player/     MinMaxSimplePlayer, SelectiveMinMaxPlayer, joueurs de base
gui/        interface graphique Swing (bonus, hors notation)
Game.java   lancement de parties en console
NOTES.md    journal de la démarche, choix et mesures
```
 
## Lancer le projet
 
Java 16 ou plus (le code utilise des `record`). Depuis la racine du dépôt :
 
```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java).FullName
java -cp out fr.esisar.in450.gomoku.Game
```
 
Pour l'interface graphique :
 
```powershell
java -cp out fr.esisar.in450.gomoku.gui.GomokuApp
```
 
`Game.main` lance par défaut une partie IA contre IA ; les autres cas (contre un humain en console, en réseau) sont dans les méthodes de `Game.java`.
 
## Branches
 
`minmax-depth2`, `candidate-moves`, `win-cutoff`, `alpha-beta`, `move-ordering`, `killer-moves` (abandonnée), `selective-depth10`, `match-selective`, `combo-eval` et `gui`. `main` contient l'ensemble.
 
## Aide d'une IA
 
Ce projet a été réalisé avec l'aide de Claude (assistant IA). `NOTES.md` est le journal de cette démarche. `SelectiveMinMaxPlayer` a été écrit par Claude, pour l'expérience.
 
## Auteur
 
Lissandro Di Filippo — [GitHub](https://github.com/difilipl)
 
