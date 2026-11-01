# Contexte du projet Nova

Dernière mise à jour : 25 juillet 2026

Ce fichier est la mémoire de travail du projet. Il rassemble les décisions produit,
les demandes importantes, l’état du code, les procédures de build et les points à
tester lors des prochaines modifications.

## 1. Vision du produit

Nova est une application Android en Kotlin destinée à lire des manhwa et des light
novels.

Principes directeurs :

- L’interface reprend le langage visuel et l’ergonomie de Viki, adaptés à une
  bibliothèque de manhwa et de light novels.
- Le code propriétaire et les assets propriétaires ne doivent pas être copiés tels
  quels. Reproduire les comportements avec du code original et des alternatives
  open source récentes.
- La base technique vient de l’écosystème Komikku/Mihon/Tachiyomi.
- Komiku sert à récupérer les manhwa, mais son interface de bibliothèque ne doit pas
  être affichée dans Nova.
- Le lecteur doit être celui de LNReader, avec son interface et ses options, pour les
  light novels comme pour les manhwa.
- L’ancien lecteur Nova ne doit plus être utilisé.
- Toute l’interface doit s’adapter aux dimensions réelles du téléphone ou de la
  tablette.

## 2. Projet actif et références

Projet Android actif :

`C:\Users\skyde\OneDrive\Documents\Nova\nova-komikku-kotlin`

Références locales :

- `C:\Users\skyde\OneDrive\Documents\Nova\lnreader-source`
  contient le projet LNReader utilisé comme référence pour le lecteur.
- `C:\Users\skyde\OneDrive\Documents\Nova\extensions-source`
  contient le projet d’extensions, notamment NovelFrance.
- `nova-manhwa-kotlin` et `manhwa-android` sont d’anciens projets ou prototypes. Ne
  pas les considérer comme la source active sans vérification explicite.

Référence d’interface initiale :

`C:\Users\skyde\Downloads\com.viki.android-26.5.0.xapk`

Cette APK/XAPK sert uniquement à comprendre les parcours et les proportions de
l’interface. Ne pas en extraire du code ou des assets propriétaires pour les
redistribuer.

## 3. État technique actuel

- Langage : Kotlin.
- UI : Jetpack Compose, avec quelques composants Android/WebView.
- Package principal : `com.nova.manhwa`.
- Version courante : `versionCode = 101`.
- Nom de version : `1.14.0-nova-fast-data`.
- Variante utilisée pour les APK de test : `preview`.
- Le package de la variante preview reçoit le suffixe `.debug`.
- Le dépôt contient de nombreuses modifications non commitées. Toujours préserver
  les changements existants et vérifier `git status` avant toute intervention.

Fichiers structurants ajoutés ou fortement modifiés :

- `app/src/main/java/eu/kanade/tachiyomi/ui/home/HomeScreen.kt`
- `app/src/main/java/eu/kanade/tachiyomi/ui/home/NovaLibraryTab.kt`
- `app/src/main/java/eu/kanade/presentation/library/components/NovaVikiHomeScreen.kt`
- `app/src/main/java/eu/kanade/presentation/library/components/NovaVikiLibraryScreen.kt`
- `app/src/main/java/eu/kanade/presentation/manga/NovaVikiMangaScreen.kt`
- `app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/lnreader/`
- `app/src/main/java/eu/kanade/presentation/reader/settings/LnReaderSettingsPage.kt`
- `app/src/main/assets/`

## 4. Navigation et interface Viki

État et décisions actuels :

- L’ancien écran Accueil a été supprimé du parcours principal.
- La bannière de l’accueil a été transférée dans Bibliothèque.
- Bibliothèque est le premier onglet et l’écran ouvert au lancement.
- La navigation actuelle contient Bibliothèque, Parcourir et Moi.
- La barre de navigation doit conserver des dimensions proches de celle de Viki.
- La barre de notification Android doit être masquée dans l’expérience principale.
- La barre supérieure contenant la recherche doit rester visible pendant le scroll.
- L’interface du menu Moi doit suivre le style Viki.
- Les éléments inutiles liés à Chromecast, Get Pass et aux fonctions vidéo de Viki
  ne doivent pas être présents.
- Ne pas afficher les labels `Genre`, `Movie` ou `Saved`.
- Ne jamais afficher le texte `Saved from Komiku library`.

## 5. Bibliothèque

Comportement attendu :

- La bannière affiche les cinq derniers manhwa lus.
- La bannière défile automatiquement.
- Aucun bouton ou label `Save` ne doit apparaître sur les bannières : les titres
  affichés sont déjà dans la bibliothèque.
- La recherche est disponible en haut de la Bibliothèque.
- Deux onglets de contenu : `All Series` et `Light Novel`.
- L’onglet Light Novel est alimenté par les catégories créées.
- La grille de bibliothèque comporte trois éléments par ligne sur téléphone.
- La taille visuelle des couvertures a été demandée à 80 % de l’ancienne taille.
- Le nombre de chapitres restant à lire est affiché sur les miniatures.
- La pastille signalant un nouveau chapitre a été demandée puis supprimée. Ne pas la
  réintroduire.
- Les clics sur les labels de section doivent ouvrir une grille, pas une liste.
- `Continue Reading` doit contenir les titres commencés et afficher le prochain
  chapitre à lire.
- `Recently Added` doit être placé avant les anciennes sections de bibliothèque
  lorsqu’elles sont affichées.

Tri local actuellement ajouté à la Bibliothèque :

- Dernière lecture
- A-Z
- Ajout récent
- Nouveaux chapitres
- À lire
- Total chapitres
- Direction ascendante ou descendante

## 6. Page d’un manhwa ou light novel

La page de détail doit être une adaptation de la page d’épisode Viki à un titre de
lecture.

Comportements demandés :

- Bannière Viki améliorée et responsive.
- Le bloc ou bouton `Details` doit être supprimé.
- Le bouton de refresh séparé n’est pas nécessaire.
- Le pull-to-refresh reste disponible par glissement vers le bas.
- Le titre est cliquable et lance une recherche.
- Le nom de la source est cliquable et lance une recherche.
- Le bouton principal affiche `Read` avant toute lecture puis `Resume` dès qu’un
  chapitre a été commencé. Il reprend en priorité le dernier chapitre non terminé
  présent dans l’historique, puis un chapitre partiellement lu, puis le prochain
  chapitre non lu. Si tout est terminé, il rouvre le dernier chapitre consulté.
- Les miniatures de chapitre doivent toujours s’afficher.
- Les chapitres déjà téléchargés doivent être reconnus immédiatement.
- L’icône de téléchargement doit refléter correctement l’état local.
- La reconnaissance d’un téléchargement ne doit pas exiger une correspondance
  parfaite de la source d’origine.
- Les données restaurées depuis une sauvegarde doivent retrouver l’état téléchargé.
- L’option de téléchargement de tous les chapitres doit rester disponible, avec un
  comportement proche de Komiku.
- Les fonctions WebView nécessaires à une source doivent rester disponibles.
- Les éléments `Tracking` et le texte redondant `Téléchargé` sous celui-ci ne doivent
  pas encombrer cette page.
- Un chapitre devient automatiquement lu quand sa lecture est terminée.
- Il doit être possible de marquer tous les chapitres comme lus.
- Il doit aussi être possible de marquer comme lus tous les chapitres jusqu’au
  chapitre courant.
- Un appui long sur une ligne de chapitre ouvre un menu permettant de marquer ce
  chapitre comme lu ou de marquer tous les chapitres précédents comme lus. Le même
  menu reste accessible par l’icône d’actions à droite.

Objectif de performance :

- Afficher la page complète en moins d’une seconde quand les données existent
  localement.
- Montrer immédiatement les chapitres et états de téléchargement en cache.
- Actualiser ensuite les données réseau sans bloquer l’affichage.
- Éviter de recharger les mêmes données à chaque ouverture.

## 7. Lecteur LNReader

Le lecteur actif est `LnReaderWebViewViewer`. Il doit servir aux manhwa et aux light
novels. Les options LNReader doivent être affichées dès que ce viewer est actif,
sans dépendre uniquement du type détecté de la source.

Fonctions présentes ou migrées :

- WebView LNReader et assets HTML/CSS/JavaScript locaux.
- Modes de lecture et réglages typographiques LNReader.
- Taille de police, hauteur de ligne, marges et alignement du texte.
- Thèmes, couleurs et couleurs personnalisées.
- Onglet Advanced.
- CSS personnalisé.
- JavaScript personnalisé.
- Injection CSS/JS dans la WebView.
- Synchronisation à chaud des paramètres.
- TTS natif.
- File TTS, état TTS et progression.
- Reprise TTS lors du passage au chapitre suivant.
- Notification média TTS basée sur AndroidX Media3.
- Play/pause, stop, chapitre précédent/suivant, retour, seek et poursuite en
  arrière-plan.
- La fonctionnalité de traduction a été entièrement annulée le 28 juillet 2026.
  Ne pas réintroduire d’overlay, d’OCR, de modèles locaux ou d’onglet de traduction
  sans une nouvelle demande explicite.
- Barre de navigation du lecteur et scrollbar restaurées.
- Barre inférieure redondante contenant pourcentage, heure et autres informations
  supprimée.
- Pour les chapitres en images, masquer les icônes et options de texte qui ne sont
  pas pertinentes.
- Pour les manhwa en images uniquement, un tap sur une page doit afficher ou masquer
  l’interface du lecteur. Le script image ne doit jamais arrêter la propagation du
  clic. Le réglage `tapToScroll` reste disponible pour les light novels, mais est
  désactivé pour les manhwa afin que le tap ouvre toujours les commandes.
- Après une seconde régression constatée le 26 juillet 2026, l’affichage des
  commandes manhwa ne dépend plus du clic JavaScript. Le WebView possède un
  `GestureDetector` Android qui appelle directement `ReaderActivity.toggleMenu()`.
- Les barres Compose du lecteur sont maintenant créées sans dépendre de
  `SourceManager.isInitialized`, car un chapitre local peut être lisible même si sa
  source restaurée est indisponible.
- Lors de la première ouverture d’un chapitre manhwa en images, les commandes sont
  affichées automatiquement une fois pour confirmer visuellement leur présence.

Optimisations récentes :

- Le 25 juillet 2026, une course de démarrage a été corrigée : un chapitre manhwa
  local pouvait finir de charger avant l’attachement de `LnReaderWebViewViewer`.
  `ReaderActivity.updateViewer()` rejoue maintenant le dernier `viewerChapters`
  disponible après avoir ajouté le WebView au conteneur.
- La propriété CSS `content-visibility: auto` a été retirée des pages de manhwa, car
  certaines versions d’Android WebView pouvaient ne rien afficher.
- Une erreur pendant la préparation du contenu LNReader est désormais interceptée et
  produit une page d’erreur lisible au lieu de laisser le viewer vide.
- Le 25 juillet 2026, la navigation entre chapitres a été verrouillée pour empêcher
  les sauts de `n` vers `n+2`. Un seul changement peut être en cours à la fois.
- Chaque message JavaScript du lecteur contient maintenant l’identifiant du chapitre.
  Les événements tardifs de l’ancien chapitre sont ignorés au lieu de modifier la
  progression ou la navigation du nouveau.
