/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for flattening a complex type.
 *
 * <pre class="brush: java">
 * &#064;DynamoDBFlattened(attributes={
 *     &#064;DynamoDBAttribute(mappedBy=&quot;start&quot;, attributeName=&quot;effectiveStartDate&quot;),
 *     &#064;DynamoDBAttribute(mappedBy=&quot;end&quot;, attributeName=&quot;effectiveEndDate&quot;)})
 * public DateRange getEffectiveRange() { return effectiveRange; }
 * public void setEffectiveRange(DateRange effectiveRange) { this.effectiveRange = effectiveRange; }
 *
 * &#064;DynamoDBFlattened(attributes={
 *     &#064;DynamoDBAttribute(mappedBy=&quot;start&quot;, attributeName=&quot;extensionstartDate&quot;),
 *     &#064;DynamoDBAttribute(mappedBy=&quot;end&quot;, attributeName=&quot;extensionEndDate&quot;)})
 * public DateRange getExtensionRange() { return extensionRange; }
 * public void setExtensionRange(DateRange extensionRange) { this.extensionRange = extensionRange; }
 * </pre>
 *
 * <p>Where,</p>
 * <pre class="brush: java">
 * public class DateRange {
 *     private Date start;
 *     private Date end;
 *
 *     public Date start() { return start; }
 *     public void setStart(Date start) { this.start = start; }
 *
 *     public Date getEnd() { return end; }
 *     public void setEnd(Date end) { this.end = end; }
 * }
 * </pre>
 *
 * <p>Attributes defined within the complex type may also be annotated.</p>
 *
 * <p>May be used as a meta-annotation.</p>
 */
@DynamoDb
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface DynamoDbFlattened {

    /**
     * Indicates the attributes that should be flattened.
     */
    DynamoDbAttribute[] attributes() default {};

}
