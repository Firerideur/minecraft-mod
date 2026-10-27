# Arcane Quest

Mod RPG pour **Minecraft 1.21.1** (Forge 52.1.20).

## Fonctionnalités

### Niveaux et statistiques
- Tuer des monstres rapporte de l'XP. Plus le monstre est fort (vie, niveau, champion, boss), plus il en donne.
- Niveau max : 100. Chaque niveau donne **3 points de stats**.
- Touche **K** : fiche de personnage pour répartir les points et choisir sa classe.
- En haut à gauche : niveau, barre d'XP et barre de mana.
- Tout est conservé à la mort.

| Stat | Effet par point |
|------|-----------------|
| Force | +0,5 dégât de mêlée |
| Vitalité | +1 PV (½ cœur) |
| Agilité | +0,5 % vitesse de déplacement, +1 % vitesse d'attaque |
| Intelligence | +5 mana max, +5 % puissance des sorts, régénération du mana plus rapide |

### Classes (choix définitif, touche K)
| Classe | Bonus |
|--------|-------|
| Guerrier | +2 cœurs, +2 armure, +10 % dégâts de mêlée, +10 % résistance au recul |
| Mage | +50 % mana max et régénération, +25 % puissance des sorts |
| Archer | +30 % dégâts des flèches, +5 % vitesse |
| Assassin | Coups critiques ×2 (au lieu de ×1,5), +10 % vitesse d'attaque, +5 % vitesse |

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
| **Roi Squelette** (300 PV) | Crâne maudit | 30 | Salves de flèches de Wither, invoque des squelettes, enragé à 50 % | Lame du Roi Squelette (Wither), mithril, arcanite, crâne de wither squelette |
| **Archimage corrompu** (260 PV) | Grimoire interdit | 45 | Crocs et vex, traits de feu, se téléporte, foudre quand enragé | Bâton de l'Archimage, arcanite, totem d'immortalité |
| **Golem d'adamantite** (400 PV) | Cœur de golem | 60 | Coups dévastateurs, onde de choc qui projette en l'air, enragé à 50 % | Marteau d'adamantite (onde de choc), adamantite |

### Commandes (op)
- `/aq xp add <montant> [joueur]`
- `/aq level set <niveau> [joueur]`
- `/aq class <warrior|mage|archer|assassin|none> [joueur]` (`none` permet de rechoisir)
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

Les textures sont du pixel art original au style « PvP » (outils 32×32 sombres à reflets colorés), dessiné par `tools/textures.py`. Les traductions sont dans `src/main/resources/assets/arcanequest/lang/` (fr_fr, en_us).

### Look « Rose Revamp »
Si tu as le pack de textures Rose Revamp, `tools/build_rose_pack.py` crée à partir de ta copie un pack de ressources personnel qui donne aux objets du mod le même look :

```bash
python3 tools/build_rose_pack.py "Rose Revamp 1.15.zip" "ArcaneQuest x Rose Revamp.zip"
```

Mets le zip obtenu dans `.minecraft/resourcepacks` et active-le. Les textures de Rose Revamp appartiennent à leur auteur : elles ne sont pas dans ce dépôt, et le pack généré est pour un usage personnel.

Pour jouer : copier `build/libs/arcanequest-<version>.jar` dans le dossier `mods` d'une installation Forge 1.21.1.