- Les tâches de préparation HTML obsolètes sont annulées avec un numéro de
  génération, afin qu’un ancien chapitre ne puisse plus remplacer celui demandé.
- Un scroll produit par la restauration de position n’est plus considéré comme un
  geste utilisateur. Le passage automatique au chapitre suivant exige un véritable
  geste, une position toujours en bas après temporisation et démarre le nouveau
  chapitre en haut.
- Le 27 juillet 2026, LNReader a reçu un préchargement bidirectionnel : la liste des
  pages, les trois premières images et le HTML du chapitre précédent et du chapitre
  suivant sont préparés pendant la lecture du chapitre courant.
- Le cache HTML de transition est limité à quatre chapitres.
- Le dialogue modal de chargement des chapitres adjacents est désactivé pour
  LNReader. Le chapitre courant reste visible jusqu’à ce que le suivant soit prêt.
- Un événement de préchargement ne recharge plus inutilement le chapitre courant
  dans le WebView LNReader.
- Les premières images sont chargées immédiatement avec une priorité élevée.
- Les images suivantes utilisent le chargement différé et le décodage asynchrone.
- Les déplacements internes utilisent un scroll direct au lieu d’une animation
  lente.
- Le panneau intermédiaire de transition de chapitre est masqué.
- Le chapitre courant reste affiché jusqu’à ce que le suivant soit prêt.
- Le passage automatique au chapitre suivant se déclenche rapidement en fin de
  chapitre.
- La lecture doit continuer vers le chapitre suivant sans écran de transition.
- Un clic sur une image ne doit jamais empêcher de scroller jusqu’à la fin.

Régressions à surveiller :

- Options du lecteur invisibles.
- Écran blanc sur certains chapitres téléchargés.
- Chapitre suivant qui ne s’ouvre pas en fin de lecture.
- Scroll bloqué après un clic sur une image.
- Disparition de la navbar ou de la scrollbar.
- Retour accidentel à l’ancien lecteur Nova.

## 8. Données, téléchargements et sauvegardes

Principes attendus :

- Affichage local-first : base locale et cache immédiatement, réseau ensuite.
- Les changements de données doivent être observables et propagés sans forcer un
  rechargement complet de l’écran.
- La mise à jour de la bibliothèque doit réellement récupérer les nouveaux chapitres
  de chaque titre.
- Une actualisation automatique en arrière-plan est configurée pour 30 minutes.
- Le pull-to-refresh doit exister dans la Bibliothèque et sur la page d’un titre.
- Les chapitres locaux doivent être trouvés même après restauration d’une sauvegarde.
- Le cache de téléchargements doit être invalidé ou reconstruit quand le dossier de
  stockage ou les données restaurées changent.
- Les couvertures, téléchargements et sauvegardes automatiques doivent utiliser le
  dossier choisi par l’utilisateur.
- Lors de la première configuration, l’utilisateur doit pouvoir sélectionner le
  dossier principal de stockage.

Journalisation :

- Les modifications qui généraient des fichiers de logs personnalisés ont été
  annulées car elles provoquaient des arrêts systématiques de l’application.
- Ne pas réintroduire une création automatique de logs Nova sans une demande
  explicite et un test complet.
- Les fichiers `hs_err_pid*.log` présents à la racine sont des rapports de crash JVM
  de l’environnement de build, pas un système de logs à intégrer à l’application.

## 9. Recherche et fluidité

- La recherche doit réagir rapidement et éviter les requêtes inutiles.
- L’ouverture d’un résultat ne doit pas attendre un refresh réseau si les données
  locales sont disponibles.
- Les grilles et bannières doivent conserver des dimensions stables.
- Le scroll doit rester fluide dans les bibliothèques et le lecteur.
- Les changements de chapitre doivent être continus et sans clignotement.
- Tous les textes, boutons, bannières, grilles et barres doivent s’adapter à la
  largeur et à la hauteur de l’appareil.

## 10. Extensions

NovelFrance :

- Site : `https://novelfrance.fr/`
- L’extension doit être une APK séparée au format d’une extension
  Keiyoushi/Mihon.
- Elle ne doit pas être intégrée directement dans l’APK Nova.
- Source locale :
  `extensions-source/src/fr/novelfrance/src/eu/kanade/tachiyomi/extension/fr/novelfrance/NovelFrance.kt`
- La classe source sait récupérer les détails, la liste des chapitres et le contenu
  des chapitres.
- Un ancien problème de chapitres non détectés avait déjà été corrigé dans le projet
  de plugin LNReader ; comparer les parseurs en cas de régression.

Dépôts d’extensions :

- Éviter d’ajouter deux dépôts ayant la même empreinte de clé de signature.
- L’erreur connue est :
  `Keiyoushi has the same signing key fingerprint`.
- La restauration d’une sauvegarde doit dédupliquer les dépôts par empreinte et ne
  pas réajouter un dépôt équivalent sous un autre nom.

## 11. Build de Nova

Commande validée :

```powershell
.\gradlew.bat :app:assemblePreview --console=plain --no-watch-fs --no-daemon --max-workers=2 --no-problems-report
```

L’option `--no-problems-report` évite une erreur Gradle
`FileAlreadyExistsException` sur `build/reports/problems/problems-report.html`.

Sortie :

`app\build\outputs\apk\preview\app-preview.apk`

Dernier build généré avant le retrait de la traduction, désormais obsolète :

- Date : 27 juillet 2026 à 23:36
- Nom distribué : `Nova-v101-traduction-overlay.apk`
- Taille : 230 789 809 octets
- SHA-256 :
  `BD7D4A4543C1F4C8A1BB936F258CDFD2A52B688D34CDF42FB5BFDD282C30A85A`
- Lien temporaire Gofile : `https://gofile.io/d/dUH9jM`

Aucun build n’a encore été effectué après le retrait de la traduction.

Le lien Gofile peut expirer. Toujours vérifier que l’APK local a une date récente
avant un nouvel envoi.

Règle de collaboration :

- Ne lancer un build que lorsque l’utilisateur le demande ou le confirme.
- Après un build demandé, vérifier la date, la taille et le SHA-256 de l’APK.
- L’utilisateur utilise souvent son téléphone : fournir un lien de téléchargement
  accessible depuis le téléphone lorsqu’il le demande.

## 12. Contraintes de maintenance

- Ne jamais supprimer les caches Gradle, les dossiers de build ou d’autres fichiers
  pour libérer de l’espace sans autorisation explicite.
- Ne jamais annuler les modifications existantes du dépôt sans demande claire.
- Ne pas faire de `git reset --hard` ou de restauration globale.
- Les modifications actuelles ne sont pas toutes commitées : travailler avec elles.
- Utiliser les patterns existants de Komikku/Mihon et de LNReader.
- Préférer des changements ciblés et tester les parcours concernés.
- Après une modification du lecteur, tester au minimum un light novel réseau, un
  manhwa réseau, un chapitre image téléchargé et le passage au chapitre suivant.
- Après une modification des téléchargements, tester un téléchargement frais et une
  bibliothèque restaurée depuis une sauvegarde.

## 13. Checklist avant livraison

- L’application démarre sans arrêt systématique.
- Bibliothèque est l’écran principal.
- La bannière affiche les cinq derniers titres lus et défile seule.
- La recherche est visible et rapide.
- La grille est responsive et affiche le nombre de chapitres restant.
- La mise à jour de bibliothèque récupère les nouveaux chapitres.
- La page d’un titre apparaît rapidement avec ses chapitres locaux.
- Les icônes de téléchargement correspondent aux fichiers réellement présents.
- Le pull-to-refresh fonctionne.
- Le titre et la source lancent une recherche au clic.
- Le lecteur LNReader s’ouvre pour manhwa et light novel.
- Les options LNReader sont visibles.
- Le TTS et sa notification Media3 fonctionnent.
- La navbar et la scrollbar du lecteur sont visibles.
- La barre inférieure pourcentage/heure reste absente.
- Le scroll reste fluide et le chapitre suivant s’ouvre sans transition.
- Aucun système de logs personnalisé dangereux n’a été réintroduit.
- L’APK est construit, daté, haché et téléchargeable.

## 14. Optimisations lecteur du 28 juillet 2026

- La recherche UX a compare Mihon, Aniyomi, WEBTOON et les recommandations
  Android/WebView recentes.
- Le lecteur commun reste `LnReaderWebViewViewer` pour les manhwa et light novels.
- Pour les chapitres image, les requetes locales sont maintenant isolees par
  identifiant de chapitre afin qu'une requete tardive ne recoive jamais la page
  d'un autre chapitre.
- Les images valides disposent d'un cache WebView prive d'une heure. Les reponses
  en erreur restent sans cache.
- Les trois premieres pages sont prioritaires et les pages proches de la zone
  visible sont rechauffees pendant le scroll.
- Les pages hors ecran utilisent `content-visibility` et une taille intrinseque
  estimee pour limiter le travail de rendu et stabiliser la scrollbar.
- Une image en erreur est relancee automatiquement deux fois, puis un bouton
  accessible de 48 px permet de relancer uniquement cette page.
- Le zoom natif par pincement est disponible pour les chapitres image, sans
  afficher les anciens boutons de zoom WebView.
- Le panneau de reglages detecte les chapitres image et affiche un onglet Webtoon
  avec theme, marges laterales et zoom. Les onglets TTS et Advanced restent
  reserves aux light novels.
- Les options de barre inferieure heure/pourcentage ont ete retirees du panneau,
  conformement au choix de garder cette barre masquee.
- La traduction overlay reste entierement annulee.
- Aucun build n'a encore ete lance apres ces modifications.

## 15. Modernisation UI/UX Android 2026

- Recherche effectuee le 28 juillet 2026 a partir des recommandations Android
  officielles, Material 3 Expressive, Compose Pager, layouts adaptatifs et de la
  version Viki Android mise a jour en juin 2026.
- L'identite reste inspiree de Viki : hero immersif, contenu dense, navigation
  directe et progression de lecture visible.
- Le systeme visuel Nova est centralise dans `NovaVikiVisuals.kt` :
  fond encre, surfaces graphite, texte clair, accent bleu, accent corail,
  accent or et vert de succes.
- Le theme Material 3 par defaut utilise maintenant la meme palette afin que les
  menus, recherches, reglages et ecrans historiques restent coherents.
- La banniere utilise `HorizontalPager`, avance seule, se cale page par page et
  suspend l'avance pendant un glissement ou un appui.
- Les boutons principaux, recherche, tri, retour et actions importantes ont une
  cible tactile d'au moins 48 dp.
- Les onglets Bibliotheque utilisent les semantiques de selection et la navigation
  basse utilise le role d'onglet avec un indicateur actif.
- La grille reste a trois colonnes sur telephone puis passe a 4, 5 ou 6 colonnes
  selon la largeur disponible.
- La fiche d'une oeuvre conserve le hero Viki, utilise des actions de 48 dp et
  garde la barre Chapitres visible pendant le scroll.
- Sur petit ecran, le statut lu/en cours passe dans les metadonnees afin de ne pas
  comprimer le titre entre le telechargement et le menu.
- Le menu Moi est limite a 760 dp sur grand ecran et utilise des lignes d'au moins
  64 dp avec des accents fonctionnels.
