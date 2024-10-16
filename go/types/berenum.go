package asn1

import (
	"dsmagic.com/asn1"
	"io"
)

// Extends an BERInteger

type BerEnum struct {
	BerInteger
}

var enumTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.ENUMERATED_TAG)

func NewBerEnum(v int64) *BerEnum {
	return &BerEnum{
		BerInteger: *NewBerInteger(v)}
}

func (b *BerEnum) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.encodeUsingTag(enumTag, reversedWriter, withTagList...)
}

func (b *BerEnum) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.decodeUsingTag(enumTag, input, withTagList...)
}
