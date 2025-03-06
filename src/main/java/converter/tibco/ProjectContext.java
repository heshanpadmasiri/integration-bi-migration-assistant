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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static ballerina.BallerinaModel.TypeDesc.BuiltinType.ANYDATA;
import static ballerina.BallerinaModel.TypeDesc.BuiltinType.XML;

import ballerina.BallerinaModel;
import converter.tibco.analyzer.AnalysisResult;
import converter.tibco.analyzer.ModelAnalyser;
import tibco.TibcoModel;

public class ProjectContext {

    private final Map<TibcoModel.Process, ProcessContext> processContextMap = new HashMap<>();
    private final Map<String, Optional<BallerinaModel.ModuleTypeDef>> moduleTypeDefs = new HashMap<>();
    private int nextPort = 8080;

    ProcessContext getProcessContext(TibcoModel.Process process) {
        return processContextMap.computeIfAbsent(process, p -> new ProcessContext(this, p));
    }

    private int allocatePort() {
        return nextPort++;
    }

    public BallerinaModel.Module serialize(Collection<BallerinaModel.TextDocument> textDocuments) {
        List<BallerinaModel.TextDocument> combinedTextDocuments = Stream.concat(textDocuments.stream(),
                Stream.of(typesFile())).toList();
        return new BallerinaModel.Module("tibco", combinedTextDocuments);
    }

    private BallerinaModel.TextDocument typesFile() {
        List<BallerinaModel.ModuleTypeDef> typeDefs = new ArrayList<>();
        for (var entry : moduleTypeDefs.entrySet()) {
            if (entry.getValue().isPresent()) {
                typeDefs.add(entry.getValue().get());
            } else {
                throw new IllegalStateException("Type definition not found for " + entry.getKey());
            }
        }
        // FIXME: handle imports
        List<BallerinaModel.Import> imports = List.of();
        return new BallerinaModel.TextDocument("types.bal", imports, typeDefs, List.of(), List.of(), List.of(),
                List.of(), List.of());
    }

    private String getProcessStartFunction(String processName) {
        TibcoModel.Process
                process = processContextMap.keySet().stream().filter(proc -> proc.name().equals(processName)).findAny()
                .orElseThrow(() -> new IndexOutOfBoundsException("failed to find process" + processName));
        return getProcessContext(process).getProcessStartFunctionName();
    }

    BallerinaModel.TypeDesc getTypeByName(String name, ProcessContext processContext) {
        // TODO: how to handle names spaces
        name = ConversionUtils.sanitizes(XmlToTibcoModelConverter.getTagNameWithoutNameSpace(name));
        if (moduleTypeDefs.containsKey(name)) {
            return new BallerinaModel.TypeDesc.TypeReference(name);
        }
        if (processContext.constants.containsKey(name)) {
            return new BallerinaModel.TypeDesc.TypeReference(name);
        }

        Optional<BallerinaModel.TypeDesc.BuiltinType> builtinType = mapToBuiltinType(name);
        if (builtinType.isPresent()) {
            return builtinType.get();
        }

        Optional<BallerinaModel.TypeDesc.TypeReference> libraryType = mapToLibraryType(name, processContext);
        if (libraryType.isPresent()) {
            return libraryType.get();
        }

        if (!moduleTypeDefs.containsKey(name)) {
            moduleTypeDefs.put(name, Optional.empty());
        }
        return new BallerinaModel.TypeDesc.TypeReference(name);
    }

    private Optional<BallerinaModel.TypeDesc.TypeReference> mapToLibraryType(String name,
                                                                             ProcessContext processContext) {
        return switch (name) {
            case "client4XXError" -> Optional.of(getLibraryType(Library.HTTP, "NotFound", processContext));
            case "server5XXError" -> Optional.of(getLibraryType(Library.HTTP, "InternalServerError", processContext));
            default -> Optional.empty();
        };
    }

    private BallerinaModel.TypeDesc.TypeReference getLibraryType(Library library, String typeName,
                                                                 ProcessContext processContext) {
        processContext.addLibraryImport(library);
        return new BallerinaModel.TypeDesc.TypeReference(library.value + ":" + typeName);
    }

