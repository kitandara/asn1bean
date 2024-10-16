package strings

import (
	"dsmagic.com/asn1"
	asn12 "dsmagic.com/asn1/types"
	"io"
)

type BerPrintableString struct {
	asn12.BerOctetString
}

var printablestringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.PRINTABLE_STRING_TAG)

func NewBerPrintableString(value []byte) *BerPrintableString {
	return &BerPrintableString{BerOctetString: *asn12.NewBerOctetString(value)}
}

func (b *BerPrintableString) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(printablestringTag, reversedWriter, withTagList...)
}

func (b *BerPrintableString) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(printablestringTag, input, withTagList...)
}
