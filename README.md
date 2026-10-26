# Arcane Quest

Mod RPG pour **Minecraft 1.21.1** (Forge 52.1.20).

## Fonctionnalités

### ✅ Niveaux et statistiques (v0.1)
- Tuer des monstres rapporte de l'XP (plus le monstre a de vie, plus il en donne).
- Niveau max : 100. Chaque niveau donne **3 points de stats**.
- Touche **K** : fiche de personnage pour répartir les points.
- Niveau et barre d'XP affichés en haut à gauche de l'écran.
- Les stats sont conservées à la mort.

| Stat | Effet par point |
|------|-----------------|
| Force | +0,5 dégât de mêlée |
| Vitalité | +1 PV (½ cœur) |
| Agilité | +0,5 % vitesse de déplacement, +1 % vitesse d'attaque |
| Intelligence | réservée à la magie (à venir) |

Commandes (op) : `/aq xp add <montant> [joueur]`, `/aq level set <niveau> [joueur]`, `/aq reset [joueur]`, `/aq info [joueur]`.

### 🗺️ À venir
- Classes (Guerrier, Mage, Archer…)
- Monstres de différents niveaux, mini-boss et boss
- Magie (mana, sorts)
- Nouveaux minerais, armes et armures

## Développement

Prérequis : Java 21.

```bash
./gradlew build              # compile le mod -> build/libs/arcanequest-<version>.jar
./gradlew runClient          # lance Minecraft avec le mod
./gradlew runGameTestServer  # lance les tests automatiques
```

Pour jouer : copier `build/libs/arcanequest-<version>.jar` dans le dossier `mods` d'une installation Forge 1.21.1.
