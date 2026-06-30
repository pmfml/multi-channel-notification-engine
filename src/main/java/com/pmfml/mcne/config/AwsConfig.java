package com.pmfml.mcne.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Configuration class for AWS services.
 *
 * <p>Credential resolution strategy (in order of preference):
 * <ol>
 *   <li>{@link DefaultCredentialsProvider} — honours IAM roles, instance profiles,
 *       environment variables, and the standard AWS credentials file. This is the
 *       expected path in staging/production.</li>
 *   <li>Static fallback — if the default chain fails <em>and</em> explicit
 *       {@code aws.accessKeyId} / {@code aws.secretKey} properties are set (non-blank),
 *       a {@link StaticCredentialsProvider} is used. Intended for local development
 *       only; never set real credentials in a committed config file.</li>
 * </ol>
 */
@Configuration
public class AwsConfig {

  private static final Logger log = LoggerFactory.getLogger(AwsConfig.class);

  @Value("${aws.region}")
  private String region;

  @Value("${aws.accessKeyId:}")
  private String accessKey;

  @Value("${aws.secretKey:}")
  private String secretKey;

  @Bean
  AwsCredentialsProvider awsCredentialsProvider() {
    boolean hasExplicitKeys = !accessKey.isBlank() && !secretKey.isBlank()
        && !"none".equalsIgnoreCase(accessKey);

    if (hasExplicitKeys) {
      log.warn("Using static AWS credentials from configuration. Use IAM roles/instance profiles in production.");
      return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    log.info("Using DefaultCredentialsProvider for AWS authentication.");
    return DefaultCredentialsProvider.create();
  }

  @Bean
  SesClient sesClient(AwsCredentialsProvider credentialsProvider) {
    return SesClient.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider)
        .build();
  }

  @Bean
  SnsClient snsClient(AwsCredentialsProvider credentialsProvider) {
    return SnsClient.builder()
        .region(Region.of(region))
        .credentialsProvider(credentialsProvider)
        .build();
  }
}