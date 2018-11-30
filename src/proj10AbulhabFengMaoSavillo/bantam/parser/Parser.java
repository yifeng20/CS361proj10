/*
 * Authors: Haoyu Song and Dale Skrien
 * Date: Spring and Summer, 2018
 *
 * In the grammar below, the variables are enclosed in angle brackets.
 * The notation "::=" is used instead of "-->" to separate a variable from its rules.
 * The special character "|" is used to separate the rules for each variable.
 * All other symbols in the rules are terminals.
 * EMPTY indicates a rule with an empty right hand side.
 * All other terminal symbols that are in all caps correspond to keywords.
 */

package proj10AbulhabFengMaoSavillo.bantam.parser;

import proj10AbulhabFengMaoSavillo.bantam.lexer.*;
import proj10AbulhabFengMaoSavillo.bantam.util.*;

import static proj10AbulhabFengMaoSavillo.bantam.lexer.Token.Kind.*;

import proj10AbulhabFengMaoSavillo.bantam.ast.*;
import proj10AbulhabFengMaoSavillo.bantam.util.Error;


/**
 * This class constructs an AST from a legal Bantam Java program.  If the
 * program is illegal, then one or more error messages are displayed.
 */
public class Parser
{
    // instance variables
    private Scanner scanner;
    private Token currentToken; // the lookahead token
    private ErrorHandler errorHandler;
    private String filename;

    // constructor
    public Parser(ErrorHandler errorHandler)
    {
        this.errorHandler = errorHandler;
        scanner = new Scanner(errorHandler);
    }

    /**
     * Eats comments
     */
    private void advancePastCommentary()
    {
        while (this.currentToken.kind == COMMENT)
        {
            this.currentToken = this.scanner.scan();
        }
    }

    /**
     * Test code for Parser
     */
    public static void main(String[] args)
    {
        //make sure at least one filename was given
        if (args.length < 1)
        {
            System.err.println("Missing input filename");
            System.exit(-1);
        }

        //for each file given, scan
        ErrorHandler errorHandler = new ErrorHandler();
        for (String filename : args)
        {
            System.out.println("Scanning file: " + filename + "\n");

            //scan tokens
            try
            {
//				Scanner scanner = new Scanner(filename, errorHandler);
//				Token currentToken = scanner.scan();
//				while (currentToken.kind != Token.Kind.EOF)
//				{
//					System.out.println(currentToken);
//					currentToken = scanner.scan();
//				}
                Parser parser = new Parser(errorHandler);
                parser.parse(filename);
            }
            catch (CompilationException e)
            {
                errorHandler.register(Error.Kind.LEX_ERROR, "Failed to read in source file");
            }

            //check for errors
            if (errorHandler.errorsFound())
            {
                System.out.println(String.format("\n%d errors found", errorHandler.getErrorList().size()));
            }
            else
            {
                System.out.println("\nScanning was successful");
            }

            System.out.println("-----------------------------------------------");

            //clear errors to scan next file
            errorHandler.clear();
        }
    }

    /**
     * Parsing Error occurred!
     *
     * @param errorMessage
     * @throws CompilationException
     */
    private void whinge(String errorMessage)
            throws CompilationException
    {
        this.errorHandler.register(Error.Kind.PARSE_ERROR,
                                   this.filename,
                                   this.currentToken.position,
                                   errorMessage);
        throw new CompilationException(String.format("Compilation Exception: %s", errorMessage));
    }

    /**
     * parse the given file and return the root node of the AST
     *
     * @param filename The name of the Bantam Java file to be parsed
     * @return The Program node forming the root of the AST generated by the parser
     */
    public Program parse(String filename)
    {
        this.scanner.setSourceFile(filename);
        this.currentToken = scanner.scan();
        this.filename = filename;
        return (parseProgram());
    }


