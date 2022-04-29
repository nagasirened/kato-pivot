package com.kato.pro.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.kato.pro.aws.AwsEndpoint;
import com.kato.pro.aws.AwsProperties;
import com.kato.pro.aws.AwsTemplate;
import com.kato.pro.aws.DefaultAwsTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(prefix = "kato.aws", name = "enable", havingValue = "true", matchIfMissing = false)
public class AwsConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kato.aws")
    public AwsProperties awsProperties() {
        return awsProperties();
    }

    @Bean
    public AmazonS3 amazonS3(@Autowired AwsProperties awsProperties) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        AwsClientBuilder.EndpointConfiguration endpointConfiguration =
                new AwsClientBuilder.EndpointConfiguration(awsProperties.getEndpoint(), awsProperties.getRegion());
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(awsProperties.getAccessKey(), awsProperties.getSecretKey());
        AWSCredentialsProvider awsCredentialsProvider = new AWSStaticCredentialsProvider(awsCredentials);
        return AmazonS3Client.builder()
                .withEndpointConfiguration(endpointConfiguration)
                .withClientConfiguration(clientConfiguration)
                .withCredentials(awsCredentialsProvider)
                .disableChunkedEncoding()
                .withPathStyleAccessEnabled(awsProperties.getPathStyleAccess())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(value = {AwsProperties.class, AmazonS3.class})
    public AwsTemplate awsTemplate(@Autowired AwsProperties awsProperties,
                                   @Autowired AmazonS3 amazonS3) {
        return new DefaultAwsTemplate(awsProperties, amazonS3);
    }

    @Bean
    @ConditionalOnMissingBean
    public AwsEndpoint awsEndpoint(@Autowired AwsTemplate awsTemplate) {
        return new AwsEndpoint(awsTemplate);
    }
}
