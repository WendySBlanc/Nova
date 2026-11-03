# Mise a jour automatique de Nova

Le workflow `Nova phone update` construit et publie un APK Preview a chaque push sur
`nova-stabilization`. L'application verifie les nouvelles revisions au demarrage et environ
toutes les 30 minutes en arriere-plan. Android peut decaler ce travail pour economiser la
batterie.

## Depot GitHub

Le depot attendu par defaut est `WendySBlanc/Nova`. Le workflow injecte automatiquement le
nom reel du depot GitHub dans l'APK, donc un renommage ou un fork ne demande pas de modifier le
code.

## Signature obligatoire

Android n'accepte une mise a jour que si elle garde le meme identifiant
`com.nova.manhwa.debug` et la meme cle de signature. L'APK Preview actuel est signe par
`%USERPROFILE%\.android\debug.keystore`, empreinte SHA-256 :

`FC:A3:DA:0C:67:BC:95:8F:0A:AE:A3:3F:0C:62:04:A5:A9:E7:80:0D:BE:8C:76:50:CA:DD:36:3C:EC:30:56:44`

Dans GitHub, ouvrir `Settings > Secrets and variables > Actions`, puis creer :

| Secret | Valeur |
| --- | --- |
| `SIGNING_KEY` | contenu Base64 du fichier `debug.keystore` |
| `ALIAS` | `androiddebugkey` |
| `KEY_STORE_PASSWORD` | `android` |
| `KEY_PASSWORD` | `android` |

PowerShell peut placer la valeur Base64 dans le presse-papiers :

```powershell
[Convert]::ToBase64String(
    [IO.File]::ReadAllBytes("$env:USERPROFILE\.android\debug.keystore")
) | Set-Clipboard
```

Ne jamais ajouter le keystore ni sa valeur Base64 au depot. Conserver une sauvegarde privee du
keystore : sa perte rendrait toute mise a jour de l'installation actuelle impossible.
Le workflow refuse de publier si l'identifiant de l'application, le `versionCode` ou l'empreinte
SHA-256 ne correspondent pas aux valeurs attendues.

## Premier raccordement du telephone

L'APK deja installe pointe encore vers Komikku. Il faut donc installer manuellement une seule
fois le premier APK Nova construit par ce workflow. Les revisions suivantes seront detectees,
telechargees et remises a Android pour installation automatiquement.

Sur Android, autoriser une fois Nova a installer des applications inconnues. Selon la version
et la politique du telephone, Android peut encore afficher une confirmation systeme avant de
remplacer l'application.
