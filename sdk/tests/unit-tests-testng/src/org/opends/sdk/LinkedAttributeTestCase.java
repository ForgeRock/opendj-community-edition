/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2009-2010 Sun Microsystems, Inc.
 */

package org.opends.sdk;



import org.opends.sdk.schema.Schema;
import org.testng.Assert;
import org.testng.annotations.Test;



/**
 * Test {@code BasicAttribute}.
 */

public final class LinkedAttributeTestCase extends SdkTestCase
{
  @Test
  public void smokeTest() throws Exception
  {
    // TODO: write a proper test suite.
    final AbstractAttribute attribute = new LinkedAttribute(
        AttributeDescription.valueOf("ALTSERVER", Schema.getCoreSchema()));

    attribute.add(1);
    attribute.add("a value");
    attribute.add(ByteString.valueOf("another value"));

    Assert.assertTrue(attribute.contains(1));
    Assert.assertTrue(attribute.contains("a value"));
    Assert.assertTrue(attribute.contains(ByteString.valueOf("another value")));

    Assert.assertEquals(attribute.size(), 3);
    Assert.assertTrue(attribute.remove(1));
    Assert.assertEquals(attribute.size(), 2);
    Assert.assertFalse(attribute.remove("a missing value"));
    Assert.assertEquals(attribute.size(), 2);
    Assert.assertTrue(attribute.remove("a value"));
    Assert.assertEquals(attribute.size(), 1);
    Assert.assertTrue(attribute.remove(ByteString.valueOf("another value")));
    Assert.assertEquals(attribute.size(), 0);
  }
}