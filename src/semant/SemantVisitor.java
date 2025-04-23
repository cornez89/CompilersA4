package semant;

import java.lang.foreign.SymbolLookup;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import ast.ASTNode;
import ast.Formal;
import ast.FormalList;
import ast.Method;
import util.ClassTreeNode;
import util.ErrorHandler;
import util.SymbolTable;
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

    public boolean isPrimitiveOrArray(String type) {
        if (isArray(type)) {
            type = type.substring(0, type.length()-2);
        }
        return isPrimitive(type);
    }
    public boolean isPrimitive(String type) {
        return type.equals(STRING) || type.equals(BOOL) || type.equals(INT);
    }
    private boolean isArray(String type) {
        return type.length() > 2 && type.substring(type.length() -2).equals("[]");
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
     * @param type1 
     * @param type2
     * @return true if no errors are found
     * @throws RuntimeException
     */
    public boolean conformsTo(String type1, String type2) throws RuntimeException{
        ClassTreeNode node1; 
        ClassTreeNode node2;

        if (type1.equals(VOID) && type2.equals(VOID)) {
            return true;
        } else if (!typeExists(type1) || !typeExists(type2)) {
            return false;
        } else if (isPrimitiveOrArray(type1) ^ isPrimitiveOrArray(type2)) {
            return false;
        } else if (isPrimitiveOrArray(type1) && isPrimitiveOrArray(type2) && !type1.equals(type2)) {
            return false;
        } else if (!isPrimitiveOrArray(type1) && !isPrimitiveOrArray(type2)) {
            node1 = classTreeNode.lookupClass(type1);
            node2 = classTreeNode.lookupClass(type2);
            
            System.out.println(node1.getName());
            System.out.println(node2.getName());

            while(node1.getParent() != null) {
                if (node1.getName().equals(node2.getName())) {
                    return true;
                }
                node1 = node1.getParent();
            }
            return false;
        }
    
        return true;
    }   

    protected boolean typeExists(String type) {
        return classTreeNode.lookupClass(type) != null || isPrimitive(type) || 
            (type.substring(type.length()-2).equals("[]") && (classTreeNode.lookupClass(type.substring(0, type.length()-2)) != null || isPrimitive(type.substring(0, type.length()-2))));
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

    protected boolean isValidMethodOverride(Method method) {
        Method originalMethod = (Method) classTreeNode.getMethodSymbolTable().lookup(method.getName());
        boolean isValid = true;
        
        if (!method.getReturnType().equals(originalMethod.getReturnType())) {
            errorHandler.register(errorHandler.SEMANT_ERROR, 
                classTreeNode.getASTNode().getFilename(),
                0, "Error in BuildClassTree: Invalid Override. Return type: " + method.getReturnType() + 
                    " does not match original return type: " + originalMethod.getReturnType());
            isValid = false;
        } 

        if (methodArgsMatch(method, originalMethod)) {
            //already registered errors
            isValid = false;
        } 

        return isValid;
    }

    protected boolean methodArgsMatch(Method method1, Method method2 ) {
        boolean isValid = true;
        if (method1.getFormalList().getSize() != method2.getFormalList().getSize()) {
            errorHandler.register(errorHandler.SEMANT_ERROR, 
                classTreeNode.getASTNode().getFilename(),
                0, "number of actual parameters (" + method1.getFormalList().getSize() + ") differs from number of formal parameters (" + method2.getFormalList().getSize() + ") in dispatch to method '" + method1.getName() + "'");
            isValid = false;
        } else {
            Iterator<ASTNode> formals1 = method1.getFormalList().getIterator();
            Iterator<ASTNode> formals2 = method2.getFormalList().getIterator();
            int i = 0;
            while (formals1.hasNext()) {
                String type1 = ((Formal) formals1.next()).getType();
                String type2 = ((Formal) formals2.next()).getType();
                if (!type1.equals(type2)) {
                    errorHandler.register(errorHandler.SEMANT_ERROR, 
                    classTreeNode.getASTNode().getFilename(),
                    0, "actual parameter " + i + " with type '" + 
                    type1  + "'' does not match formal parameter " + i + " with declared type '" +  
                    type2 + "' in dispatch to method'" + method1.getName() + "'");
                        isValid = false;
                }
                i++;
            }
        }
        return isValid;
    }

    protected void addVar(String name, String type) {
        classTreeNode.getVarSymbolTable().add(name, type);
        classTreeNode.getVarSymbolTable().add("this." + name, type);
    }

    protected boolean existsInCurrentVarScope(String name) {
        return classTreeNode.getVarSymbolTable().getScopeLevel(name) == classTreeNode.getVarSymbolTable().getCurrScopeLevel();
    }

    protected boolean existsInCurrentMethodScope(String name) {
        return classTreeNode.getMethodSymbolTable().getScopeLevel(name) == classTreeNode.getMethodSymbolTable().getCurrScopeLevel();
    }

    protected Object lookupVar(String name) {
        System.out.printf("Class: %s, currScope: %d, size: %d\n", classTreeNode.getName(), classTreeNode.getVarSymbolTable().getCurrScopeLevel(), classTreeNode.getVarSymbolTable().getCurrScopeSize());
       return classTreeNode.getVarSymbolTable().lookup(name);
    }
    protected Object thisLookupVar(String name) {
        System.out.printf("Class: %s, currScope: %d, size: %d\n", classTreeNode.getName(), classTreeNode.getVarSymbolTable().getCurrScopeLevel(), classTreeNode.getVarSymbolTable().getCurrScopeSize());
       return lookupVar("this." + name);
    }
    protected Object superLookupVar(String name) {
        System.out.printf("Class: %s, currScope: %d, size: %d\n", classTreeNode.getName(), classTreeNode.getVarSymbolTable().getCurrScopeLevel(), classTreeNode.getVarSymbolTable().getCurrScopeSize());
       return classTreeNode.getParent().getVarSymbolTable().lookup(name);
    }

    protected Object lookupMethodInClass(String className, String name) {
        ClassTreeNode classTreeNode = this.classTreeNode.lookupClass(className);
        return classTreeNode.getMethodSymbolTable().lookup(name);
    }
    
    protected Object lookupMethod(String name) {
        System.out.printf("Class: %s, currScope: %d, size: %d\n", classTreeNode.getName(), classTreeNode.getVarSymbolTable().getCurrScopeLevel(), classTreeNode.getVarSymbolTable().getCurrScopeSize());
       return classTreeNode.getMethodSymbolTable().lookup(name);
    }

    protected Object peekVar(String name) {
        System.out.printf("Class: %s, currScope: %d, size: %d\n", classTreeNode.getName(), classTreeNode.getVarSymbolTable().getCurrScopeLevel(), classTreeNode.getVarSymbolTable().getCurrScopeSize());
       return classTreeNode.getVarSymbolTable().peek(name);
    }

    protected Object methodExistsInClass(String name) {
        System.out.printf("Class: %s, currScope: %d, size: %d\n", classTreeNode.getName(), classTreeNode.getVarSymbolTable().getCurrScopeLevel(), classTreeNode.getVarSymbolTable().getCurrScopeSize());
       return classTreeNode.getMethodSymbolTable().lookup(name);
    }


        protected void enterScope() {
        System.out.printf("Class: %s\n", classTreeNode.getName());
                classTreeNode.getVarSymbolTable().enterScope();
                classTreeNode.getMethodSymbolTable().enterScope();
        }

        protected void exitScope() {
        System.out.printf("Class: %s\n", classTreeNode.getName());
                classTreeNode.getVarSymbolTable().exitScope();
                classTreeNode.getMethodSymbolTable().exitScope();
        }
}
