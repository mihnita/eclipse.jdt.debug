package org.eclipse.jdi.hcr;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import com.sun.jdi.ClassType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.StepRequest;

/**
 * A reenter step request is a step event request that will be activated when the given 
 * thread is about to pop the top stack frame. At this point, the VM is expected to do 
 * the following:
 * <ol>
 *   <li>The arguments to the method are carefully set aside, along with the identity of the
 *       actual method. 
 *   <li>The stack frame is popped. Any value being returned is discarded. Any exception being 
 *       thrown is ignored. Instruction counter in caller is set <i>at</i> (rather than after) the 
 *       send bytecode.
 *   <li>Suspend the thread depending on the suspend policy and report a <code>StepEvent</code>
 *       for this request.
 *   <li>When the thread is resumed, the method is re-retrieved; if the class had recently 
 *       been reloaded, this must find the new bytecodes. If the method is no longer present, 
 *       throw a <code>java.lang.NoSuchMethodError</code> as specified in the Java VM 
 *       Specification. 
 *   <li>The method is entered as per normal, using the saved arguments. 
 * </ol>
 * <p>
 * Note that other events may need to be reported as well (e.g., hit breakpoint on first
 * instruction). Execution does not reenter the caller at any point; so no step out or step 
 * into events are reported.
 *
 */
public interface ReenterStepRequest extends StepRequest {
	/**
	 * Restricts the events generated by this request to those 
	 * whose location is in a class whose name does NOT match this restricted
	 * regular expression. e.g. "java.*" or "*.Foo".
	 * @param classPattern the pattern String to filter against.
	 */
	public void addClassExclusionFilter(String classPattern);
	
	/**
	 * Restricts the events generated by this request to those 
	 * whose location is in this class..
	 * @param clazz the class to filter on.
	 */
	public void addClassFilter(ReferenceType clazz);
	
	/**
	 * Restricts the events generated by this request to those 
	 * whose location is in a class whose name matches this restricted
	 * regular expression. e.g. "java.*" or "*.Foo".
	 * @param classPattern the pattern String to filter for.
	 */
	public void addClassFilter(String classPattern);
	
	/**
	 * @return the thread on which the step event is being requested.
	 */
	public ThreadReference thread();
}