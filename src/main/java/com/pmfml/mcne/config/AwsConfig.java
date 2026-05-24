package com.pmfml.mcne.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Configuration class for AWS services.
 * Initializes and exposes AWS clients (SES and SNS) as Spring Beans,
 * injecting credentials and region settings from the environment.
 */
@Configuration
public class AwsConfig {

  @Value("${aws.region}")
  private String region;

  @Value("${aws.accessKeyId}")
  private String accessKey;

  @Value("${aws.secretKey}")
  private String secretKey;

  @Bean
  public StaticCredentialsProvider awsCredentialsProvider() {
    return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
  }

  @Bean
  public SesClient sesClient() {
    return SesClient.builder()
        .region(Region.of(region))
        .credentialsProvider(awsCredentialsProvider())
        .build();
  }

  @Bean
  public SnsClient snsClient() {
    return SnsClient.builder()
        .region(Region.of(region))
        .credentialsProvider(awsCredentialsProvider())
        .build();
  }
}