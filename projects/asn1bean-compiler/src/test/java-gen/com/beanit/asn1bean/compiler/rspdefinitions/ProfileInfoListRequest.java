/*
 * This class file was automatically generated by ASN1bean (http://www.beanit.com)
 */

package com.beanit.asn1bean.compiler.rspdefinitions;

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

import com.beanit.asn1bean.compiler.pkix1explicit88.Certificate;
import com.beanit.asn1bean.compiler.pkix1explicit88.CertificateList;
import com.beanit.asn1bean.compiler.pkix1explicit88.Time;
import com.beanit.asn1bean.compiler.pkix1implicit88.SubjectKeyIdentifier;

public class ProfileInfoListRequest implements BerType, Serializable {

	private static final long serialVersionUID = 1L;

	public static class SearchCriteria implements BerType, Serializable {

		private static final long serialVersionUID = 1L;

		private byte[] code = null;
		public OctetTo16 isdpAid = null;
		public Iccid iccid = null;
		public ProfileClass profileClass = null;
		
		public SearchCriteria() {
		}

		public SearchCriteria(byte[] code) {
			this.code = code;
		}

		public SearchCriteria(OctetTo16 isdpAid, Iccid iccid, ProfileClass profileClass) {
			this.isdpAid = isdpAid;
			this.iccid = iccid;
			this.profileClass = profileClass;
		}

		@Override public int encode(OutputStream reverseOS) throws IOException {

			if (code != null) {
				reverseOS.write(code);
				return code.length;
			}

			int codeLength = 0;
			if (profileClass != null) {
				codeLength += profileClass.encode(reverseOS, false);
				// write tag: CONTEXT_CLASS, PRIMITIVE, 21
				reverseOS.write(0x95);
				codeLength += 1;
				return codeLength;
			}
			
			if (iccid != null) {
				codeLength += iccid.encode(reverseOS, true);
				return codeLength;
			}
			
			if (isdpAid != null) {
				codeLength += isdpAid.encode(reverseOS, false);
				// write tag: APPLICATION_CLASS, PRIMITIVE, 15
				reverseOS.write(0x4F);
				codeLength += 1;
				return codeLength;
			}
			
			throw new IOException("Error encoding CHOICE: No element of CHOICE was selected.");
		}

		@Override public int decode(InputStream is) throws IOException {
			return decode(is, null);
		}

		public int decode(InputStream is, BerTag berTag) throws IOException {

			int tlvByteCount = 0;
			boolean tagWasPassed = (berTag != null);

			if (berTag == null) {
				berTag = new BerTag();
				tlvByteCount += berTag.decode(is);
			}

			if (berTag.equals(BerTag.APPLICATION_CLASS, BerTag.PRIMITIVE, 15)) {
				isdpAid = new OctetTo16();
				tlvByteCount += isdpAid.decode(is, false);
				return tlvByteCount;
			}

			if (berTag.equals(Iccid.tag)) {
				iccid = new Iccid();
				tlvByteCount += iccid.decode(is, false);
				return tlvByteCount;
			}

			if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.PRIMITIVE, 21)) {
				profileClass = new ProfileClass();
				tlvByteCount += profileClass.decode(is, false);
				return tlvByteCount;
			}

			if (tagWasPassed) {
				return 0;
			}

			throw new IOException("Error decoding CHOICE: Tag " + berTag + " matched to no item.");
		}

		public void encodeAndSave(int encodingSizeGuess) throws IOException {
			ReverseByteArrayOutputStream reverseOS = new ReverseByteArrayOutputStream(encodingSizeGuess);
			encode(reverseOS);
			code = reverseOS.getArray();
		}

		@Override public String toString() {
			StringBuilder sb = new StringBuilder();
			appendAsString(sb, 0);
			return sb.toString();
		}

		public void appendAsString(StringBuilder sb, int indentLevel) {

			if (isdpAid != null) {
				sb.append("isdpAid: ").append(isdpAid);
				return;
			}

			if (iccid != null) {
				sb.append("iccid: ").append(iccid);
				return;
			}

			if (profileClass != null) {
				sb.append("profileClass: ").append(profileClass);
				return;
			}

			sb.append("<none>");
		}

	}

	public static final BerTag tag = new BerTag(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 45);

	private byte[] code = null;
	public SearchCriteria searchCriteria = null;
	public BerOctetString tagList = null;
	
	public ProfileInfoListRequest() {
	}

	public ProfileInfoListRequest(byte[] code) {
		this.code = code;
	}

	public ProfileInfoListRequest(SearchCriteria searchCriteria, BerOctetString tagList) {
		this.searchCriteria = searchCriteria;
		this.tagList = tagList;
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
		int sublength;

		if (tagList != null) {
			codeLength += tagList.encode(reverseOS, false);
			// write tag: APPLICATION_CLASS, PRIMITIVE, 28
			reverseOS.write(0x5C);
			codeLength += 1;
		}
		
		if (searchCriteria != null) {
			sublength = searchCriteria.encode(reverseOS);
			codeLength += sublength;
			codeLength += BerLength.encodeLength(reverseOS, sublength);
			// write tag: CONTEXT_CLASS, CONSTRUCTED, 0
			reverseOS.write(0xA0);
			codeLength += 1;
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
		vByteCount += berTag.decode(is);

		if (berTag.equals(BerTag.CONTEXT_CLASS, BerTag.CONSTRUCTED, 0)) {
			vByteCount += length.decode(is);
			searchCriteria = new SearchCriteria();
			vByteCount += searchCriteria.decode(is, null);
			vByteCount += length.readEocIfIndefinite(is);
			if (lengthVal >= 0 && vByteCount == lengthVal) {
				return tlByteCount + vByteCount;
			}
			vByteCount += berTag.decode(is);
		}
		
		if (berTag.equals(BerTag.APPLICATION_CLASS, BerTag.PRIMITIVE, 28)) {
			tagList = new BerOctetString();
			vByteCount += tagList.decode(is, false);
			if (lengthVal >= 0 && vByteCount == lengthVal) {
				return tlByteCount + vByteCount;
			}
			vByteCount += berTag.decode(is);
		}
		
		if (lengthVal < 0) {
			if (!berTag.equals(0, 0, 0)) {
				throw new IOException("Decoded sequence has wrong end of contents octets");
			}
			vByteCount += BerLength.readEocByte(is);
			return tlByteCount + vByteCount;
		}

		throw new IOException("Unexpected end of sequence, length tag: " + lengthVal + ", bytes decoded: " + vByteCount);

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
		if (searchCriteria != null) {
			sb.append("\n");
			for (int i = 0; i < indentLevel + 1; i++) {
				sb.append("\t");
			}
			sb.append("searchCriteria: ");
			searchCriteria.appendAsString(sb, indentLevel + 1);
			firstSelectedElement = false;
		}
		
		if (tagList != null) {
			if (!firstSelectedElement) {
				sb.append(",\n");
			}
			for (int i = 0; i < indentLevel + 1; i++) {
				sb.append("\t");
			}
			sb.append("tagList: ").append(tagList);
		}
		
		sb.append("\n");
		for (int i = 0; i < indentLevel; i++) {
			sb.append("\t");
		}
		sb.append("}");
	}

}

