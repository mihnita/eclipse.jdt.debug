package org.eclipse.jdt.internal.debug.eval.ast.engine;

/*
 * (c) Copyright IBM Corp. 2002.
 * All Rights Reserved.
 */

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.AndAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.AndOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.ArrayAllocation;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.ArrayInitializerInstruction;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.AssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.Cast;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.CompoundInstruction;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.ConditionalJump;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.Constructor;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.DivideAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.DivideOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.EqualEqualOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.GreaterEqualOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.GreaterOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.InstanceOfOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.Instruction;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.InstructionSequence;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.Jump;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.LeftShiftAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.LeftShiftOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.LessEqualOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.LessOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.MinusAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.MinusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.MultiplyAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.MultiplyOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.NoOp;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.NotOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.OrAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.OrOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PlusAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PlusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.Pop;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PostfixMinusMinusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PostfixPlusPlusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PrefixMinusMinusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PrefixPlusPlusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushArrayType;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushBoolean;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushChar;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushClassLiteralValue;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushDouble;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushFieldVariable;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushFloat;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushInt;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushLong;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushNull;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushString;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushThis;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushType;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.PushVariable;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.RemainderAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.RemainderOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.RightShiftAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.RightShiftOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.SendMessage;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.SendStaticMessage;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.TwiddleOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.UnaryMinusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.UnaryPlusOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.UnsignedRightShiftAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.UnsignedRightShiftOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.XorAssignmentOperator;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.XorOperator;

/**
 * The AST instruction compiler generates a sequence
 * of instructions (InstructionSequence) from a
 * DOM AST.
 */
public class ASTInstructionCompiler extends ASTVisitor {

	/**
	 * Whether to print debug messages to the console
	 */
	private static boolean VERBOSE = false;
	
	private InstructionSequence fInstructions;
	
	private int fStartPosition;
	
	private boolean fActive;
	
	private boolean fHasErrors;
	
	private Stack fStack;
	
	private int fCounter;
	

	/**
	 * Create a new AST instruction compiler
	 */
	public ASTInstructionCompiler(int startPosition, String snippet) {
		fStartPosition = startPosition;
		fInstructions = new InstructionSequence(snippet);
		fStack = new Stack();
	}
	
	/**
	 * Returns the instruction sequence generated
	 * by this AST instruction compiler
	 */
	public InstructionSequence getInstructions() {
		return fInstructions;
	}
	
	/**
	 * Returns whether the generated instruction sequence
	 * has errors.
	 * Errors include:
	 * <ol>
	 * <li>AST contains unimplemented operations (features which will be supported,
	 *  but aren't yet)</li>
	 * <li>AST contains unsupported operations (features which are not yet implemented
	 *  and are likely NOT to be implemented)</li>
	 * </ol>
	 */
	public boolean hasErrors() {
		return fHasErrors;
	}
	
	private void setHasError(boolean value) {
		fHasErrors= value;
	}
	
	private void addErrorMessage(Message message) {
		fInstructions.addError(message);
	}

	private boolean isActive() {
		return fActive;
	}
	
	private void setActive(boolean active) {
		fActive = active;
	}

	
	private void push(Instruction i) {
		fStack.push(i);
	}
	
	private Instruction pop() {
		return (Instruction)fStack.pop();
	}
	
	private void storeInstruction() {
		Instruction instruction= pop();
		fCounter++;
		if (instruction instanceof CompoundInstruction) {
			((CompoundInstruction)instruction).setEnd(fCounter);
		}
		fInstructions.add(instruction);
		verbose("Add " + instruction.toString());
	}
	
	
	/**
	 * Prints the given message to the console if verbose
	 * mode is on.
	 * 
	 * @param message the message to display
	 */
	private void verbose(String message) {
		if (VERBOSE) {
			System.out.println(message);
		}
	}
	

	private String getQualifiedIdentifier(Name name) {
		String typeName = "";
		while (name.isQualifiedName()) {
			QualifiedName qualifiedName = (QualifiedName) name;
			typeName = "." + qualifiedName.getName().getIdentifier() + typeName;
			name = qualifiedName.getQualifier();
		}
		if (name.isSimpleName()) {
			typeName = ((SimpleName)name).getIdentifier() + typeName;
		} else {
			// throw exception
		}
		return typeName;
	}
	
	private String getTypeName(ITypeBinding typeBinding) {
		String name= typeBinding.getName();
		IPackageBinding packageBinding= typeBinding.getPackage();
		typeBinding= typeBinding.getDeclaringClass();
		while(typeBinding != null) {
			name= typeBinding.getName() + '$' + name;
			typeBinding= typeBinding.getDeclaringClass();
		}
		if (packageBinding != null && !packageBinding.isUnnamed()) {
			name= packageBinding.getName() + '.' + name;
		}
		return name;
	}
	
	private String getTypeSignature(ITypeBinding typeBinding) {
		return Signature.createTypeSignature(getTypeName(typeBinding), true).replace('.', '/');
	}
	
	/**
	 * End visit methods
	 * 
	 * There are two paths to ending a visit to a node:
	 * <ol>
	 * <li>For control statements, the necessary control
	 *  instructions (jump, conditional jump) are inserted
	 *  into the instruction sequence</li>
	 * <li>For other cases, we simply remove the node's
	 *  instruction from the stack and add it to the 
	 *  instruction sequence.</li>
	 * </ol>
	 */

	/**
	 * @see ASTVisitor#endVisit(AnonymousClassDeclaration)
	 */
	public void endVisit(AnonymousClassDeclaration node) {
		
	}

