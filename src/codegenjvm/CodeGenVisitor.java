package codegenjvm;

import visitor.Visitor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import ast.*;
import parser.JavaCharStream;
import semant.SemantVisitor;
import util.ClassTreeNode;

public class CodeGenVisitor extends Visitor {

    PrintWriter out;
    private ClassTreeNode classTreeNode;
    LinkedList bytecodeBuffer = new LinkedList<String>();

    // might need these to keep track of limits
    private int currStackSize = 0;
    private int currLocalSize = 1; // start at 1 for this reference
    private int[] sizesAtStart = { 0, 1 };
    private int[] currLimits = { 0, 1 };

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

    // Helper method to that indents the input by 4 spaces and puts a \n at
    // the end
    private void printBytecode(String bytecode) {
        bytecode = bytecode.replace("\n", "\\n");
        bytecode = bytecode.replace("\t", "\\t");
        bytecode = bytecode.replace("\f", "\\f");
        bytecode = bytecode.replace("\\", "\\\\");

        println(bytecode);
        bytecodeBuffer.add("    " + bytecode);
    }

    private void emptyQueue() {
        while(!bytecodeBuffer.isEmpty()) {
            out.println(bytecodeBuffer.remove());
        }
    }

    private void printComment(String comment) {
        out.println("    ; " + comment);
    }

    /**
     * 
     * Bytecode Methods
     * 
     */

    // no args
    // net stack size + 1
    private void ldc(String constant) {
        printBytecode("ldc " + constant);
        currStackSize++;
        checkLimits();
    }

