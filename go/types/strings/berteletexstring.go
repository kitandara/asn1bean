package strings

import (
	"dsmagic.com/asn1"
	asn12 "dsmagic.com/asn1/types"
	"io"
)

type BerTeletexString struct {
	asn12.BerOctetString
}

var teletexstringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.TELETEX_STRING_TAG)

func NewBerTeletexString(value []byte) *BerTeletexString {
	return &BerTeletexString{BerOctetString: *asn12.NewBerOctetString(value)}
}

func (b *BerTeletexString) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(teletexstringTag, reversedWriter, withTagList...)
}

func (b *BerTeletexString) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(teletexstringTag, input, withTagList...)
}
