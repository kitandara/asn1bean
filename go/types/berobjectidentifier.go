package asn1

import (
	"bytes"
	"dsmagic.com/asn1"
	"errors"
	"fmt"
	"io"
	"math"
)

type BerObjectIdentifier struct {
	value []int
}

var objectIdentifierTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.OBJECT_IDENTIFIER_TAG)

func NewBerObjectIdentifier(value []int) *BerObjectIdentifier {
	return &BerObjectIdentifier{value: value} // Do not check for now...
}

func (b *BerObjectIdentifier) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}
	firstSubidentifier := 40*b.value[0] + b.value[1]

	var subidentifier int
	codeLength := 0

	for i := len(b.value) - 1; i > 0; i-- {

		if i == 1 {
			subidentifier = firstSubidentifier
		} else {
			subidentifier = b.value[i]
		}

		// get length of subidentifier
		subIDLength := 1
		for subidentifier > int(math.Pow(2, float64(7*subIDLength)))-1 {
			subIDLength++
		}

		_, _ = asn1.WriteByte(reversedWriter, subidentifier&0x7f)

		for j := 1; j <= (subIDLength - 1); j++ {
			_, _ = asn1.WriteByte(((subidentifier >> (7 * j)) & 0xff) | 0x80)
		}

		codeLength += subIDLength
	}
	n, _ := asn1.EncodeLength(codeLength, reversedWriter)
	codeLength += n
	if withTag {
		n, _ = objectIdentifierTag.Encode(reversedWriter)
		codeLength += n
	}
	return codeLength, nil
}

func (b *BerObjectIdentifier) Decode(input io.Reader, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}
	codeLength := 0
	if withTag {
		n, err := objectIdentifierTag.DecodeAndCheck(input)
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

	if berLength.Length == 0 {
		b.value = []int{0}
		return codeLength, err
	}

	byteCode := make([]byte, berLength.Length)
	n, err = io.ReadFull(input, byteCode)
	if err != nil {
		return 0, err
	}
	codeLength += n
	objectIdentifierComponentsList := make([]int, 0)

	subIDEndIndex := 0
	for (byteCode[subIDEndIndex] & 0x80) == 0x80 {
		if subIDEndIndex >= (berLength.Length - 1) {
			return codeLength, errors.New("invalid Object Identifier")
		}
		subIDEndIndex++
	}

	subIdentifier := 0
	for i := 0; i <= subIDEndIndex; i++ {
		subIdentifier |= (byteCode[i] & 0x7f) << ((subIDEndIndex - i) * 7)
	}

	if subIdentifier < 40 {
		objectIdentifierComponentsList = append(objectIdentifierComponentsList, 0, subIdentifier)
	} else if subIdentifier < 80 {
		objectIdentifierComponentsList = append(objectIdentifierComponentsList, 1, subIdentifier-40)
	} else {
		objectIdentifierComponentsList = append(objectIdentifierComponentsList, 2, subIdentifier-80)
	}

	subIDEndIndex++

	for subIDEndIndex < berLength.Length {
		subIDStartIndex := subIDEndIndex

		for (byteCode[subIDEndIndex] & 0x80) == 0x80 {
			if subIDEndIndex == (berLength.Length - 1) {
				return codeLength, errors.New("invalid Object Identifier")
			}
			subIDEndIndex++
		}
		subIdentifier = 0
		for j := subIDStartIndex; j <= subIDEndIndex; j++ {
			subIdentifier |= (byteCode[j] & 0x7f) << ((subIDEndIndex - j) * 7)
		}
		objectIdentifierComponentsList = append(objectIdentifierComponentsList, subIdentifier)
		subIDEndIndex++
	}

	b.value = objectIdentifierComponentsList
	return codeLength, err
}

func (b *BerObjectIdentifier) S() string {
	var buffer bytes.Buffer
	sep := ""
	for _, i := range b.value {
		buffer.WriteString(fmt.Sprintf("%s%d", sep, i))
		sep = "."
	}
	return buffer.String()
}
