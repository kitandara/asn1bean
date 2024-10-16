package asn1

import (
	"dsmagic.com/asn1"
	"io"
)

type BerUtcTime struct {
	BerTime
}

var utcTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.UTC_TIME_TAG)

func NewBerUtcTime(value string) *BerUtcTime {
	return &BerUtcTime{BerTime: *NewBerTime(value)}
}

func (b *BerUtcTime) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(utcTag, reversedWriter, withTagList...)
}

func (b *BerUtcTime) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(utcTag, input, withTagList...)
}
