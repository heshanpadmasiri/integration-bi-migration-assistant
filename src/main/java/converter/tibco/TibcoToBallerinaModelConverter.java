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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import ballerina.BallerinaModel;
import tibco.TibcoModel;

public class TibcoToBallerinaModelConverter {

    private TibcoToBallerinaModelConverter() {

    }

    static BallerinaModel.Module convertProcess(ProcessContext cx, TibcoModel.Process process) {
        List<BallerinaModel.ModuleTypeDef> moduleTypeDefs = new ArrayList<>();
        List<BallerinaModel.Service> services = new ArrayList<>();
        for (TibcoModel.Type type : process.types()) {
            switch (type) {
                case TibcoModel.Type.Schema schema -> moduleTypeDefs.addAll(convertSchema(cx, schema));
                case TibcoModel.Type.WSDLDefinition wsdlDefinition ->
                        services.addAll(convertWsdlDefinition(cx, wsdlDefinition));
            }
        }
        // TODO: generate module variables
        List<BallerinaModel.ModuleVar> moduleVars = List.of();

        List<BallerinaModel.Function> functions = convertProcessScope(cx, process.scope());

        // FIXME: this is wrong(name is not a package name)
        String name = process.name();
        List<BallerinaModel.Import> imports = List.of();
        List<BallerinaModel.Listener> listeners = List.of();
        List<String> comments = List.of();
        BallerinaModel.TextDocument textDocument = cx.finish(
                new BallerinaModel.TextDocument(name + ".bal", imports, moduleTypeDefs, moduleVars, listeners, services,
                        functions, comments));
        return new BallerinaModel.Module(name, List.of(textDocument));
    }

    private static List<BallerinaModel.Function> convertProcessScope(ProcessContext cx, TibcoModel.Scope scope) {
        return scope.flows().stream().map(flow -> convertFlow(cx, flow)).flatMap(Collection::stream).toList();
    }

    private static List<BallerinaModel.Function> convertFlow(ProcessContext cx, TibcoModel.Scope.Flow flow) {
        ActivityContext fx = new ActivityContext(cx, flow.name(), List.of());
        return convertFlowInner(cx, fx, flow);
    }

    private static List<BallerinaModel.Function> convertFlowInner(ProcessContext cx, ActivityContext fx,
                                                                  TibcoModel.Scope.Flow flow) {
        flow.links().forEach(link -> fx.addLinkHandler(link.name()));
        List<BallerinaModel.Function> generatedFunctions = new ArrayList<>();
        for (TibcoModel.Scope.Flow.Activity activity : flow.activities()) {
            if (activity instanceof TibcoModel.Scope.Flow.Activity.Pick pick) {
                generatedFunctions.addAll(convertPickAction(cx, pick));
                continue;
            }
            switch (activity) {
                case TibcoModel.Scope.Flow.Activity.ActivityExtension activityExtension ->
                        fx.addComment("Activity extension" + Objects.toIdentityString(activityExtension));
                case TibcoModel.Scope.Flow.Activity.Empty empty -> fx.addComment(empty.name());
                case TibcoModel.Scope.Flow.Activity.ExtActivity extActivity ->
                        generatedFunctions.addAll(convertExtActivity(fx, extActivity));
                case TibcoModel.Scope.Flow.Activity.Invoke invoke ->
                        fx.addComment("invoke" + Objects.toIdentityString(invoke));
                case TibcoModel.Scope.Flow.Activity.Pick ignored ->
                        throw new IllegalStateException("Pick should have been handled");
                case TibcoModel.Scope.Flow.Activity.ReceiveEvent receiveEvent ->
                        fx.addComment("Receive event" + Objects.toIdentityString(receiveEvent));
                case TibcoModel.Scope.Flow.Activity.Reply reply -> convertReply(fx, reply);
            }
        }
        generatedFunctions.addAll(fx.finalizeFunction());
        return generatedFunctions;
    }

    private static Collection<BallerinaModel.Function> convertExtActivity(ActivityContext fx,
                                                                          TibcoModel.Scope.Flow.Activity.ExtActivity extActivity) {
        Collection<TibcoModel.Scope.Flow.Activity.Source> sources = extActivity.sources();
        assert sources.size() == 1 : "multiple sources not implemented";
        TibcoModel.Scope.Flow.Activity.Source source = sources.iterator().next();
        fx = fx.registerWithLinkHandler(source.linkName());
        BallerinaModel.Expression.VariableReference input = fx.getInputAsXml();
        BallerinaModel.Expression.VariableReference output =
                convertInputBindings(fx, fx.getInputAsXml(), extActivity.inputBindings());
        output = fx.xsltTransform(output, (TibcoModel.Scope.Flow.Activity.Expression.XSLT) extActivity.expression());
        fx.callProcess(extActivity.callProcess().subprocessName(), output);
        return fx.finalizeFunction();
    }

