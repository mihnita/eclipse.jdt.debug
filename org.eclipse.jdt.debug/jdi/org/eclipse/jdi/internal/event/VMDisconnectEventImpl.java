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

/**
 * this class implements the corresponding interfaces
 * declared by the JDI specification. See the com.sun.jdi package
 * for more information.
 *
 */
public class VMDisconnectEventImpl extends EventImpl implements VMDisconnectEvent {
	/** Jdwp Event Kind. */
	public static final byte EVENT_KIND = EVENT_VM_DISCONNECTED;

	/**
	 * Creates new VMDisconnectEventImpl.
	 */
	public VMDisconnectEventImpl(VirtualMachineImpl vmImpl, RequestID requestID) {
		super("VMDisconnectEvent", vmImpl, requestID);
	}

	/**
	 * @return Creates, reads and returns new EventImpl, of which requestID has already been read.
	 */
	public static VMDisconnectEventImpl read(MirrorImpl target, RequestID requestID, DataInputStream dataInStream) throws IOException {
		VirtualMachineImpl vmImpl = target.virtualMachineImpl();
		VMDisconnectEventImpl event = new VMDisconnectEventImpl(vmImpl, requestID);
		return event;
   	}
}