	/**
	 * @see ASTVisitor#endVisit(ArrayAccess)
	 */
	public void endVisit(ArrayAccess node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(ArrayCreation)
	 */
	public void endVisit(ArrayCreation node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(ArrayInitializer)
	 */
	public void endVisit(ArrayInitializer node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(ArrayType)
	 */
	public void endVisit(ArrayType node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(AssertStatement)
	 */
	public void endVisit(AssertStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(Assignment)
	 */
	public void endVisit(Assignment node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();
	}

	/**
	 * @see ASTVisitor#endVisit(Block)
	 */
	public void endVisit(Block node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(BooleanLiteral)
	 */
	public void endVisit(BooleanLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(BreakStatement)
	 */
	public void endVisit(BreakStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(CastExpression)
	 */
	public void endVisit(CastExpression node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(CatchClause)
	 */
	public void endVisit(CatchClause node) {

	}

	/**
	 * @see ASTVisitor#endVisit(CharacterLiteral)
	 */
	public void endVisit(CharacterLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(ClassInstanceCreation)
	 */
	public void endVisit(ClassInstanceCreation node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(CompilationUnit)
	 */
	public void endVisit(CompilationUnit node) {

	}

	/**
	 * @see ASTVisitor#endVisit(ConditionalExpression)
	 */
	public void endVisit(ConditionalExpression node) {
		if (!isActive() || hasErrors())
			return;

		// Get the instructions			
		int ifFalseAddress= fInstructions.getEnd();
		Instruction ifFalse= fInstructions.get(ifFalseAddress);
		int ifTrueAddress= ifFalseAddress - ifFalse.getSize();
		Instruction ifTrue= fInstructions.get(ifTrueAddress);
		int conditionalAddress= ifTrueAddress - ifTrue.getSize();

		// Insert the conditional jump
		ConditionalJump conditionalJump= new ConditionalJump(false);
		fInstructions.insert(conditionalJump, conditionalAddress + 1);
		
		// Insert the jump
		int jumpAddress= ifTrueAddress + 2;
		Jump jump= new Jump();
		fInstructions.insert(jump, jumpAddress);
		
		// Set the jump offsets
		conditionalJump.setOffset(ifTrue.getSize() + 1);
		jump.setOffset(ifFalse.getSize() + 1);
		
		fCounter += 2;
		storeInstruction();
		
	}

	/**
	 * @see ASTVisitor#endVisit(ConstructorInvocation)
	 */
	public void endVisit(ConstructorInvocation node) {

	}

	/**
	 * @see ASTVisitor#endVisit(ContinueStatement)
	 */
	public void endVisit(ContinueStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(DoStatement)
	 */
	public void endVisit(DoStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(EmptyStatement)
	 */
	public void endVisit(EmptyStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(ExpressionStatement)
	 */
	public void endVisit(ExpressionStatement node) {
		if (!isActive() || hasErrors())
			return;
		boolean pop= true;

		Expression expression= node.getExpression();
		if (expression instanceof MethodInvocation) {
			IMethodBinding methodBinding= (IMethodBinding)((MethodInvocation)expression).getName().resolveBinding();
			if ("void".equals(methodBinding.getReturnType().getName())) {
				pop= false;
			}
		} else if (expression instanceof SuperMethodInvocation) {
			IMethodBinding methodBinding= (IMethodBinding)((SuperMethodInvocation)expression).getName().resolveBinding();
			if ("void".equals(methodBinding.getReturnType().getName())) {
				pop= false;
			}
		}

		if (pop) {
			storeInstruction();			
		}
	}

	/**
	 * @see ASTVisitor#endVisit(FieldAccess)
	 */
	public void endVisit(FieldAccess node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(FieldDeclaration)
	 */
	public void endVisit(FieldDeclaration node) {

	}

	/**
	 * @see ASTVisitor#endVisit(ForStatement)
	 */
	public void endVisit(ForStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(IfStatement)
	 */
	public void endVisit(IfStatement node) {
		if (!isActive() || hasErrors())
			return;
			
		boolean hasElseStatement= node.getElseStatement() != null;

		// Get the instructions			

		int ifFalseAddress= 0;
		Instruction ifFalse= null;
		int ifTrueAddress= 0;
		Instruction ifTrue= null;
		
		if (hasElseStatement) {
			ifFalseAddress= fInstructions.getEnd();
			ifFalse= fInstructions.get(ifFalseAddress);
			ifTrueAddress= ifFalseAddress - ifFalse.getSize();
			ifTrue= fInstructions.get(ifTrueAddress);
		} else {
			ifTrueAddress= fInstructions.getEnd();
			ifTrue= fInstructions.get(ifTrueAddress);
		}

		int conditionalAddress= ifTrueAddress - ifTrue.getSize();

		// Insert the conditional jump
		ConditionalJump conditionalJump= new ConditionalJump(false);
		fInstructions.insert(conditionalJump, conditionalAddress + 1);
		// Set the jump offset
		conditionalJump.setOffset(ifTrue.getSize() + ((hasElseStatement)? 1 : 0));
		fCounter++;

		if (hasElseStatement) {
			// Insert the jump
			int jumpAddress= ifTrueAddress + 2;
			Jump jump= new Jump();
			fInstructions.insert(jump, jumpAddress);
			// Set the jump offset
			jump.setOffset(ifFalse.getSize() + 1);
			fCounter++;
		}
		
		storeInstruction();
		
	}

	/**
	 * @see ASTVisitor#endVisit(ImportDeclaration)
	 */
	public void endVisit(ImportDeclaration node) {

	}

	/**
	 * @see ASTVisitor#endVisit(InfixExpression)
	 */
	public void endVisit(InfixExpression node) {
	}

	/**
	 * @see ASTVisitor#endVisit(Initializer)
	 */
	public void endVisit(Initializer node) {

	}

	/**
	 * @see ASTVisitor#endVisit(Javadoc)
	 */
	public void endVisit(Javadoc node) {

	}

	/**
	 * @see ASTVisitor#endVisit(LabeledStatement)
	 */
	public void endVisit(LabeledStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(MethodDeclaration)
	 */
	public void endVisit(MethodDeclaration node) {
		setActive(false);
	}

	/**
	 * @see ASTVisitor#endVisit(MethodInvocation)
	 */
	public void endVisit(MethodInvocation node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(NullLiteral)
	 */
	public void endVisit(NullLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(NumberLiteral)
	 */
	public void endVisit(NumberLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(PackageDeclaration)
	 */
	public void endVisit(PackageDeclaration node) {

	}

	/**
	 * @see ASTVisitor#endVisit(ParenthesizedExpression)
	 */
	public void endVisit(ParenthesizedExpression node) {

	}

	/**
	 * @see ASTVisitor#endVisit(PostfixExpression)
	 */
	public void endVisit(PostfixExpression node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(PrefixExpression)
	 */
	public void endVisit(PrefixExpression node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(PrimitiveType)
	 */
	public void endVisit(PrimitiveType node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(QualifiedName)
	 */
	public void endVisit(QualifiedName node) {
	}

	/**
	 * @see ASTVisitor#endVisit(ReturnStatement)
	 */
	public void endVisit(ReturnStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(SimpleName)
	 */
	public void endVisit(SimpleName node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(SimpleType)
	 */
	public void endVisit(SimpleType node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(SingleVariableDeclaration)
	 */
	public void endVisit(SingleVariableDeclaration node) {

	}

	/**
	 * @see ASTVisitor#endVisit(StringLiteral)
	 */
	public void endVisit(StringLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(SuperConstructorInvocation)
	 */
	public void endVisit(SuperConstructorInvocation node) {

	}

	/**
	 * @see ASTVisitor#endVisit(SuperFieldAccess)
	 */
	public void endVisit(SuperFieldAccess node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(SuperMethodInvocation)
	 */
	public void endVisit(SuperMethodInvocation node) {

	}

	/**
	 * @see ASTVisitor#endVisit(SwitchCase)
	 */
	public void endVisit(SwitchCase node) {

	}

	/**
	 * @see ASTVisitor#endVisit(SwitchStatement)
	 */
	public void endVisit(SwitchStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(SynchronizedStatement)
	 */
	public void endVisit(SynchronizedStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(ThisExpression)
	 */
	public void endVisit(ThisExpression node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(ThrowStatement)
	 */
	public void endVisit(ThrowStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(TryStatement)
	 */
	public void endVisit(TryStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(TypeDeclaration)
	 */
	public void endVisit(TypeDeclaration node) {

	}

	/**
	 * @see ASTVisitor#endVisit(TypeDeclarationStatement)
	 */
	public void endVisit(TypeDeclarationStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(TypeLiteral)
	 */
	public void endVisit(TypeLiteral node) {
		if (!isActive() || hasErrors())
			return;
		storeInstruction();			
	}

	/**
	 * @see ASTVisitor#endVisit(VariableDeclarationExpression)
	 */
	public void endVisit(VariableDeclarationExpression node) {

	}

	/**
	 * @see ASTVisitor#endVisit(VariableDeclarationFragment)
	 */
	public void endVisit(VariableDeclarationFragment node) {

	}

	/**
	 * @see ASTVisitor#endVisit(VariableDeclarationStatement)
	 */
	public void endVisit(VariableDeclarationStatement node) {

	}

	/**
	 * @see ASTVisitor#endVisit(WhileStatement)
	 */
	public void endVisit(WhileStatement node) {

	}

	/**
	 * Visit methods
	 * 
	 * There are two variations of node visiting:
	 * <ol>
	 * <li>Push the instruction corresponding to the node
	 *  onto the stack and return <code>true</code> to visit
	 *  the children of the node.</li>
	 * <li>Push the instruction corresponding to the node
	 *  onto the stack and visit the children of the node
	 *  manually (return <code>false</code> to avoid the
	 *  default child visiting implementation).</li>
	 * </ol>
	 */

	/**
	 * @see ASTVisitor#visit(AnonymousClassDeclaration)
	 */
	public boolean visit(AnonymousClassDeclaration node) {
		if (!isActive()) {
			return true;
		}
		setHasError(true);
		addErrorMessage(new Message("Anonymous type declaration cannot be used in an evaluation expression", node.getStartPosition()));
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ArrayAccess)
	 */
	public boolean visit(ArrayAccess node) {
		if (!isActive()) {
			return false;
		}
		
		push(new org.eclipse.jdt.internal.debug.eval.ast.instructions.ArrayAccess(fCounter));
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(ArrayCreation)
	 */
	public boolean visit(ArrayCreation node) {
		if (!isActive()) {
			return false;
		}
		
		ArrayType arrayType= node.getType();
		
		push(new ArrayAllocation(arrayType.getDimensions(), node.dimensions().size(), node.getInitializer() != null, fCounter));
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(ArrayInitializer)
	 */
	public boolean visit(ArrayInitializer node) {
		if (!isActive()) {
			return false;
		}
		
		ITypeBinding typeBinding= node.resolveTypeBinding();
		
		int dimension= typeBinding.getDimensions();
		String signature= Signature.createTypeSignature(getQualifiedName(typeBinding.getElementType()), true);
		
		push(new ArrayInitializerInstruction(signature, node.expressions().size(), dimension, fCounter));
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(ArrayType)
	 */
	public boolean visit(ArrayType node) {
		if (!isActive()) {
			return false;
		}
		ITypeBinding typeBinding= node.getElementType().resolveBinding();
		
		int dimension= node.getDimensions();
		String signature= Signature.createTypeSignature(getQualifiedName(typeBinding), true);

		push(new PushArrayType(signature, dimension, fCounter));
		
		return false;
	}

	/**
	 * @see ASTVisitor#visit(AssertStatement)
	 */
	public boolean visit(AssertStatement node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("Assert statement is not implemented", node.getStartPosition()));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(Assignment)
	 */
	public boolean visit(Assignment node) {
		if (!isActive()) {
			return false;
		}
		int variableTypeId = getTypeId(node.getLeftHandSide());
		int valueTypeId = getTypeId(node.getRightHandSide());
		
		String opToken = node.getOperator().toString();
		int opTokenLength = opToken.length();
		char char0 = opToken.charAt(0);
		char char2 = '\0';
		if (opTokenLength > 2) {
			char2 = opToken.charAt(2);
		}

		boolean unrecognized = false;
		
		switch (char0) {
			case '=': // equal
				push(new AssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '+': // plus equal
				push(new PlusAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '-': // minus equal
				push(new MinusAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '*': // multiply equal
				push(new MultiplyAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '/': // divide equal
				push(new DivideAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '%': // remainder equal
				push(new RemainderAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '^': // xor equal
				push(new XorAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '|': // or equal
				push(new OrAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '&': // and equal
				push(new AndAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '<': // left shift equal
				push(new LeftShiftAssignmentOperator(variableTypeId, valueTypeId, fCounter));
				break;
			case '>': // right shift equal or unsigned right shift equal
				switch (char2) {
					case '=': // right shift equal
						push(new RightShiftAssignmentOperator(variableTypeId, valueTypeId, fCounter));
						break;
					case '>': // unsigned right shift equal
						push(new UnsignedRightShiftAssignmentOperator(variableTypeId, valueTypeId, fCounter));
						break;
					default:
						unrecognized = true;
						break;
				}
				break;
			default:
				unrecognized = true;
				break;
		}
		
		if (unrecognized) {
			setHasError(true);
			addErrorMessage(new Message("Unrecognized assignment operator : " + opToken, node.getStartPosition()));
		}
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(Block)
	 */
	public boolean visit(Block node) {
		int start= node.getStartPosition();
		if (start == fStartPosition || start == (fStartPosition + 1)) {
			setActive(true);
		}
		if (!isActive()) {
			return true;
		}
		
		push(new NoOp(fCounter));
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(BooleanLiteral)
	 */
	public boolean visit(BooleanLiteral node) {
		if (!isActive()) {
			return false;
		}
		
		push(new PushBoolean(node.booleanValue()));
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(BreakStatement)
	 */
	public boolean visit(BreakStatement node) {
		setHasError(true);
		addErrorMessage(new Message("Break Statement cannot be used in an evaluation expression", node.getStartPosition()));
		return false;
	}

	/**
	 * @see ASTVisitor#visit(CastExpression)
	 */
	public boolean visit(CastExpression node) {
		if (!isActive()) {
			return false;
		}
		
		int typeId = getTypeId(node.getType());
		
		push(new Cast(typeId, fCounter));
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(CatchClause)
	 */
	public boolean visit(CatchClause node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("Catch clause is not implemented", node.getStartPosition()));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(CharacterLiteral)
	 */
	public boolean visit(CharacterLiteral node) {
		if (!isActive()) {
			return false;
		}
		
		push(new PushChar(node.charValue()));
		
		return true;
	}

	/**
	 * return false, visit name & arguments, don't visit expression & body declaration
	 * @see ASTVisitor#visit(ClassInstanceCreation)
	 */
	public boolean visit(ClassInstanceCreation node) {
		if (!isActive()) {
			return true;
		}
		
		if (node.getAnonymousClassDeclaration() != null) {
			setHasError(true);
			addErrorMessage(new Message("Anonymous type declaration cannot be used in an evaluation expression", node.getStartPosition()));
		}
		if (node.getExpression() != null) {
			setHasError(true);
			addErrorMessage(new Message("Class Instance Creation Expression  is not managed", node.getStartPosition()));
		}
		
		if (hasErrors()) {
			return true;
		}
		
		IMethodBinding methodBinding = node.resolveConstructorBinding();

		int argCount= methodBinding.getParameterTypes().length;

		String signature= getMethodSignature(methodBinding).replace('.','/');

		push(new Constructor(signature, argCount, fCounter));
 		
//		node.getName().accept(this);
		// TO DO: Use the method call above instead of the following code
		push(new PushType(getTypeName(methodBinding.getDeclaringClass()), false));
		storeInstruction();

		Iterator iterator= node.arguments().iterator();
		while (iterator.hasNext()) {
			((Expression) iterator.next()).accept(this);
		}

		

		return false;

	}

	/**
	 * @see ASTVisitor#visit(CompilationUnit)
	 */
	public boolean visit(CompilationUnit node) {
		return true;
	}

	/**
	 * @see ASTVisitor#visit(ConditionalExpression)
	 */
	public boolean visit(ConditionalExpression node) {
		if (!isActive()) {
			return true;
		}
		
		push(new NoOp(fCounter));
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(ConstructorInvocation)
	 */
	public boolean visit(ConstructorInvocation node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("this constructor invocation cannot be used in an evaluation expression", node.getStartPosition()));
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ContinueStatement)
	 */
	public boolean visit(ContinueStatement node) {
		setHasError(true);
		addErrorMessage(new Message("Continue statement cannot be used in an evaluation expression", node.getStartPosition()));
		return false;
	}

	/**
	 * @see ASTVisitor#visit(DoStatement)
	 */
	public boolean visit(DoStatement node) {
		setHasError(true);
		addErrorMessage(new Message("Do statement cannot be used in an evaluation expression", node.getStartPosition()));
		return false;
	}

	/**
	 * @see ASTVisitor#visit(EmptyStatement)
	 */
	public boolean visit(EmptyStatement node) {
		if (!isActive()) {
			return false;
		}
		return true;
	}

	/**
	 * @see ASTVisitor#visit(ExpressionStatement)
	 */
	public boolean visit(ExpressionStatement node) {
		if (!isActive()) {
			return true;
		}

		boolean pop= true;

		Expression expression= node.getExpression();
		if (expression instanceof MethodInvocation) {
			IMethodBinding methodBinding= (IMethodBinding)((MethodInvocation)expression).getName().resolveBinding();
			if ("void".equals(methodBinding.getReturnType().getName())) {
				pop= false;
			}
		} else if (expression instanceof SuperMethodInvocation) {
			IMethodBinding methodBinding= (IMethodBinding)((SuperMethodInvocation)expression).getName().resolveBinding();
			if ("void".equals(methodBinding.getReturnType().getName())) {
				pop= false;
			}
		}

		if (pop) {
			push(new Pop());
		}
		
		return true;
	}

	/**
	 * return false, visit expression, don't visit name
	 * 
	 * @see ASTVisitor#visit(FieldAccess)
	 */
	public boolean visit(FieldAccess node) {
		if (!isActive()) {
			return false;
		}
		
		String signature = getTypeSignature(node.getExpression().resolveTypeBinding());
		
		push(new PushFieldVariable(node.getName().getIdentifier(), signature, fCounter));
		
		node.getExpression().accept(this);
		
		return false;
	}

	/**
	 * @see ASTVisitor#visit(FieldDeclaration)
	 */
	public boolean visit(FieldDeclaration node) {
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ForStatement)
	 */
	public boolean visit(ForStatement node) {
		setHasError(true);
		addErrorMessage(new Message("For statement cannot be used in an evaluation expression", node.getStartPosition()));
		return false;
	}

	/**
	 * @see ASTVisitor#visit(IfStatement)
	 */
	public boolean visit(IfStatement node) {
		if (!isActive()) {
			return false;
		}
		
		push(new NoOp(fCounter));
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(ImportDeclaration)
	 */
	public boolean visit(ImportDeclaration node) {
		return false;
	}

	/**
	 * return <code>false</code>, don't use the standart accept order.
	 * 
	 * @see ASTVisitor#visit(InfixExpression)
	 */
	public boolean visit(InfixExpression node) {
		if (!isActive()) {
			return false;
		}
		
		String opToken = node.getOperator().toString();
		int opTokenLength = opToken.length();
		char char0 = opToken.charAt(0);
		char char1 = '\0';
		char char2 = '\0';
		if (opTokenLength > 1) {
			char1 = opToken.charAt(1);
			if (opTokenLength > 2) {
				char2 = opToken.charAt(2);
			}
		}
		
		List extendedOperands = node.extendedOperands();
		
		int operatorNumber=extendedOperands.size() + 1;
		
		int[][] types = new int[operatorNumber][3];

		Iterator iterator = extendedOperands.iterator();

		int leftTypeId = getTypeId(node.getLeftOperand());
		int rightTypeId = getTypeId(node.getRightOperand());
		int resultTypeId = Instruction.getBinaryPromotionType(leftTypeId, rightTypeId);
		
		types[0][0] = resultTypeId;
		types[0][1] = leftTypeId;
		types[0][2] = rightTypeId;
		
		for (int i = 1; i < operatorNumber; i++) {
			Expression operand = (Expression) iterator.next();
			leftTypeId = resultTypeId;
			rightTypeId = getTypeId(operand);
			resultTypeId = Instruction.getBinaryPromotionType(leftTypeId, rightTypeId);
			types[i][0] = resultTypeId;
			types[i][1] = leftTypeId;
			types[i][2] = rightTypeId;
		}
		
		boolean unrecognized= false;
		
		switch (char0) {
			case '*': // multiply
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new MultiplyOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '/': // divide
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new DivideOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '%': // remainder
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new RemainderOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '+': // plus
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new PlusOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '-': // minus
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new MinusOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '<': // left shift or less or less equal
				switch (char1) {
					case '\0': // less
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new LessOperator(types[i][1], types[i][2], fCounter));
						}
						break;
					case '<': // left shift
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new LeftShiftOperator(Instruction.getUnaryPromotionType(types[i][1]), types[i][1], types[i][2], fCounter));
						}
						break;
					case '=': // less equal
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new LessEqualOperator(types[i][1], types[i][2], fCounter));
						}
						break;
					default:
						unrecognized= true;
						break;
				}
				break;
			case '>': // right shift or unsigned right shift or greater or greater equal
				switch (char1) {
					case '\0': // greater
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new GreaterOperator(types[i][1], types[i][2], fCounter));
						}
						break;
					case '>': // right shift or unsigned right shift
						switch (char2) {
							case '\0': // right shift
								for (int i = operatorNumber - 1; i >= 0; i--) {
									push(new RightShiftOperator(Instruction.getUnaryPromotionType(types[i][1]), types[i][1], types[i][2], fCounter));
								}
								break;
							case '>': // unsigned right shift
								for (int i = operatorNumber - 1; i >= 0; i--) {
									push(new UnsignedRightShiftOperator(Instruction.getUnaryPromotionType(types[i][1]), types[i][1], types[i][2], fCounter));
								}
								break;
						}
						break;
					case '=': // greater equal
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new GreaterEqualOperator(types[i][1], types[i][2], fCounter));
						}
						break;
					default:
						unrecognized= true;
						break;
				}
				break;
			case 'i': // instanceof
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new InstanceOfOperator(fCounter));
				}
				break;
			case '=': // equal equal
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new EqualEqualOperator(types[i][1], types[i][2], true, fCounter));
				}
				break;
			case '!': // not equal
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new EqualEqualOperator(types[i][1], types[i][2], false, fCounter));
				}
				break;
			case '^': // xor
				for (int i = operatorNumber - 1; i >= 0; i--) {
					push(new XorOperator(types[i][0], types[i][1], types[i][2], fCounter));
				}
				break;
			case '|': // or or or or
				switch (char1) {
					case '\0': // or
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new OrOperator(types[i][0], types[i][1], types[i][2], fCounter));
						}
						break;
					case '|': // or or
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new NoOp(fCounter));
						}
						break;
					default:
						unrecognized= true;
						break;
				}
				break;
			case '&': // and or and and
				switch (char1) {
					case '\0': // and
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new AndOperator(types[i][0], types[i][1], types[i][2], fCounter));
						}
						break;
					case '&': // and and
						for (int i = operatorNumber - 1; i >= 0; i--) {
							push(new NoOp(fCounter));
						}
						break;
					default:
						unrecognized= true;
						break;
				}
				break;
			default:
				unrecognized= true;
				break;
		}
		
		if (unrecognized) {
			setHasError(true);
			addErrorMessage(new Message("Unrecognized infix operator : " + opToken, node.getStartPosition()));
		}
		
		if (hasErrors()) {
			return true;
		}
		
		iterator = extendedOperands.iterator();
		
		if ((char0 == '&' && char1 == '&') || (char0 == '|' && char1 == '|')) { // and and operator
			
			boolean isOrOr= char0 == '|';
			
			ConditionalJump[] conditionalJumps= new ConditionalJump[operatorNumber];
			int[] conditionalJumpAddresses = new int[operatorNumber];
			
			node.getLeftOperand().accept(this);
			
			ConditionalJump conditionalJump= new ConditionalJump(isOrOr);
			conditionalJumps[0]= conditionalJump;
			conditionalJumpAddresses[0] = fCounter;
			push(conditionalJump);
			storeInstruction();
			
			node.getRightOperand().accept(this);
			
			for (int i= 1; i < operatorNumber; i ++) {
				conditionalJump= new ConditionalJump(isOrOr);
				conditionalJumps[i]= conditionalJump;
				conditionalJumpAddresses[i] = fCounter;
				push(conditionalJump);
				storeInstruction();
				((Expression) iterator.next()).accept(this);
			}
			
			Jump jump = new Jump();
			jump.setOffset(1);
			push(jump);
			storeInstruction();
			
			for (int i= 0; i < operatorNumber; i ++) {
				conditionalJumps[i].setOffset(fCounter - conditionalJumpAddresses[i] - 1);
			}
			
			push(new PushBoolean(isOrOr));
			storeInstruction();
			
			// store the noop
			storeInstruction();
			
		} else { // other operatos
			
			node.getLeftOperand().accept(this);
			node.getRightOperand().accept(this);
		
			storeInstruction();
			for (int i= 1; i < operatorNumber; i ++) {
				((Expression) iterator.next()).accept(this);
				storeInstruction();
			}
		}
		
		
		
		return false;
	}

	/**
	 * @see ASTVisitor#visit(Initializer)
	 */
	public boolean visit(Initializer node) {
		return true;
	}

	/**
	 * @see ASTVisitor#visit(Javadoc)
	 */
	public boolean visit(Javadoc node) {
		return false;
	}

	/**
	 * @see ASTVisitor#visit(LabeledStatement)
	 */
	public boolean visit(LabeledStatement node) {
		setHasError(true);
		addErrorMessage(new Message("Labaled Statement cannot be used in an evaluation expression", node.getStartPosition()));
		return false;
	}

	/**
	 * @see ASTVisitor#visit(MethodDeclaration)
	 */
	public boolean visit(MethodDeclaration node) {
		int start= node.getStartPosition();
		int end= start + node.getLength();
		if (start < fStartPosition && end > fStartPosition) {
			return true;
		}
		return false;
	}

	/**
	 * return false, don't visit name, visit expression & arguments
	 * 
	 * @see ASTVisitor#visit(MethodInvocation)
	 */
	public boolean visit(MethodInvocation node) {
		if (!isActive()) {
			return false;
		}
		
		if (hasErrors()) {
			return true;
		}
		
		IMethodBinding methodBinding= (IMethodBinding) node.getName().resolveBinding();
		
		ITypeBinding[] parameterTypes= methodBinding.getParameterTypes();
		int argCount= parameterTypes.length;
		String selector= methodBinding.getName();

		String signature= getMethodSignature(methodBinding).replace('.','/');
		
		boolean isStatic= Flags.isStatic(methodBinding.getModifiers());
		Expression expression= node.getExpression();
		
		if (isStatic) {
			String typeSignature= Signature.createTypeSignature(getQualifiedName(methodBinding.getDeclaringClass()), true);
			push(new SendStaticMessage(typeSignature, selector, signature, argCount, fCounter));

			if (expression != null) {
				node.getExpression().accept(this);
				push(new Pop());
				storeInstruction();
			}
		} else {
			push(new SendMessage(selector, signature, argCount, false, fCounter));

			if (expression == null) {
				push(new PushThis());
				storeInstruction();	
			} else {
				node.getExpression().accept(this);
			}
		}

		Iterator iterator= node.arguments().iterator();
		while (iterator.hasNext()) {
			((Expression) iterator.next()).accept(this);
		}
		
		return false;
	}

	/**
	 * @see ASTVisitor#visit(NullLiteral)
	 */
	public boolean visit(NullLiteral node) {
		if (!isActive()) {
			return false;
		}
		
		push(new PushNull());
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(NumberLiteral)
	 */
	public boolean visit(NumberLiteral node) {
		if (!isActive()) {
			return false;
		}
		
		int literalType = getTypeId(node);
		String token = node.getToken();
		
		char lastChar = token.charAt(token.length() - 1);
		if ((lastChar < '0' || lastChar > '9') && lastChar != '.') {
			token = token.substring(0,token.length() - 1);
		}
		
		switch (literalType) {
			case Instruction.T_int:
				push(new PushInt(Integer.decode(token).intValue()));
				break;
			case Instruction.T_long:
				push(new PushLong(Long.decode(token).longValue()));
				break;
			case Instruction.T_float:
				push(new PushFloat(Float.parseFloat(token)));
				break;
			case Instruction.T_double:
				push(new PushDouble(Double.parseDouble(token)));
				break;
		}
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(PackageDeclaration)
	 */
	public boolean visit(PackageDeclaration node) {
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ParenthesizedExpression)
	 */
	public boolean visit(ParenthesizedExpression node) {
		if (!isActive()) {
			return false;
		}
		return true;
	}

	/**
	 * @see ASTVisitor#visit(PostfixExpression)
	 */
	public boolean visit(PostfixExpression node) {
		if (!isActive()) {
			return false;
		}

		int expressionTypeId = getTypeId(node.getOperand());

		String opToken = node.getOperator().toString();
		char char0 = opToken.charAt(0);
		
		switch (char0) {
			case '+': // plus plus or unary plus
				push(new PostfixPlusPlusOperator(expressionTypeId, fCounter));
				break;
			case '-': // minus minus or unary minus
				push(new PostfixMinusMinusOperator(expressionTypeId, fCounter));
				break;
			default:
				setHasError(true);
				addErrorMessage(new Message("unrecognized postfix operator : " + opToken, node.getStartPosition()));
				break;
		}

		return true;
	}

	/**
	 * @see ASTVisitor#visit(PrefixExpression)
	 */
	public boolean visit(PrefixExpression node) {
		if (!isActive()) {
			return false;
		}
		
		int expressionTypeId = getTypeId(node.getOperand());

		String opToken = node.getOperator().toString();
		int opTokenLength = opToken.length();
		char char0 = opToken.charAt(0);
		char char1 = '\0';
		if (opTokenLength > 1) {
			char1 = opToken.charAt(1);
		}
		
		boolean unrecognized = false;
		
		switch (char0) {
			case '+': // plus plus or unary plus
				switch (char1) {
					case '\0': // unary plus
						push(new UnaryPlusOperator(expressionTypeId, fCounter));
						break;
					case '+': // plus plus
						push(new PrefixPlusPlusOperator(expressionTypeId, fCounter));
						break;
					default:
						unrecognized= true;
						break;
				}
				break;
			case '-': // minus minus or unary minus
				switch (char1) {
					case '\0': // unary minus
						push(new UnaryMinusOperator(expressionTypeId, fCounter));
					break;
					case '-': // minus minus
						push(new PrefixMinusMinusOperator(expressionTypeId, fCounter));
					break;
					default:
						unrecognized= true;
						break;
				}
				break;
			case '~': // twiddle
				push(new TwiddleOperator(expressionTypeId, fCounter));
				break;
			case '!': // not
				push(new NotOperator(expressionTypeId, fCounter));
				break;
			default:
				unrecognized= true;
				break;
		}


		if (unrecognized) {
			setHasError(true);
			addErrorMessage(new Message("unrecognized prefix operator : " + opToken, node.getStartPosition()));
		}
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(PrimitiveType)
	 */
	public boolean visit(PrimitiveType node) {
		if (!isActive()) {
			return false;
		}
		
		push(new PushType(getPrimitiveTypeSignature(node.getPrimitiveTypeCode().toString()), true));
		
		return true;
	}

	/** 
	 * @see ASTVisitor#visit(QualifiedName)
	 */
	public boolean visit(QualifiedName node) {
		if (!isActive()) {
			return false;
		}
		
		if (hasErrors()) {
			return true;
		}
		
		IBinding binding = node.resolveBinding();
		switch (binding.getKind()) {
			case IBinding.TYPE:
				node.getName().accept(this);
				break;
			case IBinding.VARIABLE:
				String signature = getTypeSignature(node.getQualifier().resolveTypeBinding());
		
				push(new PushFieldVariable(node.getName().getIdentifier(), signature, fCounter));
				node.getQualifier().accept(this);
				storeInstruction();
				break;
		}
		
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ReturnStatement)
	 */
	public boolean visit(ReturnStatement node) {
		if (!isActive()) {
			return false;
		}
		return true;
	}

	/**
	 * @see ASTVisitor#visit(SimpleName)
	 */
	public boolean visit(SimpleName node) {
		if (!isActive()) {
			return false;
		}
		
		IBinding binding = node.resolveBinding();
		
		if (binding == null) {
			setHasError(true);
			addErrorMessage(new Message("binding == null for " + node.getIdentifier(), node.getStartPosition()));
			return true;
		}
		
		switch (binding.getKind()) {
			case IBinding.TYPE:
				ITypeBinding typeBinding= (ITypeBinding) binding;
				push(new PushType(getTypeName(typeBinding), false));
				break;
			case IBinding.VARIABLE:
				IVariableBinding variableBinding= (IVariableBinding) binding;
				if (variableBinding.isField() && Modifier.isStatic(variableBinding.getModifiers())) {
					push(new PushFieldVariable(node.getIdentifier(), false, fCounter));
					typeBinding= variableBinding.getDeclaringClass();
					push(new PushType(getTypeName(typeBinding), false));
					storeInstruction();
				} else {
					push(new PushVariable(node.getIdentifier()));
				}
				break;
		}
		return true;
	}

	/**
	 * return false, don't visit child
	 * 
	 * @see ASTVisitor#visit(SimpleType)
	 */
	public boolean visit(SimpleType node) {
		if (!isActive()) {
			return false;
		}
		
		ITypeBinding typeBinding  = node.resolveBinding();
		push(new PushType(getTypeName(typeBinding), false));
		
		return false;
	}

	/**
	 * @see ASTVisitor#visit(SingleVariableDeclaration)
	 */
	public boolean visit(SingleVariableDeclaration node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("Single variable declaration is not implemented", node.getStartPosition()));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(StringLiteral)
	 */
	public boolean visit(StringLiteral node) {
		if (!isActive()) {
			return false;
		}
		
		push(new PushString(node.getLiteralValue()));
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(SuperConstructorInvocation)
	 */
	public boolean visit(SuperConstructorInvocation node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("super constructor invocation cannot be used in an evaluation expression", node.getStartPosition()));
		return false;
	}

	/**
	 * @see ASTVisitor#visit(SuperFieldAccess)
	 */
	public boolean visit(SuperFieldAccess node) {
		if (!isActive()) {
			return false;
		}
		
		push(new PushThis());
		storeInstruction();
		
		push(new PushFieldVariable(node.getName().getIdentifier(), true, fCounter));
		
		return false;
	}

	/**
	 * return false, don't visit name, visit arguments
	 * 
	 * @see ASTVisitor#visit(SuperMethodInvocation)
	 */
	public boolean visit(SuperMethodInvocation node) {
		if (!isActive()) {
			return false;
		}
		
		if (node.getQualifier() != null) {
			setHasError(true);
			addErrorMessage(new Message("Qualifier for super method invocation is not implemented", node.getStartPosition()));
			return true;
		}
		
		if (hasErrors()) {
			return true;
		}
		
		IMethodBinding methodBinding = (IMethodBinding) node.getName().resolveBinding();
		
		ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
		int argCount = parameterTypes.length;
		String selector = methodBinding.getName();
		String signature = getMethodSignature(methodBinding);
		
		push(new SendMessage(selector, signature, argCount, true, fCounter));
		
		push(new PushThis());
		storeInstruction();
		
		Iterator iterator = node.arguments().iterator();
		while (iterator.hasNext()) {
			((Expression) iterator.next()).accept(this);
		}
		
		return false;
	}

	/**
	 * @see ASTVisitor#visit(SwitchCase)
	 */
	public boolean visit(SwitchCase node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("Switch case is not implemented", node.getStartPosition()));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(SwitchStatement)
	 */
	public boolean visit(SwitchStatement node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("Switch statement is not implemented", node.getStartPosition()));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(SynchronizedStatement)
	 */
	public boolean visit(SynchronizedStatement node) {
		if (!isActive()) {
			return false;
		}
		return true;
	}

	/**
	 * @see ASTVisitor#visit(ThisExpression)
	 */
	public boolean visit(ThisExpression node) {
		if (!isActive()) {
			return false;
		}
				
		push(new PushThis());
		
		return false;
	}

	/**
	 * @see ASTVisitor#visit(ThrowStatement)
	 */
	public boolean visit(ThrowStatement node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("Throw statement is not implemented", node.getStartPosition()));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(TryStatement)
	 */
	public boolean visit(TryStatement node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("Try statement is not implemented", node.getStartPosition()));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(TypeDeclaration)
	 */
	public boolean visit(TypeDeclaration node) {
		if (!isActive()) {
			return true;
		}
		setHasError(true);
		addErrorMessage(new Message("Type declaration cannot be used in an evaluation expression", node.getStartPosition()));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(TypeDeclarationStatement)
	 */
	public boolean visit(TypeDeclarationStatement node) {
		if (!isActive()) {
			return true;
		}
		setHasError(true);
		addErrorMessage(new Message("Type declaration statement cannot be used in an evaluation expression", node.getStartPosition()));
		return false;
	}

	/**
	 * @see ASTVisitor#visit(TypeLiteral)
	 */
	public boolean visit(TypeLiteral node) {
		if (!isActive()) {
			return false;
		}
		
		push(new PushClassLiteralValue(fCounter));
		
		return true;
	}

	/**
	 * @see ASTVisitor#visit(VariableDeclarationExpression)
	 */
	public boolean visit(VariableDeclarationExpression node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("Variable declaration expression is not implemented", node.getStartPosition()));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(VariableDeclarationFragment)
	 */
	public boolean visit(VariableDeclarationFragment node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("Variable declaration fragment is not implemented", node.getStartPosition()));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(VariableDeclarationStatement)
	 */
	public boolean visit(VariableDeclarationStatement node) {
		if (!isActive()) {
			return false;
		}
		setHasError(true);
		addErrorMessage(new Message("Variable declaration statement is not implemented", node.getStartPosition()));
		return true;
	}

	/**
	 * @see ASTVisitor#visit(WhileStatement)
	 */
	public boolean visit(WhileStatement node) {
		setHasError(true);
		addErrorMessage(new Message("While statement cannot be used in an evaluation expression", node.getStartPosition()));
		return false;
	}
	
	//--------------------------
	
	private String getQualifiedName(ITypeBinding typeBinding) {
		IPackageBinding packageBinding = typeBinding.getPackage();
		return ((packageBinding == null || packageBinding.isUnnamed())? "" : packageBinding.getName() + ".") + typeBinding.getName();
	}
	
	private int getTypeId(Expression expression) {
		ITypeBinding typeBinding = expression.resolveTypeBinding();
		String typeName = typeBinding.getName();
		if (typeBinding.isPrimitive()) {
			return getPrimitiveTypeId(typeName);
		} else if ("String".equals(typeName) && "java.lang".equals(typeBinding.getPackage().getName())){
			return Instruction.T_String;
		} else {
			return Instruction.T_Object;
		}
	}
	
	private int getTypeId(Type type) {
		if (type.isPrimitiveType()) {
			return getPrimitiveTypeId(((PrimitiveType)type).getPrimitiveTypeCode().toString());
		} else if (type.isSimpleType()) {
			SimpleType simpleType = (SimpleType) type;
			if ("java.lang.String".equals(simpleType.getName())){
				return Instruction.T_String;
			} else {
				return Instruction.T_Object;
			}
		} else if (type.isArrayType()) {
			return Instruction.T_Object;
		} else {
			return Instruction.T_undefined;
		}
		
	}

	private String getMethodSignature(IMethodBinding methodBinding) {
		ITypeBinding[] parameterTypes = methodBinding.getParameterTypes();
		int argCount = parameterTypes.length;
		String[] parameterSignatures = new String[argCount];
		for (int i = 0; i < argCount; i++) {
			parameterSignatures[i] = Signature.createTypeSignature(getQualifiedName(parameterTypes[i]), true);
		}
		String signature = Signature.createMethodSignature(parameterSignatures, Signature.createTypeSignature(getQualifiedName(methodBinding.getReturnType()), true));
		return signature;
	}

	private int getPrimitiveTypeId(String typeName) {
		switch (typeName.charAt(0)) {
			case 'b': // byte or boolean
				switch (typeName.charAt(1)) {
					case 'o': // boolean;
						return Instruction.T_boolean;
					case 'y': // byte
						return Instruction.T_byte;
				}
				break;
			case 'c': // char
				return Instruction.T_char;
			case 'd': // double
				return Instruction.T_double;
			case 'f': // float
				return Instruction.T_float;
			case 'i': // int
				return Instruction.T_int;
			case 'l': // long
				return Instruction.T_long;
			case 'n':
				return Instruction.T_null;
			case 's': // short
				return Instruction.T_short;
			case 'v': // void
				return Instruction.T_void;
		}
		return Instruction.T_undefined;
	}
	
	private String getPrimitiveTypeSignature(String typeName) {
		switch (getPrimitiveTypeId(typeName)) {
			case Instruction.T_byte:
				return "B";
			case Instruction.T_char:
				return "C";
			case Instruction.T_double:
				return "D";
			case Instruction.T_float:
				return "F";
			case Instruction.T_int:
				return "I";
			case Instruction.T_long:
				return "J";
			case Instruction.T_short:
				return "S";
			case Instruction.T_boolean:
				return "Z";
			case Instruction.T_void:
				return "V";
		}
		// throw exception
		return null;
	}

}
