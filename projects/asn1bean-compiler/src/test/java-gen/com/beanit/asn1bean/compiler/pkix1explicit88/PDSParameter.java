/*
 * This class file was automatically generated by ASN1bean (http://www.beanit.com)
 */

package com.beanit.asn1bean.compiler.pkix1explicit88;

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.io.Serializable;
import com.beanit.asn1bean.ber.*;
import com.beanit.asn1bean.ber.types.*;
import com.beanit.asn1bean.ber.types.string.*;


public class PDSParameter implements BerType, Serializable {

	private static final long serialVersionUID = 1L;

	public static final BerTag tag = new BerTag(BerTag.UNIVERSAL_CLASS, BerTag.CONSTRUCTED, 17);

	private byte[] code = null;
	public BerPrintableString printableString = null;
	public BerTeletexString teletexString = null;
	
	public PDSParameter() {
	}

	public PDSParameter(byte[] code) {
		this.code = code;
	}

	public PDSParameter(BerPrintableString printableString, BerTeletexString teletexString) {
		this.printableString = printableString;
		this.teletexString = teletexString;
	}

	@Override public int encode(OutputStream reverseOS) throws IOException {
		return encode(reverseOS, true);
	}

	public int encode(OutputStream reverseOS, boolean withTag) throws IOException {

		if (code != null) {
			reverseOS.write(code);
			if (withTag) {
				return tag.encode(reverseOS) + code.length;
			}
			return code.length;
		}

		int codeLength = 0;
		if (teletexString != null) {
			codeLength += teletexString.encode(reverseOS, true);
		}
		
		if (printableString != null) {
			codeLength += printableString.encode(reverseOS, true);
		}
		
		codeLength += BerLength.encodeLength(reverseOS, codeLength);

		if (withTag) {
			codeLength += tag.encode(reverseOS);
		}

		return codeLength;

	}

	@Override public int decode(InputStream is) throws IOException {
		return decode(is, true);
	}

	public int decode(InputStream is, boolean withTag) throws IOException {
		int tlByteCount = 0;
		int vByteCount = 0;
		BerTag berTag = new BerTag();

		if (withTag) {
			tlByteCount += tag.decodeAndCheck(is);
		}

		BerLength length = new BerLength();
		tlByteCount += length.decode(is);
		int lengthVal = length.val;

		if (lengthVal == 0) {
			return tlByteCount;
		}

		while (vByteCount < lengthVal || lengthVal < 0) {
			vByteCount += berTag.decode(is);
			if (berTag.equals(BerPrintableString.tag)) {
				printableString = new BerPrintableString();
				vByteCount += printableString.decode(is, false);
			}
			else if (berTag.equals(BerTeletexString.tag)) {
				teletexString = new BerTeletexString();
				vByteCount += teletexString.decode(is, false);
			}
			else if (lengthVal < 0 && berTag.equals(0, 0, 0)) {
				vByteCount += BerLength.readEocByte(is);
				return tlByteCount + vByteCount;
			}
			else {
				throw new IOException("Tag does not match any set component: " + berTag);
			}
		}
		if (vByteCount != lengthVal) {
			throw new IOException("Length of set does not match length tag, length tag: " + lengthVal + ", actual set length: " + vByteCount);
		}
		return tlByteCount + vByteCount;
	}

	public void encodeAndSave(int encodingSizeGuess) throws IOException {
		ReverseByteArrayOutputStream reverseOS = new ReverseByteArrayOutputStream(encodingSizeGuess);
		encode(reverseOS, false);
		code = reverseOS.getArray();
	}

	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		appendAsString(sb, 0);
		return sb.toString();
	}

	public void appendAsString(StringBuilder sb, int indentLevel) {

		sb.append("{");
		boolean firstSelectedElement = true;
		if (printableString != null) {
			sb.append("\n");
			for (int i = 0; i < indentLevel + 1; i++) {
				sb.append("\t");
			}
			sb.append("printableString: ").append(printableString);
			firstSelectedElement = false;
		}
		
		if (teletexString != null) {
			if (!firstSelectedElement) {
				sb.append(",\n");
			}
			for (int i = 0; i < indentLevel + 1; i++) {
				sb.append("\t");
			}
			sb.append("teletexString: ").append(teletexString);
		}
		
		sb.append("\n");
		for (int i = 0; i < indentLevel; i++) {
			sb.append("\t");
		}
		sb.append("}");
	}

}

