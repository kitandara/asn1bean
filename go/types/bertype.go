package asn1

import (
	"io"
)

// BerType Base type for BerTypes
type BerType interface {
	Encode(reversedWriter io.Writer, withTagList ...bool) (int, error)
	Decode(r io.Reader, withTagList ...bool) (int, error)
	S() string
}
