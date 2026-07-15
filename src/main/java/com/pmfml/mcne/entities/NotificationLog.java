package com.pmfml.mcne.entities;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.pmfml.mcne.enums.NotificationChannel;
import com.pmfml.mcne.enums.NotificationStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 100)
  private String recipient;

  @Column(name = "message", length = 300)
  private String message;

  @Enumerated(EnumType.STRING)
  private NotificationChannel channel;

  @Enumerated(EnumType.STRING)
  private NotificationStatus status;

  @CreationTimestamp
  private Instant createdAt;

}
