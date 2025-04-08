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

import ballerina.BallerinaModel;
import converter.tibco.analyzer.AnalysisResult;
import org.jetbrains.annotations.NotNull;
import tibco.TibcoModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static ballerina.BallerinaModel.TypeDesc.BuiltinType.ERROR;
import static ballerina.BallerinaModel.TypeDesc.BuiltinType.JSON;
import static ballerina.BallerinaModel.TypeDesc.BuiltinType.STRING;
import static ballerina.BallerinaModel.TypeDesc.BuiltinType.XML;

class ActivityConverter {

    private ActivityConverter() {
    }

    public static BallerinaModel.Function convertActivity(ProcessContext cx, TibcoModel.Scope.Flow.Activity activity) {
        return convertActivity(new ActivityContext(cx, activity), activity);
    }

    private static BallerinaModel.Function convertActivity(ActivityContext cx,
                                                           TibcoModel.Scope.Flow.Activity activity) {
        List<BallerinaModel.Statement> body = switch (activity) {
            case TibcoModel.Scope.Flow.Activity.ActivityExtension activityExtension ->
                    convertActivityExtension(cx, activityExtension);
            case TibcoModel.Scope.Flow.Activity.Empty ignored -> convertEmptyAction(cx);
            case TibcoModel.Scope.Flow.Activity.ExtActivity extActivity -> convertExtActivity(cx, extActivity);
            case TibcoModel.Scope.Flow.Activity.Invoke invoke -> convertInvoke(cx, invoke);
            case TibcoModel.Scope.Flow.Activity.Pick pick -> convertPickAction(cx, pick);
            case TibcoModel.Scope.Flow.Activity.CatchAll catchAll -> convertCatchAll(cx, catchAll);
            case TibcoModel.Scope.Flow.Activity.ReceiveEvent receiveEvent -> convertReceiveEvent(cx, receiveEvent);
            case TibcoModel.Scope.Flow.Activity.Reply reply -> convertReply(cx, reply);
            case TibcoModel.Scope.Flow.Activity.UnhandledActivity unhandledActivity ->
                    convertUnhandledActivity(cx, unhandledActivity);
            case TibcoModel.Scope.Flow.Activity.Throw throwActivity -> convertThrowActivity(cx, throwActivity);
        };
        return new BallerinaModel.Function(Optional.empty(), cx.functionName(), cx.parameters(),
                Optional.of(ActivityContext.returnType().toString()), body);
    }

    private static List<BallerinaModel.Statement> convertThrowActivity(
            ActivityContext cx,
            TibcoModel.Scope.Flow.Activity.Throw throwActivity
    ) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        BallerinaModel.Expression.VariableReference input = cx.getInputAsXml();
        BallerinaModel.Expression.VariableReference result;
        if (throwActivity.inputBindings().isEmpty()) {
            result = input;
        } else {
            List<BallerinaModel.VarDeclStatment> inputBindings =
                    convertInputBindings(cx, input, throwActivity.inputBindings());
            body.addAll(inputBindings);
            result = inputBindings.getLast().ref();
        }
        // TODO: set the body correctly
        BallerinaModel.VarDeclStatment errorValue = new BallerinaModel.VarDeclStatment(ERROR, cx.getAnnonVarName(),
                new BallerinaModel.BallerinaExpression("error(\"TODO: create error value\")"));
        body.add(errorValue);
        body.add(new BallerinaModel.BallerinaStatement("panic " + errorValue.varName() + ";"));

