package strings

import (
	"dsmagic.com/asn1"
	asn12 "dsmagic.com/asn1/types"
	"io"
)

type BerBmpString struct {
	asn12.BerOctetString
}

var bmpstringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.BMP_STRING_TAG)

func NewBerBmpString(value []byte) *BerBmpString {
	return &BerBmpString{BerOctetString: *asn12.NewBerOctetString(value)}
}

func (b *BerBmpString) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(bmpstringTag, reversedWriter, withTagList...)
}

func (b *BerBmpString) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(bmpstringTag, input, withTagList...)
}
