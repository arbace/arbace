package breaks

func Valid(s string) bool {
	for _, r := range s {
		switch r {
		case '-', '.', '_', ':', '~', '!', '$', '&', '\'',
			'(', ')', '*', '+', ',', ';', '=', '%':
			continue
		case '@':
			return false
		}
	}
	return true
}

func Kind(x any) int {
	switch x.(type) {
	case int, int8,
		int16:
		return 1
	}
	return 0
}

func Pair() (int, string) {
	return 1,
		"two"
}

func Multi() int {
	a, b :=
		1, 2
	c,
		d := 3, 4
	return a + b + c + d
}
