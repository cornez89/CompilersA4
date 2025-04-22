package visitor;

import java.lang.foreign.SymbolLookup;
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
import ast.IfStmt;
import ast.Method;
import ast.Program;
import ast.Stmt;
import ast.WhileStmt;
import semant.SemanticAnalyzer;
import util.ClassTreeNode;
import util.ErrorHandler;
import util.SymbolTable;

public class ClassEnvVisitor extends SemantVisitor {

    public ClassEnvVisitor(ClassTreeNode classTreeNode, ErrorHandler errorHandler) {
        super.classTreeNode = classTreeNode;
                super.errorHandler = errorHandler;
    }
        
        public Object visit(ClassTreeNode classTreeNode) {
                Iterator<ClassTreeNode> children = classTreeNode.getChildrenList();
                enterScope(); 
                classTreeNode.getASTNode().accept(this);
                
                while (children.hasNext()) {
                        ClassTreeNode child = children.next();

                        //check after each child
                        if (classTreeNode.getVarSymbolTable().getSize() > 1500) {
                                errorHandler.register(errorHandler.SEMANT_ERROR, 
                                        classTreeNode.getASTNode().getFilename(), 0, 
                                "Error in BuildSymbolTable: Max number of field has been exceeded in class: " 
                                + classTreeNode.getName() + ".");
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
                        //name already exists
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

                        //name already exists
                        errorHandler.register(errorHandler.SEMANT_ERROR, 
                                classTreeNode.getASTNode().getFilename(), node.getLineNum(), 
                                "Error in BuildSymbolTable: Method: " + node.getName() + 
                                "() already exists in the current scope.");
                } else if (classTreeNode.getMethodSymbolTable().lookup(node.getName()) != null) {
                        isValidMethodOverride(node);
                        //errors already registered
                } else {
                        classTreeNode.getMethodSymbolTable().add(node.getName(), node);
                }

                return null;
        
        }

        public Object visit(DeclStmt node) {
                if (classTreeNode.getVarSymbolTable().getScopeLevel(node.getName()) < classTreeNode.getVarSymbolTable().getCurrScopeLevel()) {
                        classTreeNode.getVarSymbolTable().add(node.getName(), node.getType());
                } else {
                        errorHandler.register(errorHandler.SEMANT_ERROR, 
                                classTreeNode.getASTNode().getFilename(), node.getLineNum(), 
                                "Error in ClassEnvVisitor: Var: " + node.getName() + 
                                " already exists in the current scope.");
                }
                return null;
        }

        public Object visit (IfStmt node) {
                enterScope();
                node.getThenStmt().accept(this);
                exitScope();
                enterScope();
                node.getElseStmt().accept(this);
                exitScope();
                return null;
        }

        public Object visit (WhileStmt node) {
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
                while(stmts.hasNext()) {
                        stmts.next().accept(this);
                }
                exitScope();

                return null;
        }
}
