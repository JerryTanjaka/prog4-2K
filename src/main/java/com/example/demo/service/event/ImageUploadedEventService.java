package com.example.demo.service.event;

import com.example.demo.endpoint.event.model.ImageUploadedEvent;
import com.example.demo.entity.ImageSubmission;
import com.example.demo.exception.ImageSubmissionNotFoundException;
import com.example.demo.file.bucket.BucketComponent;
import com.example.demo.file.image.GrayscaleConverter;
import com.example.demo.mail.Email;
import com.example.demo.mail.Mailer;
import com.example.demo.repository.ImageSubmissionRepository;
import jakarta.mail.internet.InternetAddress;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ImageUploadedEventService implements Consumer<ImageUploadedEvent> {

  private final BucketComponent bucketComponent;
  private final GrayscaleConverter grayscaleConverter;
  private final Mailer mailer;
  private final ImageSubmissionRepository imageSubmissionRepository;

  @SneakyThrows
  @Transactional
  @Override
  public void accept(ImageUploadedEvent event) {
    UUID id = UUID.fromString(event.getImageSubmissionId());
    ImageSubmission submission =
        imageSubmissionRepository
            .findById(id)
            .orElseThrow(() -> new ImageSubmissionNotFoundException(id));

    File original = bucketComponent.download(event.getBucketKey());
    File grayscale = grayscaleConverter.toGrayscale(original, event.getExtension());

    String bwBucketKey = "images/" + id + "/bw." + event.getExtension();
    bucketComponent.upload(grayscale, bwBucketKey);

    URL downloadUrl = bucketComponent.presign(bwBucketKey, Duration.ofDays(7));

    String htmlBody =
        "<html><body>"
            + "<h2>Votre image en noir et blanc est pr\u00EAte !</h2>"
            + "<p><a href=\""
            + downloadUrl
            + "\">T\u00E9l\u00E9charger l'image</a></p>"
            + "</body></html>";

    mailer.accept(
        new Email(
            new InternetAddress(event.getEmail()),
            List.of(),
            List.of(),
            "Votre image en noir et blanc",
            htmlBody,
            List.of()));

    submission.setBwBucketKey(bwBucketKey);
    imageSubmissionRepository.save(submission);

    original.delete();
    grayscale.delete();
  }
}
