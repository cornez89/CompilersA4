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


/** The <tt>NewArrayExpr</tt> class represents a new array expression (constructing
  * new arrays).  It contains a type name (<tt>type</tt>) to be 
  * constructed and a size expression (<tt>size</tt>).
  * @see ASTNode
  * @see Expr
  * */
public class NewArrayExpr extends Expr {
    /** The type of the constructed array */
    protected String type;

    /** The size of the constructed array */
    protected Expr size;
    
    /** NewArrayExpr constructor
      * @param lineNum source line number corresponding to this AST node
      * @param type the type of the constructed array
      * @param size the size of the constructed array
      * */
    public NewArrayExpr(int lineNum, String type, Expr size) {
	super(lineNum);
	this.type = type;
	this.size = size;
    }
    
    /** Get the type of the constructed array
      * @return type
      * */
    public String getType() { return type; }
    
    /** Get the construction size expression
      * @return size expression
      * */
    public Expr getSize() { return size; }
    
    /** Visitor method
      * @param v visitor object
      * @return result of visiting this node
      * @see visitor.Visitor
      * */
    public Object accept(Visitor v) {
	return v.visit(this);
    }
}
