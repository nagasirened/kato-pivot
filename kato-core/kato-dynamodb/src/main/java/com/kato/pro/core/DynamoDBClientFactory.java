package com.kato.pro.core;

import com.kato.pro.pool.AbstractKeyedPooledObjectFactory;
import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Getter
public class DynamoDBClientFactory extends AbstractKeyedPooledObjectFactory<DynamoDbClient> {

    private final DynamoProperties dynamoProperties;
    public DynamoDBClientFactory(DynamoProperties dynamoProperties) {
        this.dynamoProperties = dynamoProperties;
    }

    @Override
    public DynamoDbClient generalClient(String type) {
        StaticCredentialsProvider provider = StaticCredentialsProvider.create(AwsBasicCredentials.create(dynamoProperties.getKey(), dynamoProperties.getSecret()));
        return DynamoDbClient.builder()
                .credentialsProvider(provider)
                .region(Region.of(dynamoProperties.getRegion()))
                .build();
    }
}
