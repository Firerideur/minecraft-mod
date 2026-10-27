# Arcane Quest

Mod RPG pour **Minecraft 1.21.1** (Forge 52.1.20).

## Fonctionnalités

### Niveaux et statistiques
- Tuer des monstres rapporte de l'XP. Plus le monstre est fort (vie, niveau, champion, boss), plus il en donne.
- Niveau max : 100. Chaque niveau donne **3 points de stats**.
- Touche **K** : fiche de personnage pour répartir les points et choisir sa classe.
- En haut à gauche : médaillon avec le niveau, barres de vie, de mana et d'XP (valeurs affichées, traînée quand on perd de la vie).
- Tout est conservé à la mort.

| Stat | Effet par point |
|------|-----------------|
| Force | +0,5 dégât de mêlée |
| Vitalité | +1 PV (½ cœur) |
| Agilité | +0,5 % vitesse de déplacement, +1 % vitesse d'attaque |
| Intelligence | +5 mana max, +5 % puissance des sorts, régénération du mana plus rapide |

### Classes (niveau 20, choix définitif, touche K)
La classe se débloque au **niveau 20**. Le jeu propose alors **3 classes** selon la façon dont vous avez réparti vos points de stats (avant le niveau 20, l'onglet Compétences montre déjà un aperçu). Chaque classe correspond à une ou deux stats :

| Classe | Stats | Bonus |
|--------|-------|-------|
| Berserker | Force | +15 % dégâts de mêlée, +10 % vitesse d'attaque ; sous la moitié de sa vie : +30 % dégâts |
| Paladin | Force + Vitalité | +2 cœurs, +2 armure, +10 % dégâts de mêlée ; chaque coup de mêlée soigne ½ cœur |
| Gardien | Vitalité | +4 cœurs, +4 armure, +2 robustesse, +30 % résistance au recul |
| Duelliste | Force + Agilité | +10 % dégâts de mêlée, +15 % vitesse d'attaque, critiques ×1,75 |
| Assassin | Agilité | Critiques ×2, +10 % vitesse d'attaque, +10 % vitesse |
| Rôdeur | Agilité + Vitalité | +35 % dégâts des flèches, +10 % vitesse, +1 cœur |
| Mage | Intelligence | +50 % mana max et régénération, +30 % puissance des sorts |
| Lame-sort | Force + Intelligence | +10 % dégâts de mêlée, +15 % puissance des sorts ; chaque coup de mêlée rend 2 mana |
| Clerc | Vitalité + Intelligence | +2 cœurs, +10 % puissance des sorts, soins ×1,5, régénère ½ cœur toutes les 4 s |
| Arcaniste | Agilité + Intelligence | Sorts 30 % moins chers, +10 % puissance des sorts, +10 % vitesse |

### Magie
Mana de base : 20 (+5 par Intelligence, +15 par pièce d'armure d'arcanite). Clic droit avec un bâton pour lancer son sort.

| Bâton | Sort | Mana | Recharge |
|-------|------|------|----------|
| Bâton de feu | Trait de feu (6 dégâts, enflamme) | 8 | 0,75 s |
| Bâton de givre | Éclat de givre (4 dégâts, ralentit) | 8 | 0,75 s |
| Bâton arcanique | Missile arcanique (9 dégâts, affaiblit) | 12 | 0,5 s |
| Bâton de foudre | Foudre à 32 blocs (10 dégâts de zone) | 25 | 3 s |
| Bâton de soin | Soin (4 PV) | 20 | 4 s |
| Bâton de l'Archimage (boss) | Barrage de 5 missiles arcaniques | 30 | 2 s |

Les dégâts et soins sont multipliés par la puissance des sorts.

### Minerais, armes et armures
| Matériau | Où | Outil requis | Utilisation |
|----------|----|--------------|-------------|
| **Mithril** | Y -32 à 56 | Fer | Outils et armure entre fer et diamant (peut miner le diamant et l'adamantite) |
| **Arcanite** (cristal) | Y -64 à 16 | Fer | Bâtons de sort, armure de mage (+mana) |
| **Adamantite** | Y -64 à -24 (deepslate) | Diamant / mithril | Outils et armure meilleurs que la netherite |

Chaque métal a : minerai, matériau brut, lingot, bloc, épée, pioche, hache, pelle, houe, casque, plastron, jambières, bottes.

### Monstres de niveau et champions
- Les monstres ont un niveau selon l'endroit où ils apparaissent : **distance au spawn** (+1 tous les 150 blocs), **profondeur** (+1 tous les 16 blocs sous Y=0), **Nether** (+20), **End** (+35).
- Par niveau : +8 % de vie, +5 % de dégâts, +0,1 armure.
- Regardez un monstre pour voir son nom, son niveau (couleur selon la difficulté, ☠ si beaucoup plus fort que vous) et sa vie.
- **Champions** (3 % des monstres, mini-boss) : ×3 vie, +50 % dégâts, plus grands et plus rapides, barre de boss, ×3 XP, lâchent du mithril, de l'arcanite et parfois de l'adamantite.

### Boss
Invoqués en utilisant leur objet sur un bloc.

| Boss | Invocation | Niveau | Capacités | Butin |
|------|-----------|--------|-----------|-------|
| **Roi Squelette** (300 PV) | Crâne maudit | 30 | Tourbillon d'épée, éruption d'os, salves de flèches de Wither, invoque des squelettes | Lame du Roi Squelette (Wither), mithril, arcanite, crâne de wither squelette |
| **Archimage corrompu** (260 PV) | Grimoire interdit | 45 | Météore, nova arcanique, traits de feu, crocs et vex, se téléporte, foudre quand enragé | Bâton de l'Archimage, arcanite, totem d'immortalité |
| **Golem d'adamantite** (400 PV) | Cœur de golem | 60 | Frappe au sol avec onde de choc, charge, coups dévastateurs | Marteau d'adamantite (onde de choc), adamantite |

Chaque boss a son propre modèle 3D, ses textures (avec parties lumineuses) et ses animations. Les attaques spéciales sont **annoncées** (on voit le boss se préparer) pour pouvoir les esquiver :
- **Roi Squelette** (couronne, cape, épaulières) : lève son épée puis tourne sur lui-même en avançant (reculer) ; plante son épée et des mâchoires d'os jaillissent le long d'une ligne de feu d'âme (s'écarter de la ligne). Enragé : yeux rouges, 3 lignes.
- **Archimage corrompu** (capuche, bâton, cristaux en orbite, flotte au-dessus du sol) : un cercle de feu marque le sol puis un météore s'écrase dessus (sortir du cercle, 3 météores enragé) ; les cristaux se resserrent puis une nova repousse tout autour (s'éloigner).
- **Golem d'adamantite** (voûté, cristaux dans le dos, cœur en fusion) : lève les poings puis frappe le sol, une onde de choc avance au sol (sauter par-dessus) ; gratte le sol puis charge en ligne droite (s'écarter), et s'il percute un mur il est **étourdi** et prend +50 % de dégâts.

