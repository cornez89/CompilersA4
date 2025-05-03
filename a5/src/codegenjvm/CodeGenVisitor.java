package codegenjvm;

import visitor.Visitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ast.*;
import parser.JavaCharStream;
import semant.SemantVisitor;
import util.ClassTreeNode;

public class CodeGenVisitor extends Visitor {

    PrintWriter out;
    public static String rootFilePath = "java/lang/";
    private int labelCount = 1;
    private int currStackSize = 0;
    private int currLocalSize = 1;
    private ClassTreeNode classTreeNode;

    Map<String, Integer> variableToLocalIndex = new HashMap<String, Integer>();

    /*
     * 
     * Helper Methods
     * 
     */
    private void print(String string) {
        System.out.print(string);
    }

    private void println(String string) {
        System.out.println(string);
    }

    private void printBytecode(String bytecode) {
        // scan bytecode for special characters like \n and \\
        // check all two character strings
        int i = 0;
        while (i < bytecode.length() - 1) {
            char first = bytecode.charAt(i);
            char second = bytecode.charAt(i + 1);

            // handle illegal escape sequences
            if (first == '\\') {
                switch (second) {
                    case '"':
                    case 'n':
                    case 'f':
                    case 'r':
                    case 't':
                    case '\\':
                        bytecode = bytecode.substring(0, i) + "\\" + second
                                + bytecode.substring(i + 2);
                        i += 2;
                    default:
                        // invalid escape sequence, shouln't happen
                        throw new RuntimeException(
                                "Error: Illegal escape sequence of \\ followed by:"
                                        + second);
                }
            }
            i++;
        }
        out.println("    " + bytecode);
    }

    private void printComment(String comment) {
        out.println("    ;" + comment);
    }

    /**
     * 
     * Bytecode Methods
     * 
     */

    private void ldc(String constant) {
        printBytecode("ldc " + constant);
        currStackSize++;
        // net stack size + 1
    }

    private void iconst(int number) {
        if (number < -(2 << 16))
            printBytecode("ldc " + number);
        else if (number < -(2 << 8))
            printBytecode("bipush " + number);
        else if (number < -1)
            printBytecode("sipush " + number);
        else if (number == -1)
            printBytecode("iconst_m1");
        else if (number < 6)
            printBytecode("iconst_" + number);
        else if (number < (2 << 8))
            printBytecode("sipush " + number);
        else if (number < (2 << 16))
            printBytecode("bipush " + number);
        else
            printBytecode("ldc " + number);
        currStackSize++;
        // net stack size + 1
    }

    private void iload(int index) {
        if (index < 4)
            printBytecode("iload_" + index);
        else
            printBytecode("iload " + index);
        currStackSize++;
        // net stack size + 1
    }

    private void istore(int index) {
        if (currStackSize <= 0)
            throw new RuntimeException("Error: popped from an empty stack");
        if (index < 4)
            printBytecode("istore_" + index);
        else
            printBytecode("istore " + index);
        currStackSize--;
        // net stack size - 1
    }

    private void aload(int index) {
        if (index == 0)
            printBytecode("aload_" + index);
        else
            printBytecode("aload " + index);
        currStackSize++;
        // net stack size + 1
    }

    private void astore(int index) {
        if (currStackSize <= 0)
            throw new RuntimeException("Error: popped from an empty stack");
        if (index == 0)
            printBytecode("astore_" + index);
        else
            printBytecode("astore " + index);
        currStackSize--;
        // net stack size - 1
    }

    private void pop() {
        if (currStackSize <= 0)
            throw new RuntimeException("Error: popped from an empty stack");
        printBytecode("pop");
        currStackSize--;
        // net stack size - 1
    }

    private void dup() {
        if (currStackSize <= 0)
            throw new RuntimeException("Error: Nothing to dup");
        printBytecode("dup");
        currStackSize++;
        // net stack size + 1
    }

