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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static ballerina.BallerinaModel.TypeDesc.BuiltinType.ANYDATA;
import static ballerina.BallerinaModel.TypeDesc.BuiltinType.BOOLEAN;
import static ballerina.BallerinaModel.TypeDesc.BuiltinType.JSON;
import static ballerina.BallerinaModel.TypeDesc.BuiltinType.NIL;
import static ballerina.BallerinaModel.TypeDesc.BuiltinType.STRING;
import static ballerina.BallerinaModel.TypeDesc.BuiltinType.XML;

public class ProjectContext {

    private final Map<TibcoModel.Process, ProcessContext> processContextMap = new HashMap<>();
    private final Map<String, Optional<BallerinaModel.ModuleTypeDef>> moduleTypeDefs = new HashMap<>();

    private final List<BallerinaModel.Function> utilityFunctions = new ArrayList<>();
    private final Set<BallerinaModel.Import> utilityFunctionImports = new HashSet<>();
    private final List<BallerinaModel.ModuleVar> utilityConstants = new ArrayList<>();
    private final Set<Intrinsics> utilityIntrinsics = new HashSet<>();

    private final Map<String, BallerinaModel.Expression.VariableReference> dbClients = new HashMap<>();
    private final List<String> typeIntrinsics = new ArrayList<>();
    private String toXMLFunction = null;
    private String jsonToXMLFunction = null;
    private String toHttpConfigFunction = null;
    private int nextPort = 8080;
    private int typeCount = 0;
    private int typeAliasCount = 0;
    private int unhandledTypeCount = 0;

    private final ContextWrapperForTypeFile typeCx = new ContextWrapperForTypeFile(this);
    private static final Logger logger = Logger.getLogger(ProjectContext.class.getName());

    ProcessContext getProcessContext(TibcoModel.Process process) {
        return processContextMap.computeIfAbsent(process, p -> new ProcessContext(this, p));
    }

    int allocatePort() {
        return nextPort++;
    }

    public BallerinaModel.Module serialize(Collection<BallerinaModel.TextDocument> textDocuments) {
        List<BallerinaModel.TextDocument> combinedTextDocuments = Stream.concat(textDocuments.stream(),
                Stream.of(typesFile(), utilsFile())).toList();
        logger.info(String.format("Type Statistics - Total Types: %d, Type Aliases: %d, Unhandled Types: %d",
                typeCount, typeAliasCount, unhandledTypeCount));
        return new BallerinaModel.Module("tibco", combinedTextDocuments);
    }