    /*
     * <Program> ::= <Class> | <Class> <Program>
     */
    private Program parseProgram()
    {
        this.advancePastCommentary();
        int position = this.currentToken.position;
        ClassList classList = new ClassList(position);

        while (currentToken.kind != EOF)
        {
            Class_ aClass = parseClass();
            classList.addElement(aClass);
        }

        return new Program(position, classList);
    }


    /*
     * <Class> ::= CLASS <Identifier> <ExtendsClause> { <MemberList> }
     * <ExtendsClause> ::= EXTENDS <Identifier> | EMPTY
     * <MemberList> ::= EMPTY | <Member> <MemberList>
     */
    private Class_ parseClass()
    {
        this.advancePastCommentary();
        int initialPosition = this.currentToken.position;
        String className = "";
        String parent = "";
        MemberList memberList = new MemberList(initialPosition);

        if (this.currentToken.kind == CLASS)
        {
            this.currentToken = this.scanner.scan();
            if (this.currentToken.kind == IDENTIFIER)
            {
                className = this.currentToken.spelling;
            }
            else
            {
                //TODO error
            }

            this.currentToken = scanner.scan();
            if (this.currentToken.kind == EXTENDS)
            {
                this.currentToken = scanner.scan();
                if (this.currentToken.kind == IDENTIFIER)
                {
                    parent = this.currentToken.spelling;
                    this.currentToken = scanner.scan();
                }
                else
                {
                    //TODO error
                }
            }

            if (this.currentToken.kind != LCURLY)
            {
                this.whinge("Expecting left brace.");
            }

            while (this.currentToken.kind != RCURLY)
            {
                Member member = parseMember();
                memberList.addElement(member);
            }

            this.currentToken = this.scanner.scan();

            Class_ class_ = new Class_(initialPosition, this.filename, className, parent, memberList);
            System.out.println(class_.getFilename());
            System.out.println(class_.getName());
            System.out.println(class_.getParent());
            System.out.println(class_.getMemberList());

        }

        return new Class_(initialPosition, className, className, parent, memberList);
    }

    /* Fields and Methods
     * <Member> ::= <Field> | <Method>
     * <Method> ::= <Type> <Identifier> ( <Parameters> ) <Block>
     * <Field> ::= <Type> <Identifier> <InitialValue> ;
     * <InitialValue> ::= EMPTY | = <Expression>
     */
     private Member parseMember()
     {
    	 int position = currentToken.position;
    	 String type = this.parseType();
    	 String nameIdentifier = this.parseIdentifier();
    	 Member member = null;

    	 //if member matches form for a method
    	 if (this.currentToken.kind == LPAREN)
    	 {
    		 scanner.scan();
    		 FormalList params = this.parseParameters();

    		 //check for closing parenthesis
    		 if (this.currentToken.kind != RPAREN)
    		 {
    			 //TODO: error: Missing closing parenthesis.
    		 }
    		 else //if present, move on to next token
    		 {
    			 this.currentToken = scanner.scan();
    		 }

    		 BlockStmt block = (BlockStmt) this.parseBlock();
    		 StmtList blockStmts = block.getStmtList();

    		 member = new Method(position, type, nameIdentifier,
                     				params, blockStmts);
    	 }
    	 else //otherwise parse member as field
    	 {
    		 if (this.currentToken.kind == SEMICOLON)
    		 {
    			 //TODO: is it okay to have null for an empty init value?
    			 //empty initial value
    			 member = new Field(position, type, nameIdentifier, null);
    		 }
    		 else if (this.currentToken.kind == ASSIGN)
    		 {
    			 //non-empty initial value
    			 scanner.scan(); //read past =
    			 Expr initValueExpr = this.parseExpression();

    			 //check for semicolon
    			 if (this.currentToken.kind != SEMICOLON)
    			 {
    				 //TODO: error: Missing ending semicolon.
    			 }
    			 else
    			 {
    				 member = new Field(position, type, nameIdentifier, initValueExpr);
    			 }
    		 }
    		 else
    		 {
    			 //invalid syntax
    			 //TODO: error: Invalid field initialization.
    		 }
    	 }

    	 return member;
     }


