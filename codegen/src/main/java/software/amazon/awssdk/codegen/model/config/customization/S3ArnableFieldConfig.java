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

import java.util.Map;

/**
 * Indicating a field that can be an ARN
 */
public class S3ArnableFieldConfig {
    private String field;

    private String arnConverterFqcn;

    private String arnResourceFqcn;

    /**
     * The ARN field to be substituted set the value from the getter
     */
    private String arnResourceSubstitutionGetter;

    private String baseArnResourceFqcn;

    private String executionAttributeKeyFqcn;

    private String executionAttributeValueFqcn;

    /**
     * Contains the fields that need to be populated if null from the getter methods.
     *
     * The key is the field name and the value is the getter method in ARN which supplies the value
     */
    private Map<String, String> otherFieldsToPopulate;

    public String getField() {
        return field;
    }

    /**
     * Sets the field
     *
     * @param field The new field value.
     * @return This object for method chaining.
     */
    public S3ArnableFieldConfig setField(String field) {
        this.field = field;
        return this;
    }

    /**
     * @return the FQCN of the ArnConverter
     */
    public String getArnConverterFqcn() {
        return arnConverterFqcn;
    }

    /**
     * Sets the arnConverterFqcn
     *
     * @param arnConverterFqcn The new arnConverterFqcn value.
     * @return This object for method chaining.
     */
    public S3ArnableFieldConfig setArnConverterFqcn(String arnConverterFqcn) {
        this.arnConverterFqcn = arnConverterFqcn;
        return this;
    }

    public String getArnResourceFqcn() {
        return arnResourceFqcn;
    }

    /**
     * Sets the arnResourceFqcn
     *
     * @param arnResourceFqcn The new arnResourceFqcn value.
     * @return This object for method chaining.
     */
    public S3ArnableFieldConfig setArnResourceFqcn(String arnResourceFqcn) {
        this.arnResourceFqcn = arnResourceFqcn;
        return this;
    }

    public String getArnResourceSubstitutionGetter() {
        return arnResourceSubstitutionGetter;
    }

    /**
     * Sets the arnResourceSubstitutionGetter
     *
     * @param arnResourceSubstitutionGetter The new arnResourceSubstitutionGetter value.
     * @return This object for method chaining.
     */
    public S3ArnableFieldConfig setArnResourceSubstitutionGetter(String arnResourceSubstitutionGetter) {
        this.arnResourceSubstitutionGetter = arnResourceSubstitutionGetter;
        return this;
    }

    public Map<String, String> getOtherFieldsToPopulate() {
        return otherFieldsToPopulate;
    }

    /**
     * Sets the substitionSetterToGetter
     *
     * @param substitionSetterToGetter The new substitionSetterToGetter value.
     * @return This object for method chaining.
     */
    public S3ArnableFieldConfig setSubstitionSetterToGetter(Map<String, String> substitionSetterToGetter) {
        this.otherFieldsToPopulate = substitionSetterToGetter;
        return this;
    }

    public String getBaseArnResourceFqcn() {
        return baseArnResourceFqcn;
    }

    /**
     * Sets the baseArnResourceFqcn
     *
     * @param baseArnResourceFqcn The new baseArnResourceFqcn value.
     * @return This object for method chaining.
     */
    public S3ArnableFieldConfig setBaseArnResourceFqcn(String baseArnResourceFqcn) {
        this.baseArnResourceFqcn = baseArnResourceFqcn;
        return this;
    }

    public String getExecutionAttributeKeyFqcn() {
        return executionAttributeKeyFqcn;
    }

    /**
     * Sets the executionAttributeKeyFqcn
     *
     * @param executionAttributeKeyFqcn The new executionAttributeKeyFqcn value.
     * @return This object for method chaining.
     */
    public S3ArnableFieldConfig setExecutionAttributeKeyFqcn(String executionAttributeKeyFqcn) {
        this.executionAttributeKeyFqcn = executionAttributeKeyFqcn;
        return this;
    }

    public String getExecutionAttributeValueFqcn() {
        return executionAttributeValueFqcn;
    }

    /**
     * Sets the executionAttributeValueFqcn
     *
     * @param executionAttributeValueFqcn The new executionAttributeValueFqcn value.
     * @return This object for method chaining.
     */
    public S3ArnableFieldConfig setExecutionAttributeValueFqcn(String executionAttributeValueFqcn) {
        this.executionAttributeValueFqcn = executionAttributeValueFqcn;
        return this;
    }

    /**
     * Sets the otherFieldsToPopulate
     *
     * @param otherFieldsToPopulate The new otherFieldsToPopulate value.
     * @return This object for method chaining.
     */
    public S3ArnableFieldConfig setOtherFieldsToPopulate(Map<String, String> otherFieldsToPopulate) {
        this.otherFieldsToPopulate = otherFieldsToPopulate;
        return this;
    }
}
