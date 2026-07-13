package com.example.demo.exception;

import java.util.UUID;

public class ImageSubmissionNotFoundException extends ResourceNotFoundException {

  public ImageSubmissionNotFoundException(UUID id) {
    super("Image submission with id " + id + " was not found");
  }
}
