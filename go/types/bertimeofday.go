package asn1

import (
	"dsmagic.com/asn1"
	"io"
)

type BerTimeOfDay struct {
	BerTime
}

var timeOfDayTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.TIME_OF_DAY_TAG)

func NewBerTimeOfDay(value string) *BerTimeOfDay {
	return &BerTimeOfDay{BerTime: *NewBerTime(value)}
}

func (b *BerTimeOfDay) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(timeOfDayTag, reversedWriter, withTagList...)
}

func (b *BerTimeOfDay) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(timeOfDayTag, input, withTagList...)
}
