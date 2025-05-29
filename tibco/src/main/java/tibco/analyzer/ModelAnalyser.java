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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelAnalyser {

    private final List<AnalysisPass> passes;

    public ModelAnalyser(List<AnalysisPass> passes) {
        this.passes = passes;
    }

    public Map<TibcoModel.Process, AnalysisResult> analyseProcesses(ProjectAnalysisContext cx,
                                                                    Collection<TibcoModel.Process> processes) {
        Map<TibcoModel.Process, AnalysisResult> analysisResults = new HashMap<>();
        for (TibcoModel.Process process : processes) {
            AnalysisResult combined = AnalysisResult.empty();
            for (AnalysisPass pass : passes) {
                ProcessAnalysisContext analysisContext = new ProcessAnalysisContext(cx);
                pass.analyseProcess(analysisContext, process);
                AnalysisResult result = pass.getResult(analysisContext, process);
                combined = combined.combine(result);
            }
            analysisResults.put(process, combined);
        }
        return Collections.unmodifiableMap(analysisResults);
    }

}