    //-----------------------------------

    /* Statements
     *  <Stmt> ::= <WhileStmt> | <ReturnStmt> | <BreakStmt> | <DeclStmt>
     *              | <ExpressionStmt> | <ForStmt> | <BlockStmt> | <IfStmt>
     */
     private Stmt parseStatement() {
            Stmt stmt;

            switch (currentToken.kind) {
                case IF:
                    stmt = parseIf();
                    break;
                case LCURLY:
                    stmt = parseBlock();
                    break;
                case VAR:
                    stmt = parseDeclStmt();
                    break;
                case RETURN:
                    stmt = parseReturn();
                    break;
                case FOR:
                    stmt = parseFor();
                    break;
                case WHILE:
                    stmt = parseWhile();
                    break;
                case BREAK:
                    stmt = parseBreak();
                    break;
                default:
                    stmt = parseExpressionStmt();
            }

            return stmt;
    }


    /*
     * <WhileStmt> ::= WHILE ( <Expression> ) <Stmt>
     */
    private Stmt parseWhile() { }


    /*
     * <ReturnStmt> ::= RETURN <Expression> ; | RETURN ;
     */
	private Stmt parseReturn() { }


    /*
	 * BreakStmt> ::= BREAK ;
     */
	private Stmt parseBreak() { }


    /*
	 * <ExpressionStmt> ::= <Expression> ;
     */
	private ExprStmt parseExpressionStmt() { }


    /*
	 * <DeclStmt> ::= VAR <Identifier> = <Expression> ;
     * every local variable must be initialized
     */
	private Stmt parseDeclStmt() { }


    /*
	 * <ForStmt> ::= FOR ( <Start> ; <Terminate> ; <Increment> ) <STMT>
     * <Start>     ::= EMPTY | <Expression>
     * <Terminate> ::= EMPTY | <Expression>
     * <Increment> ::= EMPTY | <Expression>
     */
	private Stmt parseFor() { }


    /*
	 * <BlockStmt> ::= { <Body> }
     * <Body> ::= EMPTY | <Stmt> <Body>
     */
	private Stmt parseBlock() { }


    /*
	 * <IfStmt> ::= IF ( <Expr> ) <Stmt> | IF ( <Expr> ) <Stmt> ELSE <Stmt>
     */
	private Stmt parseIf() { }


    //-----------------------------------------
    // Expressions
    //Here we introduce the precedence to operations

    /*
	 * <Expression> ::= <LogicalOrExpr> <OptionalAssignment>
     * <OptionalAssignment> ::= EMPTY | = <Expression>
     */
	private Expr parseExpression() { }


    /*
	 * <LogicalOR> ::= <logicalAND> <LogicalORRest>
     * <LogicalORRest> ::= EMPTY |  || <LogicalAND> <LogicalORRest>
     */
	private Expr parseOrExpr() {
        int position = currentToken.position;

        Expr left = parseAndExpr();
        while (this.currentToken.spelling.equals("||")) {
            this.currentToken = scanner.scan();
            Expr right = parseAndExpr();
            left = new BinaryLogicOrExpr(position, left, right);
        }

        return left;
	}
	
    /*
	 * <LogicalAND> ::= <ComparisonExpr> <LogicalANDRest>
     * <LogicalANDRest> ::= EMPTY |  && <ComparisonExpr> <LogicalANDRest>
     */
	private Expr parseAndExpr() {
	}


    /*
	 * <ComparisonExpr> ::= <RelationalExpr> <equalOrNotEqual> <RelationalExpr> |
     *                     <RelationalExpr>
     * <equalOrNotEqual> ::=  == | !=
     */
	private Expr parseEqualityExpr() { }


    /*
	 * <RelationalExpr> ::=<AddExpr> | <AddExpr> <ComparisonOp> <AddExpr>
     * <ComparisonOp> ::=  < | > | <= | >= | INSTANCEOF
     */
	private Expr parseRelationalExpr() { }


