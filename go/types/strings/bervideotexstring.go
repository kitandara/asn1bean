package strings

import (
	"dsmagic.com/asn1"
	asn12 "dsmagic.com/asn1/types"
	"io"
)

type BerVideotexString struct {
	asn12.BerOctetString
}

var videotexstringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.VIDEOTEX_STRING_TAG)

func NewBerVideotexString(value []byte) *BerVideotexString {
	return &BerVideotexString{BerOctetString: *asn12.NewBerOctetString(value)}
}

func (b *BerVideotexString) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(videotexstringTag, reversedWriter, withTagList...)
}

func (b *BerVideotexString) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(videotexstringTag, input, withTagList...)
}
