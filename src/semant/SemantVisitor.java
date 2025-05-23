package semant;

import ast.ASTNode;
import util.ClassTreeNode;
import util.ErrorHandler;
import visitor.Visitor;

abstract public class SemantVisitor extends Visitor {

    protected ClassTreeNode classTreeNode;
    protected ErrorHandler errorHandler;

    protected static String STRING = "String";
    protected static String INT = "int";
    protected static String BOOL = "boolean";
    protected static String VOID = "void";
    protected static String OBJECT = "Object";
    protected static String SUPER = "super";
    protected static String THIS = "this";
    protected static String NULL = "null";
    static private String[] reservedWordsArray = { "this", "super", "null" };

    /**
     * Gets if 'type' is an array or array of primitives.
     * 
     * @param type
     */
    public static boolean isPrimitiveOrArray(String type) {
        if (isArray(type)) {
            type = type.substring(0, type.length() - 2);
        }
        return isPrimitive(type);
    }

    public static boolean isPrimitive(String type) {
        return type != null && (type.equals(BOOL) || type.equals(INT));
    }

    public static boolean isPrimitiveOrVoid(String type) {
        return type != null && (isPrimitive(type) || type.equals(VOID));
    }

    public static boolean isArray(String type) {
        return type.length() > 2
            && type.substring(type.length() - 2).equals("[]");
    }

    protected void registerSemanticError(ASTNode node, String message) {
        if (node == null) {
            errorHandler.register(errorHandler.SEMANT_ERROR, message);
        } else {
            errorHandler.register(errorHandler.SEMANT_ERROR,
                classTreeNode.getASTNode().getFilename(), node.getLineNum(),
                message);
        }
    }

    /**
     * Must be given types that exist
     * Checks if type 1 conforms to type 2 (type 1 is a subclass of type 2)
     * or if they are primitive, checks if they are equal.
     * Gives error messages if either are false
     * 
     * @param type1
     * @param type2
     * @return true if no errors are found
     */
    public boolean conformsTo(String type1, String type2) {
        if (type1 == null || type2 == null)
            return false;
        boolean isArray1 = type1.endsWith("[]");
        boolean isArray2 = type2.endsWith("[]");
        if (isArray1 ^ isArray2) {
            return type1.equals(NULL) || type2.equals(OBJECT);
        }
        if (isArray1) {
            return conformsTo(type1.substring(0, type1.length() - 2),
                type2.substring(0, type2.length() - 2));
        }
        ClassTreeNode node1;
        ClassTreeNode node2;

        if (type1.equals(VOID) && type2.equals(VOID)) {
            return true;
        } else if (isPrimitive(type1) ^ isPrimitive(type2)) {
            return false;
        } else if (isPrimitive(type1) && isPrimitive(type2)) {
            return type1.equals(type2);
        } else if (type1.equals(NULL) || type2.equals(NULL)) {
            return true;
        } else if (!typeExists(type1) || !typeExists(type2)) {
            return false;
        } else {
            node1 = classTreeNode.lookupClass(type1);
            node2 = classTreeNode.lookupClass(type2);

            while (node1 != null) {
                if (node1.getName().equals(node2.getName()))
                    return true;
                node1 = node1.getParent();
            }
            return false;
        }
    }

    protected boolean typeExists(String type) {
        String trimmedType = removeArray(type);
        return classTreeNode.lookupClass(trimmedType) != null
            || isPrimitive(trimmedType);
    }

    protected String removeArray(String type) {
        if (type.endsWith("[]")) {
            return type.substring(0, type.length() - 2);
        }
        return type;
    }

    protected boolean isReserved(String name) {
        for (String word : reservedWordsArray)
            if (word.equals(name))
                return true;
        return false;
    }

    protected boolean isValidReturnType(String type) {
        return typeExists(type) || type.equals(VOID);
    }

    protected void addVar(String name, String type) {
        classTreeNode.getVarSymbolTable().add(name, type);
    }

    protected boolean existsInCurrentVarScope(String name) {
        return classTreeNode.getVarSymbolTable().getScopeLevel(
            name) == classTreeNode.getVarSymbolTable().getCurrScopeLevel();
    }

    static public boolean existsInClass(String name, ClassTreeNode classTreeNode) {
        if (classTreeNode.getParent() != null)
            return classTreeNode.getVarSymbolTable().getScopeLevel(name)
                - classTreeNode.getParent().getVarSymbolTable()
                    .getCurrScopeLevel() > -1;
        else
            return classTreeNode.getVarSymbolTable().getScopeLevel(name) > -1;
    }

    protected boolean existsInCurrentMethodScope(String name) {
        return classTreeNode.getMethodSymbolTable().getScopeLevel(
            name) == classTreeNode.getMethodSymbolTable().getCurrScopeLevel();
    }

    protected boolean existsInMethodVarScope(String name) {
        if (classTreeNode.getParent() != null)
            return classTreeNode.getVarSymbolTable().getScopeLevel(name)
                - classTreeNode.getParent().getVarSymbolTable()
                    .getCurrScopeLevel() > 1;
        else
            return classTreeNode.getVarSymbolTable().getScopeLevel(name) > 1;
    }

    protected Object lookupVar(String name) {
        return classTreeNode.getVarSymbolTable().lookup(name);
    }

    protected Object thisLookupVar(String name) {
        return lookupVar("this." + name);
    }

    protected Object superLookupVar(String name) {
        return classTreeNode.getParent().getVarSymbolTable().lookup(name);
    }

    protected Object lookupMethodInClass(String className, String name) {
        ClassTreeNode classTreeNode;
        if (className.endsWith("[]")) {
            classTreeNode = this.classTreeNode.lookupClass(OBJECT);
        } else {
            classTreeNode = this.classTreeNode.lookupClass(className);
        }

        if (classTreeNode == null) {
            return null;
        }

        return classTreeNode.getMethodSymbolTable().lookup(name);
    }

    protected Object lookupMethod(String name) {
        return classTreeNode.getMethodSymbolTable().lookup(name);
    }

    protected Object peekVar(String name) {
        return classTreeNode.getVarSymbolTable().peek(name);
    }

    protected void enterScope() {
        classTreeNode.getVarSymbolTable().enterScope();
        classTreeNode.getMethodSymbolTable().enterScope();
    }

    protected void exitScope() {
        classTreeNode.getVarSymbolTable().exitScope();
        classTreeNode.getMethodSymbolTable().exitScope();
    }
}
