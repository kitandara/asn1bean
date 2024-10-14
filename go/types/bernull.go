package asn1

import (
	"dsmagic.com/asn1"
	"errors"
	"io"
)

type BerNull struct {
}

var nullTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.NULL_TAG)

func (b *BerNull) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = false
	}

	codeLength, err := asn1.EncodeLength(0, reversedWriter)
	if err != nil {
		return 0, err
	}
	if withTag {
		n, err := nullTag.Encode(reversedWriter)
		codeLength += n
		if err != nil {
			return 0, err
		}
	}
	return codeLength, nil
}

func (b *BerNull) Decode(input io.Reader, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}
	codeLength := 0
	if withTag {
		n, err := nullTag.DecodeAndCheck(input)
		codeLength += n

		if err != nil {
			return codeLength, err
		}
	}

	berLength := &asn1.BerLength{}
	n, err := berLength.Decode(input)
	codeLength += n
	if err != nil {
		return codeLength, err
	}
	if berLength.Length != 0 {
		return codeLength, errors.New("BerNull length is incorrect")
	}
	return codeLength, nil
}

func (b *BerNull) S() string {
	return "ASN1_NULL"
}
