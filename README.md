# SAFESealing

Ein Tool zum sicheren Versiegeln und Entsiegeln von Daten sowie zur Verifizierung von Transparenzdokumenten (OCMF).

**Herausgeber:** SAFE e.V.  https://www.safe-ev.de/en/  
**Autor:** J.Wilkes, metabit

## Was ist SAFESealing?

SAFESealing ist ein Tool, das Ihnen ermöglicht, Daten sicher zu verschlüsseln und zu übertragen, ohne auf herkömmliche Hash-Funktionen oder Message Authentication Codes (MACs) angewiesen zu sein. Das Tool verwendet das **IIP (Interleaved Integrity Padding)** Verfahren, um die Integrität verschlüsselter Daten zu gewährleisten.

### Wofür können Sie das Tool nutzen?

- **Daten sicher versiegeln**: Verschlüsseln Sie Dateien oder Texte mit Ihrem privaten Schlüssel, um sie sicher zu übertragen
- **Daten entsiegeln**: Entschlüsseln Sie versiegelte Daten mit dem passenden öffentlichen Schlüssel
- **Transparenzdokumente verifizieren**: Überprüfen Sie die Echtheit von OCMF-Format Transparenz-XML-Dateien (z.B. für Energiezähler-Daten)
- **Schlüsselpaare generieren**: Erstellen Sie neue kryptographische Schlüsselpaare für die Verschlüsselung

## Schnellstart – Web-Interface

Das einfachste und schnellste, um SAFESealing zu nutzen, ist über die Web-Oberfläche.

### 1. Anwendung starten

**Option A: Mit Docker (empfohlen)**

```bash
# Docker Image erstellen
docker build -t safesealing-web .

# Container starten (läuft auf Port 8080)
docker run --rm -p 8080:8080 safesealing-web
```

**Option B: Direkt mit Java**

```bash
# Projekt bauen (Tests überspringen für schnelleren Build)
mvn -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -q package

# Server starten
java -cp target/safesealing-0.9.2-runnable.jar com.metabit.custom.safe.web.ServerMain
```

### 2. Web-Interface öffnen

Öffnen Sie in Ihrem Browser: **http://localhost:8080/**

## Wie Sie das Tool verwenden

### Daten versiegeln (Seal)

1. **Schlüsselpaar erstellen** (falls noch nicht vorhanden):
   - Klicken Sie auf "Generate Keys"
   - Es werden ein privater und ein öffentlicher Schlüssel generiert

2. **Daten eingeben**:
   - Geben Sie Ihren **privaten Schlüssel** ein (oder laden Sie eine .pem-Datei hoch)
   - Wählen Sie entweder eine Datei aus oder geben Sie Text direkt ein
   - Optional: Geben Sie eine eindeutige ID (uniqueId) ein

3. **Versiegeln**:
   - Klicken Sie auf "Seal"
   - Das Tool erstellt eine verschlüsselte, versiegelte Version Ihrer Daten
   - Sie können die versiegelte Datei herunterladen (`.der`-Format)

### Daten entsiegeln (Reveal)

1. **Öffentlichen Schlüssel bereitstellen**:
   - Geben Sie den öffentlichen Schlüssel ein (oder laden Sie eine .pem-Datei hoch)

2. **Versiegelte Daten eingeben**:
   - Laden Sie die versiegelte Datei hoch, oder
   - Fügen Sie die Base64-kodierte Version ein

3. **Entsiegeln**:
   - Klicken Sie auf "Reveal"
   - Die ursprünglichen Daten werden wiederhergestellt
   - Sie können die entsiegelten Daten als Datei herunterladen oder als Textvorschau ansehen

### Transparenzdokumente verifizieren

Diese Funktion prüft die Echtheit von OCMF-Format Transparenz-XML-Dateien (z.B. für Smart Meter Daten).

1. **XML-Datei hochladen**:
   - Laden Sie eine Transparenz-XML-Datei hoch, die ein `<signedData format="ocmf">` Element enthält
   - Oder fügen Sie den XML-Text direkt ein

2. **Öffentlichen Schlüssel angeben** (falls nötig):
   - Wenn der öffentliche Schlüssel nicht in der XML-Datei enthalten ist (`<publicKey>` Element), geben Sie ihn separat an
   - Entweder als PEM-Text oder als Base64-kodierte DER-Datei

3. **Verifizieren**:
   - Klicken Sie auf "Verify"
   - Das Tool prüft die ECDSA-Signatur gegen den öffentlichen Schlüssel
   - Sie erhalten eine Bestätigung (ok) oder eine Liste von Fehlern

**Ergebnis**: Sie sehen, ob die Signatur gültig ist und das Dokument nicht manipuliert wurde.

## Technische Details

### IIP – Interleaved Integrity Padding

Das Kernverfahren dieses Tools ist IIP, eine Methode zur Integritätsprüfung verschlüsselter Nachrichten ohne Verwendung von Hash-Funktionen oder MACs.

**Wie funktioniert es?**

- Eine zufällige Byte-Sequenz (Nonce) wird mehrfach in die Nachricht eingebunden
- Die Nachricht wird mit einem Verschlüsselungsalgorithmus verschlüsselt, der eine hohe Diffusion bietet
- Nach der Entschlüsselung wird die Konsistenz der Nonce-Werte geprüft
- Wurde der verschlüsselte Text verändert, hat sich dies auf die Nonce-Werte ausgewirkt und wird erkannt

**Wichtig**: IIP bietet keine Vertraulichkeit (Confidentiality) – das wird durch die Verschlüsselung erreicht. Es verbessert auch nicht etablierte kryptographische Padding-Verfahren, sondern ergänzt sie um Integritätsprüfung ohne Hash-Verwendung.

