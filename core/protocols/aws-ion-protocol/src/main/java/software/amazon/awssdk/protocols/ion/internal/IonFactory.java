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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.ion.IonSystem;

@SdkInternalApi
public final class IonFactory extends JsonFactory {
    private static final long serialVersionUID = 1;

    private static final boolean SHOULD_CLOSE_READER_YES = true;
    private static final boolean SHOULD_CLOSE_READER_NO = false;

    // TODO IonReaderBuilder will soon be available for constructing IonReaders
    // without an IonSystem. This should use an IonReaderBuilder.
    private final transient IonSystem ionSystem;

    public IonFactory(IonSystem ionSystem) {
        this.ionSystem = ionSystem;
    }

    @Override
    public JsonParser createParser(InputStream in) {
        return new IonParser(ionSystem.newReader(in), SHOULD_CLOSE_READER_NO);
    }

    @Override
    public JsonParser createParser(byte[] data) {
        return new IonParser(ionSystem.newReader(data), SHOULD_CLOSE_READER_NO);
    }

    @Override
    public JsonParser createParser(byte[] data, int offset, int length) {
        return new IonParser(ionSystem.newReader(data, offset, length), SHOULD_CLOSE_READER_NO);
    }

    @Override
    public JsonParser createParser(char[] data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonParser createParser(char[] data, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JsonParser createParser(String data) {
        return new IonParser(ionSystem.newReader(data), SHOULD_CLOSE_READER_NO);
    }

    @Override
    public JsonParser createParser(Reader data) {
        return new IonParser(ionSystem.newReader(data), SHOULD_CLOSE_READER_NO);
    }

    @Override
    public JsonParser createParser(File data) throws IOException {
        return new IonParser(ionSystem.newReader(new FileInputStream(data)), SHOULD_CLOSE_READER_YES);
    }

    @Override
    public JsonParser createParser(URL data) throws IOException {
        return new IonParser(ionSystem.newReader(data.openStream()), SHOULD_CLOSE_READER_YES);
    }
}
