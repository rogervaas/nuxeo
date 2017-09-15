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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.AsyncBlob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Work to zip a list of blob and store the produced archive into the TransientStore.
 * 
 * @since 9.3
 */
public class BlobListZipWork extends TransientStoreWork {

    public static final String CATEGORY = "blobListZip";

    public static final String CACHE_NAME = "blobListZip";

    private static final long serialVersionUID = 1L;

    protected BlobList blobList;

    protected String filename;

    protected String key;

    public BlobListZipWork(String transientStoreKey, String originatingUsername, String filename, BlobList blobList) {
        this.key = transientStoreKey;
        this.blobList = blobList;
        this.originatingUsername = originatingUsername;
        this.id = "BlobListZipWork-" + this.key + "-" + this.originatingUsername;
        if (StringUtils.isNotBlank(filename)) {
            this.filename = filename.toLowerCase().endsWith(".zip") ? filename : filename + ".zip";
        }

    }

    @Override
    public void cleanUp(boolean ok, Exception e) {
        super.cleanUp(ok, e);
        if (ok) {
            return;
        }
        List<Blob> blobs = new ArrayList<Blob>();
        Blob emptyBlob = new AsyncBlob(key);
        blobs.add(emptyBlob);
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(getTransientStoreName());
        ts.putParameter(key, DownloadService.ERROR, e);
        updateAndCompleteStoreEntry(blobs);
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getTitle() {
        return this.id;
    }

    protected String getTransientStoreName() {
        return CACHE_NAME;
    }

    void updateAndCompleteStoreEntry(List<Blob> blobs) {
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(getTransientStoreName());

        if (!ts.exists(key)) {
            throw new NuxeoException("Rendition TransientStore entry can not be null");
        }
        ts.putBlobs(key, blobs);
        ts.setCompleted(key, true);
    }

    @Override
    public void work() {
        openUserSession();

        AutomationService as = Framework.getLocalService(AutomationService.class);
        try (OperationContext oc = new OperationContext(session)) {
            oc.push("filename", StringUtils.isNotBlank(this.filename) ? this.filename : this.id);
            oc.setInput(blobList);
            Blob blob = (Blob) as.run(oc, CreateZip.ID);
            List<Blob> blobs = new ArrayList<Blob>();
            blobs.add(blob);
            updateAndCompleteStoreEntry(blobs);
        } catch (Exception e) {
            TransientStoreService tss = Framework.getService(TransientStoreService.class);
            TransientStore ts = tss.getStore(getTransientStoreName());
            ts.putParameter(key, DownloadService.ERROR, e);
            throw new NuxeoException("Exception while zipping blob list", e);
        }

    }

}
