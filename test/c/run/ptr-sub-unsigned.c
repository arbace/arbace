/* Pointer minus an unsigned int (doc/C23-SURVEY.md, "As built: review 006's fixes", finding 2): one element back. */
int a[4] = {1,2,3,4}; int main(void) { int *p = a + 3; unsigned k = 1; return *(p - k); }
