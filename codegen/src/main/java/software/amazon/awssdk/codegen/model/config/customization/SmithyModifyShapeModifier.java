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

package software.amazon.awssdk.codegen.model.config.customization;

/**
 * Smithy-native equivalent of {@link ModifyModelShapeModifier}. Drops C2J-specific
 * serialization fields ({@code marshallLocationName}, {@code unmarshallLocationName},
 * {@code ignoreDataTypeConversionFailures}) and uses Smithy trait concepts.
 */
public class SmithyModifyShapeModifier {

    /**
     * Apply @deprecated trait to the member.
     */
    private boolean deprecated;

    /**
     * Message for the @deprecated trait.
     */
    private String deprecatedMessage;

    /**
     * Whether the old name should get deprecated getters/setters.
     */
    private boolean existingNameDeprecated;

    /**
     * Rename the member (Smithy member name).
     */
    private String emitPropertyName;

    /**
     * Override enum name in generated code.
     */
    private String emitEnumName;

    /**
     * Override enum value in generated code.
     */
    private String emitEnumValue;

    /**
     * Alternate bean property name for generated code.
     */
    private String alternateBeanPropertyName;

    /**
     * Retarget the member to a different Smithy shape type.
     * Value is a Smithy shape type name (e.g., "string", "integer",
     * "bigDecimal") rather than a Java type name.
     */
    private String emitAsType;

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getDeprecatedMessage() {
        return deprecatedMessage;
    }

    public void setDeprecatedMessage(String deprecatedMessage) {
        this.deprecatedMessage = deprecatedMessage;
    }

    public boolean isExistingNameDeprecated() {
        return existingNameDeprecated;
    }

    public void setExistingNameDeprecated(boolean existingNameDeprecated) {
        this.existingNameDeprecated = existingNameDeprecated;
    }

    public String getEmitPropertyName() {
        return emitPropertyName;
    }

    public void setEmitPropertyName(String emitPropertyName) {
        this.emitPropertyName = emitPropertyName;
    }

    public String getEmitEnumName() {
        return emitEnumName;
    }

    public void setEmitEnumName(String emitEnumName) {
        this.emitEnumName = emitEnumName;
    }

    public String getEmitEnumValue() {
        return emitEnumValue;
    }

    public void setEmitEnumValue(String emitEnumValue) {
        this.emitEnumValue = emitEnumValue;
    }

    public String getAlternateBeanPropertyName() {
        return alternateBeanPropertyName;
    }

    public void setAlternateBeanPropertyName(String alternateBeanPropertyName) {
        this.alternateBeanPropertyName = alternateBeanPropertyName;
    }

    public String getEmitAsType() {
        return emitAsType;
    }

    public void setEmitAsType(String emitAsType) {
        this.emitAsType = emitAsType;
    }
}
