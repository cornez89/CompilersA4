package semant;

import java.util.Iterator;

import ast.ASTNode;
import ast.ArrayAssignExpr;
import ast.ArrayExpr;
import ast.AssignExpr;
import ast.BinaryArithDivideExpr;
import ast.BinaryArithMinusExpr;
import ast.BinaryArithModulusExpr;
import ast.BinaryArithPlusExpr;
import ast.BinaryArithTimesExpr;
import ast.BinaryCompEqExpr;
import ast.BinaryCompGeqExpr;
import ast.BinaryCompGtExpr;
import ast.BinaryCompLeqExpr;
import ast.BinaryCompLtExpr;
import ast.BinaryCompNeExpr;
import ast.BinaryExpr;
import ast.BinaryLogicAndExpr;
import ast.BinaryLogicOrExpr;
import ast.BlockStmt;
import ast.BreakStmt;
import ast.CastExpr;
import ast.Class_;
import ast.ConstBooleanExpr;
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

public class TypeCheckVisitor extends SemantVisitor {

    public TypeCheckVisitor(ClassTreeNode classTreeNode, ErrorHandler errorHandler) {
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
     * 
     * Completed Nodes
     * 
     * 
     * 
     */

    public Object visit(ast.Field node) {
        if (node.getInit() != null) {
            Expr initExpr = node.getInit();
            initExpr.accept(this);

            if (!typeExists(node.getType())) {
                registerSemanticError(node,
                        "type '" + node.getType() + "' of field '" + node.getName() + "' is undefined");
            }

            if (initExpr.getExprType().equals(VOID)) {
                registerSemanticError(node, "cannot return an expression of type '" + VOID + "' from a method");
            }

            // Two errors in one
            // checks if primitives match
            // checks if references types conform
            if (!conformsTo(initExpr.getExprType(), node.getType())) {
                registerSemanticError(node,
                        "expression type '" + initExpr.getExprType() + "' of field '" + node.getName()
                                + "' does not conform to declared type '" + node.getType() + "'");
            }

        }

        return null;
    }

    /**
     * Visit a method node
     * 
     * @param node the method node
     * @return result of the visit
     */
    public Object visit(Method node) {
        enterScope();

        // type check and add formals
        node.getFormalList().accept(this);

        // type check and add local vars
        node.getStmtList().accept(this);

        // check that return types conform
        ReturnStmt returnStmt = null;
        String returnType = "void";
        Iterator<ASTNode> bodyStmts = node.getStmtList().getIterator();

        // check that return stmt should be the last stmt
        while (bodyStmts.hasNext()) {
            Stmt stmt = (Stmt) bodyStmts.next();
            if (stmt instanceof ReturnStmt) {
                returnStmt = (ReturnStmt) stmt;
                returnType = (String) returnStmt.accept(this);
                break;
            }
        }

        // check that there are no expressions after the returnStmt
        if (bodyStmts.hasNext()) {
            registerSemanticError(node, "Unreachable statement of type '" +
                    bodyStmts.next().getClass().getSimpleName() + "' after return statement");
        }

        // check that return type matches the actual return statement
        if (!conformsTo(returnType, node.getReturnType())) {
            registerSemanticError(node, "return type '" + returnType + "' is not compatible with declared return type '"
                    + node.getReturnType() + "' in method '" + node.getName() + "'");
        }

        exitScope();
        return null;
    }

    /**
     * Visit a list node of formals
     * 
     * @param node the formal list node
     * @return result of the visit
     */
    public Object visit(FormalList node) {
        Iterator<ASTNode> formals = node.getIterator();
        while (formals.hasNext()) {
            Formal formal = (Formal) formals.next();
            String name = formal.getName();
            String type = formal.getType();

            // checks if type of formal exists, if not assign as "Object"
            type = (String) formal.accept(this);
            addVar(name, type);

        }
        return null;
    }

    /**
     * Visit a formal node
     * 
     * @param node the formal node
     * @return result of the visit
     */
    public Object visit(Formal node) {
        String type = node.getType();
        String name = node.getName();

        type = (String) checkFormal(name, type, node, "formal");

        return type;
    }

    protected Object checkFormal(String name, String type, ASTNode node, String nodeType) {
        if (!typeExists(type)) {
            registerSemanticError(node, "type '" + type + "' of " + nodeType + " '" + name + "' is undefined");
            type = "Object";
        }

        if (isReserved(name)) {
            registerSemanticError(node, nodeType + "s cannot be named '" + name + "'");
        }

        if (existsInCurrentVarScope(name)) {
            registerSemanticError(node, nodeType + " '" + name + "' is multiply defined");
        }

        return type;
    }

    /**
     * Visit a list node of statements
     * 
     * @param node the statement list node
     * @return result of the visit
     */
    public Object visit(StmtList node) {
        for (Iterator<ASTNode> it = node.getIterator(); it.hasNext();) {
            Stmt stmt = (Stmt) it.next();
            stmt.accept(this);
        }
        return null;
    }

    /**
     * Visit a declaration statement node
     * 
     * @param node the declaration statement node
     * @return result of the visit
     */
    public Object visit(DeclStmt node) {
        // same as formal
        String declaredType = node.getType();
        String name = node.getName();
        declaredType = (String) checkFormal(name, declaredType, node, "declaration");

        // check that init type conforms to declared type
        node.getInit().accept(this);
        String type = node.getInit().getExprType();
        if (!conformsTo(type, declaredType)) {
            registerSemanticError(node, "expression type '" + type + "' of declaration '" + node.getName()
                    + "' does not match declared type '" + declaredType + "'");
        } else {
            addVar(name, type);
        }

        return null;
    }

    /**
     * Visit an expression statement node
     * 
     * @param node the expression statement node
     * @return result of the visit
     */
    public Object visit(ExprStmt node) {

        // type check expression
        node.getExpr().accept(this);
        Expr expr = node.getExpr();
        // Only valid ExprStmt are assignment, new, dispatch and unary
        if (!(expr instanceof AssignExpr ||
                expr instanceof ArrayAssignExpr ||
                expr instanceof NewExpr ||
                expr instanceof NewArrayExpr ||
                expr instanceof DispatchExpr ||
                expr instanceof UnaryIncrExpr ||
                expr instanceof UnaryDecrExpr)) {
            registerSemanticError(node, "not a statement");
        }
        return null;
    }

    /**
     * Visit an assignment expression node
     * 
     * @param node the assignment expression node
     * @return result of the visit
     */
    public Object visit(AssignExpr node) {

        node.getExpr().accept(this);

        String name = node.getName();
        String ref = node.getRefName();
        String declaredType = checkTypeOfAssignment(name, ref, node);

        // check that expr type conforms to the type of the variable
        String exprType = node.getExpr().getExprType();
        if (!conformsTo(exprType, declaredType)) {
            registerSemanticError(node, "the lefthand type '" + declaredType +
                    "' and righthand type '" + exprType + "' are not compatible in assignment");
        }

        node.setExprType(declaredType);

        return null;
    }

    protected String checkTypeOfAssignment(String name, String ref, ASTNode node) {

        String declaredType = (String) lookupVar(name);
        // Check that lefthand side is declared
        if (declaredType == null) {
            registerSemanticError(node, "the lefthand variable '" + name + "' must be declared");
        }

        // check that ref is valid
        if (ref == null) {
            declaredType = (String) lookupVar(name);
            // lookup from current scope
        } else if (ref.equals(THIS)) {
            declaredType = (String) thisLookupVar(name);
            // lookup from first scope of curr class
        } else if (ref.equals(SUPER)) {
            declaredType = (String) superLookupVar(name);
            // lookup from first scope in super class
        } else {
            registerSemanticError(node, "bad reference '" + ref
                    + "': fields are 'protected' and can only be accessed within the class or subclass via 'this' or 'super'");
        }

        return declaredType;
    }

    /**
     * Visit an array assignment expression node
     * 
     * @param node the array assignment expression node
     * @return result of the visit
     */
    public Object visit(ArrayAssignExpr node) {

        // check that index returns an int
        node.getIndex().accept(this);
        String indexType = node.getIndex().getExprType();
        if (!indexType.equals(INT)) {
            registerSemanticError(node, "invalid index expression of type '" +
                    indexType + "' expression must be type 'int'");
        }

        // Check that the var is defined
        String name = node.getName();

        String ref = node.getRefName();
        String declaredType = checkTypeOfAssignment(name, ref, node);
        declaredType = declaredType.substring(0, declaredType.length() - 2);

        // check that return type of expr conforms to type of array
        node.getExpr().accept(this);
        String exprType = node.getExpr().getExprType();

        if (!conformsTo(exprType, declaredType.substring(0))) {
            registerSemanticError(node, "the lefthand type '" + declaredType +
                    "' and righthand type '" + exprType + "' are not compatible in assignment");
        }

        node.setExprType(declaredType);

        return null;
    }

    /**
     * Visit a new expression node
     * 
     * @param node the new expression node
     * @return result of the visit
     */
    public Object visit(NewExpr node) {
        // Check that type exists
        String type = node.getType();
        if (!typeExists(type)) {
            registerSemanticError(node, "type '" + type + "' of new construction is undefined");
            node.setExprType(OBJECT);
        } else if (isPrimitive(type)) {
            registerSemanticError(node,
                    "type '" + type + "' of new construction is primitive and cannot be constructed");
            node.setExprType(VOID);
        } else {
            node.setExprType(type);
        }
        return null;
    }

    /**
     * Visit a dispatch expression node
     * 
     * @param node the dispatch expression node
     * @return result of the visit
     */
    public Object visit(DispatchExpr node) {
        // type check reference expression
        node.getRefExpr().accept(this);
        Expr refExpr = node.getRefExpr();

        // not sure if this is right
        Method method = (Method) lookupMethodInClass(refExpr.getExprType(), node.getMethodName());

        // check if method exists
        if (method == null) {
            registerSemanticError(node, "dispatch to unknown method '" + node.getMethodName() + "'");
            return null;
        }

        // type check formals
        node.getActualList().accept(this);
        Iterator<ASTNode> arguments = node.getActualList().getIterator();
        if (method.getFormalList() == null) {
        }
        Iterator<ASTNode> formals = method.getFormalList().getIterator();
        int numOfFormals = method.getFormalList().getSize();
        int numOfArgs = node.getActualList().getSize();
        if (numOfArgs != numOfFormals) {
            registerSemanticError(node, "number of actual parameters (" + numOfArgs +
                    ") differs from number of formal parameters (" + numOfFormals +
                    ") in dispatch to method '" + method.getName() + "'");
        }

        // check that formals match in type and args aren't void
        int i = 1;
        while (arguments.hasNext() && formals.hasNext()) {
            Expr arg = (Expr) arguments.next();
            Formal formal = (Formal) formals.next();
            if (arg.getExprType().equals(VOID)) {
                registerSemanticError(node, "actual parameter " + i +
                        " in the call to method " + method.getName() +
                        " is void and cannot be used within an expression");
            } else if (!conformsTo(arg.getExprType(), formal.getType())) {
                registerSemanticError(node,
                        "actual parameter " + i + " with type '" + arg.getExprType()
                                + "' does not match formal parameter " + i
                                + " with declared type '" + formal.getType() + "' in dispatch to method '"
                                + method.getName() + "'");
            }
        }

        node.setExprType(method.getReturnType());
        return null;
    }

    /**
     * Visit a list node of expressions
     * 
     * @param node the expression list node
     * @return result of the visit
     */
    public Object visit(ExprList node) {
        for (Iterator it = node.getIterator(); it.hasNext();)
            ((Expr) it.next()).accept(this);
        return null;
    }

    /**
     * Visit a list node of classes
     * 
     * @param node the class list node
     * @return result of the visit
     */

    /**
     * Visit a unary increment expression node
     * 
     * @param node the unary increment expression node
     * @return result of the visit
     */

    protected Object typeCheckUnary(UnaryExpr unaryExpr, String type) {

        if (!type.equals(unaryExpr.getOperandType())) {
            registerSemanticError(unaryExpr, "the expression type '" + type + "' in the unary operation ('"
                    + unaryExpr.getOpName() + "') is incorrect; should have been: " + unaryExpr.getOperandType());
        }
        unaryExpr.setExprType(unaryExpr.getOpType());
        return null;
    }

    public Object visit(UnaryIncrExpr node) {
        node.getExpr().accept(this);
        String type = node.getExpr().getExprType();
        typeCheckUnary(node, type);

        return null;
    }

    /**
     * Visit a unary decrement expression node
     * 
     * @param node the unary decrement expression node
     * @return result of the visit
     */
    public Object visit(UnaryDecrExpr node) {
        node.getExpr().accept(this);
        String type = node.getExpr().getExprType();
        typeCheckUnary(node, type);

        return null;
    }

    /**
     * Visit a unary negation expression node
     * 
     * @param node the unary negation expression node
     * @return result of the visit
     */
    public Object visit(UnaryNegExpr node) {
        node.getExpr().accept(this);
        String type = node.getExpr().getExprType();
        typeCheckUnary(node, type);

        return null;
    }

    /**
     * Visit a unary NOT expression node
     * 
     * @param node the unary NOT expression node
     * @return result of the visit
     */
    public Object visit(UnaryNotExpr node) {
        node.getExpr().accept(this);
        String type = node.getExpr().getExprType();
        typeCheckUnary(node, type);

        return null;
    }

    /**
     * Visit an if statement node
     * 
     * @param node the if statement node
     * @return result of the visit
     */
    public Object visit(IfStmt node) {
        node.getPredExpr().accept(this);
        String predType = node.getPredExpr().getExprType();
        if (!predType.equals(BOOL)) {
            registerSemanticError(node, "predicate in if-statement does not have type boolean");
        }
        enterScope();
        node.getThenStmt().accept(this);

        exitScope();

        enterScope();
        node.getElseStmt().accept(this);

        exitScope();
        return null;
    }

    /**
     * Visit a while statement node
     * 
     * @param node the while statement node
     * @return result of the visit
     */
    public Object visit(WhileStmt node) {
        Expr predExpr = node.getPredExpr();
        predExpr.accept(this);
        if (!predExpr.getExprType().equals(BOOL)) {
            registerSemanticError(node, "predicate in while-statement does not have type boolean");
        }

        enterScope();
        node.getBodyStmt().accept(this);

        exitScope();
        return null;
    }

    /**
     * Visit a for statement node
     * 
     * @param node the for statement node
     * @return result of the visit
     */
    public Object visit(ForStmt node) {

        if (node.getInitExpr() != null)
            node.getInitExpr().accept(this);
        if (node.getPredExpr() != null) {
            node.getPredExpr().accept(this);
            if (!node.getPredExpr().getExprType().equals(BOOL)) {
                registerSemanticError(node, "predicate in while-statement does not have type boolean");
            }
        }
        if (node.getUpdateExpr() != null) {
            node.getUpdateExpr().accept(this);
        }

        enterScope();
        node.getBodyStmt().accept(this);

        exitScope();
        return null;
    }

    /**
     * Visit a block statement node
     * 
     * @param node the block statement node
     * @return result of the visit
     */
    public Object visit(BlockStmt node) {

        enterScope();
        node.getStmtList().accept(this);
        exitScope();
        return null;
    }

    /**
     * Visit a return statement node
     * 
     * @param node the return statement node
     * @return result of the visit
     */
    public Object visit(ReturnStmt node) {
        if (node.getExpr() != null) {
            node.getExpr().accept(this);
            return node.getExpr().getExprType();
        }

        return VOID;
    }

    public Object visit(ClassTreeNode classTreeNode) {

        Iterator<ClassTreeNode> children = classTreeNode.getChildrenList();
        if (!classTreeNode.isBuiltIn())
            classTreeNode.getASTNode().accept(this);

        while (children.hasNext()) {

            ClassTreeNode child = children.next();
            child.getVarSymbolTable().setParent(classTreeNode.getVarSymbolTable());
            child.getMethodSymbolTable().setParent(classTreeNode.getMethodSymbolTable());
            super.classTreeNode = child;
            visit(child);
        }
        return null;
    }

    /**
     * Visit a class node
     * 
     * @param node the class node
     * @return result of the visit
     */
    public Object visit(Class_ node) {
        node.getMemberList().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison expression node (should never be called)
     * 
     * @param node the binary comparison expression node
     * @return result of the visit
     */
    public Object binaryExpr(BinaryExpr node) {
        // check that both operands conform to each other

        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        String leftType = node.getLeftExpr().getExprType();
        String rightType = node.getRightExpr().getExprType();

        if (node.getOperandType() != null) {
            if (!leftType.equals(node.getOperandType())) {
                registerSemanticError(node,
                        "the lefthand type '" + leftType + "' in the binary operation ('"
                                + node.getOpName() + "') is incorrect; should have been: " + node.getOperandType());
            }
            if (!rightType.equals(node.getOperandType())) {
                registerSemanticError(node,
                        "the righthand type '" + rightType + "' in the binary operation ('"
                                + node.getOpName() + "') is incorrect; should have been: " + node.getOperandType());
            }
        } else {
            if (!conformsTo(leftType, rightType) &&
                    !conformsTo(rightType, leftType)) {
                registerSemanticError(node,
                        "the lefthand type '" + leftType + "' in the binary operation ('"
                                + node.getOpName() + "') does not match the righthand type '" + rightType
                                + "'");
            }
        }

        node.setExprType(node.getOpType());
        return null;
    }

    /**
     * Visit a binary comparison equals expression node
     * 
     * @param node the binary comparison equals expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompEqExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary comparison not equals expression node
     * 
     * @param node the binary comparison not equals expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompNeExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary comparison less than expression node
     * 
     * @param node the binary comparison less than expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompLtExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary comparison less than or equal to expression node
     * 
     * @param node the binary comparison less than or equal to expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompLeqExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary comparison greater than expression node
     * 
     * @param node the binary comparison greater than expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompGtExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary comparison greater than or equal to expression node
     * 
     * @param node the binary comparison greater to or equal to expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompGeqExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary arithmetic plus expression node
     * 
     * @param node the binary arithmetic plus expression node
     * @return result of the visit
     */
    public Object visit(BinaryArithPlusExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary arithmetic minus expression node
     * 
     * @param node the binary arithmetic minus expression node
     * @return result of the visit
     */
    public Object visit(BinaryArithMinusExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary arithmetic times expression node
     * 
     * @param node the binary arithmetic times expression node
     * @return result of the visit
     */
    public Object visit(BinaryArithTimesExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary arithmetic divide expression node
     * 
     * @param node the binary arithmetic divide expression node
     * @return result of the visit
     */
    public Object visit(BinaryArithDivideExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary arithmetic modulus expression node
     * 
     * @param node the binary arithmetic modulus expression node
     * @return result of the visit
     */
    public Object visit(BinaryArithModulusExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary logical AND expression node
     * 
     * @param node the binary logical AND expression node
     * @return result of the visit
     */
    public Object visit(BinaryLogicAndExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a binary logical OR expression node
     * 
     * @param node the binary logical OR expression node
     * @return result of the visit
     */
    public Object visit(BinaryLogicOrExpr node) {
        binaryExpr(node);
        return null;
    }

    /**
     * Visit a new array expression node
     * 
     * @param node the new array expression node
     * @return result of the visit
     */
    public Object visit(NewArrayExpr node) {
        // type check size expr
        node.getSize().accept(this);
        String sizeType = node.getSize().getExprType();
        String type = node.getType();
        if (!sizeType.equals(INT)) {
            registerSemanticError(node, "size expression of type '" + sizeType + "' is not type '" + INT + "'");
        }

        if (!typeExists(type)) {
            registerSemanticError(node, "type '" + type + "' of array is undefined");
        }

        node.setExprType(node.getType() + "[]");
        return null;
    }

    /**
     * Visit an instanceof expression node
     * 
     * @param node the instanceof expression node
     * @return result of the visit
     */
    public Object visit(InstanceofExpr node) {
        // type check expr
        node.getExpr().accept(this);

        String type = node.getType();
        String exprType = node.getExpr().getExprType();
        // check if expr type is known
        if (typeExists(type)) {
            registerSemanticError(node, "the instanceof righthand type '" + type + "' is undefined");
        }

        if (isPrimitive(exprType)) {
            registerSemanticError(node,
                    "the instanceof lefthand expression has type '" + type
                            + "', which is primitive and not an object type");
        }

        if (exprType.equals(VOID)) {
            registerSemanticError(node, "the instanceof righthand type cannot be type '" + VOID + "'");
        }

        node.setExprType(BOOL);
        return BOOL;
    }

    /**
     * Visit a cast expression node
     * 
     * @param node the cast expression node
     * @return result of the visit
     */
    public Object visit(CastExpr node) {
        String castType;

        // check if cast type exists
        castType = node.getType();
        if (!typeExists(castType)) {
            registerSemanticError(node, "type '" + castType + "' of cast is undefined");
            castType = "Object";
        }

        if (isPrimitive(castType)) {
            registerSemanticError(node,
                    "expression in cast has type '" + castType + "', which is primitive and can't be casted");
            castType = "Object";
        }

        // type check expression
        node.getExpr().accept(this);
        String currType = node.getExpr().getExprType();

        if (conformsTo(currType, castType)) { // upcast
            node.setUpCast(true);
            node.setExprType(castType);
        } else if (conformsTo(castType, currType)) { // downcast
            node.setUpCast(false);
            node.setExprType(castType);
        } else {
            registerSemanticError(node, "invalid cast of type '" + currType + "' into type '" + castType + "'");
            castType = "Object";
        }

        return castType;
    }

    public String getTypeOfVarExp(Expr refExpr, String name, ASTNode node) {
        String type = null;

        if (refExpr != null) {
            // check if refExpr is 'this' or 'super'
            if (refExpr instanceof VarExpr) {
                String refName = ((VarExpr) refExpr).getName();
                if (refName.equals(THIS)) {
                    type = (String) thisLookupVar(name);
                } else if (refName.equals(SUPER)) {
                    type = (String) superLookupVar(name);
                } else {
                    String refType = refExpr.getExprType();
                    if (!typeExists(refType)) {
                        registerSemanticError(node, "type '" + refType + "' is undefined for variable expression");
                        type = OBJECT;
                    } else if (((String) lookupVar(refName)).endsWith("]")) {
                        if (!name.equals("length")) {
                            registerSemanticError(node, "bad reference to '" + name + "': arrays do not have this field (they only have a 'length' field)");
                            type = OBJECT;
                        } else {
                            type = INT;
                        }
                    } else {
                        registerSemanticError(node, "bad reference '" + name + "': fields are 'protected' and can only be accessed within the class or subclass via 'this' or 'super'");
                        type = OBJECT;
                    }
                }
            } else {
                String refType = refExpr.getExprType();
                if (!typeExists(refType)) {
                    registerSemanticError(node, "type '" + refType + "' is undefined for variable expression");
                    type = (String) lookupMethodInClass(refType, name);
                } else {
                    type = OBJECT;
                }
            }
        } else {// ref is null
            if (name.equals(THIS)) {
                type = classTreeNode.getName();
            } else if (name.equals(SUPER)) {
                type = classTreeNode.getParent().getName();
            } else if (name.equals(NULL)) {
                type = OBJECT;
            } else {
                type = (String) lookupVar(name);
                if (type == null) {
                    registerSemanticError(node, "type '" + type + "' is undefined for variable expression");
                    type = OBJECT;
                }
            }
        }
        if (type == null) {
            registerSemanticError(node, "var in getTypeOfVarExp is null");
            type = OBJECT;
        }

        return type;
    }

    /**
     * Visit a variable expression node
     * 
     * @param node the variable expression node
     * @return result of the visit
     */
    public Object visit(VarExpr node) {

        String name = node.getName();

        Expr refExpr = node.getRef();

        // type check reference expression
        if (refExpr != null)
            refExpr.accept(this);

        node.setExprType(getTypeOfVarExp(refExpr, name, node));
        return null;
    }

    public Object visit(ConstIntExpr node) {
        node.setExprType(INT);
        return null;
    }

    /**
     * Visit an array expression node
     * 
     * @param node the array expression node
     * @return result of the visit
     */
    public Object visit(ArrayExpr node) {
        Expr refExpr = node.getRef();
        String name = node.getName();
        String indexType;
        if (refExpr != null)
            refExpr.accept(this);

        String baseType = getTypeOfVarExp(refExpr, name, node);
        baseType = baseType.substring(0, baseType.length() -2);

        node.setExprType(baseType);

        node.getIndex().accept(this);
        indexType = node.getIndex().getExprType();
        if (!indexType.equals(INT)) {
            registerSemanticError(node, "invalid index expression of type '" +
                    indexType + "' expression must be type 'int'");
        }
        return null;
    }

    /**
     * Visit a boolean constant expression node
     * 
     * @param node the boolean constant expression node
     * @return result of the visit
     */
    public Object visit(ConstBooleanExpr node) {
        node.setExprType(BOOL);
        return null;
    }

    /**
     * Visit a string constant expression node
     * 
     * @param node the string constant expression node
     * @return result of the visit
     */
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

    /**
     * Visit a break statement node
     * 
     * @param node the break statement node
     * @return result of the visit
     */
    public Object visit(BreakStmt node) {
        return null;
    }

}
