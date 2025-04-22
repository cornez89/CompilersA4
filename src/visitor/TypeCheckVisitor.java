package visitor;
import java.util.Hashtable;
import java.util.Iterator;

import javax.management.RuntimeErrorException;
import javax.print.attribute.HashAttributeSet;

import ast.ASTNode;
import ast.ArrayAssignExpr;
import ast.ArrayExpr;
import ast.AssignExpr;
import ast.BinaryArithDivideExpr;
import ast.BinaryArithExpr;
import ast.BinaryArithMinusExpr;
import ast.BinaryArithModulusExpr;
import ast.BinaryArithPlusExpr;
import ast.BinaryArithTimesExpr;
import ast.BinaryCompEqExpr;
import ast.BinaryCompExpr;
import ast.BinaryCompGeqExpr;
import ast.BinaryCompGtExpr;
import ast.BinaryCompLeqExpr;
import ast.BinaryCompLtExpr;
import ast.BinaryCompNeExpr;
import ast.BinaryExpr;
import ast.BinaryLogicAndExpr;
import ast.BinaryLogicExpr;
import ast.BinaryLogicOrExpr;
import ast.BlockStmt;
import ast.BreakStmt;
import ast.CastExpr;
import ast.ClassList;
import ast.Class_;
import ast.ConstBooleanExpr;
import ast.ConstExpr;
import ast.ConstIntExpr;
import ast.ConstStringExpr;
import ast.DeclStmt;
import ast.DispatchExpr;
import ast.Expr;
import ast.ExprList;
import ast.ExprStmt;
import ast.ForStmt;
import ast.Formal;
import ast.FormalList;
import ast.IfStmt;
import ast.InstanceofExpr;
import ast.Method;
import ast.NewArrayExpr;
import ast.NewExpr;
import ast.ReturnStmt;
import ast.Stmt;
import ast.StmtList;
import ast.UnaryDecrExpr;
import ast.UnaryExpr;
import ast.UnaryIncrExpr;
import ast.UnaryNegExpr;
import ast.UnaryNotExpr;
import ast.VarExpr;
import ast.WhileStmt;
import util.ClassTreeNode;
import util.ErrorHandler;
import util.SymbolTable;

public class TypeCheckVisitor extends SemantVisitor {


    public TypeCheckVisitor (ClassTreeNode classTreeNode, ErrorHandler errorHandler) {
        super.classTreeNode = classTreeNode;
        super.errorHandler = errorHandler;
    }



    String errorMessagePrefix(ASTNode node) {
        return "Error in " + node.getClass().getSimpleName() + ": ";
    }

    
    /**
     * 
     * 
     * 
     * 
     * Tested
     * 
     * 
     * 
     */



    /**
     * 
     *
     * 
     *  
     * Completed Nodes
     * 
     * 
     * 
     */

     /** Visit a list node of classes
      * @param node the class list node 
      * @return result of the visit 
      * */

      public Object visit(ClassTreeNode classTreeNode) {
        System.out.printf("Curr Class: %s, num of children: %d", classTreeNode.getName(), classTreeNode.getNumChildren());
        Iterator<ClassTreeNode> children = classTreeNode.getChildrenList();
        System.out.printf("ClassTreeNode\n");
        enterScope(); 
        classTreeNode.getASTNode().accept(this);
        
        while (children.hasNext()) {
          enterScope();
          ClassTreeNode child = children.next();
          child.getVarSymbolTable().setParent(classTreeNode.getVarSymbolTable());
          child.getMethodSymbolTable().setParent(classTreeNode.getMethodSymbolTable());
          (new TypeCheckVisitor(child, errorHandler)).visit(child);
          exitScope();
        }
        exitScope();
        return null;
      }

      
    /** Visit a class node
      * @param node the class node
      * @return result of the visit 
      * */
    public Object visit(Class_ node) {
      node.getMemberList().accept(this);
      return null;
        }

