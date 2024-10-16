package asn1

import (
	"dsmagic.com/asn1"
	string2 "dsmagic.com/asn1/types/strings"
	"io"
)

type BerTime struct {
	string2.BerVisibleString
}

var timeTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.TIME_TAG)

func NewBerTime(value string) *BerTime {
	return &BerTime{BerVisibleString: *string2.NewBerVisibleString(value)}
}

func (b *BerTime) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(timeTag, reversedWriter, withTagList...)
}

func (b *BerTime) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(timeTag, input, withTagList...)
}
