/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.docchangelog.internal;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.xwiki.contrib.docchangelog.DocumentChangeType;

/**
 * Represent a document change entry.
 * 
 * @version $Id$
 */
public class DocChange implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String reference;

    private String locale;

    private Date date;

    private DocumentChangeType type;

    /**
     * @param reference the reference of the modified document
     */
    public void setReference(String reference)
    {
        this.reference = reference;
    }

    /**
     * @return the reference of the modified document
     */
    public String getReference()
    {
        return this.reference;
    }

    /**
     * @return the locale of the document
     */
    public String getLocale()
    {
        return locale;
    }

    /**
     * @param locale the locale of the document
     */
    public void setLocale(String locale)
    {
        this.locale = locale;
    }

    /**
     * @param date the date of the modification
     */
    public void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * @return the date of the modification
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * @param type the type of change applied to the document
     */
    public void setType(DocumentChangeType type)
    {
        this.type = type;
    }

    /**
     * @return the type of change applied to the document
     */
    public DocumentChangeType getType()
    {
        return this.type;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof DocChange) {
            DocChange other = (DocChange) obj;

            EqualsBuilder builder = new EqualsBuilder();

            builder.append(getReference(), other.getReference());
            builder.append(getDate(), other.getDate());
            builder.append(getType(), other.getType());

            return builder.isEquals();
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(getReference());
        builder.append(getDate());
        builder.append(getType());

        return builder.toHashCode();
    }
}
