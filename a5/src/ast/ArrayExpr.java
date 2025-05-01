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


/** The <tt>ArrayExpr</tt> class represents array expressions.
  * It contains the name of the variable and an index expression.  Note: 
  * these may or may not include a reference object name (if the array is a 
  * field).  Because fields are 'protected' in Bantam Java, the reference
  * object name must always be either 'this' or 'super'.
  * @see ASTNode
  * @see Expr
  * */
public class ArrayExpr extends Expr {
    /** The optional reference object (must be 'this' or 'super') */
    protected Expr ref;

    /** The name of the variable (possibly 'this', 'super', or 'null') */
    protected String name;
    
    /** The index expression */
    protected Expr index;
    
    /** ArrayExpr constructor
      * @param lineNum source line number corresponding to this AST node
      * @param ref the optional reference object (must be 'this' or 'super')
      * @param name the name of the variable
      * @param index the index expression
      * */
    public ArrayExpr(int lineNum, Expr ref, String name, Expr index) {
	super(lineNum);
	this.ref = ref;
	this.name = name;
	this.index = index;
    }
    
    /** Get the reference object 
      * Only applicable if array is a field (otherwise this returns null)
      * @return reference object
      * */
    public Expr getRef() { return ref; }
    
    /** Get the name of the array
      * @return name
      * */
    public String getName() { return name; }
    
    /** Get the index expression
      * @return index expression
      * */
    public Expr getIndex() { return index; }
    
    /** Visitor method
      * @param v visitor object
      * @return result of visiting this node
      * @see visitor.Visitor
      * */
    public Object accept(Visitor v) {
	return v.visit(this);
    }
}
