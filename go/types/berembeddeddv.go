package asn1

import (
	"dsmagic.com/asn1"
	"dsmagic.com/asn1/types/strings"
	"errors"
	"fmt"
	"io"
)

var dvTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.CONSTRUCTED, asn1.CONSTRUCTED_TAG)

type BerEmbeddedPdv struct {
	identification      *Identification
	dataValueDescriptor *strings.BerObjectDescriptor
	dataValue           *BerOctetString
}

func (b *BerEmbeddedPdv) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}

	codeLength := 0
	subLength := 0
	n, err := b.dataValue.Encode(reversedWriter, false)
	codeLength += n
	if err != nil {
		return 0, err
	}
	// write tag: CONTEXT_CLASS, PRIMITIVE, 2
	n, err = asn1.WriteByte(reversedWriter, 0x82)
	codeLength += 1
	if err != nil {
		return 0, err
	}
	if b.dataValueDescriptor != nil {
		n, err = b.dataValueDescriptor.Encode(reversedWriter, false)
		if err != nil {
			return 0, err
		}
		// write tag: CONTEXT_CLASS, PRIMITIVE, 1
		n, err = asn1.WriteByte(reversedWriter, 0x81)
		codeLength += 1
	}
	subLength, err = b.identification.encode(reversedWriter)
	if err != nil {
		return 0, err
	}
	codeLength += subLength
	n, err = asn1.EncodeLength(subLength, reversedWriter)
	codeLength += n
	// write tag: CONTEXT_CLASS, CONSTRUCTED, 0
	n, err = asn1.WriteByte(reversedWriter, 0xA0)
	codeLength += 1
	if err != nil {
		return 0, err
	}
	n, err = asn1.EncodeLength(codeLength, reversedWriter)
	if err != nil {
		return 0, err
	}
	if withTag {
		n, err = dvTag.Encode(reversedWriter)
		codeLength += n
	}
	return codeLength, err
}

func (b *BerEmbeddedPdv) Decode(input io.Reader, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}
	tlByteCount := 0
	vByteCount := 0
	berTag := new(asn1.BerTag)

	codeLength := 0
	if withTag {
		n, err := dvTag.DecodeAndCheck(input)
		if err != nil {
			return 0, err
		}
		tlByteCount += n
	}

	berLength := &asn1.BerLength{}
	n, err := berLength.Decode(input)
	lengthVal := n
	if err != nil {
		return codeLength, err
	}
	tlByteCount += n
	n, err = berTag.Decode(input)
	vByteCount += n
	if err != nil {
		return 0, err
	}

	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.CONSTRUCTED, 0) {
		n, err = berLength.Decode(input)
		vByteCount += n
		b.identification = new(Identification)
		n, err = b.identification.decode(input, nil)
		if err != nil {
			return codeLength, err
		}
		vByteCount += n
		n, err = berLength.ReadEocIndefinite(input)
		if err != nil {
			return codeLength, err
		}
		vByteCount += n
		n, err = berTag.Decode(input)
		vByteCount += n
		if err != nil {
			return codeLength, err
		}
	} else {
		return 0, errors.New("tag does not match mandatory sequence component.")
	}
	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.PRIMITIVE, 1) {
		b.dataValueDescriptor = new(strings.BerObjectDescriptor)
		n, err = b.dataValueDescriptor.Decode(input, false)
		if err != nil {
			return codeLength, err
		}
		vByteCount += n
		n, err = berTag.Decode(input)
		if err != nil {
			return codeLength, err
		}
		vByteCount += n
	}

	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.PRIMITIVE, 2) {
		b.dataValue = new(BerOctetString)
		n, err = b.dataValue.Decode(input, false)
		vByteCount += n
		if err != nil {
			return codeLength, err
		}
		if lengthVal >= 0 && vByteCount == lengthVal {
			return tlByteCount + vByteCount, nil
		}
		n, err = berTag.Decode(input)
		vByteCount += n
		if err != nil {
			return codeLength, err
		}
	} else {
		return 0, errors.New("tag does not match mandatory sequence component.")
	}

	if lengthVal < 0 {
		if !berTag.Equals(0, 0, 0) {
			return 0, errors.New("decoded sequence has wrong end of contents octets")
		}
		err = asn1.ReadEocByte(input)
		vByteCount += 1
		return tlByteCount + vByteCount, nil
	}

	return codeLength, errors.New(fmt.Sprintf("Unexpected end of sequence, length tag: %d, bytes decode: %d", lengthVal, vByteCount))
}

func (b *BerEmbeddedPdv) S() string {
	return fmt.Sprintf("{EmbeddedPdv<<fields-not-included>}")
}

type Identification struct {
	syntaxes              *Syntaxes
	syntax                *BerObjectIdentifier
	presentationContextId *BerInteger
	contextNegotiation    *contextNegotiation
	transferSyntax        *BerObjectIdentifier
	fixed                 *BerNull
}

