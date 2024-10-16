package asn1

import (
	"dsmagic.com/asn1"
	"io"
)

type BerDateTime struct {
	BerTime
}

var dateTimeTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.DATE_TIME_TAG)

func NewBerDateTime(value string) *BerDateTime {
	return &BerDateTime{BerTime: *NewBerTime(value)}
}

func (b *BerDateTime) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(dateTimeTag, reversedWriter, withTagList...)
}

func (b *BerDateTime) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(dateTimeTag, input, withTagList...)
}
