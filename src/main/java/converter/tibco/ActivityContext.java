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
import tibco.TibcoModel;

import java.util.List;

import static ballerina.BallerinaModel.TypeDesc.BuiltinType.ERROR;
import static ballerina.BallerinaModel.TypeDesc.BuiltinType.XML;

class ActivityContext {

    public final ProcessContext processContext;
    private final TibcoModel.Scope.Flow.Activity activity;
    private static final String INPUT_VAR_NAME = "input";
    private int varCounter = 0;

    String getAnnonVarName() {
        return "var" + varCounter++;
    }

    ActivityContext(ProcessContext processContext, TibcoModel.Scope.Flow.Activity activity) {
        this.activity = activity;
        this.processContext = processContext;
    }

    BallerinaModel.Expression.VariableReference getInputAsXml() {
        return new BallerinaModel.Expression.VariableReference(INPUT_VAR_NAME);
    }

    public ProjectContext.FunctionData getProcessStartFunctionName(String processName) {
        return processContext.getProcessStartFunction(processName);
    }

    public String functionName() {
        return processContext.analysisResult.from(activity).functionName();
    }

    public List<BallerinaModel.Parameter> parameters() {
        return List.of(new BallerinaModel.Parameter(XML, INPUT_VAR_NAME),
                new BallerinaModel.Parameter(processContext.contextType(), ProcessContext.CONTEXT_VAR_NAME));
    }

    public static BallerinaModel.TypeDesc returnType() {
        return new BallerinaModel.TypeDesc.UnionTypeDesc(List.of(XML, ERROR));
    }

    public String getParseHttpConfigFunction() {
        return processContext.getParseHttpConfigFunction();
    }

    public BallerinaModel.TypeDesc.TypeReference getHttpConfigType() {
        return processContext.getHttpConfigType();
    }

    public BallerinaModel.Expression.VariableReference addConfigurableVariable(
            BallerinaModel.TypeDesc td, String name) {
        return processContext.addConfigurableVariable(td, name);
    }

    public BallerinaModel.Expression contextVarRef() {
        return processContext.contextVarRef();
    }

    public BallerinaModel.Expression.VariableReference dbClient(String sharedResourcePropertyName) {
        return processContext.dbClient(sharedResourcePropertyName);
    }

    public String getConvertToTypeFunction(BallerinaModel.TypeDesc targetType) {
        return processContext.getConvertToTypeFunction(targetType);
    }

    public String getAddToContextFn() {
        return processContext.getAddToContextFn();
    }

    public String getTransformXSLTFn() {
        return processContext.getTransformXSLTFn();
    }

    void addLibraryImport(Library library) {
        processContext.addLibraryImport(library);
    }

    public BallerinaModel.TypeDesc getFileWriteConfigType() {
        return processContext.getFileWriteConfigType();
    }

    public String getFileWriteFunction() {
        return processContext.getFileWriteFunction();
    }

    BallerinaModel.TypeDesc getLogInputType() {
        return processContext.getLogInputType();
    }

    String getLogFunction() {
        return processContext.getLogFunction();
    }
}
