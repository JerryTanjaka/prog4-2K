package com.example.demo.file.image;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class GrayscaleConverter {

  @SneakyThrows
  public File toGrayscale(File input, String extension) {
    BufferedImage original = ImageIO.read(input);
    if (original == null) {
      throw new RuntimeException("Unable to read image file: " + input.getAbsolutePath());
    }

    BufferedImage grayImage =
        new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
    Graphics graphics = grayImage.getGraphics();
    graphics.drawImage(original, 0, 0, null);
    graphics.dispose();

    File output = File.createTempFile("bw-", "." + extension);
    ImageIO.write(grayImage, extension, output);
    return output;
  }
}
