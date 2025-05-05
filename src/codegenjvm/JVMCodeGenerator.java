package codegenjvm;

import java.util.Iterator;

import util.ClassTreeNode;

public class JVMCodeGenerator {
    ClassTreeNode root;
    boolean debug;

    public JVMCodeGenerator(ClassTreeNode root, boolean debug) {
        this.root = root;
        this.debug = debug;
    }

    public void generate() {

        CodeGenVisitor codeGenVisitor = new CodeGenVisitor();
        Iterator<ClassTreeNode> children = root.getChildrenList();

        System.out.printf("Begin generate %s\n", root.getName());
        codeGenVisitor.visit(root);

        while (children.hasNext()) {
            root = children.next();
            generate();
        }
    }
}
