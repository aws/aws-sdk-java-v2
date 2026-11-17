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

package software.amazon.awssdk.auth.credentials.internal.daemon;

import java.util.Arrays;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;

/**
 * A {@code credential/get} request to the credential daemon.
 */
@SdkInternalApi
public final class CredentialDaemonRequest {
    private final String credentialName;
    private final String accountId;
    private final String roleName;

    public CredentialDaemonRequest(String credentialName, String accountId, String roleName) {
        this.credentialName = credentialName;
        this.accountId = accountId;
        this.roleName = roleName;
    }

    public String credentialName() {
        return credentialName;
    }

    /**
     * Returns the request as one JSON-RPC 2.0 line, ending with {@code \n}.
     */
    byte[] toLine() {
        JsonWriter writer = JsonWriter.create();
        writer.writeStartObject()
              .writeFieldName("jsonrpc").writeValue("2.0")
              .writeFieldName("id").writeValue(1)
              .writeFieldName("method").writeValue("credential/get")
              .writeFieldName("params").writeStartObject()
              .writeFieldName("credential").writeValue(credentialName);
        if (accountId != null && roleName != null) {
            writer.writeFieldName("account_id").writeValue(accountId)
                  .writeFieldName("role_name").writeValue(roleName);
        }
        writer.writeEndObject().writeEndObject();
        byte[] json = writer.getBytes();
        byte[] line = Arrays.copyOf(json, json.length + 1);
        line[json.length] = (byte) '\n';
        return line;
    }
}
