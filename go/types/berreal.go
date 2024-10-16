package asn1

import (
	"dsmagic.com/asn1"
	"errors"
	"fmt"
	"io"
	"math"
	"math/big"
	"math/bits"
	"strconv"
	"strings"
)

type BerReal struct {
	value float64
}

var realTag = asn1.NewBerTag(asn1.UNIVERSAL_CLASS, asn1.PRIMITIVE, asn1.REAL_TAG)

func NewBerReal(v float64) *BerReal {
	return &BerReal{value: v}
}

func (b *BerReal) Encode(reversedWriter io.Writer, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}

	n, err := b.encodeValue(reversedWriter)
	codeLength := n
	n, err = asn1.EncodeLength(codeLength, reversedWriter)
	codeLength += n
	if err != nil {
		return 0, err
	}
	if withTag {
		n, err = realTag.Encode(reversedWriter)
		codeLength += n
	}
	return codeLength, err
}

func (b *BerReal) Decode(input io.Reader, withTagList ...bool) (int, error) {
	var withTag bool
	if len(withTagList) > 0 {
		withTag = withTagList[0]
	} else {
		withTag = true
	}
	codeLength := 0

	if withTag {
		n, err := realTag.DecodeAndCheck(input)
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
		b.value = 0
		return codeLength, nil
	}
	if berLength.Length == 1 {
		nextByte, err := asn1.ReadByte(input)
		if err != nil {
			return 0, err
		} else if nextByte == 0x40 {
			b.value = math.Inf(1)
		} else if nextByte == 0x41 {
			b.value = math.Inf(-1)
		} else {
			return 0, errors.New("invalid real encoding")
		}
		return codeLength + 1, nil
	}
	byteCode := make([]byte, berLength.Length)
	_, err = io.ReadFull(input, byteCode)
	if err != nil {
		return 0, err
	}
	codeLength += berLength.Length
	formatOctet := int(byteCode[0] & 0xFF)

	if (formatOctet & 0x80) != 0x80 {
		if formatOctet == 0 || (formatOctet|0x03) != 0x03 {
			return 0, errors.New("only binary and decimal REAL encoding is supported")
		}

		str := string(byteCode[1:])
		str = strings.Replace(str, ",", ".", -1)
		f, err := strconv.ParseFloat(str, 64)
		if err != nil {
			return 0, err
		}
		b.value = f

		return codeLength, nil
	}

	tempLength := 1
	sign := 1
	if (byteCode[0] & 0x40) == 0x40 {
		sign = -1
	}

	var exponentLength = int((byteCode[0] & 0x03) + 1)
	if exponentLength == 4 {
		exponentLength = int(byteCode[1])
		tempLength++
	}
	tempLength += exponentLength
	exponent := 0

	for i := 0; i < exponentLength; i++ {
		exponent |= byteCode[1+i] << (8 * (exponentLength - i - 1))
	}

	mantissa := int64(0)
	for i := 0; i < berLength.Length-tempLength; i++ {
		mantissa |= (byteCode[i+tempLength] & 0xff) << (8 * (berLength.Length - tempLength - i - 1))
	}

	b.value = float64(sign) * float64(mantissa) * math.Pow(2, float64(exponent))
	return codeLength, nil
}

func (b *BerReal) encodeValue(writer io.Writer) (int, error) {

	longBits := math.Float64bits(b.value)
	isNegative := (longBits & uint64(0x8000000000000000)) == uint64(0x8000000000000000)
	exponent := ((int)(longBits >> 52)) & 0x7ff
	mantissa := (longBits & uint64(0x000fffffffffffff)) | uint64(0x0010000000000000)

	if exponent == 0x7ff {
		if mantissa == 0x0010000000000000 {
			if isNegative {
				// - infinity
				_, _ = asn1.WriteByte(writer, 0x41)
			} else {
				// + infinity
				_, _ = asn1.WriteByte(writer, 0x40)
			}
			return 1, nil
		} else {
			return 0, errors.New("NAN not supported")
		}
	}

	if exponent == 0 && mantissa == 0x0010000000000000 {
		// zero
		return 0, nil
	}

	// subtract 1023 and 52
	// from the exponent to get an exponent corresponding to an integer matissa as need here.
	exponent -= 1075

	// trailing zeros of the mantissa should be removed. Therefor find out how much the mantissa can
	// be shifted and
	// the exponent can be increased
	exponentIncr := 0
	for ((mantissa >> exponentIncr) & 0xff) == 0x00 {
		exponentIncr += 8
	}
	for ((mantissa >> exponentIncr) & 0x01) == 0x00 {
		exponentIncr++
	}

	exponent += exponentIncr
	mantissa >>= exponentIncr

	mantissaLength := (8 - bits.LeadingZeros64(mantissa) + 7) / 8

	for i := 0; i < mantissaLength; i++ {
		_, _ = asn1.WriteByte(writer, mantissa>>(8*i))
	}
	codeLength := mantissaLength

	exponentBytes := new(big.Int).SetInt64(int64(exponent)).Bytes()
	_, _ = writer.Write(exponentBytes)
	codeLength += len(exponentBytes)
	var exponentFormat int

	if len(exponentBytes) < 4 {
		exponentFormat = len(exponentBytes) - 1
	} else {
		_, _ = asn1.WriteByte(writer, len(exponentBytes))
		codeLength++
		exponentFormat = 0x03
	}

	if isNegative {
		_, _ = asn1.WriteByte(writer, 0x80|0x40|exponentFormat)
	} else {
		_, _ = asn1.WriteByte(writer, 0x80|exponentFormat)
	}

	codeLength++

	return codeLength, nil
}

func (b *BerReal) S() string {
	return fmt.Sprintf("%f", b.value)
}
