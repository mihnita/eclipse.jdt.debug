package org.eclipse.jdi.internal;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import org.eclipse.jdi.internal.connect.*;
import org.eclipse.jdi.internal.request.*;
import org.eclipse.jdi.internal.event.*;
import org.eclipse.jdi.internal.jdwp.*;
import org.eclipse.jdi.internal.spy.*;
import java.util.*;
import java.io.*;

/**
 * this class implements the corresponding interfaces
 * declared by the JDI specification. See the com.sun.jdi package
 * for more information.
 *
 */
public class ByteValueImpl extends PrimitiveValueImpl implements ByteValue {
	/** JDWP Tag. */
	public static final byte tag = JdwpID.BYTE_TAG;

	/**
	 * Creates new instance.
	 */
	public ByteValueImpl(VirtualMachineImpl vmImpl, Byte value) {
		super("ByteValue", vmImpl, value);
	}

	/**
	 * @returns tag.
	 */
	public byte getTag() {
		return tag;
	}
	
	/**
	 * @returns type of value.
   	 */
	public Type type() {
		return new ByteTypeImpl(virtualMachineImpl());
	}
	
	/**
	 * @returns Value.
	 */
	public byte value() {
		return byteValue();
	}
	
	/**
	 * @return Reads and returns new instance.
	 */
	public static ByteValueImpl read(MirrorImpl target, DataInputStream in) throws IOException {
		VirtualMachineImpl vmImpl = target.virtualMachineImpl();
		byte value = target.readByte("byteValue", in);
		return new ByteValueImpl(vmImpl, new Byte(value));
	}
	
	/**
	 * Writes value without value tag.
	 */
	public void write(MirrorImpl target, DataOutputStream out) throws IOException {
		target.writeByte(((Byte)fValue).byteValue(), "byteValue", out);
	}
}
