package strings

import (
	"dsmagic.com/asn1"
	asn12 "dsmagic.com/asn1/types"
	"io"
)

type BerNumericString struct {
	asn12.BerOctetString
}

var numericstringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.NUMERIC_STRING_TAG)

func NewBerNumericString(value []byte) *BerNumericString {
	return &BerNumericString{BerOctetString: *asn12.NewBerOctetString(value)}
}

func (b *BerNumericString) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(numericstringTag, reversedWriter, withTagList...)
}

func (b *BerNumericString) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(numericstringTag, input, withTagList...)
}
