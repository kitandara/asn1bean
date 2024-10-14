package asn1

import (
	"bytes"
	"dsmagic.com/asn1"
	"errors"
	"fmt"
	"io"
)

type BerBitString struct {
	value   []byte
	numBits int
}

var bitStringTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.BIT_STRING_TAG)

func NewBerBitString(value []bool) (*BerBitString, error) {
	if value == nil {
		return nil, errors.New("value cannot be null")
	}
	numBits := len(value)
	b := &BerBitString{
		numBits: numBits,
		value:   make([]byte, (numBits+7)/8),
	}
	for i := 0; i < numBits; i++ {
		if value[i] {
			b.value[i/8] = (byte)(b.value[i/8] | (1 << (7 - (i % 8))))
		}
	}
	return b, nil
}

func (b *BerBitString) getValueAsBooleans() []bool {
	if b.value == nil {
		return nil
	}

	vals := make([]bool, b.numBits)
	for i := 0; i < b.numBits; i++ {
		vals[i] = (b.value[i/8] & (1 << (7 - (i % 8)))) > 0
	}
	return vals
}

func (b *BerBitString) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}

	for i := len(b.value) - 1; i >= 0; i-- {
		_, err := reversedWriter.Write([]byte{b.value[i]})
		if err != nil {
			return 0, err
		}
	}
	codeLength := len(b.value) + 1
	n, err := asn1.EncodeLength(codeLength, reversedWriter)
	if err != nil {
		return 0, err
	}
	codeLength += n

	if withTag {
		n, err = bitStringTag.Encode(reversedWriter)
		codeLength += n
	}
	return codeLength, err
}

func (b *BerBitString) Decode(input io.Reader, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = false
	}
	codeLength := 0
	if withTag {
		n, err := bitStringTag.DecodeAndCheck(input)
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
	b.value = make([]byte, berLength.Length-1)

	bx := []byte{0}
	n, err = input.Read(bx)
	unusedBits := int(bx[0])
	if err != nil {
		return codeLength, err
	}
	if unusedBits > 7 {
		return codeLength, errors.New(fmt.Sprintf("Number of unused bits should be less than 8, found: %d", unusedBits))
	}
	b.numBits = len(b.value)*8 - unusedBits
	if len(b.value) > 0 {
		_, err = io.ReadFull(input, b.value)
		if err != nil {
			return codeLength, err
		}
	}
	codeLength += len(b.value) + 1
	return codeLength, nil
}

func (b *BerBitString) S() string {
	var buffer bytes.Buffer
	for _, bit := range b.getValueAsBooleans() {
		if bit {
			buffer.WriteString("1")
		} else {
			buffer.WriteString("0")
		}
	}
	return buffer.String()
}
