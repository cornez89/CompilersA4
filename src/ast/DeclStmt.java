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


/** The <tt>DeclStmt</tt> class represents a variable declaration statement 
  * appearing in a method declaration.  It contains a variable type (<tt>type</tt>), 
  * a name (<tt>name</tt>), and an (non-optional) initialization expression
  * (<tt>init</tt>).
  * @see ASTNode
  * @see Stmt
  * */
public class DeclStmt extends Stmt {
    /** The type of the variable being declared */
    protected String type;
    
    /** The name of the variable being declared */
    protected String name;
    
    /** The (non-optional) initialization expression for the variable being declared */
    protected Expr init;
    
    /** DeclStmt constructor
      * @param lineNum source line number corresponding to this AST node
      * @param type the symbol representing the type of the variable being declared
      * @param name the symbol representing the name of the variable being declared
      * @param init the (non-optional) initialization expression for the declared variable
      * */
    public DeclStmt(int lineNum, String type, String name, Expr init) {
	super(lineNum);
	this.type = type;
	this.name = name;
	this.init = init;
    }
    
    /** Get the type of the declared variable
      * @return declared variable type
      * */
    public String getType() { return type; }
    
    /** Get the name of the declared variable
      * @return declared variable name
      * */
    public String getName() { return name; }
    
    /** Get the initialization expression for the declared variable
      * @return initialization expression
      * */
    public Expr getInit() { return init; }
    
    /** Visitor method
      * @param v visitor object
      * @return result of visiting this node
      * @see visitor.Visitor
      * */
    public Object accept(Visitor v) {
	return v.visit(this);
    }
}
