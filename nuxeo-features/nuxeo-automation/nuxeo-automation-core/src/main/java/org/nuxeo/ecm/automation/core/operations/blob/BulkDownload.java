/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.AsyncBlob;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;

/**
 * Asynchrnous Bulk Download Operation.
 * 
 * @since 9.3
 */
@Operation(id = BulkDownload.ID, category = Constants.CAT_BLOB, label = "Bulk Downlaod", description = "Prepare a Zip of a list of documents which is build asynchrously. Produced Zip will be available in the TransientStore with the key returned by the AsyncBlob.")
public class BulkDownload {

    public static final String ID = "Blob.BulkDownload";

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    public static final String WORKERID_KEY = "workerid";

    @Context
    protected CoreSession session;

    @Param(name = "filename", required = false)
    protected String fileName;

    protected String buildTransientStoreKey(DocumentModelList docs) {
        StringBuffer sb = new StringBuffer();
        for (DocumentModel doc : docs) {
            sb.append(doc.getId());
            sb.append("::");
            Calendar modif = (Calendar) doc.getPropertyValue("dc:modified");
            if (modif != null) {
                long millis = modif.getTimeInMillis();
                // the date may have been rounded by the storage layer, normalize it to the second
                millis -= millis % 1000;
                sb.append(millis);
                sb.append("::");
            }
        }
        return getDigest(sb.toString());
    }

    protected String getDigest(String key) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return key;
        }
        byte[] data = digest.digest(key.getBytes());
        StringBuilder buf = new StringBuilder(2 * data.length);
        for (byte b : data) {
            buf.append(HEX_DIGITS[(0xF0 & b) >> 4]);
            buf.append(HEX_DIGITS[0x0F & b]);
        }
        return buf.toString();
    }

    @OperationMethod
    public Blob run(DocumentModelList docs) throws IOException {

        BlobList blobList = new BlobList();
        for (DocumentModel doc : docs) {
            BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
            if (blobHolder != null) {
                Blob b = blobHolder.getBlob();
                if (b != null) {
                    blobList.add(blobHolder.getBlob());
                }
            }

        }
        if (blobList.isEmpty()) {
            throw new NuxeoException("No blob to be downloaded");
        }

        // build the key
        String key = buildTransientStoreKey(docs);

        TransientStoreService tss = Framework.getService(TransientStoreService.class);

        TransientStore ts = tss.getStore("download");
        if (ts == null) {
            throw new NuxeoException("Unable to find download Transient Store");
        }
        List<Blob> blobs = null;
        if (!ts.exists(key)) {
            Work work = new BlobListZipWork(key, session.getPrincipal().getName(), this.fileName, blobList);
            ts.setCompleted(key, false);
            ts.putParameter(key, WORKERID_KEY, work.getId());
            blobs = new ArrayList<Blob>();
            blobs.add(new AsyncBlob(key));
            ts.putBlobs(key, blobs);
            Framework.getService(WorkManager.class).schedule(work, Scheduling.IF_NOT_SCHEDULED);
            blobs = ts.getBlobs(key);
            return blobs.get(0);
        } else {
            blobs = ts.getBlobs(key);
            if (ts.isCompleted(key)) {
                if (blobs != null && blobs.size() == 1) {
                    Blob blob = blobs.get(0);
                    ts.release(key);
                    return blob;
                } else {
                    ts.release(key);
                    throw new NuxeoException("Cannot retrieve blob");
                }

            } else {
                Work work = new BlobListZipWork(key, session.getPrincipal().getName(), this.fileName, blobList);
                String workId = work.getId();
                WorkManager wm = Framework.getService(WorkManager.class);
                if (wm.find(workId, null) == null) {
                    wm.schedule(work, Scheduling.IF_NOT_SCHEDULED);
                }
                return new AsyncBlob(key);
            }
        }
    }

}
