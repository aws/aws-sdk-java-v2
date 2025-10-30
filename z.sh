#!/bin/bash

#
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#  http://aws.amazon.com/apache2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
#

s3_dl='s3_download'
s3_ul='s3_upload'
tm_dl='tm_download'
tm_ul='tm_upload'

# /mnt/raid0/1TiB
key='1TiB-1761661274568'
path="/mnt/raid0/$key"
test=$tm_dl

mvn test -pl :s3-transfer-manager \
    -Dtest=software.amazon.awssdk.transfer.s3.ExpressTest#$test \
    -Dtestpath=$path \
    -Dtestkey=$key