    // no args
    // net stack size + 1
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
        checkLimits();

    }

    // no args
    // net stack size + 1
    private void iload(int index) {
        if (index < 4)
            printBytecode("iload_" + index);
        else
            printBytecode("iload " + index);
        currStackSize++;
        checkLimits();
        // net stack size + 1
    }

    // 1 arg <value>
    // removes 1 from stack
    private void istore(int index) {
        if (currStackSize <= 0)
            throw new RuntimeException("Error: popped from an empty stack");
        else if (index < 4)
            printBytecode("istore_" + index);
        else
            printBytecode("istore " + index);
        currStackSize--;
        checkLimits();
    }

    // no args
    // adds 1 to stack
    private void aload(int index) {
        if (index == 0)
            printBytecode("aload_" + index);
        else if (index < 4)
            printBytecode("aload_" + index);
        else
            printBytecode("aload " + index);
        currStackSize++;
        checkLimits();
    }
    // 2 args <reference> <index>
    // removes 2 from stack pushes a ref. net -1
    private void aaload() {
        printBytecode("aastore");
        currStackSize--;
        checkLimits();
    }

    // 2 args <reference> <index>
    // removes 2 from stack pushes an int. net -1
    private void iaload() {
        printBytecode("iastore");
        currStackSize--;
        checkLimits();
    }

    // 1 args <value>
    // removes 1 from stack
    private void astore(int index) {
        if (currStackSize <= 0)
            throw new RuntimeException("Error: popped from an empty stack");
        else if (index < 4)
            printBytecode("astore_" + index);
        else
            printBytecode("astore " + index);
        currStackSize--;
        checkLimits();
    }

    // 3 args <reference> <index> <value>
    // removes 3 from stack
    private void aastore() {
        printBytecode("aastore ");
        currStackSize -= 3;
        checkLimits();
    }

    // 3 args <reference> <index> <value>
    // removes 3 from stack
    private void iastore() {
        printBytecode("iastore");
        currStackSize -= 3;
        checkLimits();
    }

    // 1 arg <anything>
    // removes 1 from stack
    private void pop() {
        if (currStackSize <= 0)
            throw new RuntimeException("Error: popped from an empty stack");
        printBytecode("pop");
        currStackSize--;
        checkLimits();
    }

    // 1 arg anything
    // adds 1 to stack
    private void dup() {
        if (currStackSize <= 0)
            throw new RuntimeException("Error: Nothing to dup");
        printBytecode("dup");
        currStackSize++;
        checkLimits();
    }

    // //
    // private void putStatic(ClassTreeNode classTreeNode, String fieldName,
    // String descriptor) {
    // aload(0);
    // printBytecode("putfield " + classTreeNode.getName() + "/" + fieldName
    // + " " + descriptor);
    // currStackSize--;
    // currStackSize--;
    // // net stack size - 1
    // }

    // private void getStatic(ClassTreeNode classTreeNode, String fieldName,
    // String descriptor) {
    // aload(0);
    // printBytecode("getstatic " + classTreeNode.getName() + "/" + fieldName
    // + " " + descriptor);
    // // net stack size + 1
    // }

    // 2 arg <reference> <value>
    // removes 2 from stack
    private void putField(String field) {
        printBytecode("putfield " + field);
        currStackSize--;
        currStackSize--;
        checkLimits();
    }

    // 1 arg reference
    // net equal stack
    private void getField(String field) {
        printBytecode("getfield " + field);
    }

    // new array:
    // 1 arg <count>
    // net equal stack
    // new:
    // no arg
    // add 1 to stack
    private void newObject(String className) {
        String type;

        type = getDescriptor(className);
        type = type.substring(1, type.length() - 1);
        printBytecode("new " + type);

        println("new " + type);

        currStackSize++;
        checkLimits();
        // call constructor if its an object
        if (!SemantVisitor.isPrimitive(className)) {
            dup();
            String initSignature = (String) classTreeNode
                .lookupClass(className).getMethodSymbolTable()
                .lookup("<init>");
            println(initSignature);
            invokeSpecial(initSignature);
        }

        // net stack size + 1
    }

    //args <size>
    //removes 1 from the stack but adds reference so =
    private void newArray(String className) {
        String type;

        if (SemantVisitor.isPrimitive(className))
            printBytecode("newarray " + className);
        else {// if its a class, we need to call the constructor too
            type = getDescriptor(className);
            type = type.substring(1, type.length() - 1);
            printBytecode("anewarray " + type);
        }

        checkLimits();
    }

    // 1 arg <reference>
    // remove 1 from stack
    private void callSuper() {
        aload(0);
        invokeSpecial(
            getClass(classTreeNode.getParent().getName()) + "/<init>()V");
    }

    // n+1 for <reference> args for each <param>
    // remove n+1 - 1(if it returns something)
    private void invokeSpecial(String method) {
        printBytecode("invokespecial " + method);
        int numOfParameters = 1;
        if (!method.endsWith("V"))
            numOfParameters--;

        numOfParameters += method.substring(0, method.length() - 1)
            // remove the last character in case the return type had another
            // ';' delimiter
            .split(";").length - 1;

        println("invokespecial removed " + numOfParameters + " items from stack");
        currStackSize -= numOfParameters;// for the reference
        checkLimits();
    }

    // n+1 for <reference> args for each <param>
    // remove n+1 - 1(if it returns something)
    private void invokeVirtual(String method) {

        printBytecode("invokevirtual " + method);
        int numOfParameters = 1;
        if (!method.endsWith("V"))
            numOfParameters--;

        numOfParameters += method.substring(0, method.length() - 1)
            // remove the last character in case the return type had another
            // ';' delimiter
            .split(";").length - 1;
        currStackSize -= numOfParameters + 1;// for the reference
        checkLimits();
    }

    // no args
    // remove 1 from stack
    private void iadd() {
        printBytecode("iadd");
        currStackSize -= 1;
    }

    // no args
    // remove 1 from stack
    private void isub() {
        printBytecode("isub");
        currStackSize -= 1;
    }

    // no args
    // remove 1 from stack
    private void imul() {
        printBytecode("imul");
        currStackSize -= 1;
    }

    // no args
    // remove 1 from stack
    private void idiv() {
        printBytecode("idiv");
        currStackSize -= 1;
    }

    // no args
    // remove 1 from stack
    private void irem() {
        printBytecode("irem");
        currStackSize -= 1;
    }

    // no args
    // remove 1 from stack
    private void ineg() {
        printBytecode("ineg");
        currStackSize -= 1;
    }

    // 2 args <index of local> <const to add>
    // no change to stack
    private void iinc() {
        printBytecode("iinc");
    }

    // no args
    // remove 1 from stack
    private void iand() {
        printBytecode("iand");
        currStackSize -= 1;
    }

    // no args
    // remove 1 from stack
    private void ior() {
        printBytecode("ior");
        currStackSize -= 1;
    }

    // no args
    // remove 1 from stack
    private void ixor() {
        printBytecode("ixor");
        currStackSize -= 1;
    }

    // no args
    // nothing
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

    private void checkLimits() {
        currLimits[0] = Math.max(currLimits[0],
            currStackSize - sizesAtStart[0]);
        currLimits[1] = Math.max(currLimits[1],
            currLocalSize - sizesAtStart[1]);
        
            println("CurrStackSize: " +currLocalSize + " currLocalSize: " + currLocalSize);
        
            println("CurrLimits[0]: " + currLimits[0] + " currLimits[1]: " + currLimits[1]);
    }

    private String getDescriptor(String type) {
        if (type == null) {
            println("Null type");
        }
        switch (type) {
            case "int":
                return "I";
            case "boolean":
                return "Z";
            case "void":
                return "V";
            case "String":
                return "Ljava/lang/String;";
            case "Object":
                return "Ljava/lang/Object;";
            default:
                return "L" + type + ";";
        }
    }

    private String getMethodSignature(Method node) {
        String signature = getClass(classTreeNode.getName()) + "/"
            + node.getName() + "(";
        Iterator<ASTNode> formals = node.getFormalList().getIterator();
        while (formals.hasNext()) {
            Formal formal = (Formal) formals.next();
            signature += getDescriptor(formal.getType());
        }
        signature += ")";
        signature += getDescriptor(node.getReturnType());
        classTreeNode.getMethodSymbolTable().add(node.getName(), signature);
        
        return signature;
    }

    // recursively creates a file path as so
    // filepath/etc.../ParentOfParentClass/parentClass/class
    private String getFilePath(ClassTreeNode classNode, String filePath) {
        return filePath + getFilePathHelper(classNode.getParent())
            + classNode.getName();
    }

    private String getFilePathHelper(ClassTreeNode classNode) {
        if (classNode == null)
            return "";
        else
            return getFilePathHelper(classNode.getParent())
                + classNode.getName() + "/";
    }

    ///
    /// All the fields in a class get initalized here with there actual/default values
    /// 
    void initializeFields(ArrayList<Field> fields) {
        if (fields.isEmpty()) {
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                Expr init = field.getInit();

                // Assign default values if non exist
                if (init != null) {
                    init.accept(this);
                } else {

                    // assign default values
                    switch (getDescriptor(field.getType())) {
                        case "I":
                        case "Z":

                            iconst(0);
                            break;
                        case "S":
                        default:
                            printBytecode("aconst_null");
                    }
                }
                putField(field.getName());
            }
        }
    }

    /**
     * Visit an AST node (should never be called)
     * 
     * @param node
     *            the AST node
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
     *            the list node
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
     *            the program node
     * @return result of the visit
     */
    public Object visit(Program node) {
        throw new RuntimeException(
            "This visitor method should not be called");
    }

    /**
     * Visit a list node of classes
     * 
     * @param node
     *            the class list node
     * @return result of the visit
     */
    public Object visit(ClassList node) {
        throw new RuntimeException(
            "This visitor method should not be called");
    }

    /**
     * Visit a class node
     * 
     * @param node
     *            the class node
     * @return result of the visit
     */
    public Object visit(ClassTreeNode node) {
        // start
        println("ClassTree object start: " + node.getName());
        classTreeNode = node;
        classTreeNode.getASTNode().accept(this);
        return null;
    }

    private String getClass(String className) {
        String descriptor = getDescriptor(className);
        return descriptor.substring(1, descriptor.length() - 1);
    }

    public Object visit(Class_ node) {
        try {
            out = new PrintWriter(new File(node.getName() + ".j"));

            // print top of file info
            out.println(".source " + node.getFilename().substring(8));
            out.println(".class " + "public " + node.getName());
            if (classTreeNode.getParent() != null) {
                out.println(".super "
                    + getClass(classTreeNode.getParent().getName()));
            }
            out.println(".implements " + "java/lang/" + "Cloneable");
            out.println();

            // declare all fields, then constructor, then do all methods
            ArrayList<Field> fields = new ArrayList<>();
            ArrayList<Method> methods = new ArrayList<>();
            Iterator<ASTNode> members = node.getMemberList().getIterator();
            classTreeNode.getVarSymbolTable().enterScope();
            classTreeNode.getMethodSymbolTable().enterScope();

            while (members.hasNext()) {
                Member member = (Member) members.next();
                if (member instanceof Field)
                    fields.add((Field) member);
                else
                    methods.add((Method) member);
            }

            // fields
            for (int i = 0; i < fields.size(); i++)
                out.println(".field " + "protected " + fields.get(i).getName()
                    + getDescriptor(fields.get(i).getType()));

            out.println();

            // write constructor
            String initSignature = "<init>()V";
            out.println(".method " + "public " + initSignature);
            classTreeNode.getMethodSymbolTable().add("<init>",
                getClass(classTreeNode.getName()) + "/" + initSignature);
            initializeFields(fields); // calls accept on each field and
                                      // assigns default values
            if (classTreeNode.getParent() != null)
                callSuper();
            printBytecode("return");
            out.println("    .limit " + "stack " + currLimits[0]);
            out.println("    .limit " + "locals " + currLimits[1]);
            emptyQueue();

            out.println(".end method");
            out.println();

            // methods
            while (!methods.isEmpty())
                methods.removeFirst().accept(this);
            out.println();

            // doesn't work and doesn't complain if I get rid of it so yeah
            // class init
            // out.println(".method " + "static " + "<clinit>()V");
            // initializeFields(fields);
            // printBytecode("return");
            // out.println(".end method");

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
     *            the member list node
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
     *            the member node
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
     *            the field node
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
     *            the method node
     * @return result of the visit
     */
    public Object visit(Method node) { 
        int[] sizesAtStart = {currStackSize, currLocalSize};
        this.sizesAtStart = sizesAtStart;
        currLimits = sizesAtStart.clone();

        classTreeNode.getVarSymbolTable().enterScope();
        classTreeNode.getMethodSymbolTable().enterScope();

        //print the method signature
        if (node.getName().equals("main")) {
            println("main method");
            out.println(".method public static main([Ljava/lang/String;)V");

            // newClass("String[]");
            out.println(".throws java/lang/CloneNotSupportedException");
        } else {
            // print method signature
            out.print(".method " + "public " + node.getName() + "(");
            out.println(".throws java/lang/CloneNotSupportedException");

            // print formals and
            node.getFormalList().accept(this);

            // print return type
            out.print(")" + getDescriptor(node.getReturnType()));
            out.println();
        }

        // add method signature to symbol table
        String signature = getMethodSignature(node);
        println(signature);

        //deal with statements
        //for each bytecode that adds something to the stack, increment currStackSize
        //decrement if it pops
        //find max currstack size after each bytecode and track that through the method
        node.getStmtList().accept(this);
        
        //print max sizes
        out.println("    .limit stack " + currLimits[0]);
        out.println("    .limit locals " + currLimits[1]);

        //printbytecodes
        emptyQueue();
        
        out.println(".end method");
        classTreeNode.getVarSymbolTable().exitScope();
        classTreeNode.getMethodSymbolTable().exitScope();
        return null;
    }

    /**
     * Visit a list node of formals
     * 
     * @param node
     *            the formal list node
     * @return result of the visit
     */
    public Object visit(FormalList node) {
        for (Iterator it = node.getIterator(); it.hasNext();) {
            ((Formal) it.next()).accept(this);
        }
        return null;
    }

    /**
     * Visit a formal node
     * 
     * @param node
     *            the formal node
     * @return result of the visit
     */
    public Object visit(Formal node) {
        classTreeNode.getVarSymbolTable().add(node.getName(), currLocalSize);

        // output descriptor
        String type = getDescriptor(node.getType());
        out.print(type + ";");
        return null;
    }

    /**
     * Visit a list node of statements
     * 
     * @param node
     *            the statement list node
     * @return result of the visit
     */
    public Object visit(StmtList node) {
        println("StmtList");
        for (Iterator it = node.getIterator(); it.hasNext();)
            ((Stmt) it.next()).accept(this);
        return null;
    }

    /**
     * Visit a statement node (should never be calle)
     * 
     * @param node
     *            the statement node
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
     *            the declaration statement node
     * @return result of the visit
     */
    public Object visit(DeclStmt node) {
        printComment(
            "Declaration " + node.getName() + " : " + node.getType());
        classTreeNode.getVarSymbolTable().add(node.getName(), currLocalSize);

        // should push a value onto the stack
        node.getInit().accept(this);
        if (SemantVisitor.isPrimitive(node.getType())) {
            istore(currLocalSize);
        } else {
            astore(currLocalSize);
        }

        return null;
    }

    /**
     * Visit an expression statement node
     * 
     * @param node
     *            the expression statement node
     * @return result of the visit
     */
    public Object visit(ExprStmt node) {
        println("ExprStmt");
        node.getExpr().accept(this);
        return null;
    }

    /**
     * Visit an if statement node
     * 
     * @param node
     *            the if statement node
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
     *            the while statement node
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
     *            the for statement node
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
     *            the break statement node
     * @return result of the visit
     */
    public Object visit(BreakStmt node) {
        return null;
    }

    /**
     * Visit a block statement node
     * 
     * @param node
     *            the block statement node
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
     *            the return statement node
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
     *            the expression list node
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
     *            the expression node
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
     *            the dispatch expression node
     * @return result of the visit
     */
    public Object visit(DispatchExpr node) {
        println("DispatchExpr on " + node.getMethodName());

        // push reference to stack
        node.getRefExpr().accept(this);

        // push parameters to stack
        node.getActualList().accept(this);

        if (node.getRefExpr() instanceof ArrayExpr) {
            if (node.getMethodName().equals("clone")) {
                invokeVirtual((String) classTreeNode.lookupClass("Object")
                    .getMethodSymbolTable().lookup("clone"));
            } else {
                throw new RuntimeException("Error: the only supported method"
                    + "for arrays is clone. Attempted to dispatch method name: "
                    + node.getMethodName());
            }
            return null;
        } else if (node.getRefExpr() instanceof VarExpr) {
            VarExpr refExpr = (VarExpr) node.getRefExpr();
            print("ref = " + refExpr.getName());
            ClassTreeNode refClass;
            if (refExpr.getName().equals("this")) {
                refClass = classTreeNode;
                Object returnValue = refClass.getMethodSymbolTable()
                .lookup(node.getMethodName());
                if (returnValue instanceof String) {
                    String signature = (String) returnValue;
                    invokeVirtual(signature);
                } else {
                    println("Error return value of getMethodSymbolTable is not a String. type = " + returnValue.getClass().getSimpleName());
                }
            } else if (refExpr.getName().equals("super")) {
                invokeSpecial(
                    classTreeNode.getParent() + "." + node.getMethodName());
            } else {
                //Is a class type reference so we need to look up the class and find its method
                refClass = classTreeNode.lookupClass(refExpr.getExprType());
                println("ref class " + refClass.getName());
                Object returnValue = refClass.getMethodSymbolTable()
                .lookup(node.getMethodName());
                if (returnValue instanceof String) {
                    String signature = (String) returnValue;
                    invokeVirtual(signature);
                } else {
                    println("Error return value of getMethodSymbolTable is not a String. type = ");
                }
            }
        }

        return null;
    }

    /**
     * Visit a new expression node
     * 
     * @param node
     *            the new expression node
     * @return result of the visit
     */
    public Object visit(NewExpr node) {
        println("NewExpr : " + node.getType());
        newObject(node.getType());
        return null;
    }

    /**
     * Visit a new array expression node
     * 
     * @param node
     *            the new array expression node
     * @return result of the visit
     */
    public Object visit(NewArrayExpr node) {
        println("NewArray : " + node.getType());
        node.getSize().accept(this);
        newArray(node.getType());

        return null;
    }

    /**
     * Visit an instanceof expression node
     * 
     * @param node
     *            the instanceof expression node
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
     *            the cast expression node
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
     *            the assignment expression node
     * @return result of the visit
     */
    public Object visit(AssignExpr node) {
        node.getExpr().accept(this);
        int indexOfVar = (int) classTreeNode.getVarSymbolTable()
            .lookup(node.getName());

        if (SemantVisitor.isPrimitive(node.getExprType())) {
            istore(indexOfVar);
        } else {
            astore(indexOfVar);
        }
        return null;
    }

    /**
     * Visit an array assignment expression node
     * 
     * @param node
     *            the array assignment expression node
     * @return result of the visit
     */

    public Object visit(ArrayAssignExpr node) {
        // load reference
        ClassTreeNode classToLookupIn = classTreeNode;

        if (node.getRefName() == null) {

        } else {

            if (node.getRefName().equals("this")) {
            } else if (node.getRefName().equals("super")) {
                classToLookupIn = classTreeNode.getParent();
            } else {
            }
        }
        aload(
            (int) classToLookupIn.getVarSymbolTable().lookup(node.getName()));

        node.getIndex().accept(this);
        node.getExpr().accept(this);
        if (SemantVisitor.isPrimitive(node.getExprType()))
            iastore();
        else
            aastore();
        return null;
    }

    /**
     * Visit a binary expression node (should never be called)
     * 
     * @param node
     *            the binary expression node
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
     *            the binary comparison expression node
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
     *            the binary comparison equals expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompEqExpr node) {
        // short circuit
        node.getLeftExpr().accept(this);

        node.getRightExpr().accept(this);
        return null;
    }

    /**
     * Visit a binary comparison not equals expression node
     * 
     * @param node
     *            the binary comparison not equals expression node
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
     *            the binary comparison less than expression node
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
     *            the binary comparison less than or equal to expression node
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
     *            the binary comparison greater than expression node
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
     *            the binary comparison greater to or equal to expression node
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
     *            the binary arithmetic expression node
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
     *            the binary arithmetic plus expression node
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
     *            the binary arithmetic minus expression node
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
     *            the binary arithmetic times expression node
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
     *            the binary arithmetic divide expression node
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
     *            the binary arithmetic modulus expression node
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
     *            the binary logical expression node
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
     *            the binary logical AND expression node
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
     *            the binary logical OR expression node
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
     *            the unary expression node
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
     *            the unary negation expression node
     * @return result of the visit
     */
    public Object visit(UnaryNegExpr node) {
        node.getExpr().accept(this);
        ineg();
        return null;
    }

    /**
     * Visit a unary NOT expression node
     * 
     * @param node
     *            the unary NOT expression node
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
     *            the unary increment expression node
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
     *            the unary decrement expression node
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
     *            the variable expression node
     * @return result of the visit
     */
    public Object visit(VarExpr node) {
        println("VarExpr");
        if (node.getRef() != null) {
            Expr ref = node.getRef();
            ref.accept(this);

            if (ref instanceof VarExpr) {
                ClassTreeNode classToLookUpIn = classTreeNode;
                String refName = ((VarExpr) ref).getName();
                switch (refName) {
                    case "this":
                        classToLookUpIn = classTreeNode;

                        getField(node.getName());
                        break;
                    case "super":
                        classToLookUpIn = classTreeNode.getParent();

                        getField(node.getName());
                        break;
                    default: // field, local or formal
                        getField(node.getName());

                }
            } else if (ref instanceof ArrayExpr) {
                getField(node.getName());
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
        return null;
    }

    /**
     * Visit an array expression node
     * 
     * @param node
     *            the array expression node
     * @return result of the visit
     */
    public Object visit(ArrayExpr node) {
        if (node.getRef() != null)
            node.getRef().accept(this);
        node.getIndex().accept(this);
        if (SemantVisitor.isPrimitive(node.getExprType()))
            iaload();
        else
            aaload();
        return null;
    }

    /**
     * Visit a constant expression node (should never be called)
     * 
     * @param node
     *            the constant expression node
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
     *            the int constant expression node
     * @return result of the visit
     */
    public Object visit(ConstIntExpr node) {
        iconst(node.getIntConstant());

        return null;
    }

    /**
     * Visit a boolean constant expression node
     * 
     * @param node
     *            the boolean constant expression node
     * @return result of the visit
     */
    public Object visit(ConstBooleanExpr node) {
        iconst(node.getConstant().equals("true") ? 1 : 0);

        return null;
    }

    /**
     * Visit a string constant expression node
     * 
     * @param node
     *            the string constant expression node
     * @return result of the visit
     */
    public Object visit(ConstStringExpr node) {
        ldc("\"" + node.getConstant() + "\"");

        return null;
    }
}
