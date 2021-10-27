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

package software.amazon.awssdk.enhanced.dynamodb.mocktests;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Testpoly {





    public void processItem(Object object){
        System.out.println("Processing Object " +object.getClass().getSimpleName());
    }

    public void processItem(List object){
        System.out.println("Processing List " +object.getClass().getSimpleName());
    }


    public static void main(String[] args) {
        Testpoly testpoly = new Testpoly();

        Object list = new ArrayList<>();


        testpoly.processItem(classType(list).cast(list));




    }

    private static  <T> T castObject(Class<T> clazz, Object object) {
        return (T) object;
    }

    private static Class<List> classType(Object object) {
        return  List.class;
    }



}
