package asn1

import (
	"dsmagic.com/asn1"
	"io"
)

type BerGeneralizedTime struct {
	BerTime
}

var generalizedTimeTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.GENERALIZED_TIME_TAG)

func NewBerGeneralizedTime(value string) *BerGeneralizedTime {
	return &BerGeneralizedTime{BerTime: *NewBerTime(value)}
}

func (b *BerGeneralizedTime) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(generalizedTimeTag, reversedWriter, withTagList...)
}

func (b *BerGeneralizedTime) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(generalizedTimeTag, input, withTagList...)
}
