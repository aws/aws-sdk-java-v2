#!/bin/bash

dl='getHagridFile'
ul='uploadHagridFile'

key='1TB'
path="/mnt/raid0/$key"

mvn test -pl :s3-transfer-manager \
    -Dtest=software.amazon.awssdk.transfer.s3.HagridTest#$ul \
    -Dtestpath=$path \
    -Dtestkey=$key