    /** Visit an assignment expression node
      * @param node the assignment expression node
      * @return result of the visit 
      * */
      public Object visit(AssignExpr node) { 
        //check that expr type conforms to the type of the variable
        node.getExpr().accept(this);
        String assignmentType = classTreeNode.getVarSymbolTable().lookup(node.getName()).toString();
        if (!conformsTo(node.getExpr().getExprType(), assignmentType)) {
            throw new RuntimeException(errorMessagePrefix(node) + "Expr type"
                + node.getExpr().getExprType() + " doesn't conform to type: " 
                + assignmentType + ".");
        }

        node.setExprType(assignmentType);
        return null;
    }

    /** Visit an array assignment expression node
      * @param node the array assignment expression node
      * @return result of the visit 
      * */
    public Object visit(ArrayAssignExpr node) { 
        //check that index returns an int
        
        node.getIndex().accept(this);
        System.out.printf("got here 1\n");
        if (node.getIndex().getExprType() != INT) {
            throw new RuntimeException(errorMessagePrefix(node) + "Type of index expression must be int, given type: " + node.getIndex().getExprType() + ". Line Num " + node.getLineNum());
            
        }
        //check that the variable is 

        //check that return type of expr conforms to type of array
        node.getExpr().accept(this);
        conformsTo("String", "Object");
        lookupVar("foo");
        System.out.printf("got here 2\n");

        String exprType = node.getExpr().getExprType();
        String baseTypeOfArray = (String) lookupVar(node.getName());

        System.out.printf("got here 3\n");
        if (!conformsTo(exprType, baseTypeOfArray)) {
            throw new RuntimeException(errorMessagePrefix(node) + "Tried to assign type: " + 
                exprType + " to the array: " + baseTypeOfArray + "[].");
        }
        
        System.out.printf("got here 4\n");
        node.setExprType(node.getExpr().getExprType());
        
        System.out.printf("got here 5\n");
        return null; 
    }

    /** Visit a binary comparison expression node (should never be called)
      * @param node the binary comparison expression node
      * @return result of the visit 
      * */
      public Object binaryCompExpr(BinaryCompExpr node) { 
        //check that both operands conform to each other
        
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);

        Expr leftOperand = (Expr) node.getLeftExpr();
        Expr rightOperand = (Expr) node.getRightExpr();
        
        
        if(!conformsTo(leftOperand.getExprType(), rightOperand.getExprType()) && 
            !conformsTo(rightOperand.getExprType(), leftOperand.getExprType())) {
            throw new RuntimeException(errorMessagePrefix(node) + "Left operand or right" +
                "expression must conform to the other. Left operand type: " + 
                leftOperand.getExprType() + " and right operand type: " 
                + rightOperand.getExprType()  + ".");
        }  
    
