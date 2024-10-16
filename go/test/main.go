package main

import (
	asn12 "dsmagic.com/asn1"
	asn1 "dsmagic.com/asn1/types"
	"encoding/hex"
	"fmt"
)

func main() {

	d := asn1.NewBerEnum(4)
	w := asn12.NewReversedIOWriter()

	d.Encode(w)

	xd := w.GetBytes()

	fmt.Println("WE got: " + hex.EncodeToString(xd))
}
