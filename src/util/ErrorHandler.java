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

/** The <tt>ErrorHandler</tt> class performs error handling.
  * */
public class ErrorHandler {
    /** Lexical error constant - use to indicate the type of error */
    public final int LEX_ERROR = 0;
    /** Parse error constant - use to indicate the type of error */
    public final int PARSE_ERROR = 1;
    /** Semantic error constant - use to indicate the type of error */
    public final int SEMANT_ERROR = 2;

    /** The number of errors registered */
    private int numErrors;
    /** The list of errors */
    private Vector<Error> errorList = new Vector<Error>();

    /** Register an error - auxiliarly method used by the other (public) register methods
      * @param error the error object
      * */
    private void register(Error error) {
	// insert a new error into the error list
	insert(error);
	// update numErrors, if 200 then call checkErrors (which will halt)
	if (++numErrors > 199)
	    checkErrors();
    }

    /** Register an error
      * @param type the type (lex, parse, semantic) of error
      * @param filename the name of the filename where the error occurred
      * @param lineNum the starting line number in the source file where the error occurred
      * @param errorMessage the error message
      * */
    public void register(int type, String filename, int lineNum, String errorMessage) {
	// create error and register it
	register((new Error(type, filename, lineNum, errorMessage)));
    }

    /** Register an error
      * @param type the type (lex, parse, semantic) of error
      * @param errorMessage the error message
      * */
    public void register(int type, String errorMessage) {	
	// create error and register it
	// note: filename, line number, line, and charNum not specified 
	// so using null, 0, null, and 0, respectively
	register((new Error(type, null, -1, errorMessage)));
    }

    /** Check the errors - halts if there are any registered errors
      * */
    public void checkErrors() {
	// if errors have been registered then print them and exit
	if (numErrors > 0) {
	    printErrors();
	    System.err.println("Stopping compilation due to errors");
	    System.err.println(numErrors + " error(s)");
	    System.exit(1);
	}
    }

    /** Insert an error onto the error list
      * @param e error object to insert
      * */
    private void insert(Error e) {
	// the algorithm below will insert errors in order by filename first and
	// then line number.  filenames are kept in the order that they are seen
	// (i.e., an error is registered with that filename).  line numbers are
	// ordered numerically.

	int i = 0;

	if (e.getFilename() != null) {
	    // find the section of the list with the same filename (goes to the end
	    // if a section is not found)
	    for (i = 0; i < errorList.size(); i++) {
		if (errorList.elementAt(i).getFilename() != null) {
		    if (e.getFilename().equals(errorList.elementAt(i).getFilename()))
			break;
		}
	    }

	    // find the correct spot in the section for this error's line number
	    for (; i < errorList.size(); i++) {
		if (!e.getFilename().equals(errorList.elementAt(i).getFilename()) ||
		    e.getLineNum() < (errorList.elementAt(i)).getLineNum())
		    break;
	    }
	}

	// add the error to the list
	errorList.add(i, e);
    }

    /** Print the error messages
      * */
    private void printErrors() {
	// traverse the error list
	for (int i = 0; i < errorList.size(); i++) {
	    // get the next error
	    Error e = errorList.elementAt(i);

	    // print error message
	    if (e.getFilename() == null)
		System.err.println("Error: " + getTypeString(e.getType()) + e.getMessage());
	    else
		System.err.println(e.getFilename() + ":" + e.getLineNum() + ":" + 
				   getTypeString(e.getType()) + e.getMessage());
	    System.err.println();
	}
    }

    /** Get the type string (lex, parse, semantic, none)
      * @return string representing the type of error
      * */
    private String getTypeString(int type) {
	if (type == LEX_ERROR)
	    return "lexical error: ";
	else if (type == PARSE_ERROR)
	    return "syntactic error: ";
	else if (type == SEMANT_ERROR)
	    return "semantic error: ";
	else
	    return "";
    }

    /** Class for representing errors 
     * */
    class Error {
	/** Type of an error (lex, parse, semantic) */
	private int type;
	/** File name where the error occurred */
	private String filename;
	/** Line number in the source file where the error occurred */
	private int lineNum;
	/** Source line where the error occurred */
	private String line;
	/** Starting character number in the source line where the error occurred */
	private int charNum;
	/** Error message */
	private String message;
	
	/** Error constructor
	  * @param type the type of error (lex, parse, semantic)
	  * @param filename file name where the error occurred
	  * @param lineNum line number where the error occurred
	  * @param message error message
	  * */
	public Error(int type, String filename, int lineNum, String message) {
	    this.type = type;
	    this.filename = filename;
	    this.lineNum = lineNum;
	    this.message = message;
	}
	
	/** Get the type of error (lex, parse, semantic)
	  * @return the type of error 
	  * */
	public int getType() { return type; }
	/** Get the file name where the error occurred
	  * @return the file name
	  * */
	public String getFilename() { return filename; }
	/** Get the line number in the source file where the error occurred
	  * @return the line number
	  * */
	public int getLineNum() { return lineNum; }
	/** Get the error message
	  * @return the error message
	  * */
	public String getMessage() { return message; }
    }
}
