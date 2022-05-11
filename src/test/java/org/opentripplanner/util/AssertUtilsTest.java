package org.opentripplanner.util;

import static org.junit.Assert.fail;

import org.junit.Test;

public class AssertUtilsTest {

  @Test
  public void assertValueExist() {
    // Ok if any value
    AssertUtils.assertHasValue("a");

    // Should fail for these values
    var illegalValues = new String[] { null, "", " ", "\t", " \n\r\t\f" };

    for (var it : illegalValues) {
      try {
        AssertUtils.assertHasValue(it);
        fail("Assertion is expected to throw an exception for value: " + it);
      } catch (IllegalArgumentException expected) {}
    }
  }
}
