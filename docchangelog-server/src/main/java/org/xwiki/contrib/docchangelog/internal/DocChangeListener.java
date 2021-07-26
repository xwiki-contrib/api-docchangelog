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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.docchangelog.DocumentChangeType;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Listener to document changes and store them.
 * 
 * @version $Id$
 */
@Component
@Named(DocChangeListener.NAME)
@Singleton
public class DocChangeListener extends AbstractEventListener
{
    /**
     * The unique name of the listener.
     */
    public static final String NAME = "DocChangeListener";

    @Inject
    private Logger logger;

    @Inject
    private DocChangeStore store;

    /**
     * The default constructor.
     */
    public DocChangeListener()
    {
        super(NAME, new DocumentCreatedEvent(), new DocumentDeletedEvent(), new DocumentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentChangeType type;
        if (event instanceof DocumentCreatedEvent) {
            type = DocumentChangeType.CREATE;
        } else if (event instanceof DocumentDeletedEvent) {
            type = DocumentChangeType.DELETE;
        } else if (event instanceof DocumentUpdatedEvent) {
            type = DocumentChangeType.UPDATE;
        } else {
            this.logger.warn("Unsupported event type [{}]", event.getClass());

            return;
        }

        XWikiDocument document = (XWikiDocument) source;

        this.store.add(document.getDocumentReference(), document.getLocale(), document.getRealLocale(),
            document.getDate(), type);
    }
}
