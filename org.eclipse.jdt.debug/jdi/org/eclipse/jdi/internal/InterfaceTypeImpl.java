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
public class InterfaceTypeImpl extends ReferenceTypeImpl implements InterfaceType {
	/** JDWP Tag. */
	public static final byte typeTag = JdwpID.TYPE_TAG_INTERFACE;

	/**
	 * Creates new InterfaceTypeImpl.
	 */
	public InterfaceTypeImpl(VirtualMachineImpl vmImpl, JdwpInterfaceID interfaceID) {
		super("InterfaceType", vmImpl, interfaceID);
	}

	/**
	 * Creates new InterfaceTypeImpl.
	 */
	public InterfaceTypeImpl(VirtualMachineImpl vmImpl, JdwpInterfaceID interfaceID, String signature) {
		super("InterfaceType", vmImpl, interfaceID, signature);
	}

	/**
	 * @return Create a null value instance of the type.
	 */
	public Value createNullValue() {
		return new ClassObjectReferenceImpl(virtualMachineImpl(), new JdwpClassObjectID(virtualMachineImpl()));
	}

	/**
	 * @return Returns type tag.
	 */
	public byte typeTag() {
		return typeTag;
	}
	
	/**
	 * Flushes all stored Jdwp results.
	 */
	public void flushStoredJdwpResults() {
		super.flushStoredJdwpResults();

		// For all reftypes that have this interface cached, this cache must be undone.
		Enumeration enum = virtualMachineImpl().allCachedRefTypesEnum();
		while (enum.hasMoreElements()) {
			ReferenceTypeImpl refType = (ReferenceTypeImpl)enum.nextElement();
			if (refType.fInterfaces != null && refType.fInterfaces.contains(this)) {
				refType.flushStoredJdwpResults();
			}
		}
		
	}
	
	/**
	 * @return Returns the currently prepared classes which directly implement this interface.
	 */
	public List implementors() {
		// Note that this information should not be cached.
		Vector implementors = new Vector();
		Enumeration enum = virtualMachineImpl().allRefTypesEnum();
		while (enum.hasMoreElements()) {
			ReferenceTypeImpl refType = (ReferenceTypeImpl)enum.nextElement();
			if (refType instanceof ClassTypeImpl) {
				try {
					ClassTypeImpl classType = (ClassTypeImpl)refType;
					List interfaces = classType.interfaces();
					if (interfaces.contains(this))
						implementors .add(classType);
				} catch (ClassNotPreparedException e) {
					continue;
				}
			}
		}
		return implementors;
	}
	
	/**
	 * @return Returns the currently prepared interfaces which directly extend this interface. 
	 */
	public List subinterfaces() {
		// Note that this information should not be cached.
		Vector implementors = new Vector();
		Enumeration enum = virtualMachineImpl().allRefTypesEnum();
		while (enum.hasMoreElements()) {
			try {
				ReferenceTypeImpl refType = (ReferenceTypeImpl)enum.nextElement();
				if (refType instanceof InterfaceTypeImpl) {
					InterfaceTypeImpl interFaceType = (InterfaceTypeImpl)refType;
					List interfaces = interFaceType.superinterfaces();
					if (interfaces.contains(this))
						implementors .add(interFaceType);
				}
			} catch (ClassNotPreparedException e) {
				continue;
			}
		}
		return implementors;
	}
	
	/*
	 * @return Returns the interfaces directly extended by this interface. 
	 */
	public List superinterfaces() {
		return interfaces();
	}
	
	/** 
	 * @return Returns true if this type has been initialized.
	 */
	public boolean isInitialized() {
		return (status() & ReferenceTypeImpl.JDWP_CLASS_STATUS_PREPARED) != 0;
	}

	/**
	 * @return Reads ID and returns known ReferenceTypeImpl with that ID, or if ID is unknown a newly created ReferenceTypeImpl.
	 */
	public static InterfaceTypeImpl read(MirrorImpl target, DataInputStream in) throws IOException {
		VirtualMachineImpl vmImpl = target.virtualMachineImpl();
		JdwpInterfaceID ID = new JdwpInterfaceID(vmImpl);
		ID.read(in);
		if (target.fVerboseWriter != null)
			target.fVerboseWriter.println("interfaceType", ID.value());

		if (ID.isNull())
			return null;
			
		InterfaceTypeImpl mirror = (InterfaceTypeImpl)vmImpl.getCachedMirror(ID);
		if (mirror == null) {
			mirror = new InterfaceTypeImpl(vmImpl, ID);
			vmImpl.addCachedMirror(mirror);
		}
		return mirror;
	 }
	
	/**
	 * @return Reads ID and returns known ReferenceTypeImpl with that ID, or if ID is unknown a newly created ReferenceTypeImpl.
	 */
	public static InterfaceTypeImpl readWithSignature(MirrorImpl target, DataInputStream in) throws IOException {
		VirtualMachineImpl vmImpl = target.virtualMachineImpl();
		JdwpInterfaceID ID = new JdwpInterfaceID(vmImpl);
		ID.read(in);
		if (target.fVerboseWriter != null)
			target.fVerboseWriter.println("interfaceType", ID.value());

		String signature = target.readString("signature", in);
		if (ID.isNull())
			return null;
			
		InterfaceTypeImpl mirror = (InterfaceTypeImpl)vmImpl.getCachedMirror(ID);
		if (mirror == null) {
			mirror = new InterfaceTypeImpl(vmImpl, ID);
			vmImpl.addCachedMirror(mirror);
		}
		mirror.setSignature(signature);
		return mirror;
	 }
}