- La recherche active utilise un champ tonal de 48 dp et les resultats possedent
  des cles stables pour eviter des recompositions inutiles.
- Aucun build n'a ete lance apres cette modernisation.

## 16. Fiche manhwa et light novel 2026

- La fiche Viki reste commune aux manhwa et aux light novels, avec une
  presentation adaptee au type de contenu.
- Le bouton Lire/Reprendre reste l'action principale. Le telechargement reste
  visible, tandis que Site et Partager sont regroupes dans un seul menu Plus.
- La double couverture est retiree sur telephone. Le poster superpose reste
  reserve aux grandes largeurs.
- La meme fiche Nova est utilisee sur telephone, paysage et tablette.
- L'en-tete fixe des chapitres affiche le total et le nombre restant a lire.
  Le filtre actif utilise l'accent bleu.
- Le menu a une seule entree pour Tout marquer lu est supprime : la commande
  utilise directement l'icone standard avec une cible tactile de 48 dp.
- Le statut Lu/En cours apparait une seule fois dans les metadonnees de la ligne.
- Les chapitres de light novel utilisent un repere typographique compact au lieu
  d'une miniature video. Les manhwa conservent leurs miniatures de chapitre.
- La detection du type light novel est centralisee dans `NovaContentType.kt` et
  partagee par la bibliotheque, la fiche et les reglages du lecteur.
- L'action d'un chapitre permet maintenant de basculer entre lu et non lu, puis
  de marquer les chapitres precedents comme lus.
- Un etat vide explique si aucun chapitre n'est disponible ou si les filtres ne
  renvoient aucun resultat.
- Aucun build n'a ete lance apres cette refonte.

## 17. Lecteur manhua et light novel 2026

- La modernisation du lecteur s'appuie sur les recommandations Android 2026 pour
  les feuilles basses, les contenus adaptatifs, le reflow et les cibles tactiles
  de 48 dp, ainsi que sur l'organisation actuelle du lecteur Mihon.
- Le moteur reste exclusivement `LnReaderWebViewViewer` pour les manhua, webtoons
  et light novels. Aucun ancien lecteur Nova n'a ete reintroduit.
- La barre superieure affiche le contexte de lecture, le signet et un unique menu
  Plus regroupant Site, Navigateur et Partager.
- Le dock inferieur conserve cinq commandes fixes : chapitre precedent, retour en
  haut, chapitres, reglages et chapitre suivant. Sa largeur est limitee sur
  tablette et ses positions ne changent pas en paysage.
- Le panneau de reglages est une feuille basse sur telephone et un panneau centre
  sur grand ecran. Les onglets sont adaptes au contenu : Webtoon/General pour les
  images et Lecture/General/Voix/Avance pour le texte.
- Les polices et les voix TTS utilisent un menu unique au lieu d'une accumulation
  de pastilles. L'alignement utilise un controle segmente et les themes montrent
  un apercu lisible.
- Le texte des light novels est limite a une largeur de lecture confortable. Les
  images restent en plein flux avec le pincement et les marges choisies.
- Le rail de progression ne montre plus les nombres 0/100. Il utilise une ligne
  bleue fine avec une zone tactile de 48 px, verticale ou horizontale selon la
  preference.
- Le pied WebView batterie/heure/pourcentage est entierement retire de l'interface
  et n'est plus monte par le JavaScript.
- Le bouton TTS flottant utilise la palette Nova, reste au-dessus du dock et
  conserve le geste LNReader permettant de choisir le passage de depart. La
  lecture en arriere-plan et la notification restent gerees par Media3.
- Le mode page par page utilise les dimensions reelles de la fenetre, prend en
  charge le redimensionnement et conserve la position courante lors d'une rotation
  ou d'un changement typographique.
- Le tiroir des chapitres affiche le total, une fermeture explicite, des lignes de
  56 dp, un repere bleu pour le chapitre courant et des commandes compactes pour
  atteindre le premier ou le dernier chapitre.
- La traduction overlay reste annulee.
- Aucun build n'a ete lance apres cette modernisation du lecteur.

## 18. Modernisation globale de Nova

- Le langage visuel commun est defini dans `NovaVikiVisuals.kt` : palette sombre
  neutre, progression bleue, succes vert, nouveaute corail, formes de 4 a 8 dp,
  typographie sans espacement negatif et mouvements de 120 a 220 ms.
- `TachiyomiTheme` applique ces formes et cette typographie aux composants
  Material 3 de toute l'application.
- Les barres d'application, champs de recherche, menus, etats vides et chargements
  partagent maintenant la meme hierarchie visuelle et les memes couleurs.
- La navigation reste basse sur telephone et utilise un rail sur grand ecran.
  Les libelles, icones et indicateurs actifs conservent des dimensions stables.
- La bibliotheque est centree, reste en trois colonnes sur telephone et augmente
  progressivement jusqu'a six colonnes sans etirer les couvertures.
- La banniere, les onglets de bibliotheque et la recherche sont limites a une
  largeur confortable sur tablette et grand ecran.
- La recherche globale met en evidence la source de depart avec un repere visuel
  accessible et affiche les erreurs comme des erreurs semantiques.
- Le catalogue des sources utilise des lignes de 64 dp, des en-tetes plus lisibles
  et une colonne centree de 760 dp sur grand ecran.
- Les fiches manhwa et light novel sont limitees a 1200 dp et conservent les
  chapitres dans le meme flux, avec l'en-tete de chapitre fixe.
- Historique, mises a jour, menu Moi et parametres utilisent une largeur maximale
  de 760 dp pour faciliter le balayage visuel.
- Le menu Moi regroupe les commandes par fonction, avec une seule ligne par action
  et des accents reserves aux etats utiles.
- Les parametres ont des lignes de 64 dp, des icones tonales de 40 dp et une
  selection compacte de 8 dp en mode deux panneaux.
- Les etats vides n'utilisent plus de visage aleatoire : ils affichent un symbole
  de lecture stable, un message clair et des actions centrees.
- Le chargement global utilise l'indicateur Material 3 Expressive contenu, sans
  modifier la taille de la mise en page.
- L'accessibilite conserve des cibles tactiles d'au moins 48 dp, des roles
  d'onglets, des descriptions d'icones et des erreurs annoncees aux lecteurs
  d'ecran.
- Aucun build Gradle n'a ete lance apres cette modernisation globale. Toujours
  demander l'accord de l'utilisateur avant le prochain build.

## 19. Verification statique apres modernisation

- Les diffs ne contiennent ni marqueur de conflit ni erreur d'espacement Git.
- Les fichiers XML modifies sont bien formes, les catalogues TOML sont lisibles
  et les ressources de meme nom ne sont pas dupliquees dans les traductions.
- Les scripts JavaScript du lecteur passent le controle de syntaxe Node et toutes
  les ressources `file:///android_asset` referencees existent.
- Les signatures Material 3 Expressive et Media3 utilisees ont ete controlees
  directement dans les AAR presents dans le cache local.
- Le theme respecte de nouveau le choix utilisateur et le mode AMOLED, tout en
  conservant l'identite sombre Nova.
- L'etat instantane de la fiche conserve les chapitres en cache jusqu'au retour
  de la base de donnees, ce qui evite un clignotement vide.
- Le cache de telechargement ne peut plus associer un chapitre homonyme provenant
  d'un autre titre. La restauration entre sources du meme titre reste permise.
- La valeur Jamais de la mise a jour automatique est maintenant respectee. Le
  nouvel intervalle par defaut reste de 30 minutes.
- Le calcul du prochain chapitre ne charge les donnees fusionnees que pour une
  serie provenant effectivement de la source fusionnee.
- La barre d'etat Android reste masquee dans le lecteur et la notification TTS
  Media3 comporte une action Arreter explicite.
- Aucun build ou test Gradle n'a ete lance. Une compilation puis un essai reel
  sur telephone restent necessaires et doivent etre autorises par l'utilisateur.

## 20. Build Android valide

- L'utilisateur a autorise explicitement le build le 28 juillet 2026.
- Le premier daemon Gradle a manque de memoire virtuelle. Le build stable utilise
  `--no-daemon`, `--max-workers=1`, une JVM de 2 Go et le compilateur Kotlin dans
  le processus Gradle.
- `LoadingScreen` utilise l'indicateur Material 3 stable car le module
  `presentation-core` n'expose pas l'API Expressive de l'application principale.
- Les imports `LocalIndication` et `dp`, ainsi que les parametres du bouton
  segmente d'alignement, ont ete corriges apres compilation.
- `assembleRelease` et `assemblePreview` se terminent avec succes.
- La variante Release est non signee. La variante Preview est installable et
  signee avec le certificat Android Debug du projet.
- APK : `app/build/outputs/apk/preview/Nova-v101-modernise.apk`
- Identifiant : `com.nova.manhwa.debug`
- Version : code 101, nom `1.14.0-nova-fast-data-1`
- SDK : minimum 26, cible 36
- Taille : 114315104 octets, soit 109,02 Mio
- SHA-256 :
  `786B5FD671FD681B2C7A04BBCB7C3D43E3D36A8FDAF5EEA204FEF44C3B5AD8A2`
- La signature APK v2 a ete verifiee avec `apksigner`.
- Un essai fonctionnel reel sur telephone reste necessaire.

## 21. Suppression des options en double

- Le menu Moi ne repete plus `Donnees et stockage` ni `A propos`.
- Ces deux destinations restent disponibles une seule fois dans `Reglages`.
- La section Application du menu Moi conserve uniquement `Reglages` et `Aide`.
- Les callbacks et les imports devenus inutiles ont ete supprimes dans
  `MoreScreen.kt` et `MoreTab.kt`.
- Les onglets du panneau lecteur actif ont ete controles : chaque preference y
  est rendue une seule fois.
- Aucun build Gradle n'a ete lance apres cette modification. Toujours demander
  l'accord de l'utilisateur avant le prochain build.

## 23. Actions uniques sur la fiche manhwa

- La banniere conserve uniquement Lire/Reprendre, Ajouter, Site et Partager.
- Les actions globales des chapitres sont regroupees dans l'en-tete fixe :
  telechargement, tout marquer lu et filtres.
- Le telechargement global n'est plus affiche dans la banniere.
- Le menu de telechargement global contient quatre choix distincts : prochain
  chapitre, cinq prochains, non lus et tous les chapitres. Les choix dix et
  vingt-cinq ont ete retires car ils recouvraient souvent les memes resultats.
- Chaque ligne conserve uniquement son indicateur de telechargement propre.
- Le menu Lu/Non lu et Marquer les precedents est accessible uniquement par appui
  long. Le bouton trois-points qui ouvrait exactement le meme menu est retire.
- Lire/Reprendre vise la progression generale, tandis qu'un clic sur une ligne
  ouvre ce chapitre precis : ces deux actions restent donc distinctes.
- Aucun build Gradle n'a ete lance apres cette modification. Toujours demander
  l'accord de l'utilisateur avant le prochain build.

## 22. Etat de lecture visible dans les chapitres

- Chaque ligne de chapitre affiche une pastille fixe : `A lire`, `En cours` ou
  `Lu`.
- Les couleurs sont distinctes : bleu pour a lire, or pour en cours et vert avec
  coche pour lu.
