#include <coroutine>
struct Gen { struct promise_type { int v;
  Gen get_return_object(){ return Gen{std::coroutine_handle<promise_type>::from_promise(*this)}; }
  std::suspend_always initial_suspend(){return {};} std::suspend_always final_suspend() noexcept{return {};}
  std::suspend_always yield_value(int x){ v=x; return {}; } void return_void(){} void unhandled_exception(){} };
  std::coroutine_handle<promise_type> h; };
Gen count(int n){ for(int i=0;i<n;i++) co_yield i; }
int main(){ Gen g=count(3); int s=0; while(!g.h.done()){ g.h.resume(); if(!g.h.done()) s+=g.h.promise().v; } g.h.destroy(); return s; }
