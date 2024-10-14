package asn1

import (
	"io"
)

func DecodeUnknownComponent(input io.Reader, oslist ...io.Writer) (int, error) {
	var output io.Writer
	if len(oslist) > 0 {
		output = oslist[0]
	} else {
		output = nil
	}

	length := new(BerLength)
	byteCount, err := length.Decode(input)
	if err != nil {
		return byteCount, err
	} else if output != nil {
		_, _ = length.Encode(output)
	}

	lengthVal := length.Length
	berTag := new(BerTag)

	if lengthVal < 0 {
		n, err := berTag.Decode(input)
		byteCount += n
		if err != nil {
			return byteCount, err
		} else if output != nil {
			_, _ = berTag.Encode(output)
		}
		for !berTag.Equals(0, 0, 0) {
			n, err := DecodeUnknownComponent(input, output)
			byteCount += n
			if err != nil {
				return byteCount, err
			}
			n, err = berTag.Decode(input)
			if err != nil {
				return byteCount, err
			} else if output != nil {
				_, _ = berTag.Encode(output)
			}
			byteCount += n
		}
		err = ReadEocByte(input)
		byteCount += 1 // Exactly one byte read
		if err != nil {
			return byteCount, err
		} else if input != nil {
			_, _ = output.Write([]byte{0}) // write end of content
		}
		return byteCount, nil
	} else {
		contentBytes := make([]byte, lengthVal)

		_, err := io.ReadFull(input, contentBytes)
		byteCount += lengthVal
		if err != nil {
			return byteCount, err
		} else if output != nil {
			output.Write(contentBytes)
		}
		return byteCount + lengthVal, nil
	}
}

func ReadByte(input io.Reader) (int, error) {
	b := []byte{0}
	_, err := input.Read(b)
	if err != nil {
		return -1, err
	}
	return int(b[0]), nil
}