- La miniature d'un chapitre lu affiche une coche au lieu du bouton Lecture.
- Les light novels utilisent la meme couleur d'etat sur leur repere vertical.
- Le titre d'un chapitre lu est attenue et moins gras, sans dependre uniquement
  de la couleur.
- L'etat est expose comme description semantique pour les lecteurs d'ecran.
- Les metadonnees restent tronquables sur petit ecran afin que la pastille ne
  disparaisse jamais.
- Aucun build Gradle n'a ete lance apres cette modification. Toujours demander
  l'accord de l'utilisateur avant le prochain build.

## 24. Miniatures selon la progression de lecture

- Une miniature de chapitre manhwa lu est entierement affichee en noir et blanc.
- La coche centrale a ete retiree de la miniature lue : la desaturation remplace
  cet ancien indicateur visuel.
- Pour un chapitre en cours, la portion droite en noir et blanc correspond au
  pourcentage de pages restant a lire.
- Le pourcentage restant est aussi affiche dans les metadonnees du chapitre.
- Le nombre total de pages est memorise pendant la lecture. La fiche manhwa ne
  parcourt donc pas les fichiers telecharges et conserve son chargement rapide.
- Les anciens chapitres termines sont immediatement en noir et blanc. Pour une
  ancienne progression partielle sans total memorise, le pourcentage devient
  disponible des la prochaine ouverture du chapitre.
- La compilation Kotlin ciblee et `assemblePreview` se terminent avec succes.
- APK : `app/build/outputs/apk/preview/Nova-v101-modernise.apk`
- Taille : 114315104 octets.
- SHA-256 :
  `31121B86601A8BB47EA559149B72A47FA449E6AECFD0C463B78774CC7C25F7B2`
- La signature APK v2 et les metadonnees package/version ont ete verifiees.
- Le serveur de telechargement repond en HTTP 200 sur le port 8765.

## 25. Suppression de Soutenez-moi

- La carte `Soutenez-moi` a ete retiree du menu `Moi`.
- La fonction Compose associee, ses imports et la constante du lien GitHub
  Sponsors ont ete supprimes.
- Les ressources i18n communes du projet amont restent intactes.
- Aucun build n'a ete lance apres cette modification. Toujours demander l'accord
  de l'utilisateur avant le prochain build.

## 26. Bibliotheque UI/UX 2026

- L'en-tete fixe de la bibliotheque affiche maintenant l'etat utile de la
  collection et conserve la recherche accessible pendant le defilement.
- La banniere des cinq dernieres lectures est plus compacte afin de laisser
  apparaitre les controles et le debut de la grille dans le premier ecran.
- Les onglets `All Series` et `Light Novel` utilisent un controle segmente avec
  leur nombre de titres.
- Le choix du mode, le tri et sa direction sont reunis dans un seul bandeau
  adaptatif. Les informations repetees ont ete retirees.
- La grille reste a trois colonnes sur telephone, puis passe a quatre, cinq et
  six colonnes selon la largeur disponible.
- Les couvertures ne sont plus masquees par un gros bouton Lecture central.
  Elles affichent le nombre de chapitres restants, le pourcentage lu et une barre
  de progression.
- Une animation d'appui courte donne un retour tactile visuel sans perturber le
  defilement.
- Un squelette stable remplace le faux etat vide pendant le chargement initial.
- Les sous-titres de lecture et les textes de tri ont ete harmonises en francais.
- Les cibles interactives importantes conservent une hauteur minimale de 48 dp.
- Les controles statiques couvrent les largeurs 320, 360, 411, 600, 840 et
  1200 dp. Aucun build Gradle n'a ete lance apres cette modification.
- Toujours demander l'accord de l'utilisateur avant le prochain build.

## 40. Inventaire recu le 8 septembre 2026

- Inventaire conserve dans
  `extension-inventory/nova-installed-extensions-2026-09-08.json`.
- Generation Nova : `2026-09-08T21:13:24.392459+02:00`.
- 102 extensions installees : 56 actives et 46 obsoletes.
- Repartition : 32 francaises, 55 anglaises, 14 multilingues et 1 coreenne.
- Le catalogue Nova ne signalait aucune mise a jour disponible au moment de
  l'export.
- `Mangas-Origines.fr` est installee en version `1.6.60` avec le package
  `eu.kanade.tachiyomi.extension.fr.mangasoriginesfr`.
- Une extension obsolete n'est plus presente dans le catalogue et doit etre
  remplacee ou migree ; elle ne peut pas recevoir une mise a jour ordinaire.

## 27. Build de la bibliotheque modernisee

- L'utilisateur a autorise explicitement le build le 29 juillet 2026.
- L'import Compose de `animateColorAsState` a ete adapte a la version utilisee
  par le projet.
- `compilePreviewKotlin` et `assemblePreview` se terminent avec succes.
- La suppression de `Soutenez-moi` et la bibliotheque UI/UX 2026 sont incluses.
- APK : `app/build/outputs/apk/preview/Nova-v101-modernise.apk`
- Identifiant : `com.nova.manhwa.debug`
- Version : code 101, nom `1.14.0-nova-fast-data-1`
- SDK : minimum 26, cible 36
- Taille : 114331488 octets.
- SHA-256 :
  `F95367DB20114D21C7CBA3F2870651EA887AF585D80BA13574301E0995C5DAEE`
- La signature APK v2 a ete verifiee.
- Le serveur de telechargement repond en HTTP 200 sur le port 8765.

## 28. Categories dans la bibliotheque Nova

- Les deux modes `All Series` et `Light Novel` restent les filtres de type
  principaux.
- Une rangee horizontale affiche maintenant `Toutes` puis chaque categorie creee
  et non masquee.
- L'ordre visuel est identique sur toutes les largeurs : type de contenu,
  categories, puis tri. Ces controles utilisent les memes marges horizontales.
- Chaque categorie affiche un compteur recalcule selon le mode actif.
- Le filtre de categorie, le mode et le tri s'appliquent simultanement.
- La banniere et la grille utilisent la meme selection de categorie.
- Les titres sans categorie restent accessibles dans `Toutes`.
- Si la categorie selectionnee est supprimee, le filtre revient automatiquement
  sur `Toutes`.
- Les appartenances sont lues depuis les donnees locales deja chargees. Aucun
  acces reseau ni parcours des telechargements n'est ajoute.
- Aucun build Gradle n'a ete lance apres cette modification. Toujours demander
  l'accord de l'utilisateur avant le prochain build.

## 29. Build avec categories

- L'utilisateur a autorise explicitement le build le 29 juillet 2026.
- `compilePreviewKotlin` et `assemblePreview` se terminent avec succes.
- Les categories dynamiques et leur placement aligne sont inclus.
- APK : `app/build/outputs/apk/preview/Nova-v101-modernise.apk`
- Identifiant : `com.nova.manhwa.debug`
- Version : code 101, nom `1.14.0-nova-fast-data-1`
- Taille : 114331488 octets.
- SHA-256 :
  `FEDA51C70A603810A0B1A9B2FD9B500979C996A54FB1B3CF2FD9D3DFA3A79A00`
- La signature APK v2 a ete verifiee.
- Le serveur de telechargement repond en HTTP 200 sur le port 8765.

## 30. Qualite des miniatures de bibliotheque

- Les cartes de la bibliotheque utilisent maintenant le modele `MangaCover`
  dedie afin de conserver la gestion des couvertures personnalisees et
  l'invalidation correcte du cache.
- Chaque miniature est decodee a `480 x 667 px`, une taille adaptee a la grille
  de trois a six colonnes et aux ecrans haute densite.
- Le RGB565 est desactive uniquement pour ces couvertures afin d'eviter les
  bandes de couleurs et la perte de details sur les appareils a memoire limitee.
- La precision Coil est fixee a `EXACT` et l'interpolation Compose a
  `FilterQuality.High`.
- La taille reste bornee afin de conserver un defilement fluide et une
  consommation memoire raisonnable.
- Aucun build Gradle n'a ete lance apres cette modification. Toujours demander
  l'accord de l'utilisateur avant le prochain build.

## 31. Qualite des miniatures dans toute l'application

- Un helper Coil commun, `rememberHighQualityImageRequest`, desactive le RGB565
  uniquement pour les images d'interface et conserve les dimensions imposees
  lorsqu'un ecran en a besoin.
- Le composant partage `MangaCover` utilise maintenant ce helper et
  `FilterQuality.High`. Cela couvre notamment la bibliotheque standard, la
  recherche, l'historique, les mises a jour, les migrations et le suivi.
- Les banniere et cartes Nova, la fiche manhwa/light novel et les miniatures de
  chapitres utilisent le meme rendu haute qualite.
- Les apercus de pages, icones d'extensions et avatars utilisent aussi ce
  traitement.
- Les couvertures des notifications, l'affichage plein ecran et l'enregistrement
  de la couverture originale n'utilisent plus le RGB565.
- Les images longues du lecteur manhwa restent sur leur pipeline specialise afin
  d'eviter de doubler leur consommation memoire.
- Le cache memoire Coil reste borne et seules les images visibles sont decodees a
  la taille de leur composant, sauf la grille Nova bornee a `480 x 667 px`.
- Les controles statiques de format et de couverture des appels Coil sont
  propres. Aucun build Gradle n'a ete lance apres ces modifications.
- Toujours demander l'accord de l'utilisateur avant le prochain build.

## 32. Modernisation du menu Moi

- L'onglet et l'en-tete utilisent maintenant le libelle francais `Moi`.
- L'en-tete reste visible pendant le defilement.
- La grande carte generique a ete remplacee par une identite Nova compacte qui
  affiche aussi les modes actifs : telecharges uniquement et historique
  suspendu.
- Telechargements, statistiques, categories et parametres sont regroupes dans
  quatre acces rapides uniques. Ils ne sont plus repetes dans les sections.
- L'etat de la file de telechargement reste visible dans son raccourci.
- Les modes de lecture utilisent des lignes avec interrupteurs et cibles
  tactiles completes.
- Activite, bibliotheque et aide utilisent des groupes plats avec separateurs,
  au lieu d'une succession de cartes independantes.
- Les mises a jour et l'historique ne sont affiches dans `Moi` que lorsque leurs
  onglets de navigation sont masques.
- La mise en page reste sur une colonne sur telephone. A partir de 720 dp, les
  sections passent sur deux colonnes; les quatre raccourcis passent sur une ligne
  a partir de 600 dp.
- Les marges sont adaptees aux largeurs compactes, normales et etendues, avec
  des cibles tactiles d'au moins 48 dp.
- Les controles statiques de format et de duplication sont propres. Aucun build
  Gradle n'a ete lance apres cette modification.
- Toujours demander l'accord de l'utilisateur avant le prochain build.

## 33. Modernisation globale UI/UX adaptative

- La direction visuelle Nova est conservee : fond sombre neutre, accents bleu,
  rose, or et vert, angles de 6 a 8 dp et typographie sans espacement negatif.
- Les parametres sont regroupes par intention : lecture, bibliotheque, services,
  donnees, avance et application. Les options ne forment plus une liste plate.
- L'apercu des parametres passe automatiquement a deux colonnes a partir de
  840 dp. Les sous-ecrans restent contenus et centres.
