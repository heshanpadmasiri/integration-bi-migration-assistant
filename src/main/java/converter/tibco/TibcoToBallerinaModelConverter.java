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

package converter.tibco;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static ballerina.BallerinaModel.TypeDesc.BuiltinType.XML;

import ballerina.BallerinaModel;
import converter.tibco.analyzer.AnalysisResult;
import org.jetbrains.annotations.NotNull;
import tibco.TibcoModel;

public class TibcoToBallerinaModelConverter {

    private TibcoToBallerinaModelConverter() {
    }

    static BallerinaModel.Module convertProcesses(List<TibcoModel.Process> processes) {
        ProjectContext cx = new ProjectContext();
        record ProcessResult(TibcoModel.Process process, TypeConversionResult result) {

        }
        List<ProcessResult> results =
                processes.stream().map(process -> new ProcessResult(process,
                        convertTypes(cx.getProcessContext(process), process))).toList();
        // We need to ensure all the type definitions have been processed before we start processing the functions
        List<BallerinaModel.TextDocument> textDocuments = results.stream().map(result -> {
            TibcoModel.Process process = result.process();
            return convertBody(cx.getProcessContext(process), process, result.result());
        }).toList();
        return cx.serialize(textDocuments);
    }

    static BallerinaModel.Module convertProcess(TibcoModel.Process process) {
        ProjectContext cx = new ProjectContext();
        return convertProcess(cx.getProcessContext(process), process);
    }

    private static BallerinaModel.Module convertProcess(ProjectContext.ProcessContext cx, TibcoModel.Process process) {
        TypeConversionResult result = convertTypes(cx, process);
        BallerinaModel.TextDocument textDocument = convertBody(cx, process, result);
        ProjectContext projectContext = cx.projectContext;
        return projectContext.serialize(List.of(textDocument));
    }

    private static BallerinaModel.TextDocument convertBody(ProjectContext.ProcessContext cx, TibcoModel.Process process,
                                                           TypeConversionResult result) {
        List<BallerinaModel.Function> functions = cx.analysisResult.activities().stream()
                .map(activity -> ActivityConverter.convertActivity(cx, activity))
                .collect(Collectors.toCollection(ArrayList::new));
        functions.add(generateStartFunction(cx, cx.analysisResult.startActivity(process)));
        functions.add(generateProcessFunction(cx, process));
        functions.sort(Comparator.comparing(BallerinaModel.Function::methodName));

        return cx.serialize(result.service(), functions);
    }

    private static TypeConversionResult convertTypes(ProjectContext.ProcessContext cx, TibcoModel.Process process) {
        List<BallerinaModel.ModuleTypeDef> moduleTypeDefs = new ArrayList<>();
        BallerinaModel.Service service = null;
        for (TibcoModel.Type type : process.types()) {
            switch (type) {
                case TibcoModel.Type.Schema schema -> moduleTypeDefs.addAll(TypeConverter.convertSchema(cx, schema));
                case TibcoModel.Type.WSDLDefinition wsdlDefinition -> {
                    if (service != null) {
                        throw new IllegalStateException("Multiple services not supported");
                    }
                    service = TypeConverter.convertWsdlDefinition(cx, wsdlDefinition);
                }
            }
        }
        return new TypeConversionResult(moduleTypeDefs, service);
    }

    private record TypeConversionResult(List<BallerinaModel.ModuleTypeDef> moduleTypeDefs,
                                        BallerinaModel.Service service) {

    }

    private static BallerinaModel.Function generateProcessFunction(ProjectContext.ProcessContext cx,
                                                                   TibcoModel.Process process) {
        String name = cx.getProcessFunction();
        AnalysisResult analysisResult = cx.analysisResult;
        TibcoModel.Scope.Flow.Activity startActivity = cx.analysisResult.startActivity(process);
        assert startActivity != null : "Start activity not found";
        List<BallerinaModel.Statement> body = new ArrayList<>();
        body.add(generateWorkerForStartAction(cx, startActivity));
        analysisResult.links().stream().map(link -> generateLink(cx, link)).forEach(body::add);
        // Shouldn't this always be one?
        List<String> resultVars = new ArrayList<>();
        int resultCount = 0;
        for (String worker : cx.terminateWorkers) {
            String resultName = "result" + resultCount++;
            resultVars.add(resultName);
            BallerinaModel.VarDeclStatment inputVarDecl = receiveVarFromPeer(worker, resultName);
            body.add(inputVarDecl);
        }
        String resultVar = String.join(" + ", resultVars);
        body.add(new BallerinaModel.BallerinaStatement("return " + resultVar + ";"));
        return new BallerinaModel.Function(Optional.empty(), name,
                List.of(new BallerinaModel.Parameter(XML, "input")), Optional.of(XML.toString()), body);
    }

