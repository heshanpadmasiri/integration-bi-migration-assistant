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
import java.util.List;
import java.util.Optional;

import ballerina.BallerinaModel;
import tibco.TibcoModel;

public class TibcoToBallerinaModelConverter {

    private TibcoToBallerinaModelConverter() {

    }

    static BallerinaModel.Module convertProcess(TibcoModel.Process process) {
        return convertProcess(new ProcessContext(process), process);
    }

    private static BallerinaModel.Module convertProcess(ProcessContext cx, TibcoModel.Process process) {
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
        // TODO: generate module variables

        List<BallerinaModel.Function> functions = new ArrayList<>();
        cx.analysisResult.activities().stream()
                .map(activity -> ActivityConverter.convertActivity(cx, activity))
                .forEach(functions::add);
        // FIXME: handle start activity
        functions.add(generateStartFunction(cx, cx.analysisResult.startActivity(process)));

        var textDocument = cx.serialize(moduleTypeDefs, service, functions);
        String name = process.name();
        return new BallerinaModel.Module(name, List.of(textDocument));
    }

    private static BallerinaModel.Function generateStartFunction(ProcessContext cx,
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
                new BallerinaModel.VarDeclStatment(BallerinaModel.TypeDesc.BuiltinType.XML,
                        inputXML, toXMLCall);
        body.add(inputXMLVar);
        String processFunction = cx.getProcessFunction();
        BallerinaModel.VarDeclStatment xmlResult =
                new BallerinaModel.VarDeclStatment(BallerinaModel.TypeDesc.BuiltinType.XML,
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