### API-Endpunkte

Die Web-Oberfläche nutzt folgende REST-API-Endpunkte unter `/api/*`:

**Schlüssel generieren:**
- `POST /api/keys/generate`
  - Gibt ein neues Schlüsselpaar zurück: `{ privateKeyPem, publicKeyPem }`

**Daten versiegeln:**
- `POST /api/seal` (multipart/form-data)
  - Felder: `privateKeyPem` (Text oder Datei), `payload` (Datei) oder `payloadText` (Text), optional `uniqueId`, optional `algorithmVersion` (1|2, Standard: 2), optional `compression` (true|false, Standard: true)
  - Gibt zurück: `{ base64, hexPreview, size }`

**Daten entsiegeln:**
- `POST /api/reveal` (multipart/form-data)
  - Felder: `publicKeyPem` (Text oder Datei), `sealed` (Datei) oder `sealedBase64` (Text), optional `algorithmVersion` (Standard: 2)
  - Gibt zurück: `{ payloadBase64, utf8Preview, size }`

**OCMF-Verifizierung:**
- `POST /api/verify` (multipart/form-data)
  - Felder: `xml` (Datei) oder `xmlText` (Text): Transparenz-XML mit `<signedData format="ocmf">` und optional `<publicKey>`
  - Optional: `publicKeyPem` (Text, PEM) oder `publicKeyBase64` (Text, Base64-DER), falls kein `<publicKey>` im XML enthalten ist
  - Gibt zurück: `{ ok: boolean, errors: Error[] | [], format: "OCMF" }`

### Beispiel: Verifizierung via cURL

**XML-Datei hochladen:**

```bash
curl -s -X POST http://localhost:8080/api/verify \
  -F xml=@/path/to/transparency.xml | jq
```

**XML und Schlüssel direkt senden:**

```bash
curl -s -X POST http://localhost:8080/api/verify \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "xmlText=$(cat /path/to/transparency.xml)" \
  --data-urlencode "publicKeyPem=$(cat /path/to/public.pem)" | jq
```

### Als Bibliothek nutzen

Sie können SAFESealing auch direkt in Ihre Java-Anwendung einbinden.

1. **JAR-Datei hinzufügen**:
   - Fügen Sie die `safesealing*.jar` Datei zu Ihrem Projekt hinzu

2. **SAFESealer verwenden**:

```java
import com.metabit.custom.safe.safeseal.SAFESealSealer;
// ...

PrivateKey senderPrivateKey;  // Ihr privater Schlüssel
Long uniqueID;                 // Eindeutige ID
byte[] payload;                // Die zu versiegelnden Daten

SAFESealSealer sealer = new SAFESealSealer();
byte[] sealedForTransport = sealer.seal(senderPrivateKey, null, payload, uniqueID);
```

Das resultierende Byte-Array enthält die gepaddeten, verschlüsselten, formatierten und serialisierten Daten, die transportbereit sind.

### Projekt bauen

**Von Source bauen:**

Beachten Sie, dass Maven installiert sein muss: https://maven.apache.org/

```bash
# Projekt kompilieren und JAR erstellen (inklusive Tests)
mvn clean package
```

Dies erstellt JAR-Dateien im `target`-Ordner (`safesealing*.jar`). Die Tests können einige Zeit in Anspruch nehmen.

```bash
# Lokal installieren (für Nutzung in anderen Projekten)
mvn install
```

**Für schnelleren Build (ohne Tests):**

```bash
mvn -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -q package
```

## Entwicklung

### Projektstruktur

In IntelliJ öffnen Sie das Projekt als Maven-Projekt (wählen Sie die `pom.xml` Datei).

Die Haupt-Einstiegspunkte sind die Klassen `SAFESealingSealer` und `SAFESealingRevealer`.

Das Projekt besteht aus 4 Haupt-Paketen:

- **safeseal**: API und Kommandozeilen-Tests
- **iip**: Enthält die Kern-Kryptographie (InterleavedIntegrityPadding.java – reines JDK Java)
- **shared**: Konstanten, Funktionen, Algorithmus-Spezifikationen
- **safeseal/impl**: Die Implementierung hinter der API

### Abhängigkeiten

- **BouncyCastle**: Kryptographie-Provider-Bibliothek
- **Javalin**: Web-Framework für die REST-API
- **Maven**: Build-Engine

### Zusätzliche Krypto-Algorithmen hinzufügen

Neue Verschlüsselungsalgorithmen müssen zu `shared/AlgorithmSpecCollection.java` hinzugefügt und getestet werden, bevor sie verwendet werden können.

## Hintergrund & Referenzen

Das IIP-Verfahren basiert auf den Grundlagen der Informationstheorie und Kryptographie:

- **Diffusion**: Veränderungen im Klartext sollten sich über den gesamten verschlüsselten Text verteilen (siehe Avalanche-Effekt)
- **Ohne Plaintext-Kenntnis**: Die Integritätsprüfung erfolgt ohne Kenntnis der ursprünglichen Nachricht

### Wissenschaftliche Referenzen

* [1] Claude E. Shannon, "A Mathematical Theory of Cryptography", Bell System Technical Memo MM 45-110-02, September 1, 1945.
* [2] Claude E. Shannon, "Communication Theory of Secrecy Systems", Bell System Technical Journal, vol. 28-4, pages 656–715, 1949.
* [3] "Information Theory and Entropy". Model Based Inference in the Life Sciences: A Primer on Evidence. Springer New York. 2008-01-01. pp. 51–82. doi:10.1007/978-0-387-74075-1_3. ISBN 9780387740737.
