# ZitounaFlow

Application mobile Android de **gestion des passages dans une huilerie** : rendez-vous,
réception des lots d'olives, file d'attente, suivi du traitement, huile produite,
historique, statistiques et analyse de la qualité des olives par IA.

| Élément | Choix |
|---|---|
| Plateforme | Android Studio, **Java**, minSdk 24 |
| Base de données | Cloud Firestore (Firebase) |
| Authentification | Firebase Authentication (e-mail / mot de passe) |
| IA (sprint 6) | TensorFlow Lite (MobileNetV2) |
| Méthode | Scrum — voir [`docs/BACKLOG_SCRUM.md`](docs/BACKLOG_SCRUM.md) |

## Lancer le projet

1. Ouvrir le dossier dans **Android Studio**.
2. Dans la [console Firebase](https://console.firebase.google.com) :
   - créer un projet, puis ajouter une **application Android** avec le package
     `com.example.zitounaproject` ;
   - télécharger **`google-services.json`** et le placer dans le dossier **`app/`** ;
   - *Authentication* > *Sign-in method* : activer **E-mail/Mot de passe** ;
   - *Firestore Database* : créer la base, puis coller le contenu de
     [`firestore.rules`](firestore.rules) dans l'onglet *Règles*.
3. *Sync Project with Gradle Files*, puis lancer l'application sur un émulateur.

## Tests

`FileAttenteCalculatorTest`, `LotFilterTest` et `StatistiquesTest` (dossier `app/src/test`) vérifient le calcul de la
file d'attente, du rendement, des campagnes, de la recherche, des bilans et des statistiques. Dans Android Studio : clic droit sur le fichier > *Run*.

## Architecture

```
app/src/main/java/com/example/zitounaproject/
├── MainActivity.java          Routeur (session -> écran du rôle)
├── model/                     Classes du diagramme de classes (Utilisateur, Agriculteur, ...)
├── data/                      Accès Firebase (AuthRepository, UserSession, ...)
├── util/                      Validation des formulaires, messages d'erreur
└── ui/
    ├── auth/                  Onboarding, connexion, inscription
    ├── common/                Activités/fragments partagés (profil, navigation)
    ├── agriculteur/           Espace agriculteur
    └── gestionnaire/          Espace gestionnaire
```

### Modèle Firestore

| Collection | Document | Champs principaux |
|---|---|---|
| `utilisateurs` | uid | nom, prenom, telephone, email, role, dateCreation, nomExploitation, adresse / matricule |
| `rendezVous` | auto | agriculteurId, date, jour, creneau, variete, quantiteEstimeeKg, remarque, statut, motifRefus, dateDemande, dateTraitement |
| `planning` | yyyy-MM-dd | s0..s3 : nombre de réservations actives par créneau |
| `notifications` | auto | destinataireId, titre, message, type, dateEnvoi, lue |
| `lots` | auto | numero, agriculteurId, variete, poidsBrutKg, tareKg, poidsKg, statut, ordreFile, prioritaire, positionFile, tempsAttenteEstimeMin, debutEstime, dateDebutTraitement, quantiteHuileL… |
| `lots/{id}/historique` | auto | ancienStatut, nouveauStatut, dateChangement, parId |
| `compteurs` | lots | dernier (numérotation des lots) |

## Avancement

| Sprint | Contenu | Statut |
|---|---|---|
| 1 | Firebase, authentification, navigation par rôle, profil | ✅ |
| 2 | Rendez-vous, notifications | ✅ |
| 3 | Lots et file d'attente | ✅ |
| 4 | Huile produite, récupération, historiques et bilans | ✅ |
| 5 | Statistiques, console, notifications Android | ✅ |
| 6 | Analyse IA de la qualité des olives | ⏳ |
