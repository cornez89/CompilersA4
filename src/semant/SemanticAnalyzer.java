/* Bantam Java Compiler and Language Toolset.

   Copyright (C) 2007 by Marc Corliss (corliss@hws.edu) and 
                         E Christopher Lewis (lewis@vmware.com).
   ALL RIGHTS RESERVED.

   The Bantam Java toolset is distributed under the following 
   conditions:

     You may make copies of the toolset for your own use and 
     modify those copies.

     All copies of the toolset must retain the author names and 
     copyright notice.

     You may not sell the toolset or distribute it in 
     conjunction with a commerical product or service without 
     the expressed written consent of the authors.

   THIS SOFTWARE IS PROVIDED ``AS IS'' AND WITHOUT ANY EXPRESS 
   OR IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE 
   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
   PARTICULAR PURPOSE. 
*/

package semant;

import ast.*;
import java.lang.reflect.Array;
import util.*;
import visitor.*;
import java.util.*;
import java.util.function.Consumer;

/** The <tt>SemanticAnalyzer</tt> class performs semantic analysis.
  * In particular this class is able to perform (via the <tt>analyze()</tt>
  * method) the following tests and analyses: (1) legal inheritence
  * hierarchy (all classes have existing parent, no cycles), (2) 
  * legal class member declaration, (3) there is a correct Main class
  * and main() method, and (4) each class member is correctly typed.
  * 
  * This class is incomplete and will need to be implemented by the student. 
  * */
public class SemanticAnalyzer {
    /** Root of the AST */
    private Program program;
    
    /** Root of the class hierarchy tree */
    private ClassTreeNode root;
    
    /** Maps class names to ClassTreeNode objects describing the class */
    private Hashtable<String,ClassTreeNode>
        classMap = new Hashtable<String,ClassTreeNode>();

    /** Ordered list of ClassTreeNode objects (breadth first) */
    private Vector<ClassTreeNode> orderedClassList = new Vector<ClassTreeNode>();
    
    /** Object for error handling */
    private ErrorHandler errorHandler = new ErrorHandler();
    
    /** Boolean indicating whether debugging is enabled */
    private boolean debug = true;

    /** Maximum number of inherited and non-inherited fields that can
     * be defined for any one class */
    private final int MAX_NUM_FIELDS = 1500;

    /** SemanticAnalyzer constructor
      * @param program root of the AST
      * @param debug boolean indicating whether debugging is enabled
      * */
    public SemanticAnalyzer(Program program, boolean debug) {
        this.program = program;
        this.debug = debug;
    }
    
    /** Analyze the AST checking for semantic errors and annotating the tree
      * Also builds an auxiliary class hierarchy tree 
      * @return root of the class hierarchy tree (needed for code generation)
      *
      * Must add code to do the following:
      *   1 - build built-in class nodes in class hierarchy tree (already done) and
      *       build and check the rest of the class hierarchy tree
      *   2 - build the environment for each class (adding class members only) 
      *       and check that members are declared properly
      *   3 - check that the Main class and main method are declared properly
      *   4 - type check each class member
      * See the lab manual for more details on each of these steps.
      * */
    public ClassTreeNode analyze() {

        // list of class declarations
        ClassList classList = program.getClassList();
        
        // PART 1: class tree
        // build and check class hierarchy tree
        buildClassTree(classList);
        
        // PART 2: class symbol table
        // build class symbol table for members and check that members are
        // declared properly
        buildSymbolTable();
        
        // PART 3: Main class/main method
        // check that there is a Main class and main method
        checkMain();
        
        // PART 4: type checking
        // type check each member (fields and methods) of each user-defined class
        typeCheck();

        errorHandler.checkErrors();        
        return root;

    }
    
