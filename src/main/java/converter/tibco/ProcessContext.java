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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import ballerina.BallerinaModel;
import converter.tibco.analyzer.AnalysisResult;
import converter.tibco.analyzer.ModelAnalyser;
import tibco.TibcoModel;

class ProcessContext {

    private final Map<String, Optional<BallerinaModel.ModuleTypeDef>> moduleTypeDefs = new HashMap<>();
    private final Set<BallerinaModel.Import> imports = new HashSet<>();
    private final List<BallerinaModel.Listener> listeners = new ArrayList<>();
    private final Map<String, BallerinaModel.ModuleVar> constants = new HashMap<>();
    private final List<BallerinaModel.Function> utilityFunctions = new ArrayList<>();
    private final Map<BallerinaModel.TypeDesc, String> typeConversionFunction = new HashMap<>();
    public int acitivityCounter = 0;
    public String startWorkerName;
    private String toXMLFunction = null;
    public final TibcoModel.Process process;
    public BallerinaModel.TypeDesc processInputType;
    public BallerinaModel.TypeDesc processReturnType;

    public final AnalysisResult analysisResult;

    ProcessContext(TibcoModel.Process process) {
        this.process = process;
        this.analysisResult = ModelAnalyser.analyseProcess(process);
    }

    String getToXmlFunction() {
        if (toXMLFunction == null) {
            addLibraryImport(Library.XML_DATA);
            String functionName = "toXML";
            utilityFunctions.add(new BallerinaModel.Function(Optional.empty(), functionName,
                    List.of(new BallerinaModel.Parameter("map<anydata>", "data")),
                    Optional.of("xml"), List.of(new BallerinaModel.Return<>(
                    Optional.of(new BallerinaModel.Expression.CheckPanic(
                            new BallerinaModel.Expression.FunctionCall("xmldata:toXml", new String[]{"data"})))))));
            toXMLFunction = functionName;
        }
        return toXMLFunction;
    }

    boolean addModuleTypeDef(String name, BallerinaModel.ModuleTypeDef moduleTypeDef) {
        if (moduleTypeDef.typeDesc() instanceof BallerinaModel.TypeDesc.TypeReference(String name1) &&
                name1.equals(name)) {
            return false;
        }
        moduleTypeDefs.put(name, Optional.of(moduleTypeDef));
        return true;
    }

    BallerinaModel.TypeDesc getTypeByName(String name) {
        // TODO: how to handle names spaces
        name = ConversionUtils.sanitizes(XmlToTibcoModelConverter.getTagNameWithoutNameSpace(name));
        if (moduleTypeDefs.containsKey(name)) {
            return new BallerinaModel.TypeDesc.TypeReference(name);
        }
        if (constants.containsKey(name)) {
            return new BallerinaModel.TypeDesc.TypeReference(name);
        }

        Optional<BallerinaModel.TypeDesc.BuiltinType> builtinType = mapToBuiltinType(name);
        if (builtinType.isPresent()) {
            return builtinType.get();
        }

        Optional<BallerinaModel.TypeDesc.TypeReference> libraryType = mapToLibraryType(name);
        if (libraryType.isPresent()) {
            return libraryType.get();
        }

        if (!moduleTypeDefs.containsKey(name)) {
            moduleTypeDefs.put(name, Optional.empty());
        }
        return new BallerinaModel.TypeDesc.TypeReference(name);
    }

    String declareConstant(String name, String valueRepr, String type) {
        name = ConversionUtils.sanitizes(name);
        BallerinaModel.TypeDesc td = getTypeByName(type);
        assert td == BallerinaModel.TypeDesc.BuiltinType.STRING;
        String expr = "\"" + valueRepr + "\"";
        var prev = constants.put(name, new BallerinaModel.ModuleVar(name, td.toString(),
                new BallerinaModel.BallerinaExpression(expr), true));
        assert prev == null || prev.expr().expr().equals(expr);
        return name;
    }

    private Optional<BallerinaModel.TypeDesc.BuiltinType> mapToBuiltinType(String name) {
        return switch (name) {
            case "string" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.STRING);
            case "integer" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.INT);
            case "anydata" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.ANYDATA);
            case "xml" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.XML);
            default -> Optional.empty();
        };
    }

    private Optional<BallerinaModel.TypeDesc.TypeReference> mapToLibraryType(String name) {
        return switch (name) {
            case "client4XXError" -> Optional.of(getLibraryType(Library.HTTP, "NotFound"));
            case "server5XXError" -> Optional.of(getLibraryType(Library.HTTP, "InternalServerError"));
            default -> Optional.empty();
        };
    }

    private BallerinaModel.TypeDesc.TypeReference getLibraryType(Library library, String typeName) {
        addLibraryImport(library);
        return new BallerinaModel.TypeDesc.TypeReference(library.value + ":" + typeName);
    }

    void addLibraryImport(Library library) {
        imports.add(new BallerinaModel.Import("ballerina", library.value, Optional.empty()));
    }

    String getDefaultHttpListenerRef() {
        // FIXME: this needs to increment ports so push the port to conversion context
        if (listeners.isEmpty()) {
            addLibraryImport(Library.HTTP);
            String listenerRef = "LISTENER";
            listeners.add(
                    new BallerinaModel.Listener(BallerinaModel.ListenerType.HTTP, listenerRef, "8080",
                            Map.of("host", "localhost")));
            return listenerRef;
        } else {
            return listeners.getFirst().name();
        }

    }

    BallerinaModel.TextDocument serialize(List<BallerinaModel.ModuleTypeDef> typeDefs,
                                          BallerinaModel.Service processService,
                                          List<BallerinaModel.Function> functions) {
        String name = ConversionUtils.sanitizes(process.name()) + ".bal";
        List<BallerinaModel.Function> combinedFunctions =
                Stream.concat(functions.stream(), utilityFunctions.stream()).toList();
        return new BallerinaModel.TextDocument(name, imports.stream().toList(), typeDefs,
                constants.values().stream().toList(), listeners.stream().toList(), List.of(processService),
                combinedFunctions, List.of());
    }

    public String getProcessStartFunctionName() {
        return ConversionUtils.sanitizes(process.name()) + "_start";
    }

    public String getProcessFunction() {
        return "process_" + ConversionUtils.sanitizes(process.name());
    }

    public String getConvertToTypeFunction(BallerinaModel.TypeDesc targetType) {
        // FIXME: create a utility function
        return typeConversionFunction.computeIfAbsent(targetType,
                ignored -> "typeConversion_" + typeConversionFunction.size());
    }
}
