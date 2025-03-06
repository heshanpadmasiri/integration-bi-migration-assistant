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

import static ballerina.BallerinaModel.TypeDesc.BuiltinType.XML;

import ballerina.BallerinaModel;
import converter.tibco.analyzer.AnalysisResult;
import tibco.TibcoModel;

public class TibcoToBallerinaModelConverter {

    private TibcoToBallerinaModelConverter() {
    }

    static BallerinaModel.Module convertProcesses(List<TibcoModel.Process> processes) {
        ProjectContext cx = new ProjectContext();
        List<TypeConversionResult> results =
                processes.stream().map(process -> convertTypes(cx.getProcessContext(process), process)).toList();
        assert results.size() == processes.size();
        List<BallerinaModel.TextDocument> textDocuments = new ArrayList<>(results.size());
        for (int i = 0; i < results.size(); i++) {
            TibcoModel.Process process = processes.get(i);
            BallerinaModel.TextDocument textDocument =
                    convertBody(cx.getProcessContext(process), process, results.get(i));
            textDocuments.add(textDocument);
        }
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

    private static BallerinaModel.TextDocument convertBody(ProjectContext.ProcessContext cx,
                                                           TibcoModel.Process process,
                                                           TypeConversionResult result) {
        List<BallerinaModel.Function> functions = new ArrayList<>();
        cx.analysisResult.activities().stream()
                .map(activity -> ActivityConverter.convertActivity(cx, activity))
                .forEach(functions::add);
        functions.add(generateStartFunction(cx, cx.analysisResult.startActivity(process)));
        functions.add(generateProcessFunction(cx, process));
        functions.sort(Comparator.comparing(BallerinaModel.Function::methodName));

        return cx.serialize(result.moduleTypeDefs(), result.service(), functions);
    }

    private static TypeConversionResult convertTypes(ProjectContext.ProcessContext cx,
                                                     TibcoModel.Process process) {
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
                List.of(new BallerinaModel.Parameter(XML.toString(), "input")),
                Optional.of(XML.toString()), body);
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

    // FIXME:
    private static BallerinaModel.Statement generateWorkerForPickStartAction(ProjectContext.ProcessContext cx,
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
        return generateWorkerForStartActionInner(cx, startActivity);
    }

    private static BallerinaModel.NamedWorkerDecl generateWorkerForStartActionInner(
            ProjectContext.ProcessContext cx,
            TibcoModel.Scope.Flow.Activity startActivity) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        AnalysisResult analysisResult = cx.analysisResult;
        AnalysisResult.ActivityData activityData = analysisResult.from(startActivity);
        String inputVarName = "input";
        String fn = activityData.functionName();
        BallerinaModel.Expression.FunctionCall callExpr =
                new BallerinaModel.Expression.FunctionCall(fn, new String[]{inputVarName});
        String outputVarName = "output";
        BallerinaModel.VarDeclStatment outputVarDecl = new BallerinaModel.VarDeclStatment(XML, outputVarName, callExpr);
        body.add(outputVarDecl);

        Collection<TibcoModel.Scope.Flow.Link> destinationLinks = analysisResult.destinations(startActivity);
        for (TibcoModel.Scope.Flow.Link destinationLink : destinationLinks) {
            AnalysisResult.LinkData linkData = analysisResult.from(destinationLink);
            BallerinaModel.Action.WorkerSendAction sendAction = new BallerinaModel.Action.WorkerSendAction(
                    new BallerinaModel.Expression.VariableReference(outputVarName), linkData.workerName());
            body.add(new BallerinaModel.BallerinaStatement(sendAction + ";"));
        }

        return new BallerinaModel.NamedWorkerDecl(cx.startWorkerName, body);
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
                BallerinaModel.VarDeclStatment inputVarDecl = receiveVarFromPeer(startWorkerName, inputVarName);
                body.add(inputVarDecl);
                inputVarNames.add(inputVarName);
            } else {
                for (TibcoModel.Scope.Flow.Link each : linkPrev) {
                    AnalysisResult.LinkData data = analysisResult.from(each);
                    String inputVarName = "v" + inputCount++;
                    String peer = data.workerName();
                    BallerinaModel.VarDeclStatment inputVarDecl = receiveVarFromPeer(peer, inputVarName);
                    body.add(inputVarDecl);
                    inputVarNames.add(inputVarName);
                }
            }
        }
        int outPutCount = 0;
        for (TibcoModel.Scope.Flow.Activity destinations : analysisResult.destinations(link)) {
            AnalysisResult.ActivityData activityData = analysisResult.from(destinations);
            assert inputVarNames.size() == 1 : "Multiple input vars not supported";
            String inputVarName = inputVarNames.getFirst();
            String fn = activityData.functionName();
            BallerinaModel.Expression.FunctionCall callExpr = new BallerinaModel.Expression.FunctionCall(fn,
                    new String[]{inputVarName});
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
                AnalysisResult.LinkData destinationData = analysisResult.from(destinationLink);
                BallerinaModel.Action.WorkerSendAction sendAction = new BallerinaModel.Action.WorkerSendAction(
                        new BallerinaModel.Expression.VariableReference(outputVarName), destinationData.workerName());
                body.add(new BallerinaModel.BallerinaStatement(sendAction + ";"));
            }
        }
        return new BallerinaModel.NamedWorkerDecl(linkData.workerName(), body);
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
                new BallerinaModel.Expression.FunctionCall(cx.getToXmlFunction(),
                        new String[]{inputVariable});
        String inputXML = "inputXML";
        BallerinaModel.VarDeclStatment inputXMLVar =
                new BallerinaModel.VarDeclStatment(XML,
                        inputXML, toXMLCall);
        body.add(inputXMLVar);
        String processFunction = cx.getProcessFunction();
        BallerinaModel.VarDeclStatment xmlResult =
                new BallerinaModel.VarDeclStatment(XML,
                        "xmlResult",
                        new BallerinaModel.Expression.FunctionCall(processFunction, new String[]{inputXML}));
        body.add(xmlResult);
        String convertToTypeFunction = cx.getConvertToTypeFunction(returnType);
        BallerinaModel.VarDeclStatment result =
                new BallerinaModel.VarDeclStatment(returnType, "result",
                        new BallerinaModel.Expression.FunctionCall(convertToTypeFunction, new String[]{"xmlResult"}));
        body.add(result);
        BallerinaModel.Return<BallerinaModel.Expression.VariableReference> returnStatement =
                new BallerinaModel.Return<>(Optional.of(new BallerinaModel.Expression.VariableReference("result")));
        body.add(returnStatement);
        return new BallerinaModel.Function(Optional.of("public"), cx.getProcessStartFunctionName(),
                List.of(new BallerinaModel.Parameter(inputType.toString(), inputVariable)),
                Optional.of(returnType.toString()), body);
    }
}
