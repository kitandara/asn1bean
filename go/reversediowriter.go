package asn1

// ReversedIoWriter Writer for outputing ber data in reversed order, so we can make it easier to write BER types.
type ReversedIoWriter struct {
	buffer []byte // The storage buffer
	index  int
}

func (w *ReversedIoWriter) Write(b []byte) (int, error) {
	n := len(b)
	// Resize it as needed.
	for w.index-n < 0 {
		w.resize()
	}

	i := w.index - n + 1
	for j := 0; j < n; j++ {
		w.buffer[i] = b[j]
		i++
	}
	w.index -= n
	return n, nil
}

func (w *ReversedIoWriter) GetBytes() []byte {
	if w.index < 0 {
		return w.buffer
	}
	subBufferLen := len(w.buffer) - w.index - 1
	b := make([]byte, subBufferLen)
	for i := 0; i < subBufferLen; i++ {
		b[i] = w.buffer[w.index+i+1]
	}
	return b
}

// Resize the buffer. Make a new buffer that is twice the size of existing, then copy old items to the end of it.
func (w *ReversedIoWriter) resize() {
	n := len(w.buffer)
	newBuffer := make([]byte, n*2)

	j := len(newBuffer) - 1
	for i := n - 1; i > w.index; i-- {
		newBuffer[j] = w.buffer[i]
		j--
	}
	w.index = j
	w.buffer = newBuffer
}

var (
	initialSize = 128
)

func (w *ReversedIoWriter) reset() {
	w.index = len(w.buffer) - 1
}
func NewReversedIOWriter() *ReversedIoWriter {
	return &ReversedIoWriter{
		buffer: make([]byte, initialSize),
		index:  initialSize - 1,
	}
}
