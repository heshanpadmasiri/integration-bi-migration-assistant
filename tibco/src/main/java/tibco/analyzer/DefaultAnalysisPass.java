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

import org.jetbrains.annotations.NotNull;
import tibco.TibcoModel;
import tibco.TibcoModel.Process.ExplicitTransitionGroup;
import tibco.converter.ConversionUtils;
import tibco.converter.ProjectConverter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DefaultAnalysisPass extends AnalysisPass {
    private static final Logger logger = ProjectConverter.LOGGER;

    public DefaultAnalysisPass() {

    }

    @Override
    public AnalysisResult analyseProcess(ProcessAnalysisContext cx, TibcoModel.Process process) {
        analyseProcessInner(cx, process);
        return getAnalysisResult(cx, process);
    }

    private @NotNull AnalysisResult getAnalysisResult(ProcessAnalysisContext cx, TibcoModel.Process process) {
        Map<TibcoModel.Scope.Flow.Activity, AnalysisResult.ActivityData> activityData = cx.activityData();
        Map<String, TibcoModel.PartnerLink.Binding> partnerLinkBindings = cx.getPartnerLinkBindings();

        Map<TibcoModel.Process, Collection<String>> inputTypeNames = Map.of(process, cx.getInputTypeName());
        Map<TibcoModel.Process, String> outputTypeName = Map.of(process, cx.getOutputTypeName());
        Map<TibcoModel.Process, Map<String, String>> variableTypes = Map.of(process, cx.getVariableTypes());
        Map<TibcoModel.Process, Collection<TibcoModel.Scope>> scopes = Map.of(process,
                cx.getDependencyGraphs().keySet());
        record ActivityNames(String name, TibcoModel.Scope.Flow.Activity activity) {
        }
        Map<String, TibcoModel.Scope.Flow.Activity> activityByName = cx.getActivities().stream()
                .filter(each -> each instanceof TibcoModel.Scope.Flow.Activity.ActivityWithName)
                .map(each -> (TibcoModel.Scope.Flow.Activity.ActivityWithName) each)
                .filter(each -> each.getName().isPresent())
                .map(each -> new ActivityNames(each.getName().get(), each))
                .collect(Collectors.toMap(ActivityNames::name, ActivityNames::activity));
        return new AnalysisResult(cx.getDestinationMap(), cx.getSourceMap(),
                activityData, partnerLinkBindings, cx.getQueryIndex(), inputTypeNames, outputTypeName, variableTypes,
                cx.getDependencyGraphs(), cx.getControlFlowFunctions(), scopes, activityByName,
                cx.getExplicitTransitionGroupDependencyGraph(), cx.getTransitionGroupControlFlowFunctions());
    }

    private void analyseProcessInner(ProcessAnalysisContext cx, TibcoModel.Process process) {
        analyzeVariables(cx, process.variables());
        analysePartnerLinks(cx, process.partnerLinks());
        analyseTypes(cx, process.types());
        if (process.scope() != null) {
            analyseScope(cx, process.scope());
        }
        process.processInterface().ifPresent(i -> analyzeProcessInterface(cx, i));
        if (process.transitionGroup() != null) {
            analyseExplicitTransitionGroup(cx, process.transitionGroup());
        }
    }

    @Override
    protected void analyseExplicitTransitionGroup(
            ProcessAnalysisContext cx, ExplicitTransitionGroup explicitTransitionGroup) {
        cx.allocateControlFlowFunctionsIfNeeded(explicitTransitionGroup);
        Graph<AnalysisResult.GraphNode> graph = cx.getExplicitTransitionGroupGraph(explicitTransitionGroup);
        record ActivityGraphData(String name, AnalysisResult.GraphNode node) {
        }
        Map<String, AnalysisResult.GraphNode> activityNodes = explicitTransitionGroup.activities().stream()
                .map(activity -> new ActivityGraphData(activity.name(), cx.activityNode(activity)))
                .collect(Collectors.toMap(ActivityGraphData::name, ActivityGraphData::node));
        cx.setActivityNodes(activityNodes);
        ExplicitTransitionGroup.InlineActivity startActivity = explicitTransitionGroup.startActivity();
        activityNodes.put(startActivity.name(), cx.activityNode(startActivity));
        graph.addRoot(activityNodes.get(startActivity.name()));
        explicitTransitionGroup.transitions().forEach(transition -> {
            analyseTransition(cx, explicitTransitionGroup, transition);
        });
        cx.allocateActivityNameIfNeeded(startActivity);
        explicitTransitionGroup.activities().forEach(cx::allocateActivityNameIfNeeded);
        explicitTransitionGroup.activities().stream()
                .flatMap(each -> {
                    if (each instanceof ExplicitTransitionGroup.InlineActivityWithBody inlineActivityWithBody) {
                        return Stream.of(inlineActivityWithBody);
                    } else {
                        return Stream.empty();
                    }
                })
                .map(ExplicitTransitionGroup.InlineActivityWithBody::body)
                .forEach(each -> analyseExplicitTransitionGroup(cx, each));
    }

    @Override
    protected void analyseTransition(
            ProcessAnalysisContext cx, ExplicitTransitionGroup explicitTransitionGroup,
            ExplicitTransitionGroup.Transition transition) {
        Map<String, AnalysisResult.GraphNode> activityNodes = cx.getActivityNodes();
        Graph<AnalysisResult.GraphNode> graph = cx.getExplicitTransitionGroupGraph(explicitTransitionGroup);
        if (activityNodes.containsKey(transition.from()) && activityNodes.containsKey(transition.to())) {
            graph.addEdge(activityNodes.get(transition.from()), activityNodes.get(transition.to()));
        }
        // Start and end transitions can be safely skipped
    }

    @Override
    protected void analyzeVariables(ProcessAnalysisContext cx, Collection<TibcoModel.Variable> variables) {
        variables.forEach(variable -> {
            String typeName = ConversionUtils.stripNamespace(variable.type());
            cx.setVariableType(variable.name(), typeName);
        });
    }

    @Override
    protected void analyzeProcessInterface(ProcessAnalysisContext cx,
                                           TibcoModel.ProcessInterface processInterface) {
        String inputType = sanitizeTypeName(processInterface.input());
        if (!inputType.isEmpty()) {
            cx.appendInputTypeName(inputType);
        }
        String outputType = sanitizeTypeName(processInterface.output());
        if (!outputType.isEmpty()) {
            cx.setOutputTypeName(outputType);
        }
    }

    private String sanitizeTypeName(String typeName) {
        if (typeName == null) {
            return "";
        }
        if (typeName.contains("{") && typeName.contains("}")) {
            typeName = typeName.substring(typeName.indexOf("}") + 1);
        }
        return typeName;
    }

    @Override
    protected void analyseTypes(ProcessAnalysisContext cx, Collection<TibcoModel.Type> types) {
        types.forEach(type -> {
            if (type instanceof TibcoModel.Type.WSDLDefinition wsdlDefinition) {
                analyseWSDLDefinition(cx, wsdlDefinition);
            }
        });
    }

    @Override
    protected void analyseWSDLDefinition(ProcessAnalysisContext cx,
                                         TibcoModel.Type.WSDLDefinition wsdlDefinition) {
        var messageTypes = getMessageTypeDefinitions(wsdlDefinition);
        wsdlDefinition.portType().forEach(portType -> {
            var operation = portType.operation();
            String inputType = messageTypes.get(operation.input().message().value());
            if (inputType == null) {
                inputType = "null";
            }
            cx.appendInputTypeName(inputType);
            cx.setOutputTypeName(messageTypes.get(operation.output().message().value()));
        });
    }

    private Map<String, String> getMessageTypeDefinitions(TibcoModel.Type.WSDLDefinition wsdlDefinition) {
        Map<String, String> result = new HashMap<>();
        for (TibcoModel.Type.WSDLDefinition.Message message : wsdlDefinition.messages()) {
            Optional<String> referredTypeName = getMessageTypeName(message);
            if (referredTypeName.isEmpty()) {
                continue;
            }
            result.put(message.name(), referredTypeName.get());
        }
        return result;
    }

    private Optional<String> getMessageTypeName(TibcoModel.Type.WSDLDefinition.Message message) {
        Optional<TibcoModel.Type.WSDLDefinition.Message.Part> part;
        // if (message.parts().size() == 1) {
        // part = Optional.ofNullable(message.parts().getFirst());
        // } else {
        part = message.parts().stream().filter(each -> each.name().equals("item")).findFirst();
        // }
        if (part.isEmpty()) {
            return Optional.empty();
        }
        String typeName = switch (part.get()) {
            case TibcoModel.Type.WSDLDefinition.Message.Part.InlineError inlineError -> inlineError.name();
            case TibcoModel.Type.WSDLDefinition.Message.Part.Reference ref -> ref.element().value();
        };
        return Optional.of(typeName);
    }

    @Override
    protected void analysePartnerLinks(ProcessAnalysisContext cx, Collection<TibcoModel.PartnerLink> links) {
        links.stream()
                .flatMap(link -> link instanceof TibcoModel.PartnerLink.NonEmptyPartnerLink nonEmptyPartnerLink
                        ? Stream.of(nonEmptyPartnerLink)
                        : Stream.empty())
                .forEach(link -> cx.setPartnerLinkBinding(link, link.binding()));
    }


    @Override
    protected void analyseScope(ProcessAnalysisContext cx, TibcoModel.Scope scope) {
        cx.allocateControlFlowFunctionsIfNeeded(scope);
        cx.pushScope(scope);
        scope.flows().forEach(flow -> analyseFlow(cx, flow));
        scope.faultHandlers().forEach(faultHandler -> analyseActivity(cx, faultHandler));
        scope.sequence().forEach(sequence -> analyseSequence(cx, sequence));
        cx.popScope();
    }


    @Override
    protected void analyseSequence(ProcessAnalysisContext cx, TibcoModel.Scope.Sequence sequence) {
        cx.getInSequence().push(true);
        List<TibcoModel.Scope.Flow.Activity> activities = sequence.activities();
        for (int i = 0; i < activities.size(); i++) {
            TibcoModel.Scope.Flow.Activity activity = activities.get(i);
            if (i == 0) {
                cx.addStartActivity(activity);
            } else {
                cx.addDestination(activities.get(i - 1), activity);
            }
            if (i == activities.size() - 1) {
                cx.addEndActivity(activity);
            }
            analyseActivity(cx, activity);
        }
        cx.getInSequence().pop();
    }

    @Override
    protected void analyseFlow(ProcessAnalysisContext cx, TibcoModel.Scope.Flow flow) {
        cx.getInSequence().push(false);
        flow.links().forEach(link -> analyseLink(cx, link));
        flow.activities().forEach(activity -> analyseActivity(cx, activity));
        cx.getInSequence().pop();
    }


    @Override
    protected void analyseLink(ProcessAnalysisContext cx, TibcoModel.Scope.Flow.Link link) {
        cx.allocateLinkIfNeeded(link);
    }

    @Override
    protected void analyseActivity(ProcessAnalysisContext cx, TibcoModel.Scope.Flow.Activity activity) {
        if (activity instanceof TibcoModel.Scope.Flow.Activity.Empty) {
            return;
        }
        boolean isInSequence = cx.getInSequence().peek();
        cx.allocateActivityNameIfNeeded(activity);
        if (activity instanceof TibcoModel.Scope.Flow.Activity.ActivityWithSources activityWithSources) {
            Collection<TibcoModel.Scope.Flow.Activity.Source> sources = activityWithSources.sources();
            if (!isInSequence && sources.isEmpty()) {
                cx.addEndActivity(activity);
            }
            sources.stream().map(TibcoModel.Scope.Flow.Activity.Source::linkName).map(
                    TibcoModel.Scope.Flow.Link::new).forEach(link -> cx.addSource(activity, link));
        }
        if (activity instanceof TibcoModel.Scope.Flow.Activity.ActivityWithTargets activityWithTargets) {
            Collection<TibcoModel.Scope.Flow.Activity.Target> targets = activityWithTargets.targets();
            if (!isInSequence && targets.isEmpty()) {
                cx.addStartActivity(activity);
            }
            targets.stream().map(TibcoModel.Scope.Flow.Activity.Target::linkName).map(
                    TibcoModel.Scope.Flow.Link::new).forEach(link -> cx.addDestination(link, activity));
        }
        if (activity instanceof TibcoModel.Scope.Flow.Activity.StartActivity) {
            cx.addStartActivity(activity);
        }
        analyseActivityInner(cx, activity);
    }

    private void analyseActivityInner(ProcessAnalysisContext cx, TibcoModel.Scope.Flow.Activity activity) {
        if (activity instanceof TibcoModel.Scope.Flow.Activity.ActivityWithScope activityWithScope) {
            analyseScope(cx, activityWithScope.scope());
            return;
        }

        boolean isInSequence = cx.getInSequence().peek();
        if (!isInSequence && !(activity instanceof TibcoModel.Scope.Flow.Activity.ActivityWithSources)) {
            cx.addEndActivity(activity);
        }

        if (!isInSequence && !(activity instanceof TibcoModel.Scope.Flow.Activity.ActivityWithTargets)) {
            cx.addStartActivity(activity);
        }
        if (activity instanceof TibcoModel.Scope.Flow.Activity.ActivityExtension activityExtension) {
            analyseActivityExtensionConfig(cx, activityExtension.config());
        }
    }

}
