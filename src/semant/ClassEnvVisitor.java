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

    protected boolean methodArgsMatch(Method method1, Method method2) {
        boolean isValid = true;
        if (method1.getFormalList().getSize() != method2.getFormalList().getSize()) {
            registerSemanticError(method1,
                    "overriding method '" + method1.getName() + "' has " + method1.getFormalList().getSize()
                            + " formals, which differs from the inherited method (" + method2.getFormalList().getSize()
                            + ")");
            isValid = false;
        } else {
            Iterator<ASTNode> formals1 = method1.getFormalList().getIterator();
            Iterator<ASTNode> formals2 = method2.getFormalList().getIterator();
            int i = 1;
            while (formals1.hasNext()) {
                String type1 = ((Formal) formals1.next()).getType();
                String type2 = ((Formal) formals2.next()).getType();
                if (!type1.equals(type2)) {
                    registerSemanticError(method1,
                            "overriding method '" + method1.getName()
                                    + "' has formal type '" + type1 + "' for formal " + i
                                    + ", which differs from the inherited method's formal type '" + type2 + "'");
                    isValid = false;
                }
                i++;
            }
        }
        return isValid;
    }

    protected boolean isValidMethodOverride(Method method) {
        Method originalMethod = (Method) classTreeNode.getParent().getMethodSymbolTable().lookup(method.getName());
        boolean isValid = true;

        if (methodArgsMatch(method, originalMethod)) {
            // already registered errors
            isValid = false;
        }

        if (!method.getReturnType().equals(originalMethod.getReturnType())) {
            errorHandler.register(errorHandler.SEMANT_ERROR,
                    classTreeNode.getASTNode().getFilename(),
                    method.getLineNum(),
                    "overriding method '" + method.getName() + "' has return type '" + method.getReturnType()
                            + "', which differs from the inherited method's return type '"
                            + originalMethod.getReturnType() + "'");
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
        if (classTreeNode.getParent() != null
                && classTreeNode.getParent().getMethodSymbolTable().lookup(node.getName()) != null) {

            // method will report errors for us
            isValidMethodOverride(node);
            classTreeNode.getMethodSymbolTable().add(node.getName(), node);
        } else if (classTreeNode.getMethodSymbolTable().peek(node.getName()) != null) {
            // name already exists
            registerSemanticError(node, "method '" + node.getName() +
                    "' is already defined in class '" + classTreeNode.getName() + "'");
        } else {
            classTreeNode.getMethodSymbolTable().add(node.getName(), node);
        }

        return null;
    }
}