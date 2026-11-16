# ZitounaFlow — Backlog Scrum

Application Android (Java) de gestion des passages dans une huilerie, avec Firebase
(Authentication + Cloud Firestore) et une extension IA (TensorFlow Lite) pour l'analyse
de la qualité des olives.

- **Product Owner** : encadrant / huilerie
- **Équipe** : Zeineb Bahloul
- **Durée d'un sprint** : 1 semaine
- **Definition of Done** : code compilable dans Android Studio, fonctionnalité testée sur
  émulateur avec Firebase, écrans conformes aux maquettes, règles Firestore à jour,
  commit poussé sur GitHub.

Priorités : **M** = Must, **S** = Should, **C** = Could. Estimation en points (suite de Fibonacci).

---

## Sprint 1 — Socle technique, authentification et profil

**Objectif** : un utilisateur (agriculteur ou gestionnaire) peut créer un compte, se connecter,
réinitialiser son mot de passe, gérer son profil, et arrive sur l'espace propre à son rôle.

| ID | User story | BF | Prio | Pts |
|----|-----------|----|------|-----|
| US-01 | En tant que **développeuse**, je veux configurer Firebase (Auth + Firestore), le thème graphique des maquettes et l'architecture du projet, afin de pouvoir développer les fonctionnalités. | — | M | 3 |
| US-02 | En tant que **visiteur**, je veux voir un écran d'accueil présentant ZitounaFlow, afin de choisir de m'inscrire ou de me connecter. | — | S | 2 |
| US-03 | En tant que **visiteur**, je veux créer un compte (nom, prénom, téléphone, e-mail, mot de passe) en choisissant mon rôle, afin d'accéder à l'application. | BF-01 | M | 5 |
| US-04 | En tant qu'**utilisateur**, je veux me connecter et me déconnecter, afin d'accéder à mon espace de façon sécurisée. | BF-02 | M | 3 |
| US-05 | En tant qu'**utilisateur**, je veux réinitialiser mon mot de passe par e-mail, afin de récupérer mon compte. | BF-03 | M | 2 |
| US-06 | En tant qu'**utilisateur**, je veux consulter et modifier mon profil, afin de garder mes informations à jour. | BF-04 | M | 3 |
| US-07 | En tant qu'**utilisateur**, je veux être redirigé vers l'espace de mon rôle (agriculteur ou gestionnaire) avec sa barre de navigation, afin d'accéder aux bons écrans. | — | M | 3 |

**Critères d'acceptation principaux**
- US-03 : e-mail valide, téléphone à 8 chiffres, mot de passe ≥ 6 caractères et confirmé ;
  l'agriculteur saisit son exploitation et son adresse, le gestionnaire son matricule ;
  le profil est enregistré dans `utilisateurs/{uid}` avec le rôle et la date de création.
- US-04 : message clair en cas d'identifiants incorrects ; la session reste ouverte au
  redémarrage ; la déconnexion ramène à l'écran d'accueil.
- US-05 : un e-mail de réinitialisation Firebase est envoyé.
- US-06 : l'e-mail et le rôle ne sont pas modifiables ; les modifications sont persistées.
- US-07 : agriculteur → Accueil / RDV / Lots / Profil ; gestionnaire → Console / RDV / Lots / Stats / Profil.

**Compléments ajoutés pendant le sprint 2 (revue des maquettes)** : bouton de déconnexion
et cloche des notifications dans l'en-tête des tableaux de bord, option « Mémoriser ce
terminal » à la connexion, bannière « N demandes de pressage à valider » dans la console.

---

## Sprint 2 — Rendez-vous

**Objectif** : l'agriculteur demande un créneau de pressage, le gestionnaire le confirme ou le refuse.

| ID | User story | BF | Prio | Pts |
|----|-----------|----|------|-----|
| US-08 | En tant qu'**agriculteur**, je veux demander un rendez-vous (date, créneau, variété, quantité estimée), afin de réserver mon passage. | BF-10 | M | 5 |
| US-09 | En tant qu'**agriculteur**, je veux consulter l'état de mes rendez-vous et annuler une demande en attente. | BF-11 | M | 3 |
| US-10 | En tant que **gestionnaire**, je veux consulter les demandes (par jour) et les confirmer ou les refuser avec un motif. | BF-20 | M | 5 |
| US-11 | En tant qu'**utilisateur**, je veux recevoir une notification dans l'application quand un rendez-vous change d'état. | BF-05 | S | 3 |

