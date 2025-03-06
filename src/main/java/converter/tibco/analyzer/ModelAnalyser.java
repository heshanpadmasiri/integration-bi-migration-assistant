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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static ballerina.BallerinaModel.TypeDesc.BuiltinType.XML;

import converter.tibco.ConversionUtils;
import tibco.TibcoModel;

public class ModelAnalyser {

    private ModelAnalyser() {

    }

    public static AnalysisResult analyseProcess(TibcoModel.Process process) {
        ProcessAnalysisContext cx = new ProcessAnalysisContext();
        analyseProcess(cx, process);
        assert cx.startActivity != null : "No start activity found";
        Map<TibcoModel.Process, TibcoModel.Scope.Flow.Activity> startActivities = Map.of(process, cx.startActivity);
        Map<TibcoModel.Process, TibcoModel.Scope.Flow.Activity> endActivities =
                cx.endActivity != null ? Map.of(process, cx.endActivity) : Map.of();
        record ActivityDataCollector(TibcoModel.Scope.Flow.Activity activity, AnalysisResult.ActivityData data) {

            static ActivityDataCollector from(TibcoModel.Scope.Flow.Activity activity, String functionName) {
                return new ActivityDataCollector(activity, new AnalysisResult.ActivityData(functionName, XML, XML));
            }
        }

        record NameOf<E>(E object, String name) {

        }

        Map<TibcoModel.Scope.Flow.Activity, AnalysisResult.ActivityData> activityData =
                cx.activities.stream()
                        .map(activity -> new NameOf<>(activity, ProcessAnalysisContext.activityNames.get(activity)))
                        .map(each -> ActivityDataCollector.from(each.object(), each.name()))
                        .collect(Collectors.toMap(ActivityDataCollector::activity, ActivityDataCollector::data));

        Map<TibcoModel.Scope.Flow.Link, String> workerNames =
                cx.links.stream().map(each -> new NameOf<>(each, ProcessAnalysisContext.workerNames.get(each)))
                        .collect(Collectors.toMap(NameOf::object, NameOf::name));
        return new AnalysisResult(cx.destinationMap, cx.sourceMap, startActivities, endActivities, workerNames,
                activityData);
    }

    private static void analyseProcess(ProcessAnalysisContext cx, TibcoModel.Process process) {
        analyseScope(cx, process.scope());
    }

    private static void analyseScope(ProcessAnalysisContext cx, TibcoModel.Scope scope) {
        scope.flows().forEach(flow -> analyseFlow(cx, flow));
    }

    private static void analyseFlow(ProcessAnalysisContext cx, TibcoModel.Scope.Flow flow) {
        flow.links().forEach(link -> analyseLink(cx, link));
        flow.activities().forEach(activity -> analyseActivity(cx, activity));
    }

    private static void analyseActivity(ProcessAnalysisContext cx, TibcoModel.Scope.Flow.Activity activity) {
        cx.allocateActivityNameIfNeeded(activity);
        if (activity instanceof TibcoModel.Scope.Flow.Activity.ActivityWithSources activityWithSources) {
            Collection<TibcoModel.Scope.Flow.Activity.Source> sources = activityWithSources.sources();
            if (sources.isEmpty()) {
                cx.startActivity = activityWithSources;
            }
            sources.stream().map(TibcoModel.Scope.Flow.Activity.Source::linkName).map(
                    TibcoModel.Scope.Flow.Link::new).forEach(link -> cx.addDestination(link, activity));
        }
        if (activity instanceof TibcoModel.Scope.Flow.Activity.ActivityWithTargets activityWithTargets) {
            activityWithTargets.targets().stream().map(TibcoModel.Scope.Flow.Activity.Target::linkName).map(
                    TibcoModel.Scope.Flow.Link::new).forEach(link -> cx.addSource(activity, link));
        }

        // TODO: handle pick
        if (activity instanceof TibcoModel.Scope.Flow.Activity.Pick pick) {
            analysePick(cx, pick);
        }
        // TODO: handle ExtActivity (when we have multiple process support this needs to generate a method name for the process)
    }

    private static void analysePick(ProcessAnalysisContext cx, TibcoModel.Scope.Flow.Activity.Pick pick) {
        cx.startActivity = pick;
        analyseScope(cx, pick.onMessage().scope());
    }

    private static void analyseLink(ProcessAnalysisContext cx, TibcoModel.Scope.Flow.Link link) {
        cx.allocateWorkerIfNeeded(link);
    }

    private static class ProcessAnalysisContext {

        private TibcoModel.Scope.Flow.Activity startActivity;
        private TibcoModel.Scope.Flow.Activity endActivity;
        // places where data added to the link ends up
        private final Map<TibcoModel.Scope.Flow.Link, Collection<TibcoModel.Scope.Flow.Activity>> destinationMap =
                new HashMap<>();

        // activities that add data to the link
        private final Map<TibcoModel.Scope.Flow.Link, Collection<TibcoModel.Scope.Flow.Activity>> sourceMap =
                new HashMap<>();

        private final Set<TibcoModel.Scope.Flow.Link> links = new HashSet<>();
        private final Set<TibcoModel.Scope.Flow.Activity> activities = new HashSet<>();

        private static final Map<TibcoModel.Scope.Flow.Link, String> workerNames = new ConcurrentHashMap<>();
        private static final Map<TibcoModel.Scope.Flow.Activity, String> activityNames = new ConcurrentHashMap<>();

        public void setStartActivity(TibcoModel.Scope.Flow.Activity activity) {
            if (startActivity != null) {
                throw new IllegalStateException("Start activity already set");
            }
            startActivity = activity;
        }

        public void allocateWorkerIfNeeded(TibcoModel.Scope.Flow.Link link) {
            links.add(link);
            if (workerNames.containsKey(link)) {
                return;
            }

            String workerName = ConversionUtils.getSanitizedUniqueName(link.name(), workerNames.values());
            workerNames.put(link, workerName);
        }

        public void allocateActivityNameIfNeeded(TibcoModel.Scope.Flow.Activity activity) {
            activities.add(activity);
            if (activityNames.containsKey(activity)) {
                return;
            }
            String prefix = switch (activity) {
                case TibcoModel.Scope.Flow.Activity.ActivityExtension ignored -> "activityExtension";
                case TibcoModel.Scope.Flow.Activity.Empty ignored -> "empty";
                case TibcoModel.Scope.Flow.Activity.ExtActivity ignored -> "extActivity";
                case TibcoModel.Scope.Flow.Activity.Invoke ignored -> "invoke";
                case TibcoModel.Scope.Flow.Activity.Pick ignored -> "pick";
                case TibcoModel.Scope.Flow.Activity.ReceiveEvent ignored -> "receiveEvent";
                case TibcoModel.Scope.Flow.Activity.Reply ignored -> "reply";
            };
            String activityName = ConversionUtils.getSanitizedUniqueName(prefix, activityNames.values());
            activityNames.put(activity, activityName);
        }

        public void addDestination(TibcoModel.Scope.Flow.Link source, TibcoModel.Scope.Flow.Activity activity) {
            destinationMap.computeIfAbsent(source, (ignored) -> new ArrayList<>()).add(activity);
        }

        public void addSource(TibcoModel.Scope.Flow.Activity source, TibcoModel.Scope.Flow.Link destination) {
            sourceMap.computeIfAbsent(destination, (ignored) -> new ArrayList<>()).add(source);
        }
    }
}
