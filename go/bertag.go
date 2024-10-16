package asn1

import (
	"errors"
	"fmt"
	"io"
	"math"
)

const UNIVERSAL_CLASS = 0x00
const APPLICATION_CLASS = 0x40
const CONTEXT_CLASS = 0x80
const PRIVATE_CLASS = 0xc0
const PRIMITIVE = 0x00
const CONSTRUCTED = 0x20
const BOOLEAN_TAG = 1
const INTEGER_TAG = 2
const BIT_STRING_TAG = 3
const OCTET_STRING_TAG = 4
const NULL_TAG = 5
const OBJECT_IDENTIFIER_TAG = 6
const OBJECT_DESCRIPTOR_TAG = 7
const REAL_TAG = 9
const ENUMERATED_TAG = 10
const CONSTRUCTED_TAG = 11
const UTF8_STRING_TAG = 12
const TIME_TAG = 14
const SEQUENCE_TAG = 16
const SET_TAG = 17
const NUMERIC_STRING_TAG = 18
const PRINTABLE_STRING_TAG = 19
const TELETEX_STRING_TAG = 20
const VIDEOTEX_STRING_TAG = 21
const IA5_STRING_TAG = 22
const UTC_TIME_TAG = 23
const GENERALIZED_TIME_TAG = 24
const GRAPHIC_STRING_TAG = 25
const VISIBLE_STRING_TAG = 26
const GENERAL_STRING_TAG = 27
const UNIVERSAL_STRING_TAG = 28
const BMP_STRING_TAG = 30
const DATE_TAG = 31
const TIME_OF_DAY_TAG = 32
const DATE_TIME_TAG = 33
const DURATION_TAG = 34

var SEQUENCE = &BerTag{tagClass: UNIVERSAL_CLASS, primitive: CONSTRUCTED, tagNumber: SEQUENCE_TAG}
var SET = &BerTag{tagClass: UNIVERSAL_CLASS, primitive: CONSTRUCTED, tagNumber: SET_TAG}

type BerTag struct {
	tagClass  int
	primitive int
	tagNumber int
	tagBytes  []byte
}

type Tagged interface {
	Tag() *BerTag
}

func NewBerTag(tagClass int, primitive int, tagNumber int) *BerTag {
	return &BerTag{tagClass: tagClass, primitive: primitive, tagNumber: tagNumber}
}

func (tag *BerTag) code() {
	if tag.tagNumber < 31 {
		tag.tagBytes = []byte{byte(tag.tagClass | tag.primitive | tag.tagNumber)}
	} else {
		tagLength := 1
		for tag.tagNumber > int(math.Pow(2, float64(7*tagLength))-1) {
			tagLength++
		}

		tag.tagBytes = make([]byte, 1+tagLength)
		tag.tagBytes[0] = byte(tag.tagClass | tag.primitive | 31)
		for j := 1; j <= tagLength-1; j++ {
			tag.tagBytes[j] = (byte)(((tag.tagNumber >> (7 * (tagLength - j))) & 0xff) | 0x80)
		}
		tag.tagBytes[tagLength] = (byte)(tag.tagNumber & 0x7f)
	}
}

func (tag *BerTag) Encode(w io.Writer) (int, error) {
	if tag.tagBytes == nil {
		tag.code()
	}
	for i := len(tag.tagBytes) - 1; i >= 0; i-- {
		_, err := w.Write([]byte{tag.tagBytes[i]})
		if err != nil {
			return 0, err
		}
	}
	return len(tag.tagBytes), nil
}

func (tag *BerTag) Decode(input io.Reader) (int, error) {
	b := []byte{0}
	_, err := input.Read(b)
	if err != nil {
		return 0, err
	}
	nextByte := int(b[0])
	tag.tagClass = nextByte & 0xC0
	tag.primitive = nextByte & 0x20
	tag.tagNumber = nextByte & 0x1f

	codeLength := 1

	if tag.tagNumber == 0x1f {
		tag.tagNumber = 0
		numTagBytes := 0

		for {
			_, err := input.Read(b)
			if err != nil {
				return 0, err
			}
			codeLength++
			if numTagBytes >= 6 {
				return 0, errors.New("Tag is too large")
			}
			nextByte = int(b[0])
			tag.tagNumber = tag.tagNumber << 7
			tag.tagNumber |= nextByte & 0x7f
			numTagBytes++

			if (nextByte & 0x80) == 0 {
				break // We are done
			}
		}
	}
	tag.tagBytes = nil // Clear generared bytes, if any
	return codeLength, nil
}

func (tag *BerTag) DecodeAndCheck(input io.Reader) (int, error) {
	if tag.tagBytes == nil {
		tag.code()
	}

	b := []byte{0}
	for _, indentifierByte := range tag.tagBytes {
		nextByte, err := input.Read(b)
		if err != nil {
			return 0, err
		} else if nextByte != int(indentifierByte&0xFF) {
			return 0, errors.New(fmt.Sprintf("Mismatch in data, expected: 0x%02x, got x%02x!", indentifierByte, nextByte))
		}

	}
	return len(tag.tagBytes), nil
}
func (tag *BerTag) Equals(tagClass int, primitive int, tagNumber int) bool {
	return tag.tagNumber == tagNumber && tag.tagClass == tagClass && tag.primitive == primitive
}

func (tag *BerTag) S() string {
	return fmt.Sprintf(
		"identifier class: %d, primitive: %d, Tag number: %d",
		tag.tagClass,
		tag.primitive,
		tag.tagNumber,
	)
}
