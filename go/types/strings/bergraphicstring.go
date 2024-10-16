package strings

import (
	asn1 "dsmagic.com/asn1"
	asn12 "dsmagic.com/asn1/types"
	"io"
)

type BerGraphicString struct {
	asn12.BerOctetString
}

var graphicStringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.GRAPHIC_STRING_TAG)

func NewBerGraphicString(value []byte) *BerGraphicString {
	return &BerGraphicString{BerOctetString: *asn12.NewBerOctetString(value)}
}

func (b *BerGraphicString) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(graphicStringTag, reversedWriter, withTagList...)
}

func (b *BerGraphicString) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(graphicStringTag, input, withTagList...)
}
