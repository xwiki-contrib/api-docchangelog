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

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.contrib.docchangelog.DocumentChangeType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * The store of documents changes logs.
 * 
 * @version $Id$
 */
@Component(roles = DocChangeStore.class)
@Singleton
public class DocChangeStore implements Disposable, Initializable
{
    private static final DocChange WAKEUP = new DocChange();

    private static final int QUEUE_CAPACITY = 1000;

    @Inject
    @Named(XWikiHibernateBaseStore.HINT)
    private Provider<XWikiStoreInterface> hibernateStoreProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private boolean disposed;

    private BlockingQueue<DocChange> queue;

    @Override
    public void initialize() throws InitializationException
    {
        this.queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

        Thread thread = new Thread(this::run);
        thread.setName("Document Change Logs storage");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        thread.start();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.disposed = true;
        this.queue.add(WAKEUP);
    }

    /**
     * Asynchronously store a new document change entry in the database.
     * 
     * @param reference the reference of the modified document
     * @param locale the locale of the modified document
     * @param date the date at which the document was modified
     * @param type the type of modification applied on the document
     */
    public void add(DocumentReference reference, Locale locale, Date date, DocumentChangeType type)
    {
        DocChange docChange = new DocChange();

        docChange.setReference(this.serializer.serialize(reference));
        docChange.setLocale(locale.toString());
        docChange.setDate(date);
        docChange.setType(type);

        this.queue.add(docChange);
    }

    private void run()
    {
        this.execution.setContext(new ExecutionContext());

        try {
            while (!this.disposed) {
                DocChange change;
                try {
                    change = this.queue.take();
                } catch (InterruptedException e) {
                    this.logger.warn("The thread handling document change log storage has been interrupted");

                    Thread.currentThread().interrupt();
                    break;
                }

                try {
                    storeChangesTasks(change);
                } catch (Throwable e) {
                    this.logger.error("Exception error when saving the document change logs", e);
                }
            }
        } finally {
            this.execution.removeContext();
        }
    }

    private void storeChangesTasks(DocChange firstChange) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore hibernateStore = (XWikiHibernateStore) this.hibernateStoreProvider.get();

        boolean transation = hibernateStore.beginTransaction(xcontext);

        try {
            DocChange change = firstChange;
            do {
                // Bulletproof: use saveOrUpdate instead of save to avoid crashing when a document is saved twice in
                // less that one second (which is the granularity of the timestamp in MySQL for example)
                hibernateStore.getSession(xcontext).saveOrUpdate(firstChange);

                change = this.queue.poll();
            } while (change != null);
        } finally {
            if (transation) {
                hibernateStore.endTransaction(xcontext, true);
            }
        }
    }

    /**
     * @param start the date after which to get document changes
     * @param end the date before which to get document changes
     * @return the list of document changes found in the database
     * @throws XWikiException when failing to request the documents changes
     */
    public List<DocChange> getChanges(Date start, Date end) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiHibernateStore hibernateStore = (XWikiHibernateStore) this.hibernateStoreProvider.get();

        boolean transation = hibernateStore.beginTransaction(xcontext);

        try {
            Session session = hibernateStore.getSession(xcontext);

            StringBuilder statement = new StringBuilder("SELECT change FROM DocChange AS change");

            if (start != null) {
                statement.append(" WHERE change.date >= :start");
                if (end != null) {
                    statement.append(" AND change.date < :end");
                }
            } else if (end != null) {
                statement.append(" WHERE change.date < :end");
            }

            statement.append(" ORDER BY change.date ASC");

            Query<DocChange> query = session.createQuery(statement.toString());

            if (start != null) {
                query.setParameter("start", start);
            }
            if (end != null) {
                query.setParameter("end", end);
            }

            return query.list();
        } finally {
            if (transation) {
                hibernateStore.endTransaction(xcontext, false);
            }
        }
    }
}
