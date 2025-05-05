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

import parser.*;
import semant.*;
import codegenjvm.*;
import util.*;
import ast.*;
import visitor.*;

/** Main class that runs the Bantam compiler
  * Constructs and runs each phase of the compiler
  * (lexing, parsing, semantic analysis, and code generation).
  * */
public class Main {
    /** Array for holding each input file name */
    private static String[] inFiles;

    /** Boolean flags that indicate whether we should stop after 
      * a particular phase.  If turned on Main prints out an 
      * intermediate representation of the phase before exiting */
    private static boolean stopAfterLexing, stopAfterParsing, stopAfterSemant;

    /** Debugging flags for each phase of the compiler */
    private static boolean debugLexer, debugParser, debugSemant, debugCodeGen;


    /** Integer indicating target (2 = jvm by default) */
    private static int targetType = 2;

    /** Constant for MIPS target */
    private static final int TARG_MIPS = 0;
    /** Constant for x86 target */
    private static final int TARG_X86 = 1;
    /** Constant for JVM target */
    private static final int TARG_JVM = 2;

    /** Prints out a usage message to the screen */
    private static void showHelp() {
	System.err.println("Usage: bantamc [-h]");
	System.err.println("               [-dp] [-ds] [-dc] [-sp] [-ss] <input_files>");
	System.err.println("man bantamc for more details");
	System.exit(1);
    }

    /** Get target name 
      * Converts targetType (global) into the target name
      * @return name of target
      * */
    private static String getTargName() {
	if (targetType == TARG_MIPS) return "mips";
	else if (targetType == TARG_X86) return "x86";
	else if (targetType == TARG_JVM) return "jvm";
	else throw new RuntimeException("Internal error: bad target type (" + 
					targetType + ") in Main.getTargName");
    }

    /** Processes the commandline flags, setting appropriate variables
      * @param args list of commandline arguments
      * */
    private static void processFlags(String[] args) {
	// initialize inFiles to size of args, will probably be smaller, but args length gives upper bound
	inFiles = new String[args.length];
	// cnt represents the number of input files found - initialize to 0
	int cnt = 0;

	// if no arguments then call showHelp (which eventually exits)
	if (args.length == 0)
	    showHelp();

	// otherwise inspect the arguments
	for (int i = 0; i < args.length; i++) {
	    // if '-h' or help then call showHelp (which eventually exits)
	    if (args[i].equals("-h"))
		showHelp();

	    // if -dp, -ds, -dc then set corresponding boolean to enable debugging for that phase
	    else if (args[i].equals("-dp"))
		debugParser = true;
	    else if (args[i].equals("-ds"))
		debugSemant = true;
	    else if (args[i].equals("-dc"))
		debugCodeGen = true;

	    // if -sp, -ss, then set corresponding boolean to stop compilation after that phase
	    else if (args[i].equals("-sp"))
		stopAfterParsing = true;
	    else if (args[i].equals("-ss"))
		stopAfterSemant = true;


	    // any other arguments must be input files
	    
	    // check if argument ends in .btm
	    else if (args[i].length() >= 5 && args[i].substring(args[i].length()-4).equals(".btm")) {
		// if so then set next entry in inFiles
		inFiles[cnt++] = args[i];
	    }

	    else {
		// if we get to here then we have an illegal argument
		// (we treat this as a bad input file name)
		System.err.println("Usage error: bad input file name: " + args[i]);
		System.err.println("             file names must end with '.btm'");
		showHelp();
	    }
	}

	// make sure at least one input file was specified
	if (cnt == 0) {
	    System.err.println("Usage error: must specify some input files");
	    showHelp();
	}

	// resize inFiles to the number of specified input files (i.e., cnt)
	String[] tmp = inFiles;
	inFiles = new String[cnt];
	for (int i = 0; i < cnt; i++)
	    inFiles[i] = tmp[i];
    }

    /** Main method, which drives compilation
      * builds and runs each phase of the compiler
      * @param args list of commandline arguments 
      * */
    public static void main(String [] args) {
	// process flags
	processFlags(args);

	try {

	    // parsing
	    Parser parser = new Parser(inFiles, debugParser);
	    Program program = parser.parse();

	    if (stopAfterParsing) {
		// if stopAfterParsing==true, then print AST and exit
		PrintVisitor visitor = new PrintVisitor(/*start at indent 0*/0, 
							/*increment by 4 each indent level*/4);
		visitor.visit( program );
		System.exit(0);
	    }
	   
	    // semantic analysis
	    SemanticAnalyzer semanticAnalyzer = 
		new SemanticAnalyzer(program, debugSemant);
	    ClassTreeNode classTree = semanticAnalyzer.analyze();

	    if (stopAfterSemant) {
		// if stopAfterSemant==true, then print AST (with types) and exit
		PrintVisitor visitor = new PrintVisitor(/*start at indent 0*/0, 
							/*increment by 4 each indent level*/4);
		visitor.visit(program);
		System.exit(0);
	    }

	    else // (targetType == TARG_JVM) 
            {
		JVMCodeGenerator codeGenerator = 
		    new JVMCodeGenerator(classTree, 
					 debugCodeGen);
		codeGenerator.generate();
	    }
	}

	catch (Exception e) {
	    e.printStackTrace();
	    System.err.println("Internal error within compiler: stopping compilation");
	    System.exit(1);
	}
    }
}
