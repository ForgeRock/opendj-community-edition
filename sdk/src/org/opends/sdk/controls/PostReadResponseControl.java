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

package org.opends.sdk.controls;



import static com.sun.opends.sdk.messages.Messages.ERR_POSTREADRESP_CANNOT_DECODE_VALUE;
import static com.sun.opends.sdk.messages.Messages.ERR_POSTREADRESP_NO_CONTROL_VALUE;
import static com.sun.opends.sdk.messages.Messages.ERR_POSTREAD_CONTROL_BAD_OID;

import java.io.IOException;

import org.opends.sdk.*;
import org.opends.sdk.asn1.ASN1;
import org.opends.sdk.asn1.ASN1Reader;
import org.opends.sdk.asn1.ASN1Writer;
import org.opends.sdk.responses.Responses;
import org.opends.sdk.responses.SearchResultEntry;

import com.sun.opends.sdk.ldap.LDAPUtils;
import com.sun.opends.sdk.util.StaticUtils;
import com.sun.opends.sdk.util.Validator;



/**
 * The post-read response control as defined in RFC 4527. This control is
 * returned by the server in response to a successful update operation which
 * included a post-read request control. The control contains a Search Result
 * Entry containing, subject to access controls and other constraints, values of
 * the requested attributes.
 *
 * @see PostReadRequestControl
 * @see <a href="http://tools.ietf.org/html/rfc4527">RFC 4527 - Lightweight
 *      Directory Access Protocol (LDAP) Read Entry Controls </a>
 */
public final class PostReadResponseControl implements Control
{
  /**
   * The IANA-assigned OID for the LDAP post-read response control used for
   * retrieving an entry in the state it had immediately after an update was
   * applied.
   */
  public static final String OID = PostReadRequestControl.OID;

  /**
   * A decoder which can be used for decoding the post-read response control.
   */
  public static final ControlDecoder<PostReadResponseControl> DECODER =
    new ControlDecoder<PostReadResponseControl>()
  {

    public PostReadResponseControl decodeControl(final Control control,
        final DecodeOptions options) throws DecodeException
    {
      Validator.ensureNotNull(control);

      if (control instanceof PostReadResponseControl)
      {
        return (PostReadResponseControl) control;
      }

      if (!control.getOID().equals(OID))
      {
        final LocalizableMessage message = ERR_POSTREAD_CONTROL_BAD_OID.get(
            control.getOID(), OID);
        throw DecodeException.error(message);
      }

      if (!control.hasValue())
      {
        // The control must always have a value.
        final LocalizableMessage message = ERR_POSTREADRESP_NO_CONTROL_VALUE
            .get();
        throw DecodeException.error(message);
      }

      final ASN1Reader reader = ASN1.getReader(control.getValue());
      SearchResultEntry searchEntry;
      try
      {
        searchEntry = LDAPUtils.decodeSearchResultEntry(reader, options);
      }
      catch (final IOException le)
      {
        StaticUtils.DEBUG_LOG.throwing("PostReadResponseControl",
            "decodeControl", le);

        final LocalizableMessage message = ERR_POSTREADRESP_CANNOT_DECODE_VALUE
            .get(le.getMessage());
        throw DecodeException.error(message, le);
      }

      /**
       * FIXME: the RFC states that the control contains a SearchResultEntry
       * rather than an Entry. Can we assume that the response will not contain
       * a nested set of controls?
       */
      return new PostReadResponseControl(control.isCritical(), Types
          .unmodifiableEntry(searchEntry));
    }



    public String getOID()
    {
      return OID;
    }
  };



  /**
   * Creates a new post-read response control.
   *
   * @param entry
   *          The entry whose contents reflect the state of the updated entry
   *          immediately after the update operation was performed.
   * @return The new control.
   * @throws NullPointerException
   *           If {@code entry} was {@code null}.
   */
  public static PostReadResponseControl newControl(final Entry entry)
      throws NullPointerException
  {
    /**
     * FIXME: all other control implementations are fully immutable. We should
     * really do a defensive copy here in order to be consistent, rather than
     * just wrap it. Also, the RFC states that the control contains a
     * SearchResultEntry rather than an Entry. Can we assume that the response
     * will not contain a nested set of controls?
     */
    return new PostReadResponseControl(false, Types.unmodifiableEntry(entry));
  }



  private final Entry entry;

  private final boolean isCritical;



  private PostReadResponseControl(final boolean isCritical, final Entry entry)
  {
    this.isCritical = isCritical;
    this.entry = entry;
  }



  /**
   * Returns an unmodifiable entry whose contents reflect the state of the
   * updated entry immediately after the update operation was performed.
   *
   * @return The unmodifiable entry whose contents reflect the state of the
   *         updated entry immediately after the update operation was performed.
   */
  public Entry getEntry()
  {
    return entry;
  }



  /**
   * {@inheritDoc}
   */
  public String getOID()
  {
    return OID;
  }



  /**
   * {@inheritDoc}
   */
  public ByteString getValue()
  {
    final ByteStringBuilder buffer = new ByteStringBuilder();
    final ASN1Writer writer = ASN1.getWriter(buffer);
    try
    {
      LDAPUtils.encodeSearchResultEntry(writer, Responses
          .newSearchResultEntry(entry));
      return buffer.toByteString();
    }
    catch (final IOException ioe)
    {
      // This should never happen unless there is a bug somewhere.
      throw new RuntimeException(ioe);
    }
  }



  /**
   * {@inheritDoc}
   */
  public boolean hasValue()
  {
    return true;
  }



  /**
   * {@inheritDoc}
   */
  public boolean isCritical()
  {
    return isCritical;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("PostReadResponseControl(oid=");
    builder.append(getOID());
    builder.append(", criticality=");
    builder.append(isCritical());
    builder.append(", entry=");
    builder.append(entry);
    builder.append(")");
    return builder.toString();
  }
}