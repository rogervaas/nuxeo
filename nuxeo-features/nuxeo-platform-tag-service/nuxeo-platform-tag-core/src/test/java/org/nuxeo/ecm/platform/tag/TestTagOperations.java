/*
 * (C) Copyright 2012-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 *      Thibaud Arguillere <targuillere@nuxeo.com>
 */
package org.nuxeo.ecm.platform.tag;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.tag.operations.RemoveDocumentTags;
import org.nuxeo.ecm.platform.tag.operations.TagDocument;
import org.nuxeo.ecm.platform.tag.operations.UntagDocument;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, AutomationFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.tag" })
public class TestTagOperations {

    private static final String TAG_1 = "1";

    private static final String TAG_2 = "2";

    private static final String TAG_3 = "3";

    private static final String TAGS = TAG_1 + "," + TAG_2 + ", " + TAG_3;

    private static final String TAGS_COMMA = TAG_1 + "," + TAG_2 + ", " + TAG_3 + ",";

    protected DocumentModel document;

    protected String docId;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    CoreSession session;

    @Inject
    AutomationService automationService;

    @Inject
    TagService tagService;

    @Test
    public void testTagOperationsSuite() throws Exception {

        boolean facetedTags = Framework.getService(ConfigurationService.class).isBooleanPropertyTrue(
                TagServiceImpl.FACETED_TAG_SERVICE_ENABLED);
        assumeTrue("DBS does not support tags based on SQL relations",
                !coreFeature.getStorageConfiguration().isDBS() || facetedTags);

        // quick init
        document = session.createDocumentModel("/", "File", "File");
        document = session.createDocument(document);
        docId = document.getId();

        testTagDocument(TAGS);
        testRemoveTags();
        testTagDocument(TAGS_COMMA);
        testUntagDocument();
        testRemoveTags();
    }

    public void testTagDocument(String inputTags) throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(document);
        Map<String, Object> params = new HashMap<>();
        params.put("tags", inputTags);
        automationService.run(ctx, TagDocument.ID, params);
        List<Tag> tags = tagService.getDocumentTags(session, docId, "Administrator");
        assertEquals(3, tags.size());
        Set<String> actual = new HashSet<>();
        for (Tag tag : tags) {
            actual.add(tag.getLabel());
        }
        assertEquals(new HashSet<>(Arrays.asList(TAG_1, TAG_2, TAG_3)), actual);
    }

    public void testUntagDocument() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(document);
        Map<String, Object> params = new HashMap<>();
        params.put("tags", TAG_1);
        automationService.run(ctx, UntagDocument.ID, params);
        List<Tag> tags = tagService.getDocumentTags(session, docId, "Administrator");
        assertEquals(2, tags.size());
        Set<String> actual = new HashSet<>();
        for (Tag tag : tags) {
            actual.add(tag.getLabel());
        }
        assertEquals(new HashSet<>(Arrays.asList(TAG_2, TAG_3)), actual);
    }

    public void testRemoveTags() throws Exception {
        OperationContext ctx = new OperationContext(session);
        ctx.setInput(document);
        automationService.run(ctx, RemoveDocumentTags.ID);
        List<Tag> tags = tagService.getDocumentTags(session, docId, "Administrator");
        assertEquals(0, tags.size());
    }

}
