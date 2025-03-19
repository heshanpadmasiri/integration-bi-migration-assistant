package ballerina;

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
                               List<Function> functions, List<String> Comments, List<String> intrinsics) {

        public TextDocument(String documentName, List<Import> imports, List<ModuleTypeDef> moduleTypeDefs,
                            List<ModuleVar> moduleVars, List<Listener> listeners, List<Service> services,
                            List<Function> functions, List<String> comments) {
            this(documentName, imports, moduleTypeDefs, moduleVars, listeners, services, functions, comments,
                    List.of());
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

        record RecordTypeDesc(List<TypeDesc> inclusions, List<RecordField> fields, Optional<TypeDesc> rest)
                implements TypeDesc {

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

            public record RecordField(String name, TypeDesc typeDesc, Optional<Expression> defaultValue) {

                public RecordField(String name, TypeDesc typeDesc) {
                    this(name, typeDesc, Optional.empty());
                }

                @Override
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    sb.append(typeDesc).append(" ").append(name);
                    defaultValue.ifPresent(expression -> sb.append(" = ").append(expression));
                    sb.append(";");
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
            DECIMAL("decimal");

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

    public record IfElseStatement(BallerinaExpression ifCondition, List<Statement> ifBody,
                                  List<ElseIfClause> elseIfClauses, List<Statement> elseBody) implements Statement {

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

        record CheckPanic(Expression callExpr) implements Expression {

            @Override
            public String toString() {
                return "checkpanic " + callExpr;
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

    public record CallStatement(Expression.FunctionCall callExpr) implements Statement {

        @Override
        public String toString() {
            return callExpr + ";";
        }
    }

    public record Return<E extends Expression>(Optional<E> value) implements Statement {

        public Return(E value) {
            this(Optional.of(value));
        }

        @Override
        public String toString() {
            return value.map(expression -> "return " + expression + ";").orElse("return;");
        }
    }

    public record ElseIfClause(BallerinaExpression condition, List<Statement> elseIfBody) {

    }

    public record DoStatement(List<Statement> doBody, Optional<OnFailClause> onFailClause) implements Statement {

    }

    public record OnFailClause(List<Statement> onFailBody) {

    }

    public sealed interface Statement
            permits BallerinaStatement, CallStatement, Comment, DoStatement, IfElseStatement, NamedWorkerDecl, Return,
            VarAssignStatement, VarDeclStatment {

    }

    public record VarAssignStatement(Expression ref, Expression value) implements Statement {

        @Override
        public String toString() {
            return ref + " = " + value + ";";
        }
    }

    public record VarDeclStatment(TypeDesc type, String varName, Expression expr) implements Statement {

        @Override
        public String toString() {
            return type + " " + varName + " = " + expr + ";";
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
