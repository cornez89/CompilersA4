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

import java.util.*;

/** The <tt>InstanceofExpr</tt> class represents an instanceof expression.  
  * It contains a lefthand expression (<tt>expr</tt>) and a righthand type 
  * name (<tt>type</tt>).
  * @see ASTNode
  * @see Expr
  * */
public class InstanceofExpr extends Expr {
    /** The lefthand expression */
    protected Expr expr;
    
    /** The righthand type */
    protected String type;
    
    /** InstanceofExpr constructor
      * @param lineNum source line number corresponding to this AST node
      * @param expr the lefthand expression
      * @param type the righthand type
      * */
    public InstanceofExpr(int lineNum, Expr expr, String type) {
	super(lineNum);
	this.expr = expr;
	this.type = type;
    }
    
    /** Get the lefthand expression
      * @return expression
      * */
    public Expr getExpr() { return expr; }
    
    /** Get the righthand type
      * @return type
      * */
    public String getType() { return type; }
    
    /** Visitor method
      * @param v visitor object
      * @return result of visiting this node
      * @see visitor.Visitor
      * */
    public Object accept(Visitor v) {
	return v.visit(this);
    }
}
