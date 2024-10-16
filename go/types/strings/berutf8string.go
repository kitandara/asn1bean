package strings

import (
	"dsmagic.com/asn1"
	asn12 "dsmagic.com/asn1/types"
	"io"
)

type BerUTF8String struct {
	asn12.BerOctetString
}

var utf8stringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.UTF8_STRING_TAG)

func NewBerUTF8String(value []byte) *BerUTF8String {
	return &BerUTF8String{BerOctetString: *asn12.NewBerOctetString(value)}
}

func (b *BerUTF8String) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(utf8stringTag, reversedWriter, withTagList...)
}

func (b *BerUTF8String) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(utf8stringTag, input, withTagList...)
}
