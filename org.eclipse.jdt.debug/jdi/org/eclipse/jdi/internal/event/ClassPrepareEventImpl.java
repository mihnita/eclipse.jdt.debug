package org.eclipse.jdi.internal.event;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.request.*;
import org.eclipse.jdi.internal.*;
import org.eclipse.jdi.internal.jdwp.*;
import org.eclipse.jdi.internal.request.*;
import java.io.*;
import java.util.*;

/**
 * this class implements the corresponding interfaces
 * declared by the JDI specification. See the com.sun.jdi package
 * for more information.
 *
 */
public class ClassPrepareEventImpl extends EventImpl implements ClassPrepareEvent {
	/** Jdwp Event Kind. */
	public static final byte EVENT_KIND = EVENT_CLASS_PREPARE;

	/** Reference type for which this event was generated. */
	private ReferenceTypeImpl fReferenceType;
	/** Status of type, see JDWP.ClassStatus. */
	private int fStatus;
	
	/**
	 * Creates new BreakpointEventImpl.
	 */
	private ClassPrepareEventImpl(VirtualMachineImpl vmImpl, RequestID requestID) {
		super("ClassPrepareEvent", vmImpl, requestID);
	}
		
	/**
	 * @return Creates, reads and returns new EventImpl, of which requestID has already been read.
	 */
	public static ClassPrepareEventImpl read(MirrorImpl target, RequestID requestID, DataInputStream dataInStream) throws IOException {
		VirtualMachineImpl vmImpl = target.virtualMachineImpl();
		ClassPrepareEventImpl event = new ClassPrepareEventImpl(vmImpl, requestID);
		event.fThreadRef = ThreadReferenceImpl.read(target, dataInStream);
		event.fReferenceType = ReferenceTypeImpl.readWithTypeTagAndSignature(target, dataInStream);
		event.fStatus = target.readInt("class status", ReferenceTypeImpl.classStatusVector(), dataInStream);
		// Events that do not have status prepared are not given to the application.
		if ((event.fStatus & ReferenceTypeImpl.JDWP_CLASS_STATUS_PREPARED) == 0)
			return null;
		return event;
   	}
   	
	/**
	 * @return Returns the reference type for which this event was generated.
	 */
	public ReferenceType referenceType() {
		return fReferenceType;
	}
	
	/**
	 * @return Returns the JNI-style signature of the class that has been unloaded.
	 */
	public String classSignature() {
		return referenceType().signature();
	}
}