    private static void convertReply(ActivityContext fx, TibcoModel.Scope.Flow.Activity.Reply reply) {
        BallerinaModel.Expression.VariableReference output =
                convertInputBindings(fx, fx.getInputAsXml(), reply.inputBindings());
        for (TibcoModel.Scope.Flow.Activity.Target target : reply.targets()) {
            fx.sendToTarget(target, output);
        }
    }

    private static BallerinaModel.Expression.VariableReference convertInputBindings(ActivityContext fx,
                                                                                    BallerinaModel.Expression.VariableReference input,
                                                                                    Collection<TibcoModel.Scope.Flow.Activity.InputBinding> inputBindings) {
        for (TibcoModel.Scope.Flow.Activity.InputBinding transform : inputBindings) {
            TibcoModel.Scope.Flow.Activity.Expression.XSLT xslt = transform.xslt();
            input = fx.xsltTransform(input, xslt);
        }
        return input;
    }

    private static List<BallerinaModel.Function> convertPickAction(ProcessContext cx,
                                                                   TibcoModel.Scope.Flow.Activity.Pick pick) {
        TibcoModel.Scope.Flow.Activity.Pick.OnMessage message = pick.onMessage();
        String functionName = message.scope().name();
        // TODO: register with context for the partner link
        String partnerLink = message.partnerLink();
        ActivityContext fx = cx.registerWithPortHandler(partnerLink, functionName);
        Collection<TibcoModel.Scope.Flow> flows = message.scope().flows();
        assert flows.size() == 1;
        TibcoModel.Scope.Flow flow = flows.iterator().next();
        return convertFlowInner(cx, fx, flow);
    }

    private static Collection<BallerinaModel.ModuleTypeDef> convertSchema(ProcessContext cx,
                                                                          TibcoModel.Type.Schema schema) {
        // TODO: (may be) handle namespaces
        Stream<BallerinaModel.ModuleTypeDef> newTypeDefinitions =
                schema.types().stream().filter(type -> !type.name().equals("anydata"))
                        .map(type -> convertComplexType(cx, type));
        Stream<BallerinaModel.ModuleTypeDef> typeAliases =
                schema.elements().stream().map(element -> convertTypeAlias(cx, element))
                        .filter(Optional::isPresent).map(Optional::get);
        return Stream.concat(newTypeDefinitions, typeAliases).toList();
    }

    private static Optional<BallerinaModel.ModuleTypeDef> convertTypeAlias(ProcessContext cx,
                                                                           TibcoModel.Type.Schema.Element element) {
        // FIXME: handle namespaces
        String name = XmlToTibcoModelConverter.getTagNameWithoutNameSpace(element.name());
        BallerinaModel.TypeDesc ref = cx.getTypeByName(element.type().name());
        BallerinaModel.ModuleTypeDef defn = new BallerinaModel.ModuleTypeDef(name, ref);
        return cx.addModuleTypeDef(name, defn) ? Optional.of(defn) : Optional.empty();
    }

    static Collection<BallerinaModel.Service> convertWsdlDefinition(ProcessContext cx,
                                                                    TibcoModel.Type.WSDLDefinition wsdlDefinition) {
        // For each portType, from context get the listner based on basePath
        //   -- Create a endpoint with api path and operation
        //   -- Set return type (can we just do this ignoring the wsdl:message part)
        // TODO: how do we fill the function body
        //  --  May be we can call a function here and fill that after filling flows
        Map<String, String> messageTypes = getMessageTypeDefinitions(cx, wsdlDefinition);
        return wsdlDefinition.portTypes().stream().map(each -> convertPortType(cx, messageTypes, each)).toList();
    }

    private static Map<String, String> getMessageTypeDefinitions(ProcessContext cx,
                                                                 TibcoModel.Type.WSDLDefinition wsdlDefinition) {
        Map<String, String> result = new HashMap<>();
        for (TibcoModel.Type.WSDLDefinition.Message message : wsdlDefinition.messages()) {
            String referredTypeName = getMessageTypeName(cx, message);
            result.put(message.name(), referredTypeName);
        }
        return result;
    }

