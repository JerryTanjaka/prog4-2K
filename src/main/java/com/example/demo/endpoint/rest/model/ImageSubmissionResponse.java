package com.example.demo.endpoint.rest.model;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageSubmissionResponse {
  private UUID id;
  private String email;
  private LocalDateTime createdAt;
  private boolean blackAndWhiteReady;
}
