package asn1

import (
	"dsmagic.com/asn1"
	"encoding/hex"
	"errors"
	"fmt"
	"io"
	"slices"
)

type BerOctetString struct {
	value []byte
}

var octetStringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.OCTET_STRING_TAG)

func NewBerOctetString(v []byte) *BerOctetString {
	return &BerOctetString{value: v}
}

func (b *BerOctetString) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = false
	}

	codeLength := len(b.value)

	_, err := reversedWriter.Write(b.value)
	if err != nil {
		return codeLength, err
	}
	n, err := asn1.EncodeLength(codeLength, reversedWriter)
	if err != nil {
		return codeLength, err
	}
	codeLength += n
	if withTag {
		n, err = octetStringTag.Encode(reversedWriter)
		codeLength += n
	}
	return codeLength, err
}

func (b *BerOctetString) Decode(input io.Reader, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}
	codeLength := 0
	if withTag {
		nextByte, err := asn1.ReadByte(input)
		if err != nil {
			return codeLength, err
		}

		n := 0
		switch nextByte {
		case -1:
			return codeLength, errors.New("unexpected end of input")
		case 0x04:
			n, err = b.decodePrimitiveOctetString(input)
			return 1 + n, err
		case 0x24:
			n, err = b.decodeConstructedOctetString(input)
			return 1 + n, err
		default:
			return codeLength, errors.New(fmt.Sprintf("octet String identifier does not match, expected: 0x04 or 0x24, received: 0x%02x", nextByte))
		}
	}

	return b.decodePrimitiveOctetString(input)
}

func (b *BerOctetString) decodePrimitiveOctetString(input io.Reader) (int, error) {
	codeLength := 0
	berLength := &asn1.BerLength{}
	n, err := berLength.Decode(input)
	codeLength += n
	if err != nil {
		return codeLength, err
	}
	b.value = make([]byte, berLength.Length)
	if berLength.Length != 0 {
		n, err := io.ReadFull(input, b.value)
		codeLength += n
		if err != nil {
			return codeLength, err
		}
	}
	return codeLength, nil
}

func (b *BerOctetString) decodeConstructedOctetString(input io.Reader) (int, error) {
	codeLength := 0
	berLength := &asn1.BerLength{}
	n, err := berLength.Decode(input)
	if err != nil {
		return codeLength, err
	}
	b.value = []byte{} // Zero length
	vLength := 0

	if berLength.Length < 0 {
		berTag := new(asn1.BerTag)
		n, err := berTag.Decode(input)
		vLength += n
		for !berTag.Equals(0, 0, 0) {
			subOctetString := new(BerOctetString)
			n, err = subOctetString.Decode(input, false)
			vLength += n
			if err != nil {
				return 0, err
			}
			b.value = slices.Concat(b.value, subOctetString.value)
			n, err = berTag.Decode(input)
			vLength += n
			if err != nil {
				return 0, err
			}
		}
		_ = asn1.ReadEocByte(input)
		vLength += 1
	} else {
		for vLength < berLength.Length {
			subOctetString := new(BerOctetString)
			n, err = subOctetString.Decode(input)
			vLength += n
			if err != nil {
				return 0, err
			}
			b.value = slices.Concat(b.value, subOctetString.value)
		}
	}
	return berLength.Length + vLength, nil
}

func (b *BerOctetString) S() string {
	return hex.EncodeToString(b.value)
}
