package com.kato.pro.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.kato.pro.aws.AwsEndpoint;
import com.kato.pro.aws.DefaultAwsTemplate;
import com.kato.pro.aws.S3Properties;
import com.kato.pro.aws.S3Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnProperty(prefix = "kato.s3", name = "enable", havingValue = "true")
public class AwsConfiguration {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "kato.s3")
    public S3Properties awsProperties() {
        return new S3Properties();
    }

    @Bean
    @ConditionalOnMissingBean(value = AmazonS3.class)
    public AmazonS3 amazonS3(@Autowired S3Properties awsProperties) {
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
    @ConditionalOnBean(value = {S3Properties.class, AmazonS3.class})
    public S3Template awsTemplate(@Autowired AmazonS3 amazonS3) {
        return new DefaultAwsTemplate(amazonS3);
    }

    @Bean
    @ConditionalOnMissingBean
    public AwsEndpoint awsEndpoint(@Autowired S3Template s3Template) {
        return new AwsEndpoint(s3Template);
    }
}
