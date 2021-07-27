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

package org.xwiki.contrib.docchangelog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.docchangelog.internal.DocChange;
import org.xwiki.contrib.docchangelog.internal.DocChangeStore;
import org.xwiki.contrib.docchangelog.xwiki.model.jaxb.DocumentLog;
import org.xwiki.contrib.docchangelog.xwiki.model.jaxb.DocumentLogs;
import org.xwiki.rest.XWikiResource;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id: 27d301df657f1738245f68a136a32e80d3aaa0f6 $
 */
@Component
@Named("org.xwiki.contrib.docchangelog.DocChangelogRESTResource")
@Path("/docchangelog")
@Singleton
public class DocChangelogRESTResource extends XWikiResource
{
    @Inject
    private DocChangeStore store;

    /**
     * @param start the date after which to get document changes
     * @param end the date before which to get document changes
     * @return the found document logs
     * @throws ParseException when failing to parse the provided dates
     * @throws XWikiException when failing to request the document logs
     */
    @GET
    public DocumentLogs get(@QueryParam("start") String start, @QueryParam("end") String end)
        throws ParseException, XWikiException
    {
        DocumentLogs logs = new DocumentLogs();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");

        Date startDate = start != null ? dateFormat.parse(start.trim()) : null;
        Date endDate = end != null ? dateFormat.parse(end.trim()) : null;

        if (startDate != null) {
            logs.setStart(toCalendar(startDate));
        }
        if (endDate != null) {
            logs.setEnd(toCalendar(endDate));
        }

        for (DocChange change : this.store.getChanges(startDate, endDate)) {
            DocumentLog log = new DocumentLog();

            log.setReference(change.getReference());
            log.setLocale(change.getLocale());
            log.setRealLocale(change.getRealLocale());
            log.setType(change.getType().toString());

            log.setDate(toCalendar(change.getDate()));

            logs.getLogs().add(log);
        }

        return logs;
    }

    private Calendar toCalendar(Date date)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar;
    }
}
