/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.protocols.ion.internal;

import static com.fasterxml.jackson.core.JsonParser.NumberType.BIG_DECIMAL;
import static com.fasterxml.jackson.core.JsonParser.NumberType.BIG_INTEGER;
import static com.fasterxml.jackson.core.JsonParser.NumberType.DOUBLE;
import static software.amazon.ion.IonType.STRUCT;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonType;

@SdkInternalApi
public final class IonParser extends JsonParser {
    private final IonReader reader;
    private final boolean shouldCloseReader;
    private State state = State.BEFORE_VALUE;
    private JsonToken currentToken;
    private JsonToken lastClearedToken;
    private boolean shouldSkipContainer;
    private boolean closed;

    public IonParser(IonReader reader, boolean shouldCloseReader) {
        super(Feature.collectDefaults());
        this.reader = reader;
        this.shouldCloseReader = shouldCloseReader;
    }

    @Override
    public ObjectCodec getCodec() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCodec(ObjectCodec c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version version() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        if (shouldCloseReader) {
            reader.close();
        } else if (Feature.AUTO_CLOSE_SOURCE.enabledIn(_features)) {
            reader.close();
        }
        closed = true;
    }

    @Override
    public JsonToken nextToken() {
        currentToken = doNextToken();
        return currentToken;
    }

    private JsonToken doNextToken() {
        for (; ; ) {
            switch (state) {
                case BEFORE_VALUE:
                    IonType currentType = reader.next();

                    if (currentType == null) {
                        boolean topLevel = reader.getDepth() == 0;
                        if (topLevel) {
                            state = State.EOF;
                            continue;
                        } else {
                            state = State.END_OF_CONTAINER;
                            return reader.isInStruct()
                                   ? JsonToken.END_OBJECT
                                   : JsonToken.END_ARRAY;
                        }
                    }

                    if (reader.isInStruct()) {
                        state = State.FIELD_NAME;
                        return JsonToken.FIELD_NAME;
                    } else {
                        state = State.VALUE;
                        return getJsonToken();
                    }

                case END_OF_CONTAINER:
                    reader.stepOut();
                    state = State.BEFORE_VALUE;
                    continue;

                case EOF:
                    return null;

                case FIELD_NAME:
                    state = State.VALUE;
                    return getJsonToken();

                case VALUE:
                    state = State.BEFORE_VALUE;
                    if (IonType.isContainer(reader.getType()) && !reader.isNullValue() && !shouldSkipContainer) {
                        reader.stepIn();
                    }
                    shouldSkipContainer = false;
                    continue;
                default:
                    // Ignore.
            }
        }
    }

    @Override
    public JsonToken nextValue() {
        JsonToken token = nextToken();
        return (token == JsonToken.FIELD_NAME)
               ? nextToken()
               : token;
    }

