package asn1

import (
	"bytes"
	"dsmagic.com/asn1"
	"encoding/hex"
	"io"
)

// BerAny  Any BER object
type BerAny struct {
	value []byte
}

func (b *BerAny) Decode(r io.Reader, withTagList ...bool) (int, error) {
	return b.DecodeWithTag(r, nil)
}

func (b *BerAny) DecodeWithTag(r io.Reader, tag *asn1.BerTag) (int, error) {
	os := &bytes.Buffer{}

	byteCount := 0
	if tag == nil {
		tag = new(asn1.BerTag)
		n, err := tag.Decode(r)
		byteCount += n
		if err != nil {
			return byteCount, err
		}
	}
	_, _ = tag.Encode(os)
	n, err := asn1.DecodeUnknownComponent(r, os)
	byteCount += n
	if err != nil {
		return byteCount, err
	}
	b.value = os.Bytes() // get the bytes

	return byteCount, nil
}

func (b *BerAny) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	return reversedWriter.Write(b.value)
}

func (b *BerAny) S() string {
	return hex.EncodeToString(b.value)
}
