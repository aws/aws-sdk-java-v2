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

package software.amazon.awssdk.enhanced.dynamodb.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Specifies that when calling {@link TableSchema#mapToItem(Map)}, a separate DynamoDB object that is stored in the current
 * object should be initialized as empty class if all fields in this class are null. Note that if this annotation is absent,
 * the object will be null.
 *
 * <p>
 * Example using {@link DynamoDbPreserveEmptyObject}:
 * <pre>
 * {@code
 * @DynamoDbBean
 * public class NestedBean {
 *     private AbstractBean innerBean1;
 *     private AbstractBean innerBean2;
 *
 *     @DynamoDbPreserveEmptyObject
 *     public AbstractBean getInnerBean1() {
 *         return innerBean1;
 *     }
 *     public void setInnerBean1(AbstractBean innerBean) {
 *         this.innerBean1 = innerBean;
 *     }
 *
 *     public AbstractBean getInnerBean2() {
 *         return innerBean;
 *     }
 *     public void setInnerBean2(AbstractBean innerBean) {
 *         this.innerBean2 = innerBean;
 *     }
 * }
 *
 * BeanTableSchema<NestedBean> beanTableSchema = BeanTableSchema.create(NestedBean.class);
 * AbstractBean innerBean1 = new AbstractBean();
 * AbstractBean innerBean2 = new AbstractBean();
 *
 * NestedBean bean = new NestedBean();
 * bean.setInnerBean1(innerBean1);
 * bean.setInnerBean2(innerBean2);
 *
 * Map<String, AttributeValue> itemMap = beanTableSchema.itemToMap(bean, true);
 * NestedBean nestedBean = beanTableSchema.mapToItem(itemMap);
 *
 * // innerBean1 w/ @DynamoDbPreserveEmptyObject is mapped to empty object
 * assertThat(nestedBean.getInnerBean1(), is(innerBean1));
 *
 * // innerBean2 w/o @DynamoDbPreserveEmptyObject is mapped to null.
 * assertThat(nestedBean.getInnerBean2(), isNull());
 * }
 * </pre>
 */
@SdkPublicApi
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamoDbPreserveEmptyObject {
}
