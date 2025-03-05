/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
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

package converter.tibco.analyzer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ballerina.BallerinaModel;
import tibco.TibcoModel;

public final class AnalysisResult {

    private final Map<TibcoModel.Scope.Flow.Link, Collection<TibcoModel.Scope.Flow.Activity>> destinationMap;
    private final Map<TibcoModel.Scope.Flow.Link, Collection<TibcoModel.Scope.Flow.Activity>> sourceMap;
    private final Map<TibcoModel.Process, TibcoModel.Scope.Flow.Activity> startActivities;
    private final Map<TibcoModel.Process, TibcoModel.Scope.Flow.Activity> endActivities;
    private final Map<TibcoModel.Scope.Flow.Link, String> workerNames;
    private final Map<TibcoModel.Scope.Flow.Activity, ActivityData> activityData;

    AnalysisResult(Map<TibcoModel.Scope.Flow.Link, Collection<TibcoModel.Scope.Flow.Activity>> destinationMap,
                   Map<TibcoModel.Scope.Flow.Link, Collection<TibcoModel.Scope.Flow.Activity>> sourceMap,
                   Map<TibcoModel.Process, TibcoModel.Scope.Flow.Activity> startActivities,
                   Map<TibcoModel.Process, TibcoModel.Scope.Flow.Activity> endActivities,
                   Map<TibcoModel.Scope.Flow.Link, String> workerNames,
                   Map<TibcoModel.Scope.Flow.Activity, ActivityData> activityData) {
        this.destinationMap = destinationMap;
        this.sourceMap = sourceMap;
        this.startActivities = startActivities;
        this.endActivities = endActivities;
        this.workerNames = workerNames;
        this.activityData = activityData;
    }

    public TibcoModel.Scope.Flow.Activity startActivity(TibcoModel.Process process) {
        return Objects.requireNonNull(startActivities.get(process));
    }

    public Optional<TibcoModel.Scope.Flow.Activity> endActivity(TibcoModel.Process process) {
        return Optional.ofNullable(endActivities.get(process));
    }

    public Collection<TibcoModel.Scope.Flow.Activity> destinations(TibcoModel.Scope.Flow.Link link) {
        var destinations = destinationMap.get(link);
        if (destinations == null) {
            return List.of();
        }
        return destinations;
    }

    public Collection<TibcoModel.Scope.Flow.Link> sources(TibcoModel.Scope.Flow.Activity activity) {
        if (activity instanceof TibcoModel.Scope.Flow.Activity.ActivityWithSources activityWithSources) {
            return activityWithSources.sources().stream()
                    .map(TibcoModel.Scope.Flow.Activity.Source::linkName)
                    .map(TibcoModel.Scope.Flow.Link::new)
                    .toList();
        }
        return List.of();
    }

    public Collection<TibcoModel.Scope.Flow.Activity> sources(TibcoModel.Scope.Flow.Link link) {
        var sources = sourceMap.get(link);
        if (sources == null) {
            throw new IllegalArgumentException("No sources found for link: " + link);
        }
        return sources;
    }

    public LinkData from(TibcoModel.Scope.Flow.Link link) {
        String workerName = workerNames.get(link);
        if (workerName == null) {
            throw new IllegalArgumentException("No worker name found for link: " + link);
        }
        return new LinkData(workerName, sources(link), destinations(link));
    }

    public ActivityData from(TibcoModel.Scope.Flow.Activity activity) {
        var data = activityData.get(activity);
        if (data == null) {
            throw new IllegalArgumentException("No data found for activity: " + activity);
        }
        return data;
    }

    public Collection<TibcoModel.Scope.Flow.Activity> activities() {
        return activityData.keySet();
    }

    public record LinkData(String workerName, Collection<TibcoModel.Scope.Flow.Activity> sourceActivities,
                           Collection<TibcoModel.Scope.Flow.Activity> destinationActivities) {

    }

    public record ActivityData(String functionName, BallerinaModel.TypeDesc argumentType,
                               BallerinaModel.TypeDesc returnType) {

    }
}
