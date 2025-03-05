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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import ballerina.BallerinaModel;
import tibco.TibcoModel;

class ActivityContext {

    private final ProcessContext cx;
    private final String functionName;
    private final List<BallerinaModel.Statement> statements = new ArrayList<>();
    private final List<BallerinaModel.Parameter> parameters;
    private final Optional<String> returnType;
    private final Map<String, TibcoToBallerinaModelConverter.LinkHandler> linkHandlers = new HashMap<>();
    private int varCounter = 0;
    private String inputXMLVarName = null;

    private String getAnnonVarName() {
        return "var" + varCounter++;
    }

    ActivityContext(ProcessContext cx, String functionName, List<BallerinaModel.Parameter> parameters,
                    String returnType) {
        this.cx = cx;
        this.functionName = ConversionUtils.sanitizes(functionName);
        this.parameters = parameters;
        this.returnType = Optional.of(returnType);
    }

    ActivityContext(ProcessContext cx, String functionName, List<BallerinaModel.Parameter> parameters) {
        this.cx = cx;
        this.functionName = ConversionUtils.sanitizes(functionName);
        this.parameters = parameters;
        this.returnType = Optional.empty();
    }

    BallerinaModel.Expression.VariableReference inputVariable() {
        assert parameters.size() == 1;
        return new BallerinaModel.Expression.VariableReference(parameters.getFirst().name());
    }

    BallerinaModel.Expression.VariableReference getInputAsXml() {
        if (inputXMLVarName == null) {
            String inputXML = "inputXML";
            String toXMLFunction = cx.getToXmlFunction();
            BallerinaModel.Expression.VariableReference inputVariable = inputVariable();
            BallerinaModel.Expression.TypeCheckExpression checkIsXML =
                    new BallerinaModel.Expression.TypeCheckExpression(inputVariable,
                            BallerinaModel.TypeDesc.BuiltinType.XML);
            BallerinaModel.Expression.TernaryExpression ternaryExpr =
                    new BallerinaModel.Expression.TernaryExpression(checkIsXML, inputVariable,
                            new BallerinaModel.Expression.FunctionCall(toXMLFunction, new String[]{"input"}));
            addVarInitStatement(BallerinaModel.TypeDesc.BuiltinType.XML, inputXML, ternaryExpr);
            inputXMLVarName = inputXML;
        }
        return new BallerinaModel.Expression.VariableReference(inputXMLVarName);
    }

    BallerinaModel.Expression.VariableReference xsltTransform(
            BallerinaModel.Expression.VariableReference inputVariable,
            TibcoModel.Scope.Flow.Activity.Expression.XSLT xslt) {
        cx.addLibraryImport(Library.XSLT);
        BallerinaModel.Expression.FunctionCall callExpr =
                new BallerinaModel.Expression.FunctionCall("xslt:transform",
                        new String[]{inputVariable.varName(), "xml`" + xslt.expression() + "`"});
        BallerinaModel.Expression.CheckPanic checkPanic = new BallerinaModel.Expression.CheckPanic(callExpr);
        String varName = getAnnonVarName();
        addVarInitStatement(BallerinaModel.TypeDesc.BuiltinType.XML, varName, checkPanic);
        return new BallerinaModel.Expression.VariableReference(varName);
    }

    void addVarInitStatement(BallerinaModel.TypeDesc type, String varName, BallerinaModel.Expression expr) {
        statements.add(new BallerinaModel.VarDeclStatment(type, varName, expr));
    }

    void addStatement(BallerinaModel.Statement statement) {
        statements.add(statement);
    }

    void addComment(String comment) {
        statements.add(new BallerinaModel.Comment(comment));
    }

    Collection<BallerinaModel.Function> finalizeFunction() {
        return Stream.concat(linkHandlers.values().stream().map(TibcoToBallerinaModelConverter.LinkHandler::toFunction),
                        Stream.of(
                                new BallerinaModel.Function(Optional.empty(), functionName, parameters, returnType,
                                        statements)))
                .toList();
    }

    void addLinkHandler(String name) {
        linkHandlers.put(name,
                new TibcoToBallerinaModelConverter.LinkHandler(name, BallerinaModel.TypeDesc.BuiltinType.XML,
                        new ArrayList<>()));
    }

    ActivityContext registerWithLinkHandler(String linkName) {
        TibcoToBallerinaModelConverter.LinkHandler handler = linkHandlers.get(linkName);
        if (handler == null) {
            throw new IllegalArgumentException("Link handler not found: " + linkName);
        }
        String listenerName = cx.getAnnonFunctionName();
        handler.registerListener(listenerName);
        return new ActivityContext(cx, listenerName,
                List.of(new BallerinaModel.Parameter("xml", "input")),
                "xml");
    }

    void sendToTarget(TibcoModel.Scope.Flow.Activity.Target target,
                      BallerinaModel.Expression.VariableReference value) {
        TibcoToBallerinaModelConverter.LinkHandler handler = linkHandlers.get(target.linkName());
        if (handler == null) {
            throw new IllegalArgumentException("Link handler not found: " + target.linkName());
        }
        addStatement(new BallerinaModel.CallStatement(
                new BallerinaModel.Expression.FunctionCall(handler.name(), new String[]{value.toString()})));
    }

    void callProcess(String processName, BallerinaModel.Expression.VariableReference input) {
        addStatement(new BallerinaModel.CallStatement(
                new BallerinaModel.Expression.FunctionCall("PROC_" + processName, new String[]{input.varName()})));
    }
}