    private Optional<BallerinaModel.TypeDesc.BuiltinType> mapToBuiltinType(String name) {
        return switch (name) {
            case "string" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.STRING);
            case "integer", "int" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.INT);
            case "anydata" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.ANYDATA);
            case "xml" -> Optional.of(XML);
            // FIXME:
            case "base64Binary" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.INT);
            default -> Optional.empty();
        };
    }

    static class ProcessContext {

        private final Set<BallerinaModel.Import> imports = new HashSet<>();
        private BallerinaModel.Listener defaultListner = null;
        private final Map<String, BallerinaModel.ModuleVar> constants = new HashMap<>();
        private final List<BallerinaModel.Function> utilityFunctions = new ArrayList<>();
        private final Map<BallerinaModel.TypeDesc, String> typeConversionFunction = new HashMap<>();
        public int activityCounter = 0;
        public String startWorkerName;
        public final List<String> terminateWorkers = new ArrayList<>();
        private String toXMLFunction = null;
        public final TibcoModel.Process process;
        public BallerinaModel.TypeDesc processInputType;
        public BallerinaModel.TypeDesc processReturnType;

        public final ProjectContext projectContext;
        public final AnalysisResult analysisResult;

        private ProcessContext(ProjectContext projectContext, TibcoModel.Process process) {
            this.projectContext = projectContext;
            this.process = process;
            this.analysisResult = ModelAnalyser.analyseProcess(process);
        }

        String getToXmlFunction() {
            if (toXMLFunction == null) {
                addLibraryImport(Library.XML_DATA);
                String functionName = "toXML";
                utilityFunctions.add(new BallerinaModel.Function(Optional.empty(), functionName,
                        List.of(new BallerinaModel.Parameter(new BallerinaModel.TypeDesc.MapTypeDesc(ANYDATA), "data")),
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
            this.projectContext.moduleTypeDefs.put(name, Optional.of(moduleTypeDef));
            return true;
        }

        BallerinaModel.TypeDesc getTypeByName(String name) {
            return projectContext.getTypeByName(name, this);
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

        void addLibraryImport(Library library) {
            imports.add(new BallerinaModel.Import("ballerina", library.value, Optional.empty()));
        }

        String getDefaultHttpListenerRef() {
            if (defaultListner == null) {
                addLibraryImport(Library.HTTP);
                String listenerRef = "LISTENER";
                defaultListner = new BallerinaModel.Listener(BallerinaModel.ListenerType.HTTP, listenerRef,
                        Integer.toString(projectContext.allocatePort()),
                        Map.of("host", "localhost"));
            }
            return defaultListner.name();

        }

        // FIXME: don't get the typeDefs
        BallerinaModel.TextDocument serialize(List<BallerinaModel.ModuleTypeDef> typeDefs,
                                              BallerinaModel.Service processService,
                                              List<BallerinaModel.Function> functions) {
            String name = ConversionUtils.sanitizes(process.name()) + ".bal";
            List<BallerinaModel.Function> combinedFunctions =
                    Stream.concat(functions.stream(), utilityFunctions.stream()).toList();
            List<BallerinaModel.Listener> listeners = defaultListner != null ? List.of(defaultListner) : List.of();
            return new BallerinaModel.TextDocument(name, imports.stream().toList(), List.of(),
                    constants.values().stream().toList(), listeners, List.of(processService),
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
            return typeConversionFunction.computeIfAbsent(targetType, this::createConvertToTypeFunction);
        }

        private String createConvertToTypeFunction(BallerinaModel.TypeDesc targetType) {
            String functionName = "convertTo" + ConversionUtils.sanitizes(targetType.toString());
            addLibraryImport(Library.XML_DATA);
            BallerinaModel.Expression.FunctionCall parseAsTypeCall =
                    new BallerinaModel.Expression.FunctionCall("xmldata:parseAsType", new String[]{"input"});
            BallerinaModel.Expression.CheckPanic checkPanic = new BallerinaModel.Expression.CheckPanic(parseAsTypeCall);
            BallerinaModel.Return<BallerinaModel.Expression.CheckPanic> returnStmt =
                    new BallerinaModel.Return<>(Optional.of(checkPanic));
            BallerinaModel.Function function = new BallerinaModel.Function(Optional.empty(), functionName,
                    List.of(new BallerinaModel.Parameter(XML, "input")), Optional.of(targetType.toString()),
                    List.of(returnStmt));
            utilityFunctions.add(function);
            return functionName;
        }

        public String getProcessStartFunction(String processName) {
            return projectContext.getProcessStartFunction(processName);
        }
    }

}
