package asn1

import (
	"dsmagic.com/asn1"
	"errors"
	"io"
	"math/big"
)

type BerInteger struct {
	value *big.Int
}

var intTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.INTEGER_TAG)

func NewBerInteger(v int64) *BerInteger {
	return &BerInteger{value: new(big.Int).SetInt64(v)}
}

func (b *BerInteger) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = false
	}

	encoded := b.value.Bytes()
	codeLength := len(encoded)

	_, err := reversedWriter.Write(encoded)
	if err != nil {
		return codeLength, err
	}
	n, err := asn1.EncodeLength(codeLength, reversedWriter)
	if err != nil {
		return codeLength, err
	}
	codeLength += n
	if withTag {
		n, err = intTag.Encode(reversedWriter)
		codeLength += n
	}
	return codeLength, err
}

func (b *BerInteger) Decode(input io.Reader, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}
	codeLength := 0
	if withTag {
		n, err := intTag.DecodeAndCheck(input)
		codeLength += n
		if err != nil {
			return codeLength, err
		}
	}
	berLength := &asn1.BerLength{}
	n, err := berLength.Decode(input)
	codeLength += n
	if err != nil {
		return codeLength, err
	}
	if berLength.Length < 1 {
		return codeLength, errors.New("invalid integer length")
	}
	byteCode := make([]byte, berLength.Length)
	_, err = io.ReadFull(input, byteCode)
	if err != nil {
		return codeLength, err
	}
	b.value.SetBytes(byteCode)

	return codeLength, nil
}

func (b *BerInteger) S() string {
	return b.value.String()
}

func (b *BerInteger) longValue() int64 {
	return b.value.Int64()
}
