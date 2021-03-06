/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.impl;

import org.nuxeo.ecm.automation.AutomationFilter;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.runtime.model.ContributionFragmentRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 5.7.3
 */
public class AutomationFilterRegistry extends ContributionFragmentRegistry<AutomationFilter> {

    /**
     * Modifiable exception chain registry. Modifying the registry is using a lock and it's thread safe. Modifications
     * are removing the cache.
     */
    protected final Map<String, AutomationFilter> automationFilters = new HashMap<>();

    /**
     * Read only cache for exception chain lookup. Thread safe. Not using synchronization if cache already created.
     */
    protected volatile Map<String, AutomationFilter> lookup;

    public synchronized void addContribution(AutomationFilter automationFilter, boolean replace)
            throws OperationException {
        if (!replace && automationFilters.containsKey(automationFilter.getId())) {
            throw new OperationException("An automation filter is already bound to: " + automationFilter.getId()
                    + ". Use 'replace=true' to replace an existing automation filter");
        }
        super.addContribution(automationFilter);
    }

    @Override
    public boolean isSupportingMerge() {
        return false;
    }

    @Override
    public String getContributionId(AutomationFilter contrib) {
        return contrib.getId();
    }

    @Override
    public void contributionUpdated(String id, AutomationFilter contrib, AutomationFilter newOrigContrib) {
        automationFilters.put(id, contrib);
        lookup = null;
    }

    @Override
    public void contributionRemoved(String id, AutomationFilter origContrib) {
        automationFilters.remove(id);
        lookup = null;
    }

    @Override
    public AutomationFilter clone(AutomationFilter orig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void merge(AutomationFilter src, AutomationFilter dst) {
        throw new UnsupportedOperationException();
    }

    public Map<String, AutomationFilter> lookup() {
        Map<String, AutomationFilter> _lookup = lookup;
        if (_lookup == null) {
            synchronized (this) {
                lookup = new HashMap<>(automationFilters);
                _lookup = lookup;
            }
        }
        return _lookup;
    }

    public AutomationFilter getAutomationFilter(String id) {
        return automationFilters.get(id);
    }
}