        return body;
    }

    private static List<BallerinaModel.Statement> convertCatchAll(ActivityContext cx,
                                                                  TibcoModel.Scope.Flow.Activity.CatchAll catchAll) {
        return convertEmptyAction(cx);
    }

    private static List<BallerinaModel.Statement> convertUnhandledActivity(
            ActivityContext cx,
            TibcoModel.Scope.Flow.Activity.UnhandledActivity unhandledActivity) {
        BallerinaModel.Expression.VariableReference inputXml = cx.getInputAsXml();
        return List.of(new BallerinaModel.Comment(unhandledActivity.reason()), new BallerinaModel.Return<>(inputXml));
    }

    private static List<BallerinaModel.Statement> convertReply(ActivityContext cx,
                                                               TibcoModel.Scope.Flow.Activity.Reply reply) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        BallerinaModel.Expression.VariableReference input = cx.getInputAsXml();
        BallerinaModel.Expression.VariableReference result;
        if (reply.inputBindings().isEmpty()) {
            result = input;
        } else {
            List<BallerinaModel.VarDeclStatment> inputBindings = convertInputBindings(cx, input, reply.inputBindings());
            body.addAll(inputBindings);
            result = new BallerinaModel.Expression.VariableReference(inputBindings.getLast().varName());
        }
        body.add(new BallerinaModel.Return<>(result));
        return body;
    }

    private static List<BallerinaModel.Statement> convertPickAction(ActivityContext cx,
                                                                    TibcoModel.Scope.Flow.Activity.Pick pick) {
        return convertEmptyAction(cx);
    }

    private static List<BallerinaModel.Statement> convertEmptyAction(ActivityContext cx) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        BallerinaModel.Expression.VariableReference inputXml = cx.getInputAsXml();
        body.add(new BallerinaModel.Return<>(Optional.of(inputXml)));
        return body;
    }

    private static List<BallerinaModel.Statement> convertActivityExtension(
            ActivityContext cx,
            TibcoModel.Scope.Flow.Activity.ActivityExtension activityExtension
    ) {
        var inputBindings = convertInputBindings(cx, cx.getInputAsXml(), activityExtension.inputBindings());
        List<BallerinaModel.Statement> body = new ArrayList<>(inputBindings);
        BallerinaModel.Expression.VariableReference result =
                new BallerinaModel.Expression.VariableReference(inputBindings.getLast().varName());

        TibcoModel.Scope.Flow.Activity.ActivityExtension.Config config = activityExtension.config();
        List<BallerinaModel.Statement> rest = switch (config) {
            case TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.End ignored ->
                    List.of(new BallerinaModel.Return<>(result));
            case TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.HTTPSend httpSend ->
                    createHttpSend(cx, result, httpSend, activityExtension.outputVariable());
            case TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.JsonOperation jsonOperation ->
                    createJsonOperation(cx, result, jsonOperation, activityExtension.outputVariable());
            case TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.SQL sql -> createSQLOperation(cx, result, sql);
            case TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.SendHTTPResponse ignored ->
                    List.of(new BallerinaModel.Return<>(result));
            case TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.FileWrite fileWrite ->
                    createFileWriteOperation(cx, result, fileWrite);
            case TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.Log log -> createLogOperation(cx, result, log);
            case TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.RenderXML renderXML ->
                    createRenderXml(cx, result, renderXML);
        };
        body.addAll(rest);
        return body;
    }

    private static List<BallerinaModel.Statement> createRenderXml(
            ActivityContext cx,
            BallerinaModel.Expression.VariableReference result,
            TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.RenderXML renderXML
    ) {
        return List.of(new BallerinaModel.Return<>(result));
    }

    private static List<BallerinaModel.Statement> createLogOperation(
            ActivityContext cx,
            BallerinaModel.Expression.VariableReference result,
            TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.Log log
    ) {
        List<BallerinaModel.Statement> body = new ArrayList<>();

        BallerinaModel.TypeDesc dataType = cx.getLogInputType();
        BallerinaModel.VarDeclStatment dataDecl = new BallerinaModel.VarDeclStatment(dataType, cx.getAnnonVarName(),
                new BallerinaModel.Expression.FunctionCall(cx.getConvertToTypeFunction(dataType), List.of(result)));
        body.add(dataDecl);

        BallerinaModel.CallStatement callStatement = new BallerinaModel.CallStatement(
                new BallerinaModel.Expression.FunctionCall(cx.getLogFunction(), List.of(
                        new BallerinaModel.Expression.VariableReference(dataDecl.varName()))));
        body.add(callStatement);

        body.add(new BallerinaModel.Return<>(result));

        return body;
    }

    private static List<BallerinaModel.Statement> createFileWriteOperation(
            ActivityContext cx,
            BallerinaModel.Expression.VariableReference result,
            TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.FileWrite fileWrite
    ) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        BallerinaModel.TypeDesc dataType = cx.getFileWriteConfigType();
        BallerinaModel.VarDeclStatment dataDecl = new BallerinaModel.VarDeclStatment(dataType, cx.getAnnonVarName(),
                new BallerinaModel.Expression.FunctionCall(cx.getConvertToTypeFunction(dataType), List.of(result)));
        body.add(dataDecl);

        BallerinaModel.VarDeclStatment fileNameDecl = new BallerinaModel.VarDeclStatment(STRING, cx.getAnnonVarName(),
                new BallerinaModel.Expression.TypeCast(STRING, new BallerinaModel.Expression.FieldAccess(
                        new BallerinaModel.Expression.VariableReference(dataDecl.varName()),
                        "fileName"))
        );
        body.add(fileNameDecl);

        BallerinaModel.VarDeclStatment textContentDecl =
                new BallerinaModel.VarDeclStatment(STRING, cx.getAnnonVarName(),
                        new BallerinaModel.Expression.FieldAccess(
                                new BallerinaModel.Expression.VariableReference(dataDecl.varName()),
                                "textContent"));
        body.add(textContentDecl);

        BallerinaModel.CallStatement callStatement = new BallerinaModel.CallStatement(
                new BallerinaModel.Expression.CheckPanic(new BallerinaModel.Expression.FunctionCall(
                        cx.getFileWriteFunction(),
                        List.of(new BallerinaModel.Expression.VariableReference(fileNameDecl.varName()),
                                new BallerinaModel.Expression.VariableReference(textContentDecl.varName())))));
        body.add(callStatement);

        body.add(new BallerinaModel.Return<>(result));
        return body;
    }

    private static List<BallerinaModel.Statement> createSQLOperation(
            ActivityContext cx,
            BallerinaModel.Expression.VariableReference inputVar,
            TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.SQL sql
    ) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        BallerinaModel.TypeDesc dataType = ConversionUtils.createQueryInputType(cx, sql);
        BallerinaModel.VarDeclStatment dataDecl = new BallerinaModel.VarDeclStatment(dataType, "data",
                new BallerinaModel.Expression.FunctionCall(cx.getConvertToTypeFunction(dataType), List.of(inputVar)));
        body.add(dataDecl);
        BallerinaModel.Expression.VariableReference paramData = new BallerinaModel.Expression.VariableReference(
                dataDecl.varName());

        BallerinaModel.VarDeclStatment queryDecl = ConversionUtils.createQueryDecl(cx, paramData, sql);
        body.add(queryDecl);
        BallerinaModel.Expression.VariableReference query = new BallerinaModel.Expression.VariableReference(
                queryDecl.varName());

        BallerinaModel.Expression.VariableReference dbClient = cx.client(sql.sharedResourcePropertyName());
        BallerinaModel.TypeDesc executionResultType = cx.processContext.getTypeByName("sql:ExecutionResult");
        BallerinaModel.VarDeclStatment result =
                new BallerinaModel.VarDeclStatment(executionResultType, cx.getAnnonVarName(),
                        new BallerinaModel.Expression.CheckPanic(
                                new BallerinaModel.Action.RemoteMethodCallAction(
                                        dbClient, "execute", List.of(query))));
        body.add(result);

        // TODO: handle things like select properly
        body.add(new BallerinaModel.Return<>(inputVar));
        return body;
    }

    private static List<BallerinaModel.Statement> createJsonOperation(
            ActivityContext cx,
            BallerinaModel.Expression.VariableReference inputVar,
            TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.JsonOperation jsonOperation,
            Optional<String> outputVarName
    ) {
        // TODO: how to implement this
        return outputVarName.map(
                        s -> List.of(addToContext(cx, inputVar, s), new BallerinaModel.Return<>(inputVar)))
                .orElseGet(() -> List.of(new BallerinaModel.Return<>(inputVar)));
    }

    private static List<BallerinaModel.Statement> createHttpSend(
            ActivityContext cx,
            BallerinaModel.Expression.VariableReference configVar,
            TibcoModel.Scope.Flow.Activity.ActivityExtension.Config.HTTPSend httpSend,
            Optional<String> outputVarName
    ) {
        String parseFn = cx.getParseHttpConfigFunction();
        List<BallerinaModel.Statement> body = new ArrayList<>();
        BallerinaModel.TypeDesc.TypeReference httpConfigType = cx.getHttpConfigType();
        BallerinaModel.Expression.FunctionCall parseCall =
                new BallerinaModel.Expression.FunctionCall(parseFn, new String[]{configVar.varName()});
        BallerinaModel.VarDeclStatment configVarDecl =
                new BallerinaModel.VarDeclStatment(httpConfigType, cx.getAnnonVarName(), parseCall);
        body.add(configVarDecl);
        BallerinaModel.Expression.VariableReference configVarRef =
                new BallerinaModel.Expression.VariableReference(configVarDecl.varName());
        BallerinaModel.Expression.VariableReference client = cx.client(httpSend.httpClientResource());

        BallerinaModel.Expression.FunctionCall pathGetFunctionCall =
                new BallerinaModel.Expression.FunctionCall(Intrinsics.CREATE_HTTP_REQUEST_PATH_FROM_CONFIG.name,
                        List.of(configVarRef));
        BallerinaModel.VarDeclStatment requestURI =
                new BallerinaModel.VarDeclStatment(STRING, cx.getAnnonVarName(), pathGetFunctionCall);
        body.add(requestURI);

        // TODO: handle non-post
        BallerinaModel.Action.RemoteMethodCallAction call = new BallerinaModel.Action.RemoteMethodCallAction(
                new BallerinaModel.Expression.VariableReference(
                        client.varName()),
                "/" + requestURI.varName() + ".post",
                List.of(new BallerinaModel.Expression.FieldAccess(configVarRef, "PostData"),
                        new BallerinaModel.Expression.FieldAccess(configVarRef, "Headers")));
        BallerinaModel.VarDeclStatment responseDecl =
                new BallerinaModel.VarDeclStatment(JSON, cx.getAnnonVarName(),
                        new BallerinaModel.Expression.Check(call));
        body.add(responseDecl);

        String jsonToXmlFunction = cx.processContext.getJsonToXMLFunction();
        BallerinaModel.Expression.FunctionCall jsonToXmlFunctionCall =
                new BallerinaModel.Expression.FunctionCall(jsonToXmlFunction, new String[]{responseDecl.varName()});
        BallerinaModel.VarDeclStatment resultDecl =
                new BallerinaModel.VarDeclStatment(XML, cx.getAnnonVarName(),
                        new BallerinaModel.Expression.Check(jsonToXmlFunctionCall));
        body.add(resultDecl);
        BallerinaModel.Expression.VariableReference result =
                new BallerinaModel.Expression.VariableReference(resultDecl.varName());
        outputVarName.ifPresent(s -> body.add(addToContext(cx, result, s)));
        body.add(new BallerinaModel.Return<>(result));

        return body;
    }

    private static List<BallerinaModel.Statement> convertReceiveEvent(
            ActivityContext cx,
            TibcoModel.Scope.Flow.Activity.ReceiveEvent receiveEvent
    ) {
        // This is just a no-op since, we have created the service already and connected it to the process function
        // when handling the WSDL type definition.
        return createNoOp(cx);
    }

    private static @NotNull List<BallerinaModel.Statement> createNoOp(ActivityContext cx) {
        return List.of(new BallerinaModel.Return<>(cx.getInputAsXml()));
    }

    private static List<BallerinaModel.Statement> convertInvoke(ActivityContext cx,
                                                                TibcoModel.Scope.Flow.Activity.Invoke invoke) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        AnalysisResult ar = cx.processContext.analysisResult;
        TibcoModel.PartnerLink.Binding binding = ar.getBinding(invoke.partnerLink());
        BallerinaModel.VarDeclStatment clientDecl = createClientForBinding(cx, binding);
        body.add(clientDecl);
        // TODO: may be this needs to be json
        BallerinaModel.VarDeclStatment bindingCallResult =
                createBindingCall(cx, binding, new BallerinaModel.Expression.VariableReference(clientDecl.varName()),
                        cx.getInputAsXml());
        body.add(bindingCallResult);
        String jsonToXMLFunction = cx.processContext.getJsonToXMLFunction();
        BallerinaModel.Expression.FunctionCall jsonToXMLFunctionCall =
                new BallerinaModel.Expression.FunctionCall(jsonToXMLFunction,
                        new String[]{bindingCallResult.varName()});
        BallerinaModel.VarDeclStatment resultDecl =
                new BallerinaModel.VarDeclStatment(XML, cx.getAnnonVarName(),
                        new BallerinaModel.Expression.Check(jsonToXMLFunctionCall));
        body.add(resultDecl);
        BallerinaModel.Expression.VariableReference result =
                new BallerinaModel.Expression.VariableReference(resultDecl.varName());

        body.add(addToContext(cx, result, invoke.outputVariable()));
        body.add(new BallerinaModel.Return<>(result));
        return body;
    }

    private static BallerinaModel.VarDeclStatment createBindingCall(ActivityContext cx,
                                                                    TibcoModel.PartnerLink.Binding binding,
                                                                    BallerinaModel.Expression.VariableReference client,
                                                                    BallerinaModel.Expression.VariableReference value) {
        String path = binding.path().path();
        assert binding.operation().method() == TibcoModel.PartnerLink.Binding.Operation.Method.POST;
        BallerinaModel.Action.RemoteMethodCallAction call =
                new BallerinaModel.Action.RemoteMethodCallAction(client, "post",
                        List.of(new BallerinaModel.Expression.StringConstant(path), value));
        return new BallerinaModel.VarDeclStatment(JSON, cx.getAnnonVarName(),
                new BallerinaModel.Expression.Check(call));
    }

    private static BallerinaModel.VarDeclStatment createClientForBinding(ActivityContext cx,
                                                                         TibcoModel.PartnerLink.Binding binding) {
        String basePath = binding.path().basePath();
        return createHTTPClientWithBasePath(cx, new BallerinaModel.Expression.StringConstant(basePath));
    }

    private static BallerinaModel.@NotNull VarDeclStatment createHTTPClientWithBasePath(
            ActivityContext cx,
            BallerinaModel.Expression basePath
    ) {
        BallerinaModel.Expression.NewExpression newExpression =
                new BallerinaModel.Expression.NewExpression(List.of(basePath));
        return new BallerinaModel.VarDeclStatment(cx.processContext.getTypeByName("http:Client"), cx.getAnnonVarName(),
                new BallerinaModel.Expression.Check(newExpression));
    }

    private static List<BallerinaModel.Statement> convertExtActivity(
            ActivityContext cx,
            TibcoModel.Scope.Flow.Activity.ExtActivity extActivity
    ) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        BallerinaModel.Expression.VariableReference result = cx.getInputAsXml();
        if (!extActivity.inputBindings().isEmpty()) {
            List<BallerinaModel.VarDeclStatment> inputBindings =
                    convertInputBindings(cx, result, extActivity.inputBindings());
            body.addAll(inputBindings);
            result = new BallerinaModel.Expression.VariableReference(inputBindings.getLast().varName());
        }
        var startFunction = cx.getProcessStartFunctionName(extActivity.callProcess().subprocessName());

        String convertToTypeFunction = cx.processContext.getConvertToTypeFunction(startFunction.inputType());
        BallerinaModel.Expression.FunctionCall convertToTypeFunctionCall =
                new BallerinaModel.Expression.FunctionCall(convertToTypeFunction, new String[]{result.varName()});

        BallerinaModel.VarDeclStatment resultDecl =
                new BallerinaModel.VarDeclStatment(XML, cx.getAnnonVarName(),
                        new BallerinaModel.Expression.Check(
                                new BallerinaModel.Expression.FunctionCall(cx.processContext.getToXmlFunction(),
                                        List.of(new BallerinaModel.Expression.Check(
                                                new BallerinaModel.Expression.Trap(
                                                        new BallerinaModel.Expression.FunctionCall(startFunction.name(),
                                                                List.of(convertToTypeFunctionCall))))))));
        body.add(resultDecl);
        BallerinaModel.Expression.VariableReference resultRef = resultDecl.ref();
        body.add(addToContext(cx, resultRef, extActivity.outputVariable()));
        body.add(new BallerinaModel.Return<>(resultRef));
        return body;
    }

    private static List<BallerinaModel.VarDeclStatment> convertInputBindings(
            ActivityContext cx,
            BallerinaModel.Expression.VariableReference input,
            Collection<TibcoModel.Scope.Flow.Activity.InputBinding> inputBindings
    ) {
        List<BallerinaModel.VarDeclStatment> varDelStatements = new ArrayList<>();
        BallerinaModel.Expression.VariableReference last = input;
        for (TibcoModel.Scope.Flow.Activity.InputBinding transform : inputBindings) {
            TibcoModel.Scope.Flow.Activity.Expression.XSLT xslt = transform.xslt();

            BallerinaModel.VarDeclStatment varDecl = xsltTransform(cx, last, xslt);
            varDelStatements.add(varDecl);
            last = new BallerinaModel.Expression.VariableReference(varDecl.varName());
        }
        return varDelStatements;
    }

    private static BallerinaModel.VarDeclStatment xsltTransform(
            ActivityContext cx,
            BallerinaModel.Expression.VariableReference inputVariable,
            TibcoModel.Scope.Flow.Activity.Expression.XSLT xslt
    ) {
        cx.addLibraryImport(Library.XSLT);
        String stylesheetTransformer = cx.getTransformXSLTFn();
        BallerinaModel.Expression.FunctionCall transformerCall =
                new BallerinaModel.Expression.FunctionCall(stylesheetTransformer,
                        List.of(new BallerinaModel.Expression.XMLTemplate(
                                replaceVariableReferences(cx, xslt.expression()))));
        BallerinaModel.Expression.FunctionCall callExpr = new BallerinaModel.Expression.FunctionCall("xslt:transform",
                new BallerinaModel.Expression[]{inputVariable,
                        transformerCall, cx.contextVarRef()});
        return new BallerinaModel.VarDeclStatment(XML, cx.getAnnonVarName(),
                new BallerinaModel.Expression.Check(callExpr));
    }

    private static String replaceVariableReferences(ActivityContext cx, String expression) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("bw:getModuleProperty\\('([^']+)'\\)");
        java.util.regex.Matcher matcher = pattern.matcher(expression);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String configVarName = cx.getConfigVarName(propertyName);
            matcher.appendReplacement(result, "\\$\\{" + configVarName + "\\}");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static BallerinaModel.Statement addToContext(ActivityContext cx,
                                                         BallerinaModel.Expression.VariableReference value,
                                                         String key) {
        assert !key.isEmpty();
        String addToContextFn = cx.getAddToContextFn();
        return new BallerinaModel.CallStatement(
                new BallerinaModel.Expression.FunctionCall(addToContextFn, List.of(cx.contextVarRef(),
                        new BallerinaModel.Expression.StringConstant(key), value)));
    }
}
