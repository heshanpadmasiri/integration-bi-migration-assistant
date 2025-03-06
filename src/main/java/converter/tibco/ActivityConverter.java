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
import java.util.List;
import java.util.Optional;

import static ballerina.BallerinaModel.TypeDesc.BuiltinType.XML;

import ballerina.BallerinaModel;
import tibco.TibcoModel;

class ActivityConverter {

    private ActivityConverter() {
    }

    public static BallerinaModel.Function convertActivity(ProjectContext.ProcessContext cx,
                                                          TibcoModel.Scope.Flow.Activity activity) {
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
            case TibcoModel.Scope.Flow.Activity.ReceiveEvent receiveEvent -> convertReceiveEvent(cx, receiveEvent);
            case TibcoModel.Scope.Flow.Activity.Reply reply -> convertReply(cx, reply);
        };
        return new BallerinaModel.Function(Optional.empty(), cx.functionName(), cx.parameters(), cx.returnType(), body);
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
            result =
                    new BallerinaModel.Expression.VariableReference(inputBindings.getLast().varName());
        }
        body.add(new BallerinaModel.Return<>(result));
        return body;
    }

    private static List<BallerinaModel.Statement> convertPickAction(ActivityContext cx,
                                                                    TibcoModel.Scope.Flow.Activity.Pick pick) {
        if (!cx.isStartActivity(pick)) {
            throw new UnsupportedOperationException("Converting nested pick action not supported");
        }
        return convertEmptyAction(cx);
    }

    private static List<BallerinaModel.Statement> convertEmptyAction(ActivityContext cx) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        BallerinaModel.Expression.VariableReference inputXml = cx.getInputAsXml();
        body.add(new BallerinaModel.Return<>(Optional.of(inputXml)));
        return body;
    }

    private static List<BallerinaModel.Statement> convertActivityExtension(ActivityContext cx,
                                                                           TibcoModel.Scope.Flow.Activity.ActivityExtension activityExtension) {
        // FIXME:
        return List.of(new BallerinaModel.Return<>(cx.getInputAsXml()));
    }

    private static List<BallerinaModel.Statement> convertReceiveEvent(ActivityContext cx,
                                                                      TibcoModel.Scope.Flow.Activity.ReceiveEvent receiveEvent) {
        // FIXME:
        return List.of(new BallerinaModel.Return<>(cx.getInputAsXml()));
    }

    private static List<BallerinaModel.Statement> convertInvoke(ActivityContext cx,
                                                                TibcoModel.Scope.Flow.Activity.Invoke invoke) {
        // FIXME:
        return List.of(new BallerinaModel.Return<>(cx.getInputAsXml()));
    }

    private static List<BallerinaModel.Statement> convertExtActivity(ActivityContext fx,
                                                                     TibcoModel.Scope.Flow.Activity.ExtActivity extActivity) {
        List<BallerinaModel.Statement> body = new ArrayList<>();
        BallerinaModel.Expression.VariableReference result = fx.getInputAsXml();
        if (!extActivity.inputBindings().isEmpty()) {
            List<BallerinaModel.VarDeclStatment> inputBindings =
                    convertInputBindings(fx, result, extActivity.inputBindings());
            body.addAll(inputBindings);
            result = new BallerinaModel.Expression.VariableReference(inputBindings.getLast().varName());
        }
        // FIXME: convert to type
        var startFunction = fx.getProcessStartFunctionName(extActivity.callProcess().subprocessName());

        String convertToTypeFunction = fx.processContext.getConvertToTypeFunction(startFunction.inputType());
        BallerinaModel.Expression.FunctionCall convertToTypeFunctionCall =
                new BallerinaModel.Expression.FunctionCall(convertToTypeFunction, new String[]{result.varName()});

        BallerinaModel.Expression.FunctionCall startFunctionCall =
                new BallerinaModel.Expression.FunctionCall(startFunction.name(),
                        new BallerinaModel.Expression[]{convertToTypeFunctionCall});
        BallerinaModel.Expression.FunctionCall convertToXmlCall =
                new BallerinaModel.Expression.FunctionCall(fx.processContext.getToXmlFunction(),
                        new BallerinaModel.Expression[]{startFunctionCall});
        body.add(new BallerinaModel.Return<>(convertToXmlCall));
        return body;
    }

    private static List<BallerinaModel.VarDeclStatment> convertInputBindings(ActivityContext cx,
                                                                             BallerinaModel.Expression.VariableReference input,
                                                                             Collection<TibcoModel.Scope.Flow.Activity.InputBinding> inputBindings) {
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

    private static BallerinaModel.VarDeclStatment xsltTransform(ActivityContext cx,
                                                                BallerinaModel.Expression.VariableReference inputVariable,
                                                                TibcoModel.Scope.Flow.Activity.Expression.XSLT xslt) {
        cx.processContext.addLibraryImport(Library.XSLT);
        BallerinaModel.Expression.FunctionCall callExpr = new BallerinaModel.Expression.FunctionCall("xslt:transform",
                new String[]{inputVariable.varName(), "xml`" + xslt.expression() + "`"});
        BallerinaModel.Expression.CheckPanic checkPanic = new BallerinaModel.Expression.CheckPanic(callExpr);
        return new BallerinaModel.VarDeclStatment(XML, cx.getAnnonVarName(), checkPanic);
    }
}