    private void putStatic(ClassTreeNode classTreeNode, String fieldName,
            String descriptor) {
        aload(0);
        printBytecode("putfield " + classTreeNode.getName() + "/" + fieldName
                + " " + descriptor);
        currStackSize--;
        currStackSize--;
        // net stack size - 1
    }

    private void getStatic(ClassTreeNode classTreeNode, String fieldName,
            String descriptor) {
        aload(0);
        printBytecode("getstatic " + classTreeNode.getName() + "/" + fieldName
                + " " + descriptor);
        // net stack size + 1
    }

    private void putField(int indexOfObjectRef, String field) {
        aload(indexOfObjectRef);
        printBytecode("putfield " + field);
        currStackSize--;
        currStackSize--;
        // net stack size + 1
    }

    private void getField(int indexOfObjectRef, String field) {
        aload(indexOfObjectRef);
        printBytecode("getfield " + field);
        // net stack size - 1
    }

    private void newClass(String className) {
        printBytecode("new " + className);
        currStackSize++;
        // net stack size + 1
    }

    private void callSuper() {
        aload(0);
        printBytecode("invokespecial " + rootFilePath + "/<init>()V");
        currStackSize--;
        // net stack size =
    }

    private void returnStmt() {
        printBytecode("return");
    }

    /**
     * 
     * Other Helper Methods
     * 
     */
    private void addLimits(int[] temp, int[] orig) {
        orig[0] += temp[0];
        orig[1] += temp[1];
    }

    private String getDesciptor(String type) {
        switch (type) {
            case "int":
                return "I";
            case "boolean":
                return "Z";
            case "void":
                return "V";
            case "String":
                return "Ljava/lang/String";
            default:
                return getFilePath(classTreeNode.lookupClass(type),
                        "Ljava/lang/");
        }
    }

