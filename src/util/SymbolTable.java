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

package util;

import java.util.*;

/** Class for representing a class symbol table */
public class SymbolTable {
    /** Hash table that maps strings to Objects.  The object value corresponds to
      * the type of the variable or method.  For variables it will be a String and
      * for methods it will be an AST node.
      * */
    private Hashtable<String,Object> hash;
    /** List that holds each scope */
    private Vector<Hashtable<String,Object>> scopes;
    /** Parent class symbol table (may be null) 
      * If lookup fails in this symbol table should lookup in parent
      * */
    private SymbolTable parent;

    /** Create an empty symbol table */
    public SymbolTable() {
	hash = null;
	scopes = new Vector<Hashtable<String,Object>>();
	parent = null;
    }

    /** Set the parent symbol table 
      * @param parent symbol table of the parent class 
      * */
    public void setParent(SymbolTable parent) {
	this.parent = parent;
    }

    /** Enter a new scope 
      * */
    public void enterScope() {
	hash = new Hashtable<String,Object>();
	scopes.add(hash);
    }

    /** Exit a scope 
      * */
    public void exitScope() {
	if (scopes.size() == 0)
	    throw new RuntimeException("No scope to exit");
	scopes.removeElementAt(scopes.size()-1);
	if (scopes.size() > 0)
	    hash = scopes.elementAt(scopes.size()-1);
	else
	    hash = null;
    }

    /** Adds a symbol to the symbol table if one does not already exist
      * Sets the value of the symbol to the specified parameter
      * @param s symbol name (i.e., name of variable or method)
      * @param value value of symbol (i.e., type)
      * */
    public void add(String s, Object value) {
	if (scopes.size() == 0)
	    throw new RuntimeException("Must enter a scope before adding to table");
	hash.put(s, value);
    }

    /** Looks up a symbol in any scope in the symbol table
      * @param s string of symbol to lookup
      * @return value of symbol (i.e., type) */
    public Object lookup(String s) {
	if (scopes.size() == 0)
	    throw new RuntimeException("Must enter a scope before looking up in table");
	
	for (int i = scopes.size()-1; i >= 0; i--) {
	    Hashtable<String,Object> h = scopes.elementAt(i);
	    Object value = h.get(s);
	    if (value != null)
		return value;
	}

	if (parent != null)
	    return parent.lookup(s);
	return null;
    }

    /** Looks up a symbol in the current scope in the table
      * @param s string of symbol to lookup
      * @return value of symbol (i.e., type)
      * */
    public Object peek(String s) {
	if (scopes.size() == 0)
	    throw new RuntimeException("Must enter a scope before peeking in table");
	return hash.get(s);
    }

    /** Gets scope level of a symbol in the table 
      * (<0 means symbol not in table)
      * @param s string of symbol to lookup
      * @return scope level 
      * */
    public int getScopeLevel(String s) {
	if (scopes.size() == 0)
	    throw new RuntimeException("Must enter a scope before looking up in table");
	
	for (int i = scopes.size()-1; i >= 0; i--) {
	    Hashtable<String,Object> h = scopes.elementAt(i);
	    if (h.get(s) != null)
		return (i + 1) + parent.getCurrScopeLevel();
	}

	if (parent != null)
	    return parent.getScopeLevel(s);

	return -1;
    }

    /** Gets the number of entries in all scopes of the symbol table
      * Note: includes inherited scopes
      * @return size of current scope
      * */
    public int getSize() {
	int size = 0;

	for (int i = 0; i < scopes.size(); i++)
	    size += ((Hashtable<String,Object>)scopes.elementAt(i)).size();

	if (parent != null)
	    return parent.getSize() + size;
	return size;
    }

    /** Gets the number of entries in the current scope of the symbol table
      * @return size of current scope
      * */
    public int getCurrScopeSize() {
	if (hash != null)
	    return hash.size();
	else
	    return 0;
    }

    /** Gets the current scope level of the symbol table 
      * (first scope starts at 1)
      * @return current scope level
      * */
    public int getCurrScopeLevel() {
	if (parent != null)
	    return scopes.size() + parent.getCurrScopeLevel();
	return scopes.size();
    }
}
