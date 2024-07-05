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

package software.amazon.awssdk.enhanced.dynamodb.mapper.testbeans;

import java.util.Objects;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class RelativeBean {
    private String attribute2;
    private AddressBean address;

    public String getAttribute2() {
        return attribute2;
    }
    public void setAttribute2(String attribute2) {
        this.attribute2 = attribute2;
    }

    public AddressBean getAddress() {
        return address;
    }
    public void setAddress(AddressBean address) {
        this.address = address;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RelativeBean that = (RelativeBean) o;

        return Objects.equals(this, that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute2, address);
    }
}
