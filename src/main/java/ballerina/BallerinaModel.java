package ballerina;

import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record BallerinaModel(DefaultPackage defaultPackage, List<Module> modules) {

    public record DefaultPackage(String org, String name, String version) {

    }

    public record Module(String name, List<TextDocument> textDocuments) {

    }

    public record TextDocument(String documentName, List<Import> imports, List<ModuleTypeDef> moduleTypeDefs,
                               List<ModuleVar> moduleVars, List<Listener> listeners, List<Service> services,
                               List<Function> functions, List<String> Comments, List<String> intrinsics,
                               List<ModuleMemberDeclarationNode> astNodes) {

        public TextDocument(String documentName, List<Import> imports, List<ModuleTypeDef> moduleTypeDefs,
                            List<ModuleVar> moduleVars, List<Listener> listeners, List<Service> services,
                            List<Function> functions, List<String> comments) {
            this(documentName, imports, moduleTypeDefs, moduleVars, listeners, services, functions, comments,
                    List.of(), List.of());
        }
    }

    public record Import(String orgName, String moduleName, Optional<String> importPrefix) {

    }

    public record ModuleTypeDef(String name, TypeDesc typeDesc, List<Comment> comments) {

        public ModuleTypeDef(String name, TypeDesc typeDesc) {
            this(name, typeDesc, List.of());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Comment comment : comments) {
                sb.append(comment);
            }
            if (typeDesc instanceof TypeDesc.RecordTypeDesc recordTypeDesc) {
                recordTypeDesc.namespace().ifPresent(ns -> sb.append(ns.annotation()));
                recordTypeDesc.xmlName().ifPresent(name -> sb.append("""
                        @xmldata:Name {
                            value: "%s"
                        }
                        """.formatted(name)));
            }
            sb.append("type ").append(name).append(" ").append(typeDesc).append(";");
            return sb.toString();
        }
    }

    public sealed interface TypeDesc {

        record MapTypeDesc(TypeDesc typeDesc) implements TypeDesc {

            @Override
            public String toString() {
                return "map<" + typeDesc + ">";
            }
        }

        record ListType(List<TypeDesc> required, Optional<TypeDesc> rest) implements TypeDesc {

            public ListType {
                if (required.isEmpty() && rest.isEmpty()) {
                    throw new IllegalArgumentException("ListType must have at least one required type or a rest type");
                }
            }

            public ListType(TypeDesc rest) {
                this(List.of(), Optional.of(rest));
            }

            public ListType(List<TypeDesc> required) {
                this(required, Optional.empty());
            }

            public ListType(List<TypeDesc> required, TypeDesc rest) {
                this(required, Optional.of(rest));
            }

            @Override
            public String toString() {
                if (required.isEmpty()) {
                    assert rest.isPresent();
                    return rest().get() + "[]";
                }
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                sb.append(String.join(", ", required.stream().map(Objects::toString).toList()));
                rest.ifPresent(typeDesc -> sb.append(", ...").append(typeDesc));
                sb.append("]");
                return sb.toString();
            }
        }

        record StreamTypeDesc(TypeDesc valueTy, TypeDesc completionType) implements TypeDesc {

            @Override
            public String toString() {
                return "stream<" + valueTy + ", " + completionType + ">";
            }
        }

        record RecordTypeDesc(List<TypeDesc> inclusions, List<RecordField> fields, Optional<TypeDesc> rest,
                              Optional<Namespace> namespace, Optional<String> xmlName)
                implements TypeDesc {

            public RecordTypeDesc(List<TypeDesc> inclusions, List<RecordField> fields) {
                this(inclusions, fields, Optional.empty(), Optional.empty(), Optional.empty());
            }

            private static final String INDENT = "  ";

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                boolean inclusiveRecord = rest.isEmpty();
                if (inclusiveRecord) {
                    sb.append("record {").append("\n");
                } else {
                    sb.append("record {|").append("\n");
                }
                for (TypeDesc inclusion : inclusions) {
                    sb.append(INDENT).append("*").append(inclusion).append(";").append("\n");
                }
                for (RecordField field : fields) {
                    sb.append(INDENT).append(field).append("\n");
                }
                rest.ifPresent(typeDesc -> sb.append(INDENT).append(typeDesc).append("...;").append("\n"));
                if (inclusiveRecord) {
                    sb.append("}");
                } else {
                    sb.append("|}");
                }
                return sb.toString();
            }

            public record RecordField(String name, TypeDesc typeDesc, Optional<Expression> defaultValue,
                                      Optional<Namespace> namespace, boolean optional) {

                public RecordField(String name, TypeDesc typeDesc, Expression defaultValue) {
                    this(name, typeDesc, Optional.of(defaultValue), Optional.empty(), false);
                }

                public RecordField(String name, TypeDesc typeDesc) {
                    this(name, typeDesc, Optional.empty(), Optional.empty(), false);
                }

                public RecordField(String name, TypeDesc typeDesc, Namespace namespace) {
                    this(name, typeDesc, Optional.empty(), Optional.of(namespace), false);
                }

                public RecordField(String name, TypeDesc typeDesc, Namespace namespace, boolean optional) {
                    this(name, typeDesc, Optional.empty(), Optional.of(namespace), optional);
                }

                @Override
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    namespace.ifPresent(ns -> sb.append(ns.annotation()));
                    sb.append(typeDesc).append(" ").append(name);
                    defaultValue.ifPresent(expression -> sb.append(" = ").append(expression));
                    if (optional) {
                        sb.append("?");
                    }
                    sb.append(";");
                    return sb.toString();
                }
            }

            public record Namespace(Optional<String> prefix, String uri) {

                String annotation() {
                    StringBuilder sb = new StringBuilder();
                    sb.append("@xmldata:Namespace {");
                    prefix.ifPresent(p -> sb.append(" prefix: \"").append(p).append("\","));
                    sb.append(" uri: \"").append(uri).append("\" }");
                    return sb.toString();
                }
            }
        }

        record TypeReference(String name) implements TypeDesc {

            @Override
            public String toString() {
                return name;
            }
        }

        record UnionTypeDesc(Collection<? extends TypeDesc> members) implements TypeDesc {

            public static UnionTypeDesc of(TypeDesc... members) {
                return new UnionTypeDesc(List.of(members));
            }

            @Override
            public String toString() {
                return String.join(" | ", members.stream().map(Object::toString).toList());
            }
        }

        enum BuiltinType implements TypeDesc {
            ANYDATA("anydata"), JSON("json"), NIL("()"), STRING("string"), INT("int"), XML("xml"), BOOLEAN("boolean"),
            ERROR("error"), DECIMAL("decimal");

            private final String name;

            BuiltinType(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
            }
        }

    }

    // TODO: expr must be optional
    public record ModuleVar(String name, String type, Expression expr, boolean isConstant,
                            boolean isConfigurable) {

        public ModuleVar {
            assert !isConfigurable || isConstant;
        }

        public ModuleVar(String name, String type, BallerinaExpression expr) {
            this(name, type, expr, false, false);
        }

        public static ModuleVar constant(String name, TypeDesc typeDesc, Expression expr) {
            return new ModuleVar(name, typeDesc.toString(), expr, true, false);
        }

        public static ModuleVar configurable(String name, TypeDesc typeDesc, Expression expr) {
            return new ModuleVar(name, typeDesc.toString(), expr, true, true);
        }

        public static ModuleVar configurable(String name, TypeDesc typeDesc) {
            return new ModuleVar(name, typeDesc.toString(), new BallerinaExpression("?"), true, true);
        }

        public ModuleVar(String name, TypeDesc type, BallerinaExpression expr) {
            this(name, type.toString(), expr);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (isConfigurable) {
                sb.append("configurable ");
            } else if (isConstant) {
                sb.append("const ");
            }
            sb.append(type).append(" ").append(name);
            Expression expr = expr();
            if (expr instanceof BallerinaExpression(String content)) {
                if (!content.isEmpty()) {
                    sb.append(" ").append("=").append(" ").append(content);
                }
            } else {
                sb.append(" ").append("=").append(" ").append(expr);
            }
            sb.append(";\n");
            return sb.toString();
        }
    }

    public record Service(String basePath, List<String> listenerRefs, List<Resource> resources,
                          List<Function> functions, List<String> pathParams, List<String> queryParams) {

    }

    // TODO: move port to config map
    public record Listener(ListenerType type, String name, String port, Map<String, String> config) {

    }

    public enum ListenerType {
        HTTP
    }

    public record Resource(String resourceMethodName, String path, List<Parameter> parameters,
                           Optional<String> returnType, List<Statement> body) {

    }

    public record Function(Optional<String> visibilityQualifier, String methodName, List<Parameter> parameters,
                           Optional<String> returnType, List<Statement> body) {

    }

    public record Parameter(String name, String type, Optional<BallerinaExpression> defaultExpr) {

        public Parameter(TypeDesc typeDesc, String name, BallerinaExpression defaultExpr) {
            this(name, typeDesc.toString(), Optional.of(defaultExpr));
        }
        public Parameter(TypeDesc typeDesc, String name) {
            this(name, typeDesc.toString(), Optional.empty());
        }
    }

    public record BallerinaStatement(String stmt) implements Statement {

        @Override
        public String toString() {
            return stmt;
        }
    }

    public record BallerinaExpression(String expr) implements Expression {

        @Override
        public String toString() {
            return expr;
        }
    }

    public record IfElseStatement(Expression ifCondition, List<Statement> ifBody,
                                  List<ElseIfClause> elseIfClauses, List<Statement> elseBody) implements Statement {

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("if ").append(ifCondition).append(" {\n");
            for (Statement statement : ifBody) {
                sb.append(statement).append("\n");
            }
            sb.append("}");
            for (ElseIfClause elseIfClause : elseIfClauses) {
                sb.append(elseIfClause);
            }
            if (!elseBody.isEmpty()) {
                sb.append("else {\n");
                for (Statement statement : elseBody) {
                    sb.append(statement).append("\n");
                }
                sb.append("}");
            }
            return sb.toString();
        }
    }

    public record Comment(String comment) implements Statement {

        public Comment {
            comment = comment.trim();
        }

        @Override
        public String toString() {
            return "\n//" + comment.lines().filter(Predicate.not(String::isBlank)).collect(Collectors.joining("\n//")) +
                    "\n";
        }
    }

    public sealed interface Expression {

        record Not(Expression expression) implements Expression {

            @Override
            public String toString() {
                return "!" + expression;
            }
        }

        record BinaryLogical(Expression left, Expression right, Operator operator) implements Expression {

            @Override
            public String toString() {
                return left + " " + operator + " " + right;
            }

            public enum Operator {
                AND("&&"), OR("||");

                private final String symbol;

                Operator(String symbol) {
                    this.symbol = symbol;
                }

                @Override
                public String toString() {
                    return symbol;
                }
            }
        }

        record TypeCast(TypeDesc typeDesc, Expression expression) implements Expression {

            @Override
            public String toString() {
                return "<" + typeDesc + ">" + expression;
            }
        }

        record FieldAccess(Expression expression, String fieldName) implements Expression {

            @Override
            public String toString() {
                return expression + "." + fieldName;
            }
        }

        record MemberAccess(Expression container, String field) implements Expression {

            @Override
            public String toString() {
                return container + "[\"" + field + "\"]";
            }
        }

        record XMLTemplate(String body) implements Expression {

            @Override
            public String toString() {
                return "xml`" + body() + "`";
            }
        }

        record StringTemplate(String body) implements Expression {

            @Override
            public String toString() {
                return "string`" + body() + "`";
            }
        }

        record MappingConstructor(List<MappingField> fields) implements Expression {

            @Override
            public String toString() {
                return "{" + String.join(", ", fields.stream().map(Objects::toString).toList()) + "}";
            }

            public record MappingField(String key, Expression value) {

                @Override
                public String toString() {
                    return key + ": " + value;
                }
            }
        }

        record StringConstant(String value) implements Expression {

            @Override
            public String toString() {
                return "\"" + value + "\"";
            }
        }

        record NewExpression(Optional<String> classDescriptor, List<Expression> args) implements Expression {

            public NewExpression(List<Expression> args) {
                this(Optional.empty(), args);
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("new ");
                classDescriptor.ifPresent(s -> sb.append(s).append(" "));
                sb.append("(");
                sb.append(String.join(", ", args.stream().map(Objects::toString).toList()));
                sb.append(")");
                return sb.toString();
            }
        }

        record MethodCall(VariableReference object, String methodName, List<Expression> args) implements Expression {

            @Override
            public String toString() {
                return object + "." + methodName + "(" +
                        String.join(", ", args.stream().map(Objects::toString).toList()) + ")";
            }
        }

        record FunctionCall(String functionName, String[] args) implements Expression {

            public FunctionCall(String functionName, List<Expression> args) {
                this(functionName, args.stream().map(Objects::toString).toArray(String[]::new));
            }

            // TODO: replace this with the list
            public FunctionCall(String functionName, Expression[] args) {
                this(functionName, Arrays.stream(args).map(Objects::toString).toArray(String[]::new));
            }

            @Override
            public String toString() {
                return functionName + "(" + String.join(", ", args) + ")";
            }

        }

        record VariableReference(String varName) implements Expression {

            @Override
            public String toString() {
                return varName;
            }
        }

        record Panic(Expression callExpr) implements Expression {

            @Override
            public String toString() {
                return "panic " + callExpr;
            }
        }

        record CheckPanic(Expression callExpr) implements Expression {

            @Override
            public String toString() {
                return "checkpanic " + callExpr;
            }
        }

        record Trap(Expression expr) implements Expression {

            @Override
            public String toString() {
                return "trap " + expr;
            }
        }

        record Check(Expression callExpr) implements Expression {

            @Override
            public String toString() {
                return "check " + callExpr;
            }
        }

        record TypeCheckExpression(VariableReference variableReference, TypeDesc td) implements Expression {

            @Override
            public String toString() {
                return variableReference.varName + " is " + td;
            }
        }

        record TernaryExpression(Expression condition, Expression ifTrue, Expression ifFalse) implements Expression {

            @Override
            public String toString() {
                return condition + " ? " + ifTrue + " : " + ifFalse;
            }
        }
    }

    public record PanicStatement(Expression expression) implements Statement {

        @Override
        public String toString() {
            return "panic " + expression + ";";
        }
    }

    public record CallStatement(Expression callExpr) implements Statement {

        @Override
        public String toString() {
            return callExpr + ";";
        }
    }

    public record Return<E extends Expression>(Optional<E> value) implements Statement {

        public Return() {
            this(Optional.empty());
        }

        public Return(E value) {
            this(Optional.of(value));
        }

        @Override
        public String toString() {
            return value.map(expression -> "return " + expression + ";").orElse("return;");
        }
    }

    public record ElseIfClause(BallerinaExpression condition, List<Statement> elseIfBody) {

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("else if ").append(condition).append(" {\n");
            for (Statement statement : elseIfBody) {
                sb.append(statement).append("\n");
            }
            sb.append("}");
            return sb.toString();
        }
    }

    public record DoStatement(List<Statement> doBody, Optional<OnFailClause> onFailClause) implements Statement {

    }

    public record OnFailClause(List<Statement> onFailBody) {

    }

    public sealed interface Statement
            permits BallerinaStatement, CallStatement, Comment, DoStatement, IfElseStatement, NamedWorkerDecl,
            PanicStatement, Return, VarAssignStatement, VarDeclStatment {

    }

    public record VarAssignStatement(Expression ref, Expression value) implements Statement {

        @Override
        public String toString() {
            return ref + " = " + value + ";";
        }
    }

    public record VarDeclStatment(TypeDesc type, String varName, Optional<Expression> expr) implements Statement {

        public VarDeclStatment(TypeDesc type, String varName) {
            this(type, varName, Optional.empty());
        }

        public VarDeclStatment(TypeDesc type, String varName, Expression expr) {
            this(type, varName, Optional.of(expr));
        }

        @Override
        public String toString() {
            return expr.map(expression -> type + " " + varName + " = " + expression + ";")
                    .orElseGet(() -> type + " " + varName + ";");
        }

        public Expression.VariableReference ref() {
            return new Expression.VariableReference(varName());
        }
    }

    // This is wrong but should be good enough for our uses
    public sealed interface Action extends Expression {

        record WorkerSendAction(Expression expression, String peer) implements Action {

            @Override
            public String toString() {
                return expression.toString() + " -> " + peer;
            }
        }

        record WorkerReceiveAction(String peer) implements Action {

            @Override
            public String toString() {
                return "<- " + peer;
            }
        }

        record RemoteMethodCallAction(Expression expression, String methodName, List<Expression> args)
                implements Action {

            @Override
            public String toString() {
                return expression + "->" + methodName + "(" +
                        String.join(", ", args.stream().map(Objects::toString).toList()) + ")";
            }
        }
    }

    public record NamedWorkerDecl(String name, List<Statement> body) implements Statement {

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("worker ").append(name).append(" {\n");
            for (Statement statement : body) {
                sb.append(statement).append("\n");
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