    private static BallerinaModel.Statement generateWorkerForStartAction(ProjectContext.ProcessContext cx,
                                                                         TibcoModel.Scope.Flow.Activity startActivity) {
        cx.startWorkerName = "start_worker";
        return switch (startActivity) {
            case TibcoModel.Scope.Flow.Activity.Pick pick -> generateWorkerForPickStartAction(cx, pick);
            case TibcoModel.Scope.Flow.Activity.ActivityExtension activityExtension ->
                    generateWorkerForActivityExtensionAction(cx, activityExtension);
            default -> throw new IllegalStateException("Unexpected value: " + startActivity);
        };
    }

    private static BallerinaModel.Statement generateWorkerForActivityExtensionAction(ProjectContext.ProcessContext cx,
                                                                                     TibcoModel.Scope.Flow.Activity.ActivityExtension activityExtension) {
        return generateWorkerForStartActionInner(cx, activityExtension);
    }

    private static BallerinaModel.Statement generateWorkerForPickStartAction(ProjectContext.ProcessContext cx,
                                                                             TibcoModel.Scope.Flow.Activity.Pick pick) {
        return generateWorkerForStartActionInner(cx, findPickStartActivity(cx, pick));
    }

    private static TibcoModel.Scope.Flow.@NotNull Activity findPickStartActivity(ProjectContext.ProcessContext cx,
                                                                                 TibcoModel.Scope.Flow.Activity.Pick pick) {
        TibcoModel.Scope.Flow.Activity.Pick.OnMessage onMessage = pick.onMessage();
        TibcoModel.Scope scope = onMessage.scope();
        Collection<TibcoModel.Scope.Flow> flows = scope.flows();
        assert flows.size() == 1;
        TibcoModel.Scope.Flow flow = flows.iterator().next();
        TibcoModel.Scope.Flow.Activity startActivity = null;
        AnalysisResult analysisResult = cx.analysisResult;
        for (TibcoModel.Scope.Flow.Activity activity : flow.activities()) {
            if (activity instanceof TibcoModel.Scope.Flow.Activity.Empty) {
                continue;
            }
            if (analysisResult.sources(activity).isEmpty()) {
                startActivity = activity;
                break;
            }
        }
        assert startActivity != null : "Failed to find start activity";
        return startActivity;
    }

    private static BallerinaModel.NamedWorkerDecl generateWorkerForStartActionInner(ProjectContext.ProcessContext cx,
                                                                                    TibcoModel.Scope.Flow.Activity startActivity) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        AnalysisResult analysisResult = cx.analysisResult;
        AnalysisResult.ActivityData activityData = analysisResult.from(startActivity);
        String fn = activityData.functionName();
        String inputVarName = "input";
        BallerinaModel.Expression.FunctionCall callExpr =
                new BallerinaModel.Expression.FunctionCall(fn, new String[]{inputVarName});
        String outputVarName = "output";
        BallerinaModel.VarDeclStatment outputVarDecl = new BallerinaModel.VarDeclStatment(XML, outputVarName, callExpr);
        body.add(outputVarDecl);

        Collection<TibcoModel.Scope.Flow.Link> destinationLinks = analysisResult.destinations(startActivity);
        for (TibcoModel.Scope.Flow.Link destinationLink : destinationLinks) {
            body.add(generateSendToWorker(cx, destinationLink, outputVarName));
        }

