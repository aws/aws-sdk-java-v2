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

package software.amazon.awssdk.policybuilder.iam.internal;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.policybuilder.iam.IamCondition;
import software.amazon.awssdk.policybuilder.iam.IamConditionKey;
import software.amazon.awssdk.policybuilder.iam.IamConditionOperator;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPolicyWriter;
import software.amazon.awssdk.policybuilder.iam.IamPrincipal;
import software.amazon.awssdk.policybuilder.iam.IamPrincipalType;
import software.amazon.awssdk.policybuilder.iam.IamStatement;
import software.amazon.awssdk.policybuilder.iam.IamValue;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter;
import software.amazon.awssdk.protocols.jsoncore.JsonWriter.JsonGeneratorFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonGenerator;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link IamPolicyWriter}.
 *
 * @see IamPolicyWriter#create
 */
@SdkInternalApi
public final class DefaultIamPolicyWriter implements IamPolicyWriter {
    private static final IamPolicyWriter INSTANCE = IamPolicyWriter.builder().build();

    private final Boolean prettyPrint;
    @NotNull private final transient JsonGeneratorFactory jsonGeneratorFactory;

    public DefaultIamPolicyWriter(Builder builder) {
        this.prettyPrint = builder.prettyPrint;
        if (Boolean.TRUE.equals(builder.prettyPrint)) {
            this.jsonGeneratorFactory = os -> {
                JsonGenerator generator = JsonNodeParser.DEFAULT_JSON_FACTORY.createGenerator(os);
                generator.useDefaultPrettyPrinter();
                return generator;
            };
        } else {
            this.jsonGeneratorFactory = null;
        }
    }

    public static IamPolicyWriter create() {
        return INSTANCE;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String writeToString(IamPolicy policy) {
        return new String(writeToBytes(policy), StandardCharsets.UTF_8);
    }

    @Override
    public byte[] writeToBytes(IamPolicy policy) {
        return writePolicy(policy).getBytes();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultIamPolicyWriter that = (DefaultIamPolicyWriter) o;

        return Objects.equals(prettyPrint, that.prettyPrint);
    }

    @Override
    public int hashCode() {
        return prettyPrint != null ? prettyPrint.hashCode() : 0;
    }

    private JsonWriter writePolicy(IamPolicy policy) {
        JsonWriter writer =
            JsonWriter.builder()
                      .jsonGeneratorFactory(jsonGeneratorFactory)
                      .build();

        writer.writeStartObject();

        writeFieldIfNotNull(writer, "Version", policy.version());
        writeFieldIfNotNull(writer, "Id", policy.id());
        writeStatements(writer, policy.statements());

        writer.writeEndObject();
        return writer;
    }

    private void writeStatements(JsonWriter writer, List<IamStatement> statements) {
        if (statements.isEmpty()) {
            return;
        }

        writer.writeFieldName("Statement");

        if (statements.size() == 1) {
            writeStatement(writer, statements.get(0));
            return;
        }

        writer.writeStartArray();
        statements.forEach(statement -> {
            writeStatement(writer, statement);
        });
        writer.writeEndArray();
    }

    private void writeStatement(JsonWriter writer, IamStatement statement) {
        writer.writeStartObject();
        writeFieldIfNotNull(writer, "Sid", statement.sid());
        writeFieldIfNotNull(writer, "Effect", statement.effect());
        writePrincipals(writer, "Principal", statement.principals());
        writePrincipals(writer, "NotPrincipal", statement.notPrincipals());
        writeValueArrayField(writer, "Action", statement.actions());
        writeValueArrayField(writer, "NotAction", statement.notActions());
        writeValueArrayField(writer, "Resource", statement.resources());
        writeValueArrayField(writer, "NotResource", statement.notResources());
        writeConditions(writer, statement.conditions());
        writer.writeEndObject();
    }

    private void writePrincipals(JsonWriter writer, String fieldName, List<IamPrincipal> principals) {
        if (principals.isEmpty()) {
            return;
        }

        if (principals.size() == 1 && principals.get(0).equals(IamPrincipal.ALL)) {
            writeFieldIfNotNull(writer, fieldName, IamPrincipal.ALL.id());
            return;
        }

        principals.forEach(p -> Validate.isTrue(!IamPrincipal.ALL.equals(p),
                                                "IamPrincipal.ALL must not be combined with other principals."));

        Map<IamPrincipalType, List<String>> aggregatedPrincipals = new LinkedHashMap<>();
        principals.forEach(principal -> {
            aggregatedPrincipals.computeIfAbsent(principal.type(), t -> new ArrayList<>())
                                .add(principal.id());
        });

        writer.writeFieldName(fieldName);
        writer.writeStartObject();
        aggregatedPrincipals.forEach((principalType, ids) -> {
            writeArrayField(writer, principalType.value(), ids);
        });
        writer.writeEndObject();
    }


    private void writeConditions(JsonWriter writer, List<IamCondition> conditions) {
        if (conditions.isEmpty()) {
            return;
        }

        Map<IamConditionOperator, Map<IamConditionKey, List<String>>> aggregatedConditions = new LinkedHashMap<>();
        conditions.forEach(condition -> {
            aggregatedConditions.computeIfAbsent(condition.operator(), t -> new LinkedHashMap<>())
                                .computeIfAbsent(condition.key(), t -> new ArrayList<>())
                                .add(condition.value());
        });

        writer.writeFieldName("Condition");
        writer.writeStartObject();
        aggregatedConditions.forEach((operator, keyValues) -> {
            writer.writeFieldName(operator.value());
            writer.writeStartObject();
            keyValues.forEach((key, values) -> {
                writeArrayField(writer, key.value(), values);
            });
            writer.writeEndObject();
        });
        writer.writeEndObject();
    }

    private void writeValueArrayField(JsonWriter writer, String fieldName, List<? extends IamValue> fieldValues) {
        List<String> values = new ArrayList<>(fieldValues.size());
        fieldValues.forEach(v -> values.add(v.value()));
        writeArrayField(writer, fieldName, values);
    }

    private void writeArrayField(JsonWriter writer,
                                 String fieldName, List<String> fieldValues) {
        if (fieldValues.isEmpty()) {
            return;
        }

        if (fieldValues.size() == 1) {
            writeFieldIfNotNull(writer, fieldName, fieldValues.get(0));
            return;
        }

        writer.writeFieldName(fieldName);
        writer.writeStartArray();
        fieldValues.forEach(writer::writeValue);
        writer.writeEndArray();
    }

    private void writeFieldIfNotNull(JsonWriter writer, String key, IamValue value) {
        if (value == null) {
            return;
        }

        writeFieldIfNotNull(writer, key, value.value());
    }

    private void writeFieldIfNotNull(JsonWriter writer, String key, String value) {
        if (value != null) {
            writer.writeFieldName(key);
            writer.writeValue(value);
        }
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder implements IamPolicyWriter.Builder {
        private Boolean prettyPrint;

        private Builder() {
        }

        private Builder(DefaultIamPolicyWriter writer) {
            this.prettyPrint = writer.prettyPrint;
        }

        @Override
        public IamPolicyWriter.Builder prettyPrint(Boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        @Override
        public IamPolicyWriter build() {
            return new DefaultIamPolicyWriter(this);
        }
    }
}