**Critères d'acceptation principaux**
- US-08 : choix d'un jour parmi les 14 prochains, d'un créneau (08:00-10:00, 10:30-12:30,
  14:00-16:00, 16:30-18:30) avec places restantes (4 par créneau, créneaux passés grisés),
  de la variété et de la quantité (50 à 20 000 kg) ; estimation du nombre de caisses, de la
  durée de traitement (capacité machine 1 000 kg/h) et de l'huile attendue (18 à 22 %) ;
  la demande est créée avec le statut « En attente de confirmation ». Une transaction
  Firestore empêche la surréservation d'un créneau.
- US-09 : filtres À venir / En attente / Historique / Tous, mise à jour en temps réel ;
  annulation possible tant que le rendez-vous est actif et pas encore passé (la place est libérée).
- US-10 : navigation jour par jour ou via un calendrier ; capacité du quai (22 000 kg/jour)
  avec kg confirmés et en attente ; refus impossible sans motif.
- US-11 : notification enregistrée dans `notifications` à la confirmation et au refus ;
  badge du nombre de non lues sur la cloche ; les notifications sont marquées lues à l'ouverture.

---

## Sprint 3 — Lots et file d'attente

**Objectif** : le gestionnaire enregistre les lots à l'arrivée et pilote la file ; l'agriculteur suit ses lots.

| ID | User story | BF | Prio | Pts |
|----|-----------|----|------|-----|
| US-12 | En tant que **gestionnaire**, je veux enregistrer un lot à l'arrivée (propriétaire, poids, variété, date de réception), afin qu'il rejoigne la file. | BF-21 | M | 5 |
| US-13 | En tant que **gestionnaire**, je veux consulter et réordonner la file d'attente et prioriser un lot. | BF-23 | M | 5 |
| US-14 | En tant que **gestionnaire**, je veux faire avancer le statut d'un lot (cycle strictement séquentiel), avec historique des changements. | BF-24 | M | 3 |
| US-15 | En tant qu'**agriculteur**, je veux suivre mes lots et leur statut en temps réel. | BF-12 | M | 3 |
| US-16 | En tant qu'**agriculteur**, je veux connaître ma position dans la file et le temps d'attente estimé (poids des lots précédents / capacité machine). | BF-13 | M | 3 |

**Critères d'acceptation principaux**
- US-12 : producteur choisi parmi les agriculteurs inscrits, depuis un rendez-vous confirmé du jour
  (pré-remplissage) ou saisi pour un apport sans compte ; pesée brut − tare (proposée selon le
  contenant) = poids net ; numéro séquentiel `LOT-001`, `LOT-002`… attribué par transaction ;
  statut « Réceptionné », ajout en fin de file, historique et notification de l'agriculteur.
- US-13 : file ordonnée (lots en traitement puis lots en attente) ; boutons ↑ ↓ pour réordonner,
  ★ pour placer un lot en tête ; filtres Tous / En traitement / En attente.
- US-14 : passage Réceptionné → En attente → En traitement uniquement dans l'ordre (contrôle en
  transaction) ; chaque changement est tracé dans `lots/{id}/historique` et notifié.
  Le passage à « Prêt à récupérer » se fait avec la saisie de l'huile (sprint 4).
- US-15 : liste « Mes Lots » en temps réel, détail avec les 4 étapes horodatées et l'historique ;
  tableau de bord avec le suivi en direct (avancement du traitement) et les apports du jour.
- US-16 : `FileAttenteCalculator` : attente = somme des poids des lots précédents (et du temps
  restant du lot en traitement) / 1 000 kg/h ; position et attente enregistrées sur chaque lot
  après chaque modification de la file ; test unitaire `FileAttenteCalculatorTest`.

**Correctifs après la revue du sprint 3**
- Les gestionnaires reçoivent maintenant des notifications : nouvelle demande de rendez-vous
  et annulation par un agriculteur (destinataire collectif `GESTIONNAIRES`).
- La carte de chaque lot affiche sa phase (« Étape 2/4 : En attente → prochaine : … »).
- US-17 et US-18 (sprint 4) avancées pour débloquer la suite du cycle : saisie de l'huile
  (rendement calculé, passage à « Prêt à récupérer ») et enregistrement de la récupération ;
  filtre « Prêts » dans la file du moulin.

---

## Sprint 4 — Production d'huile, récupération et historique

| ID | User story | BF | Prio | Pts |
|----|-----------|----|------|-----|
| US-17 ✅ | En tant que **gestionnaire**, je veux saisir la quantité d'huile produite ; le rendement est calculé (L / kg × 100) et le lot passe à « Prêt à récupérer ». | BF-25 | M | 3 |
| US-18 ✅ | En tant que **gestionnaire**, je veux enregistrer la date de récupération effective d'un lot. | BF-26 | M | 2 |
| US-19 ✅ | En tant qu'**agriculteur**, je veux consulter la quantité d'huile obtenue, le rendement et la date de récupération. | BF-14 | M | 2 |
| US-20 ✅ | En tant qu'**agriculteur**, je veux consulter l'historique de mes passages et le bilan de campagne. | BF-15 | M | 3 |
| US-21 ✅ | En tant que **gestionnaire**, je veux consulter l'historique global avec recherche et filtres. | BF-27 | M | 5 |

