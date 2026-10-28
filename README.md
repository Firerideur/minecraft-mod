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

### Classes (style Solo Leveling, touche K → Compétences)
37 classes avec des **rangs** (C, B, A, S), en deux étapes, chacune inspirée d'un chasseur, d'un Monarque ou d'une technique de Solo Leveling (survoler une classe dans le menu affiche ses bonus et son inspiration) :

1. **Niveau 20 : première classe.** Le jeu propose les **3 classes** qui correspondent le mieux à la façon dont vous avez réparti vos points (aperçu visible avant le niveau 20).
2. **Niveau 50 : Éveil.** Votre classe évolue : choisissez parmi ses 2 éveils (rang A).
3. **Classe cachée (rang S) : Monarque des Ombres.** Proposée à l'éveil du Nécromancien ou de l'Assassin, seulement si vous avez vaincu le **Roi Squelette** (avant ça, elle apparaît comme « ??? [S] »).

| Première classe (rang) | Stats | Inspiration | Bonus | Éveils (rang A) |
|---|---|---|---|---|
| Berserker (C) | Force | Combattant, Soif de sang de Sung Jinwoo | +15 % mêlée, +10 % vit. d'attaque, +30 % sous ½ vie, frénésie à chaque élimination | Seigneur de guerre, Bourreau |
| Gardien (C) | Vitalité | Tank (Arise), Ténacité | +4 cœurs, +4 armure, +2 robustesse, +30 % anti-recul, Ténacité 20 % | Forteresse, Gardien runique |
| Assassin (C) | Agilité | Sung Jinwoo avant son changement de classe | Critiques ×2, +10 % vitesse et vit. d'attaque, +30 % dans le dos | Ninja, Assassin des ombres, (Monarque des Ombres) |
| Mage (C) | Intelligence | Choi Jong-In | +50 % mana, +30 % sorts, 10 % de critiques magiques, mana par élimination | Archimage, Élémentaliste |
| Paladin (B) | Force + Vitalité | La lumière des Souverains | +2 cœurs, +2 armure, +10 % mêlée, chaque coup soigne, +20 % contre les morts-vivants | Croisé, Chevalier sacré |
| Duelliste (B) | Force + Agilité | Cha Hae-In | +10 % mêlée, +15 % vit. d'attaque, critiques ×1,75, 5 % d'esquive | Maître d'armes, Lame-vent |
| Rôdeur (B) | Agilité + Vitalité | Ranger (Arise) | +35 % flèches, +10 % vitesse, +20 % contre une cible intacte | Tireur d'élite, Chasseur |
| Lame-sort (B) | Force + Intelligence | Les dagues du Roi-Démon Baran | +10 % mêlée, +15 % sorts, chaque coup rend du mana | Chevalier runique, Démoniste |
| Clerc (B) | Vitalité + Intelligence | Lee Joohee | +2 cœurs, soins ×1,5, régénération, aura de soin | Saint, Druide |
| Arcaniste (B) | Agilité + Intelligence | Magie spatiale des mages de rang S | Sorts -30 % de mana, +10 % sorts et vitesse, 8 % de critiques magiques | Chronomancien, Mage de bataille |
| Moine (B) | Force + Agilité + Vitalité | Combattants à mains nues | +5 dégâts à mains nues, vitesse, enchaînement (+6 % par coup, 5 max) | Grand maître, Moine de fer |
| Nécromancien (B) | Intelligence + un peu de tout | La classe offerte à Sung Jinwoo lors de son changement de classe | Les monstres tués peuvent se relever en **soldats de l'ombre** | Seigneur liche, (Monarque des Ombres) |

