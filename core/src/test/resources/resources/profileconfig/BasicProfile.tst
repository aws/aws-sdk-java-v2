[default]
aws_access_key_id = defaultAccessKey
aws_secret_access_key = defaultSecretAccessKey

[profile-with-session-token]
aws_access_key_id = defaultAccessKey
aws_secret_access_key = defaultSecretAccessKey
aws_session_token = awsSessionToken

[profile-with-region]
region = us-east-1

[profile-with-assume-role]
source_profile=default
role_arn=arn:aws:iam::123456789012:role/testRole

[profile profile-with-profile-prefix]