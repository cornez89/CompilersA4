package codegenjvm;

import visitor.Visitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;
import java.util.function.Consumer;

import ast.*;
import semant.SemantVisitor;
import util.ClassTreeNode;

public class CodeGenVisitor extends Visitor {

    PrintWriter out;
    private ClassTreeNode classTreeNode;
    LinkedList<String> bytecodeBuffer = new LinkedList<String>();

    // might need these to keep track of limits
    private int currStackSize = 0;
    private int currLocalSize = 1; // start at 1 for this reference
    private int[] sizesAtStart = { 0, 1 };
    private int[] currLimits = { 0, 1 };
    

    private static class ConditionEntry {
        private String exitLabel;
        private int startStackHeight;

        /**
         * @param condLabel The condition/exit label for where to go after the body statement
         * @param exitLabel The exit label for where to go on a break statement
         * @param startStackHeight The height of the stack before the loop started
         */
        ConditionEntry(String exitLabel, int startStackHeight) {
            this.exitLabel = exitLabel;
            this.startStackHeight = startStackHeight;
        }
    }
    private Stack<ConditionEntry> conditionStack = new Stack<ConditionEntry>();
    private int labelNumber = 0;

    
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
        bytecode = bytecode.replace("\\", "\\\\");
        bytecode = bytecode.replace("\n", "\\n");
        bytecode = bytecode.replace("\t", "\\t");
        bytecode = bytecode.replace("\f", "\\f");