func (b *Identification) decode(input io.Reader, berTag *asn1.BerTag) (int, error) {
	tlvByteCount := 0
	tagWasPassed := (berTag != nil)

	if berTag == nil {
		berTag = new(asn1.BerTag)
		n, err := berTag.Decode(input)
		tlvByteCount += n
		if err != nil {
			return 0, err
		}
	}

	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.CONSTRUCTED, 0) {
		b.syntaxes = new(Syntaxes)
		n, err := b.syntaxes.Decode(input, false)
		tlvByteCount += n
		return tlvByteCount, err
	}

	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.PRIMITIVE, 1) {
		b.syntax = new(BerObjectIdentifier)
		n, err := b.syntax.Decode(input, false)
		tlvByteCount += n
		return tlvByteCount, err
	}
	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.PRIMITIVE, 2) {
		b.presentationContextId = new(BerInteger)
		n, err := b.presentationContextId.Decode(input, false)
		tlvByteCount += n
		return tlvByteCount, err
	}
	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.CONSTRUCTED, 3) {
		b.contextNegotiation = new(contextNegotiation)
		n, err := b.contextNegotiation.Decode(input, false)
		tlvByteCount += n
		return tlvByteCount, err
	}
	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.PRIMITIVE, 4) {
		b.transferSyntax = new(BerObjectIdentifier)
		n, err := b.transferSyntax.Decode(input, false)
		tlvByteCount += n
		return tlvByteCount, err
	}
	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.PRIMITIVE, 5) {
		b.fixed = new(BerNull)
		n, err := b.fixed.Decode(input, false)
		tlvByteCount += n
		return tlvByteCount, err
	}
	if tagWasPassed {
		return 0, nil
	} else {
		return 0, errors.New(fmt.Sprintf("error decoding CHOICE: TAG %s", berTag.S()))
	}
}

func (b *Identification) encode(reversedWriter io.Writer) (int, error) {

	codeLength := 0
	if b.fixed != nil {

		n, _ := b.fixed.Encode(reversedWriter, false)
		// write tag: CONTEXT_CLASS, PRIMITIVE, 5
		codeLength += n
		_, err := asn1.WriteByte(reversedWriter, 0x85)
		if err != nil {
			return codeLength, err
		}
		codeLength += 1
		return codeLength, nil
	}
	if b.transferSyntax != nil {

		n, _ := b.transferSyntax.Encode(reversedWriter, false)
		// write tag: CONTEXT_CLASS, PRIMITIVE, 4
		codeLength += n
		_, err := asn1.WriteByte(reversedWriter, 0x84)
		if err != nil {
			return codeLength, err
		}
		codeLength += 1
		return codeLength, nil
	}

	if b.contextNegotiation != nil {

		n, _ := b.contextNegotiation.Encode(reversedWriter, false)
		// write tag: CONTEXT_CLASS, PRIMITIVE, 3
		codeLength += n
		_, err := asn1.WriteByte(reversedWriter, 0xA3)
		if err != nil {
			return codeLength, err
		}
		codeLength += 1
		return codeLength, nil
	}

	if b.presentationContextId != nil {

		n, _ := b.presentationContextId.Encode(reversedWriter, false)
		// write tag: CONTEXT_CLASS, PRIMITIVE, 2
		codeLength += n
		_, err := asn1.WriteByte(reversedWriter, 0x82)
		if err != nil {
			return codeLength, err
		}
		codeLength += 1
		return codeLength, nil
	}

	if b.syntax != nil {

		n, _ := b.syntax.Encode(reversedWriter, false)
		// write tag: CONTEXT_CLASS, PRIMITIVE, 1
		codeLength += n
		_, err := asn1.WriteByte(reversedWriter, 0x81)
		if err != nil {
			return codeLength, err
		}
		codeLength += 1
		return codeLength, nil
	}

	if b.syntaxes != nil {

		n, _ := b.syntaxes.Encode(reversedWriter, false)
		// write tag: CONTEXT_CLASS, PRIMITIVE, 0
		codeLength += n
		_, err := asn1.WriteByte(reversedWriter, 0xA0)
		if err != nil {
			return codeLength, err
		}
		codeLength += 1
		return codeLength, nil
	}

	return codeLength, errors.New("error encoding CHOICE: No element of CHOICE was selected")
}

var syntaxesTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.CONSTRUCTED, 16)

type Syntaxes struct {
	abstract_ BerObjectIdentifier
	transfer  BerObjectIdentifier
}

func (b *Syntaxes) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}

	n, _ := b.transfer.Encode(reversedWriter, false)
	// write tag: CONTEXT_CLASS, PRIMITIVE, 1
	codeLength := n
	_, err := asn1.WriteByte(reversedWriter, 0x81)
	if err != nil {
		return codeLength, err
	}
	codeLength += 1
	n, err = b.abstract_.Encode(reversedWriter, false)
	if err != nil {
		return codeLength, err
	}
	codeLength += n
	// write tag: CONTEXT_CLASS, PRIMITIVE, 0
	_, err = asn1.WriteByte(reversedWriter, 0x80)
	if err != nil {
		return codeLength, err
	}
	codeLength += 1

	n, err = asn1.EncodeLength(codeLength, reversedWriter)
	if err != nil {
		return codeLength, err
	}
	codeLength += n
	if withTag {
		n, err = syntaxesTag.Encode(reversedWriter)
		if err != nil {
			return codeLength, err
		}
		codeLength += n
	}
	return codeLength, nil
}

