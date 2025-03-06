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

import java.util.List;
import java.util.Optional;

import static ballerina.BallerinaModel.TypeDesc.BuiltinType.XML;

import ballerina.BallerinaModel;
import org.jetbrains.annotations.NotNull;
import tibco.TibcoModel;

class ActivityContext {

    public final ProjectContext.ProcessContext processContext;
    private final TibcoModel.Scope.Flow.Activity activity;
    private int varCounter = 0;
    private String inputXMLVarName = null;

    String getAnnonVarName() {
        return "var" + varCounter++;
    }

    ActivityContext(ProjectContext.ProcessContext processContext, TibcoModel.Scope.Flow.Activity activity) {
        this.activity = activity;
        this.processContext = processContext;
    }

    BallerinaModel.Expression.VariableReference inputVariable() {
        return new BallerinaModel.Expression.VariableReference("input");
    }

    BallerinaModel.Expression.VariableReference getInputAsXml(List<BallerinaModel.Statement> body) {
        if (inputXMLVarName == null) {
            addTypeConversionLogic(body);
        }
        return new BallerinaModel.Expression.VariableReference(inputXMLVarName);
    }

    private void addTypeConversionLogic(List<BallerinaModel.Statement> body) {
        BallerinaModel.Expression.TernaryExpression ternaryExpr = addTypeConversionIfNeeded(inputVariable());
        String inputXML = "inputXML";
        body.add(new BallerinaModel.VarDeclStatment(XML, inputXML, ternaryExpr));
        inputXMLVarName = inputXML;
    }

    private BallerinaModel.Expression.@NotNull TernaryExpression addTypeConversionIfNeeded(
            BallerinaModel.Expression.VariableReference inputVariable) {
        BallerinaModel.Expression.TypeCheckExpression checkIsXML =
                new BallerinaModel.Expression.TypeCheckExpression(inputVariable, XML);
        String toXMLFunction = processContext.getToXmlFunction();
        return new BallerinaModel.Expression.TernaryExpression(checkIsXML, inputVariable,
                new BallerinaModel.Expression.FunctionCall(toXMLFunction, new String[]{inputVariable.varName()}));
    }

    public boolean isStartActivity(TibcoModel.Scope.Flow.Activity activity) {
        return processContext.analysisResult.startActivity(processContext.process).equals(activity);
    }

    public String getProcessStartFunctionName(String processName) {
        return processContext.getProcessStartFunction(processName);
    }

    public String functionName() {
        return processContext.analysisResult.from(activity).functionName();
    }

    public List<BallerinaModel.Parameter> parameters() {
        return List.of(new BallerinaModel.Parameter(XML, "input"));
    }

    public Optional<String> returnType() {
        return Optional.of(XML.toString());
    }
}