    String getToXmlFunction() {
        if (toXMLFunction == null) {
            importLibraryIfNeededToUtility(Library.XML_DATA);
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

    String getJsonToXMLFunction() {
        if (jsonToXMLFunction == null) {
            importLibraryIfNeededToUtility(Library.XML_DATA);
            String functionName = "fromJson";
            utilityFunctions.add(new BallerinaModel.Function(Optional.empty(), functionName,
                    List.of(new BallerinaModel.Parameter(JSON, "data")),
                    Optional.of("xml"), List.of(new BallerinaModel.Return<>(
                    Optional.of(new BallerinaModel.Expression.CheckPanic(
                            new BallerinaModel.Expression.FunctionCall("xmldata:fromJson", new String[]{"data"})))))));
            jsonToXMLFunction = functionName;
        }
        return jsonToXMLFunction;
    }

    String getParseHttpConfigFunction() {
        if (toHttpConfigFunction == null) {
            BallerinaModel.TypeDesc targetType = getHttpConfigType();
            toHttpConfigFunction = createConvertToTypeFunction(targetType);
        }
        return toHttpConfigFunction;
    }

    BallerinaModel.TypeDesc.TypeReference getHttpConfigType() {
        // type HTTPRequestConfig record {
        //     string Method;
        //     string RequestURI;
        //     json PostData = "";
        //     map<string> Headers = {};
        //     map<string> parameters = {};
        // };
        String httpConfigTy = "HTTPRequestConfig";
        if (moduleTypeDefs.containsKey(httpConfigTy)) {
            return new BallerinaModel.TypeDesc.TypeReference(httpConfigTy);
        }
        BallerinaModel.ModuleTypeDef httpConfigType = new BallerinaModel.ModuleTypeDef(httpConfigTy,
                new BallerinaModel.TypeDesc.RecordTypeDesc(
                        List.of(),
                        List.of(
                                new BallerinaModel.TypeDesc.RecordTypeDesc.RecordField("Method", STRING),
                                new BallerinaModel.TypeDesc.RecordTypeDesc.RecordField("RequestURI", STRING),
                                // TODO: handle put
                                new BallerinaModel.TypeDesc.RecordTypeDesc.RecordField("PostData", JSON, Optional.of(
                                        new BallerinaModel.Expression.StringConstant(""))),
                                new BallerinaModel.TypeDesc.RecordTypeDesc.RecordField("Headers",
                                        new BallerinaModel.TypeDesc.MapTypeDesc(STRING), Optional.of(
                                        new BallerinaModel.Expression.MappingConstructor(List.of()))),
                                new BallerinaModel.TypeDesc.RecordTypeDesc.RecordField("parameters",
                                        new BallerinaModel.TypeDesc.MapTypeDesc(STRING), Optional.of(
                                        new BallerinaModel.Expression.MappingConstructor(List.of())))
                        ),
                        Optional.empty()
                ));
        moduleTypeDefs.put(httpConfigTy, Optional.of(httpConfigType));
        typeIntrinsics.add(Intrinsics.CREATE_HTTP_REQUEST_PATH_FROM_CONFIG.body);

        return new BallerinaModel.TypeDesc.TypeReference(httpConfigTy);
    }

    private void importLibraryIfNeededToUtility(Library library) {
        utilityFunctionImports.add(new BallerinaModel.Import(library.orgName, library.moduleName, Optional.empty()));
    }

    String createConvertToTypeFunction(BallerinaModel.TypeDesc targetType) {
        String functionName = "convertTo" + ConversionUtils.sanitizes(targetType.toString());
        importLibraryIfNeededToUtility(Library.XML_DATA);
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

    private BallerinaModel.TextDocument utilsFile() {
        List<BallerinaModel.Import> imports = utilityFunctionImports.stream().toList();
        List<BallerinaModel.ModuleVar> sortedConstants = utilityConstants.stream()
                .sorted(Comparator.comparing(BallerinaModel.ModuleVar::name))
                .toList();
        List<BallerinaModel.Function> sortedFunctions = utilityFunctions.stream()
                .sorted(Comparator.comparing(BallerinaModel.Function::methodName))
                .toList();
        List<String> sortedIntrinsics = utilityIntrinsics.stream()
                .sorted(Comparator.comparing(Intrinsics::name))
                .map(each -> each.body)
                .toList();
        return new BallerinaModel.TextDocument("utils.bal", imports, List.of(), sortedConstants,
                List.of(), List.of(), sortedFunctions, List.of(), sortedIntrinsics);
    }

    private BallerinaModel.TextDocument typesFile() {
        List<BallerinaModel.ModuleTypeDef> typeDefs = new ArrayList<>();
        for (Map.Entry<String, Optional<BallerinaModel.ModuleTypeDef>> entry : moduleTypeDefs.entrySet()) {
            if (entry.getValue().isPresent()) {
                typeDefs.add(entry.getValue().get());
            } else {
                logger.warning(
                        String.format("Type definition not found for %s using `anydata` as fallback", entry.getKey()));
                typeDefs.add(new BallerinaModel.ModuleTypeDef(entry.getKey(), ANYDATA));
            }
        }
        List<BallerinaModel.Import> imports = typeCx.imports.stream().toList();
        return new BallerinaModel.TextDocument("types.bal", imports, typeDefs, List.of(), List.of(), List.of(),
                List.of(), List.of(), typeIntrinsics);
    }

    FunctionData getProcessStartFunction(String processName) {
        TibcoModel.Process
                process = processContextMap.keySet().stream().filter(proc -> proc.name().equals(processName)).findAny()
                .orElseThrow(() -> new IndexOutOfBoundsException("failed to find process" + processName));
        return getProcessContext(process).getProcessStartFunction();
    }

    BallerinaModel.TypeDesc getTypeByName(String name, ContextWithFile cx) {
        // TODO: how to handle names spaces
        name = ConversionUtils.sanitizes(XmlToTibcoModelConverter.getTagNameWithoutNameSpace(name));
        if (moduleTypeDefs.containsKey(name)) {
            return new BallerinaModel.TypeDesc.TypeReference(name);
        }
        if (cx.hasConstantWithName(name)) {
            return new BallerinaModel.TypeDesc.TypeReference(name);
        }

        Optional<BallerinaModel.TypeDesc.BuiltinType> builtinType = mapToBuiltinType(name);
        if (builtinType.isPresent()) {
            return builtinType.get();
        }

        Optional<BallerinaModel.TypeDesc.TypeReference> libraryType = mapToLibraryType(cx, name);
        if (libraryType.isPresent()) {
            return libraryType.get();
        }

        if (!moduleTypeDefs.containsKey(name)) {
            moduleTypeDefs.put(name, Optional.empty());
        }
        return new BallerinaModel.TypeDesc.TypeReference(name);
    }

    private Optional<BallerinaModel.TypeDesc.TypeReference> mapToLibraryType(ContextWithFile cx, String name) {
        return switch (name) {
            case "client4XXError" -> Optional.of(getLibraryType(cx, Library.HTTP, "NotFound"));
            case "server5XXError" -> Optional.of(getLibraryType(cx, Library.HTTP, "InternalServerError"));
            case "Client" -> Optional.of(getLibraryType(cx, Library.HTTP, "Client"));
            case "ParameterizedQuery" -> Optional.of(getLibraryType(cx, Library.SQL, "ParameterizedQuery"));
            case "ExecutionResult" -> Optional.of(getLibraryType(cx, Library.SQL, "ExecutionResult"));
            default -> Optional.empty();
        };
    }

    private BallerinaModel.TypeDesc.TypeReference getLibraryType(ContextWithFile cx, Library library, String typeName) {
        cx.addLibraryImport(library);
        return new BallerinaModel.TypeDesc.TypeReference(library.moduleName + ":" + typeName);
    }

    private Optional<BallerinaModel.TypeDesc.BuiltinType> mapToBuiltinType(String name) {
        return switch (name) {
            case "string" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.STRING);
            case "integer", "int", "long" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.INT);
            case "anydata" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.ANYDATA);
            case "xml" -> Optional.of(XML);
            case "null" -> Optional.of(NIL);
            case "boolean" -> Optional.of(BOOLEAN);
            // TODO: handle base64Binary
            case "base64Binary" -> Optional.of(BallerinaModel.TypeDesc.BuiltinType.INT);
            default -> Optional.empty();
        };
    }

