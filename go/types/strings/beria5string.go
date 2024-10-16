package strings

import (
	"dsmagic.com/asn1"
	asn12 "dsmagic.com/asn1/types"
	"io"
)

type BerIA5String struct {
	asn12.BerOctetString
}

var ia5stringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.IA5_STRING_TAG)

func NewBerIA5String(value []byte) *BerIA5String {
	return &BerIA5String{BerOctetString: *asn12.NewBerOctetString(value)}
}

func (b *BerIA5String) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(ia5stringTag, reversedWriter, withTagList...)
}

func (b *BerIA5String) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(ia5stringTag, input, withTagList...)
}