**Critères d'acceptation principaux**
- US-17 : saisie des litres quand le lot est en traitement, rendement affiché en direct
  (L / kg × 100), contrôle de cohérence (≤ 40 %), passage à « Prêt à récupérer », notification.
- US-18 : date de récupération effective enregistrée une seule fois, notification.
- US-19 : huile, rendement et date de récupération visibles sur la carte et le détail du lot.
- US-20 : « Mes Campagnes & Lots » (maquette 6) : recherche (n° de lot, variété, parcelle,
  sans accents), campagnes de septembre à août, bilan de la campagne (olives pesées, huile
  extraite, rendement moyen, nombre de lots), filtres Tous / En cours / Terminés, export CSV.
- US-21 : historique global (bouton « Historique » de la file et de la console) : recherche
  (n° de lot, producteur, téléphone, variété), filtres de statut, de période et de variété,
  totaux de la sélection, export CSV. Tests unitaires `LotFilterTest`.

---

## Sprint 5 — Statistiques et notifications

| ID | User story | BF | Prio | Pts |
|----|-----------|----|------|-----|
| US-22 ✅ | En tant que **gestionnaire**, je veux voir les statistiques (lots par jour, poids total, huile produite, rendement moyen, temps d'attente moyen, répartition de la qualité) sur une période. | BF-28 | M | 8 |
| US-23 ✅ | En tant que **gestionnaire**, je veux un tableau de bord (console) avec les indicateurs du jour. | BF-28 | S | 3 |
| US-24 ✅ | En tant qu'**agriculteur**, je veux être notifié quand mon lot change de statut ou est prêt à récupérer, et consulter mes notifications. | BF-05 | M | 5 |

**Critères d'acceptation principaux**
- US-22 : onglet Stats (maquette « Statistiques & Rendement ») ; périodes Aujourd'hui / 7 jours /
  30 jours / Campagne ; huile produite, olives reçues, lots reçus et pressés, rendement moyen
  (huile / poids des lots pressés), temps d'attente moyen (réception → début du traitement) ;
  histogramme par heure, jour ou mois (lots ou kg) ; répartition de la qualité (Excellente,
  Acceptable, Défectueuse, non analysés — alimentée par l'IA au sprint 6) ; rendement par variété.
  Classe `Statistiques` conforme au diagramme, tests `StatistiquesTest`.
- US-23 : console : lots et kg reçus, olives pressées, huile extraite et rendement du jour en
  temps réel, histogramme de l'activité de réception par heure.
- US-24 : notifications de lot (réception, file, traitement, prêt, récupération) affichées aussi
  dans la barre de notifications Android (autorisation demandée sur Android 13+), sans doublon.
  Limite : avec le plan gratuit Firebase Spark (pas de Cloud Functions), elles s'affichent tant que
  l'application est ouverte ou en arrière-plan ; un vrai push application fermée demanderait FCM
  + Cloud Functions (plan Blaze).

---

## Sprint 6 — Extension IA : analyse de la qualité des olives

| ID | User story | BF | Prio | Pts |
|----|-----------|----|------|-----|
| US-25 | En tant que **développeuse**, je veux entraîner un modèle MobileNetV2 (Colab) et l'exporter en TensorFlow Lite (classes : Excellente, Acceptable, Défectueuse). | BF-22 | M | 8 |
| US-26 | En tant que **gestionnaire**, je veux prendre ou choisir une photo des olives et obtenir la classe prédite avec un score de confiance. | BF-22 | M | 5 |
| US-27 | En tant que **gestionnaire**, je veux valider ou corriger le résultat et le rattacher au lot. | BF-22 | M | 3 |

---

## Suivi des sprints

| Sprint | Statut | Commit |
|--------|--------|--------|
| Sprint 1 | Terminé | `Sprint 1 : socle Firebase, authentification, navigation par rôle et profil` |
| Sprint 2 | Terminé | `Sprint 2 : rendez-vous, notifications et compléments des maquettes` |
| Sprint 3 | Terminé | `Sprint 3 : lots, file d'attente et suivi des lots` |
| Sprint 4 | Terminé | `Sprint 4 : huile produite, récupération, historiques et bilans` |
| Sprint 5 | Terminé | `Sprint 5 : statistiques, indicateurs de la console et notifications Android` |
| Sprint 6 | À faire | |
