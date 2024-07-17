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
from scripts.utils import find_sdk_version
from scripts.utils import load_module_mappings
from scripts.utils import write_copy_right_header
from scripts.utils import RESOURCES_ROOT_DIR
from scripts.utils import RECIPE_ROOT_DIR

MAPPING_FILE_NAME = 'upgrade-sdk-dependencies.yml'
DIFF_CSV_NAME = 'v1-v2-service-mapping-diffs.csv'
SERVICE_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(os.path.join(__file__, "../../../../")))),
    'services'
)


def load_all_service_modules():
    service_mapping = {}
    for s in [s for s in os.listdir(SERVICE_DIR) if os.path.isdir(os.path.join(SERVICE_DIR, s))]:
        v1_equivalent = find_v1_equivalent(s)
        if v1_equivalent:
            service_mapping[s] = v1_equivalent
    return service_mapping


def find_v1_equivalent(s):
    filename = os.path.join(RESOURCES_ROOT_DIR, DIFF_CSV_NAME)
    mappings = load_module_mappings(filename)

    if s in mappings:
        if not mappings[s]:
            # v2 module does not exist in v1
            return ""
        else:
            # v2 module is named differently in v1
            return "aws-java-sdk-" + mappings[s]
    else:
        return "aws-java-sdk-" + s


def write_bom_recipe(f, version):
    change_bom = '''
  - org.openrewrite.java.dependencies.ChangeDependency:
      oldGroupId: com.amazonaws
      oldArtifactId: aws-java-sdk-bom
      newGroupId: software.amazon.awssdk
      newArtifactId: bom
      newVersion: {0}'''
    f.write(change_bom.format(version))


# Only add apache-client and netty-nio-client if ClientConfiguration is used
def add_http_client_dependencies_if_needed(f, version):
    add_dependencies_str = '''
  - org.openrewrite.java.dependencies.AddDependency:
      groupId: software.amazon.awssdk
      artifactId: apache-client
      version: {0}
      onlyIfUsing: com.amazonaws.ClientConfiguration
  - org.openrewrite.java.dependencies.AddDependency:
      groupId: software.amazon.awssdk
      artifactId: netty-nio-client
      version: {0}
      onlyIfUsing: com.amazonaws.ClientConfiguration'''
    f.write(add_dependencies_str.format(version))


def replace_core_dependencies(f, version):
    add_dependencies_str = '''
  - org.openrewrite.java.dependencies.ChangeDependency:
      oldGroupId: com.amazonaws
      oldArtifactId: aws-java-sdk-core
      newGroupId: software.amazon.awssdk
      newArtifactId: aws-core
      newVersion: {0}
      '''
    f.write(add_dependencies_str.format(version))


def write_cloudwatch_recipe(f, version):
    change_bom = '''
  - org.openrewrite.java.dependencies.ChangeDependency:
      oldGroupId: com.amazonaws
      oldArtifactId: aws-java-sdk-cloudwatch
      newGroupId: software.amazon.awssdk
      newArtifactId: cloudwatch
      newVersion: {0}'''
    f.write(change_bom.format(version))


def write_recipe_yml_file(service_mapping):
    filename = os.path.join(RECIPE_ROOT_DIR, MAPPING_FILE_NAME)
    version = find_sdk_version()
    with open(filename, 'w') as f:
        write_copy_right_header(f)
        write_recipe_metadata(f)
        add_http_client_dependencies_if_needed(f, version)
        replace_core_dependencies(f, version)
        write_bom_recipe(f, version)
        for s in service_mapping:
            # edge case : v1 contains modules: cloudwatch AND cloudwatchmetrics, which both map to cloudwatch in v2
            # (there is no cloudwatchmetrics module in v2)
            # service_mapping maps cloudwatch to cloudwatchmetrics, so we'll write cloudwatch-cloudwatch manually
            if (s == "cloudwatch"):
                write_cloudwatch_recipe(f, version)
            write_recipe(f, s, service_mapping, version)
    return filename


def write_recipe_metadata(f):
    f.write('''---
type: specs.openrewrite.org/v1beta/recipe
name: software.amazon.awssdk.v2migration.UpgradeSdkDependencies
displayName: Change v1 Maven/Gradle dependencies to v2
recipeList:
''')


def write_recipe(f, s, service_mapping, version):
    change_dependency_group_id_and_artifact_id = '''
  - org.openrewrite.java.dependencies.ChangeDependency:
      oldGroupId: com.amazonaws
      oldArtifactId: {0}
      newGroupId: software.amazon.awssdk
      newArtifactId: {1}
      newVersion: {2}'''
    f.write(change_dependency_group_id_and_artifact_id.format(service_mapping[s], s, version))


def generate_upgrade_sdk_dependencies_recipe():
    service_mapping = load_all_service_modules()
    write_recipe_yml_file(service_mapping)
