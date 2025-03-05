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

class ProcessContext {

    private final Map<String, Optional<BallerinaModel.ModuleTypeDef>> moduleTypeDefs = new HashMap<>();
    private final Set<BallerinaModel.Import> imports = new HashSet<>();
    private final List<BallerinaModel.Listener> listeners = new ArrayList<>();
    private final Map<String, BallerinaModel.ModuleVar> constants = new HashMap<>();
    private final Map<String, TibcoToBallerinaModelConverter.PortHandler> portHandlers = new HashMap<>();
    private final List<BallerinaModel.Function> utilityFunctions = new ArrayList<>();
    private int annonFunctionCounter = 0;
    private String toXMLFunction = null;

    String getAnnonFunctionName() {
        return "annonFunction" + annonFunctionCounter++;
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

    String addPortHandler(String portName, String basePath, String apiPath,
                          BallerinaModel.TypeDesc inputType,
                          BallerinaModel.TypeDesc returnType) {
        String name = ConversionUtils.sanitizes(basePath + apiPath) + "Handler";
        this.portHandlers.put(portName,
                new TibcoToBallerinaModelConverter.PortHandler(name, inputType, returnType));
        return name;
    }

    ActivityContext registerWithPortHandler(String portname, String listener) {
        TibcoToBallerinaModelConverter.PortHandler portHandler = portHandlers.get(portname);
        if (portHandler == null) {
            throw new IllegalArgumentException("Port handler not found: " + portname);
        }
        portHandler.registerListener(listener);
        BallerinaModel.Parameter input = new BallerinaModel.Parameter(portHandler.inputType().toString(), "input");
        return new ActivityContext(this, listener, List.of(input), portHandler.returnType().toString());
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

    BallerinaModel.TextDocument finish(BallerinaModel.TextDocument textDocument) {
        for (Map.Entry<String, Optional<BallerinaModel.ModuleTypeDef>> entry : moduleTypeDefs.entrySet()) {
            if (entry.getValue().isEmpty()) {
                throw new IllegalStateException("Type not found: " + entry.getKey());
            }
        }
        List<BallerinaModel.Import> combinedImports =
                Stream.concat(imports.stream(), textDocument.imports().stream()).toList();
        List<BallerinaModel.Listener> combinedListeners =
                Stream.concat(listeners.stream(), textDocument.listeners().stream()).toList();
        List<BallerinaModel.ModuleVar> combinedModuleVars =
                Stream.concat(constants.values().stream(), textDocument.moduleVars().stream()).toList();
        List<BallerinaModel.Function> combinedFunctions =
                Stream.concat(utilityFunctions.stream(),
                                Stream.concat(
                                        portHandlers.values().stream()
                                                .map(TibcoToBallerinaModelConverter.PortHandler::toFunction),
                                        textDocument.functions().stream()))
                        .toList();
        return new BallerinaModel.TextDocument(textDocument.documentName(), combinedImports,
                textDocument.moduleTypeDefs(), combinedModuleVars, combinedListeners,
                textDocument.services(), combinedFunctions, textDocument.Comments());
    }

    String getDefaultHttpListenerRef() {
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
}