| Éveil (rang) | Inspiration | Passifs marquants |
|---|---|---|
| Seigneur de guerre (A) | Go Gunhee | +25 % mêlée, +50 % sous ½ vie, frénésie 5 s, Ténacité 15 % |
| Bourreau (A) | Hwang Dongsoo | +60 % contre les ennemis sous 30 % de vie, chaque élimination soigne |
| Forteresse (A) | Thomas Andre « le Goliath » | +6 cœurs, +6 armure, -15 % dégâts subis, Ténacité 30 % |
| Gardien runique (A) | Barrières de mana des tanks | Renvoie 30 % des dégâts de mêlée, Ténacité 20 % |
| Ninja (A) | Assassins de rang S | 25 % d'esquive, +20 % vitesse, +30 % dans le dos |
| Assassin des ombres (A) | Kang Taeshik, Furtivité | Critiques ×2,5, +60 % dans le dos, invisible accroupi |
| Archimage (A) | Choi Jong-In | Mana ×2, +60 % sorts, 20 % de critiques magiques |
| Élémentaliste (A) | Monarque du Givre, Baran | Chaque coup enflamme et ralentit |
| Croisé (A) | Armées des Souverains | Chaque coup soigne 1 cœur, aura de soin |
| Chevalier sacré (A) | Les Souverains | +60 % contre les morts-vivants |
| Maître d'armes (A) | Liu Zhigang | +25 % vit. d'attaque, critiques ×2 |
| Lame-vent (A) | Cha Hae-In | +20 % vitesse, 12 % d'esquive |
| Tireur d'élite (A) | Archers d'élite | +70 % flèches, +50 % contre une cible intacte |
| Chasseur (A) | Rakan, Monarque des Crocs | +45 % flèches, chaque coup ralentit |
| Chevalier runique (A) | Baran, le Roi-Démon | +30 % sorts, 4 mana par coup |
| Démoniste (A) | Démons du Château des démons | Les sorts soignent de 25 % des dégâts |
| Saint (A) | Min Byung-Gu | Soins ×2,5, aura de soin puissante |
| Druide (A) | Tusk, l'ombre chamane | Régénération, chaque coup empoisonne |
| Chronomancien (A) | L'Architecte | Sorts -50 % de mana, 10 % de critiques magiques |
| Mage de bataille (A) | Esil Radiru | Sorts et mêlée, 2 mana par coup |
| Grand maître (A) | Baek Yoonho | +10 dégâts à mains nues, enchaînement +10 % par coup |
| Moine de fer (A) | Iron, l'ombre au bouclier | +6 armure, Ténacité 20 % |
| Seigneur liche (A) | Yogumunt, Monarque de la Transfiguration | 30 % des monstres tués deviennent des ombres (5 max, 2 min) |
| **Monarque des Ombres (S)** | **Sung Jinwoo** | 50 % des monstres tués deviennent des ombres permanentes (8 max), Ténacité 30 %, régénération |

Passifs : **Ténacité** (dégâts subis réduits sous 30 % de vie), **attaque dans le dos**, **critiques magiques** (×1,5), **premier coup** (bonus contre une cible à pleine vie), **enchaînement** (Moine), **frénésie** (Force et Vitesse après une élimination), soin ou mana par élimination, **aura** qui soigne les joueurs proches, esquive, renvoi de dégâts, exécution, bonus contre les morts-vivants, invisibilité accroupi, brûlure/ralentissement/poison à chaque coup, vol de vie des sorts.

**Soldats de l'ombre** : les monstres tués par vous ou par vos ombres peuvent se relever pour vous servir. Ils vous suivent, attaquent ce que vous combattez (ou ce qui vous attaque) et l'XP de leurs victimes vous revient. Nécromancien : 15 %, 3 max, 1 min ; Seigneur liche : 30 %, 5 max, 2 min ; **Monarque des Ombres** : 50 %, 8 max, permanents et bien plus forts.
- **Extraction d'ombre (« Lève-toi ! »)** : les cadavres des monstres tués dans la dernière minute (12 blocs) se relèvent. Un cadavre peut résister : 3 essais au plus, les monstres forts et les boss résistent plus (le Monarque réussit toujours sur les monstres faibles).
- **Rangs des ombres** selon le monstre d'origine : Soldat, Élite, Chevalier, Chevalier d'élite, Commandant (boss), Maréchal (boss très puissant). Plus le rang est haut, plus l'ombre est grande et forte (jusqu'à ×2,5).