### Armes forgées
5 types d'armes, chacun avec sa mécanique et son animation (1re et 3e personne) :

| Arme | Dégâts / vitesse | Spécial | Animation |
|------|------------------|---------|-----------|
| **Dague** | +1 / très rapide | ×1,75 dans le dos, portée -0,5 | Estoc rapide vers l'avant |
| **Katana** | +3 / rapide | Clic droit : ruée de 6 blocs qui tranche tout sur le passage (3 s) | Taille horizontale |
| **Épée longue** | +6 / lente | À deux mains, chaque coup fend les ennemis devant (60 %), portée +0,5 | Lever puis frappe verticale |
| **Lance** | +4 / lente | Portée +2 blocs | Estoc long |
| **Faux** | +5 / lente | Fauche tout autour (50 %) et soigne (10 % par cible) | Grand balayage |

8 matériaux : fer, acier, diamant, mithril, orichalque, adamantite, arcanium (chaque coup rend 2 mana), acier stellaire.

**Forge d'armes** : plan (non consommé) + lingots (1 à 4 selon l'arme) + poignée (dague, katana, épée longue) ou hampe (lance, faux).

### Marteaux de minage
Marteaux qui cassent une zone d'un coup (accroupi : un seul bloc), plus lents qu'une pioche :

| Matériau | Zone |
|----------|------|
| Fer, acier | 3×3 |
| Diamant, mithril, orichalque | 5×5 |
| Adamantite, arcanium | 7×7 |
| Acier stellaire | 9×9 |

