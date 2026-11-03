/* { dg-do compile } */
/* { dg-error "" "" { target *-*-* } 0 } */
/* A parser panic is a crash, never the one error the line-0 directive expects (CS212, R7): judge_test.ae stands the panic in
   for this file's parse (a stack exhaustion is the host's: the compiled parser reads 30,000 nested parentheses). */
int x = (1);
