package com.example.demo.endpoint.rest.controller;

import com.example.demo.endpoint.event.EventProducer;
import com.example.demo.endpoint.event.model.ImageUploadedEvent;
import com.example.demo.endpoint.rest.model.ImageSubmissionResponse;
import com.example.demo.entity.ImageSubmission;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ImageSubmissionNotFoundException;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.repository.ImageSubmissionRepository;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
public class ImageSubmissionController {

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
  private static final Duration NOIR_URL_DURATION = Duration.ofMinutes(10);

  private final ImageSubmissionRepository imageSubmissionRepository;
  private final BucketComponent bucketComponent;
  private final EventProducer<ImageUploadedEvent> eventProducer;

  @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @SneakyThrows
  public ImageSubmissionResponse submitImage(
      @RequestParam String email, @RequestParam("file") MultipartFile file) {

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new BadRequestException("Only JPEG or PNG images are accepted");
    }

    ImageSubmission submission = new ImageSubmission();
    submission.setEmail(email);
    submission.setContentType(contentType);
    ImageSubmission saved = imageSubmissionRepository.save(submission);

    String extension = "image/png".equals(contentType) ? "png" : "jpg";
    String originalBucketKey = "images/" + saved.getId() + "/original." + extension;

    File tempFile = File.createTempFile("upload-", "." + extension);
    file.transferTo(tempFile);
    bucketComponent.upload(tempFile, originalBucketKey);
    tempFile.delete();

    saved.setOriginalBucketKey(originalBucketKey);
    imageSubmissionRepository.save(saved);

    eventProducer.accept(
        List.of(
            ImageUploadedEvent.builder()
                .imageSubmissionId(saved.getId().toString())
                .email(email)
                .bucketKey(originalBucketKey)
                .extension(extension)
                .build()));

    return toResponse(saved);
  }

  @GetMapping("/images")
  public List<ImageSubmissionResponse> findAll() {
    return imageSubmissionRepository.findAll().stream().map(this::toResponse).toList();
  }

  @GetMapping("/images/{id}/noir")
  public ResponseEntity<Void> getBlackAndWhiteImage(@PathVariable UUID id) {
    ImageSubmission submission =
        imageSubmissionRepository
            .findById(id)
            .orElseThrow(() -> new ImageSubmissionNotFoundException(id));

    if (submission.getBwBucketKey() == null) {
      throw new BadRequestException("The black and white image is not ready yet for id=" + id);
    }

    URL presignedUrl = bucketComponent.presign(submission.getBwBucketKey(), NOIR_URL_DURATION);

    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(presignedUrl.toString()))
        .build();
  }

  private ImageSubmissionResponse toResponse(ImageSubmission submission) {
    return ImageSubmissionResponse.builder()
        .id(submission.getId())
        .email(submission.getEmail())
        .createdAt(submission.getCreatedAt())
        .blackAndWhiteReady(submission.getBwBucketKey() != null)
        .build();
  }
}
