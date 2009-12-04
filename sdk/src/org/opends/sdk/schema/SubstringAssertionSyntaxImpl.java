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
 *      Copyright 2009 Sun Microsystems, Inc.
 */

package org.opends.sdk.schema;



import static org.opends.sdk.schema.SchemaConstants.EMR_CASE_IGNORE_OID;
import static org.opends.sdk.schema.SchemaConstants.OMR_CASE_IGNORE_OID;
import static org.opends.sdk.schema.SchemaConstants.SMR_CASE_IGNORE_OID;
import static org.opends.sdk.schema.SchemaConstants.SYNTAX_SUBSTRING_ASSERTION_NAME;

import com.sun.opends.sdk.util.MessageBuilder;
import com.sun.opends.sdk.util.Messages;
import org.opends.sdk.util.ByteSequence;



/**
 * This class defines the substring assertion attribute syntax, which
 * contains one or more substring components, as used in a substring
 * search filter. For the purposes of matching, it will be treated like
 * a Directory String syntax except that approximate matching will not
 * be allowed.
 */
final class SubstringAssertionSyntaxImpl extends AbstractSyntaxImpl
{

  @Override
  public String getEqualityMatchingRule()
  {
    return EMR_CASE_IGNORE_OID;
  }



  public String getName()
  {
    return SYNTAX_SUBSTRING_ASSERTION_NAME;
  }



  @Override
  public String getOrderingMatchingRule()
  {
    return OMR_CASE_IGNORE_OID;
  }



  @Override
  public String getSubstringMatchingRule()
  {
    return SMR_CASE_IGNORE_OID;
  }



  public boolean isHumanReadable()
  {
    return true;
  }



  /**
   * Indicates whether the provided value is acceptable for use in an
   * attribute with this syntax. If it is not, then the reason may be
   * appended to the provided buffer.
   * 
   * @param schema
   *          The schema in which this syntax is defined.
   * @param value
   *          The value for which to make the determination.
   * @param invalidReason
   *          The buffer to which the invalid reason should be appended.
   * @return <CODE>true</CODE> if the provided value is acceptable for
   *         use with this syntax, or <CODE>false</CODE> if not.
   */
  public boolean valueIsAcceptable(Schema schema, ByteSequence value,
      MessageBuilder invalidReason)
  {
    // Get the string representation of the value and check its length.
    // A zero-length value is acceptable. A one-length value is
    // acceptable as long as it is not an asterisk. For all other
    // lengths, just ensure that there are no consecutive wildcards.
    final String valueString = value.toString();
    final int valueLength = valueString.length();
    if (valueLength == 0)
    {
      return true;
    }
    else if (valueLength == 1)
    {
      if (valueString.charAt(0) == '*')
      {
        invalidReason
            .append(Messages.WARN_ATTR_SYNTAX_SUBSTRING_ONLY_WILDCARD
                .get());

        return false;
      }
      else
      {
        return true;
      }
    }
    else
    {
      for (int i = 1; i < valueLength; i++)
      {
        if (valueString.charAt(i) == '*'
            && valueString.charAt(i - 1) == '*')
        {
          invalidReason
              .append(Messages.WARN_ATTR_SYNTAX_SUBSTRING_CONSECUTIVE_WILDCARDS
                  .get(valueString, i));
          return false;
        }
      }

      return true;
    }
  }
}