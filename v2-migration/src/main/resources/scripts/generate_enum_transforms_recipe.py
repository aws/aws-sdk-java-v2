#  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License").
#  You may not use this file except in compliance with the License.
#  A copy of the License is located at
#
#   http://aws.amazon.com/apache2.0
#
#  or in the "license" file accompanying this file. This file is distributed
#  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
#  express or implied. See the License for the specific language governing
#  permissions and limitations under the License.

import os
from scripts.utils import load_model_file
from scripts.utils import write_copy_right_header
from scripts.utils import RECIPE_ROOT_DIR

ENUM_GETTERS_FILE_NAME = 'change-enum-getters.yml'
# Add services as needed
SERVICES_TO_TRANSFORM = ['sqs', 'sns', 'dynamodb']


def generate_enum_getters_transform_recipe():
    getters_recipe = os.path.join(RECIPE_ROOT_DIR, ENUM_GETTERS_FILE_NAME)

    with open(getters_recipe, 'w') as getters_file:
        write_copy_right_header(getters_file)
        write_getters_recipe_metadata(getters_file)

        for service in SERVICES_TO_TRANSFORM:
            model_data = load_model_file(service)
            shapes_with_enums = extract_shapes_with_enums(model_data)
            write_getters_recipe(getters_file, service, model_data, shapes_with_enums)


def write_getters_recipe_metadata(f):
    f.write('''---
type: specs.openrewrite.org/v1beta/recipe
name: software.amazon.awssdk.v2migration.EnumGettersToV2
displayName: Change v1 enum getters to v2
recipeList:''')


def extract_shapes_with_enums(model_data):
    shapes_with_enums = set()
    shapes = model_data.get("shapes", {})
    for shape_name, shape_data in shapes.items():
        if "enum" in shape_data:
            shapes_with_enums.add(shape_name)
    return shapes_with_enums


def write_getters_recipe(f, service, model_data, shapes_with_enums):
    collection_shapes_with_enum = set()
    shapes = model_data.get("shapes").items()
    for shape_name, shape_data in shapes:
        members = shape_data.get("members", {})
        for member_name, member_data in members.items():
            if member_data.get("shape") in shapes_with_enums:
                write_change_getters_recipe(f, service, shape_name, member_name, False)

        key_shape = shape_data.get("key", {}).get("shape")
        member_shape = shape_data.get("member", {}).get("shape")
        if key_shape in shapes_with_enums or member_shape in shapes_with_enums:
            if shape_data.get("type") in ["map", "list"]:
                collection_shapes_with_enum.add(shape_name)

    for shape_name, shape_data in shapes:
        members = shape_data.get("members", {})
        for member_name, member_data in members.items():
            if member_data.get("shape") in collection_shapes_with_enum:
                write_change_getters_recipe(f, service, shape_name, member_name, True)


def write_change_getters_recipe(f, service, pojo, getter, isCollection):
    if isCollection:
        suffix = "AsStrings"
    else:
        suffix = "AsString"

    v1_getter = "get" + getter
    v2_getter = lowercase_first_letter(getter) + suffix

    change_getter = '''
  - org.openrewrite.java.ChangeMethodName:
      methodPattern: com.amazonaws.services.{0}.model.{1} {2}()
      newMethodName: {3}'''
    f.write(change_getter.format(service, pojo, v1_getter, v2_getter))


def lowercase_first_letter(s):
    return s[0].lower() + s[1:]
