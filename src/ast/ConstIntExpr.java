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


/** The <tt>ConstIntExpr</tt> class represents an integer constant expression.  
  * It extends constant expressions so it contains a constant value (represented
  * as a String).  It also stores the constant as an int (<tt>intConstant</tt>).
  * Since this class is similar to other subclasses most of the functionality can 
  * be implemented in the visitor method for the parent class.
  * @see ASTNode
  * @see ConstExpr
  * */
public class ConstIntExpr extends ConstExpr {
    /** The constant value represented as an int */
    private int intConstant;
    
    /** ConstIntExpr constructor
      * @param lineNum source line number corresponding to this AST node
      * @param constant constant value (as a String)
      * */
    public ConstIntExpr(int lineNum, String constant) { 
	super(lineNum, constant);
	intConstant = Integer.parseInt(constant);
    }
    
    /** Get the constant value represented as an int 
      * @return the constant value 
      * */
    public int getIntConstant() {
	return intConstant;
    }

    /** Visitor method
      * @param v visitor object
      * @return result of visiting this node
      * @see visitor.Visitor
      * */
    public Object accept(Visitor v) {
	return v.visit(this);
    }
}
