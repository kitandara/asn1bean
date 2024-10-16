package asn1

import (
	"dsmagic.com/asn1"
	"io"
)

type BerDuration struct {
	BerTime
}

var durationTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.DURATION_TAG)

func NewBerDuration(value string) *BerDuration {
	return &BerDuration{BerTime: *NewBerTime(value)}
}

func (b *BerDuration) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(durationTag, reversedWriter, withTagList...)
}

func (b *BerDuration) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(durationTag, input, withTagList...)
}
