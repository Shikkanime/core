# Guide des Tests de Performance — Shikkanime

Ce document présente, de manière claire et accessible (non-technique), le fonctionnement
des tests de performance de Shikkanime, la manière d'interpréter leurs résultats et les étapes
pour les étendre facilement à de nouvelles plateformes de streaming.

---

## 1. Pourquoi des tests de performance ?

Shikkanime surveille et analyse en continu les catalogues de plusieurs plateformes d'animation.
Chaque jour, des centaines voire des milliers d'épisodes, de films et d'extraits sont publiés.

Nos tests de performance ont trois missions fondamentales :
1. **Garantir la fluidité** : S'assurer que le traitement d'un volume massif de vidéos reste ultra-rapide
   (plusieurs centaines de milliers d'épisodes traités par seconde).
2. **Détecter les régressions** : Si une modification de code ralentit le système ou consomme trop de
   mémoire, le test le signale immédiatement avant que le code ne parte en production.
3. **Identifier les futurs goulots d'étranglement** : Savoir précisément si la limite future viendra de
   la puissance de calcul du processeur (CPU) ou de la consommation de mémoire (RAM).

---

## 2. Le principe clé : Tester le VRAI code avec UN SEUL test représentatif

Contrairement à des tests qui simuleraient des boucles simplifiées ou multiplieraient les micro-tests
redondants, notre approche privilégie la **simplicité et la maintenabilité** :
- **Un seul test par plateforme** : Chaque plateforme possède un unique test de performance focalisé sur
  un **catalogue réaliste de 1 000 000 d'épisodes**.
- **Exécution du vrai code** : Le test appelle la méthode réelle de transformation (`convertVideos`), sans
  duplication de logique ni mocks superflus.
- **Tout est validé en un seul passage** :
  - Le nettoyage des titres et des suffixes de saisons (expressions régulières).
  - Le filtrage automatique des contenus exclus (bandes-annonces, bonus, séries non-animation).
  - La détection des langues audio et des types de médias (films, épisodes, OAV).
  - La réutilisation en mémoire des fiches d'animés pour éviter la surconsommation de RAM.

Cette approche évite d'ajouter des briques de maintenance complexes tout en garantissant une couverture
optimale des performances réelles.

---

## 3. Le scénario testé : Le catalogue réaliste (1 000 000 d'épisodes)

Le test simule un volume massif représentatif du monde réel :
- **1 000 séries distinctes** avec descriptions longues et titres variés.
- **Un million d'épisodes** comprenant :
  - Des épisodes réguliers numérotés.
  - Des contenus promotionnels et bonus à filtrer (ex. bandes-annonces, openings).
  - Des séries en prise de vue réelle (*live-action*) que l'algorithme doit automatiquement ignorer.
  - Des variations de formats d'images (bannières horizontales, portraits).

---

## 4. Comment lire les résultats et repérer les goulots d'étranglement

À chaque exécution, un tableau diagnostic clair est affiché dans les logs :

```text
--------------------------------------------------------------------------------
 BENCHMARK: benchmark-adn-realistic-catalog
--------------------------------------------------------------------------------
  Duration      : 840.5 ms (0.84 s)
  Throughput    : 1189768 items/sec
  Thread CPU    : 745.2 ms (User: 745.2 ms, Util: 88.7%)
  Allocated Mem : 2298.8 MB (2410 B/item)
  GC Activity   : 5 collections, 89 ms pause
  Bottleneck    : BALANCED (Optimal compute and allocation distribution)
--------------------------------------------------------------------------------
```

### Les indicateurs clés :
- **Throughput (Débit)** : Le nombre d'épisodes traités par seconde (ex. `1 189 768 items/sec`).
- **Thread CPU & Utilisation (%)** : L'effort du processeur. Une utilisation vers 80-90 % indique
  un calcul fluide et actif.
- **Allocated Mem (Mémoire allouée)** : Le volume total de mémoire temporaire créée (en Mo) et la quantité par
  épisode (ex. `2410 B/item`, soit ~2,4 Ko par épisode).
- **GC Activity (Ramasse-miettes)** : Le nombre de nettoyages mémoire effectués par Java et le temps d'arrêt
  cumulé en millisecondes.

### Les diagnostics automatiques de goulots d'étranglement (`Bottleneck`) :
- **`BALANCED`** : Fonctionnement optimal. L'équilibre entre calcul processeur et utilisation mémoire est idéal.
- **`CPU-BOUND`** : Le processeur tourne à plein régime sur les calculs (manipulation de texte, regex).
- **`HIGH MEMORY CHURN`** : L'application crée trop d'objets temporaires par épisode, alertant sur un risque de
  saturation de la mémoire vive.
- **`GC PRESSURE`** : Le ramasse-miettes passe plus de 15 % du temps total en pause pour libérer de la mémoire.

---

## 5. Comment lancer les tests ?

Pour exécuter la suite de tests de performance, lancez la commande suivante depuis la racine du projet :

```bash
./gradlew :jobs:perfTest
```

> **Note** : Ces tests manipulent 1 million d'objets. Ils tournent sous une JVM allouée à 4 Go et sont
> isolés dans la tâche dédiée `:jobs:perfTest` pour ne jamais ralentir vos tests unitaires quotidiens (`./gradlew test`).

---

## 6. Comment ajouter une nouvelle plateforme ?

Lors de l'ajout d'une nouvelle plateforme (Crunchyroll, Netflix, etc.), la procédure est simple et rapide :

1. **Créer une classe de test unique** dans `jobs/src/perfTest/kotlin/platforms/impl/`
   (ex. `CrunchyrollPlatformPerformanceTest`).
2. **Hériter de `BasePlatformPerformanceTest`**.
3. **Créer un jeu de données réaliste d'un million d'items** et appeler `measureAndAssert` sur la méthode
   `convertVideos` (ou équivalente) de la nouvelle plateforme.

Exemple minimal :
```kotlin
@Test
@DisplayName("should convert one million episodes of a realistic catalog within budget")
fun `should convert one million episodes of a realistic catalog within budget`() {
    val rawItems = generateRealisticDataset(1_000_000)
    val platform = CrunchyrollPlatform(createMockClient())

    measureAndAssert(
        label = "benchmark-crunchyroll-realistic-catalog",
        itemCount = 1_000_000,
        expectedOutputSize = 900_000
    ) {
        platform.convertVideos(rawItems, Country.FR)
    }
}
```

Grâce à cette approche minimaliste, chaque nouvelle plateforme dispose d'un garde-fou complet sans alourdir
la maintenance du projet.
