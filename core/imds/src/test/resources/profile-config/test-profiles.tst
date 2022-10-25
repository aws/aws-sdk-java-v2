[default]
aws_secret_access_key = defaultAccessKey
aws_access_key_id = defaultSecretAccessKey
region = us-west-2


[profile testIPv4]
ec2_metadata_service_endpoint_mode = IPv4
ec2_metadata_service_endpoint = http://42.42.42.42

[profile testIPv6]
ec2_metadata_service_endpoint_mode = IPv6
ec2_metadata_service_endpoint = [1234:ec2::456]