- Les groupes de preferences utilisent une seule surface plate avec separateurs,
  au lieu d'une carte independante pour chaque ligne.
- Les onglets secondaires sont centres et limites en largeur sur grand ecran.
- Les statistiques, categories, extensions, filtres de sources et d'extensions,
  tris de flux, recherche de parametres, depots d'extensions, file de
  telechargement et page A propos utilisent une largeur adaptee.
- Les depots et sections repetes utilisent des surfaces plates avec contour
  discret. Les moteurs de liste, de telechargement et du lecteur restent
  inchanges afin de preserver leur fiabilite.
- Les controles principaux conservent des cibles tactiles d'au moins 48 dp. Les
  petites dimensions relevees correspondent aux icones et indicateurs internes,
  pas a la zone interactive.
- Les choix suivent les recommandations Android actuelles : mise en page par
  classes de largeur, navigation compacte ou laterale, groupes de parametres et
  contenu non etire sur tablette ou appareil pliable.
- `git diff --check` est propre et aucune duplication de composable n'a ete
  detectee dans les fichiers modifies.
- Aucun build Gradle n'a ete lance apres cette passe. Toujours demander l'accord
  de l'utilisateur avant le prochain build.

## 34. Slider du lecteur Nova

- Le slider historique existait encore dans le projet mais n'etait plus branche
  a la barre inferieure Nova.
- Un slider compact est maintenant affiche au-dessus des cinq commandes du
  lecteur lorsque le contenu possede plusieurs pages ou utilise LNReader.
- Pour un manhwa, le curseur affiche la page pendant le glissement et ne navigue
  qu'au relachement afin d'eviter les sauts et chargements successifs.
- Pour une Light Novel, le curseur utilise une progression de 0 a 100 %. Il sait
  se positionner dans le mode pagine LNReader ou dans le mode de defilement.
- Les valeurs gauche et droite ont une largeur stable afin que le rail ne bouge
  pas lorsque le numero de page change.
- Le rail reprend les couleurs Nova, masque les graduations trop denses et
  fournit un retour haptique pendant le glissement.
- Les preferences existantes sont respectees : slider horizontal dans le dock,
  ou slider vertical a gauche ou a droite pour les lecteurs verticaux.
- LNReader envoie maintenant la position instantanee a Android, meme lors d'un
  retour vers le haut. Cette position pilote le slider sans diminuer la
  progression maximale sauvegardee du chapitre.
- La barre WebView calcule sa progression avec la distance reellement defilable :
  0 % en haut et 100 % en bas. Son glissement utilise le meme calcul.
- Pour les manhwa, la page courante et le rail sont mis a jour dans le meme
  changement d'etat immediat. La sauvegarde en arriere-plan ne retarde plus le
  curseur.
- La zone du slider conserve une hauteur tactile minimale de 48 dp et la barre
  reste bornee a 440 dp sur les grands ecrans.
- Les controles statiques de format sont propres. Aucun build Gradle n'a ete
  lance apres cette modification. Toujours demander l'accord de l'utilisateur
  avant le prochain build.

## 35. Recuperation des nouveaux chapitres

- Le message errone `Aucun chapitre trouve` pouvait apparaitre lorsque la source
  avait change l'URL ou le slug d'une oeuvre deja enregistree.
- La fiche et la mise a jour de la bibliotheque utilisent maintenant le meme
  chemin de recuperation : requete normale, nouvelles metadonnees, puis recherche
  de secours avec le titre d'origine.
- Une URL locale n'est remplacee que si un unique resultat au titre exact renvoie
  effectivement une liste de chapitres. Les resultats ambigus sont ignores.
- Les erreurs reseau d'origine sont conservees lorsqu'aucune recuperation ne
  reussit, afin de ne pas les transformer en faux message d'absence de chapitre.
- Les annulations de coroutines Android sont toujours propagees.
- Le chargement initial et le rafraichissement manuel demandent les metadonnees
  avant les chapitres lorsqu'ils doivent tous les deux etre actualises.
- Aucun build Gradle n'a ete lance apres cette correction. Toujours demander
  l'accord de l'utilisateur avant le prochain build.

## 36. Mise a jour moderne des extensions dans Nova

- Nova lit maintenant le catalogue moderne `index.pb` du depot Keiyoushi tout
  en conservant le format historique `index.min.json` pour les autres depots.
- Les catalogues et listes d'extensions modernes peuvent etre encodes en
  Protobuf ou JSON et compresses en Gzip.
- Les URL directes des APK et icones du catalogue moderne sont conservees afin
  que l'installation et la mise a jour passent par l'ecran Extensions de Nova.
- Le chargeur accepte la bibliotheque d'extensions 1.6 et lit aussi la
  metadonnee moderne `tachiyomix.extensionLib`.
- L'API source expose `SMangaUpdate` et `SManga.memo`, avec un pont entre le
  nouvel appel combine et les anciens appels de details et de chapitres.
- Le client reseau est compatible avec le nouveau socle Keiyoushi : les anciens
  intercepteurs de compression ne sont plus ajoutes au client partage et la
  detection Cloudflare utilise l'en-tete `cf-mitigated`.
- La saisie d'un depot accepte desormais une adresse terminee par `index.pb`.
  La copie du lien Keiyoushi depuis les parametres renvoie aussi ce format.
- L'extension officielle `Mangas-Origines.fr` version 57 pourra ainsi etre
  proposee dans Nova. Elle gere l'ancien chemin `catalogues`, le nouveau chemin
  `oeuvre` et le nouvel endpoint AJAX des chapitres.
- `git diff --check` est propre. Aucun build Gradle n'a ete lance apres ces
  modifications. Toujours demander l'accord de l'utilisateur avant le prochain
  build.

## 37. Depots d'extensions WendySBlanc

- Le code source Keiyoushi est maintenant forke sur
  `https://github.com/WendySBlanc/extensions-source`.
- Le clone permanent local se trouve dans
  `C:/Users/skyde/OneDrive/Documents/Nova/extensions-source`.
- Son remote `origin` pointe vers `WendySBlanc/extensions-source` et son remote
  `upstream` vers `keiyoushi/extensions-source`.
- Le commit `87964db49` ajoute une synchronisation GitHub Actions quotidienne de
  la branche `main` avec l'amont. Le declenchement manuel est aussi disponible.
- Le depot d'APK est forke sur `https://github.com/WendySBlanc/extensions`, avec
  un clone local dans `C:/Users/skyde/OneDrive/Documents/Nova/extension-repo`.
- Le commit `2a5a2fd` ajoute une synchronisation quotidienne de sa branche
  `repo` avec `keiyoushi/extensions`. La premiere execution manuelle a reussi.
- Les APK conservees dans ce fork gardent la signature officielle Keiyoushi,
  ce qui permet les mises a jour Android sans reinstallation des extensions.
- Nova utilise `https://raw.githubusercontent.com/WendySBlanc/extensions/repo`
  pour tout depot portant l'empreinte Keiyoushi.
- Lors du rafraichissement des depots, une ancienne URL Keiyoushi est remplacee
  en base de donnees par l'URL WendySBlanc grace a la meme empreinte.
- Le dossier local non suivi `src/fr/novelfrance/` a ete preserve dans le clone
  source. Il n'a pas ete publie, car son ancien format doit etre migre vers la
  nouvelle API avant de rejoindre le fork.
- Le build Nova suivant cette liaison a ete valide le 7 septembre 2026.

## 38. Build Nova du 7 septembre 2026

- Commande validee : `:app:assemblePreview` avec la compilation Kotlin
  `in-process`, necessaire lorsque le daemon Kotlin est bloque par Windows.
- Resultat : `BUILD SUCCESSFUL`.
- APK local :
  `app/build/outputs/apk/preview/Nova-v101-github-updates.apk`.
- Package : `com.nova.manhwa.debug`.
- Version : `101` (`1.14.0-nova-fast-data-1`).
- Taille : `114413536` octets.
- SHA-256 :
  `465574FBB4DCAD59B74D54A6D1992264F8C99A412522B9DE1A7F830AC9CFB350`.
- Signature APK v2 verifiee avec `apksigner`.
- Lien mobile temporaire : `https://gofile.io/d/Ud1swRnX`.
- Toujours demander l'accord de l'utilisateur avant un futur build.

## 39. Inventaire des extensions installees

- Nova maintient automatiquement la liste des extensions installees dans le
  dossier de stockage choisi par l'utilisateur :
  `extensions/nova-installed-extensions.json`.
- Le fichier est actualise au demarrage, apres un changement de dossier et
  chaque fois que la liste installee ou le catalogue disponible change.
- Chaque entree contient le package, la langue, la version installee, la
  version disponible, l'etat de mise a jour, la signature et les sources.
- L'ecran Extensions propose l'action `Partager la liste des extensions
  installees`, qui genere le meme JSON dans le cache et ouvre le partage
  Android. L'utilisateur peut ainsi joindre le fichier a une conversation.
- Aucun jeton GitHub n'est stocke dans Nova et aucune liste n'est publiee sans
  une action explicite de l'utilisateur.
- Le build Preview avec cet inventaire a reussi le 7 septembre 2026.
- APK : `app/build/outputs/apk/preview/Nova-v101-extension-inventory.apk`.
- Taille : `114414120` octets.
- SHA-256 :
  `A58FBDB8F1B292D7CFD5DBB39DA91589003D311FE64D85A2BCAB0FE934ADA89C`.
- Signature APK v2 verifiee avec `apksigner`.
- Lien mobile temporaire : `https://gofile.io/d/jBy8P0TI`.
- Toujours demander l'accord de l'utilisateur avant le prochain build.

## 41. Depot d'extensions independant Nova

- URL a ajouter dans Nova :
  `https://raw.githubusercontent.com/WendySBlanc/extensions/repo/index.pb`.
- Depot de distribution : `https://github.com/WendySBlanc/extensions`.
- Depot source : `https://github.com/WendySBlanc/extensions-source`.
- Empreinte SHA-256 de la signature Nova :
  `f25a2ca5f93f7af24de9afff1a5afc2cf7600d3d87af03d8497a0ae4865de701`.
- Le catalogue contient les 56 extensions actives de l'inventaire du
  8 septembre 2026, ainsi que NovelFrance migree ensuite. Les versions des
  56 extensions initiales ont ete incrementees pour la migration.
- Les APK et JAR sont construits par GitHub Actions et publies uniquement sur
  le depot WendySBlanc avec la cle Nova.
- Les anciens workflows de miroir automatique Keiyoushi ont ete supprimes.
- Nova ajoute automatiquement ce depot, migre l'ancienne entree Keiyoushi et
  restaure les sauvegardes vers l'URL et l'empreinte Nova.
- Les anciennes APK portent une autre signature Android : leur remplacement
  demande une desinstallation/reinstallation unique des extensions.
- Aucun build Gradle de l'application Nova n'a ete lance apres cette migration.
  Toujours demander l'accord de l'utilisateur avant le prochain build.

## 42. Correction du crash Zstandard et publication Nova

- Le journal du 8 septembre 2026 contenait quatre crashs, tous dus a
  `NoClassDefFoundError: okhttp3.zstd.Zstd` dans des extensions 1.6, notamment
  Kagane et Asura Scans.