    private static String getMessageTypeName(ProcessContext cx, TibcoModel.Type.WSDLDefinition.Message message) {
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

    private static BallerinaModel.Service convertPortType(ProcessContext cx,
                                                          Map<String, String> messageTypes,
                                                          TibcoModel.Type.WSDLDefinition.PortType portType) {
        String basePath = portType.basePath();
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }
        String apiPath = portType.apiPath();
        List<BallerinaModel.Resource> resources =
                List.of(convertOperation(cx, portType.name(), basePath, apiPath, messageTypes, portType.operation()));
        List<String> listenerRefs = List.of(cx.getDefaultHttpListenerRef());
        return new BallerinaModel.Service(basePath, listenerRefs, resources, List.of(), List.of(), List.of());
    }

    private static BallerinaModel.Resource convertOperation(ProcessContext cx, String portName, String basePath,
                                                            String apiPath,
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
        String portHandler = cx.addPortHandler(portName, basePath, apiPath, inputType, returnType);
        List<BallerinaModel.Statement> body = List.of(new BallerinaModel.Return(Optional.of(
                new BallerinaModel.Expression.FunctionCall(portHandler, new String[]{"input"}))));
        return new BallerinaModel.Resource(resourceMethodName, path, parameters, Optional.of(returnType.toString()),
                body);
    }

    static BallerinaModel.ModuleTypeDef convertComplexType(ProcessContext cx,
                                                           TibcoModel.Type.Schema.ComplexType complexType) {
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

    private static BallerinaModel.TypeDesc.RecordTypeDesc convertTypeInclusion(ProcessContext cx,
                                                                               TibcoModel.Type.Schema.ComplexType.ComplexContent complexContent) {
        List<BallerinaModel.TypeDesc> inclusions = List.of(cx.getTypeByName(complexContent.extension().base().name()));
        RecordBody body = getRecordBody(cx, complexContent.extension().elements());
        return new BallerinaModel.TypeDesc.RecordTypeDesc(inclusions, body.fields(), body.rest());
    }

    private static BallerinaModel.TypeDesc.RecordTypeDesc convertSequenceBody(ProcessContext cx,
                                                                              TibcoModel.Type.Schema.ComplexType.SequenceBody sequenceBody) {
        Collection<TibcoModel.Type.Schema.ComplexType.SequenceBody.Member> members = sequenceBody.elements();
        RecordBody body = getRecordBody(cx, members);
        return new BallerinaModel.TypeDesc.RecordTypeDesc(List.of(), body.fields(), body.rest());
    }

    private static RecordBody getRecordBody(ProcessContext cx,
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

    static BallerinaModel.TypeDesc.UnionTypeDesc convertTypeChoice(ProcessContext cx,
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

    record LinkHandler(String name, BallerinaModel.TypeDesc inputType, Collection<String> registeredListeners) {

        public void registerListener(String listener) {
            registeredListeners.add(listener);
        }

        public BallerinaModel.Function toFunction() {
            List<BallerinaModel.Statement> body = registeredListeners.stream()
                    .map(listener -> new BallerinaModel.Expression.FunctionCall(listener, new String[]{"input"}))
                    .map(BallerinaModel.CallStatement::new).map(each -> (BallerinaModel.Statement) each).toList();

            return new BallerinaModel.Function(Optional.empty(), name,
                    List.of(new BallerinaModel.Parameter("input", inputType.toString(), Optional.empty())),
                    Optional.empty(), body);
        }
    }

    record PortHandler(String name, BallerinaModel.TypeDesc inputType, BallerinaModel.TypeDesc returnType,
                       Collection<String> registeredListeners) {

        PortHandler(String name, BallerinaModel.TypeDesc inputType, BallerinaModel.TypeDesc returnType) {
            this(name, inputType, returnType, new ArrayList<>());
        }

        public void registerListener(String listener) {
            registeredListeners.add(listener);
        }

        public BallerinaModel.Function toFunction() {
            assert registeredListeners.size() == 1 : "multiple listeners not implemented";
            List<BallerinaModel.Statement> body = registeredListeners.stream()
                    .map(listener -> new BallerinaModel.Expression.FunctionCall(listener, new String[]{"input"}))
                    .map(Optional::of)
                    .map(BallerinaModel.Return::new).map(each -> (BallerinaModel.Statement) each).toList();
            return new BallerinaModel.Function(Optional.empty(), name,
                    List.of(new BallerinaModel.Parameter("input", inputType.toString(), Optional.empty()))
                    , Optional.of(returnType.toString()), body);
        }
    }
}
