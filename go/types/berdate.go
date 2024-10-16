package asn1

import (
	"dsmagic.com/asn1"
	"io"
)

type BerDate struct {
	BerTime
}

var dateTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.DATE_TAG)

func NewBerDate(value string) *BerDate {
	return &BerDate{BerTime: *NewBerTime(value)}
}

func (b *BerDate) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(dateTag, reversedWriter, withTagList...)
}

func (b *BerDate) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(dateTag, input, withTagList...)
}
