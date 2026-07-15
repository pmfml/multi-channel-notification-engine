package com.pmfml.mcne.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;

import jakarta.validation.ConstraintValidatorContext;

class RecipientValidatorTest {

  private RecipientValidator validator;
  private ConstraintValidatorContext context;

  @BeforeEach
  void setUp() {
    validator = new RecipientValidator();
    context = mock(ConstraintValidatorContext.class);
    ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
    ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
    
    lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    lenient().when(builder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
  }

  @Test
  @DisplayName("Should pass when request or channel or recipient is null/blank (let other annotations handle it)")
  void shouldPassWhenFieldsAreEmpty() {
    NotificationRequest req1 = new NotificationRequest(null, "msg", NotificationChannel.EMAIL, Map.of());
    NotificationRequest req2 = new NotificationRequest("a@b.com", "msg", null, Map.of());
    
    assertThat(validator.isValid(null, context)).isTrue();
    assertThat(validator.isValid(req1, context)).isTrue();
    assertThat(validator.isValid(req2, context)).isTrue();
  }

  @Test
  @DisplayName("Should validate correct email addresses")
  void shouldValidateCorrectEmail() {
    NotificationRequest req = new NotificationRequest("test@example.com", "msg", NotificationChannel.EMAIL, Map.of());
    assertThat(validator.isValid(req, context)).isTrue();
  }

  @Test
  @DisplayName("Should reject invalid email addresses")
  void shouldRejectInvalidEmail() {
    NotificationRequest req = new NotificationRequest("not-an-email", "msg", NotificationChannel.EMAIL, Map.of());
    assertThat(validator.isValid(req, context)).isFalse();
    verify(context).disableDefaultConstraintViolation();
  }

  @Test
  @DisplayName("Should validate correct SMS numbers (E.164)")
  void shouldValidateCorrectSms() {
    NotificationRequest req = new NotificationRequest("+1234567890", "msg", NotificationChannel.SMS, Map.of());
    assertThat(validator.isValid(req, context)).isTrue();
  }

  @Test
  @DisplayName("Should reject invalid SMS numbers")
  void shouldRejectInvalidSms() {
    NotificationRequest req = new NotificationRequest("12345", "msg", NotificationChannel.SMS, Map.of());
    assertThat(validator.isValid(req, context)).isFalse();
    verify(context).disableDefaultConstraintViolation();
  }
  
  @Test
  @DisplayName("Should pass for other channels like PUSH")
  void shouldPassForOtherChannels() {
    NotificationRequest req = new NotificationRequest("device-token-123", "msg", NotificationChannel.PUSH, Map.of());
    assertThat(validator.isValid(req, context)).isTrue();
  }
}