        return new BallerinaModel.NamedWorkerDecl(cx.startWorkerName, body);
    }

    private static BallerinaModel.@NotNull BallerinaStatement generateSendToWorker(ProjectContext.ProcessContext cx,
                                                                                   TibcoModel.Scope.Flow.Link destinationLink,
                                                                                   String outputVarName) {
        AnalysisResult analysisResult = cx.analysisResult;
        AnalysisResult.LinkData linkData = analysisResult.from(destinationLink);
        BallerinaModel.Action.WorkerSendAction sendAction = new BallerinaModel.Action.WorkerSendAction(
                new BallerinaModel.Expression.VariableReference(outputVarName), linkData.workerName());
        // FIXME:
        return new BallerinaModel.BallerinaStatement(sendAction + ";");
    }

    private static BallerinaModel.Statement generateLink(ProjectContext.ProcessContext cx,
                                                         TibcoModel.Scope.Flow.Link link) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        List<String> inputVarNames = new ArrayList<>();
        var analysisResult = cx.analysisResult;
        int inputCount = 0;
        Collection<TibcoModel.Scope.Flow.Activity> inputActivities = analysisResult.sources(link);
        AnalysisResult.LinkData linkData = analysisResult.from(link);
        for (TibcoModel.Scope.Flow.Activity activity : inputActivities) {
            Collection<TibcoModel.Scope.Flow.Link> linkPrev = analysisResult.sources(activity);
            if (linkPrev.isEmpty()) {
                String inputVarName = "v" + inputCount++;
                String startWorkerName = cx.startWorkerName;
                addReceiveFromPeerStatement(startWorkerName, inputVarName, body, inputVarNames);
            }
            for (TibcoModel.Scope.Flow.Link each : linkPrev) {
                String inputVarName = "v" + inputCount++;
                addReceiveFromPeerStatement(analysisResult.from(each).workerName(), inputVarName, body,
                        inputVarNames);
            }
        }
        int outPutCount = 0;
        for (TibcoModel.Scope.Flow.Activity destinations : analysisResult.destinations(link)) {
            AnalysisResult.ActivityData activityData = analysisResult.from(destinations);
            assert inputVarNames.size() == 1 : "Multiple input vars not supported";
            String inputVarName = inputVarNames.getFirst();
            String fn = activityData.functionName();
            BallerinaModel.Expression.FunctionCall callExpr =
                    new BallerinaModel.Expression.FunctionCall(fn, new String[]{inputVarName});
            String outputVarName = "output" + outPutCount++;
            BallerinaModel.VarDeclStatment outputVarDecl =
                    new BallerinaModel.VarDeclStatment(XML, outputVarName, callExpr);
            body.add(outputVarDecl);
            Collection<TibcoModel.Scope.Flow.Link> destinationLinks = analysisResult.destinations(destinations);
            if (destinationLinks.isEmpty()) {
                cx.terminateWorkers.add(linkData.workerName());
                BallerinaModel.Action.WorkerSendAction sendAction = new BallerinaModel.Action.WorkerSendAction(
                        new BallerinaModel.Expression.VariableReference(outputVarName), "function");
                body.add(new BallerinaModel.BallerinaStatement(sendAction + ";"));
            }
            for (TibcoModel.Scope.Flow.Link destinationLink : destinationLinks) {
                body.add(generateSendToWorker(cx, destinationLink, outputVarName));
            }
        }
        return new BallerinaModel.NamedWorkerDecl(linkData.workerName(), body);
    }

    private static void addReceiveFromPeerStatement(String peer, String inputVarName,
                                                    List<BallerinaModel.Statement> body,
                                                    List<String> inputVarNames) {
        BallerinaModel.VarDeclStatment inputVarDecl = receiveVarFromPeer(peer, inputVarName);
        body.add(inputVarDecl);
        inputVarNames.add(inputVarName);
    }

    private static BallerinaModel.VarDeclStatment receiveVarFromPeer(String peer, String inputVarName) {
        BallerinaModel.Action.WorkerReceiveAction receiveEvent = new BallerinaModel.Action.WorkerReceiveAction(peer);
        return new BallerinaModel.VarDeclStatment(XML, inputVarName, receiveEvent);
    }

    private static BallerinaModel.Function generateStartFunction(ProjectContext.ProcessContext cx,
                                                                 TibcoModel.Scope.Flow.Activity startActivity) {

        List<BallerinaModel.Statement> body = new ArrayList<>();
        BallerinaModel.TypeDesc inputType = cx.processInputType;
        BallerinaModel.TypeDesc returnType = cx.processReturnType;

        String inputVariable = "input";
        BallerinaModel.Expression.FunctionCall toXMLCall =
                new BallerinaModel.Expression.FunctionCall(cx.getToXmlFunction(), new String[]{inputVariable});
        String inputXML = "inputXML";
        BallerinaModel.VarDeclStatment inputXMLVar = new BallerinaModel.VarDeclStatment(XML, inputXML, toXMLCall);
        body.add(inputXMLVar);

        String processFunction = cx.getProcessFunction();
        BallerinaModel.VarDeclStatment xmlResult = new BallerinaModel.VarDeclStatment(XML, "xmlResult",
                new BallerinaModel.Expression.FunctionCall(processFunction, new String[]{inputXML}));
        body.add(xmlResult);

        String convertToTypeFunction = cx.getConvertToTypeFunction(returnType);
        BallerinaModel.VarDeclStatment result = new BallerinaModel.VarDeclStatment(returnType, "result",
                new BallerinaModel.Expression.FunctionCall(convertToTypeFunction, new String[]{"xmlResult"}));
        body.add(result);

        BallerinaModel.Return<BallerinaModel.Expression.VariableReference> returnStatement =
                new BallerinaModel.Return<>(Optional.of(new BallerinaModel.Expression.VariableReference("result")));
        body.add(returnStatement);

        return BallerinaModel.Function.publicFunction(
                cx.getProcessStartFunctionName(),
                List.of(new BallerinaModel.Parameter(inputType, inputVariable)),
                Optional.of(returnType.toString()),
                body);
    }
}
