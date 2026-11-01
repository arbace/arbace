struct T{}; struct U{ U(T){} };
int main(){ U u(T()); U w((T())); (void)w; }
