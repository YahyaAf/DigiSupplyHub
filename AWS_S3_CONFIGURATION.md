# Configuration AWS S3 - Digital Logistics

## 📋 Configuration actuelle

La configuration AWS S3 est **désactivée par défaut** pour éviter les erreurs au démarrage.

### Fichier `application.properties`

```properties
# AWS S3 Configuration
aws.s3.enabled=false
aws.accessKeyId=YOUR_ACCESS_KEY_ID
aws.secretKey=YOUR_SECRET_ACCESS_KEY
aws.region=us-east-1
aws.s3.bucket=digital-logistics-bucket
```

## 🔧 Comment activer AWS S3

### Étape 1 : Obtenir les credentials AWS

1. Connectez-vous à la [console AWS](https://console.aws.amazon.com/)
2. Allez dans **IAM** (Identity and Access Management)
3. Créez un nouvel utilisateur avec les permissions S3
4. Récupérez votre `Access Key ID` et `Secret Access Key`

### Étape 2 : Créer un bucket S3

1. Allez dans le service **S3**
2. Cliquez sur "Create bucket"
3. Donnez un nom unique à votre bucket
4. Choisissez votre région (ex: `us-east-1`, `eu-west-1`)
5. Configurez les permissions selon vos besoins

### Étape 3 : Activer S3 dans l'application

Modifiez votre fichier `application.properties` :

```properties
# AWS S3 Configuration
aws.s3.enabled=true
aws.accessKeyId=VOTRE_ACCESS_KEY_ID_REEL
aws.secretKey=VOTRE_SECRET_KEY_REEL
aws.region=us-east-1
aws.s3.bucket=votre-nom-de-bucket
```

## 🛡️ Sécurité - Bonnes pratiques

### ⚠️ NE JAMAIS commiter les credentials sur Git !

Pour la production, utilisez des variables d'environnement :

```properties
# AWS S3 Configuration
aws.s3.enabled=${AWS_S3_ENABLED:false}
aws.accessKeyId=${AWS_ACCESS_KEY_ID:}
aws.secretKey=${AWS_SECRET_KEY:}
aws.region=${AWS_REGION:us-east-1}
aws.s3.bucket=${AWS_S3_BUCKET:}
```

Puis définissez les variables d'environnement :

**Windows (PowerShell):**
```powershell
$env:AWS_S3_ENABLED="true"
$env:AWS_ACCESS_KEY_ID="votre_access_key"
$env:AWS_SECRET_KEY="votre_secret_key"
$env:AWS_REGION="us-east-1"
$env:AWS_S3_BUCKET="votre-bucket"
```

**Linux/Mac:**
```bash
export AWS_S3_ENABLED=true
export AWS_ACCESS_KEY_ID=votre_access_key
export AWS_SECRET_KEY=votre_secret_key
export AWS_REGION=us-east-1
export AWS_S3_BUCKET=votre-bucket
```

## 🔍 Vérification

Après activation, vous pouvez vérifier que S3 fonctionne en :

1. Démarrant l'application : `.\mvnw.cmd spring-boot:run`
2. Vérifiant les logs pour voir si le bean S3Client est créé
3. Testant l'upload d'un fichier via l'endpoint correspondant

## 📦 Fonctionnalités affectées

Les services suivants utilisent S3 (désactivés si S3 est désactivé) :
- Upload de fichiers/images
- Stockage de documents
- Gestion des médias

Si S3 est désactivé, l'application utilisera le stockage local via `FileStorageService`.

## 🔧 Configuration conditionnelle

Les composants suivants sont conditionnels (activés seulement si `aws.s3.enabled=true`) :
- `S3Config` - Configuration du client S3
- `S3Service` - Service de gestion S3
- Les injections dans `ProductController` et `ProductService` sont optionnelles

## ❓ Résolution des problèmes

### Erreur "Could not resolve placeholder 'aws.accessKeyId'"
✅ **Résolu** : Les propriétés ont des valeurs par défaut maintenant

### Erreur "The AWS Access Key Id you provided does not exist"
- Vérifiez que vos credentials sont corrects
- Vérifiez que l'utilisateur IAM a les permissions S3

### Erreur "Access Denied"
- Vérifiez les permissions IAM de votre utilisateur
- Assurez-vous que le bucket autorise votre utilisateur

## 🌐 Régions AWS disponibles

- `us-east-1` - États-Unis Est (Virginie du Nord)
- `us-west-2` - États-Unis Ouest (Oregon)
- `eu-west-1` - Europe (Irlande)
- `eu-central-1` - Europe (Francfort)
- `ap-southeast-1` - Asie-Pacifique (Singapour)

[Liste complète des régions AWS](https://docs.aws.amazon.com/general/latest/gr/rande.html)

