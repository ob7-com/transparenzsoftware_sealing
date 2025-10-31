#!/bin/bash
# Quick test script for VerifyApi tests
# This runs the server and executes curl tests against it

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

PORT=8082
TEST_XML_FILE="src/test/resources/testdata/ocmf/destre10118001-2025-10-31-11_31_20-76358978.xml"
PUBLIC_KEY_HEX="3059301306072A8648CE3D020106082A8648CE3D030107034200048a8760ab2c8726788c513584d0cd1cccc40004bb570af5ed1e944685c0648ed40ef98f57b373a66965db565351bfabf01617da5c53147240c113c3946fca786e"

echo "🔨 Building application..."
mvn -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -q package > /dev/null 2>&1 || {
    echo "❌ Build failed"
    exit 1
}

# Check if port is already in use
if lsof -Pi :$PORT -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "⚠️  Port $PORT is already in use. Trying to find free port..."
    for p in 8080 8081 8083 8084 8085; do
        if ! lsof -Pi :$p -sTCP:LISTEN -t >/dev/null 2>&1 ; then
            PORT=$p
            echo "✅ Using port $PORT"
            break
        fi
    done
fi

echo "🚀 Starting server on port $PORT..."
PORT=$PORT java -cp target/safesealing-0.9.2-runnable.jar com.metabit.custom.safe.web.ServerMain > /tmp/safesealing-server.log 2>&1 &
SERVER_PID=$!

# Wait for server to start
echo "⏳ Waiting for server to start..."
for i in {1..30}; do
    if curl -s http://localhost:$PORT/api/health > /dev/null 2>&1; then
        echo "✅ Server started"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Server failed to start"
        kill $SERVER_PID 2>/dev/null || true
        exit 1
    fi
    sleep 0.5
done

cleanup() {
    echo ""
    echo "🛑 Stopping server..."
    kill $SERVER_PID 2>/dev/null || true
    wait $SERVER_PID 2>/dev/null || true
}

trap cleanup EXIT INT TERM

echo ""
echo "🧪 Running tests..."
echo ""

# Test 1: Verify with embedded public key (real XML file)
echo "Test 1: Verify with embedded public key from file..."
RESPONSE=$(curl -s -X POST http://localhost:$PORT/api/verify \
    -F "xml=@$TEST_XML_FILE")
OK=$(echo "$RESPONSE" | grep -o '"ok"\s*:\s*true' || echo "")
if [ -n "$OK" ]; then
    echo "  ✅ PASSED"
else
    echo "  ❌ FAILED"
    echo "  Response: $RESPONSE"
fi

# Test 2: Verify with separate public key (Base64)
echo ""
echo "Test 2: Verify with separate public key (Base64)..."
# Extract first value block (with multiline support)
FIRST_VALUE=$(awk '/<value[^>]*>/,/<\/value>/ {print}' "$TEST_XML_FILE" | head -20 | tr -d '\n' | sed 's/^[[:space:]]*//')
# Remove publicKey element
XML_WITHOUT_KEY=$(echo "$FIRST_VALUE" | sed 's/<publicKey[^>]*>.*<\/publicKey>//g' | sed 's/publicKey[^>]*>.*<\/publicKey>//g')
XML_WITHOUT_KEY="<?xml version=\"1.0\" encoding=\"utf-8\"?><values>$XML_WITHOUT_KEY</values>"

# Convert hex to base64
PUBLIC_KEY_B64=$(echo -n "$PUBLIC_KEY_HEX" | xxd -r -p | base64)

RESPONSE=$(curl -s -X POST http://localhost:$PORT/api/verify \
    -F "xmlText=$XML_WITHOUT_KEY" \
    -F "publicKeyBase64=$PUBLIC_KEY_B64")
OK=$(echo "$RESPONSE" | grep -o '"ok"\s*:\s*true' || echo "")
if [ -n "$OK" ]; then
    echo "  ✅ PASSED"
else
    echo "  ❌ FAILED"
    echo "  Response: $RESPONSE"
fi

# Test 3: Missing XML should return 400
echo ""
echo "Test 3: Missing XML should return 400..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:$PORT/api/verify \
    -F "publicKeyPem=INVALID")
if [ "$HTTP_CODE" == "400" ]; then
    echo "  ✅ PASSED (got 400)"
else
    echo "  ❌ FAILED (got $HTTP_CODE, expected 400)"
fi

# Test 4: Missing public key should return 400
echo ""
echo "Test 4: Missing public key should return 400..."
XML_NO_KEY=$(echo "<?xml version=\"1.0\" encoding=\"utf-8\"?><values>$FIRST_VALUE</values>" | sed 's/<publicKey[^>]*>.*<\/publicKey>//')
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:$PORT/api/verify \
    -F "xmlText=$XML_NO_KEY")
if [ "$HTTP_CODE" == "400" ]; then
    echo "  ✅ PASSED (got 400)"
else
    echo "  ❌ FAILED (got $HTTP_CODE, expected 400)"
fi

echo ""
echo "✨ All quick tests completed!"
echo ""
echo "💡 For full test suite, fix compilation errors in other test classes,"
echo "   or run tests manually in browser: http://localhost:$PORT/"