Forge d'armes : plan de marteau + 5 lingots + hampe.

### Interface
- **K** : menu du personnage (inventaire, artisanat, compétences/stats, carte, paramètres).
- **Carte** : onglet du menu avec position, biome, dimension, jour et monstres proches. **M** : affiche/masque la mini-carte.
- Boussole en haut, plaque de la cible (niveau, nom, vie), barres de boss ornées (compactes pour les champions).

### Fusion de minerais
**Forge de fusion** : deux ingrédients (dans n'importe quel ordre) + un catalyseur de fusion (4 fusions par catalyseur).

| Alliage | Recette | Temps |
|---------|---------|-------|
| Acier | 1 lingot de fer + 2 charbons | 5 s |
| Orichalque | 1 mithril + 1 or | 10 s |
| Arcanium | 2 arcanite + 1 adamantite | 15 s |
| Acier stellaire | 2 mithril + 1 adamantite | 20 s |

Fabrication : forge de fusion (3 mithril, 2 arcanite, haut fourneau, 3 briques de pierre noire polie), forge d'armes (3 fer, 2 planches + enclume + 3 planches), catalyseur (arcanite + poudre de blaze + redstone → 2), poignée (bâton + cuir + ficelle → 2), hampe (3 bâtons + ficelle), plans (papier + fer/pépite + ingrédient selon l'arme).

### Commandes (op)
- `/aq xp add <montant> [joueur]`
- `/aq level set <niveau> [joueur]`
- `/aq class <classe|none> [joueur]` : donne n'importe quelle classe (`none` permet de rechoisir)
- `/aq reset [joueur]` : rembourse les points de stats
- `/aq info [joueur]`

## Développement

Prérequis : Java 21.

```bash
./gradlew build              # compile le mod -> build/libs/arcanequest-<version>.jar
./gradlew runClient          # lance Minecraft avec le mod
./gradlew runGameTestServer  # lance les tests automatiques
python3 tools/generate_resources.py  # régénère textures, modèles, recettes, loots, tags, worldgen
```

Les modèles et textures des boss sont décrits dans `tools/boss_models.py`, qui génère les textures et `BossGeometry.java`. Les textures sont du pixel art original au style « PvP » (outils 32×32 sombres à reflets colorés), dessiné par `tools/textures.py`. Les traductions sont dans `src/main/resources/assets/arcanequest/lang/` (fr_fr, en_us).

### Look « Rose Revamp »
Si tu as le pack de textures Rose Revamp, `tools/build_rose_pack.py` crée à partir de ta copie un pack de ressources personnel qui donne aux objets du mod le même look :

```bash
python3 tools/build_rose_pack.py "Rose Revamp 1.15.zip" "ArcaneQuest x Rose Revamp.zip"
```

Mets le zip obtenu dans `.minecraft/resourcepacks` et active-le. Les textures de Rose Revamp appartiennent à leur auteur : elles ne sont pas dans ce dépôt, et le pack généré est pour un usage personnel.

Pour jouer : copier `build/libs/arcanequest-<version>.jar` dans le dossier `mods` d'une installation Forge 1.21.1.