- Nova utilise desormais OkHttp `5.4.0` et embarque `okhttp-zstd` dans le bundle
  reseau. L'APK contient `okhttp3.zstd.Zstd` et `libzstd-kmp.so` pour les quatre
  architectures Android.
- Le build `:app:assemblePreview` a reussi le 8 septembre 2026.
- APK : `app/build/outputs/apk/preview/Nova-v101-zstd-extension-repo.apk`.
- Taille : `117437161` octets.
- SHA-256 :
  `91F244D235152035741CC30153EBDF562C1A4F3B6B1F8F02F2BF399A8F1D8D82`.
- Signature APK v2 verifiee.
- Lien mobile : `https://gofile.io/d/GGnYL4wY`.
- Le workflow `34269653874` a construit et publie avec succes les 56 extensions
  sur le depot WendySBlanc.
- Le catalogue public contient exactement 56 entrees, toutes les URL d'APK
  pointent vers `WendySBlanc/extensions`, et une APK telechargee a ete verifiee
  avec l'empreinte Nova `f25a2ca5...e701`.
- Toujours demander l'accord de l'utilisateur avant le prochain build Nova.

## 43. Publication de NovelFrance

- L'ancienne source locale NovelFrance a ete migree vers le format moderne du
  depot d'extensions et ajoutee a la selection construite par GitHub Actions.
- Package : `eu.kanade.tachiyomi.extension.fr.novelfrance`.
- Version publiee : `1.4.2` (`104002`).
- Le workflow GitHub Actions `34272261539` a construit puis publie l'extension
  avec succes le 9 septembre 2026.
- Le catalogue public contient desormais exactement 57 extensions.
- URL APK :
  `https://github.com/WendySBlanc/extensions/releases/download/d31ab8c/tachiyomi-fr.novelfrance-v1.4.2.apk`.
- L'APK publique a ete telechargee et verifiee avec `apksigner`. Son certificat
  SHA-256 correspond a la signature Nova :
  `f25a2ca5f93f7af24de9afff1a5afc2cf7600d3d87af03d8497a0ae4865de701`.
- Aucun nouveau build de l'application Nova n'a ete lance pour cette
  publication. Toujours demander l'accord de l'utilisateur avant le prochain
  build Nova.

## 44. Remplacements maintenus des extensions obsoletes

- Le commit source `5ca169baa` ajoute six remplacements encore maintenus au
  catalogue Nova et incremente leur revision pour provoquer une construction
  ciblee.
- Le workflow GitHub Actions `34309354820` a construit et publie les six APK
  avec succes le 9 septembre 2026 :
  `https://github.com/WendySBlanc/extensions-source/actions/runs/34309354820`.
- Remplacements publies : Manta Comics `1.6.12`, Webcomics `1.6.12`, Lava
  Scans `1.4.34`, Galaxy Manga `1.4.33`, Thunder Scans `1.4.34` et
  PhenixScans (unoriginal) `1.4.2`.
- Le catalogue Nova public contient desormais exactement 63 extensions.
- Les six URL d'APK pointent vers `WendySBlanc/extensions`. Les six APK ont ete
  telechargees puis controlees avec `apksigner`; elles portent toutes la bonne
  empreinte Nova `f25a2ca5...e701`.
- L'ancien Kun Manga ne doit pas etre remplace automatiquement par Kun Manga
  Online : le suivi Keiyoushi indique explicitement qu'il s'agit d'une source
  differente. Les anciens packages sans equivalent verifie restent donc
  exclus plutot que d'etre republies sous une identite trompeuse.
- Les deux anciennes variantes Asura non officielles sont deja couvertes par
  l'extension Asura Scans maintenue et publiee dans le catalogue Nova.
- Aucun nouveau build de l'application Nova n'a ete lance pour cette
  publication. Toujours demander l'accord de l'utilisateur avant le prochain
  build Nova.

## 45. Catalogue francais complet et verification des chapitres

- Le depot source a ete synchronise avec Keiyoushi jusqu'au commit amont
  `b658a2c225b3de6cc4143e87e4f735ac89e2b44d`.
- Les 58 modules de `src/fr`, NovelFrance comprise, sont maintenant tous
  selectionnes dans `.github/nova-installed-modules.txt`. Le catalogue public
  contient 98 extensions au total, dont exactement 58 paquets `fr`.
- Les revisions des 58 extensions FR ont ete incrementees pour que Nova propose
  leur migration vers la signature WendySBlanc.
- Corrections de sources publiees : types JSON et chapitres AralosBD, cartes et
  catalogue Dassou Scan, nouveau catalogue/chapitres/pages LanorTrad, puis
  navigation catalogue/chapitres/pages Lelscan.
- Versions finales corrigees : Dassou Scan `1.4.53`, LanorTrad `1.4.4` et
  Lelscan `1.4.4`.
- Le commit source final est
  `c33709e91c15ebea964318e1f5f425df359cadd1`. Le commit final de la branche
  publique `repo` est `b0b852a51b7ce3912c489180cf2d129671e0a273`.
- Verification en direct des artefacts publies : 40 sources ont retourne un
  catalogue et une liste de chapitres non vide. Quatorze autres demandent une
  resolution Cloudflare/AWS WAF dans la WebView Android. Trois services amont
  etaient indisponibles pendant le test : Mangas Scans (HTTP 525), Perf Scan
  (erreur TLS) et ScansFR (API HTTP 404).
- NovelFrance compile et son APK Android est signee, mais le banc JVM Miwayomi
  ne peut pas l'initialiser car son faux `android.graphics.Color` n'implemente
  pas `rgb`. Ce n'est pas une erreur observee dans Nova sur Android.
- Les 58 APK FR ont ete controlees avec `apksigner`. Leur certificat SHA-256
  correspond a la cle Nova
  `f25a2ca5f93f7af24de9afff1a5afc2cf7600d3d87af03d8497a0ae4865de701`.
- Rapport detaille :
  `extension-inventory/fr-extension-smoke-2026-09-09.json`.
- URL du depot a conserver dans Nova :
  `https://raw.githubusercontent.com/WendySBlanc/extensions/repo/index.pb`.
- Aucun build de l'application Nova n'a ete lance. Toujours demander l'accord
  de l'utilisateur avant le prochain build Nova.

## 46. Recherche de migration de source

- La migration ne depend plus uniquement du titre complet exact. Elle essaie
  aussi le titre sans texte entre parentheses, une version sans ponctuation ni
  accents, les mots distinctifs et les principaux mots du titre.
- La recherche globale est limitee aux quatre meilleures variantes pour rester
  rapide. Quand une source precise est ouverte, les variantes supplementaires
  sont essayees jusqu'au premier resultat, puis cette requete est conservee
  pour les pages suivantes.
- Le changement du filtre de sources met maintenant son etat a jour avant de
  relancer la recherche, ce qui evite une recherche faite avec l'ancien filtre.
- Fichiers principaux : `MigrationSearchQueries.kt`,
  `MigrationSourceSearchScreenModel.kt`, `MigrateSearchScreenModel.kt`,
  `MigrateSourceSearchScreen.kt` et `SearchScreenModel.kt`.
- Verification : compilation Preview reussie et deux tests unitaires de
  variantes de titres reussis avec `--no-problems-report`.
- Aucun APK Nova n'a ete construit. Toujours demander l'accord de
  l'utilisateur avant le prochain build Nova.

## 47. Build Nova avec correction de migration

- Build Preview termine avec succes le 9 septembre 2026 apres accord de
  l'utilisateur.
- APK : `app/build/outputs/apk/preview/Nova-v101-migration-source.apk`.
- Package : `com.nova.manhwa.debug`.
- Version : `101` (`1.14.0-nova-fast-data-1`).
- Taille : `117437161` octets.
- SHA-256 :
  `BE8DE054DDAFD90F8EC9301325775EEF09F1462324368BCB3BFB148A73055EFE`.
- Signature APK v2 verifiee avec `apksigner`.
- Lien mobile verifie HTTP 200 : `https://gofile.io/d/S0dyVuD4`.
- Nouveau lien mobile apres expiration du precedent, verifie HTTP 200 :
  `https://gofile.io/d/Xu9lXbTz`.
- Les tests unitaires de variantes de recherche ont reussi avant le build.
- Toujours demander l'accord de l'utilisateur avant le prochain build Nova.

## 48. Etat de lecture global, migration Android et barre du lecteur

- La barre des chapitres regroupe maintenant les actions globales dans un seul
  menu : `Tout marquer comme lu` et `Tout marquer comme non lu`.
- L'action `Tout marquer comme non lu` traite aussi les chapitres en cours et
  remet leur progression a zero via `SetReadStatus`.
- Le lecteur LNReader n'affiche plus le rail vertical JavaScript ni la barre de
  defilement verticale Android. La barre horizontale Nova reste l'unique barre
  de progression du lecteur.
- Le journal `komikku_crash_logs (2).txt` montre un crash Android dans
  `MigrationSearchQueries` : Android ICU refusait l'accolade fermante non
  echappee du motif de nettoyage. Le motif echappe maintenant explicitement les
  crochets et les accolades.
- Les anciens crashs `okhttp3.zstd.Zstd` du meme journal proviennent d'une APK
  anterieure. L'APK Preview actuellement presente contient bien la classe
  `okhttp3.zstd.Zstd` et les bibliotheques natives Zstandard.
- La compilation Preview n'a pas pu etre terminee le 10 septembre 2026 car la
  memoire virtuelle Windows etait presque totalement consommee par Firefox et
  OneDrive. Aucun programme utilisateur n'a ete ferme et aucun fichier Gradle
  n'a ete supprime.
- Le lien MobiDrive fourni ouvre le fichier partage
  `4_5841701485917773544.txt`; il ne fournit pas un dossier d'envoi pour l'APK.
- APK Preview construit le 8 octobre 2026 :
  `app/build/outputs/apk/preview/Nova-v101-resumable-downloads.apk`.
- Package : `com.nova.manhwa.debug`.
- Version : `101` (`1.14.0-nova-fast-data-1`).
- Taille : `117482053` octets.
- SHA-256 :
  `15FDA1E2DAAAF0A185A42A93823E447831DC4930131D43CA7AA49929E3EDB5F3`.
- Signature APK v2 verifiee, certificat Preview Android Debug conserve.
- Lien mobile permanent : `https://files.catbox.moe/61i84f.apk`.
- Le lien distant a ete controle avec HTTP 206 et la taille totale exacte dans
  `Content-Range`.
- Toujours demander l'accord de l'utilisateur avant le prochain build Nova.

## 49. Build lecture globale et lecteur horizontal

- Build Preview termine avec succes le 10 septembre 2026 apres accord de
  l'utilisateur.
- Le journal `komikku_crash_logs (2).txt` contient exactement 15 crashs : 14
  `NoClassDefFoundError` pour `okhttp3.zstd.Zstd` dans Kagane/Asura Scans et un
  `PatternSyntaxException` dans la recherche de migration.
- Le nouvel APK contient bien `okhttp3.zstd.Zstd`. Le motif de migration est
  compatible Android et les deux tests de `MigrationSearchQueriesTest` passent
  sans erreur.
- L'action globale des chapitres propose maintenant `Tout marquer comme lu` et
  `Tout marquer comme non lu` dans un menu unique.
