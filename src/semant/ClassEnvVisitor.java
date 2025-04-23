package semant;

import java.lang.reflect.Member;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import ast.ASTNode;
import ast.ArrayAssignExpr;
import ast.AssignExpr;
import ast.BlockStmt;
import ast.Class_;
import ast.DeclStmt;
import ast.ExprStmt;
import ast.Field;
import ast.ForStmt;
import ast.Formal;
import ast.IfStmt;
import ast.Method;
import ast.Program;
import ast.Stmt;
import ast.WhileStmt;
import util.ClassTreeNode;
import util.ErrorHandler;

public class ClassEnvVisitor extends SemantVisitor {

        public ClassEnvVisitor(ClassTreeNode classTreeNode, ErrorHandler errorHandler) {
                super.classTreeNode = classTreeNode;
                super.errorHandler = errorHandler;
        }

        public Object visit(Program program)
        {
                Iterator<ASTNode> iterator = program.getClassList().getIterator();
                
                while(iterator.hasNext())
                {
                        iterator.next().accept(this);
                }

                return null;
        }

        public Object visit(ClassTreeNode classTreeNode) {
                Iterator<ClassTreeNode> children = classTreeNode.getChildrenList();
                enterScope();

                // get all parent fields and put them in current class symbol table
                ClassTreeNode parent = classTreeNode.getParent();
                while(parent != null)
                {
                        Class_ parentAST = parent.getASTNode();

                        // get an iterator of the parents member list
                        Iterator<ASTNode> iterator = parentAST.
                        getMemberList().getIterator();

                        // check each member and see if it's a field, if it
                        // is, add it to the current classTreeNode symbol table
                        while(iterator.hasNext())
                        {
                                ASTNode currentMember = iterator.next();

                                if(currentMember instanceof Field)
                                {
                                        Field field = (Field) currentMember;
                                        classTreeNode.getVarSymbolTable().add
                                        (field.getName(), field.getType());
                                }
                        }
                }

                classTreeNode.getASTNode().accept(this);

                while (children.hasNext()) {
                        ClassTreeNode child = children.next();

                        // check after each child
                        if (classTreeNode.getVarSymbolTable().getSize() > 1500) {
                                errorHandler.register(errorHandler.SEMANT_ERROR,
                                classTreeNode.getASTNode().getFilename(), 0,
                                "Error in BuildSymbolTable: Max number of field" +
                                "has been exceeded in class: " + classTreeNode.getName() + ".");
                        }

                        visit(child);

                }
                exitScope();

                return null;
        }

        public Object visit(Class_ node) {
                enterScope();
                Iterator<ASTNode> classIterator = node.getMemberList().getIterator();
                while (classIterator.hasNext()) {
                        classIterator.next().accept(this);
                }
                exitScope();
                return null;
        }

        public Object visit(Field node) {

                if (classTreeNode.getVarSymbolTable().peek(node.getName()) != null) {
                        // name already exists
                        errorHandler.register(errorHandler.SEMANT_ERROR,
                        classTreeNode.getASTNode().getFilename(), node.getLineNum(),
                        "Error in BuildSymbolTable: Variable: " + node.getName() +
                        " already exists in the current scope.");
                } else {
                        classTreeNode.getVarSymbolTable().add(node.getName(), node.getType());
                }
                return null;
        }

        public Object visit(Method node) {
                if (classTreeNode.getMethodSymbolTable().peek(node.getName()) != null) {

                        // name already exists
                        errorHandler.register(errorHandler.SEMANT_ERROR,
                        classTreeNode.getASTNode().getFilename(), node.getLineNum(),
                        "Error in BuildSymbolTable: Method: " + node.getName() +
                        "() already exists in the current scope.");
                } else if (classTreeNode.getMethodSymbolTable().lookup(node.getName()) != null) {
                        // method will report errors for us
                        isValidMethodOverride(node);

                        classTreeNode.getMethodSymbolTable().add(node.getName(), node);
                } else {
                        classTreeNode.getMethodSymbolTable().add(node.getName(), node);
                }

                // check the formals in the method
                enterScope();

                Iterator<ASTNode> iterator = node.getFormalList().getIterator();

                while (iterator.hasNext()) {
                        ASTNode current = iterator.next();
                        // type cast the ast node to a formal
                        Formal formal = (Formal) current;

                        if (classTreeNode.getVarSymbolTable().peek(formal.getName()) != null) {
                                errorHandler.register(errorHandler.SEMANT_ERROR,
                                classTreeNode.getASTNode().getFilename(),
                                formal.getLineNum(), "Error in Method " +
                                node.getName() + ", " + formal.getName() +
                                " already exists in the current scope.");
                        } else {
                                classTreeNode.getVarSymbolTable().add(formal.getName(), formal.getType());
                        }
                }

                node.getStmtList().accept(this);

                exitScope();

                return null;
        }

        public Object visit(DeclStmt node) {
                if (classTreeNode.getVarSymbolTable().getScopeLevel(node.getName())
                 < classTreeNode.getVarSymbolTable().getCurrScopeLevel()) {
                        classTreeNode.getVarSymbolTable().add(node.getName(), node.getType());
                } else {
                        errorHandler.register(errorHandler.SEMANT_ERROR,
                        classTreeNode.getASTNode().getFilename(), node.getLineNum(),
                        "Error in ClassEnvVisitor: Var: " + node.getName() +
                        " already exists in the current scope.");
                }
                return null;
        }

        public Object visit(IfStmt node) {
                enterScope();
                node.getThenStmt().accept(this);
                exitScope();
                enterScope();
                node.getElseStmt().accept(this);
                exitScope();
                return null;
        }

        public Object visit(WhileStmt node) {
                enterScope();
                node.getBodyStmt().accept(this);
                exitScope();

                return null;
        }

        public Object visit(ForStmt node) {
                enterScope();
                node.getBodyStmt().accept(this);
                exitScope();

                return null;
        }

        public Object visit(BlockStmt node) {
                Iterator<ASTNode> stmts = node.getStmtList().getIterator();
                enterScope();
                while (stmts.hasNext()) {
                        stmts.next().accept(this);
                }
                exitScope();

                return null;
        }
}