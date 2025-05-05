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

package visitor;

import ast.*;
import java.util.Iterator;

/** Visitor class for printing the AST */
public class PrintVisitor extends Visitor {
    /** Current indentation */
    private int indent;
    /** Indentation size */
    private int indentSize;

    /** PrintVisitor constructor
      * @param indent starting indentation
      * @param indentSize indentation size
      * */
    public PrintVisitor(int indent, int indentSize) {
	this.indent = indent;
	this.indentSize = indentSize;
    }

    /** Print indentation
      * */
    private void indent() {
	for (int i = 0; i < indent; i++) System.out.print(" ");
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(Class_ node) {
	indent();
	System.out.println("// Source file: " + node.getFilename());
	indent();
	System.out.println("// Source line: " + node.getLineNum());
	indent();
	System.out.println("class " + node.getName() + " extends " + 
			   node.getParent() + " {");	
	indent += indentSize;
	node.getMemberList().accept(this);
	indent -= indentSize;
	indent();
	System.out.println("}");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(Field node) {
	indent();
	System.out.println("// Source line: " + node.getLineNum());
	indent();
	if (node.getInit() == null)
	    System.out.println(node.getType() + " " + node.getName() + ";");
	else {
	    System.out.println(node.getType() + " " + node.getName() + " = ");
	    node.getInit().accept(this);
	    System.out.println(";");
	}
	return null;
    }
    
    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(Method node) {
	indent();
	System.out.println("// Source line: " + node.getLineNum());
	indent();
	System.out.print(node.getReturnType() + " " + node.getName() + "(");
	node.getFormalList().accept(this);
	System.out.println(") {");
	indent += indentSize;
	node.getStmtList().accept(this);
	indent -= indentSize;
	indent();
	System.out.println("}");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(FormalList node) {
	Iterator iter = node.getIterator();
	for (int i = 0; iter.hasNext(); i++) {
	    ((Formal)iter.next()).accept(this);
	    if (i < node.getSize() - 1)
		System.out.print(", ");
	}
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(Formal node) {
	System.out.print("/*line:" + node.getLineNum() + "*/" + node.getType() + " " + node.getName());
	return null;
    }
    
    /** Print the line number of the statement
      * @param node statement node
      * */
    private void printStmtMeta(Stmt node) {
	indent();
	System.out.println("// Source line: " + node.getLineNum());
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(DeclStmt node) {
	printStmtMeta(node);
	indent();
	// note: init can't be null as it can with fields
	System.out.print(node.getType() + " " + node.getName() + " = ");
	node.getInit().accept(this);
	System.out.println(";");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(ExprStmt node) {
	printStmtMeta(node);
	indent();
	node.getExpr().accept(this);
	System.out.println(";");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(IfStmt node) {
	printStmtMeta(node);
	indent();
	System.out.print("if (");
	node.getPredExpr().accept(this);
	System.out.println(")");
	indent += indentSize;
	node.getThenStmt().accept(this);
	indent -= indentSize;
	indent();
	System.out.println("else");
	indent += indentSize;
	node.getElseStmt().accept(this);
	indent -= indentSize;
	return null;
    }
    
    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(WhileStmt node) {
	printStmtMeta(node);
	indent();
	System.out.print("while (");
	node.getPredExpr().accept(this);
	System.out.println(")");
	indent += indentSize;
	node.getBodyStmt().accept(this);
	indent -= indentSize;
	return null;
    }
    
    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(ForStmt node) {
	printStmtMeta(node);
	indent();
	System.out.print("for (");
	if (node.getInitExpr() != null)
	    node.getInitExpr().accept(this);
	System.out.print("; ");
	if (node.getPredExpr() != null)
	    node.getPredExpr().accept(this);
	System.out.print("; ");
	if (node.getUpdateExpr() != null)
	    node.getUpdateExpr().accept(this);
	System.out.println(")");
	indent += indentSize;
	node.getBodyStmt().accept(this);
	indent -= indentSize;
	return null;
    }
    
    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BreakStmt node) {
	printStmtMeta(node);
	indent();
	System.out.println("break;");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BlockStmt node) {
	printStmtMeta(node);
	indent();
	System.out.println("{");
	indent += indentSize;
	node.getStmtList().accept(this);
	indent -= indentSize;
	indent();
	System.out.println("}");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(ReturnStmt node) {
	indent();
	System.out.println("// Source line: " + node.getLineNum());
	indent();
	if (node.getExpr() == null)
	    System.out.println("return;");
	else {
	    System.out.print("return ");
	    node.getExpr().accept(this);
	    System.out.println(";");
	}
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(ExprList node) {
	Iterator iter = node.getIterator();
	for (int i = 0; iter.hasNext(); i++) {
	    ((ASTNode)iter.next()).accept(this);
	    if (i < node.getSize() - 1)
		System.out.print(", ");
	}
	return null;
    }

    /** Print the line number and type of the expression
      * @param node expression node
      * */
    private void printExprMeta(Expr node) {
	if (node.getExprType() == null)
	    System.out.print("/*L" + node.getLineNum() + "*/");
	else
	    System.out.print("/*L" + node.getLineNum() + ",type:" + node.getExprType() + "*/");
    }
    
    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(DispatchExpr node) {
	System.out.print("(");
	printExprMeta(node);
	node.getRefExpr().accept(this);  // never null
	System.out.print("." + node.getMethodName() + "(");
	node.getActualList().accept(this);
	System.out.print("))");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(NewExpr node) {
	System.out.print("(");
	printExprMeta(node);
	System.out.print("new " + node.getType() + "())");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(NewArrayExpr node) {
	System.out.print("(");
	printExprMeta(node);
	System.out.print("new " + node.getType() + "[");
	node.getSize().accept(this);
	System.out.print("])");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(InstanceofExpr node) {
	System.out.print("(");
	printExprMeta(node);
	node.getExpr().accept(this);
	System.out.print(" instanceof " + node.getType() + ")");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(CastExpr node) {
	System.out.print("(");
	printExprMeta(node);
	System.out.print("(" + node.getType() + ")(");
	node.getExpr().accept(this);
	System.out.print("))");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(AssignExpr node) {
	System.out.print("(");
	printExprMeta(node);
	if (node.getRefName() != null)
	    System.out.print(node.getRefName() + ".");
	System.out.print(node.getName() + " = ");
	node.getExpr().accept(this);
	System.out.print(")");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(ArrayAssignExpr node) {
	System.out.print("(");
	printExprMeta(node);
	if (node.getRefName() != null)
	    System.out.print(node.getRefName() + ".");
	System.out.print(node.getName() + "[");
	node.getIndex().accept(this);
	System.out.print("] = ");
	node.getExpr().accept(this);
	System.out.print(")");
	return null;
    }

    /** Print a binary expression node
      * @param node binary expression node
      * */
    public void printBinaryExpr(BinaryExpr node) {
	System.out.print("(");
	printExprMeta(node);
	node.getLeftExpr().accept(this);
	System.out.print(" " + node.getOpName() + " ");
	node.getRightExpr().accept(this);
	System.out.print(")");
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryCompEqExpr node) {
	printBinaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryCompNeExpr node) {
	printBinaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryCompLtExpr node) {
	printBinaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryCompLeqExpr node) {
	printBinaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryCompGtExpr node) {
	printBinaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryCompGeqExpr node) {
	printBinaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryArithPlusExpr node) {
	printBinaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryArithMinusExpr node) {
	printBinaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryArithTimesExpr node) {
	printBinaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryArithDivideExpr node) {
	printBinaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryArithModulusExpr node) {
	printBinaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryLogicAndExpr node) {
	printBinaryExpr(node);
	return null;
    }

   /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(BinaryLogicOrExpr node) {
	printBinaryExpr(node);
	return null;
    }
        
    /** Print a unary expression node
      * @param node unary expression node
      * */
    public void printUnaryExpr(UnaryExpr node) {
	System.out.print("(");
	printExprMeta(node);
	if (!node.isPostfix())
	    System.out.print(node.getOpName());
	System.out.print("(");
	node.getExpr().accept(this);
	System.out.print(")");
	if (node.isPostfix())
	    System.out.print(node.getOpName());
	System.out.print(")");
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(UnaryNegExpr node) {
	printUnaryExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(UnaryNotExpr node) {
	printUnaryExpr(node);
	return null;
    }
    
    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(UnaryIncrExpr node) {
	printUnaryExpr(node);
	return null;
    }
    
    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(UnaryDecrExpr node) {
	printUnaryExpr(node);
	return null;
    }
    
    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(VarExpr node) {
	/*
	System.out.print("(");
	printExprMeta(node);
	if (node.getRef() != null)
	    node.getRef().accept(this);
	System.out.print("." + node.getName() + ")");
	return null;
	*/
	System.out.print("(");
	printExprMeta(node);
	if (node.getRef() != null) {
	    node.getRef().accept(this);
	    System.out.print(".");
	}
	System.out.print(node.getName() + ")");
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(ArrayExpr node) {
	System.out.print("(");
	printExprMeta(node);
	if (node.getRef() != null) {
	    node.getRef().accept(this);
	    System.out.print(".");
	}
	System.out.print(node.getName() + "[");
	node.getIndex().accept(this);
	System.out.print("])");
	return null;
    }

    /** Print constant expression node
      * @param node constant expression node
      * */
    public void printConstExpr(ConstExpr node) {
	String constant = node.getConstant();
	System.out.print("(");
	printExprMeta(node);
	if (node instanceof ConstStringExpr) {
	    System.out.print("\"");
	    for (int i = 0; i < constant.length(); i++) {
		if (constant.charAt(i) == '\n')
		    System.out.print("\\n");
		else if (constant.charAt(i) == '\t')
		    System.out.print("\\t");
		else if (constant.charAt(i) == '\r')
		    System.out.print("\\r");
		else if (constant.charAt(i) == '\f')
		    System.out.print("\\f");
		else
		    System.out.print(constant.charAt(i));
	    }
	    System.out.print("\"");
	}
	else
	    System.out.print(constant);
	System.out.print(")");
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(ConstIntExpr node) {
	printConstExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(ConstBooleanExpr node) {
	printConstExpr(node);
	return null;
    }

    /** Print AST node
      * @param node AST node
      * @return null (returns value to satisfy compiler)
      * */
    public Object visit(ConstStringExpr node) {
	printConstExpr(node);
	return null;
    }
}