    @Override
    public JsonParser skipChildren() {
        IonType currentType = reader.getType();
        if (IonType.isContainer(currentType)) {
            shouldSkipContainer = true;
            currentToken = currentType == STRUCT
                           ? JsonToken.END_OBJECT
                           : JsonToken.END_ARRAY;
        }
        return this;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public JsonToken getCurrentToken() {
        return currentToken;
    }

    @Override
    public int getCurrentTokenId() {
        return currentToken == null
               ? JsonTokenId.ID_NO_TOKEN
               : currentToken.id();
    }

    @Override
    public boolean hasCurrentToken() {
        return currentToken != null;
    }

    @Override
    public boolean hasTokenId(int id) {
        return getCurrentTokenId() == id;
    }

    @Override
    public boolean hasToken(JsonToken t) {
        return currentToken == t;
    }

    @Override
    public String getCurrentName() {
        return reader.getFieldName();
    }

    @Override
    public JsonStreamContext getParsingContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonLocation getTokenLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonLocation getCurrentLocation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearCurrentToken() {
        lastClearedToken = currentToken;
        currentToken = null;
    }

    @Override
    public JsonToken getLastClearedToken() {
        return lastClearedToken;
    }

    @Override
    public void overrideCurrentName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getText() {
        if (state == State.FIELD_NAME) {
            return reader.getFieldName();
        }
        if (IonType.isText(reader.getType())) {
            return reader.stringValue();
        }
        if (currentToken == null) {
            // start or end of stream
            return null;
        }
        if (currentToken.isNumeric()) {
            return getNumberValue().toString();
        }
        return currentToken.asString();
    }

    @Override
    public char[] getTextCharacters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTextLength() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTextOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasTextCharacters() {
        return false;
    }

    @Override
    public Number getNumberValue() {
        NumberType numberType = getNumberType();
        if (numberType == null) {
            throw SdkClientException.builder()
                                    .message(String.format("Unable to get number value for non-numeric token %s",
                                                           reader.getType()))
                                    .build();
        }
        switch (numberType) {
            case BIG_DECIMAL:
                return reader.bigDecimalValue();
            case BIG_INTEGER:
                return reader.bigIntegerValue();
            case DOUBLE:
                return reader.doubleValue();
            default:
                throw SdkClientException.builder()
                                        .message(String.format("Unable to get number value for number type %s",
                                                               numberType))
                                        .build();
        }
    }

    @Override
    public NumberType getNumberType() {
        switch (reader.getType()) {
            case DECIMAL:
                return BIG_DECIMAL;
            case FLOAT:
                return DOUBLE;
            case INT:
                return BIG_INTEGER;
            default:
                return null;
        }
    }

    @Override
    public int getIntValue() {
        return reader.intValue();
    }

    @Override
    public long getLongValue() {
        return reader.longValue();
    }

    @Override
    public BigInteger getBigIntegerValue() {
        return reader.bigIntegerValue();
    }

    @Override
    public float getFloatValue() {
        return (float) reader.doubleValue();
    }

    @Override
    public double getDoubleValue() {
        return reader.doubleValue();
    }

    @Override
    public BigDecimal getDecimalValue() {
        return reader.decimalValue();
    }

    @Override
    public Object getEmbeddedObject() {
        if (currentToken != JsonToken.VALUE_EMBEDDED_OBJECT) {
            return null;
        }
        IonType currentType = reader.getType();
        switch (currentType) {
            case BLOB:
            case CLOB:
                return ByteBuffer.wrap(reader.newBytes());
            case TIMESTAMP:
                return reader.timestampValue().dateValue();
            default:
                throw SdkClientException.builder()
                                        .message(String.format("Cannot return embedded object for Ion type %s",
                                                               currentType))
                                        .build();
        }
    }

    @Override
    public byte[] getBinaryValue(Base64Variant bv) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getValueAsString(String defaultValue) {
        // The documentation is ambiguous about whether field names should
        // return their text or the default value. To conform with the
        // CBORParser, they will get the default value here.
        if (currentToken != JsonToken.VALUE_STRING) {
            if (currentToken == null || currentToken == JsonToken.VALUE_NULL || !currentToken.isScalarValue()) {
                return defaultValue;
            }
        }
        return getText();
    }

    private JsonToken getJsonToken() {
        if (reader.isNullValue()) {
            return JsonToken.VALUE_NULL;
        }

        IonType currentType = reader.getType();
        switch (currentType) {
            case BLOB:
            case CLOB:
                return JsonToken.VALUE_EMBEDDED_OBJECT;
            case BOOL:
                return reader.booleanValue() ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE;
            case DECIMAL:
                return JsonToken.VALUE_NUMBER_FLOAT;
            case FLOAT:
                return JsonToken.VALUE_NUMBER_FLOAT;
            case INT:
                return JsonToken.VALUE_NUMBER_INT;
            case LIST:
                return JsonToken.START_ARRAY;
            case SEXP:
                return JsonToken.START_ARRAY;
            case STRING:
                return JsonToken.VALUE_STRING;
            case STRUCT:
                return JsonToken.START_OBJECT;
            case SYMBOL:
                return JsonToken.VALUE_STRING;
            case TIMESTAMP:
                return JsonToken.VALUE_EMBEDDED_OBJECT;
            default:
                throw SdkClientException.builder()
                                        .message(String.format("Unhandled Ion type %s", currentType))
                                        .build();
        }
    }

    private enum State {
        BEFORE_VALUE,
        END_OF_CONTAINER,
        EOF,
        FIELD_NAME,
        VALUE
    }
}
