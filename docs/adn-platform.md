# Plateforme Animation Digital Network (ADN) — Documentation Fonctionnelle

Cette documentation détaille, de manière claire et non technique, le fonctionnement de l'intégration
entre la plateforme de streaming **Animation Digital Network (ADN)** et **Shikkanime**.

---

## 1. Présentation générale

Animation Digital Network (ADN) est l'un des principaux services légaux de diffusion d'animés
en France. Shikkanime surveille en temps réel le catalogue d'ADN pour détecter automatiquement
chaque nouvelle sortie d'épisode, de film ou d'OAV (épisode spécial), et en informer immédiatement
la communauté.

---

## 2. Comment Shikkanime détecte les sorties

Chaque jour, Shikkanime interroge le calendrier public d'ADN. Ce calendrier fournit la liste des
épisodes programmés ou mis en ligne pour la journée ainsi que les sorties à venir.

### Le cycle de détection
1. **Récupération du calendrier** : Shikkanime consulte la programmation officielle française d'ADN.
2. **Filtrage des contenus non pertinents** :
   - Les bandes-annonces, courts-métrages promotionnels, openings et making-of sont écartés.
   - Les types de vidéos promotionnels (`PV`) et les bonus (`BONUS`) sont automatiquement ignorés
     pour ne conserver que les véritables épisodes de séries ou longs-métrages.
3. **Vérification du genre** : Seuls les programmes identifiés comme de l'animation sont conservés
   (les séries ou films en prises de vues réelles ne sont pas importés).
4. **Extraction et normalisation** : Les informations brutes fournies par ADN sont nettoyées,
   complétées et harmonisées pour s'intégrer au catalogue unifié de Shikkanime sans recourir à
   des valeurs factices par défaut.

---

## 3. Typologie des contenus

Pour chaque vidéo détectée, Shikkanime identifie automatiquement la nature exacte de l'œuvre en combinant
le type natif ADN (`EPS`, `MOV`, `OAV`) et l'analyse du libellé de l'épisode :

* **Épisode standard (`EPISODE`)** : Les épisodes réguliers d'une série télévisée ou diffusée
  en streaming (ex. Épisode 12, Épisode 105).
* **Film d'animation (`FILM`)** : Les longs-métrages d'animation sortis en salle ou directement
  sur la plateforme (détectés via la mention "Film" ou le type cinéma `MOV` d'ADN).
* **Épisode spécial ou OAV (`SPECIAL`)** : Les épisodes hors-série, les OAV (type `OAV` d'ADN),
  ou les épisodes intermédiaires (comme les récapitulatifs numérotés avec des décimales, par exemple "12.5").

Si un film ou un épisode spécial ne dispose pas d'un numéro de séquence précis, Shikkanime le classe
proprement avec un repère neutre plutôt que d'inventer un faux numéro.

---

## 4. Gestion des langues (VOSTFR et VF)

Certaines vidéos sur ADN sont disponibles à la fois en version originale sous-titrée en français
(**VOSTFR**) et en version française doublée (**VF**).

* **Séparation par piste audio** : Quand un épisode est disponible dans les deux formats,
  Shikkanime crée automatiquement deux fiches distinctes, l'une avec la piste audio japonaise
  (`ja-JP`) et l'autre avec la piste audio française (`fr-FR`).
* **Distinction de la version originale** : La version originale est expressément marquée
  comme telle, permettant aux utilisateurs de filtrer leurs préférences de visionnage.

---

## 5. Nettoyage intelligent des titres et métadonnées

Les titres fournis par les plateformes de streaming contiennent souvent des suffixes encombrants
ou redondants. Shikkanime applique un nettoyage automatique :

* **Suppression des mentions de saison** : Les mentions telles que `"Saison 2"`, `"Part 2"` ou
  les chiffres romains en fin de titre (ex. `"Sword Art Online II"`) sont retirées du nom de l'animé
  pour rattacher tous les épisodes à la fiche mère de la franchise.
* **Priorité au titre court** : Si ADN fournit un titre abrégé officiel (par exemple `"One Piece"`
  au lieu de `"One Piece : Saga 15 - Egghead"`), Shikkanime privilégie ce nom principal.
* **Détection des versions non censurées** : Si le titre de l'épisode mentionne une version non
  censurée (via la mention `(NC)` ou `"Non censuré"`), l'épisode est automatiquement marqué
  comme non censuré, permettant d'avertir le public averti.

---

## 6. Visuels et médias associés

Pour chaque épisode et chaque série, Shikkanime extrait les visuels en très haute définition :

* **Affiche de la série (Portrait)** : L'illustration officielle au format vertical (1080x1543)
  avec logo pour l'affichage dans le catalogue.
* **Bannière de la série (Paysage)** : La bannière panoramique (1920x1080) avec logo utilisée pour les
  en-têtes et les fiches détaillées.
* **Miniature de l'épisode (16:9)** : L'image de prévisualisation haute résolution (1920x1080)
  fournie directement par ADN (`image2x`), sans altération ni substitution factice.

---

## 7. Résumé des informations conservées

Chaque épisode ADN répertorié par Shikkanime dispose ainsi d'un ensemble complet d'informations :
* L'animé rattaché (nom nettoyé, affiche portrait et bannière paysage)
* Le titre de l'épisode et son résumé officiel
* Le numéro de saison et le numéro de l'épisode
* Le type (Épisode, Film ou Spécial)
* La durée exacte en secondes
* La date et l'heure précises de diffusion
* La langue audio (VOSTFR ou VF)
* Le caractère original ou non censuré de la piste
* Le lien direct pour regarder l'épisode sur ADN