func (b *Syntaxes) Decode(input io.Reader, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}

	tlByteCount := 0
	vByteCount := 0
	berTag := new(asn1.BerTag)

	if withTag {
		n, err := syntaxesTag.DecodeAndCheck(input)
		if err != nil {
			return 0, err
		}
		tlByteCount += n
	}
	berLength := &asn1.BerLength{}
	n, err := berLength.Decode(input)
	if err != nil {
		return 0, err
	}
	tlByteCount += n
	lengthVal := berLength.Length
	n, err = berTag.Decode(input)
	vByteCount += n
	if err != nil {
		return 0, err
	}

	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.PRIMITIVE, 0) {
		b.abstract_ = BerObjectIdentifier{}
		n, err = b.abstract_.Decode(input, false)
		vByteCount += n
		n, _ = berTag.Decode(input)
		vByteCount += n
	} else {
		return 0, errors.New("tag does not match mandatory sequence component.")
	}

	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.PRIMITIVE, 1) {
		b.transfer = BerObjectIdentifier{}
		n, _ = b.transfer.Decode(input, false)
		vByteCount += n
		if lengthVal >= 0 && vByteCount == lengthVal {
			return tlByteCount + vByteCount, nil
		}
		n, _ = berTag.Decode(input)
		vByteCount += n
	} else {
		return 0, errors.New("tag does not match mandatory sequence component.")
	}

	if lengthVal < 0 {
		if !berTag.Equals(0, 0, 0) {
			return 0, errors.New("decoded sequence has wrong end of contents octets")
		}
		_ = asn1.ReadEocByte(input)
		vByteCount += 1
		return tlByteCount + vByteCount, nil
	}
	return 0, errors.New(fmt.Sprintf("unexpected end of sequence, length tag: %d, bytes decoded: %d", lengthVal, vByteCount))
}

var contextNegotiationTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.CONSTRUCTED, 16)

type contextNegotiation struct {
	presentationContextId BerInteger
	transferSyntax        BerObjectIdentifier
}

func (b *contextNegotiation) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}

	codeLength := 0
	n, err := b.transferSyntax.Encode(reversedWriter, false)
	codeLength += n
	if err != nil {
		return codeLength, err
	}
	// write tag: CONTEXT_CLASS, PRIMITIVE, 1
	_, err = asn1.WriteByte(reversedWriter, 0x81)
	if err != nil {
		return codeLength, err
	}
	codeLength += 1
	n, err = b.presentationContextId.Encode(reversedWriter, false)
	codeLength += n
	if err != nil {
		return codeLength, err
	}

	// write tag: CONTEXT_CLASS, PRIMITIVE, 0
	_, err = asn1.WriteByte(reversedWriter, 0x80)
	if err != nil {
		return codeLength, err
	}
	codeLength += 1

	n, err = asn1.EncodeLength(codeLength, reversedWriter)
	if err != nil {
		return codeLength, err
	}
	codeLength += n
	if withTag {
		n, err = contextNegotiationTag.Encode(reversedWriter)
		if err != nil {
			return codeLength, err
		}
		codeLength += n
	}
	return codeLength, nil
}

func (b *contextNegotiation) Decode(input io.Reader, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}

	tlByteCount := 0
	vByteCount := 0
	berTag := new(asn1.BerTag)

	if withTag {
		n, err := contextNegotiationTag.DecodeAndCheck(input)
		if err != nil {
			return 0, err
		}
		tlByteCount += n
	}
	berLength := &asn1.BerLength{}
	n, err := berLength.Decode(input)
	if err != nil {
		return 0, err
	}
	tlByteCount += n
	lengthVal := berLength.Length
	n, err = berTag.Decode(input)
	vByteCount += n
	if err != nil {
		return 0, err
	}

	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.PRIMITIVE, 0) {
		b.presentationContextId = BerInteger{}
		n, err = b.presentationContextId.Decode(input, false)
		vByteCount += n
		n, _ = berTag.Decode(input)
		vByteCount += n
	} else {
		return 0, errors.New("tag does not match mandatory sequence component.")
	}

	if berTag.Equals(asn1.CONTEXT_CLASS, asn1.PRIMITIVE, 1) {
		b.transferSyntax = BerObjectIdentifier{}
		n, _ = b.transferSyntax.Decode(input, false)
		vByteCount += n
		if lengthVal >= 0 && vByteCount == lengthVal {
			return tlByteCount + vByteCount, nil
		}
		n, _ = berTag.Decode(input)
		vByteCount += n
	} else {
		return 0, errors.New("tag does not match mandatory sequence component.")
	}

	if lengthVal < 0 {
		if !berTag.Equals(0, 0, 0) {
			return 0, errors.New("decoded sequence has wrong end of contents octets")
		}
		_ = asn1.ReadEocByte(input)
		vByteCount += 1
		return tlByteCount + vByteCount, nil
	}
	return 0, errors.New(fmt.Sprintf("unexpected end of sequence, length tag: %d, bytes decoded: %d", lengthVal, vByteCount))
}