- Le lecteur conserve uniquement la progression horizontale Nova. Le composant
  JavaScript `ToolWrapper()` qui ajoutait le second rail vertical est absent de
  l'asset emballe, et la barre verticale native du WebView est desactivee.
- APK :
  `app/build/outputs/apk/preview/Nova-v101-non-lu-scroll-horizontal.apk`.
- Package : `com.nova.manhwa.debug`.
- Version : `101` (`1.14.0-nova-fast-data-1`).
- Taille : `117420773` octets.
- SHA-256 :
  `BAE09711F2238589275A211A182B4549628003E18E5905634B0A482766E5D921`.
- Signature APK v2 verifiee.
- Lien mobile direct : `https://files.catbox.moe/f5v8oj.apk`.
- Le lien distant a ete controle avec HTTP 206, une taille totale de
  `117420773` octets et la signature ZIP `PK`.
- Toujours demander l'accord de l'utilisateur avant le prochain build Nova.

## 50. Installation et desinstallation de toutes les extensions

- La page Extensions propose maintenant `Tout installer` pour installer toutes
  les extensions disponibles, une par une, sans empiler les fenetres Android.
- La progression globale est affichee dans le menu et l'operation peut etre
  annulee pendant son execution.
- La page propose aussi `Tout desinstaller`, avec une confirmation destructive
  indiquant le nombre d'extensions concernees.
- Les extensions privees sont supprimees directement. Les APK Android sont
  desinstallees successivement au moyen de la confirmation systeme; annuler une
  confirmation interrompt la suite.
- Les listes sont dedoublonnees par paquet et signature afin d'eviter de traiter
  deux fois la meme extension provenant de plusieurs depots.
- Les nouveaux textes sont disponibles en francais et dans la langue de base.
- Verification : `:app:compilePreviewKotlin` reussit apres nettoyage des caches
  Kotlin temporaires corrompus de `data` et `source-local`; `git diff --check`
  ne signale aucune erreur.
- Aucun nouvel APK Nova n'a ete construit. Toujours demander l'accord de
  l'utilisateur avant le prochain build Nova.

## 51. Build gestion globale des extensions

- Build Preview termine avec succes le 14 septembre 2026 apres accord de
  l'utilisateur.
- APK :
  `app/build/outputs/apk/preview/Nova-v101-extensions-globales.apk`.
- Package : `com.nova.manhwa.debug`.
- Version : `101` (`1.14.0-nova-fast-data-1`).
- Taille : `117441633` octets.
- SHA-256 :
  `BCD13037F19F88ACC2E88960ED6A9F4327A5962ABBD1483CFE5A73374133576E`.
- Signature APK v2 verifiee.
- Lien mobile controle avec HTTP 200 : `https://gofile.io/d/9SVMVoev`.
- Toujours demander l'accord de l'utilisateur avant le prochain build Nova.

## 52. Selection multiple des extensions

- L'ancien choix direct `Tout installer` / `Tout desinstaller` est remplace par
  un mode `Selectionner des extensions`, afin d'eviter les options globales en
  double.
- Un appui long sur une extension entre aussi directement en mode selection.
- Chaque ligne selectionnee affiche un fond distinct et une case cochee.
- La barre d'action affiche le nombre choisi et permet de selectionner toutes
  les extensions actuellement visibles.
- L'action d'installation traite les extensions disponibles et les mises a
  jour installees selectionnees, une par une. L'action de desinstallation ne
  traite que les extensions installees ou non approuvees selectionnees.
- Une confirmation indique le nombre exact avant l'installation ou la
  desinstallation. Les APK multi-sources sont dedoublonnees.
- Le retour Android ou la croix ferme le mode selection et vide la selection.
- Le meme mode fonctionne dans l'onglet Extensions et dans l'ecran Extensions
  ouvert separement.
- Verification : XML anglais/francais valide, `git diff --check` propre et
  `:app:compilePreviewKotlin` reussit.
- L'APK de la section 51 ne contient pas encore cette selection. Toujours
  demander l'accord de l'utilisateur avant le prochain build Nova.

## 53. Build extensions avec selection

- Build Preview termine avec succes le 14 septembre 2026 apres accord de
  l'utilisateur.
- APK : `app/build/outputs/apk/preview/Nova-v101-extensions-selection.apk`.
- Package : `com.nova.manhwa.debug`.
- Version : `101` (`1.14.0-nova-fast-data-1`).
- Taille : `117446089` octets.
- SHA-256 :
  `C26DE4A498DE1BDAF79C2205B2F32A675707944DBFE47CE67A280734A2E30BAF`.
- Signature APK v2 verifiee.
- Lien mobile controle avec HTTP 200 : `https://gofile.io/d/oFsP9exi`.
- Toujours demander l'accord de l'utilisateur avant le prochain build Nova.

## 54. WebView et partage visibles sur la page manga

- Les actions WebView et Partager ne sont plus cachees dans le menu a trois
  points de la banniere manga.
- Deux boutons distincts sont maintenant affiches directement en haut a droite
  de la banniere lorsqu'une URL de source est disponible.
- Les descriptions d'accessibilite reutilisent les traductions existantes.
- La detection d'une source HTTP est recalculee lorsque la source de la page
  change, ce qui evite de conserver un ancien etat apres une migration.
- Verification : `git diff --check` propre et `:app:compilePreviewKotlin`
  reussit.
- Aucun nouvel APK n'a ete construit. Toujours demander l'accord de
  l'utilisateur avant le prochain build Nova.

## 55. Crash `runBlockingK` des extensions

- Le rapport de crash du 18 septembre 2026 a 22:04:37 signale un
  `NoSuchMethodError` sur `kotlinx.coroutines.BuildersKt.runBlockingK(...)`
  depuis la chaine OkHttp, avant `UserAgentInterceptor` et
  `UncaughtExceptionInterceptor`.
- Cause confirmee : les extensions Nova sont compilees avec
  `kotlinx-coroutines 1.11.0`, alors que l'application embarquait encore la
  version `1.10.2`.
- La comparaison des JAR avec `javap` confirme que `runBlockingK(...)` est
  present en `1.11.0` mais absent en `1.10.2`.
- Le BOM coroutines de Nova est passe de `1.10.2` a `1.11.0`. La resolution
  `previewRuntimeClasspath` selectionne maintenant `core`, `android` et
  `guava` en `1.11.0`.
- Le variant Preview n'est pas minifie et les API coroutines sont deja
  conservees par les regles ProGuard; aucune regle R8 supplementaire n'est
  necessaire.
- Verification : `:app:compilePreviewKotlin` reussit.
- Aucun nouvel APK n'a ete construit. Toujours demander l'accord de
  l'utilisateur avant le prochain build Nova.

## 56. Build correction `runBlockingK`

- Build Preview termine avec succes le 19 septembre 2026 apres accord de
  l'utilisateur.
- APK :
  `app/build/outputs/apk/preview/Nova-v101-coroutines-1.11.0.apk`.
- Package : `com.nova.manhwa.debug`.
- Version : `101` (`1.14.0-nova-fast-data-1`).
- Taille : `117478849` octets.
- SHA-256 :
  `8DC697FF939E610B30C6F03392E283F00740186CA37E6E872EF30A8D5C46A8DD`.
- Signature APK v2 verifiee.
- La chaine `runBlockingK` est presente dans les DEX emballes.
- Lien mobile direct : `https://files.catbox.moe/4xfip2.apk`.
- Le lien distant a ete controle avec HTTP 206, une taille totale de
  `117478849` octets et la signature ZIP `PK`.
- Toujours demander l'accord de l'utilisateur avant le prochain build Nova.

## 57. Erreur `getClass()` de Scan-Manga

- La source concernee est Scan-Manga (`eu.kanade.tachiyomi.extension.fr.scanmanga`)
  sur `https://m.scan-manga.com`, et non Manga-Scantrad.
- L'extension appelait `it.javaClass` pour reperer et retirer
  `CloudflareInterceptor` de la chaine OkHttp. Ce chemin correspondait a
  l'erreur Android `Object.getClass()` signalee.
- Le client de lecture conserve desormais cache, DNS et cookies, mais vide les
  intercepteurs applicatifs sans introspection de classe. Les en-tetes de la
  source fournissent deja le User-Agent necessaire.
- Les URL de recherche et d'images sont maintenant construites depuis le
  domaine racine `scan-manga.com`, au lieu des domaines invalides
  `bqj.m.scan-manga.com` et `static.m.scan-manga.com`.
- Les reponses HTTP en erreur et les pages sans donnees de chapitre produisent
  maintenant un message explicite au lieu d'un plantage de parsing.
- Version de l'extension passee a `1.4.25`.
- Verification : `:src:fr:scanmanga:compileDebugKotlin` reussit et
  `git diff --check` est propre.
- Le 28 septembre 2026, `m.scan-manga.com`, `www.scan-manga.com` et
  `scan-manga.com` repondent tous HTTP 404. L'extension ne peut pas recuperer
  de catalogue tant que le serveur du site ne revient pas.
- Aucun APK d'extension n'a encore ete construit ou publie.

## 58. Depot Nova des extensions automatise

- Le depot source `WendySBlanc/extensions-source` est synchronise chaque jour
  avec Keiyoushi a 03:43 UTC, ainsi que manuellement avec `workflow_dispatch`.
- La CI construit seulement les modules presents dans
  `.github/nova-installed-modules.txt`, par groupes de 16 et jusqu'a huit
  groupes en parallele.
- Le build utilise Java 25 et la base amont courante pour calculer les modules
  modifies.
- Le run GitHub Actions `36973283209` a construit et publie avec succes les 95
  extensions selectionnees.
- Scan-Manga `1.4.25` a ete publie dans le depot Nova apres la correction de la
  section 57.
- URL du depot a conserver dans Nova :
  `https://raw.githubusercontent.com/WendySBlanc/extensions/repo/index.pb`.

## 59. Recuperation des chapitres Japscan

- L'extension officielle migree vers l'API 1.6 contenait deja le nouveau
  lecteur WebView de Keiyoushi, mais les pages de titres et de chapitres sont
  maintenant protegees par un challenge Cloudflare interactif.
- Un client de sonde sans `CloudflareInterceptor` detecte immediatement la
  protection, sans attendre le delai du challenge WebView masque.
- Quand une fiche est protegee, la mise a jour de bibliotheque recupere les
  liens recents exposes par l'accueil public de Japscan et les fusionne avec
  les chapitres locaux sans supprimer l'historique.
- Le flux est analyse une fois puis conserve dix minutes. Le controle reel du
  2 octobre 2026 a trouve 520 chapitres uniques pour 324 series.
- La lecture detecte maintenant aussi bien Cloudflare que le puzzle Japscan,
  tente d'abord une verification WebView automatique, puis ouvre la WebView
  visible si une validation utilisateur est necessaire.
- Les cookies tiers sont acceptes dans le WebView du lecteur afin de conserver
  la validation de securite.
- Version publiee : Japscan `1.6.1` (`versionCode 106001`), commit source
  `e15c2cce5`.
- Le run GitHub Actions `37037358803` a reussi le build et la publication.
- APK direct :
  `https://github.com/WendySBlanc/extensions/releases/download/e15c2cc/tachiyomi-fr.japscan-v1.6.1.apk`.