### Aptitudes de classe (touches R, C, V, B, N, G)
Chaque classe a ses propres aptitudes actives, débloquées en montant de niveau, sur 6 emplacements : niveaux **20, 30, 40, 50, 75 et 100**.
- **Première classe** : 5 aptitudes (20, 30, 40, 50, 75).
- **Classe éveillée** : garde les 3 premières aptitudes de sa première classe, remplace celles des niveaux 50 et 75 par les siennes, et gagne une **aptitude ultime** propre au niveau 100 (★). Le Monarque des Ombres a ses 6 aptitudes propres.
- Barre d'aptitudes en bas à droite de l'écran : icône, touche, recharge, niveau requis (cadenas).
- Onglet **Aptitudes** du menu K : description, coût en mana et recharge de chaque aptitude.
- Les aptitudes deviennent plus fortes avec le niveau, et avec la puissance des sorts (aptitudes magiques) ou la Force (les autres). Les aptitudes physiques coûtent au plus 20 mana (le mana de base).

| Classe | 20 | 30 | 40 | 50 | 75 | 100 (ultime) |
|---|---|---|---|---|---|---|
| Berserker | Tourbillon | Cri de guerre | Frénésie sanglante | Soif de sang (peur) | Fracas | — |
| Seigneur de guerre | ↑ | ↑ | ↑ | Soif de sang | Furie | Cataclysme |
| Bourreau | ↑ | ↑ | ↑ | Exécution | Moisson | Sentence de mort |
| Gardien | Provocation | Coup de bouclier | Rempart | Fracas | Peau de fer | — |
| Forteresse | ↑ | ↑ | ↑ | Bastion | Séisme | Goliath (géant 15 s) |
| Gardien runique | ↑ | ↑ | ↑ | Bouclier de mana | Glyphe de garde | Égide runique |
| Assassin | Mutilation | Pas de l'ombre | Lame empoisonnée | Furtivité | Ruée des dagues | — |
| Ninja | ↑ | ↑ | ↑ | Bombe fumigène | Tempête de shurikens | Mille entailles |
| Assassin des ombres | ↑ | ↑ | ↑ | Ruée des dagues | Danse des lames | Assassinat |
| Mage | Boule de feu | Nova de givre | Chaîne d'éclairs | Lance de flammes | Météore | — |
| Archimage | ↑ | ↑ | ↑ | Lance de flammes | Barrage arcanique | Pluie de flammes |
| Élémentaliste | ↑ | ↑ | ↑ | Météore | Pics de glace | Flammes blanches |
| Paladin | Châtiment | Aura de soin | Ruée | Jugement | Bouclier divin | — |
| Croisé | ↑ | ↑ | ↑ | Étendard sacré | Tourbillon | Croisade |
| Chevalier sacré | ↑ | ↑ | ↑ | Bouclier divin | Sanctuaire | Lumière des Souverains |
| Duelliste | Ruée | Parade | Rafale de coups | Danse de l'épée | Danse des lames | — |
| Maître d'armes | ↑ | ↑ | ↑ | Danse des lames | Onde tranchante | Fendre-ciel |
| Lame-vent | ↑ | ↑ | ↑ | Danse de l'épée | Tornade | Danse de la tempête |
| Rôdeur | Tir multiple | Piège | Tir perçant | Tir de repli | Pluie de flèches | — |
| Tireur d'élite | ↑ | ↑ | ↑ | Œil de lynx | Flèche explosive | Flèche céleste |
| Chasseur | ↑ | ↑ | ↑ | Meute | Marque du chasseur | Hurlement de Rakan |
| Lame-sort | Lame runique | Ruée | Boule de feu | Taille arcanique | Bouclier de mana | — |
| Chevalier runique | ↑ | ↑ | ↑ | Taille arcanique | Chaîne d'éclairs | Lame du Roi-Démon |
| Démoniste | ↑ | ↑ | ↑ | Drain de vie | Malédiction | Pacte démoniaque |
| Clerc | Aura de soin | Châtiment | Bouclier de mana | Purification | Sanctuaire | — |
| Saint | ↑ | ↑ | ↑ | Purification | Sanctuaire | Grâce salvatrice |
| Druide | ↑ | ↑ | ↑ | Meute | Piège | Bosquet sacré |
| Arcaniste | Clignement | Barrage arcanique | Bouclier de mana | Puits de gravité | Chaîne d'éclairs | — |
| Chronomancien | ↑ | ↑ | ↑ | Arrêt du temps | Rembobinage | Effondrement temporel |
| Mage de bataille | ↑ | ↑ | ↑ | Lame runique | Météore | Avatar arcanique |
| Moine | Rafale de coups | Ruée | Peau de fer | Paume du dragon | Tornade | — |
| Grand maître | ↑ | ↑ | ↑ | Paume du dragon | Séisme | Forme bestiale |
| Moine de fer | ↑ | ↑ | ↑ | Provocation | Rempart | Corps adamantin |
| Nécromancien | Invocation des ombres | Drain de vie | Malédiction | Explosion d'ombre | Moisson d'âmes | — |
| Seigneur liche | ↑ | ↑ | ↑ | Nova de givre | Lève-toi ! | Peste de Yogumunt |
| **Monarque des Ombres** | Ruée des dagues | Échange d'ombre | Lève-toi ! (extraction) | Autorité du Souverain | Peur du dragon | Domaine du Monarque |

