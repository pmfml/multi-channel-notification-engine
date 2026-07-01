package com.pmfml.mcne.validation;

import com.pmfml.mcne.dtos.NotificationRequest;
import com.pmfml.mcne.enums.NotificationChannel;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class RecipientValidator implements ConstraintValidator<ValidRecipient, NotificationRequest> {

    // Simple email regex for general validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    
    // E.164 format for SMS (e.g. +1234567890 up to 15 digits)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+[1-9]\\d{1,14}$");

    @Override
    public boolean isValid(NotificationRequest request, ConstraintValidatorContext context) {
        if (request == null || request.channel() == null || request.recipient() == null || request.recipient().isBlank()) {
            // Let @NotNull and @NotBlank handle basic empty cases on the fields
            return true;
        }

        String recipient = request.recipient();
        NotificationChannel channel = request.channel();

        boolean isValid = false;

        if (channel == NotificationChannel.EMAIL) {
            isValid = EMAIL_PATTERN.matcher(recipient).matches();
            if (!isValid) {
                setCustomMessage(context, "Recipient must be a valid email address when channel is EMAIL");
            }
        } else if (channel == NotificationChannel.SMS) {
            isValid = PHONE_PATTERN.matcher(recipient).matches();
            if (!isValid) {
                setCustomMessage(context, "Recipient must be a valid E.164 phone number (e.g., +1234567890) when channel is SMS");
            }
        } else {
            // For PUSH or other future channels
            isValid = true;
        }

        return isValid;
    }

    private void setCustomMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
               .addPropertyNode("recipient")
               .addConstraintViolation();
    }
}
