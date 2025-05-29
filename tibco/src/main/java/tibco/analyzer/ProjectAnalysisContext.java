/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package tibco.analyzer;

import tibco.TibcoModel;
import tibco.TibcoToBalConverter;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectAnalysisContext {

    private final Set<String> controlFlowFunctionNames = new LinkedHashSet<>();
    private final Map<TibcoModel.Scope.Flow.Activity, String> activityFunctionNames =
            new ConcurrentHashMap<>();
    private final TibcoToBalConverter.ProjectConversionContext cx;

    public ProjectAnalysisContext(TibcoToBalConverter.ProjectConversionContext cx) {
        this.cx = cx;
    }

    public Set<String> controlFlowFunctionNames() {
        return controlFlowFunctionNames;
    }

    public Map<TibcoModel.Scope.Flow.Activity, String> activityFunctionNames() {
        return activityFunctionNames;
    }
}
