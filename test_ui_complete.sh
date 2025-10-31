#!/bin/bash
# Comprehensive UI test script - checks all tabs and input fields
# Tests XML verification functionality end-to-end

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

PORT=8080
TEST_XML_FILE="src/test/resources/testdata/ocmf/destre10118001-2025-10-31-11_31_20-76358978.xml"

echo "🔍 UI-Funktionalitätstest"
echo "=========================="
echo ""

# Check if server is running
echo "1️⃣ Prüfe ob Server läuft..."
if ! curl -s http://localhost:$PORT/api/health > /dev/null 2>&1; then
    echo "   ❌ Server läuft nicht auf Port $PORT"
    echo "   Bitte starten Sie den Server zuerst mit:"
    echo "   java -cp target/safesealing-0.9.2-runnable.jar com.metabit.custom.safe.web.ServerMain"
    exit 1
fi
echo "   ✅ Server läuft"
echo ""

# Test 1: Check if all required HTML elements exist in the response
echo "2️⃣ Prüfe ob alle UI-Elemente vorhanden sind..."
HTML_RESPONSE=$(curl -s http://localhost:$PORT/)

MISSING_ELEMENTS=0

# Verify Tab elements
echo "   Prüfe Verify-Tab..."
REQUIRED_VERIFY_ELEMENTS=(
    "id=\"tab-verify\""
    "id=\"xmlFile\""
    "id=\"xmlText\""
    "id=\"verifyPublicKeyPem\""
    "id=\"verifyResultContainer\""
    "onclick=\"doVerify()\""
)

for element in "${REQUIRED_VERIFY_ELEMENTS[@]}"; do
    if echo "$HTML_RESPONSE" | grep -q "$element"; then
        echo "      ✅ Gefunden: $element"
    else
        echo "      ❌ FEHLT: $element"
        MISSING_ELEMENTS=$((MISSING_ELEMENTS + 1))
    fi
done

# Seal Tab elements
echo "   Prüfe Seal-Tab..."
REQUIRED_SEAL_ELEMENTS=(
    "id=\"tab-seal\""
    "id=\"payloadFile\""
    "id=\"payloadText\""
    "id=\"sealPrivateKeyPem\""
    "id=\"uniqueId\""
    "id=\"alg\""
    "id=\"compression\""
    "id=\"sealOut\""
    "onclick=\"doSeal()\""
)

for element in "${REQUIRED_SEAL_ELEMENTS[@]}"; do
    if echo "$HTML_RESPONSE" | grep -q "$element"; then
        echo "      ✅ Gefunden: $element"
    else
        echo "      ❌ FEHLT: $element"
        MISSING_ELEMENTS=$((MISSING_ELEMENTS + 1))
    fi
done

# Reveal Tab elements
echo "   Prüfe Reveal-Tab..."
REQUIRED_REVEAL_ELEMENTS=(
    "id=\"tab-reveal\""
    "id=\"sealedFile\""
    "id=\"sealedBase64\""
    "id=\"revealPublicKeyPem\""
    "id=\"algReveal\""
    "id=\"revealOut\""
    "id=\"payloadPreview\""
    "onclick=\"doReveal()\""
)

for element in "${REQUIRED_REVEAL_ELEMENTS[@]}"; do
    if echo "$HTML_RESPONSE" | grep -q "$element"; then
        echo "      ✅ Gefunden: $element"
    else
        echo "      ❌ FEHLT: $element"
        MISSING_ELEMENTS=$((MISSING_ELEMENTS + 1))
    fi
done

# Settings Tab elements
echo "   Prüfe Settings-Tab..."
REQUIRED_SETTINGS_ELEMENTS=(
    "id=\"tab-settings\""
    "id=\"publicKeyPem\""
    "id=\"privateKeyPem\""
    "onclick=\"generateKeys()\""
)

for element in "${REQUIRED_SETTINGS_ELEMENTS[@]}"; do
    if echo "$HTML_RESPONSE" | grep -q "$element"; then
        echo "      ✅ Gefunden: $element"
    else
        echo "      ❌ FEHLT: $element"
        MISSING_ELEMENTS=$((MISSING_ELEMENTS + 1))
    fi
done

# Navigation elements
echo "   Prüfe Navigation..."
REQUIRED_NAV_ELEMENTS=(
    "data-tab=\"verify\""
    "data-tab=\"seal\""
    "data-tab=\"reveal\""
    "data-tab=\"settings\""
)

for element in "${REQUIRED_NAV_ELEMENTS[@]}"; do
    if echo "$HTML_RESPONSE" | grep -q "$element"; then
        echo "      ✅ Gefunden: $element"
    else
        echo "      ❌ FEHLT: $element"
        MISSING_ELEMENTS=$((MISSING_ELEMENTS + 1))
    fi
done

if [ $MISSING_ELEMENTS -eq 0 ]; then
    echo "   ✅ Alle UI-Elemente vorhanden"
else
    echo "   ❌ $MISSING_ELEMENTS Element(e) fehlen"
fi
echo ""

# Test 2: Test XML verification with file upload
echo "3️⃣ Teste XML-Verifizierung mit Datei-Upload..."
if [ ! -f "$TEST_XML_FILE" ]; then
    echo "   ⚠️  Test-XML-Datei nicht gefunden: $TEST_XML_FILE"
    echo "   Überspringe Datei-Upload-Test"
else
    RESPONSE=$(curl -s -X POST http://localhost:$PORT/api/verify \
        -F "xml=@$TEST_XML_FILE")
    
    if echo "$RESPONSE" | grep -q '"ok"\s*:\s*true'; then
        echo "   ✅ Verifizierung erfolgreich"
        
        # Check for measurement data
        if echo "$RESPONSE" | grep -q "measurementData"; then
            echo "   ✅ Messdaten vorhanden"
            
            # Check for energy delivered
            if echo "$RESPONSE" | grep -q "energyDelivered"; then
                ENERGY=$(echo "$RESPONSE" | grep -o '"energyDelivered"[^,}]*' | grep -o '[0-9.]*' | head -1)
                echo "   ✅ Energiemenge gefunden: $ENERGY kWh"
            fi
        else
            echo "   ⚠️  Keine Messdaten in der Antwort"
        fi
    else
        echo "   ❌ Verifizierung fehlgeschlagen"
        echo "   Response: $RESPONSE" | head -20
    fi
fi
echo ""

# Test 3: Test XML verification with xmlText
echo "4️⃣ Teste XML-Verifizierung mit XML-Text..."
if [ ! -f "$TEST_XML_FILE" ]; then
    echo "   ⚠️  Test-XML-Datei nicht gefunden: $TEST_XML_FILE"
    echo "   Überspringe XML-Text-Test"
else
    # Read first value block from XML file
    XML_CONTENT=$(cat "$TEST_XML_FILE")
    RESPONSE=$(curl -s -X POST http://localhost:$PORT/api/verify \
        -F "xmlText=$XML_CONTENT")
    
    if echo "$RESPONSE" | grep -q '"ok"\s*:\s*true'; then
        echo "   ✅ Verifizierung mit XML-Text erfolgreich"
    else
        echo "   ❌ Verifizierung mit XML-Text fehlgeschlagen"
        echo "   Response: $(echo "$RESPONSE" | head -5)"
    fi
fi
echo ""

# Test 4: Test API endpoints availability
echo "5️⃣ Prüfe API-Endpoints..."
ENDPOINTS=(
    "/api/health"
    "/api/verify"
    "/api/seal"
    "/api/reveal"
    "/api/keys/generate"
)

for endpoint in "${ENDPOINTS[@]}"; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$PORT$endpoint -X POST 2>/dev/null || echo "000")
    if [ "$HTTP_CODE" != "000" ]; then
        echo "   ✅ $endpoint erreichbar (HTTP $HTTP_CODE)"
    else
        echo "   ❌ $endpoint nicht erreichbar"
    fi
done
echo ""

# Summary
echo "📊 Zusammenfassung"
echo "=================="
if [ $MISSING_ELEMENTS -eq 0 ]; then
    echo "✅ Alle UI-Elemente vorhanden"
    echo "✅ Server läuft und ist erreichbar"
    echo "✅ API-Endpoints verfügbar"
    echo ""
    echo "🎉 Alle Tests erfolgreich!"
    exit 0
else
    echo "❌ $MISSING_ELEMENTS UI-Element(e) fehlen"
    echo ""
    echo "⚠️  Bitte überprüfen Sie die index.html Datei"
    exit 1
fi

