// a small program: accounts with a base class and an override, statics, references, a status from main
#include <cstdio>
class Account {
  static int next_id;
protected:
  int id;
  long balance;
public:
  explicit Account(long initial) : id(next_id++), balance(initial) {}
  virtual ~Account() {}
  virtual bool withdraw(long amount) { if (amount > balance) return false; balance -= amount; return true; }
  void deposit(long amount) { balance += amount; }
  long get() const { return balance; }
  int number() const { return id; }
};
int Account::next_id = 1000;
class Credit : public Account {
  long limit;
public:
  Credit(long initial, long lim) : Account(initial), limit(lim) {}
  bool withdraw(long amount) override { if (amount > balance + limit) return false; balance -= amount; return true; }
};
bool transfer(Account &from, Account &to, long amount) { if (!from.withdraw(amount)) return false; to.deposit(amount); return true; }
int main() {
  Account a(100);
  Credit c(50, 200);
  printf("%d %d\n", a.number(), c.number());
  printf("%d %d %d\n", transfer(a, c, 70), transfer(a, c, 70), transfer(c, a, 300));
  printf("%ld %ld\n", a.get(), c.get());
  return a.get() > 300 ? 3 : 4;
}