    /*
	 * <AddExpr>::＝ <MultExpr> <MoreMultExpr>
     * <MoreMultExpr> ::= EMPTY | + <MultExpr> <MoreMultExpr> | - <MultExpr> <MoreMultExpr>
     */
	private Expr parseAddExpr() { }


    /*
	 * <MultiExpr> ::= <NewCastOrUnary> <MoreNCU>
     * <MoreNCU> ::= * <NewCastOrUnary> <MoreNCU> |
     *               / <NewCastOrUnary> <MoreNCU> |
     *               % <NewCastOrUnary> <MoreNCU> |
     *               EMPTY
     */
	private Expr parseMultExpr() { }

    /*
	 * <NewCastOrUnary> ::= < NewExpression> | <CastExpression> | <UnaryPrefix>
     */
	private Expr parseNewCastOrUnary() { }


    /*
	 * <NewExpression> ::= NEW <Identifier> ( ) | NEW <Identifier> [ <Expression> ]
     */
	private Expr parseNew()
	{ 
		int position = currentToken.position;
		Expr result = null;
		
		scanner.scan();
		String type = this.parseIdentifier();
		
		if (this.currentToken.kind == LPAREN)
		{
			//new object
			result = new NewExpr(position, type);
			
			//check for closing parenthesis
			if (this.currentToken.kind != RPAREN)
			{
				//TODO: error
			}
		}
		else if (this.currentToken.kind == LBRACKET)
		{
			//new array
			
		}
		else
		{
			//TODO: error: "Invalid new expression"
		}
		
		return result;
	}

    /*
	 * <CastExpression> ::= CAST ( <Type> , <Expression> )
     */
	private Expr parseCast() { }


    /*
	 * <UnaryPrefix> ::= <PrefixOp> <UnaryPrefix> | <UnaryPostfix>
     * <PrefixOp> ::= - | ! | ++ | --
     */
	private Expr parseUnaryPrefix() { }


    /*
	 * <UnaryPostfix> ::= <Primary> <PostfixOp>
     * <PostfixOp> ::= ++ | -- | EMPTY
     */
	private Expr parseUnaryPostfix() { }


    /*
	 * <Primary> ::= ( <Expression> ) | <IntegerConst> | <BooleanConst> |
     *                               <StringConst> | <VarExpr> | <DispatchExpr>
     * <VarExpr> ::= <VarExprPrefix> <Identifier> <VarExprSuffix>
     * <VarExprPrefix> ::= SUPER . | THIS . | EMPTY
     * <VarExprSuffix> ::= [ <Expr> ] | EMPTY
     * <DispatchExpr> ::= <DispatchExprPrefix> <Identifier> ( <Arguments> )
     * <DispatchExprPrefix> ::= <Primary> . | EMPTY
     */
	private Expr parsePrimary() { }


    /*
	 * <Arguments> ::= EMPTY | <Expression> <MoreArgs>
     * <MoreArgs>  ::= EMPTY | , <Expression> <MoreArgs>
     */
	private ExprList parseArguments() { }


    /*
	 * <Parameters>  ::= EMPTY | <Formal> <MoreFormals>
     * <MoreFormals> ::= EMPTY | , <Formal> <MoreFormals
     */
	private FormalList parseParameters() { }

	/*
	 * <Formal> ::= <Type> <Identifier>
     */
	private Formal parseFormal() { }


    /*
	 * <Type> ::= <Identifier> <Brackets>
     * <Brackets> ::= EMPTY | [ ]
     */
	private String parseType() { }


    //----------------------------------------
    //Terminals

	private String parseOperator() { }


    private String parseIdentifier() { }


    private ConstStringExpr parseStringConst() { }


    private ConstIntExpr parseIntConst() { }


    private ConstBooleanExpr parseBoolean() { }
}

