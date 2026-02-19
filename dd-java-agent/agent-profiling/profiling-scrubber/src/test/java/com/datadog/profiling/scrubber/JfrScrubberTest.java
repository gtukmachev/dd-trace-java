package com.datadog.profiling.scrubber;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

class JfrScrubberTest {

  @TempDir Path tempDir;

  private Path inputFile;

  @BeforeEach
  void setUp() throws IOException {
    inputFile = tempDir.resolve("input.jfr");
    try (InputStream is = getClass().getResourceAsStream("/test-recording.jfr")) {
      if (is == null) {
        throw new IllegalStateException("test-recording.jfr not found in test resources");
      }
      Files.copy(is, inputFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  @Test
  void scrubInitialSystemPropertyValues() throws Exception {
    JfrScrubber scrubber = DefaultScrubDefinition.create(null);
    Path outputFile = tempDir.resolve("output.jfr");
    scrubber.scrubFile(inputFile, outputFile);

    assertTrue(Files.exists(outputFile));
    assertTrue(Files.size(outputFile) > 0, "Scrubbed file should not be empty");

    // Verify the scrubbed file is valid and parseable
    JfrLoaderToolkit.loadEvents(outputFile.toFile());
  }

  @Test
  void scrubWithNoMatchingEvents() throws Exception {
    // Scrubber with all default events excluded — nothing matches
    JfrScrubber scrubber = new JfrScrubber(name -> null);
    Path outputFile = tempDir.resolve("output.jfr");
    scrubber.scrubFile(inputFile, outputFile);

    // Output should be identical to input when no events match
    assertEquals(Files.size(inputFile), Files.size(outputFile));
  }

  @Test
  void scrubWithExcludedEventType() throws Exception {
    // Exclude jdk.InitialSystemProperty from scrubbing
    JfrScrubber scrubber =
        DefaultScrubDefinition.create(Collections.singletonList("jdk.InitialSystemProperty"));
    Path outputFile = tempDir.resolve("output.jfr");
    scrubber.scrubFile(inputFile, outputFile);

    assertTrue(Files.exists(outputFile));
    assertTrue(Files.size(outputFile) > 0);
  }
}
