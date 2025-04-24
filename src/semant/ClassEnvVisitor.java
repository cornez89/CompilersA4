package semant;

import java.util.Iterator;
import ast.ASTNode;
import ast.Class_;
import ast.Field;
import ast.Formal;
import ast.Method;
import ast.Program;
import util.ClassTreeNode;
import util.ErrorHandler;

public class ClassEnvVisitor extends SemantVisitor {

    // get all parent fields and put them in current class symbol table
    // ClassTreeNode parent = classTreeNode.getParent();
    // while(parent != null)
    // {
    // Class_ parentAST = parent.getASTNode();

    // // get an iterator of the parents member list
    // Iterator<ASTNode> iterator = parentAST.
    // getMemberList().getIterator();

    // // check each member and see if it's a field, if it
    // // is, add it to the current classTreeNode symbol table
    // while(iterator.hasNext())
    // {
    // ASTNode currentMember = iterator.next();

    // if(currentMember instanceof Field)
    // {
    // Field field = (Field) currentMember;
    // classTreeNode.getVarSymbolTable().add
    // (field.getName(), field.getType());
    // }
    // }
    // }
    protected boolean methodArgsMatch(Method method1, Method method2) {
        boolean isValid = true;
        if (method1.getFormalList().getSize() != method2.getFormalList().getSize()) {
            errorHandler.register(errorHandler.SEMANT_ERROR,
                    classTreeNode.getASTNode().getFilename(),
                    0,
                    "number of actual parameters (" + method1.getFormalList().getSize()
                            + ") differs from number of formal parameters ("
                            + method2.getFormalList().getSize()
                            + ") in dispatch to method '" + method1.getName() + "'");
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
                                    type1 + "'' does not match formal parameter "
                                    + i + " with declared type '" +
                                    type2 + "' in dispatch to method'"
                                    + method1.getName() + "'");
                    isValid = false;
                }
                i++;
            }
        }
        return isValid;
    }

    protected boolean isValidMethodOverride(Method method) {
        Method originalMethod = (Method) classTreeNode.getMethodSymbolTable().lookup(method.getName());
        boolean isValid = true;

        if (!method.getReturnType().equals(originalMethod.getReturnType())) {
            errorHandler.register(errorHandler.SEMANT_ERROR,
                    classTreeNode.getASTNode().getFilename(),
                    method.getLineNum(),
                    "Error in BuildClassTree: Invalid Override. Return type: "
                            + method.getReturnType() +
                            " does not match original return type: "
                            + originalMethod.getReturnType());
            isValid = false;
        }

        if (methodArgsMatch(method, originalMethod)) {
            // already registered errors
            isValid = false;
        }

        return isValid;
    }

    public ClassEnvVisitor(ClassTreeNode classTreeNode, ErrorHandler errorHandler) {
        super.classTreeNode = classTreeNode;
        super.errorHandler = errorHandler;
    }

    public Object visit(Program program) {
        Iterator<ASTNode> iterator = program.getClassList().getIterator();

        while (iterator.hasNext()) {
            iterator.next().accept(this);
        }

        return null;
    }

    public Object visit(ClassTreeNode classTreeNode) {
        super.classTreeNode = classTreeNode;
        Iterator<ClassTreeNode> children = classTreeNode.getChildrenList();
        enterScope();
        classTreeNode.getASTNode().accept(this);
        while (children.hasNext()) {
            ClassTreeNode child = children.next();
            // check after each child
            if (classTreeNode.getVarSymbolTable().getSize() > 1500) {
                errorHandler.register(errorHandler.SEMANT_ERROR,
                        classTreeNode.getASTNode().getFilename(), 0,
                        "Error in BuildSymbolTable: Max number of field" +
                                "has been exceeded in class: " + classTreeNode.getName()
                                + ".");
            }

            visit(child);
            super.classTreeNode = child.getParent();

        }
        return null;
    }

    public Object visit(Class_ node) {
        Iterator<ASTNode> classIterator = node.getMemberList().getIterator();
        while (classIterator.hasNext()) {
            classIterator.next().accept(this);
        }
        return null;
    }

    public Object visit(Field node) {
        if (classTreeNode.getVarSymbolTable().peek(node.getName()) != null) {
            // name already exists
            errorHandler.register(errorHandler.SEMANT_ERROR,
                    classTreeNode.getASTNode().getFilename(), node.getLineNum(),
                    "field '" + node.getName() + "' is already defined in class '" + classTreeNode.getName() + "'");
        } else {
            classTreeNode.getVarSymbolTable().add(node.getName(), node.getType());
        }
        return null;
    }

    public Object visit(Method node) {
        if (classTreeNode.getMethodSymbolTable().peek(node.getName()) != null) {

            // name already exists
            registerSemanticError(node, "method '" + node.getName() +
                    "' is already defined in class '" + classTreeNode.getName() + "'");
        } else if (classTreeNode.getMethodSymbolTable().lookup(node.getName()) != null) {

            // method will report errors for us
            isValidMethodOverride(node);
            classTreeNode.getMethodSymbolTable().add(node.getName(), node);
        } else {
            classTreeNode.getMethodSymbolTable().add(node.getName(), node);
        }

        return null;
    }
}