    ContextWithFile getTypeContext() {
        return typeCx;
    }

    boolean addModuleTypeDef(String name, BallerinaModel.ModuleTypeDef moduleTypeDef) {
        if (moduleTypeDef.typeDesc() instanceof BallerinaModel.TypeDesc.TypeReference(String name1) &&
                name1.equals(name)) {
            return false;
        }
        this.moduleTypeDefs.put(name, Optional.of(moduleTypeDef));
        return true;
    }

    public void incrementTypeCount() {
        typeCount++;
    }

    public void incrementUnhandledTypeCount() {
        incrementTypeCount();
        unhandledTypeCount++;
    }

    public void incrementTypeAliasCount() {
        incrementTypeCount();
        typeAliasCount++;
    }

    record FunctionData(String name, BallerinaModel.TypeDesc inputType, BallerinaModel.TypeDesc returnType) {

        FunctionData {
            assert name != null && !name.isEmpty();
            assert inputType != null;
            assert returnType != null;
        }
    }

    public BallerinaModel.Expression.VariableReference dbClient(String sharedResourcePropertyName) {
        return dbClients.computeIfAbsent(sharedResourcePropertyName, this::createDbClient);
    }

    private BallerinaModel.Expression.VariableReference createDbClient(String name) {
        // TODO: handle configurations
        importLibraryIfNeededToUtility(Library.JDBC);
        BallerinaModel.ModuleVar moduleVar = BallerinaModel.ModuleVar.constant(ConversionUtils.sanitizes(name),
                new BallerinaModel.TypeDesc.TypeReference("jdbc:Client"),
                new BallerinaModel.Expression.CheckPanic(
                        new BallerinaModel.Expression.NewExpression(
                                List.of(new BallerinaModel.Expression.StringConstant(name)))));
        utilityConstants.add(moduleVar);
        return new BallerinaModel.Expression.VariableReference(moduleVar.name());
    }

    public String getAddToContextFn() {
        utilityIntrinsics.add(Intrinsics.ADD_TO_CONTEXT);
        return Intrinsics.ADD_TO_CONTEXT.name;
    }

    public String getTransformXSLTFn() {
        addTransformXSLTFnIfNeeded();
        return Intrinsics.TRANSFORM_XSLT.name;
    }

    public void addTransformXSLTFnIfNeeded() {
        if (utilityIntrinsics.contains(Intrinsics.TRANSFORM_XSLT)) {
            return;
        }
        utilityIntrinsics.add(Intrinsics.TRANSFORM_XSLT);
    }

    private static class ContextWrapperForTypeFile implements ContextWithFile {

        final Set<BallerinaModel.Import> imports = new HashSet<>();
        final ProjectContext cx;

        private ContextWrapperForTypeFile(ProjectContext cx) {
            this.cx = cx;
        }

        @Override
        public boolean hasConstantWithName(String name) {
            return false;
        }

        @Override
        public void addLibraryImport(Library library) {
            imports.add(new BallerinaModel.Import("ballerina", library.moduleName, Optional.empty()));
        }

        @Override
        public BallerinaModel.TypeDesc getTypeByName(String name) {
            return cx.getTypeByName(name, this);
        }

        @Override
        public boolean addModuleTypeDef(String name, BallerinaModel.ModuleTypeDef defn) {
            return cx.addModuleTypeDef(name, defn);
        }

        @Override
        public ProjectContext getProjectContext() {
            return cx;
        }
    }
}