        node.setExprType(BOOL);
        return null;
    }

    /** Visit a binary comparison equals expression node
      * @param node the binary comparison equals expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryCompEqExpr node) { 
        binaryCompExpr(node);
        return null; 
    }

    /** Visit a binary comparison not equals expression node
      * @param node the binary comparison not equals expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryCompNeExpr node) { 
         binaryCompExpr(node);
        return null; 
    }

    /** Visit a binary comparison less than expression node
      * @param node the binary comparison less than expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryCompLtExpr node) { 
        binaryCompExpr(node);
            return null; 
    }

    /** Visit a binary comparison less than or equal to expression node
      * @param node the binary comparison less than or equal to expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryCompLeqExpr node) { 
        binaryCompExpr(node);
            return null; 
    }

    /** Visit a binary comparison greater than expression node
      * @param node the binary comparison greater than expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryCompGtExpr node) { 
        binaryCompExpr(node);
            return null; 
    }

    /** Visit a binary comparison greater than or equal to expression node
      * @param node the binary comparison greater to or equal to expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryCompGeqExpr node) { 
        binaryCompExpr(node);
            return null; 
    }

    /** Visit a binary arithmetic expression node (should never be called)
      * @param node the binary arithmetic expression node
      * @return result of the visit 
      * */
    public Object binaryArithExpr(BinaryArithExpr node) { 
        //check that both operands are int\
        Expr leftOperand = (Expr) node.getLeftExpr();
        Expr rightOperand = (Expr) node.getRightExpr();
        node.getLeftExpr().accept(this);
        
        if(leftOperand.getExprType() != INT) {
            throw new RuntimeException(errorMessagePrefix(node) + "Left Operand must be of type int, given type: " + leftOperand.getExprType() + ".");
        } 
    
        node.getRightExpr().accept(this);
    
        if(rightOperand.getExprType() != INT) {
            throw new RuntimeException(errorMessagePrefix(node) + "Left Operand must be of type int, given type: " + leftOperand.getExprType() + ".");
        } 
        
        node.setExprType(BOOL);
        return null;
    }

    /** Visit a binary arithmetic plus expression node
      * @param node the binary arithmetic plus expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryArithPlusExpr node) { 
        binaryArithExpr(node);
            return null;
    }

    /** Visit a binary arithmetic minus expression node
      * @param node the binary arithmetic minus expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryArithMinusExpr node) { 
        binaryArithExpr(node);
            return null; 
    }

    /** Visit a binary arithmetic times expression node
      * @param node the binary arithmetic times expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryArithTimesExpr node) { 
            binaryArithExpr(node);
        return null; 
    }

    /** Visit a binary arithmetic divide expression node
      * @param node the binary arithmetic divide expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryArithDivideExpr node) { 
        binaryArithExpr(node);
            return null; 
    }

    /** Visit a binary arithmetic modulus expression node
      * @param node the binary arithmetic modulus expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryArithModulusExpr node) { 
            binaryArithExpr(node);
        return null; 
    }

    /** Visit a binary logical expression node (should never be called)
      * @param node the binary logical expression node
      * @return result of the visit 
      * */
    public Object binaryLogicExpr(BinaryLogicExpr node) { 
        //check that both operands are int\
        Expr leftOperand = (Expr) node.getLeftExpr();
        Expr rightOperand = (Expr) node.getRightExpr();
        node.getLeftExpr().accept(this);
        
        if(leftOperand.getExprType() != BOOL) {
            throw new RuntimeException(errorMessagePrefix(node) + "Left Operand must be of type bool, given type: " + leftOperand.getExprType() + ".");
        } 
    
        node.getRightExpr().accept(this);
    
        if(rightOperand.getExprType() != BOOL) {
            throw new RuntimeException(errorMessagePrefix(node) + "Left Operand must be of type bool, given type: " + leftOperand.getExprType() + ".");
        } 
        
        node.setExprType(BOOL);
        return null;
    }

    /** Visit a binary logical AND expression node
      * @param node the binary logical AND expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryLogicAndExpr node) { 
        binaryLogicExpr(node);
            return null; 
    }

    /** Visit a binary logical OR expression node
      * @param node the binary logical OR expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryLogicOrExpr node) {
        binaryLogicExpr(node);
        return null;
    }

    

    /** Visit a unary negation expression node
      * @param node the unary negation expression node
      * @return result of the visit 
      * */
      public Object visit(UnaryNegExpr node) { 
        System.out.println("got here 6");
            node.getExpr().accept(this);
            Expr expr = node.getExpr();
        if (!expr.getExprType().equals(INT)) {
            throw new RuntimeException(errorMessagePrefix(node) + 
                "Expression must be of type int, given type: " + 
                expr.getExprType() + ".");
        }
        
        node.setExprType(INT);

        return null; 
    }

    /** Visit a unary NOT expression node
      * @param node the unary NOT expression node
      * @return result of the visit 
      * */
    public Object visit(UnaryNotExpr node) { 
        node.getExpr().accept(this);
        Expr expr = node.getExpr();
        if (!expr.getExprType().equals(BOOL)) {
            throw new RuntimeException(errorMessagePrefix(node) + 
                "Expression must be of type boolean, given type: " + 
                expr.getExprType() + ".");
        }
        
        expr.setExprType(BOOL);
        return null; 
    }

    /** Visit a unary increment expression node
      * @param node the unary increment expression node
      * @return result of the visit 
      * */
    public Object visit(UnaryIncrExpr node) { 
        node.getExpr().accept(this);
        Expr expr = node.getExpr();
        if (!expr.getExprType().equals(INT)) {
            throw new RuntimeException(errorMessagePrefix(node) + 
                "Expression must be of type int, given type: " + 
                expr.getExprType() + ".");
        }
        
        expr.setExprType(INT);
        return null; 
    }

    /** Visit a unary decrement expression node
      * @param node the unary decrement expression node
      * @return result of the visit 
      * */
    public Object visit(UnaryDecrExpr node) { 
        node.getExpr().accept(this);
            Expr expr = node.getExpr();
        if (!expr.getExprType().equals(INT)) {
            throw new RuntimeException(errorMessagePrefix(node) + 
                "Expression must be of type int, given type: " + 
                expr.getExprType() + ".");
        }
        
        expr.setExprType(INT);
        return null; 
    }

    
    /** Visit a variable expression node
      * @param node the variable expression node
      * @return result of the visit 
      * */
      public Object visit(VarExpr node) { 
        Expr refExpr = node.getRef();
        String name = node.getName();
        if (node.getRef() != null) {
            node.getRef().accept(this);
            
            //check that variable was declared
            String type = (String) classTreeNode.getVarSymbolTable().lookup(name);
            if(type == null) {
                registerSemanticError(node, "Variable with name: " + name + 
                    " has not been declared.");
            } 

            if (refExpr instanceof VarExpr) {

            } else if (refExpr instanceof ArrayExpr) {
                //if the ref is an array, the only name allowed is length
                if (!name.equals("length")) {
                    registerSemanticError(node, errorMessagePrefix(node) + 
                    "Only valid field for array is length, given field name: " + 
                    name + ".");
                }
            }
        }
            
        return null; 
    }

    public Object visit(ConstIntExpr node) { 
      node.setExprType(INT);
      return null; 
    }
  
    /** Visit a boolean constant expression node
      * @param node the boolean constant expression node
      * @return result of the visit 
      * */
    public Object visit(ConstBooleanExpr node) { 
      node.setExprType(BOOL);
      return null; 
    }
    
    /** Visit a string constant expression node
      * @param node the string constant expression node
      * @return result of the visit 
      * */
    public Object visit(ConstStringExpr node) { 
      node.setExprType(STRING);
      return null; 
    }

     /**
      * 
      *
      *
      *
      * Not completed
      *
      *
      *
      */
  public Object visit(ast.Field node) { 
    if (node.getInit() != null) {
      Expr initExpr = node.getInit();
      initExpr.accept(this);

      if (initExpr.getExprType().equals("void")) {


      }
      if (!conformsTo(node.getInit().getExprType(), node.getType())) {
        registerSemanticError(node, errorMessagePrefix(node) + "Type of init: " + 
          node.getInit().getExprType() + " doesn't conform to type of field node: " + 
          node.getType() + ".");
      }
    }

          return null; 
  }

    /** Visit a method node
      * @param node the method node
      * @return result of the visit 
      * */
    public Object visit(Method node) { 
      System.out.printf("Method\n");
      enterScope();

      if (methodExistsInClass(node.getName()) != null) {
        registerSemanticError(node, "method '" + node.getName() + "' is already defined in class '" + classTreeNode.getName() + "'" );
      } else if (lookupMethod(node.getName()) != null && !isValidMethodOverride(node)) {
        //errors handled in method
      }

      //type check and add formals
        node.getFormalList().accept(this);

      //type check and add local vars
        node.getStmtList().accept(this);
        System.out.printf("Method\n");
      exitScope();
      return null; 
    }

    /** Visit a list node of formals
      * @param node the formal list node
      * @return result of the visit 
      * */
  public Object visit(FormalList node) { 
    Iterator<ASTNode> formals = node.getIterator();
    while(formals.hasNext()) {
      Formal formal = (Formal) formals.next();
      String name = formal.getName();
      String type = formal.getType();
      
      //checks if type of formal exists, if not assign as "Object"
      if ((boolean) formal.accept(this)) {  
        type = "Object";
      }

      addVar(name, type);
    }
    return null;
  }

    /** Visit a formal node
      * @param node the formal node
      * @return result of the visit 
      * */
    public Object visit(Formal node) {
      if (typeExists(node.getType())) {
        registerSemanticError(node, "Type: " + node.getType() + " is unknown.");
        return false;
      }

      return true;
    }

    /** Visit a list node of statements
      * @param node the statement list node
      * @return result of the visit 
      * */
    public Object visit(StmtList node) {
        for (Iterator it = node.getIterator(); it.hasNext(); ) {
          Stmt stmt = (Stmt) it.next();
          if ()
          
          stmt.accept(this);
        }
        return null;
    }

    /** Visit a statement node (should never be calle)
      * @param node the statement node
      * @return result of the visit 
      * */
    public Object visit(Stmt node) { 
        throw new RuntimeException("This visitor method should not be called (node is abstract)");
    }

    /** Visit a declaration statement node
      * @param node the declaration statement node
      * @return result of the visit 
      * */
    public Object visit(DeclStmt node) {
            
      if (existsInCurrentVarScope(node.getName())) {
        registerSemanticError(node, errorMessagePrefix(node)+ "Variable name: " + node.getName() + " already declared.");
      } else if (!typeExists(node.getType())){
        if (node.getInit() != null) {
          node.getInit().accept(this);
          System.out.println(node.getInit().getClass().getSimpleName());
          System.out.println(node.getInit().getExprType());
          System.out.println(node.getType());
          if (conformsTo(node.getInit().getExprType(), node.getType())) {
            registerSemanticError(node, errorMessagePrefix(node)+ "Expression type: " + node.getInit().getExprType() + " doesn't conform to var type: " + node.getType() + ".");
          } 
        }
        registerSemanticError(node, errorMessagePrefix(node)+ "Variable type: " + node.getType() + " doesn't exist.");
      } else {
        addVar(node.getName(), node.getType());
      }
      
        return null;
    }

    /** Visit an expression statement node
      * @param node the expression statement node
      * @return result of the visit 
      * */
    public Object visit(ExprStmt node) { 
        node.getExpr().accept(this);
        return null; 
    }

    /** Visit an if statement node
      * @param node the if statement node
      * @return result of the visit 
      * */
    public Object visit(IfStmt node) { 
        node.getPredExpr().accept(this);
  System.out.printf("Then Block\n");
  enterScope();
        node.getThenStmt().accept(this);
  System.out.printf("Then Block\n");
  exitScope();
  
  System.out.printf("Else Block\n");
        enterScope();
  node.getElseStmt().accept(this);
  
  System.out.printf("Else Block\n");
  exitScope();
        return null; 
    }

    /** Visit a while statement node
      * @param node the while statement node
      * @return result of the visit 
      * */
    public Object visit(WhileStmt node) { 
        Expr predExpr = node.getPredExpr();
        predExpr.accept(this);
        if (!predExpr.getExprType().equals(BOOL)) {
            throw new RuntimeException(errorMessagePrefix(node) 
                + "Predicate must be of type boolean, given type: " 
                + predExpr.getExprType() + ".");
        }
        
  System.out.printf("While Block\n");
      enterScope();
            node.getBodyStmt().accept(this);
      
  System.out.printf("While Block\n");
      exitScope();
        return null; 
    }

    /** Visit a for statement node
      * @param node the for statement node
      * @return result of the visit 
      * */
    public Object visit(ForStmt node) { 
        if (node.getInitExpr() != null)
            node.getInitExpr().accept(this);
        if (node.getPredExpr() != null)
            node.getPredExpr().accept(this);
        if (node.getUpdateExpr() != null)
            node.getUpdateExpr().accept(this);
      
  System.out.printf("For Block\n");
      enterScope();
        node.getBodyStmt().accept(this);
  
  System.out.printf("For Block\n");
      exitScope();
        return null; 
    }

    /** Visit a break statement node
      * @param node the break statement node
      * @return result of the visit 
      * */
    public Object visit(BreakStmt node) { 
        return null;
    }

    /** Visit a block statement node
      * @param node the block statement node
      * @return result of the visit 
      * */
    public Object visit(BlockStmt node) { 
      
  System.out.printf("Block \n");
      enterScope();
        node.getStmtList().accept(this);
  
  System.out.printf("Block\n");
  exitScope();
        return null; 
    }

    /** Visit a return statement node
      * @param node the return statement node
      * @return result of the visit 
      * */
    public Object visit(ReturnStmt node) { 
        if (node.getExpr() != null)
            node.getExpr().accept(this);
        return null; 
    }

    /** Visit a list node of expressions
      * @param node the expression list node
      * @return result of the visit 
      * */
    public Object visit(ExprList node) {
        for (Iterator it = node.getIterator(); it.hasNext(); )
            ((Expr)it.next()).accept(this);
        return null;
    }

    /** Visit an expression node (should never be called)
      * @param node the expression node
      * @return result of the visit 
      * */
    public Object visit(Expr node) { 
        throw new RuntimeException("This visitor method should not be called (node is abstract)");
    }

    /** Visit a dispatch expression node
      * @param node the dispatch expression node
      * @return result of the visit 
      * */
    public Object visit(DispatchExpr node) { 
        node.getRefExpr().accept(this);
        node.getActualList().accept(this);
        return null; 
    }

    /** Visit a new expression node
      * @param node the new expression node
      * @return result of the visit 
      * */
    public Object visit(NewExpr node) { 
      node.setExprType(node.getType());
        return null; 
    }

    /** Visit a new array expression node
      * @param node the new array expression node
      * @return result of the visit 
      * */
    public Object visit(NewArrayExpr node) {
      System.out.printf("got to NewArrayExay");
            node.getSize().accept(this);
      node.setExprType(node.getType() + "[]");
        return null; 
    }

    /** Visit an instanceof expression node
      * @param node the instanceof expression node
      * @return result of the visit 
      * */
    public Object visit(InstanceofExpr node) { 
        node.getExpr().accept(this);
        return null; 
    }

    /** Visit a cast expression node
      * @param node the cast expression node
      * @return result of the visit 
      * */
    public Object visit(CastExpr node) { 
        node.getExpr().accept(this);
        return null; 
    }

    /** Visit a binary expression node (should never be called)
      * @param node the binary expression node
      * @return result of the visit 
      * */
    public Object visit(BinaryExpr node) { 
        throw new RuntimeException("This visitor method should not be called (node is abstract)");
    }

    
    /** Visit a unary expression node
      * @param node the unary expression node
      * @return result of the visit 
      * */
    public Object visit(UnaryExpr node) { 
            throw new RuntimeException("This visitor method should not be called (node is abstract)");
    }


    /** Visit an array expression node
      * @param node the array expression node
      * @return result of the visit 
      * */
    public Object visit(ArrayExpr node) { 
        if (node.getRef() != null)
            node.getRef().accept(this);
        node.getIndex().accept(this);
        return null; 
    }

    /** Visit a constant expression node (should never be called)
      * @param node the constant expression node
      * @return result of the visit 
      * */
    public Object visit(ConstExpr node) { 
            throw new RuntimeException("This visitor method should not be called (node is abstract)");
    }


}
