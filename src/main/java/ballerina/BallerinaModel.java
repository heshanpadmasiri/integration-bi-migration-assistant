package ballerina;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record BallerinaModel(DefaultPackage defaultPackage, List<Module> modules) {

    public record DefaultPackage(String org, String name, String version) {
    }

    public record Module(String name, List<TextDocument> textDocuments) {
    }

    public record TextDocument(String documentName, List<Import> imports, List<ModuleTypeDef> moduleTypeDefs,
                               List<ModuleVar> moduleVars, List<Listener> listeners, List<Service> services,
                               List<Function> functions, List<String> Comments) {
    }

    public record Import(String orgName, String moduleName, Optional<String> importPrefix) {
    }

    public record ModuleTypeDef(String name, TypeDesc typeDesc) {
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

            public record RecordField(String name, TypeDesc typeDesc) {

                @Override
                public String toString() {
                    return typeDesc + " " + name + ";";
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
            ANYDATA("anydata"),
            JSON("json"),
            NIL("()"),
            STRING("string"),
            INT("int"),
            XML("xml"),
            ;

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

    public record ModuleVar(String name, String type, BallerinaExpression expr, boolean constant) {

        public ModuleVar(String name, String type, BallerinaExpression expr) {
            this(name, type, expr, false);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (constant) {
                sb.append("const ");
            }
            sb.append(type).append(" ").append(name).append(" ").append("=").append(" ").append(expr.expr)
                    .append(";\n");
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
                           Optional<String> returnType,
                           List<Statement> body) {

        public static Function publicFunction(String name, List<Parameter> parameters, Optional<String> returnType,
                                              List<Statement> body) {
            return new Function(Optional.of("public"), name, parameters, returnType, body);
        }
    }

    public record Parameter(String name, String type, Optional<BallerinaExpression> defaultExpr) {

        // FIXME: refactor this to accept type desc
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

    public record BallerinaExpression(String expr) {
    }

    public record IfElseStatement(BallerinaExpression ifCondition, List<Statement> ifBody,
                                  List<ElseIfClause> elseIfClauses, List<Statement> elseBody) implements Statement {
    }

    public record Comment(String comment) implements Statement {

        @Override
        public String toString() {
            return "//" + comment + "\n";
        }
    }

    public sealed interface Expression {

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

    public sealed interface Statement
            permits BallerinaStatement, CallStatement, Comment, IfElseStatement, NamedWorkerDecl, Return,
            VarDeclStatment {
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
                        String.join(", ", args.stream().map(Objects::toString).toList()) +
                        ")";
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