    /** Add built in classes to the class tree 
      * */
    private void updateBuiltins() {
        // create AST node for object
        Class_ astNode = 
            new Class_(-1, "<built-in class>", "Object", null, 
                       (MemberList)(new MemberList(-1))
                       .addElement(new Method(-1, "Object", "clone", 
                                              new FormalList(-1), 
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null)))));
        // create a class tree node for object, save in variable root
        root = new ClassTreeNode(astNode,
                                 /*built-in?*/true, /*extendable?*/true, classMap);
        // add object class tree node to the mapping
        classMap.put("Object", root);
        
        // note: String, TextIO, and Sys all have fields that are not
        // shown below.  Because these classes cannot be extended and
        // fields are protected, they cannot be accessed by other
        // classes, so they do not have to be included in the AST.
        
        // create AST node for String
        astNode =
            new Class_(-1, "<built-in class>",
                       "String", "Object",                                         
                       (MemberList)(new MemberList(-1))
                       .addElement(new Method(-1, "int", "length",
                                              new FormalList(-1), 
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       .addElement(new Method(-1, "boolean", "equals",
                                              (FormalList)(new FormalList(-1))
                                              .addElement(new Formal(-1, "Object", 
                                                                     "str")),
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       .addElement(new Method(-1, "String", "substring",
                                              (FormalList)(new FormalList(-1))
                                              .addElement(new Formal(-1, "int", 
                                                                     "beginIndex"))
                                              .addElement(new Formal(-1, "int",
                                                                     "endIndex")),
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       .addElement(new Method(-1, "String", "concat",
                                              (FormalList)(new FormalList(-1))
                                              .addElement(new Formal(-1, "String",
                                                                     "str")), 
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null)))));
        // create class tree node for String, add it to the mapping
        classMap.put("String", new ClassTreeNode(astNode, /*built-in?*/true,
                                                 /*extendable?*/false, classMap));
        
        // create AST node for TextIO
        astNode =
            new Class_(-1, "<built-in class>", 
                       "TextIO", "Object",                                         
                       (MemberList)(new MemberList(-1))
                       .addElement(new Method(-1, "void", "readStdin", 
                                              new FormalList(-1), 
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       .addElement(new Method(-1, "void", "readFile",
                                              (FormalList)(new FormalList(-1))
                                              .addElement(new Formal(-1, "String", 
                                                                     "readFile")),
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       .addElement(new Method(-1, "void", "writeStdout", 
                                              new FormalList(-1), 
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       .addElement(new Method(-1, "void", "writeStderr", 
                                              new FormalList(-1), 
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       .addElement(new Method(-1, "void", "writeFile",
                                              (FormalList)(new FormalList(-1))
                                              .addElement(new Formal(-1, "String", 
                                                                     "writeFile")),
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       .addElement(new Method(-1, "String", "getString",
                                              new FormalList(-1), 
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       .addElement(new Method(-1, "int", "getInt",
                                              new FormalList(-1), 
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       .addElement(new Method(-1, "TextIO", "putString",
                                              (FormalList)(new FormalList(-1))
                                              .addElement(new Formal(-1, "String", 
                                                                     "str")),
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       .addElement(new Method(-1, "TextIO", "putInt",
                                              (FormalList)(new FormalList(-1))
                                              .addElement(new Formal(-1, "int", 
                                                                     "n")),
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null)))));
        // create class tree node for TextIO, add it to the mapping
        classMap.put("TextIO", new ClassTreeNode(astNode, /*built-in?*/true,
                                                 /*extendable?*/false, classMap));
        
        // create AST node for Sys
        astNode =
            new Class_(-1, "<built-in class>",
                       "Sys", "Object", 
                       (MemberList)(new MemberList(-1))
                       .addElement(new Method(-1, "void", "exit",
                                              (FormalList)(new FormalList(-1))
                                              .addElement(new Formal(-1, "int", 
                                                                     "status")), 
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       /*
                       .addElement(new Method(-1, "int", "time",
                                              new FormalList(-1), 
                                              (StmtList)(new StmtList(-1))
                                              .addElement(new ReturnStmt(-1, null))))
                       */
                       );
        // create class tree node for Sys, add it to the mapping
        classMap.put("Sys", new ClassTreeNode(astNode, /*built-in?*/true,
                                              /*extendable?*/false, classMap));
    }


    /*************************************************************************
     *       You should not have to modify the code above this point         *
     *************************************************************************/

    /** Build class hierarchy tree, checking to make sure it is well-formed
      * Broken up into three parts: (1) build class tree nodes, add nodes to 
      * the mapping, and check for duplicate class names; (2) set parent links
      * of the nodes, and check if parent exists; (3) check that there are
      * no cycles in the graph (i.e., that it's a tree)
      * @param classList list of AST class nodes
      * */
    private void buildClassTree(ClassList classList)
    {
                updateBuiltins();
            
                // complete this method
                Iterator<ASTNode> iterator = classList.getIterator();
                Map<String, Class_> classNodes = new HashMap<String, Class_>();
                
                while(iterator.hasNext()) {
                        ASTNode node = iterator.next();
                        if (!(node instanceof Class_))
                                return; // error
                        Class_ classNode = (Class_) node;
                        if (classNodes.containsKey(classNode.getName())) {
                                errorHandler.register(errorHandler.SEMANT_ERROR, classNode.getFilename(), classNode.getLineNum(), 
                                        "Error in BuildClassTree: Duplicate name: " + classNode.getName() + " found in class hiearchy.");
                        } else {
                                classNodes.put(classNode.getName(), classNode);
                        }
                }

                int initialNumOfClasses = classNodes.size();
                boolean classesChanged = true;

                for (Map.Entry<String, Class_> entry : classNodes.entrySet()) {
                        String className = entry.getKey();
                        Class_ classNode = entry.getValue();

                        if (classMap.containsKey(entry.))
                        for (int i = 0; i < classNodes.size();) {

                                //make class tree node
                                Class_ classNode = (Class_) classNodes.get(i);
                                ClassTreeNode classTreeNode = new ClassTreeNode(
                                        classNode,
                                        false,//never built in 
                                        true, //always extendable
                                        classMap
                                        );

                                //check if name already exists
                                if (classMap.containsKey((classNode.getName()))) {
                                        //Error duplicate class name
                                        errorHandler.register(errorHandler.SEMANT_ERROR, classNode.getFilename(), 0, 
                                        "Error in BuildClassTree: Duplicate name: " + classNode.getName() + " found in class hiearchy.");
                                        
                                        //disregard
                                        classNodes.remove(i);
                                } else {

                                        //check if parent exists
                                        if (!classMap.containsKey(classNode.getParent())) {
                                                //No parent class found
                                                //do not put into map just yet
                                                errorHandler.register(errorHandler.SEMANT_ERROR, classNode.getFilename(), 0, 
                                                        "Parent class not added yet for " + classNode.getName() + ".");
                                                i++;
                                        } else if (!classMap.get(classNode.getParent()).isExtendable()) {
                                                errorHandler.register(errorHandler.SEMANT_ERROR, classNode.getFilename(), 0, 
                                                        "Parent class: " + classNode.getParent() + " is not extendable.");
                                                classNodes.remove(i);
                                        }
                                        else {
                                                //update parent link
                                                ClassTreeNode parent = classMap.get(classNode.getParent());
                                                classTreeNode.setParent(parent); //updates the parent as well
                                                
                                                //add to classMap
                                                classMap.put(classNode.getName(), classTreeNode);

                                                //check for cycles
                                                int numOfNodes = classMap.size();
                                                int nodesTraversed = 0;
                                                ClassTreeNode currTreeNode = classTreeNode;
                                                while (currTreeNode != null && nodesTraversed <= numOfNodes) {
                                                        currTreeNode = currTreeNode.getParent();
                                                        nodesTraversed++;
                                                }
                                                
                                                boolean isCycle = nodesTraversed > numOfNodes;

                                                if (isCycle) {
                                                        errorHandler.register(errorHandler.SEMANT_ERROR, classNode.getFilename(), 0, 
                                                        "Error in BuildClassTree: Cycle found in class hiearchy after adding class: " + classNode.getName() + 
                                                        " with parent: " + classNode.getParent() + ".");
                                                }

                                                //add to ordered list
                                                classNodes.remove(i);
                                                orderedClassList.add(classTreeNode);
                                        }
                                }
                        }
                        classesChanged = initialNumOfClasses != classNodes.size();
                }
                for(String className : classMap.keySet()) {
                        System.out.println("class: " + className + " value: " + classMap.get(className));
                }
                for (ClassTreeNode node : orderedClassList) {
                        System.out.println(node.getName());
                }
                //The remaining nodes do not have a parent class
                for (ASTNode node : classNodes) {
                        Class_ classNode = (Class_) node;
                        errorHandler.register(errorHandler.SEMANT_ERROR, classNode.getFilename(), 0, 
                                "Error in BuildClassTree: Parent class: " + classNode.getParent() + 
                                " does not exist in the class hiearchy.");
                }
                
    }
    
    /** Build symbol table for each class
      * Note: builds symbol table only for class members not for locals
      * Must be done before any type checking can be done since classes may
      * contain code that refer to members in other classes
      * Note also: cannot build symbol table for a subclass before its 
      * parent class (since child may use symbols in superclass).
      * */
    private void buildSymbolTable()
    {
                
                // for(String className : classMap.keySet()) {
                //         System.out.println("class: " + className + " value: " + classMap.get(className));
                // }
                // for (ClassTreeNode node : orderedClassList) {
                //         System.out.println(node.getName());
                // }
                // for(int i = 0; i < orderedClassList.size(); i++) {
                        // ClassTreeNode classTreeNode = orderedClassList.elementAt(i);
                        ClassEnvVisitor classEnvVisitor = new ClassEnvVisitor(root, errorHandler);
                        classEnvVisitor.visit(root);

    }
    
    /** Check that Main class and main() method are defined correctly
      * */
    private void checkMain()
    {
                if (classMap.keySet().contains("Main")) {
                        ClassTreeNode mainClass = classMap.get("Main");
                        
                        Iterator<ASTNode> members = mainClass.getASTNode().getMemberList().getIterator();
                        Method mainMethod = null;
                        while(members.hasNext()) {
                                Member member = (Member) members.next();
                                if (member instanceof Method && ((Method) member).getName().equals("main")) {
                                        mainMethod = (Method) member;
                                }
                        }

                        if (mainMethod != null) {
                                if (!mainMethod.getFormalList().getIterator().hasNext() && 
                                        mainMethod.getReturnType().equals("void")) {
                                        //main class and main method are defined with no params

                                } else {
                                                //Error main method is not defined correctly
                                        errorHandler.register(errorHandler.SEMANT_ERROR, 
                                                mainClass.getASTNode().getFilename(), 0, 
                                                "Error: Main method is not defined correctly." + 
                                                "Main should have a return type of void and no" + 
                                                " params. Ex. void main().");
                                }
                        } else {
                                //Error main method is not defined 
                                        errorHandler.register(errorHandler.SEMANT_ERROR, 
                                                mainClass.getASTNode().getFilename(), 0, 
                                                "Error in CheckMain: Main method is not defined." + 
                                                " Main class should have a main method defined" + 
                                                " with return type of void and 0 params." + 
                                                " Ex. void main().");
                        }
                } else {
                        //Error main method is not defined 
                        errorHandler.register( errorHandler.SEMANT_ERROR, 
                                "Error in CheckMain: Main class is not defined." + 
                                " There must be a class named Main in" + 
                                " a program.");
                }
        }

    
    /** Type check each class member
      * */
    private void typeCheck()
    {

                TypeCheckVisitor typeCheckVisitor = new TypeCheckVisitor(root, errorHandler);
                typeCheckVisitor.visit(root);

    }

}
