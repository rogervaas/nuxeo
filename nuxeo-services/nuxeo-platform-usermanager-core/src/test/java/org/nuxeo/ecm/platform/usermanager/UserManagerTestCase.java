/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *
 */
package org.nuxeo.ecm.platform.usermanager;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.After;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, DirectoryFeature.class })
@Deploy({ "org.nuxeo.ecm.core.schema", //
        "org.nuxeo.ecm.core.api", //
        "org.nuxeo.ecm.core", //
        "org.nuxeo.ecm.core.cache", //
        "org.nuxeo.ecm.core.event", //
        "org.nuxeo.ecm.platform.usermanager.api", //
        "org.nuxeo.ecm.platform.usermanager", //
        "org.nuxeo.ecm.platform.query.api", //
})
@LocalDeploy({ "org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/usermanager-inmemory-cache-config.xml", //
        "org.nuxeo.ecm.platform.usermanager.tests:test-usermanagerimpl/userservice-config.xml", //
})
public abstract class UserManagerTestCase {

    @Inject
    protected UserManager userManager;

    public void TODO() {
//        if (!RedisFeature.setup(this)) {
//            deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
//                    "test-usermanagerimpl/usermanager-redis-cache-config.xml");
//        } else {
//            deployContrib("org.nuxeo.ecm.platform.usermanager.tests",
//                    "test-usermanagerimpl/usermanager-inmemory-cache-config.xml");
//        }
//        fireFrameworkStarted();
    }

    @After
    public void cleanup() {
        DocumentModelList users = userManager.searchUsers(null);
        for (DocumentModel user : users) {
            String userId = user.getId();
            if (userId.equals(userManager.getAnonymousUserId())) {
                continue;
            }
            if (userId.startsWith("Administrator")) {
                // comes from a CSV
                continue;
            }
            userManager.getPrincipal(userId); // init relation tables needed on delete
            userManager.deleteUser(userId);
        }
        for (String groupId : Arrays.asList("group1", "group2", "group3")) {
            if (userManager.getGroup(groupId) != null) {
                userManager.deleteGroup(groupId);
            }
        }
    }

}
