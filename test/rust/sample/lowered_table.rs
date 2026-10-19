// The expression lowered.rs includes (include! in an expression's place: the file is one
// expression, parsed with nothing stripped).
[
    1 + 2, // three
    !
    4,
    /* five */ 5,
    { let x = 6; x },
]