(↑ = aptitude gardée de la première classe.) Quelques mécaniques : **peur** (Soif de sang, Peur du dragon : les monstres fuient et ne peuvent plus cibler personne), **Furtivité** (premier coup ×2), **Autorité du Souverain** (télékinésie : soulève puis écrase, accroupi pour repousser), **Échange d'ombre** (apparaître à la place d'une ombre visée), **Domaine du Monarque** (+50 % de dégâts des ombres), transformations temporaires (**Goliath**, **Forme bestiale**), **Marque du chasseur** (+30 % de dégâts), **Grâce salvatrice** (survivre à un coup mortel).

### Portes et donjons (style Solo Leveling)
Des **Portes** s'ouvrent près des joueurs dans l'Overworld (toutes les 5 minutes, 40 % de chance ; un message indique la distance et la direction). Elles sont classées de **1 à 7 étoiles** : plus il y a d'étoiles, plus la porte est rare et dangereuse. Une porte reste ouverte un jour Minecraft.

| Étoiles | Rareté | Donjons possibles (au hasard) | Maîtres de la porte |
|---|---|---|---|
| ★ | 35 % | Grotte des gobelins | Gobelin |
| ★★ | 25 % | Tanière des crocs-d'acier, Forêt maudite, Repaire des hommes-bêtes | Loup croc-d'acier, Tréant, Loup-garou |
| ★★★ | 17 % | Forteresse de glace, Nid des araignées, Crypte des goules | Elfe des glaces, Araignée géante, Goule |
| ★★★★ | 11 % | Fourmilière géante, Temple englouti, Jungle des insectes géants | Fourmi soldat, Naga, Mante géante |
| ★★★★★ | 7 % | Camp des orcs, Labyrinthe du Minotaure | Orc supérieur, Minotaure |
| ★★★★★★ | 3,5 % | Citadelle noire, Cathédrale des gargouilles | Chevalier démon, Liche |
| ★★★★★★★ | 1,5 % | Abysse démoniaque, Trône infernal | Chevalier démon, Démon majeur |

Niveau des monstres : 4-9 (★), 16-21 (★★), 28-33 (★★★), 40-45 (★★★★), 52-57 (★★★★★), 64-69 (★★★★★★), 76-81 (★★★★★★★).

- **Entrer** : marcher dans la porte. Le donjon (salles + couloirs, plus grand avec les étoiles) est dans une dimension à part.
- **Monstres inédits** (26, modèles et textures propres) :
  - Gobelin (rapide, en groupe), Chaman gobelin (sorts), Loup croc-d'acier (bondit), Elfe des glaces (flèches qui ralentissent), Fourmi soldat (morsure empoisonnée), Orc supérieur (coups qui projettent), Chevalier démon (lame enflammée) ;
  - **Troll des cavernes** : se régénère sans arrêt… sauf s'il brûle (feu, aptitudes de feu) ;
  - **Minotaure** : baisse les cornes et charge de loin ;
  - **Gargouille** : une statue immobile (et presque invulnérable) qui s'anime quand on approche ;
  - **Liche** : traits d'âme et invocation de squelettes ;
  - **Naga** : femme-serpent qui lance des fioles de poison ;
  - **Mimique** : 30 % des coffres des salles sont des mimiques… elle mord, mais lâche du butin ;
  - **Tréant** : arbre vivant très résistant, ses racines immobilisent ;
  - **Basilic** : morsure empoisonnée, et son regard pétrifie si vous le regardez — détournez les yeux !
  - **Araignée géante** : grimpe aux murs, tisse des toiles qui immobilisent, morsure empoisonnée ;
  - **Mante géante** : frappe deux fois avec ses faux ;
  - **Scarabée blindé** : sa carapace bloque la plupart des coups de face (attaquez-le de dos), sa corne vous projette en l'air ;
  - **Guêpe géante** : vole, son dard empoisonne ;
  - **Loup-garou** : bondit, et entre en frénésie (plus rapide et plus fort) quand il est blessé ;
  - **Homme-rat** : en meute, dague empoisonnée ;
  - **Homme-lézard** : lance et bouclier (bloque souvent les coups de face) ;
  - **Goule** : mort-vivant rapide, sa morsure affame et affaiblit, et la soigne ;
  - **Diablotin** : petit démon qui lance des boules de feu ;
  - **Succube** : vous charme et vous attire à elle, ses coups volent votre vie ;
  - **Démon majeur** : aura brûlante, salves de boules de feu, lame enflammée.