- Verification locale : `:src:fr:japscan:assembleDebug` reussit. Le `index.pb`
  distant repond HTTP 200 et contient la version `1.6.1`.
- Aucun nouvel APK Nova n'a ete construit. Toujours demander l'accord de
  l'utilisateur avant le prochain build Nova.

## 60. Outil de telechargement des chapitres

- Le menu de telechargement de la page manga affiche maintenant le nombre de
  chapitres concernes pour `suivant`, `5 suivants`, `non lus` et `tous`.
- Une action sans chapitre disponible est grisee. Un message confirme le
  nombre de telechargements lances, ou indique qu'il n'y a rien a telecharger.
- Les chapitres non lus en erreur sont inclus dans les actions de reprise.
- Une demande de telechargement relance maintenant une file arretee, en pause
  ou composee de chapitres en erreur meme si cette file n'est pas vide.
- La verification avant mise en file utilise les identifiants du manga et du
  chapitre, puis controle le titre original et le titre affiche. Cela evite de
  retelecharger les chapitres restaures ou ranges sous un ancien titre.
- Verification : `:app:compilePreviewKotlin --no-problems-report` reussit.
- Aucun nouvel APK Nova n'a ete construit. Toujours demander l'accord de
  l'utilisateur avant le prochain build Nova.

## 61. Verification de la file de telechargement

- Le worker de telechargement ne reste plus bloque dans la surveillance reseau
  et n'effectue plus de boucle active sans attente.
- Une perte de reseau ou de Wi-Fi termine maintenant proprement le worker et
  permet une reprise ulterieure depuis la file.
- Le bouton Reprendre ne fait plus de lecture WorkManager bloquante sur le
  thread de l'interface.
- Chaque ligne indique maintenant `En attente`, `Preparation`, `Erreur` ou
  `Termine`, tout en conservant le compteur de pages pendant le telechargement.
- Une action `Reessayer` est disponible uniquement pour le chapitre en erreur
  et le replace en tete de file.
- Les mises a jour de progression recherchent le holder visible par chapitre,
  sans dependre d'un identifiant RecyclerView tronque.
- Verification : `:app:compilePreviewKotlin --no-problems-report` reussit.

## 62. Build file de telechargement

- Build Preview termine avec succes le 2 octobre 2026 apres accord de
  l'utilisateur.
- APK local :
  `app/build/outputs/apk/preview/Nova-v101-download-queue.apk`.
- Package : `com.nova.manhwa.debug`.
- Version : `101` (`1.14.0-nova-fast-data-1`).
- Taille : `117482053` octets.
- SHA-256 :
  `BB3BAB0BF7AC8019B87C78422C0EC4AFCEC83D90E7B5F0A254C06D41A0502F50`.
- Signature APK v2 verifiee, certificat Preview Android Debug conserve.
- Lien mobile temporaire 72 h : `https://litter.catbox.moe/vrqgxx.apk`.
- Le lien distant a ete controle avec HTTP 200, la taille exacte et les quatre
  premiers octets ZIP `PK`.
- Toujours demander l'accord de l'utilisateur avant le prochain build Nova.

## 63. Acces Epsilon Scan et Soft Epsilon Scan

- Le 2 octobre 2026, `epsilonsoft.to` et `epsilonscan.to` renvoient un
  challenge Cloudflare HTTP 403 avec l'en-tete `cf-mitigated: challenge`.
- Avec le User-Agent Android WebView contenant `wv`, le site affiche
  explicitement `Ouvrez dans votre navigateur` et refuse le WebView integre.
- Le multisource Pam fournit maintenant un User-Agent Chrome mobile sans la
  marque WebView dans `headersBuilder()`. Ce meme en-tete est donc utilise par
  les requetes de l'extension, le resoluteur Cloudflare et le WebView visible
  de Nova.
- Versions publiees : Soft Epsilon Scan `1.4.59` et Epsilon Scan `1.4.58`.
- Commit source : `f1e9104c3` ; run GitHub Actions `37094728369` termine avec
  succes.
- APK Soft Epsilon Scan :
  `https://github.com/WendySBlanc/extensions/releases/download/f1e9104/tachiyomi-fr.softepsilonscan-v1.4.59.apk`.
- APK Epsilon Scan :
  `https://github.com/WendySBlanc/extensions/releases/download/f1e9104/tachiyomi-fr.epsilonscan-v1.4.58.apk`.
- L'index Nova `https://raw.githubusercontent.com/WendySBlanc/extensions/repo/index.pb`
  et les deux APK distants ont ete verifies en HTTP 200.
- Les deux extensions ont ete compilees localement avec
  `assembleRelease`. Aucun APK Nova n'a ete construit.

## 64. Reprise fiable des telechargements

- Le 7 octobre 2026, le telechargeur d'images a ete aligne sur le mecanisme de
  reprise introduit dans Mihon 0.20.2.
- Les fichiers de page `.tmp` ne sont plus supprimes avant chaque tentative ou
  au redemarrage d'un chapitre. Une requete HTTP `Range` reprend maintenant au
  dernier octet ecrit lorsque la source utilise le telechargement standard.
- Une reponse HTTP 206 est ajoutee au fichier partiel uniquement si son
  `Content-Range` commence exactement a l'octet attendu. Une reponse HTTP 200
  remplace le fichier, et une reponse HTTP 416 supprime le partiel invalide puis
  relance la page depuis zero.
- La progression inclut les octets deja presents. Un `.tmp` n'est jamais compte
  comme une page terminee et une image trouvee dans le cache le remplace
  proprement.
- Les sources qui redefinissent leur propre recuperation d'image conservent ce
  chemin specialise et repartent de zero. Cela evite de contourner une
  authentification, un dechiffrement ou un traitement proprietaire de la source.
- Le mode economie de donnees repart aussi de zero, car l'image du proxy peut
  etre differente de l'image d'origine et les deux contenus ne doivent jamais
  etre melanges.
- Verification : `:app:compileDebugKotlin --no-problems-report` reussit. Les
  controles Spotless de `core:common` et `source-api` reussissent; le controle
  global de l'application reste bloque par des fichiers d'interface deja non
  formates et sans rapport avec cette modification.
- Aucun nouvel APK Nova n'a ete construit. Toujours demander l'accord de
  l'utilisateur avant le prochain build Nova.

## 65. Compatibilite SChapter memo des extensions TachiyomiX 1.6

- Le 18 octobre 2026, l'erreur Blossom Scans
  `NoSuchMethodError: SChapter.setMemo(JsonObject)` a ete identifiee comme une
  incompatibilite entre les extensions TachiyomiX 1.6 acceptees par Nova et le
  modele `SChapter` encore incomplet dans l'application.
- `SChapter`, les modeles de domaine et les modeles de base de donnees gerent
  maintenant `memo` sous forme de `JsonObject`. L'ABI compilee expose bien
  `setMemo(Lkotlinx/serialization/json/JsonObject;)V`, attendue par l'extension.
- La migration SQLDelight `45.sqm` ajoute la colonne `chapters.memo` en BLOB
  non nul avec `{}` comme valeur par defaut. Les migrations 28 a 45 ont ete
  appliquees avec succes sur une copie de la base de reference via le binaire
  SQLite Android; les anciennes lignes obtiennent bien un objet JSON vide.
- Les insertions, mises a jour, conversions et sauvegardes/restaurations de
  chapitres conservent maintenant `memo`. Le champ de sauvegarde protobuf 13
  est utilise avec une valeur par defaut compatible avec les anciennes
  sauvegardes.
- La synchronisation compare et recopie `memo`. Les chapitres Blossom deja
  presents avec un memo vide sont donc corriges lors de la prochaine
  actualisation de la serie, sans devoir supprimer le manga de la bibliotheque.
- Verification : test unitaire cible `ShouldUpdateDbChapterTest` reussi,
  `:source-api:spotlessCheck` et `:data:spotlessCheck` reussis,
  `:app:compileDebugKotlin --no-problems-report` reussi. `git diff --check` ne
  signale aucune erreur.
- Le verificateur Gradle SQLDelight n'a pas pu extraire sa DLL SQLite dans
  `C:\Windows` a cause d'un refus d'acces local. La chaine de migrations a ete
  verifiee a la place avec le binaire SQLite fourni par le SDK Android.
- Build Preview lance apres accord de l'utilisateur et termine avec succes le
  18 octobre 2026.
- APK local :
  `app/build/outputs/apk/preview/Nova-v101-blossom-memo-fix.apk`.
- Package : `com.nova.manhwa.debug`; version : `101`
  (`1.14.0-nova-fast-data-1`); taille : `117482053` octets.
- SHA-256 :
  `CBD1CCEF5864512F9BE7850ABF9DDA6CCAC4183F25846FE5190A41DF7862BD6B`.
- Signature APK v2 verifiee avec le certificat Preview Android Debug habituel.
- Lien mobile : `https://files.catbox.moe/ie9q45.apk`. Le serveur distant a
  ete controle avec HTTP 206, la taille exacte et les quatre premiers octets
  ZIP `PK`.
- Toujours demander l'accord de l'utilisateur avant le prochain build Nova.

## 66. Mise a jour complete du depot d'extensions Nova

- Le 26 octobre 2026, le catalogue source a ete synchronise avec Keiyoushi
  jusqu'au commit `b2f431805`, puis adapte au depot Nova.
- Les nouveaux modules FR `aniverse`, `banchanscan` et `mabrute` ont ete
  ajoutes. Les modules supprimes ou devenus indisponibles ont ete retires de la
  selection Nova.
- NovelFrance a ete migre vers l'API d'extensions 1.6 et publie en version
  `1.6.4`. Scan-Manga utilise egalement l'API 1.6 et est publie en version
  `1.6.26` avec le correctif evitant l'erreur `getClass()`.
- Le script de publication retrouve maintenant les APK et JAR par le nom de
  package Android et la version exacte. Cela prend correctement en charge les
  modules renommes Hive Scans et Vortex Scans et evite de publier un ancien
  artefact reste dans un dossier de build.
- Les 89 extensions selectionnees ont ete compilees en Release. Les 89 paires
  APK/JAR sont presentes et tous les APK ont ete verifies avec le certificat
  Nova `f25a2ca5f93f7af24de9afff1a5afc2cf7600d3d87af03d8497a0ae4865de701`.
- GitHub refusait le lancement manuel du workflow avec HTTP 422 en indiquant
  que Actions etait desactive pour le depot. La publication a donc ete faite
  localement avec le meme script et les memes controles que le workflow.
- Commit final du depot source : `4e9c2bd48`. Commit de la branche publique
  `repo` : `7ca3209`. Release publique : `4e9c2bd` avec 128 nouveaux assets.
- L'index public contient 89 extensions, sans URL APK/JAR manquante :
  `https://raw.githubusercontent.com/WendySBlanc/extensions/repo/index.pb`.
- L'index public et les APK NovelFrance, Scan-Manga et Hive Scans ont ete
  telecharges depuis GitHub apres publication, puis controles avec succes. La
  copie temporaire de la cle de signature a ete supprimee du depot de travail.
- Aucun APK Nova n'a ete construit pendant cette mise a jour d'extensions.
  Toujours demander l'accord de l'utilisateur avant le prochain build Nova.

