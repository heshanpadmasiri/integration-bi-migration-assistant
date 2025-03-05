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
import tibco.TibcoModel;

class ActivityContext {

    public final ProcessContext processContext;
    private final TibcoModel.Scope.Flow.Activity activity;
    private int varCounter = 0;
    private final int index;
    private String inputXMLVarName = null;

    String getAnnonVarName() {
        return "var" + varCounter++;
    }

    ActivityContext(ProcessContext processContext, TibcoModel.Scope.Flow.Activity activity) {
        this.activity = activity;
        this.processContext = processContext;
        index = processContext.acitivityCounter++;
    }

    BallerinaModel.Expression.VariableReference inputVariable() {
        return new BallerinaModel.Expression.VariableReference("input");
    }

    BallerinaModel.Expression.VariableReference getInputAsXml(List<BallerinaModel.Statement> body) {
        if (inputXMLVarName == null) {
            String inputXML = "inputXML";
            String toXMLFunction = processContext.getToXmlFunction();
            BallerinaModel.Expression.VariableReference inputVariable = inputVariable();
            BallerinaModel.Expression.TypeCheckExpression checkIsXML =
                    new BallerinaModel.Expression.TypeCheckExpression(inputVariable,
                            XML);
            BallerinaModel.Expression.TernaryExpression ternaryExpr =
                    new BallerinaModel.Expression.TernaryExpression(checkIsXML, inputVariable,
                            new BallerinaModel.Expression.FunctionCall(toXMLFunction,
                                    new String[]{inputVariable.varName()}));
            body.add(new BallerinaModel.VarDeclStatment(XML, inputXML, ternaryExpr));
            inputXMLVarName = inputXML;
        }
        return new BallerinaModel.Expression.VariableReference(inputXMLVarName);
    }

    public boolean isStartActivity(TibcoModel.Scope.Flow.Activity activity) {
        return processContext.analysisResult.startActivity(processContext.process).equals(activity);
    }

    public String getProcessStartFunctionName(String processName) {
        // FIXME: this needs to call back to the conversion context and get the result
        return ConversionUtils.sanitizes(processName) + "_start";
    }

    public String functionName() {
        String namePrefix = switch (activity) {
            case TibcoModel.Scope.Flow.Activity.ActivityExtension ignored -> "activity_ext";
            case TibcoModel.Scope.Flow.Activity.Empty ignored -> "empty";
            case TibcoModel.Scope.Flow.Activity.ExtActivity ignored -> "ext_activity";
            case TibcoModel.Scope.Flow.Activity.Invoke ignored -> "invoke";
            case TibcoModel.Scope.Flow.Activity.Pick ignored -> "pick";
            case TibcoModel.Scope.Flow.Activity.ReceiveEvent ignored -> "receive_event";
            case TibcoModel.Scope.Flow.Activity.Reply ignored -> "reply";
        };
        return namePrefix + "_" + index;
    }

    public List<BallerinaModel.Parameter> parameters() {
        return List.of(new BallerinaModel.Parameter(XML.toString(), "input"));
    }

    public Optional<String> returnType() {
        return Optional.of(XML.toString());
    }
}
