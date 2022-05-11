package org.opentripplanner.ext.interactivelauncher;

import static org.opentripplanner.standalone.config.ConfigLoader.isConfigFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Search for directories containing OTP configuration files. The search is recursive and searches
 * sub-directories 10 levels deep.
 */
class SearchForOtpConfig {

  private static final int DEPTH_LIMIT = 10;
  private static final Pattern EXCLUDE_DIR = Pattern.compile(
    "(otp1|archive|\\..*|te?mp|target|docs?|src|source|resource)"
  );

  static List<File> search(File rootDir) {
    return recursiveSearch(rootDir, DEPTH_LIMIT).collect(Collectors.toUnmodifiableList());
  }

  @SuppressWarnings("ConstantConditions")
  private static Stream<File> recursiveSearch(File dir, final int depthLimit) {
    if (!dir.isDirectory() || depthLimit == 0) {
      return Stream.empty();
    }
    if (EXCLUDE_DIR.matcher(dir.getName()).matches()) {
      return Stream.empty();
    }

    if (isOtpConfigDataDir(dir)) {
      return Stream.of(dir);
    }

    final int newDepthLimit = depthLimit - 1;
    File[] files = dir.listFiles();

    if (files == null) {
      return Stream.empty();
    }

    return Arrays.stream(files).flatMap(f -> recursiveSearch(f, newDepthLimit));
  }

  @SuppressWarnings("ConstantConditions")
  private static boolean isOtpConfigDataDir(File dir) {
    File[] files = dir.listFiles();
    if (files == null) {
      return false;
    }

    for (File f : files) {
      if (isConfigFile(f.getName())) {
        return true;
      }
    }
    return false;
  }
}
