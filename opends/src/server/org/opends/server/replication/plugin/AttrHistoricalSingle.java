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
 *      Copyright 2008-2010 Sun Microsystems, Inc.
 */
package org.opends.server.replication.plugin;

import java.util.ArrayList;
import java.util.Iterator;

import org.opends.server.replication.common.ChangeNumber;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.Entry;
import org.opends.server.types.Modification;
import org.opends.server.types.ModificationType;

/**
 * This classes is used to store historical information for single valued
 * attributes.
 * One object of this type is created for each attribute that was changed in
 * the entry.
 * It allows to record the last time a given value was added,
 * and the last time the whole attribute was deleted.
 */
public class AttrHistoricalSingle extends AttrHistorical
{
  private ChangeNumber deleteTime = null; // last time when the attribute was
                                          // deleted
  private ChangeNumber addTime = null;    // last time when a value was added
  private AttributeValue value = null;    // last added value

  /**
   * {@inheritDoc}
   */
  @Override
  public ChangeNumber getDeleteTime()
  {
    return this.deleteTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ArrayList<AttrValueHistorical> getValuesHistorical()
  {
    if (addTime == null)
    {
      return new ArrayList<AttrValueHistorical>();
    }
    else
    {
      ArrayList<AttrValueHistorical> values =
        new ArrayList<AttrValueHistorical>();
      values.add(new AttrValueHistorical(value, addTime, null));
      return values;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void processLocalOrNonConflictModification(ChangeNumber changeNumber,
      Modification mod)
  {
    AttributeValue newValue = null;
    Attribute modAttr = mod.getAttribute();
    if (modAttr != null && !modAttr.isEmpty())
    {
      newValue = modAttr.iterator().next();
    }

    switch (mod.getModificationType())
    {
    case DELETE:
      this.deleteTime = changeNumber;
      this.value = newValue;
      break;

    case ADD:
      this.addTime = changeNumber;
      this.value = newValue;
      break;

    case REPLACE:
      if (newValue == null)
      {
        // REPLACE with null value is actually a DELETE
        this.deleteTime = changeNumber;
      }
      else
      {
        this.deleteTime = addTime = changeNumber;
      }
      this.value = newValue;
      break;

    case INCREMENT:
      /* FIXME : we should update ChangeNumber */
      break;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean replayOperation(Iterator<Modification> modsIterator,
      ChangeNumber changeNumber, Entry modifiedEntry, Modification mod)
  {
    boolean conflict = false;

    AttributeValue newValue = null;
    Attribute modAttr = mod.getAttribute();
    if (modAttr != null && !modAttr.isEmpty())
    {
      newValue = modAttr.iterator().next();
    }

    switch (mod.getModificationType())
    {
    case DELETE:
      if ((changeNumber.newer(addTime)) &&
          ((newValue == null) ||
              ((newValue != null) && (newValue.equals(value))) ||
              (value == null)))
      {
        if (changeNumber.newer(deleteTime))
          deleteTime = changeNumber;
        AttributeType type = modAttr.getAttributeType();
        if (!modifiedEntry.hasAttribute(type))
        {
          conflict = true;
          modsIterator.remove();
        }
        else if ((newValue != null) &&
            (!modifiedEntry.hasValue(type, modAttr.getOptions(), newValue)))
        {
          conflict = true;
          modsIterator.remove();
        }
      }
      else
      {
        conflict = true;
        modsIterator.remove();
      }
      break;

    case ADD:
      if (changeNumber.newerOrEquals(deleteTime) && changeNumber.older(addTime))
      {
        conflict = true;
        mod.setModificationType(ModificationType.REPLACE);
        addTime = changeNumber;
        value = newValue;
      }
      else
      {
        if (changeNumber.newerOrEquals(deleteTime)
            && ((addTime == null ) || addTime.older(deleteTime)))
        {
          // no conflict : don't do anything beside setting the addTime
          addTime = changeNumber;
          value = newValue;
        }
        else
        {
          conflict = true;
          modsIterator.remove();
        }
      }

      break;

    case REPLACE:
      if ((changeNumber.older(deleteTime)) && (changeNumber.older(deleteTime)))
      {
        conflict = true;
        modsIterator.remove();
      }
      else
      {
        if (newValue == null)
        {
          value = newValue;
          deleteTime = changeNumber;
        }
        else
        {
          addTime = changeNumber;
          value = newValue;
          deleteTime = changeNumber;
        }
      }
      break;

    case INCREMENT:
      /* FIXME : we should update ChangeNumber */
      break;
    }
    return conflict;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void assign(HistAttrModificationKey histKey,
      AttributeValue value, ChangeNumber cn)
  {
    switch (histKey)
    {
    case ADD:
      this.addTime = cn;
      this.value = value;
      break;

    case DEL:
      this.deleteTime = cn;
      if (value != null)
        this.value = value;
      break;

    case REPL:
      this.addTime = this.deleteTime = cn;
      if (value != null)
        this.value = value;
      break;

    case DELATTR:
      this.deleteTime = cn;
      break;
    }
  }
}
