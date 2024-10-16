package strings

import (
	"dsmagic.com/asn1"
	asn12 "dsmagic.com/asn1/types"
	"io"
)

type BerUniversalString struct {
	asn12.BerOctetString
}

var universalstringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.UNIVERSAL_STRING_TAG)

func NewBerUniversalString(value []byte) *BerUniversalString {
	return &BerUniversalString{BerOctetString: *asn12.NewBerOctetString(value)}
}

func (b *BerUniversalString) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(universalstringTag, reversedWriter, withTagList...)
}

func (b *BerUniversalString) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(universalstringTag, input, withTagList...)
}
