package strings

import (
	"dsmagic.com/asn1"
	asn12 "dsmagic.com/asn1/types"
	"io"
)

type BerGeneralString struct {
	asn12.BerOctetString
}

var generalstringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.GENERAL_STRING_TAG)

func NewBerGeneralString(value []byte) *BerGeneralString {
	return &BerGeneralString{BerOctetString: *asn12.NewBerOctetString(value)}
}

func (b *BerGeneralString) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(generalstringTag, reversedWriter, withTagList...)
}

func (b *BerGeneralString) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(generalstringTag, input, withTagList...)
}