- Les monstres des portes donnent **+50 % d'XP**, le maître de la porte ×3.
- **Nettoyer la porte** : vaincre le maître dans la dernière salle → bonus d'**XP** pour tous les joueurs du donjon (400 × étoiles², soit 400 pour ★ et 19 600 pour ★★★★★★★), un **coffre au trésor** (diamants, mithril, arcanite, adamantite, une arme forgée du rang de la porte, et plus rare à haut rang) et une sortie. Des coffres plus petits se trouvent aussi dans certaines salles.
- **Sortir** : la sortie est dans la première salle (et dans la salle du boss une fois nettoyée).
- `/aq gate [étoiles]` ouvre une porte devant vous (pour tester).

### Grottes (style Better Caves)
Le sous-sol de l'Overworld est beaucoup plus spectaculaire (nouveaux mondes ou nouvelles zones explorées seulement) :
- **Cavernes géantes** : d'immenses salles au sol plat (20 à 35 blocs de large) reliées par des tunnels très larges.
- **Plus de tunnels**, plus larges et sinueux, et des **ravins profonds** plus larges.
- **Lacs souterrains** creusés dans le sol des grottes (eau, parfois lave tout en bas), qui ne débordent jamais.
- **Régions de grottes** (environ 100 blocs de large, une couche au-dessus de Y 0 et une en dessous) :
  - **Grottes moussues** : mousse, fougères, azalées, lianes à baies lumineuses, racines pendantes, fleurs de spores.
  - **Grottes de cristal** : calcite et améthyste, cristaux au sol, aux murs et au plafond.
  - **Grottes gelées** : neige, glace compacte et glace bleue, stalagmites et stalactites de glace.
  - **Grottes à champignons** : mycélium, champignons, champignons géants, champilumes au plafond, lichen lumineux.
  - **Grottes de magma** (sous Y 0) : basalte, pierre noire, magma, petites mares de lave, colonnes de basalte.
  - **Catacombes** : murs de briques (pierre ou ardoise des abîmes) fissurées et moussues, gravier, os, crânes, toiles d'araignée et lanternes des âmes.
  - Les grottes ordinaires, les grottes luxuriantes, les grottes de spéléothèmes et les abîmes restent comme dans le jeu de base.
- `/aq cave` : la région où vous êtes et la plus proche de chaque type ; `/aq cave tp <type>` vous y téléporte (op).

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
- **Carte** : onglet du menu avec position, biome, dimension et jour. **M** : affiche/masque la mini-carte.
  - Les **monstres** apparaissent en petites figurines (les boss plus grands ; statues et mimiques endormies restent cachées), les **objets au sol** en points rouges, les autres joueurs en points blancs.
  - **Vue des grottes** : sous terre, la carte montre les galeries autour de votre hauteur (sol des grottes, roche en noir, niveaux plus bas assombris, eau et lave visibles) et la mini-carte affiche « Grottes · Y ».
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
- `/aq gate [1-7]` : ouvre une porte devant soi
- `/aq cave` : région de grottes actuelle et la plus proche de chaque type ; `/aq cave tp <mossy|crystal|frozen|fungal|magma|catacombs>` : téléporte dans la plus proche

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
