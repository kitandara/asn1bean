package strings

import (
	"dsmagic.com/asn1"
	"io"
)

type BerObjectDescriptor struct {
	BerGraphicString
}

var objectDescriptorTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.OBJECT_DESCRIPTOR_TAG)

func NewBerObjectDescriptor(value []byte) *BerObjectDescriptor {
	return &BerObjectDescriptor{BerGraphicString: *NewBerGraphicString(value)}
}

func (b *BerObjectDescriptor) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return b.EncodeUsingTag(objectDescriptorTag, reversedWriter, withTagList...)
}

func (b *BerObjectDescriptor) Decode(input io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeUsingTag(objectDescriptorTag, input, withTagList...)
}
