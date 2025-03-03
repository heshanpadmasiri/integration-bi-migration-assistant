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

import ballerina.BallerinaModel;
import tibco.TibcoModel;

public class TibcoToBallerinaModelConverter {

    public static class Context {

        private enum Library {
            HTTP("http");

            public final String value;

            Library(String value) {
                this.value = value;
            }
        }

        private final Map<String, Optional<BallerinaModel.ModuleTypeDef>> moduleTypeDefs = new HashMap<>();
        private final Set<BallerinaModel.Import> imports = new HashSet<>();
        private final List<BallerinaModel.Listener> listeners = new ArrayList<>();
        private final Map<String, BallerinaModel.ModuleVar> constants = new HashMap<>();

        public boolean addModuleTypeDef(String name, BallerinaModel.ModuleTypeDef moduleTypeDef) {
            if (moduleTypeDef.typeDesc() instanceof BallerinaModel.TypeDesc.TypeReference(String name1) &&
                    name1.equals(name)) {
                return false;
            }
            moduleTypeDefs.put(name, Optional.of(moduleTypeDef));
            return true;
        }

        public BallerinaModel.TypeDesc getTypeByName(String name) {
            // TODO: how to handle names spaces
            name = sanities(XmlToTibcoModelConverter.getTagNameWithoutNameSpace(name));
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

        private static String sanities(String name) {
            return name.replaceAll("[^a-zA-Z0-9]", "_");
        }

        public String declareConstant(String name, String valueRepr, String type) {
            name = sanities(name);
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

        private void addLibraryImport(Library library) {
            imports.add(new BallerinaModel.Import("ballerina", library.value, Optional.empty()));
        }

        public BallerinaModel.TextDocument finish(BallerinaModel.TextDocument textDocument) {
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
            return new BallerinaModel.TextDocument(textDocument.documentName(), combinedImports,
                    textDocument.moduleTypeDefs(), combinedModuleVars, combinedListeners,
                    textDocument.services(), textDocument.functions(), textDocument.Comments());
        }

        public String getDefaultHttpListenerRef() {
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

    private TibcoToBallerinaModelConverter() {

    }

    static BallerinaModel.Module convertProcess(Context cx, TibcoModel.Process process) {
        List<BallerinaModel.ModuleTypeDef> moduleTypeDefs = new ArrayList<>();
        List<BallerinaModel.Service> services = new ArrayList<>();
        for (TibcoModel.Type type : process.types()) {
            switch (type) {
                case TibcoModel.Type.Schema schema -> moduleTypeDefs.addAll(convertSchema(cx, schema));
                case TibcoModel.Type.WSDLDefinition wsdlDefinition ->
                        services.addAll(convertWsdlDefinition(cx, wsdlDefinition));
            }
        }
        // FIXME: this is wrong(name is not a package name)
        String name = process.name();
        List<BallerinaModel.Import> imports = List.of();
        List<BallerinaModel.ModuleVar> moduleVars = List.of();
        List<BallerinaModel.Listener> listeners = List.of();
        List<BallerinaModel.Function> functions = List.of();
        List<String> comments = List.of();
        BallerinaModel.TextDocument textDocument = cx.finish(
                new BallerinaModel.TextDocument(name + ".bal", imports, moduleTypeDefs, moduleVars, listeners, services,
                        functions, comments));
        return new BallerinaModel.Module(name, List.of(textDocument));
    }

    private static Collection<BallerinaModel.ModuleTypeDef> convertSchema(Context cx, TibcoModel.Type.Schema schema) {
        // TODO: (may be) handle namespaces
        Stream<BallerinaModel.ModuleTypeDef> newTypeDefinitions =
                schema.types().stream().filter(type -> !type.name().equals("anydata"))
                        .map(type -> convertComplexType(cx, type));
        Stream<BallerinaModel.ModuleTypeDef> typeAliases =
                schema.elements().stream().map(element -> convertTypeAlias(cx, element))
                        .filter(Optional::isPresent).map(Optional::get);
        return Stream.concat(newTypeDefinitions, typeAliases).toList();
    }

    private static Optional<BallerinaModel.ModuleTypeDef> convertTypeAlias(Context cx,
                                                                           TibcoModel.Type.Schema.Element element) {
        // FIXME: handle namespaces
        String name = XmlToTibcoModelConverter.getTagNameWithoutNameSpace(element.name());
        BallerinaModel.TypeDesc ref = cx.getTypeByName(element.type().name());
        BallerinaModel.ModuleTypeDef defn = new BallerinaModel.ModuleTypeDef(name, ref);
        return cx.addModuleTypeDef(name, defn) ? Optional.of(defn) : Optional.empty();
    }

    static Collection<BallerinaModel.Service> convertWsdlDefinition(Context cx,
                                                                    TibcoModel.Type.WSDLDefinition wsdlDefinition) {
        // For each portType, from context get the listner based on basePath
        //   -- Create a endpoint with api path and operation
        //   -- Set return type (can we just do this ignoring the wsdl:message part)
        // TODO: how do we fill the function body
        //  --  May be we can call a function here and fill that after filling flows
        Map<String, String> messageTypes = getMessageTypeDefinitions(cx, wsdlDefinition);
        return wsdlDefinition.portTypes().stream().map(each -> convertPortType(cx, messageTypes, each)).toList();
    }

    private static Map<String, String> getMessageTypeDefinitions(Context cx,
                                                                 TibcoModel.Type.WSDLDefinition wsdlDefinition) {
        Map<String, String> result = new HashMap<>();
        for (TibcoModel.Type.WSDLDefinition.Message message : wsdlDefinition.messages()) {
            String referredTypeName = getMessageTypeName(cx, message);
            result.put(message.name(), referredTypeName);
        }
        return result;
    }

    private static String getMessageTypeName(Context cx, TibcoModel.Type.WSDLDefinition.Message message) {
        TibcoModel.Type.WSDLDefinition.Message.Part part;
        if (message.parts().size() == 1) {
            part = message.parts().getFirst();
        } else {
            part = message.parts().stream().filter(each -> each.name().equals("item")).findFirst().get();
        }
        switch (part) {
            case TibcoModel.Type.WSDLDefinition.Message.Part.InlineError inlineError -> {
                String constantName = inlineError.name();
                return cx.declareConstant(constantName, inlineError.value(), inlineError.type());
            }
            case TibcoModel.Type.WSDLDefinition.Message.Part.Reference ref -> {
                return ref.element().value();
            }
        }
    }

    private static BallerinaModel.Service convertPortType(Context cx,
                                                          Map<String, String> messageTypes,
                                                          TibcoModel.Type.WSDLDefinition.PortType portType) {
        String basePath = portType.basePath();
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }
        String apiPath = portType.apiPath();
        List<BallerinaModel.Resource> resources =
                List.of(convertOperation(cx, apiPath, messageTypes, portType.operation()));
        List<String> listenerRefs = List.of(cx.getDefaultHttpListenerRef());
        return new BallerinaModel.Service(basePath, listenerRefs, resources, List.of(), List.of(), List.of());
    }

    private static BallerinaModel.Resource convertOperation(Context cx, String apiPath,
                                                            Map<String, String> messageTypes,
                                                            TibcoModel.Type.WSDLDefinition.PortType.Operation operation) {
        String resourceMethodName = operation.name();
        String path = apiPath.startsWith("/") ? apiPath.substring(1) : apiPath;
        BallerinaModel.TypeDesc inputType = cx.getTypeByName(messageTypes.get(operation.input().message().value()));
        List<BallerinaModel.Parameter> parameters =
                List.of(new BallerinaModel.Parameter(inputType.toString(), "input"));
        List<BallerinaModel.TypeDesc> returnTypeMembers =
                Stream.concat(
                                Stream.of(operation.output().message()),
                                operation.faults().stream().map(
                                        TibcoModel.Type.WSDLDefinition.PortType.Operation.Fault::message))
                        .map(message -> cx.getTypeByName(messageTypes.get(message.value()))).toList();
        BallerinaModel.TypeDesc returnType = returnTypeMembers.size() == 1
                ? returnTypeMembers.getFirst()
                : new BallerinaModel.TypeDesc.UnionTypeDesc(returnTypeMembers);
        List<BallerinaModel.Statement> body = List.of();
        return new BallerinaModel.Resource(resourceMethodName, path, parameters, Optional.of(returnType.toString()),
                body);
    }

    static BallerinaModel.ModuleTypeDef convertComplexType(Context cx, TibcoModel.Type.Schema.ComplexType complexType) {
        BallerinaModel.TypeDesc typeDesc = switch (complexType.body()) {
            case TibcoModel.Type.Schema.ComplexType.Choice choice -> convertTypeChoice(cx, choice);
            case TibcoModel.Type.Schema.ComplexType.SequenceBody sequenceBody -> convertSequenceBody(cx, sequenceBody);
            case TibcoModel.Type.Schema.ComplexType.ComplexContent complexContent ->
                    convertTypeInclusion(cx, complexContent);
        };
        String name = complexType.name();
        BallerinaModel.ModuleTypeDef typeDef = new BallerinaModel.ModuleTypeDef(name, typeDesc);
        cx.addModuleTypeDef(name, typeDef);
        return typeDef;
    }

    private static BallerinaModel.TypeDesc.RecordTypeDesc convertTypeInclusion(Context cx,
                                                                               TibcoModel.Type.Schema.ComplexType.ComplexContent complexContent) {
        List<BallerinaModel.TypeDesc> inclusions = List.of(cx.getTypeByName(complexContent.extension().base().name()));
        RecordBody body = getRecordBody(cx, complexContent.extension().elements());
        return new BallerinaModel.TypeDesc.RecordTypeDesc(inclusions, body.fields(), body.rest());
    }

    private static BallerinaModel.TypeDesc.RecordTypeDesc convertSequenceBody(Context cx,
                                                                              TibcoModel.Type.Schema.ComplexType.SequenceBody sequenceBody) {
        Collection<TibcoModel.Type.Schema.ComplexType.SequenceBody.Member> members = sequenceBody.elements();
        RecordBody body = getRecordBody(cx, members);
        return new BallerinaModel.TypeDesc.RecordTypeDesc(List.of(), body.fields(), body.rest());
    }

    private static RecordBody getRecordBody(Context cx,
                                            Collection<? extends TibcoModel.Type.Schema.ComplexType.SequenceBody.Member> members) {
        List<BallerinaModel.TypeDesc.RecordTypeDesc.RecordField> fields = new ArrayList<>();
        Optional<BallerinaModel.TypeDesc> rest = Optional.empty();
        for (TibcoModel.Type.Schema.ComplexType.SequenceBody.Member member : members) {
            switch (member) {
                case TibcoModel.Type.Schema.ComplexType.SequenceBody.Member.Element element -> {
                    BallerinaModel.TypeDesc typeDesc = cx.getTypeByName(element.type().name());
                    fields.add(new BallerinaModel.TypeDesc.RecordTypeDesc.RecordField(element.name(), typeDesc));
                }
                case TibcoModel.Type.Schema.ComplexType.SequenceBody.Member.Rest ignored -> {
                    // FIXME: handle this properly
                    rest = Optional.of(PredefinedTypes.ANYDATA);
                }
            }
        }
        return new RecordBody(fields, rest);
    }

    private record RecordBody(List<BallerinaModel.TypeDesc.RecordTypeDesc.RecordField> fields,
                              Optional<BallerinaModel.TypeDesc> rest) {

    }

    static BallerinaModel.TypeDesc.UnionTypeDesc convertTypeChoice(Context cx,
                                                                   TibcoModel.Type.Schema.ComplexType.Choice choice) {
        List<? extends BallerinaModel.TypeDesc> types = choice.elements().stream().map(element -> {
            BallerinaModel.TypeDesc typeDesc = cx.getTypeByName(element.ref().name());
            assert element.maxOccurs() == 1;
            if (element.minOccurs() == 0) {
                return BallerinaModel.TypeDesc.UnionTypeDesc.of(typeDesc, BallerinaModel.TypeDesc.BuiltinType.NIL);
            } else {
                return typeDesc;
            }
        }).flatMap(type -> {
            if (type instanceof BallerinaModel.TypeDesc.UnionTypeDesc(
                    Collection<? extends BallerinaModel.TypeDesc> members
            )) {
                return members.stream();
            } else {
                return Stream.of(type);
            }
        }).distinct().toList();
        return new BallerinaModel.TypeDesc.UnionTypeDesc(types);
    }

    static class PredefinedTypes {

        private static final BallerinaModel.TypeDesc.BuiltinType ANYDATA = BallerinaModel.TypeDesc.BuiltinType.ANYDATA;
    }
}
