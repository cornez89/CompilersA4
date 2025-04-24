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
    static private String[] reservedWordsArray = {
            "this", "super", "null", "class", "extends", "for",
            "while", "if", "else", "return", "break", "new"
    };

    /**
     * Gets if 'type' is an array or array of primitives.
     * 
     * @param type
     */
    public boolean isPrimitiveOrArray(String type) {
        if (isArray(type)) {
            type = type.substring(0, type.length() - 2);
        }
        return isPrimitive(type);
    }

    public boolean isPrimitive(String type) {
        return type.equals(BOOL) || type.equals(INT);
    }

    private boolean isArray(String type) {
        return type.length() > 2 && type.substring(type.length() - 2).equals("[]");
    }

    protected void registerSemanticError(ASTNode node, String message) {
        if (node == null) {
            errorHandler.register(
                    errorHandler.SEMANT_ERROR,
                    message);
        } else {
            errorHandler.register(
                    errorHandler.SEMANT_ERROR,
                    classTreeNode.getASTNode().getFilename(),
                    node.getLineNum(),
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
            return null;
        boolean isArray1 = type1.endsWith("[]");
        boolean isArray2 = type2.endsWith("[]");
        if (isArray1 ^ isArray2) {
            return false;
        }
        if (isArray1) {
            return conformsTo(type1.substring(0, type1.length() - 2),
                    type2.substring(0, type1.length() - 2));
        }
        ClassTreeNode node1;
        ClassTreeNode node2;

        if (type1.equals(VOID) && type2.equals(VOID)) {
            return true;
        } else if (!typeExists(type1) || !typeExists(type2)) {
            return false;
        } else if (isPrimitive(type1) ^ isPrimitive(type2)) {
            return false;
        } else if (isPrimitive(type1) && isPrimitive(type2)
                && !type1.equals(type2)) {
            return false;
        } else if (!isPrimitive(type1) && !isPrimitive(type2)) {
            node1 = classTreeNode.lookupClass(type1);
            node2 = classTreeNode.lookupClass(type2);

            if (type2.equals("Object"))
                return true;

            while (node1 != null) {
                if (node1.getName().equals(node2.getName()))
                    return true;
                node1 = node1.getParent();
            }
            return false;
        }

        return true;
    }

    protected boolean typeExists(String type) {
        String trimmedType = removeArray(type);
        if (classTreeNode.lookupClass(trimmedType) != null || isPrimitive(trimmedType)) {
            return true;
        } else {
            return false;
        }
    }

    protected String removeArray(String type) {
        if (type.length() > 2 && type.substring(type.length() - 2).equals("[]")) {
            return type.substring(0, type.length() - 2);
        }
        return type;
    }

    protected boolean isReserved(String name) {
        for (String word : reservedWordsArray) {
            if (word.equals(name))
                return true;
        }
        return false;
    }

    protected boolean isValidReturnType(String type) {
        return typeExists(type) || isPrimitive(type) || type.equals("void");
    }

    protected void addVar(String name, String type) {
        classTreeNode.getVarSymbolTable().add(name, type);
        classTreeNode.getVarSymbolTable().add("this." + name, type);
    }

    protected boolean existsInCurrentVarScope(String name) {
        return classTreeNode.getVarSymbolTable().getScopeLevel(name) == classTreeNode.getVarSymbolTable()
                .getCurrScopeLevel();
    }

    protected boolean existsInCurrentMethodScope(String name) {
        return classTreeNode.getMethodSymbolTable().getScopeLevel(name) == classTreeNode.getMethodSymbolTable()
                .getCurrScopeLevel();
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
        ClassTreeNode classTreeNode = this.classTreeNode.lookupClass(className);
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
