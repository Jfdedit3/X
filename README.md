# X Android WebView

Application Android minimale en Kotlin qui affiche uniquement X (x.com / twitter.com) dans une WebView.

## Fonctionnalités

- ouverture directe de `https://x.com`
- WebView avec JavaScript, cookies et DOM storage activés
- limitation aux domaines X / Twitter / t.co
- liens externes ouverts hors de l'application
- retour Android géré dans l'historique WebView
- workflow GitHub Actions pour compiler un APK debug

## Build local

Ouvre le projet dans Android Studio puis lance :

- synchronisation Gradle
- `assembleDebug`

APK attendu :

`app/build/outputs/apk/debug/app-debug.apk`

## Build GitHub Actions

Le dépôt contient un workflow :

`.github/workflows/android.yml`

Tu peux le lancer via :

- push sur `main`
- exécution manuelle avec `workflow_dispatch`

L'APK debug sera disponible dans les artifacts GitHub Actions.
