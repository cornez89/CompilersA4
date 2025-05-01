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

package ast;

import visitor.*;


/** The <tt>ArrayAssignExpr</tt> class represents array assignment expressions.
  * It contains an optional reference name (potentially needed if the
  * variable is a field), an array name (array being assigned to), an
  * index expression (index into assigned array), and an expression (for 
  * assigning to the array).
  * @see ASTNode
  * @see Expr
  * */
public class ArrayAssignExpr extends Expr {
    /** The optional reference object used to access the lefthand variable
	(only applicable if the variable is a field) */
    protected String refName;

    /** The name of the lefthand variable */
    protected String name;
    
    /** The index expression */
    protected Expr index;
    
    /** The righthand expression for assigning to the lefthand variable */
    protected Expr expr;
    
    /** ArrayAssignExpr constructor
      * @param lineNum source line number corresponding to this AST node
      * @param refName the optional reference object used to access the lefthand variable
      * @param name the name of the lefthand variable
      * @param index index expression
      * @param expr righthand expression for assigning to the lefthand variable
      * */
    public ArrayAssignExpr(int lineNum, String refName, String name, Expr index, Expr expr) {
	super(lineNum);
	this.refName = refName;
	this.name = name;
	this.index = index;
	this.expr = expr;
    }
    
    /** Get the optional reference name
      * @return reference name
      * */
    public String getRefName() { return refName; }
    
    /** Get the lefthand variable name
      * @return lefthand variable name
      * */
    public String getName() { return name; }

    /** Get the index expression
      * @return index expression 
      * */
    public Expr getIndex() { return index; }
    
    /** Get the righthand expression of the assignment
      * @return righthand expression 
      * */
    public Expr getExpr() { return expr; }
    
    /** Visitor method
      * @param v visitor object
      * @return result of visiting this node
      * @see visitor.Visitor
      * */
    public Object accept(Visitor v) {
	return v.visit(this);
    }
}
