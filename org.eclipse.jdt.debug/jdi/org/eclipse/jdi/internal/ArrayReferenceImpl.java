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
public class ArrayReferenceImpl extends ObjectReferenceImpl implements ArrayReference {
	/** JDWP Tag. */
	public static final byte tag = JdwpID.ARRAY_TAG;
	
	/**
	 * Creates new ArrayReferenceImpl.
	 */
	public ArrayReferenceImpl(VirtualMachineImpl vmImpl, JdwpObjectID objectID) {
		super("ArrayReference", vmImpl, objectID);
	}
	
	/**
	 * @returns tag.
	 */
	public byte getTag() {
		return tag;
	}
	
	/**
	 * @returns Returns an array component value.
	 */
	public Value getValue(int index) throws IndexOutOfBoundsException {
		return (Value)getValues(index, 1).get(0);
	}
	
	/**
	 * @returns Returns all of the components in this array.
	 */
	public List getValues() {
		return getValues(0, -1);
	}
	
	/**
	 * @return Returns a list containing each Method declared directly in this type.
	 */
	public List methods() {
		return new Vector();
	}
	
	/**
	 * @returns Returns a range of array components.
	 */
	public List getValues(int firstIndex, int length) throws IndexOutOfBoundsException {
		// Negative length indicates all elements.
		if (length < 0)
			length = length();
			
		// Note that this information should not be cached.
		initJdwpRequest();
		try {
			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
			DataOutputStream outData = new DataOutputStream(outBytes);
			write(this, outData);	// arrayObject
			writeInt(firstIndex, "firstIndex", outData);
			writeInt(length, "length", outData);
	
			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.AR_GET_VALUES, outBytes);
			switch (replyPacket.errorCode()) {
				case JdwpReplyPacket.INVALID_INDEX:
					throw new IndexOutOfBoundsException("Invalid index of array reference given.");
			}
			defaultReplyErrorHandler(replyPacket.errorCode());
			
			DataInputStream replyData = replyPacket.dataInStream();
	
			/* NOTE: The JDWP documentation is not clear on this: it turns out that the following is received from the VM:
			 * - type tag;
			 * - length of array;
			 * - values of elements.
			 */

			int type = readByte("type", JdwpID.tagMap(), replyData);
			int readLength = readInt("length", replyData);
			// See also ValueImpl.
			switch(type) {
				// Multidimensional array.
				case ArrayReferenceImpl.tag:
				// Object references.
				case ClassLoaderReferenceImpl.tag:
				case ClassObjectReferenceImpl.tag:
				case StringReferenceImpl.tag:
				case ObjectReferenceImpl.tag:
				case ThreadGroupReferenceImpl.tag:
				case ThreadReferenceImpl.tag:
					return readObjectSequence(readLength, replyData);

				// Primitive type.
				case BooleanValueImpl.tag:
				case ByteValueImpl.tag:
				case CharValueImpl.tag:
				case DoubleValueImpl.tag:
				case FloatValueImpl.tag:
				case IntegerValueImpl.tag:
				case LongValueImpl.tag:
				case ShortValueImpl.tag:
					return readPrimitiveSequence(readLength, type, replyData);

				case VoidValueImpl.tag:
				case 0:
				default:
					throw new InternalException("Invalid ArrayReference Value tag encountered: " + type);
			}
		} catch (IOException e) {
			defaultIOExceptionHandler(e);
			return null;
		} finally {
			handledJdwpRequest();
		}
	}

	/**
	 * @returns Returns sequence of object reference values.
	 */
	private List readObjectSequence(int length, DataInputStream in) throws IOException {
		Vector elements = new Vector();
		for (int i = 0; i < length; i++) {
			ValueImpl value = ObjectReferenceImpl.readObjectRefWithTag(this, in);
			elements.add(value);
		}
		return elements;
	}
	
	/**
	 * @returns Returns sequence of values of primitive type.
	 */
	private List readPrimitiveSequence(int length, int type, DataInputStream in) throws IOException {
		Vector elements = new Vector();
		for (int i = 0; i < length; i++) {
			ValueImpl value = ValueImpl.readWithoutTag(this, type, in);
			elements.add(value);
		}
		return elements;
	}
	
	/**
	 * @returns Returns the number of components in this array.
	 */
	public int length() {
		initJdwpRequest();
		try {
			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.AR_LENGTH, this);
			defaultReplyErrorHandler(replyPacket.errorCode());
			DataInputStream replyData = replyPacket.dataInStream();
			return readInt("length", replyData);
		} catch (IOException e) {
			defaultIOExceptionHandler(e);
			return 0;
		} finally {
			handledJdwpRequest();
		}
	}
	
	/**
	 * Replaces an array component with another value.
	 */
	public void setValue(int index, Value value) throws InvalidTypeException, ClassNotLoadedException {
		ArrayList list = new ArrayList(1);
		list.add(value);
		setValues(index, list, 0, 1);
	}
	
	/**
	 * Replaces all array components with other values.
	 */
	public void setValues(List values) throws InvalidTypeException, ClassNotLoadedException {
		setValues(0, values, 0, -1);
	}
	
	/**
	 * Replaces a range of array components with other values.
	 */
	public void setValues(int index, List values, int srcIndex, int length) throws InvalidTypeException, ClassNotLoadedException {
		// Negative length indicates all elements.
		if (length < 0)
			length = length() - index;
		else if (index + length > length())
			throw new IndexOutOfBoundsException("Attempted to set more values in array than length of array.");

		// Check if enough values are given.
		if (values.size() < srcIndex + length)
			throw new IndexOutOfBoundsException("Attempted to set more values in array than given.");

		// Note that this information should not be cached.
		initJdwpRequest();
		try {
			ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
			DataOutputStream outData = new DataOutputStream(outBytes);
			write(this, outData);
			writeInt(index, "index", outData);
			writeInt(length, "length", outData);
			for (int i = srcIndex; i < srcIndex + length; i++) {
				ValueImpl value = (ValueImpl)values.get(i);
				if (value != null) {
					checkVM(value);
					((ValueImpl)value).write(this, outData);
				} else {
					ValueImpl.writeNull(this, outData);
				}
			}
	
			JdwpReplyPacket replyPacket = requestVM(JdwpCommandPacket.AR_SET_VALUES, outBytes);
			switch (replyPacket.errorCode()) {
				case JdwpReplyPacket.TYPE_MISMATCH:
					throw new InvalidTypeException();
				case JdwpReplyPacket.INVALID_CLASS:
					throw new ClassNotLoadedException(type().name());
			}
			defaultReplyErrorHandler(replyPacket.errorCode());
		} catch (IOException e) {
			defaultIOExceptionHandler(e);
		} finally {
			handledJdwpRequest();
		}
	}
	
	/**
	 * @return Returns description of Mirror object.
	 */
	public String toString() {
		try {
			StringBuffer buf = new StringBuffer(type().name());
			// Insert length of string between (last) square braces.
			buf.insert(buf.length() - 1, length());
			// Append space and idString.
			buf.append(' ');
			buf.append(idString());
			return buf.toString();
		} catch (ObjectCollectedException e) {
			return "(Garbage Collected) ArrayReference" + "[" + length() + "] " + idString();
		} catch (Exception e) {
			return fDescription;
		}
	}

	/**
	 * @return Reads JDWP representation and returns new instance.
	 */
	public static ArrayReferenceImpl read(MirrorImpl target, DataInputStream in) throws IOException {
		VirtualMachineImpl vmImpl = target.virtualMachineImpl();
		JdwpObjectID ID = new JdwpObjectID(vmImpl);
		ID.read(in);
		if (target.fVerboseWriter != null)
			target.fVerboseWriter.println("arrayReference", ID.value());

		if (ID.isNull())
			return null;
			
		ArrayReferenceImpl mirror = new ArrayReferenceImpl(vmImpl, ID);
		return mirror;
	}
}