        println(bytecode);
        bytecodeBuffer.add("    " + bytecode);
    }

    private void checkLimits() {
        currLimits[0] = Math.max(currLimits[0],
            currStackSize);
        currLimits[1] = Math.max(currLimits[1],
            currLocalSize);

        println("CurrStackSize: " + currStackSize + " currLocalSize: "
            + currLocalSize);

        println("CurrLimits[0]: " + currLimits[0] + " currLimits[1]: "
            + currLimits[1]);
    }


    private void emptyQueue() {
        while (!bytecodeBuffer.isEmpty()) {
            out.println(bytecodeBuffer.remove());
        }
    }

    private void printComment(String comment, ASTNode node) {
        printBytecode(" ; " + comment);
        println("    ; " + comment + " - line " + node.getLineNum());
        
    }

    private String createLabel() {
        String newLabel = "L" + labelNumber;
        labelNumber++;
        return newLabel;
    }

    /**
     * 
     * Bytecode Methods
     * 
     */

    // 1 arg <label>
    // net stack size unchanged
    private void goto_label(String label) {
        printBytecode("goto " + label);
    }

    // 1 arg <label>
    // net stack size - 1
    private void ifeq(String label) {
        printBytecode("ifeq " + label);
        currStackSize--;
        checkLimits();
    }

    // 1 arg <label>
    // net stack size - 1
    private void ifne(String label) {
        printBytecode("ifne " + label);
        currStackSize--;
        checkLimits();
    }

    // 1 arg <label>
    // net stack size - 2
    private void if_icmpeq(String label) {
        printBytecode("if_icmpeq " + label);
        currStackSize -= 2;
        checkLimits();
    }

    // 1 arg <label>
    // net stack size - 2
    private void if_icmpne(String label) {
        printBytecode("if_icmpne " + label);
        currStackSize -= 2;
        checkLimits();
    }

    // 1 arg <label>
    // net stack size - 2
    private void if_icmpge(String label) {
        printBytecode("if_icmpge " + label);
        currStackSize -= 2;
        checkLimits();
    }

    // 1 arg <label>
    // net stack size - 2
    private void if_icmpgt(String label) {
        printBytecode("if_icmpgt " + label);
        currStackSize -= 2;
        checkLimits();
    }

    // 1 arg <label>
    // net stack size - 2
    private void if_icmple(String label) {
        printBytecode("if_icmple " + label);
        currStackSize -= 2;
        checkLimits();
    }

    // 1 arg <label>
    // net stack size - 2
    private void if_icmplt(String label) {
        printBytecode("if_icmplt " + label);
        currStackSize -= 2;
        checkLimits();
    }

    // 1 arg <label>
    // net stack size - 2
    private void if_acmpeq(String label) {
        printBytecode("if_acmpeq " + label);
        currStackSize -= 2;
        checkLimits();
    }

    // 1 arg <label>
    // net stack size - 2
    private void if_acmpne(String label) {
        printBytecode("if_acmpne " + label);
        currStackSize -= 2;
        checkLimits();
    }

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

    //no args
    //adds a ref to the stack
    private void aconstNull() {
        printBytecode("aconst_null");
        currStackSize++;
    }

    //1 arg <reference>
    //returns new type, net = stack
    private void checkCast(String type) {
        printBytecode("checkcast " + getDescriptorShort(type));
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
        currLocalSize = Math.max(index + 1, currLocalSize);
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
    // removes 2 from stack pushes a reference net -1
    private void aaload() {
        printBytecode("aaload");
        currStackSize--;
        checkLimits();
    }

    // 2 args <reference> <index>
    // removes 2 from stack pushes an int. net -1
    private void iaload() {
        printBytecode("iaload");
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
        currLocalSize = Math.max(index + 1, currLocalSize);
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

    // 1 arg anything
    // adds 1 to stack
    private void dupx1() {
        if (currStackSize <= 0)
            throw new RuntimeException("Error: Nothing to dup");
        printBytecode("dup_x1");
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
    private void putField(String className, String name, String type) {
        
        className = getClass(className); 
        String descriptor = getDescriptor(type);
        printBytecode("putfield " + className + "." + name + " " + descriptor);
        currStackSize--;
        currStackSize--;
        checkLimits();
    }

    // 1 arg reference
    // net equal stack
    private void getField(String className, String name, String type) {
        className = getClass(className); 
        String descriptor = getDescriptor(type);
        if (descriptor.equals("Z") || descriptor.equals("[Z"))
            descriptor = descriptor.replace("Z", "I");
            //We only have int types
        printBytecode("getfield " + className + "." + name + " " + descriptor);
    }

    // 1 arg <value>
    // removes 1 from stack
    private void putStatic(String field) {
        printBytecode("putstatic " + field);
        currStackSize--;
        checkLimits();
    }

    // no args
    // adds one element to the stack
    private void getStatic(String field) {
        printBytecode("getstatic " + field);
        currStackSize++;
        checkLimits();
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

        currStackSize++;
        checkLimits();
        // call constructor if its an object
        if (!SemantVisitor.isPrimitive(className)) {
            dup();
            if (className.equals("Object")) {
                invokeSpecial("java/lang/Object/<init>()V");
            } else {
                Object temp = classTreeNode
                    .lookupClass(className)
                    .getMethodSymbolTable()
                    .lookup("<init>");
                println("DEBUG: return type of MethodSymbolTableLookup is: " + temp.getClass().getSimpleName());

                if (temp instanceof Method) {
                    invokeSpecial(getFullMethodCall((Method) temp, className));
                } else {
                    throw new RuntimeException("Invalid return value of MethodSymbolTableLookup");
                }
            }
        }

        // net stack size + 1
    }

    // args <size>
    // removes 1 from the stack but adds reference so =
    private void newArray(String className) {
        String type;

        if (SemantVisitor.isPrimitive(className)) {
            type = "int"; //atype for int according to java reference
            printBytecode("newarray " + type);
        } else {// if its a class, we need to call the constructor too
            type = getDescriptor(className);
            type = type.substring(1, type.length() - 1);
            printBytecode("anewarray " + type);
        }

        checkLimits();
    }

    //1 arg <reference>
    //net = stack size
    private void arrayLength() {
        printBytecode("arraylength");
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

        println(
            "invokespecial removed " + numOfParameters + " items from stack");
        currStackSize -= numOfParameters;// for the reference
        checkLimits();
    }

    private String getFullMethodCall(Method method, String methodClass) {
        if (methodClass.equals("Object") || methodClass.equals("String"))
            methodClass = "java/lang/" + methodClass;
        return methodClass + "/" + method.getName() + getMethodSignature(method);
    }
    
    // n+1 for <reference> args for each <param>
    // remove n+1 - 1(if it returns something)
    private void invokeVirtual(Method method, String className) {

        printBytecode("invokevirtual " + getFullMethodCall(method, className));
        int numOfParameters = 1; //for reference
        
        if (!method.getReturnType().equals("void"))
            numOfParameters--;
        
        numOfParameters += method.getFormalList().getSize();
        currStackSize -= numOfParameters;
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

    // no args
    // adds a primitive to the caller's stack
    private void ireturnStmt() {
        printBytecode("ireturn");
        currStackSize++;
    }

    // no args
    // adds a primitive to the caller's stack
    private void areturnStmt() {
        printBytecode("areturn");
        currStackSize++;
    }

    private void label(String label) {
        bytecodeBuffer.add("  " + label + ":");
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

    

    private String getDescriptor(String type) {
        if (type == null) {
            println("Null type");
        }
        String descriptor = "";
        if (SemantVisitor.isArray(type)) {
            descriptor += "[";
            type = type.substring(0,type.length() - 2);
        }

        switch (type) {
            case "int":
                descriptor += "I"; break;
            case "boolean":
                descriptor += "Z"; break;
            case "void":
                descriptor +="V"; break;
            case "String":
                descriptor += "Ljava/lang/String;"; break;
            case "Object":
                descriptor += "Ljava/lang/Object;"; break;
            default:
                descriptor += "L" + type + ";"; 
        }
        return descriptor;
    }

    private String getDescriptorShort(String type) {
        if (type == null) {
            println("Null type");
        }
        String descriptor = "";
        if (SemantVisitor.isArray(type)) {
            descriptor += "[";
            type = type.substring(0,type.length() - 2);
        }

        switch (type) {
            case "int":
                descriptor += "I"; break;
            case "boolean":
                descriptor += "Z"; break;
            case "void":
                descriptor +="V"; break;
            case "String":
                descriptor += "java/lang/String"; break;
            case "Object":
                descriptor += "java/lang/Object"; break;
            default:
                descriptor += type; 
        }
        return descriptor;
    }

    private String getMethodSignature(Method node) {
        String signature = "(";
        Iterator<ASTNode> formals = node.getFormalList().getIterator();
        while (formals.hasNext()) {
            Formal formal = (Formal) formals.next();
            signature += getDescriptor(formal.getType());
        }
        signature += ")";
        signature += getDescriptor(node.getReturnType());
        classTreeNode.getMethodSymbolTable().add(node.getName(), node);

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

    

    private boolean varIsField(String varName, ClassTreeNode classNode) {
        return SemantVisitor.existsInClass("this." + varName, classNode);
    }

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
                           aconstNull();
                    }
                }
                putField(classTreeNode.getName(), field.getName(), field.getType());
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
            for (int i = 0; i < fields.size(); i++) {
                //DEBUG
                String line = ".field " + "protected " + fields.get(i).getName()
                    + " " + getDescriptor(fields.get(i).getType());
                println(line);
                out.println(line);
            }

            out.println();

            // write constructor
            String methodName = "<init>";
            Method initMethod = new Method(node.getLineNum(), "void", methodName, new FormalList(node.getLineNum()), new StmtList(node.getLineNum()));
            String signature = getMethodSignature(initMethod);
            out.println(".method " + "public " + "<init>" + signature);
            classTreeNode.getMethodSymbolTable().add(methodName, initMethod);
            initializeFields(fields); // calls accept on each field and
                                      // assigns default values
            if (classTreeNode.getParent() != null)
                callSuper();
            printBytecode("return");
            out.println("    .limit " + "stack " + currLimits[0]);
            println("    .limit " + "stack " + currLimits[0]);
            out.println("    .limit " + "locals " + currLimits[1]);
            println("    .limit " + "locals " + currLimits[1]);
            emptyQueue();

            out.println(".end method");
            out.println();

            // methods
            while (!methods.isEmpty())
                methods.remove(0).accept(this);
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
        printComment("Stack size at start = " + currStackSize + ". Local size = " + currLocalSize, node);
        int[] sizesAtStart = { currStackSize, currLocalSize };
        this.sizesAtStart = sizesAtStart;
        currLimits = sizesAtStart.clone();
        
        String signature = getMethodSignature(node);
        classTreeNode.getMethodSymbolTable().add(node.getName(), node);
        println(signature);

        classTreeNode.getVarSymbolTable().enterScope();
        classTreeNode.getMethodSymbolTable().enterScope();

        // print the method signature
        if (node.getName().equals("main")) {
            println("main method");
            out.println(".method public static main([Ljava/lang/String;)V");

            // newClass("String[]");
            out.println(".throws java/lang/CloneNotSupportedException");
        } else {
            // print method signature
            out.print(".method " + "public " + node.getName() + "(");

            // print formals and
            node.getFormalList().accept(this);
            
            // print return type
            out.print(")" + getDescriptor(node.getReturnType()));
            out.println();
            out.println(".throws java/lang/CloneNotSupportedException");
        }

        // deal with statements
        // for each bytecode that adds something to the stack, increment
        // currStackSize
        // decrement if it pops
        // find max currstack size after each bytecode and track that through
        // the method
        node.getStmtList().accept(this);

        // print max sizes
        out.println("    .limit stack " + currLimits[0]);
        out.println("    .limit locals " + currLimits[1]);

        // printbytecodes
        emptyQueue();
        out.println(".end method");
        classTreeNode.getVarSymbolTable().exitScope();
        classTreeNode.getMethodSymbolTable().exitScope();

        //bring stack and local size down to where it should be
        currStackSize = sizesAtStart[0];
        currLocalSize = sizesAtStart[1];
        if (!node.getReturnType().equals("void"))
            currStackSize++;
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
        //printComment("local var: " + node.getName() + "/" + node.getType(), node);
        classTreeNode.getVarSymbolTable().add(node.getName(), currLocalSize++);
        classTreeNode.getVarSymbolTable().print();
        
        // output descriptor
        String type = getDescriptor(node.getType());
        
        out.print(type);
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
            "Declaration " + node.getName() + " : " + node.getType(), node);
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
        if (!node.getExpr().getExprType().equals("void"))
            pop(); // Need this because expressions add to the stack for non void return types
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
        println("if: " + node.getLineNum());
        String elseLabel = createLabel();
        String exitLabel = createLabel();
        conditionStack.add(new ConditionEntry(exitLabel, currStackSize));

        printComment("if statement predicate", node);
        node.getPredExpr().accept(this);
        ifne(elseLabel);

        printComment("if statement then block", node);
        node.getThenStmt().accept(this);
        goto_label(exitLabel);

        label(elseLabel);
        printComment("if statement else block", node);
        node.getElseStmt().accept(this);

        label(exitLabel);

        conditionStack.pop();
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
        print("while: " + node.getLineNum());
        String condLabel = createLabel();
        String exitLabel = createLabel();
        conditionStack.add(new ConditionEntry(exitLabel, currStackSize));

        label(condLabel);
        printComment("while statement predicate", node);
        node.getPredExpr().accept(this);
        ifne(exitLabel);

        printComment("while statement body", node);
        node.getBodyStmt().accept(this);
        goto_label(condLabel);

        label(exitLabel);

        conditionStack.pop();
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
        print("for: " + node.getLineNum());
        String condLabel = createLabel();
        String exitLabel = createLabel();
        conditionStack.add(new ConditionEntry(exitLabel, currStackSize));

        if (node.getInitExpr() != null) {
            printComment("for statement initialization", node);
            node.getInitExpr().accept(this);
        }

        label(condLabel);
        if (node.getPredExpr() != null) {
            printComment("for statement predicate", node);
            node.getPredExpr().accept(this);
            ifne(exitLabel);
        }

        printComment("for statement body", node);
        node.getBodyStmt().accept(this);

        if (node.getUpdateExpr() != null) {
            printComment("for statement update", node);
            node.getUpdateExpr().accept(this);
        }
        goto_label(condLabel);

        label(exitLabel);

        conditionStack.pop();
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
        ConditionEntry top = conditionStack.pop();
        int numPops = currStackSize - top.startStackHeight;
        for (int i = 0; i > numPops; i++) {
            pop();
            currStackSize--;
        }
        goto_label(top.exitLabel);
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
        if (node.getExpr() != null) {
            node.getExpr().accept(this);
            if (SemantVisitor.isPrimitive(node.getExpr().getExprType()))
                ireturnStmt();
            else
                areturnStmt();
        }

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
        for (Iterator it = node.getIterator(); it.hasNext();) {
            Expr arg = (Expr) it.next();
            printComment("argt type: " + arg.getExprType(), node);
            arg.accept(this);
        }
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

        // find the type of object the method is being called on
        String recieverType;
        if(node.getRefExpr() == null) {
            recieverType = classTreeNode.getName();
        } else {
            recieverType = node.getRefExpr().getExprType();
        }

        //find class of method
        ClassTreeNode refClass;
        
        // see if the method is being called on an array or not
        if(recieverType.endsWith("[]")) {
            if(!node.getMethodName().equals("clone")) {
                throw new RuntimeException("Error: the only supported method" +
                " for arrays is clone. Tried: " + node.getMethodName());
            }
            refClass = classTreeNode.lookupClass("Object");
        } else {
            refClass = classTreeNode.lookupClass(recieverType);
        }

        printComment("dispatch " + "(" + node.getMethodName() + ", " + refClass.getName() + ")", node);

        // push reference to stack
        node.getRefExpr().accept(this);
        println(node.getRefExpr().getExprType());

        
        // push parameters to stack
        // cast null into correct reference type
        Method method = (Method) refClass.getMethodSymbolTable().lookup(node.getMethodName());
        Iterator paramIt = method.getFormalList().getIterator();
        for (Iterator argIt = node.getActualList().getIterator(); argIt.hasNext();  ) {
            Expr arg = (Expr) argIt.next();
            Formal param = (Formal) paramIt.next(); 
            
            arg.accept(this);
            if (arg.getExprType().equals("null")) {
                checkCast(param.getType());
            }
        }
        // node.getActualList().accept(this);
        
        //and to locals
        Object temp = refClass.getMethodSymbolTable().lookup(node.getMethodName());
        println("Return type of MethodSymbolTable.lookup() was: " + temp.getClass().getSimpleName());
        
        if (temp instanceof Method) {
            invokeVirtual((Method) temp, refClass.getName());
        } else {
            throw new RuntimeException("Error: Return type of MethodSymbolTable lookup was invalid: " + temp.getClass().getSimpleName());
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
        printComment("new (" + node.getExprType() + ")", node);
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
        printComment("new (" + node.getExprType() + ")", node);
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
        checkCast(node.getType());
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
        printComment("Variable Assignment  " + node.getName() + "/" + node.getExprType(), node);
        
        String refClass = classTreeNode.getName();
        if (node.getRefName() != null) {
            switch(node.getRefName()) {
                case "this":
                    // is local var
                    if (!varIsField(node.getName(), classTreeNode)) {
                        int indexOfVar = (int) classTreeNode.getVarSymbolTable().lookup(node.getName());
                        node.getExpr().accept(this);
                        dup();   
                        if (SemantVisitor.isPrimitive(node.getExprType())) {    
                            istore(indexOfVar);
                        } else {
                            astore(indexOfVar);
                        }
                    }   
                    return null;
                case "super":
                    refClass = classTreeNode.getParent().getName();
                    break;
                default:
                    refClass = node.getRefName();
            }
        }

        aload(0);
        node.getExpr().accept(this);
        dupx1();
        putField(getClass(refClass), node.getName(), node.getExprType());     

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
        printComment("array assign " + node.getName() + "/" + node.getExprType(), node);
        ClassTreeNode refClass = classTreeNode;

        if (node.getRefName() == null) {

        } else {

            if (node.getRefName().equals("this")) {
            } else if (node.getRefName().equals("super")) {
                refClass = classTreeNode.getParent();
            } else {
                refClass = classTreeNode.lookupClass(node.getRefName());
            }
        }

        if (varIsField(node.getName(), refClass)) {
            aload(0);
            getField(refClass.getName(), node.getName(), node.getExprType());
        } else {
            
            aload((int) refClass.getVarSymbolTable().lookup(node.getName()));
        }

            
        node.getIndex().accept(this);
        node.getExpr().accept(this);
        dup();
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

    public void visitBinaryComp(BinaryCompExpr node, Consumer<String> if_comparison) {
        node.getLeftExpr().accept(this);
        node.getRightExpr().accept(this);
        String thenLabel = createLabel();
        String exitLabel = createLabel();

        if_comparison.accept(thenLabel);
        iconst(0);
        goto_label(exitLabel);
        label(thenLabel);
        iconst(1);
        label(exitLabel);

    }

    /**
     * Visit a binary comparison equals expression node
     * 
     * @param node
     *            the binary comparison equals expression node
     * @return result of the visit
     */
    public Object visit(BinaryCompEqExpr node) {
        String type = node.getExprType();
        if (type.equals("int") || type.equals("bool"))
            visitBinaryComp(node, this::if_icmpeq);
        else
            visitBinaryComp(node, this::if_acmpeq);
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
        String type = node.getExprType();
        if (type.equals("int") || type.equals("bool"))
            visitBinaryComp(node, this::if_icmpne);
        else
            visitBinaryComp(node, this::if_acmpne);
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
        visitBinaryComp(node, this::if_icmplt);
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
        visitBinaryComp(node, this::if_icmple);
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
        visitBinaryComp(node, this::if_icmpgt);
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
        visitBinaryComp(node, this::if_icmpge);
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
        iadd();
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
        isub();
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
        imul();
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
        idiv();
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
        irem();
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
        String shortCircuitLabel = createLabel();
        String exitLabel = createLabel();

        node.getLeftExpr().accept(this);
        ifne(shortCircuitLabel);          // if left expr is false, short
        node.getRightExpr().accept(this); // otherwise eval right expr
        goto_label(exitLabel);
        label(shortCircuitLabel);
        iconst(0);                        // if shorted, false
        label(exitLabel);

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
        String shortCircuitLabel = createLabel();
        String exitLabel = createLabel();

        node.getLeftExpr().accept(this);
        ifeq(shortCircuitLabel);          // if left expr is true, short
        node.getRightExpr().accept(this); // otherwise eval right expr
        goto_label(exitLabel);
        label(shortCircuitLabel);
        iconst(1);                        // if shorted, true
        label(exitLabel);

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

        ineg();
        iconst(1);
        iadd();

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
        println("VarExpr " + node.getName());

        //keywords
        if (node.getName().equals("null")) {
            aconstNull();
        } else if (node.getName().equals("true")) {
            iconst(1);
        } else if (node.getName().equals("false")) {
            iconst(0);
        } else if (node.getName().equals("this")) {
            aload(0);
        } else if (node.getName().equals("length")) {
            node.getRef().accept(this);
            arrayLength();
        } else if (node.getRef() != null) {
            String refClass = "";
            Expr refExpr = node.getRef();
            
           if (refExpr instanceof VarExpr) { //field
                String refName = ((VarExpr) refExpr).getName();
                switch (refName) {
                    case "this":
                        break;
                    case "super":
                        refClass = classTreeNode.getParent().getName() + "/";
                        break;
                    default: // field, local or formal
                        refClass = classTreeNode.lookupClass(refName).getName() + "/";
                }

            } else if (refExpr instanceof ArrayExpr) {//must be length
                refExpr.accept(this);
            } else {
                throw new RuntimeException(
                    "Error: reference expression should be either var or array expression");
            }

            aload(0);
            getField(refClass, node.getName(), node.getExprType());       
        } else {// exists in locals or field of this
            
            println(node.getName());
            //check if its a field (has .this)
            if (varIsField(node.getName(), classTreeNode)) {
                aload(0);
                getField(classTreeNode.getName(), node.getName(), node.getExprType());
            }   
            
            else {
                int localIndex = (int) classTreeNode.getVarSymbolTable()
                                                    .lookup(node.getName());
                if (SemantVisitor.isPrimitive(node.getExprType()))
                    iload(localIndex);
                else
                    aload(localIndex);
            }
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
        if (node.getRef() != null) {   

            //push class ref
            if (node.getRef() instanceof VarExpr) {
                VarExpr refExpr = (VarExpr) node.getRef();
                switch (refExpr.getName()) {
                    case "this": 
                        aload(0); 
                        getField(classTreeNode.getName(), node.getName(), node.getExprType());
                        break;
                    case "super" : 
                        aload(0);
                        getField(classTreeNode.getParent().getName(), node.getName(), node.getExprType());
                        break;
                    default: throw new RuntimeException("Reference to array " + node.getName() + " is not this, super or null.");
                }
            } else {
                throw new RuntimeException("Reference to array" + 
                node.getName() + " is not a varexpr");
            }
        } else {
            println(node.getName());
            classTreeNode.getVarSymbolTable().print();
            int indexOfVar = (int)classTreeNode.getVarSymbolTable().lookup(node.getName());
            aload(indexOfVar);
        }
        
        node.getIndex().accept(this);

        if (SemantVisitor.isPrimitive(node.getExprType()))
            iaload();
        else
            aaload();

        // make sure we update the type of element for the expression
        String arrayElementType = node.getExprType().replaceFirst("\\[\\]$","");
        node.setExprType(arrayElementType);

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
