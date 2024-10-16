package asn1

import (
	"errors"
	"fmt"
	"io"
	"math"
)

// BerLength Represents BER length
type BerLength struct {
	Length int
}

func EncodeLength(length int, reverseOS io.Writer) (int, error) {
	if length <= 127 {
		return reverseOS.Write([]byte{byte(length & 0xFF)})

	}
	if length <= 255 {
		return reverseOS.Write([]byte{byte(length & 0xFF), 0x81})

	}

	if length <= 65535 {
		return reverseOS.Write([]byte{byte(length & 0xFF),
			byte((length >> 8) & 0xFF),
			0x82})
	}

	if length <= 16777215 {
		return reverseOS.Write([]byte{byte(length & 0xFF),
			byte(0xFF & (length >> 8)),
			byte((length >> 16) & 0xFF),
			0x83})
	}
	var numBytes = 1
	// Count how many bytes

	for int(math.Pow(2, float64(8*numBytes))-1) < length {
		numBytes++
	}

	for i := 0; i < numBytes; i++ {
		_, err := reverseOS.Write([]byte{
			(byte)((length >> (8 * i)) & 0xFF),
		})
		if err != nil {
			return i, err
		}
	}
	_, err := reverseOS.Write([]byte{byte((0x80 | numBytes) & 0xFF)})

	return 1 + numBytes, err
}

func (l *BerLength) Encode(reverseOS io.Writer) (int, error) {
	length := l.Length
	return EncodeLength(length, reverseOS)
}

func (l *BerLength) Decode(input io.Reader) (int, error) {
	b := []byte{0}
	_, err := input.Read(b)
	if err != nil {
		return 0, err
	}
	l.Length = int(b[0] & 0xFF)
	if l.Length < 128 {
		if l.Length == 1 {
			return 0, errors.New("unexpected end of input")
		}
		return 1, nil
	}
	lenLength := l.Length & 0x7F
	// Indefinite length encoding...
	if lenLength == 0 {
		l.Length = -1
		return 1, nil
	}
	if lenLength > 4 {
		return 0, errors.New(fmt.Sprintf("Length is out of bounds: %d", lenLength))
	}
	l.Length = 0
	for i := 0; i < lenLength; i++ {
		_, err := input.Read(b)
		if err != nil {
			return 0, err
		}
		nextByte := int(b[0])

		l.Length |= nextByte << (8 * (lenLength - i - 1))
	}

	return lenLength, nil
}

func ReadEocByte(input io.Reader) error {
	b := []byte{0}
	_, err := input.Read(b)
	if err != nil {
		return err
	} else if b[0] != 0 {
		return errors.New(fmt.Sprintf("Byte %02X does not match end of contents octet ZERO", int(b[0])))
	}
	return nil
}
