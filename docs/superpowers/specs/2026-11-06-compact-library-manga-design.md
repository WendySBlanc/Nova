# Refonte compacte de la bibliotheque et de la fiche manhwa

## Intention

Reduire la place occupee par les controles de bibliotheque et la fiche manhwa, tout en donnant davantage de presence au carrousel. Les actions frequentes doivent rester visibles, rapides et accessibles sur telephone, tablette et avec une grande taille de texte.

## Objectifs

- Remplacer le carrousel en deux panneaux par le carrousel Material 3 multi-aspect. Il affiche un melange adaptatif de grandes, moyennes et petites cartes, avec un fond colore derive de la couverture, un degrade de lisibilite, la progression et un apercu des cartes suivantes.
- Utiliser les tabs Material 3 pour organiser la bibliotheque. Les tabs primaires selectionnent All Series ou Light Novel; les tabs secondaires scrollables selectionnent En cours, Archives ou une categorie utilisateur. La categorie generique Collections disparait au profit des vraies categories.
- Sortir le synopsis du panneau principal de la fiche. Afficher deux lignes par defaut et permettre une expansion explicite avec une animation courte.
- Fusionner le titre, le compteur et les actions de chapitres sur une seule rangee. Garder le telechargement et le filtre visibles; placer les actions de lecture globales dans un menu.
- Retirer `HorizontalFloatingToolbar` de cette zone pour utiliser des primitives Compose stables.
- Utiliser le `LoadingIndicator` Material 3 Expressive pour les chargements indetermines partages. Conserver les indicateurs lineaires ou circulaires determines lorsqu'une progression chiffree existe.
- Reorganiser Moi avec des tabs Material 3: Apercu, Lecture, Bibliotheque et Application. Une seule categorie de contenu est affichee a la fois.

## Non-objectifs

- Ne pas changer les donnees, le comportement de lecture, les regles de telechargement ou les filtres.
- Ne pas modifier les libelles fonctionnels en dehors des textes necessaires au nouveau menu compact.
- Ne pas ajouter de nouvelle dependance ni augmenter la hauteur minimale des controles.
- Ne pas remplacer les indicateurs de progression de telechargement ou de lecture qui representent une valeur mesurable.

## Approches considerees

1. **Surfaces compactes persistantes, retenue.** Les actions frequentes restent visibles et les actions secondaires passent dans des menus. Cette approche economise une rangee complete sans cacher les commandes essentielles.
2. **Controles retractables au scroll.** Plus spectaculaire, mais moins previsible et plus fragile avec les listes imbriquees.
3. **Interface ultra-minimale avec menu unique.** Plus compacte, mais ralentit les telechargements, le tri et le filtrage repetes.

## Comportement retenu

Le carrousel utilise `HorizontalMultiBrowseCarousel` et `rememberCarouselState`. Sa hauteur et sa largeur preferee sont calculees en continu depuis la largeur disponible, le padding et le nombre d'elements. Il n'utilise aucune largeur de carte fixe par type d'appareil. Les cartes changent naturellement de largeur et de masque pendant le swipe. Leur fond est une composition de l'image de couverture, d'un voile colore et d'un degrade sombre garantissant la lisibilite. Les animations respectent le reglage systeme de reduction des animations.

La bibliotheque utilise un `PrimaryTabRow` pour All Series et Light Novel, puis un `SecondaryScrollableTabRow` pour En cours, Archives et les categories creees par l'utilisateur. Le tri reste dans la zone d'actions et ne cree pas de rangee supplementaire. Sur tablette, les memes tabs sont places dans la colonne laterale sans dupliquer les options.

Moi utilise un `PrimaryScrollableTabRow` sous l'app bar. Apercu contient l'identite et les acces rapides; Lecture contient les modes de lecture; Bibliotheque contient les categories et les outils; Application contient les parametres et l'aide. Le tab selectionne est restaure pendant la recomposition.

Le synopsis est replie a deux lignes. Le clic sur Plus ou sur le texte l'etend en place; le meme controle permet de le replier. L'etat n'est pas persiste hors de la fiche.

L'en-tete des chapitres affiche le titre et le nombre restant a gauche, puis Telecharger, Filtrer et Plus a droite. Plus contient Tout marquer comme lu et Tout marquer comme non lu. Les zones tactiles restent d'au moins 48 dp.

L'ecran de chargement partage utilise `LoadingIndicator` de Material 3. La transition de fiche conserve son usage actuel de ce composant, avec la couleur d'accent NOVA.

## Validation

- Compilation Kotlin et tests unitaires existants.
- Verification des largeurs compactes, moyennes et etendues.
- Verification du melange de ratios et de l'absence de carte coupee en orientation portrait et paysage.
- Verification avec textes longs, grande police et reduction des animations.
- Aucun `ButtonGroup`, `ToggleButtonGroup` ou `HorizontalFloatingToolbar` dans les surfaces modifiees.