    void initializeFields(ArrayList<Field> fields) {
        if (fields.isEmpty()) {
            aload(0);
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                Expr init = field.getInit();

                // Assign default values if non exist
                if (init != null) {
                    init.accept(this);
                } else {

                    // assign default values
                    switch (getDesciptor(field.getType())) {
                        case "I":
                            iconst(0);
                        case "Z":
                            break;
                        case "S":
                        default:
                            printBytecode("aconst_null");
                    }
                }

                putStatic(classTreeNode, field.getName(),
                        getDesciptor(field.getType()));
            }
        }
    }

    /**
     * Visit an AST node (should never be called)
     * 
     * @param node
     *        the AST node
     * @return result of the visit
     */
    public Object visit(ASTNode node) {
        throw new RuntimeException(
                "This visitor method should not be called (node is abstract)");
    }

    /**
     * Visit a list node (should never be called)
     * 
     * @param node
     *        the list node
     * @return result of the visit
     */
    public Object visit(ListNode node) {
        throw new RuntimeException(
                "This visitor method should not be called (node is abstract)");
    }

    /**
     * Visit a program node
     * 
     * @param node
     *        the program node
     * @return result of the visit
     */
    public Object visit(Program node) {
        throw new RuntimeException("This visitor method should not be called");
    }

    /**
     * Visit a list node of classes
     * 
     * @param node
     *        the class list node
     * @return result of the visit
     */
    public Object visit(ClassList node) {
        throw new RuntimeException("This visitor method should not be called");
    }

    /**
     * Visit a class node
     * 
     * @param node
     *        the class node
     * @return result of the visit
     */
    public Object visit(ClassTreeNode node) {
        println(node.toString());
        classTreeNode = node;
        rootFilePath = getFilePath(node.getParent(), rootFilePath);

        classTreeNode.getASTNode().accept(this);
        return null;
    }

    String getFilePath(ClassTreeNode classNode, String filePath) {
        if (classNode == null)
            return filePath;
        if (classNode.getParent() == null) {
            return classNode.getName();
        } else {
            return filePath + getFilePath(classNode.getParent(), filePath) + "/"
                    + classNode.getName();
        }
    }

    public Object visit(Class_ node) {
        System.out.println("here");

        try {
            out = new PrintWriter(new File(node.getName() + ".j"));
            System.out.println("here");
            // print top of file info
            out.println(".source " + node.getFilename());
            out.println(".class " + "public " + node.getName());

            if (node.getParent() != null) {
                out.println(".super " + rootFilePath);
            }

            out.println(".implements " + "java/lang/" + "Clonable");
            out.println();

            // do all fields, then constructor, then do all methods
            ArrayList<Field> fields = new ArrayList<>();
            ArrayList<Method> methods = new ArrayList<>();
            Iterator<ASTNode> members = node.getMemberList().getIterator();
            int stackLimit = 0;
            int localLimit = 1; // For the object reference

            while (members.hasNext()) {
                Member member = (Member) members.next();
                if (member instanceof Field)
                    fields.add((Field) member);
                else
                    methods.add((Method) member);
            }

            // need 1 spot for reference
            if (fields.size() > 0 || node.getParent() != null)
                stackLimit++;

            // fields
            for (int i = 0; i < fields.size(); i++)
                out.println(".field " + "protected " + fields.get(i)
                        .getName() + getDesciptor(fields.get(i).getType()));
            out.println();

            // write constructor
            out.println(".method " + "public " + "<init>()V");
            printBytecode(".limit " + "stack " + stackLimit);
            printBytecode(".limit " + "locals " + 1);
            callSuper();
            initializeFields(fields);
            printBytecode("return");
            out.println(".end method");
            out.println();

            // methods
            while (!methods.isEmpty())
                methods.removeFirst().accept(this);
            out.println();

            // class init
            out.println(".method " + "public " + "<clinit>()V");
            initializeFields(fields);
            printBytecode("return");
            out.println(".end Method");

            out.close();
        } catch (FileNotFoundException fnfe) {
            System.out.println(fnfe.getMessage());
        }
        return null;
    }

    /**
     * Visit a list node of members
     * 
     * @param node
     *        the member list node
     * @return result of the visit
     */
    public Object visit(MemberList node) {
        for (Iterator it = node.getIterator(); it.hasNext();)
            ((Member) it.next()).accept(this);
        return null;
    }

    /**
     * Visit a member node (should never be calle)
     * 
     * @param node
     *        the member node
     * @return result of the visit
     */
    public Object visit(Member node) {
        throw new RuntimeException(
                "This visitor method should not be called (node is abstract)");

    }

    /**
     * Visit a field node
     * 
     * @param node
     *        the field node
     * @return result of the visit
     */
    public Object visit(Field node) {
        if (node.getInit() != null)
            node.getInit().accept(this);
        return null;
    }

    /**
     * Visit a method node
     * 
     * @param node
     *        the method node
     * @return result of the visit
     */
    public Object visit(Method node) {
        int[] limits = { 0, 0 };

        classTreeNode.getVarSymbolTable().enterScope();
        // print method signature
        out.print(".method " + "public " + node.getName() + "(");

        addLimits((int[]) node.getFormalList().accept(this), limits);

        out.print(")" + getDesciptor(node.getReturnType()));
        out.println();
        out.println(".throws java/lang/CloneNotSupportedException");

        // return their max stack and local limit
        node.getStmtList().accept(this);

        // go back and print limits

        out.println(".end method");
        classTreeNode.getVarSymbolTable().exitScope();
        return null;
    }

    /**
     * Visit a list node of formals
     * 
     * @param node
     *        the formal list node
     * @return result of the visit
     */
    public Object visit(FormalList node) {
        int[] limits = { 0, 0 };
        for (Iterator it = node.getIterator(); it.hasNext();) {
            addLimits((int[]) ((Formal) it.next()).accept(this), limits);
        }
        return limits;
    }

    /**
     * Visit a formal node
     * 
     * @param node
     *        the formal node
     * @return result of the visit
     */
    public Object visit(Formal node) {
        int[] limits = { 0, 1 };
        classTreeNode.getVarSymbolTable().add(node.getName(), currLocalSize);

        // output descriptor
        String type = getDesciptor(node.getType());
        out.print(type + ";");
        return limits;
    }

    /**
     * Visit a list node of statements
     * 
     * @param node
     *        the statement list node
     * @return result of the visit
     */
    public Object visit(StmtList node) {
        for (Iterator it = node.getIterator(); it.hasNext();)
            ((Stmt) it.next()).accept(this);
        return null;
    }

    /**
     * Visit a statement node (should never be calle)
     * 
     * @param node
     *        the statement node
     * @return result of the visit
     */
    public Object visit(Stmt node) {
        throw new RuntimeException(
                "This visitor method should not be called (node is abstract)");
    }

    /**
     * Visit a declaration statement node
     * 
     * @param node
     *        the declaration statement node
     * @return result of the visit
     */
    public Object visit(DeclStmt node) {
        printComment("Declaration " + node.getName() + " : " + node.getType());
        classTreeNode.getVarSymbolTable().add(node.getName(), currLocalSize);

        // should push a value onto the stack
        node.getInit().accept(this);
        if (SemantVisitor.isPrimitive(node.getType())) {
            istore(currLocalSize);
        }

        return null;
    }

    /**
     * Visit an expression statement node
     * 
     * @param node
     *        the expression statement node
     * @return result of the visit
     */
    public Object visit(ExprStmt node) {
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit an if statement node
     * 
     * @param node
     *        the if statement node
     * @return result of the visit
     */
    public Object visit(IfStmt node) {
        node.getPredExpr().accept(this);
        node.getThenStmt().accept(this);
        node.getElseStmt().accept(this);
        return null;
    }

    /**
     * Visit a while statement node
     * 
     * @param node
     *        the while statement node
     * @return result of the visit
     */
    public Object visit(WhileStmt node) {
        node.getPredExpr().accept(this);
        node.getBodyStmt().accept(this);
        return null;
    }

    /**
     * Visit a for statement node
     * 
     * @param node
     *        the for statement node
     * @return result of the visit
     */
    public Object visit(ForStmt node) {
        if (node.getInitExpr() != null)
            node.getInitExpr().accept(this);
        if (node.getPredExpr() != null)
            node.getPredExpr().accept(this);
        if (node.getUpdateExpr() != null)
            node.getUpdateExpr().accept(this);
        node.getBodyStmt().accept(this);
        return null;
    }

    /**
     * Visit a break statement node
     * 
     * @param node
     *        the break statement node
     * @return result of the visit
     */
    public Object visit(BreakStmt node) {
        return null;
    }

    /**
     * Visit a block statement node
     * 
     * @param node
     *        the block statement node
     * @return result of the visit
     */
    public Object visit(BlockStmt node) {
        node.getStmtList().accept(this);
        return null;
    }

    /**
     * Visit a return statement node
     * 
     * @param node
     *        the return statement node
     * @return result of the visit
     */
    public Object visit(ReturnStmt node) {
        if (node.getExpr() != null)
            node.getExpr().accept(this);

        returnStmt();
        return null;
    }

    /**
     * Visit a list node of expressions
     * 
     * @param node
     *        the expression list node
     * @return result of the visit
     */
    public Object visit(ExprList node) {
        for (Iterator it = node.getIterator(); it.hasNext();)
            ((Expr) it.next()).accept(this);
        return null;
    }

    /**
     * Visit an expression node (should never be called)
     * 
     * @param node
     *        the expression node
     * @return result of the visit
     */
    public Object visit(Expr node) {
        throw new RuntimeException(
                "This visitor method should not be called (node is abstract)");
    }

    /**
     * Visit a dispatch expression node
     * 
     * @param node
     *        the dispatch expression node
     * @return result of the visit
     */
    public Object visit(DispatchExpr node) {
        node.getRefExpr().accept(this);
        node.getActualList().accept(this);
        return null;
    }

    /**
     * Visit a new expression node
     * 
     * @param node
     *        the new expression node
     * @return result of the visit
     */
    public Object visit(NewExpr node) {
        return null;
    }

    /**
     * Visit a new array expression node
     * 
     * @param node
     *        the new array expression node
     * @return result of the visit
     */
    public Object visit(NewArrayExpr node) {
        node.getSize().accept(this);
        return null;
    }

    /**
     * Visit an instanceof expression node
     * 
     * @param node
     *        the instanceof expression node
     * @return result of the visit
     */
    public Object visit(InstanceofExpr node) {
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit a cast expression node
     * 
     * @param node
     *        the cast expression node
     * @return result of the visit
     */
    public Object visit(CastExpr node) {
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit an assignment expression node
     * 
     * @param node
     *        the assignment expression node
     * @return result of the visit
     */
    public Object visit(AssignExpr node) {
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit an array assignment expression node
     * 
     * @param node
     *        the array assignment expression node
     * @return result of the visit
     */
    public Object visit(ArrayAssignExpr node) {
        node.getIndex().accept(this);
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary expression node (should never be called)
     * 
     * @param node
     *        the binary expression node
     * @return result of the visit
     */
    public Object visit(BinaryExpr node) {
        throw new RuntimeException(
                "This visitor method should not be called (node is abstract)");
    }

    /**
     * Visit a binary comparison expression node (should never be called)
     * 
     * @param node
     *        the binary comparison expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompExpr node) {
        throw new RuntimeException(
                "This visitor method should not be called (node is abstract)");
    }

    /**
     * Visit a binary comparison equals expression node
     * 
     * @param node
     *        the binary comparison equals expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompEqExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison not equals expression node
     * 
     * @param node
     *        the binary comparison not equals expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompNeExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison less than expression node
     * 
     * @param node
     *        the binary comparison less than expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompLtExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison less than or equal to expression node
     * 
     * @param node
     *        the binary comparison less than or equal to expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompLeqExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison greater than expression node
     * 
     * @param node
     *        the binary comparison greater than expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompGtExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison greater than or equal to expression node
     * 
     * @param node
     *        the binary comparison greater to or equal to expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompGeqExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary arithmetic expression node (should never be called)
     * 
     * @param node
     *        the binary arithmetic expression node
     * @return result of the visit
     */
    public Object visit(BinaryArithExpr node) {
        throw new RuntimeException(
                "This visitor method should not be called (node is abstract)");
    }

    /**
     * Visit a binary arithmetic plus expression node
     * 
     * @param node
     *        the binary arithmetic plus expression node
     * @return result of the visit
     */
    public Object visit(BinaryArithPlusExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary arithmetic minus expression node
     * 
     * @param node
     *        the binary arithmetic minus expression node
     * @return result of the visit
     */
    public Object visit(BinaryArithMinusExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary arithmetic times expression node
     * 
     * @param node
     *        the binary arithmetic times expression node
     * @return result of the visit
     */
    public Object visit(BinaryArithTimesExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary arithmetic divide expression node
     * 
     * @param node
     *        the binary arithmetic divide expression node
     * @return result of the visit
     */
    public Object visit(BinaryArithDivideExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary arithmetic modulus expression node
     * 
     * @param node
     *        the binary arithmetic modulus expression node
     * @return result of the visit
     */
    public Object visit(BinaryArithModulusExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary logical expression node (should never be called)
     * 
     * @param node
     *        the binary logical expression node
     * @return result of the visit
     */
    public Object visit(BinaryLogicExpr node) {
        throw new RuntimeException(
                "This visitor method should not be called (node is abstract)");
    }

    /**
     * Visit a binary logical AND expression node
     * 
     * @param node
     *        the binary logical AND expression node
     * @return result of the visit
     */
    public Object visit(BinaryLogicAndExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary logical OR expression node
     * 
     * @param node
     *        the binary logical OR expression node
     * @return result of the visit
     */
    public Object visit(BinaryLogicOrExpr node) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a unary expression node
     * 
     * @param node
     *        the unary expression node
     * @return result of the visit
     */
    public Object visit(UnaryExpr node) {
        throw new RuntimeException(
                "This visitor method should not be called (node is abstract)");
    }

    /**
     * Visit a unary negation expression node
     * 
     * @param node
     *        the unary negation expression node
     * @return result of the visit
     */
    public Object visit(UnaryNegExpr node) {
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit a unary NOT expression node
     * 
     * @param node
     *        the unary NOT expression node
     * @return result of the visit
     */
    public Object visit(UnaryNotExpr node) {
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit a unary increment expression node
     * 
     * @param node
     *        the unary increment expression node
     * @return result of the visit
     */
    public Object visit(UnaryIncrExpr node) {
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit a unary decrement expression node
     * 
     * @param node
     *        the unary decrement expression node
     * @return result of the visit
     */
    public Object visit(UnaryDecrExpr node) {
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit a variable expression node
     * 
     * @param node
     *        the variable expression node
     * @return result of the visit
     */
    public Object visit(VarExpr node) {
        int[] limits = { 0, 0 };
        if (node.getRef() != null) {
            Expr ref = node.getRef();
            ref.accept(this);

            if (ref instanceof VarExpr) {
                ClassTreeNode classToLookUpIn = classTreeNode;
                String refName = ((VarExpr) ref).getName();
                switch (refName) {
                    case "this":
                        classToLookUpIn = classTreeNode;
                    case "super":
                        classToLookUpIn = classTreeNode.getParent();
                    default: // field, local or formal
                        getField((int) classTreeNode.getVarSymbolTable()
                                .lookup(refName), node.getName());

                }
            } else if (ref instanceof ArrayExpr) {
                getField(
                        (int) classTreeNode.getVarSymbolTable()
                                .lookup(((ArrayExpr) ref).getName()),
                        node.getName());
            } else {
                throw new RuntimeException(
                        "Error: reference expression should be either var or array expression");
            }
        } else {// exists in locals
            int localIndex = (int) classTreeNode.getVarSymbolTable()
                    .lookup(node.getName());
            if (SemantVisitor.isPrimitive(node.getExprType()))
                iload(localIndex);
            else
                aload(localIndex);
        }
        limits[0] = 1;
        return limits;
    }

    /**
     * Visit an array expression node
     * 
     * @param node
     *        the array expression node
     * @return result of the visit
     */
    public Object visit(ArrayExpr node) {
        if (node.getRef() != null)
            node.getRef().accept(this);
        node.getIndex().accept(this);
        return null;
    }

    /**
     * Visit a constant expression node (should never be called)
     * 
     * @param node
     *        the constant expression node
     * @return result of the visit
     */
    public Object visit(ConstExpr node) {
        throw new RuntimeException(
                "This visitor method should not be called (node is abstract)");
    }

    /**
     * Visit an int constant expression node
     * 
     * @param node
     *        the int constant expression node
     * @return result of the visit
     */
    public Object visit(ConstIntExpr node) {
        // adds 1 to the stack and 0 to the locals at its height
        int[] limits = { 1, 0 };
        iconst(node.getIntConstant());

        return limits;
    }

    /**
     * Visit a boolean constant expression node
     * 
     * @param node
     *        the boolean constant expression node
     * @return result of the visit
     */
    public Object visit(ConstBooleanExpr node) {
        // adds 1 to the stack and 0 to the locals at its height
        int[] limits = { 1, 0 };
        iconst(node.getConstant().equals("true") ? 1 : 0);

        return limits;
    }

    /**
     * Visit a string constant expression node
     * 
     * @param node
     *        the string constant expression node
     * @return result of the visit
     */
    public Object visit(ConstStringExpr node) {
        // adds 1 to the stack and 0 to the locals at its height
        int[] limits = { 1, 0 };
        ldc(node.getConstant());

        return limits;
    }
}
