package visitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import ast.ASTNode;
import ast.Formal;
import ast.FormalList;
import ast.Method;
import util.ClassTreeNode;
import util.ErrorHandler;

abstract public class SemantVisitor extends Visitor {
    
    protected ClassTreeNode classTreeNode;
        protected ErrorHandler errorHandler;

    protected static String STRING = "String";
    protected static String INT = "int";
    protected static String BOOL = "boolean";
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

    public boolean conformsTo(String type1, String type2) throws RuntimeException{
        ClassTreeNode node1; 
        ClassTreeNode node2;
        if (!typeExists(type1) || !typeExists(type2)) {
            //error

        } else if (isPrimitiveOrArray(type1) ^ isPrimitiveOrArray(type2)) {
            registerSemanticError(null, type2);
        } else if (!isPrimitiveOrArray(type1) && isPrimitive(type2)) {
            //error
        } else if (isPrimitiveOrArray(type1) && isPrimitiveOrArray(type2) && !type1.equals(type2)) {
            //error
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
                0, "number of actual parameters (" + 3 + ") differs from number of formal parameters (2) in dispatch to method 'm6');
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

    protected void checkFormalsMatch(FormalList formalList1, FormalList formalList2) {
        Iterator<ASTNode> formals1 = formalList1.getIterator();
        Iterator<ASTNode> formals2 = formalList2.getIterator();

        int i = 1;
        while (formals1.hasNext()) {
            String type1 = ((Formal) formals1.next()).getType();
            String type2 = ((Formal) formals2.next()).getType();

            if (!type1.equals(type2)) {
                errorHandler.register(errorHandler.SEMANT_ERROR, 
                classTreeNode.getASTNode().getFilename(),
                "actual parameter " + i + " with type '" + type1  + "'' does not match formal parameter " + i + " with declared type '" +  type2 + "' in dispatch to method'" + methodName + "'");
            }
            i++;
        }
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
